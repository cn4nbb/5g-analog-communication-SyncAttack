package entity.CoreNet;

import java.security.SecureRandom;
import java.util.Arrays;

public class Udm {
    private final byte[] k; // Master key
    private final byte[] op;
    private final SecureRandom rnd = new SecureRandom();

    //5gAKA状态
    private long sqn = 0;  //顺序计数
    private byte[] expectedXres;  //存储AMF期望的XRES

    public Udm(byte[] k, byte[] op) {
        if (k.length != 16 || op.length != 16)
            throw new IllegalArgumentException("K and OP must be 16 byte");
        this.k  = Arrays.copyOf(k, 16);
        this.op = Arrays.copyOf(op, 16);
    }

    /**
     * 返回存储的 Authentication Server Key
     */
    public byte[] getAuthenticationKey() {
        return k;
    }
    /**
     * 返回存储的 Operator Variant
     */
    public byte[] getOperatorVariant() {
        return op;
    }

    /**
     * 获取当前的顺序计数 SQN，并自增
     * @return
     */
    public synchronized long getAndIncrementSqn() {
        return sqn++;
    }
    /** 存储 AMF 在生成向量时的 XRES */
    public synchronized void storeExpectedXres(byte[] xres) {
        this.expectedXres = Arrays.copyOf(xres, xres.length);
    }
    /** AMF 验证 RES 是否与预期 XRES 相同 */
    public synchronized boolean verifyXres(byte[] res) {
        if (expectedXres == null) return false;
        boolean ok = Arrays.equals(expectedXres, res);
        if (!ok) {
            System.err.println("UDM: RES Verification failed，expect=" +
                    bytesToHex(expectedXres) + "，actual=" + bytesToHex(res));
        }
        return ok;
    }
    /**
     * 生成随机数 RAND
     */
    public byte[] generateRand() {
        byte[] rand = new byte[8];
        rnd.nextBytes(rand);
        return rand;
    }
    /** 调试用：hex 字符串 */
    private static String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) sb.append(String.format("%02X", b));
        return sb.toString();
    }
}
