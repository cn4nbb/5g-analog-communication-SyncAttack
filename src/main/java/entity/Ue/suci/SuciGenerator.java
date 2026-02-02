package entity.Ue.suci;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

public class SuciGenerator {
    //ECIES加密SUPI，实现SUCI
    private static final String EC_ALGO = "EC";
    private static final String AGREEMENT_ALGO = "ECDH";
    private static final String SYM_CIPHER = "AES/ECB/PKCS5Padding";
    private final PublicKey hnPublicKey;
    private final String supi;

    public SuciGenerator(String supi) {
        this.supi = supi;
        this.hnPublicKey = loadHomeNetworkPublicKey();
    }

    /**
     * 生成 SUCI
     * @return 按 SUCI 格式输出的字节数组
     */
    public byte[] generate() {
        try {
            // 1. 生成临时EC密钥对
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(EC_ALGO);
            kpg.initialize(256);
            KeyPair ephKeyPair = kpg.generateKeyPair();

            // 2. ECDH计算共享密钥
            KeyAgreement ka = KeyAgreement.getInstance(AGREEMENT_ALGO);
            ka.init(ephKeyPair.getPrivate());
            ka.doPhase(hnPublicKey, true);
            byte[] sharedSecret = ka.generateSecret();

            // 3. 使用共享密钥的前16字节做AES密钥
            byte[] aesKeyBytes = Arrays.copyOf(sharedSecret, 16);
            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

            // 4. 加密SUPI
            Cipher cipher = Cipher.getInstance(SYM_CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            byte[] encryptedSupi = cipher.doFinal(supi.getBytes());

            // 5. 拼装SUCI = [版本|公钥ID|EphPub|密文]
            byte[] ephPub = ephKeyPair.getPublic().getEncoded();
            byte[] version = new byte[]{0x01};
            byte[] keyId = new byte[]{0x01};

            return concat(version, concat(keyId, concat(ephPub, encryptedSupi)));
        } catch (Exception e) {
            System.err.println("SUCI Failed to generate: " + e.getMessage());
            // 失败时返回明文SUPI
            return ("SUPI:" + supi).getBytes();
        }
    }

    private PublicKey loadHomeNetworkPublicKey() {
        try {
            // TODO: 填入实际 Base64 编码的 HN 公钥
            String b64Key = System.getProperty("hn.publicKey.base64", "");
            if (b64Key == null || b64Key.isEmpty()) {
                throw new IllegalArgumentException("HN public key not configured");
            }
            byte[] keyBytes = Base64.getDecoder().decode(b64Key);
            KeyFactory kf = KeyFactory.getInstance(EC_ALGO);
            return kf.generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (Exception e) {
            System.err.println("Failed to load HN public key, a temporary key will be used: " + e.getMessage());
            // fallback: 生成临时 EC 密钥对，仅测试用途
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance(EC_ALGO);
                kpg.initialize(256);
                KeyPair kp = kpg.generateKeyPair();
                return kp.getPublic();
            } catch (GeneralSecurityException ex) {
                throw new RuntimeException("Unable to generate temporary EC public key", ex);
            }
        }
    }

    private byte[] concat(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
}
