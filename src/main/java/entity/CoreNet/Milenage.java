package entity.CoreNet;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.params.KeyParameter;

import java.util.Arrays;

public class Milenage {
    // ROT constants (bit rotations)
    private static final int R1 = 64, R2 = 0, R3 = 32, R4 = 64;

    private final byte[] K;     // subscriber key (16B)
    private final byte[] OP;    // operator variant (16B)
    private final byte[] OPc;   // OPc = AES-128(K, OP) ⊕ OP
    private final BlockCipher aes;

    public Milenage(byte[] K, byte[] OP) {
        if (K.length != 16 || OP.length != 16)
            throw new IllegalArgumentException("K and OP must be 16 bytes");
        this.K = Arrays.copyOf(K, 16);
        this.OP = Arrays.copyOf(OP, 16);
        this.aes = new AESEngine();
        this.OPc = computeOPc();
    }

    /** 认证向量 */
    public static class AuthVector {
        public final byte[] RAND, AUTN, XRES, CK, IK, AK;
        public AuthVector(byte[] RAND, byte[] AUTN, byte[] XRES, byte[] CK, byte[] IK, byte[] AK) {
            this.RAND = RAND; this.AUTN = AUTN;
            this.XRES = XRES; this.CK = CK; this.IK = IK; this.AK = AK;
        }
    }

    /** 生成一组 AuthVector */
    public AuthVector generateAuthVector(long SQN) {
        byte[] RAND = generateRAND();
        //temp对应规范里的  TEMP = E_K(RAND ⊕ OPc) ⊕ OPc
        //后续所有的f函数都基于同一个TEMP节省重复加密
        byte[] temp = aesEncrypt(xor(RAND, OPc));
        temp = xor(temp, OPc);

        //f5函数 生成AK
        // AK = AES(K, ROT(temp,R4) ⊕ OPc) ⊕ OPc 的前6个字节
        byte[] out4 = aesEncrypt(xor(rotate(temp, R4), OPc));
        byte[] AK = xor(Arrays.copyOf(out4, 6), Arrays.copyOf(OPc, 6));

        // 生成AUTN  f1 拼接 SQN掩码
        // AUTN = (SQN ⊕ AK) || MAC-A
        byte[] sqnXorAk = xor(longToBytes(SQN), AK);
        byte[] in1 = xor(rotate(temp, R1), OPc);
        byte[] out1 = aesEncrypt(in1);
        out1 = xor(out1, OPc);
        // f1公式计算得到MAC_A
        byte[] MAC_A = Arrays.copyOf(out1, 8);
        byte[] AUTN = concat(sqnXorAk, MAC_A);
        //f2公式计算生成XRES
        // XRES = first 8 of AES(K, ROT(temp,R2) ⊕ OPc) ⊕ OPc
        byte[] out2 = aesEncrypt(xor(rotate(temp, R2), OPc));
        byte[] XRES = xor(Arrays.copyOf(out2, 8), Arrays.copyOf(OPc, 8));

        //f3公式生成CK
        // CK = first 16 of AES(K, ROT(temp,R3) ⊕ OPc) ⊕ OPc
        byte[] out3 = aesEncrypt(xor(rotate(temp, R3), OPc));
        byte[] CK  = xor(Arrays.copyOf(out3, 16), Arrays.copyOf(OPc, 16));

        //f4 公式生成IK
        // IK = first 16 of AES(K, temp ⊕ OPc) ⊕ OPc
        byte[] in5 = xor(temp, OPc);
        byte[] out5 = aesEncrypt(in5);
        out5 = xor(out5, OPc);
        byte[] IK = Arrays.copyOf(out5, 16);

        return new AuthVector(RAND, AUTN, XRES, CK, IK, AK);
    }

    // —— 工具方法 ——

    /** 生成随机 RAND (16B) */
    private byte[] generateRAND() {
        byte[] rand = new byte[16];
        new java.security.SecureRandom().nextBytes(rand);
        return rand;
    }

    /** 计算 OPc = AES(K, OP) ⊕ OP */
    private byte[] computeOPc() {
        byte[] out = aesEncrypt(OP);
        return xor(out, OP);
    }

    /** AES-128(K, in) */
    private byte[] aesEncrypt(byte[] in) {
        aes.init(true, new KeyParameter(K));
        byte[] out = new byte[16];
        aes.processBlock(in, 0, out, 0);
        return out;
    }

    /** 128-bit rotate left by r bits */
    private static byte[] rotate(byte[] in, int r) {
        int n = in.length * 8;
        r %= n;
        byte[] out = new byte[in.length];
        for (int i = 0; i < n; i++) {
            int v = ((in[i/8] & 0xFF) << (i%8)) & 0xFF;
            int bit = ( (in[((i + r)%n)/8] & 0xFF) >> (((i+r)%8)) ) & 1;
            out[i/8] |= bit << (i%8);
        }
        return out;
    }

    /** 按大端将 48-bit SQN 转为 6 字节数组 */
    private static byte[] longToBytes(long v) {
        byte[] b = new byte[6];
        for (int i = 5; i >= 0; i--) {
            b[i] = (byte)(v & 0xFF);
            v >>>= 8;
        }
        return b;
    }

    private static byte[] xor(byte[] a, byte[] b) {
        byte[] c = new byte[a.length];
        for (int i = 0; i < a.length; i++) c[i] = (byte)(a[i] ^ b[i]);
        return c;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a,0,c,0,a.length);
        System.arraycopy(b,0,c,a.length,b.length);
        return c;
    }

    /**
     * 根据外部下发的 RAND 和 SQN 重新生成 AuthVector，
     * 用于 UE 端校验 AUTN、计算 RES/CK/IK。
     */
    public AuthVector generateAuthVectorWithRand(byte[] RAND, long SQN) {
        byte[] temp = aesEncrypt(xor(RAND, OPc));
        temp = xor(temp, OPc);

        byte[] out4 = aesEncrypt(xor(rotate(temp, R4), OPc));
        byte[] AK = xor(Arrays.copyOf(out4, 6), Arrays.copyOf(OPc, 6));

        // AUTN = (SQN⊕AK) || MAC-A
        byte[] sqnXorAk = xor(longToBytes(SQN), AK);
        byte[] in1       = xor(rotate(temp, R1), OPc);
        byte[] out1      = aesEncrypt(in1);
        out1 = xor(out1, OPc);
        byte[] MAC_A     = Arrays.copyOf(out1, 8);
        byte[] AUTN      = concat(sqnXorAk, MAC_A);

        byte[] out2 = aesEncrypt(xor(rotate(temp, R2), OPc));
        byte[] XRES = xor(Arrays.copyOf(out2, 8), Arrays.copyOf(OPc, 8));

        byte[] out3 = aesEncrypt(xor(rotate(temp, R3), OPc));
        byte[] CK   = xor(Arrays.copyOf(out3, 16), Arrays.copyOf(OPc, 16));

        byte[] out5 = aesEncrypt(xor(temp, OPc));
        out5 = xor(out5, OPc);
        byte[] IK   = Arrays.copyOf(out5, 16);

        return new AuthVector(RAND, AUTN, XRES, CK, IK, AK);
    }
    /**
     * 从 RAND + AUTN 中恢复出网络端的 SQN。
     * 这里内部计算 AK，再用 (AUTN[0..5] xor AK) → SQN。
     */
    public long extractSqn(byte[] RAND, byte[] AUTN) {
        // 1) temp = AES(RAND⊕OPc) ⊕ OPc
        byte[] temp = aesEncrypt(xor(RAND, OPc));
        temp = xor(temp, OPc);
        // 2) out4 = AES(ROT(temp,R4)⊕OPc) ⊕ OPc → 用来算 AK
        byte[] out4 = aesEncrypt(xor(rotate(temp, R4), OPc));
        byte[] AK = xor(Arrays.copyOf(out4,6), Arrays.copyOf(OPc,6));
        // 3) SQN⊕AK = AUTN[0..5]
        byte[] sqnXorAk = Arrays.copyOf(AUTN, 6);
        byte[] sqnBytes  = xor(sqnXorAk, AK);
        return bytesToLong(sqnBytes);
    }
    // —— 辅助：6 字节大端转 long ——
    private long bytesToLong(byte[] b6) {
        long v = 0;
        for (int i = 0; i < 6; i++) {
            v = (v << 8) | (b6[i] & 0xFF);
        }
        return v;
    }

    /** 生成 AUTS，用于 NAS Sync Failure */
    public byte[] generateAUTS(long sqn) {
        // AK 前 6 字节
        byte[] ak = generateAuthVector(sqn).AK;
        // SQN⊕AK
        byte[] sqnXorAk = xor(longToBytes(sqn), ak);
        // MAC-S ：简化为 f1 输出的前 8 字节
        // MAC-S = first8( AES(K, ROT(temp,R1)⊕OPc) ⊕ OPc )
        byte[] temp = aesEncrypt(xor(generateAuthVector(sqn).RAND, OPc));
        byte[] out1 = aesEncrypt(xor(rotate(temp, R1), OPc));
        byte[] macS  = Arrays.copyOf(xor(out1, OPc), 8);

        return concat(sqnXorAk, macS);  // 共 6+8 = 14 字节
    }

}
