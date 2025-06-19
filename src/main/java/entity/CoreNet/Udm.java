package entity.CoreNet;

import java.security.SecureRandom;

public class Udm {
    private final byte[] k; // Master key
    private final SecureRandom rnd = new SecureRandom();

    public Udm() {
        // 模拟订阅的 Kausf
        this.k = new byte[16];
        rnd.nextBytes(k);
    }

    /**
     * 返回存储的 Authentication Server Key
     */
    public byte[] getAuthenticationKey() {
        return k;
    }

    /**
     * 生成随机数 RAND
     */
    public byte[] generateRand() {
        byte[] rand = new byte[8];
        rnd.nextBytes(rand);
        return rand;
    }
}
