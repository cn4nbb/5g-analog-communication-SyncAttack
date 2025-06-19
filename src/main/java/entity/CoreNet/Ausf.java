package entity.CoreNet;

public class Ausf {
    private final Udm udm;

    public Ausf(Udm udm) {
        this.udm = udm;
    }

    /**
     * 当 AMF 请求鉴权时调用
     */
    public byte[] authenticate(byte[] regReq) {
        System.out.println("AUSF: 生成 Authentication Request");
        // 从 UDM 获取认证数据
        byte[] kAutsf = udm.getAuthenticationKey();
        byte[] rand = udm.generateRand();
        byte[] authReq = new byte[1 + rand.length];
        authReq[0] = 0x02; // Authentication Request 类型
        System.arraycopy(rand, 0, authReq, 1, rand.length);
        return authReq;
    }
}
