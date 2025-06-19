package entity.gNB.Ngap;

import entity.CoreNetworkEnvironment;

import java.util.function.Consumer;

public class NgapLayer {
    private final CoreNetworkEnvironment coreEnv;
    private Consumer<byte[]> sendToPhy;

    public NgapLayer(CoreNetworkEnvironment coreEnv) {
        this.coreEnv = coreEnv;
    }

    public void registerSendHandler(Consumer<byte[]> handler) {
        this.sendToPhy = handler;
    }

    public void onPhyMessage(byte[] nasPayload) {
//        System.out.println("gNB NGAP: 收到上行 NAS, 转发到 AMF");
//        byte[] resp = coreEnv.processNasFromGnb(nasPayload);
//        sendToPhy.accept(resp);
        System.out.println("gNB NGAP: 收到上行 NAS, 转发到 AMF");
        // 这里需要将消息封装为InitialUEMessage
        byte[] initialMsg = new byte[nasPayload.length + 2];
        initialMsg[0] = 0x00; // InitialUEMessage标识
        initialMsg[1] = (byte) nasPayload.length; // NAS长度
        System.arraycopy(nasPayload, 0, initialMsg, 2, nasPayload.length);

        byte[] resp = coreEnv.processNasFromGnb(initialMsg);  //将封装的InitialUEMessage发送到AMF
        sendToPhy.accept(resp);  //将AMF返回的回复发回给UE
    }

    public void sendDownlinkNas(byte[] nasPayload) {
        sendToPhy.accept(nasPayload);
    }
}


