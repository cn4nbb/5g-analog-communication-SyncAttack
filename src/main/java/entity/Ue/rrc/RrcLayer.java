package entity.Ue.rrc;

import entity.Ue.timer.TimerManager;

import java.util.function.Consumer;

public class RrcLayer {
    public enum RrcEvent { RRC_SETUP_COMPLETE, RRC_RELEASE }

    // 添加状态跟踪
    private enum RrcState { IDLE, CONNECTING, CONNECTED }
    private RrcState state = RrcState.IDLE;

    private final TimerManager timerManager;
    private Consumer<byte[]> sendToPhysical;
    private Consumer<RrcEvent> eventCallback;

    public RrcLayer(TimerManager timerManager) {
        this.timerManager = timerManager;
    }

    public void registerSendHandler(Consumer<byte[]> handler) {
        this.sendToPhysical = handler;
    }

    public void registerEventCallback(Consumer<RrcEvent> callback) {
        this.eventCallback = callback;
    }

    /**
     * 发起 RRC 连接请求
     */
    public void initiateRrcSetup() {
        // TODO: 构造 RRC Setup Request ASN.1/PER 编码
//        System.out.println("UE RRC: 发送 RRC Setup Request");
//        // 通过物理层发送
//        sendToPhysical.accept(new byte[]{ /* encoded */ });
//        timerManager.startTimer(TimerManager.T3560);
        if (state == RrcState.IDLE) {
            state = RrcState.CONNECTING;
            System.out.println("UE RRC: 发送 RRC Setup Request");
            sendToPhysical.accept(new byte[]{0x10}); // 0x10 = RRC Setup Request
            timerManager.startTimer(TimerManager.T3560);
        }
    }

    /**
     * 接收物理层的 RRC 消息
     */
    public void onPhyMessage(byte[] data) {
        // 简化：假设收到 Setup Complete
//        System.out.println("UE RRC: 收到 RRC Setup Complete");
//        if (eventCallback != null) eventCallback.accept(RrcEvent.RRC_SETUP_COMPLETE);
        if (data.length > 0) {
            switch (data[0]) {
                case 0x11: // RRC Setup
                    System.out.println("UE RRC: 收到 RRC Setup");
                    sendSetupComplete();
                    break;
            }
        }
    }
    /**
     * 发送 RRC Setup Complete
     */
    private void sendSetupComplete() {
        if (state == RrcState.CONNECTING) {
            state = RrcState.CONNECTED;
            System.out.println("UE RRC: 发送 RRC Setup Complete");
            sendToPhysical.accept(new byte[]{0x12}); // 0x12 = RRC Setup Complete
            timerManager.cancelTimer(TimerManager.T3560); // 取消定时器

            // 触发事件回调
            if (eventCallback != null) {
                eventCallback.accept(RrcEvent.RRC_SETUP_COMPLETE);
            }
        }
    }
}
