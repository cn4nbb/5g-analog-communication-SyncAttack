package entity.Ue.state;

public class StateMachine {
    //RRC/NAS状态机
// RRC 状态
    public enum RrcState {
        IDLE,
        CONNECTING,
        CONNECTED
    }

    // NAS 注册状态
    public enum NasState {
        UNREGISTERED,
        REGISTRATION_PENDING,
        REGISTERED,
        SECURITY_ACTIVE
    }

    private RrcState rrcState;
    private NasState nasState;

    public StateMachine() {
        this.rrcState = RrcState.IDLE;
        this.nasState = NasState.UNREGISTERED;
        logStates();
    }

    // ---------- RRC 状态转换 ----------

    public void toRrcConnecting() {
        if (rrcState == RrcState.IDLE) {
            rrcState = RrcState.CONNECTING;
            System.out.println("SM: RRC -> CONNECTING");
        } else {
            System.err.println("SM: 无法从 " + rrcState + " 进入 CONNECTING");
        }
        logStates();
    }

    public void toRrcConnected() {
        if (rrcState == RrcState.CONNECTING) {
            rrcState = RrcState.CONNECTED;
            System.out.println("SM: RRC -> CONNECTED");
        } else {
            System.err.println("SM: 无法从 " + rrcState + " 进入 CONNECTED");
        }
        logStates();
    }

    public void toRrcIdle() {
        rrcState = RrcState.IDLE;
        System.out.println("SM: RRC -> IDLE");
        logStates();
    }

    // ---------- NAS 状态转换 ----------

    public void toRegistrationPending() {
        if (nasState == NasState.UNREGISTERED) {
            nasState = NasState.REGISTRATION_PENDING;
            System.out.println("SM: NAS -> REGISTRATION_PENDING");
        } else {
            System.err.println("SM: 无法从 " + nasState + " 进入 REGISTRATION_PENDING");
        }
        logStates();
    }

    public void toRegistered() {
        if (nasState == NasState.REGISTRATION_PENDING) {
            nasState = NasState.REGISTERED;
            System.out.println("SM: NAS -> REGISTERED");
        } else {
            System.err.println("SM: 无法从 " + nasState + " 进入 REGISTERED");
        }
        logStates();
    }

    public void toSecurityActive() {
        // 允许从 REGISTERED 状态转换到 SECURITY_ACTIVE
        if (nasState == NasState.REGISTERED) {
            nasState = NasState.SECURITY_ACTIVE;
            System.out.println("SM: NAS -> SECURITY_ACTIVE");
        } else {
            System.err.println("SM: 无法从 " + nasState + " 进入 SECURITY_ACTIVE");
        }
        logStates();
    }

    public RrcState getRrcState() {
        return rrcState;
    }

    public NasState getNasState() {
        return nasState;
    }

    private void logStates() {
        System.out.println("SM: Current States => RRC=" + rrcState + ", NAS=" + nasState);
    }
}
