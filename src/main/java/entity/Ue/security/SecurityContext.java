package entity.Ue.security;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import entity.CoreNet.Milenage;

import static org.bouncycastle.pqc.math.linearalgebra.ByteUtils.xor;

public class SecurityContext {
    //KDF链，密钥管理。MAC/ENC
    // Master key from HSS/UDM
    private final byte[] kAusf;
    private byte[] kAmf;
    private byte[] knasEnc;
    private byte[] knasInt;
    private final SecureRandom random = new SecureRandom();
    private boolean securityActivated = false;
    private Milenage milenage;
    private final byte[] opVariant;

    private byte[] ck;
    private byte[] ik;

    // 记录 UE 最后一次接受的网络 SQN（用于 Sync Failure 检测）
    private long lastSeenSqn = -1;

    public SecurityContext(byte[] kAusf, byte[] opVariant) {
        if (kAusf.length != 16 || opVariant.length != 16)
            throw new IllegalArgumentException("kAusf and opVariant must be 16 字节");
        this.kAusf      = Arrays.copyOf(kAusf, 16);
        this.opVariant  = Arrays.copyOf(opVariant, 16);
        this.milenage   = new Milenage(this.kAusf, this.opVariant);
    }

    public void initMil(byte[] K, byte[] OP) {
        this.milenage = new Milenage(K,OP);
    }
    /**
     * 执行 KDF 链生成 AMF 和 NAS 密钥
     */
    public void deriveKeys(byte[] rand) {
        // 简化示例：使用 KDF(masterKey||RAND) -> kAmf
        this.kAmf = kdf(concat(kAusf, rand), 32);
        // kNasEnc = KDF(kAmf||"nasEnc")
        this.knasEnc = kdf(concat(kAmf, "nasEnc".getBytes()), 16);
        // kNasInt = KDF(kAmf||"nasInt")
        this.knasInt = kdf(concat(kAmf, "nasInt".getBytes()), 16);
        System.out.println("SEC: derive kAmf, knasEnc, knasInt");
    }

    /**
     * 构造 NAS 完整性的安全头或者加密头
     * @param seq NAS 序列号
     * @param newSecurity 表示是否为 Security Mode Complete 之后的消息
     */
    public byte[] createNasSecurityHeader(int seq, boolean newSecurity) {
        byte[] header = new byte[6];
        header[0] = newSecurity ? (byte) 0x7E : (byte) 0x5C; // 0x5C=Integrity only; 0x7E=Integrity+Ciphering
        header[1] = (byte) seq;
        // 随机数或其他字段，可扩展
        byte[] rndBytes = new byte[4];
        random.nextBytes(rndBytes);
        System.arraycopy(rndBytes,0, header, 2, rndBytes.length);
        return header;
    }

    /**
     * 对 NAS 消息进行完整性保护和可选加密
     */
    public byte[] protectNas(byte[] header, byte[] body) {
        try {
            // 1. 计算 MAC-I
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec macKey = new SecretKeySpec(knasInt, "HmacSHA256");
            mac.init(macKey);
            mac.update(header);
            mac.update(body);
            byte[] macI = Arrays.copyOf(mac.doFinal(), 4);

            // 2. 加密 body
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[16]; random.nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(knasEnc, "AES"), new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(body);

            // 组装: header || IV || encrypted || MAC-I
            return concat(header, concat(iv, concat(encrypted, macI)));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * UE 根据 Authentication Request 中的 RAND、AUTN 进行条件检查：
     *   - MAC 校验失败 → return MAC Failure (0x04)
     *   - SQN 重放或过期 → return Sync Failure (0x06||AUTS)
     *   - 否则正常return RES
     */
    public byte[] computeAuthenticationResponse(byte[] rand,byte[] autn) {
        // 1) 恢复网络 SQN
        long sqnNet = milenage.extractSqn(rand, autn);
        // 2) 生成向量便于校验
        Milenage.AuthVector vec = milenage.generateAuthVectorWithRand(rand, sqnNet);

        // 3) MAC-A 校验 (f1)
        //byte[] computedMac = Arrays.copyOf(vec.AUTN, 8);      // 前 8 字节是 MAC-A
        byte[] computedMac = Arrays.copyOfRange(vec.AUTN, 6, 14);
        byte[] recvMac     = Arrays.copyOfRange(autn, 6, 14);
        if (!Arrays.equals(computedMac, recvMac)) {
            System.out.println("SEC: MAC Verification failed → return MAC Failure");
            return new byte[]{0x04};  // NAS MAC Failure
        }

        // 4) SQN 校验：如果 sqnNet <= lastSeenSqn，视为重放
        if (sqnNet <= lastSeenSqn) {
            // 生成 AUTS，return Sync Failure
            byte[] auts = milenage.generateAUTS(sqnNet);
            byte[] msg = new byte[1 + auts.length];
            msg[0] = 0x06;  // NAS Sync Failure
            System.arraycopy(auts, 0, msg, 1, auts.length);
            System.out.println("SEC: SQN Verification failed → return Sync Failure + AUTS");
            return msg;
        }

        // 5) 校验通过，更新 lastSeenSqn，正常生成 RES
        lastSeenSqn = sqnNet;
        // 按原逻辑保存 CK/IK 并派生 NAS 密钥
        this.ck = vec.CK; this.ik = vec.IK;
        deriveKeysWithCKIK(ck, ik);
        System.out.println("SEC: Certification passed → return RES");
        return vec.XRES;
    }

    /** 用 CK/IK 生成 KAMF、KNAS-ENC、KNAS-INT */
    private void deriveKeysWithCKIK(byte[] ck, byte[] ik) {
        // 真实 KDF 是 TS 33.501 里定义的 nccf 链，这里简化：
        byte[] kamf = kdf(concat(ck, ik), 32);
        this.kAmf   = kamf;
        this.knasEnc= kdf(concat(kAmf, "nasEnc".getBytes()), 16);
        this.knasInt= kdf(concat(kAmf, "nasInt".getBytes()), 16);
        System.out.println("SEC: derive kAmf, knasEnc, knasInt (based CK/IK)");
    }
    // 工具方法：简单 KDF
    private byte[] kdf(byte[] input, int length) {
        try {
            // 使用 PBKDF2(HMAC-SHA256), iterations=1000
            PBEKeySpec spec = new PBEKeySpec(
                    toHex(input).toCharArray(), input, 1000, length * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            SecretKey key = skf.generateSecret(spec);
            return key.getEncoded();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] concat(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    private String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public void activateSecurity() {
        this.securityActivated = true;
        System.out.println("SEC: Security context has been activated");
    }

    public boolean isSecurityActivated() {
        return securityActivated;
    }

    // 修改处理安全模式命令的方法
    public void handleSecurityModeCommand(byte[] securityHeader) {
        // 解析安全头并激活安全
        this.securityActivated = true;
        System.out.println("SEC: Handle safe mode commands and activate safety");
    }

}
