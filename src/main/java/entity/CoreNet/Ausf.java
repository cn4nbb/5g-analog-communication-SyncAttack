package entity.CoreNet;

public class Ausf {
    private final Udm udm;
    private final Milenage milenage;

    public Ausf(Udm udm) {
        this.udm = udm;

        this.milenage = new Milenage(udm.getAuthenticationKey(),udm.getOperatorVariant());
    }

    /**
     * 当 AMF 请求鉴权时调用
     */
    public byte[] authenticate() {
        long sqn = udm.getAndIncrementSqn();
        Milenage.AuthVector vec = milenage.generateAuthVector(sqn);

        // 拼装 NAS Authentication Request: [0x02|RAND|AUTN]
        byte[] rand = vec.RAND;
        byte[] autn = vec.AUTN;
        byte[] msg = new byte[1 + rand.length + autn.length];
        msg[0] = 0x02; // Authentication Request
        System.arraycopy(rand,0,msg,1,rand.length);
        System.arraycopy(autn,0,msg,1+rand.length,autn.length);
        System.out.println("AUSF: Generate complete Authentication Request");
        udm.storeExpectedXres(vec.XRES); // 存储预期的 XRES 以供 AMF 验证
        return msg;
    }
}
