package entity.gNB.Rrc;

import java.util.function.Consumer;

public class RrcLayer {
    public enum RrcEvent { SETUP_REQUEST, SETUP_COMPLETE }
    private Consumer<byte[]> sendToPhy;
    private Consumer<RrcEvent> eventCallback;

    private enum RrcState { IDLE, CONNECTING, CONNECTED }
    private RrcState state = RrcState.IDLE;
    public RrcLayer() {}

    public void registerSendHandler(Consumer<byte[]> handler) {
        this.sendToPhy = handler;
    }

    public void registerEventCallback(Consumer<RrcEvent> callback) {
        this.eventCallback = callback;
    }

    /**
     * 接收物理层上行 RRC 数据
     */
    public void onPhyMessage(byte[] data) {
        // 解析第一字节判断是否 RRCSetupRequest
//        if (data.length > 0 && data[0] == 0x10) { // 假设 0x10 = RRCSetupRequest
//            System.out.println("gNB RRC: 收到 RRC Setup Request");
//            if (eventCallback != null) eventCallback.accept(RrcEvent.SETUP_REQUEST);
//        }
        if (data.length > 0) {
            switch (data[0]) {
                case 0x10: // RRC Setup Request
                    System.out.println("gNB RRC: receive RRC Setup Request");
                    if (state == RrcState.IDLE) {
                        state = RrcState.CONNECTING;
                        sendSetup(); // 发送 RRC Setup
                        if (eventCallback != null) {
                            eventCallback.accept(RrcEvent.SETUP_REQUEST);
                        }
                    }
                    break;
                case 0x12: // RRC Setup Complete
                    System.out.println("gNB RRC: receive RRC Setup Complete");
                    if (state == RrcState.CONNECTING) {
                        state = RrcState.CONNECTED;
                        if (eventCallback != null) {
                            eventCallback.accept(RrcEvent.SETUP_COMPLETE);
                        }
                    }
                    break;
            }
        }
    }
    // 发送RRC Setup消息 (不是Setup Complete!)
    public void sendSetup() {
        System.out.println("gNB RRC: send RRC Setup");
        byte[] msg = new byte[]{0x11}; // 假设0x11=RRC Setup
        sendToPhy.accept(msg);
    }

    /**
     * 发送 RRC Setup Complete
     */
    public void sendSetupComplete() {
        byte[] msg = new byte[]{0x11}; // 假设 0x11 = RRCSetupComplete
        System.out.println("gNB RRC: send RRC Setup Complete");
        sendToPhy.accept(msg);
    }
}
