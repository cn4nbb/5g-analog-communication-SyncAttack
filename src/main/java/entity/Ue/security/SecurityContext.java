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

public class SecurityContext {
    //KDF链，密钥管理。MAC/ENC
    // Master key from HSS/UDM
    private final byte[] kAusf;
    private byte[] kAmf;
    private byte[] knasEnc;
    private byte[] knasInt;
    private final SecureRandom random = new SecureRandom();
    private boolean securityActivated = false;
    public SecurityContext(byte[] kAusf) {
        this.kAusf = kAusf;
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
        System.out.println("SEC: 派生 kAmf, knasEnc, knasInt");
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
     * UE 根据 Authentication Request 中的 RAND、AUTN 验证并计算 RES
     * @param rand 从事件中获取的 RAND
     */
    public byte[] computeAuthenticationResponse(byte[] rand) {
        // 派生所有密钥
        deriveKeys(rand);
        // 简化示例：RES = HMAC(kAmf, rand)
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(kAmf, "HmacSHA256");
            mac.init(key);
            byte[] res = mac.doFinal(rand);
            System.out.println("SEC: 计算 RES 返回 UE Authentication Response");
            return res;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
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
        System.out.println("SEC: 安全上下文已激活");
    }

    public boolean isSecurityActivated() {
        return securityActivated;
    }

    // 修改处理安全模式命令的方法
    public void handleSecurityModeCommand(byte[] securityHeader) {
        // 解析安全头并激活安全
        this.securityActivated = true;
        System.out.println("SEC: 处理安全模式命令，激活安全");
    }
}
