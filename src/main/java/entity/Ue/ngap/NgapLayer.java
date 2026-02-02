package entity.Ue.ngap;

import java.util.function.Consumer;

public class NgapLayer {
    public enum NgapMessageType { INITIAL_UE_MESSAGE, UPLINK_NAS_TRANSPORT, DOWNLINK_NAS_TRANSPORT }

    private Consumer<byte[]> sendToNetwork;
    private Consumer<byte[]> nasDeliveryCallback;

    public void registerSender(Consumer<byte[]> sender) {
        this.sendToNetwork = sender;
    }

    public void registerNasCallback(Consumer<byte[]> callback) {
        this.nasDeliveryCallback = callback;
    }

    public void sendInitialUeMessage(byte[] rrcPayload) {
        System.out.println("UE NGAP: send InitialUEMessage");
        // 打包并send
        sendToNetwork.accept(rrcPayload);
    }

    public void sendUplinkNas(byte[] nasPayload) {
        System.out.println("UE NGAP: send UplinkNASTransport");
        sendToNetwork.accept(nasPayload);
    }

    public void onDownlinkNas(byte[] ngapPayload) {
        System.out.println("UE NGAP: receive DownlinkNASTransport");
        if (nasDeliveryCallback != null) nasDeliveryCallback.accept(ngapPayload);
    }
}
