package entity;

import entity.CoreNet.Amf;
import entity.CoreNet.Ausf;
import entity.CoreNet.Udm;

//public class CoreNetworkEnvironment {
//    private final Amf amf;
//    private final Ausf ausf;
//    private final Udm udm;
//
//    public CoreNetworkEnvironment() {
//        this.udm = new Udm();
//        this.ausf = new Ausf(udm);
//        this.amf = new Amf(ausf);
//    }
//
//    /**
//     * gNB 调用：接收 SUCI，走 AMF 注册流程，返回下行 NAS 消息
//     */
//    public byte[] processNasFromGnb(byte[] nasPayload) {
//        return amf.handleUplinkNas(nasPayload);
//    }
//}

public class CoreNetworkEnvironment {
    private final Amf amf;

    public CoreNetworkEnvironment() {
        Udm udm = new Udm();
        Ausf ausf = new Ausf(udm);
        this.amf = new Amf(ausf);
    }

    public byte[] processNasFromGnb(byte[] ngapMessage) {
        if (ngapMessage.length > 0 && ngapMessage[0] == 0x00) { // InitialUEMessage
            int nasLength = ngapMessage[1] & 0xFF;
            if (nasLength > 0 && ngapMessage.length >= 2 + nasLength) {
                byte[] nasPayload = new byte[nasLength];
                System.arraycopy(ngapMessage, 2, nasPayload, 0, nasLength);
                return amf.handleUplinkNas(nasPayload);
            }
        }
        return new byte[0];
    }
}