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
        private final byte[] autn;
        private final byte[] securityHeader;
        private final int rejectCause;

        public Event(EventType type, byte[] rand, byte[] autn, byte[] securityHeader,int rejectCause) {
            this.type = type;
            this.rand = rand;
            this.autn = autn;
            this.rejectCause = rejectCause;
            this.securityHeader = securityHeader;
        }

        public EventType getType() { return type; }
        public byte[] getRand() { return rand; }
        public byte[] getAutn() { return autn; }
        public int getRejectCause() { return rejectCause; }
        public byte[] getSecurityHeader() { return securityHeader; }
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
     * build Registration Request NAS 消息
     */
    public byte[] buildRegistrationRequest(byte[] suci) {
        byte[] header = new byte[]{0x01, (byte) (nasSequence++)};
        byte[] message = concat(header, suci);
        System.out.println("UE NAS: build Registration Request, seq=" + (nasSequence - 1));
        return message;
    }
    public byte[] buildAuthFailure(byte[] failMsg) {
        System.out.printf("UE NAS: build Authentication Failure, type=0x%02X%n", failMsg[0]);
        return failMsg;
    }

    public byte[] buildAuthResponse(byte[] authRes) {
        byte[] message = new byte[1 + authRes.length];
        message[0] = 0x03; // Authentication Response 类型
        System.arraycopy(authRes, 0, message, 1, authRes.length);
        System.out.println("UE NAS: build Authentication Response, seq=" + (nasSequence++));
        return message;
    }

    public byte[] buildSecurityModeComplete() {
        if (securityContext.isSecurityActivated()) {
            byte[] secHeader = securityContext.createNasSecurityHeader(nasSequence++, true);
            byte[] body = new byte[]{0x5E}; // Security Mode Complete 类型
            byte[] protectedMsg = securityContext.protectNas(secHeader, body);
            System.out.println("UE NAS: build Security Mode Complete(security protect), seq=" + (nasSequence - 1));
            return protectedMsg;
        } else {
            // 安全激活前发送未保护的消息
            System.out.println("UE NAS: build Security Mode Complete(unprotected), seq=" + nasSequence);
            return new byte[]{0x5E};
        }
    }

    /**
     * 处理接receive的 NAS 消息字节
     */
    public void handleIncoming(byte[] rawPayload) {
        timerManager.stopActiveTimers();
        byte msgType = rawPayload[0];

        switch (msgType) {
            case 0x02: // Authentication Request
                if (rawPayload.length >= 1 + 16 + 14) {
                    byte[] rand = Arrays.copyOfRange(rawPayload, 1, 1 + 16);
                    byte[] autn = Arrays.copyOfRange(rawPayload, 1 + 16, 1 + 16 + 14);
                    System.out.println("UE NAS: receive Authentication Request");
                    timerManager.cancelTimer(TimerManager.T3560);
                    callback.accept(new Event(EventType.AUTHENTICATION_REQUEST, rand, autn, null,0));
                }
                break;
            case (byte) 0x5C: // Security Mode Command
                System.out.println("UE NAS: receive Security Mode Command");
                if (rawPayload.length >= 6) {
                    byte[] secHeader = Arrays.copyOfRange(rawPayload, 0, 6);
                    callback.accept(new Event(EventType.SECURITY_MODE_COMMAND, null, null,secHeader, 0));
                }
                break;
            case 0x41: // Registration Accept
                System.out.println("UE NAS: receive Registration Accept");
                timerManager.cancelTimer(TimerManager.T3561); // 取消安全模式定时器
                callback.accept(new Event(EventType.REGISTRATION_ACCEPT, null, null,null, 0));
                break;
            case 0x42: // Registration Reject
                if (rawPayload.length >= 2) {
                    int cause = rawPayload[1] & 0xFF;
                    System.out.println("UE NAS: receive Registration Reject, cause=" + cause);
                    callback.accept(new Event(EventType.REGISTRATION_REJECT, null, null,null, cause));
                }
                break;
            default:
                System.out.println("UE NAS: Unknown message type: " + msgType);
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
