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
    private final byte[] opVariant;
    private final byte[] subscriberKey;

    public CoreNetworkEnvironment() {
        byte[] subscriberKey = new byte[16];
        byte[] opVariant = new byte[16];
        new java.security.SecureRandom().nextBytes(subscriberKey);
        new java.security.SecureRandom().nextBytes(opVariant);
        // 初始化 UDM、AUSF 和 AMF
        this.subscriberKey = subscriberKey.clone();
        Udm udm = new Udm(subscriberKey, opVariant);
        Ausf ausf = new Ausf(udm);
        this.amf = new Amf(ausf, udm);
        this.opVariant = opVariant;
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
    public byte[] getOpVariant() {
        return opVariant.clone();
    }
    /** ← 新增：供 Simulator 使用 */
    public byte[] getSubscriberKey() {
        return subscriberKey.clone();
    }
}