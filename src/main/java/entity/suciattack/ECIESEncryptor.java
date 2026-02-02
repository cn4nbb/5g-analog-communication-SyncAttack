package entity.suciattack;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * ECIES 加密器
 * 模拟 SUCI 生成过程：
 *   - 强随机：使用 SecureRandom 强随机源 → SUCI 唯一
 *   - 弱随机：使用固定种子 Random → SUCI 公钥 R 可能复用（部分重复）
 */
public class ECIESEncryptor {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final KeyPair homeNetworkKeyPair;  // HN端密钥对（PK_HN, SK_HN）
    private final boolean weakRandom;

    private static final String CURVE = "secp256r1";
    private static final String SYM_ALGO = "AES/CTR/NoPadding";

    public ECIESEncryptor(boolean weakRandom) throws GeneralSecurityException {
        this.weakRandom = weakRandom;
        this.homeNetworkKeyPair = ECCKeyUtil.generateECKeyPair(SecureRandom.getInstanceStrong());
    }

    /** 模拟UE侧生成SUCI，返回字符串形式 “R_hex:Base64(cipher)” */
    public String generateSUCI(String imsi) throws Exception {
        KeyPair ueEphemeralKey = generateEphemeralKeyPair();
        byte[] sharedSecret = ECCKeyUtil.deriveSharedSecret(ueEphemeralKey.getPrivate(), homeNetworkKeyPair.getPublic());

        // === KDF: SHA-256(sharedSecret) → AES Key ===
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] keyMaterial = sha256.digest(sharedSecret);
        SecretKeySpec aesKey = new SecretKeySpec(Arrays.copyOf(keyMaterial, 16), "AES");

        // === IV ===
        byte[] iv;
        if (weakRandom) {
            // 模拟弱随机漏洞：部分UE共享相同IV
            iv = new byte[16];
            Arrays.fill(iv, (byte) 0x11);
        } else {
            iv = SecureRandom.getInstanceStrong().generateSeed(16);
        }

        Cipher cipher = Cipher.getInstance(SYM_ALGO, "BC");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(iv));
        byte[] cipherText = cipher.doFinal(imsi.getBytes());

        // === 拼接 SUCI ===
        byte[] R = ueEphemeralKey.getPublic().getEncoded();
        String rHex = bytesToHex(R).substring(0, 80); // 前缀显示
        String cipherBase64 = Base64.getEncoder().encodeToString(cipherText);

        return rHex + ":" + cipherBase64;
    }

    private KeyPair generateEphemeralKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC", "BC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec(CURVE);
        if (weakRandom) {
            // 弱随机：低熵，容易重复
            SecureRandom lowEntropy = SecureRandom.getInstance("SHA1PRNG");
            lowEntropy.setSeed(20241123L);  // 固定种子
            gen.initialize(ecSpec, lowEntropy);
        } else {
            gen.initialize(ecSpec, SecureRandom.getInstanceStrong());
        }
        return gen.generateKeyPair();
    }

    private static String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) sb.append(String.format("%02X", b));
        return sb.toString();
    }
}
