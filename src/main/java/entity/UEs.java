package entity;
import entity.Ue.nas.NasLayer;
import entity.Ue.ngap.NgapLayer;

import entity.Ue.plmn.PlmnSelector;
import entity.Ue.rrc.RrcLayer;
import entity.Ue.security.SecurityContext;
import entity.Ue.state.StateMachine;
import entity.Ue.suci.SuciGenerator;
import entity.Ue.timer.TimerManager;
import entity.gNB.Phy.PhysicalChannel;
import entity.gNB.Phy.PhysicalLayer;


/**
 * UE类
 */
public class UEs {
    private final String supi;
    private final SuciGenerator    suciGenerator;
    private final SecurityContext  securityContext;
    private final StateMachine     stateMachine;
    private final TimerManager     timerManager;
    private final PlmnSelector     plmnSelector;
    private final NasLayer         nasLayer;
    private final RrcLayer         rrcLayer;
    private final NgapLayer        ngapLayer;
    private final PhysicalChannel  upControl;    // UE -> gNB
    private final PhysicalChannel  downControl;  // gNB -> UE

    public UEs(String supi,
               byte[] masterKey,
               PhysicalChannel downControl,
               PhysicalChannel upControl,
               PhysicalLayer userPhy) {
        this.supi    = supi;
        this.upControl   = upControl;
        this.downControl = downControl;

        this.suciGenerator  = new SuciGenerator(supi);
        this.securityContext= new SecurityContext(masterKey);
        this.stateMachine   = new StateMachine();
        this.timerManager   = new TimerManager();
        this.plmnSelector   = new PlmnSelector();
        this.nasLayer       = new NasLayer(securityContext, stateMachine, timerManager);
        this.rrcLayer       = new RrcLayer(timerManager);
        this.ngapLayer      = new NgapLayer();

        // RRC：下行到 UE
        downControl.registerToRrc(rrcLayer::onPhyMessage);
        // RRC：UE 上行到 gNB
        rrcLayer.registerSendHandler(msg -> upControl.sendToRrc(msg));

        // NGAP：下行到 UE
        downControl.registerToNgap(ngapLayer::onDownlinkNas);
        // NGAP：UE 上行到 gNB
        ngapLayer.registerSender(msg -> upControl.sendToNgap(msg));

        // NAS 处理
        ngapLayer.registerNasCallback(nasLayer::handleIncoming);
        nasLayer.registerCallback(this::onNasEvent);

        // RRC 完成后触发 NAS 注册
        rrcLayer.registerEventCallback(evt -> {
            if (evt == RrcLayer.RrcEvent.RRC_SETUP_COMPLETE) {
                onRrcConnected();
            }
        });
    }

    public void start() {
        System.out.println("=== UE 启动 ===");
        stateMachine.toRrcConnecting();
        rrcLayer.initiateRrcSetup();
    }

    private void onRrcConnected() {
//        System.out.println("UE: RRC 已连接，发送 NAS 注册请求");
//        stateMachine.toRrcConnected();
//        stateMachine.toRegistrationPending();
//        byte[] suci = suciGenerator.generate();
//        byte[] regReq = nasLayer.buildRegistrationRequest(suci);
//        ngapLayer.sendUplinkNas(regReq);
//        timerManager.startTimer(TimerManager.T3560);
        System.out.println("UE: RRC 已连接，发送 NAS 注册请求");
        stateMachine.toRrcConnected();
        stateMachine.toRegistrationPending();
        byte[] suci = suciGenerator.generate();
        byte[] regReq = nasLayer.buildRegistrationRequest(suci);
        ngapLayer.sendUplinkNas(regReq);
        timerManager.startTimer(TimerManager.T3560); // 启动NAS定时器
    }

    private void onNasEvent(NasLayer.Event event) {
        switch (event.getType()) {
            case AUTHENTICATION_REQUEST:
                System.out.println("UE: 处理 Authentication Request");
                byte[] authRes = securityContext.computeAuthenticationResponse(event.getRand());
                byte[] authRespMsg = nasLayer.buildAuthResponse(authRes);
                ngapLayer.sendUplinkNas(authRespMsg);
                timerManager.startTimer(TimerManager.T3561); // 启动认证响应定时器
                break;
            case SECURITY_MODE_COMMAND:
//                System.out.println("UE: 处理 Security Mode Command");
//                // 设置安全头并发送完成响应
//                securityContext.createNasSecurityHeader(nasLayer.getCurrentSequence(), true);
//                byte[] secModeComplete = nasLayer.buildSecurityModeComplete();
//                ngapLayer.sendUplinkNas(secModeComplete);
//                stateMachine.toSecurityActive();
//                break;
                System.out.println("UE: 处理 Security Mode Command");
                // 激活安全上下文
                securityContext.handleSecurityModeCommand(event.getSecurityHeader());
                byte[] secModeComplete = nasLayer.buildSecurityModeComplete();
                ngapLayer.sendUplinkNas(secModeComplete);
                // 注意：这里不改变状态，等待Registration Accept
                break;
            case REGISTRATION_ACCEPT:
//                System.out.println("UE: 处理 Registration Accept");
//                stateMachine.toRegistered();
//                System.out.println("UE: 注册成功完成");
//                break;
                System.out.println("UE: 处理 Registration Accept");
                stateMachine.toRegistered();
                stateMachine.toSecurityActive(); // 安全激活
                System.out.println("UE: 注册成功完成");
                break;
            case REGISTRATION_REJECT:
                System.out.println("UE: 处理 Registration Reject");
                plmnSelector.handleReject(event.getRejectCause());
                break;
        }
    }


}
