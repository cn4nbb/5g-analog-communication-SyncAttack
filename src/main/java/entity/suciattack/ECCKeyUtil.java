package entity.suciattack;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.KeyAgreement;
import java.security.*;
import java.security.spec.ECGenParameterSpec;

/**
 * 椭圆曲线密钥管理工具类
 * 支持标准曲线（secp256r1），用于模拟SUCI中使用的ECIES密钥协商机制。
 */
public class ECCKeyUtil {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final String CURVE = "secp256r1";

    /** 生成一对EC密钥（可选随机源） */
    public static KeyPair generateECKeyPair(SecureRandom random) throws GeneralSecurityException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", "BC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec(CURVE);
        if (random != null)
            keyGen.initialize(ecSpec, random);
        else
            keyGen.initialize(ecSpec);
        return keyGen.generateKeyPair();
    }

    /** 计算ECDH共享密钥 */
    public static byte[] deriveSharedSecret(PrivateKey priv, PublicKey pub) throws GeneralSecurityException {
        KeyAgreement ka = KeyAgreement.getInstance("ECDH", "BC");
        ka.init(priv);
        ka.doPhase(pub, true);
        return ka.generateSecret();
    }
}
