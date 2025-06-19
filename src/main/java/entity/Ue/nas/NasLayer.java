package entity.Ue.nas;

import entity.Ue.security.SecurityContext;
import entity.Ue.state.StateMachine;
import entity.Ue.timer.TimerManager;

import java.util.Arrays;
import java.util.function.Consumer;

public class NasLayer {

    //NAS消息封装、解析，ASN.1/PER，安全头
    public enum EventType {
        AUTHENTICATION_REQUEST,
        SECURITY_MODE_COMMAND,
        REGISTRATION_ACCEPT,
        REGISTRATION_REJECT
    }

    public static class Event {
        private final EventType type;
        private final byte[] rand;
        private final byte[] securityHeader;
        private final int rejectCause;

        public Event(EventType type, byte[] rand, byte[] securityHeader, int rejectCause) {
            this.type = type;
            this.rand = rand;
            this.securityHeader = securityHeader;
            this.rejectCause = rejectCause;
        }

        public EventType getType() { return type; }
        public byte[] getRand() { return rand; }
        public byte[] getSecurityHeader() { return securityHeader; }
        public int getRejectCause() { return rejectCause; }
    }

    private final SecurityContext securityContext;
    private final StateMachine stateMachine;
    private final TimerManager timerManager;
    private Consumer<Event> callback;
    private int nasSequence = 0;

    public NasLayer(SecurityContext securityContext, StateMachine stateMachine, TimerManager timerManager) {
        this.securityContext = securityContext;
        this.stateMachine = stateMachine;
        this.timerManager = timerManager;
    }

    /** 注册上层回调 */
    public void registerCallback(Consumer<Event> callback) {
        this.callback = callback;
    }

    /**
     * 构建 Registration Request NAS 消息
     */
    public byte[] buildRegistrationRequest(byte[] suci) {
        byte[] header = new byte[]{0x01, (byte) (nasSequence++)};
        byte[] message = concat(header, suci);
        System.out.println("UE NAS: 构建 Registration Request, seq=" + (nasSequence - 1));
        return message;
    }

    public byte[] buildAuthResponse(byte[] authRes) {
//        byte[] secHeader = securityContext.createNasSecurityHeader(nasSequence++, false);
//        byte[] protectedMsg = securityContext.protectNas(secHeader, authRes);
//        System.out.println("UE NAS: 构建 Authentication Response, seq=" + (nasSequence - 1));
//        return protectedMsg;
        byte[] message = new byte[1 + authRes.length];
        message[0] = 0x03; // Authentication Response 类型
        System.arraycopy(authRes, 0, message, 1, authRes.length);
        System.out.println("UE NAS: 构建 Authentication Response, seq=" + (nasSequence++));
        return message;
    }

    public byte[] buildSecurityModeComplete() {
//        byte[] secHeader = securityContext.createNasSecurityHeader(nasSequence++, true);
//        byte[] body = new byte[]{0x5E};
//        byte[] protectedMsg = securityContext.protectNas(secHeader, body);
//        System.out.println("UE NAS: 构建 Security Mode Complete, seq=" + (nasSequence - 1));
//        return protectedMsg;
//        byte[] secHeader = securityContext.createNasSecurityHeader(nasSequence++, true);
//        byte[] body = new byte[]{0x5E}; // Security Mode Complete 类型
//        byte[] protectedMsg = securityContext.protectNas(secHeader, body);
//        System.out.println("UE NAS: 构建 Security Mode Complete, seq=" + (nasSequence - 1));
//        return protectedMsg;
        if (securityContext.isSecurityActivated()) {
            byte[] secHeader = securityContext.createNasSecurityHeader(nasSequence++, true);
            byte[] body = new byte[]{0x5E}; // Security Mode Complete 类型
            byte[] protectedMsg = securityContext.protectNas(secHeader, body);
            System.out.println("UE NAS: 构建 Security Mode Complete(安全保护), seq=" + (nasSequence - 1));
            return protectedMsg;
        } else {
            // 安全激活前发送未保护的消息
            System.out.println("UE NAS: 构建 Security Mode Complete(未保护), seq=" + nasSequence);
            return new byte[]{0x5E};
        }
    }

    /**
     * 处理接收到的 NAS 消息字节
     */
    public void handleIncoming(byte[] rawPayload) {
        timerManager.stopActiveTimers();
        byte msgType = rawPayload[0];

        switch (msgType) {
            case 0x02: // Authentication Request
                if (rawPayload.length >= 17) {
                    byte[] rand = Arrays.copyOfRange(rawPayload, 1, 17);
                    System.out.println("UE NAS: 收到 Authentication Request");
                    timerManager.cancelTimer(TimerManager.T3560); // 取消注册定时器
                    callback.accept(new Event(EventType.AUTHENTICATION_REQUEST, rand, null, 0));
                }
                break;
            case (byte) 0x5C: // Security Mode Command
                System.out.println("UE NAS: 收到 Security Mode Command");
                if (rawPayload.length >= 6) {
                    byte[] secHeader = Arrays.copyOfRange(rawPayload, 0, 6);
                    callback.accept(new Event(EventType.SECURITY_MODE_COMMAND, null, secHeader, 0));
                }
                break;
            case 0x41: // Registration Accept
                System.out.println("UE NAS: 收到 Registration Accept");
                timerManager.cancelTimer(TimerManager.T3561); // 取消安全模式定时器
                callback.accept(new Event(EventType.REGISTRATION_ACCEPT, null, null, 0));
                break;
            case 0x42: // Registration Reject
                if (rawPayload.length >= 2) {
                    int cause = rawPayload[1] & 0xFF;
                    System.out.println("UE NAS: 收到 Registration Reject, cause=" + cause);
                    callback.accept(new Event(EventType.REGISTRATION_REJECT, null, null, cause));
                }
                break;
            default:
                System.out.println("UE NAS: 未知消息类型: " + msgType);
        }
    }

    private byte[] concat(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
    public int getCurrentSequence() {
        return nasSequence;
    }
}
