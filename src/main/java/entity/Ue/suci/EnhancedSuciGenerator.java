package entity.Ue.suci;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

/**
 * 更严格的 SUCI 生成器：
 * 模拟 5G SUCI 随机性攻击场景：
 *   - 强随机：高熵 SecureRandom 生成临时密钥
 *   - 弱随机：固定种子 Random 导致重复概率高
 * 严谨实现 ECIES (ECDH + AES-GCM) 加密过程
 */
public class EnhancedSuciGenerator {

    private static final ECParameterSpec ecSpec;
    private static final KeyPairGenerator keyGen;
    private static final PublicKey HN_PUBLIC_KEY; // 模拟运营商公钥

    static {
        try {
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
                System.out.println("BouncyCastle provider loaded successfully.");
            }

            ecSpec = ECNamedCurveTable.getParameterSpec("secp256r1");
            keyGen = KeyPairGenerator.getInstance("EC", "BC");
            keyGen.initialize(ecSpec, new SecureRandom());

            // 模拟 Home Network 公钥（固定）
            KeyPairGenerator hnGen = KeyPairGenerator.getInstance("EC", "BC");
            hnGen.initialize(ecSpec, new SecureRandom("home-network-key".getBytes(StandardCharsets.UTF_8)));
            HN_PUBLIC_KEY = hnGen.generateKeyPair().getPublic();

        } catch (Exception e) {
            throw new RuntimeException("初始化椭圆曲线失败: " + e.getMessage(), e);
        }
    }

    private final boolean weakRandom;
    private final Random weakRand;

    public EnhancedSuciGenerator(boolean weakRandom) {
        this.weakRandom = weakRandom;
        this.weakRand = weakRandom ? new Random(2024) : null;
    }

    public String generateSuci(String imsi) throws Exception {
        // === Step 1: 生成临时密钥对 ===
        KeyPair ephemeral;
        if (weakRandom) {
            // 固定种子 → 可预测/重复
            SecureRandom lowEntropyRand = SecureRandom.getInstance("SHA1PRNG");
            lowEntropyRand.setSeed(weakRand.nextLong());
            keyGen.initialize(ecSpec, lowEntropyRand);
            ephemeral = keyGen.generateKeyPair();
        } else {
            keyGen.initialize(ecSpec, new SecureRandom());
            ephemeral = keyGen.generateKeyPair();
        }

        // === Step 2: ECDH 生成共享密钥 ===
        KeyAgreement ka = KeyAgreement.getInstance("ECDH", "BC");
        ka.init(ephemeral.getPrivate());
        ka.doPhase(HN_PUBLIC_KEY, true);
        byte[] sharedSecret = ka.generateSecret();

        // === Step 3: 从共享密钥派生 AES-GCM 密钥 ===
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] aesKeyBytes = Arrays.copyOf(sha256.digest(sharedSecret), 16);
        SecretKeySpec aesKey = new SecretKeySpec(aesKeyBytes, "AES");

        // === Step 4: 随机 IV（弱随机时可复现） ===
        byte[] iv = new byte[12];
        if (weakRandom) {
            Arrays.fill(iv, (byte) 0x5A); // 固定 IV → 可重复
        } else {
            new SecureRandom().nextBytes(iv);
        }

        // === Step 5: 加密 IMSI ===
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
        byte[] encrypted = cipher.doFinal(imsi.getBytes(StandardCharsets.UTF_8));

        // === Step 6: 生成 SUCI ===
        String rHex = bytesToHex(((ECPublicKey) ephemeral.getPublic()).getQ().getEncoded(false));
        String suciPayload = Base64.getEncoder().encodeToString(encrypted);

        return rHex + ":" + suciPayload;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes)
            sb.append(String.format("%02X", b));
        return sb.toString();
    }
}
