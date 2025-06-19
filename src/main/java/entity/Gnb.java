package entity;

import entity.gNB.Mac.MacLayer;
import entity.gNB.Ngap.NgapLayer;
import entity.gNB.Pdcp.PdcpLayer;
import entity.gNB.Phy.PhysicalChannel;
import entity.gNB.Phy.PhysicalLayer;
import entity.gNB.Rlc.RlcLayer;
import entity.gNB.Rrc.RrcLayer;

public class Gnb {
    private final RrcLayer         rrc;
    private final NgapLayer        ngap;
    private final PhysicalChannel  upControl;    // UE -> gNB
    private final PhysicalChannel  downControl;  // gNB -> UE

    private final PhysicalLayer    userPhy;
    private final MacLayer         macData;
    private final RlcLayer         rlcData;
    private final PdcpLayer        pdcpData;

    public Gnb(CoreNetworkEnvironment coreNetwork,
               PhysicalChannel upControl,
               PhysicalChannel downControl,
               PhysicalLayer userPhy) {
        this.upControl   = upControl;
        this.downControl = downControl;

        // 控制面
        this.rrc  = new RrcLayer();
        this.ngap = new NgapLayer(coreNetwork);

        // RRC：上行到 gNB
        upControl.registerToRrc(rrc::onPhyMessage);
        // RRC：gNB 下行到 UE
        rrc.registerSendHandler(msg -> downControl.sendToRrc(msg));

        // NGAP：上行到 gNB
        upControl.registerToNgap(ngap::onPhyMessage);
        // NGAP：gNB 下行到 UE
        ngap.registerSendHandler(msg -> downControl.sendToNgap(msg));

        // 用户面
        this.userPhy = userPhy;
        this.macData = new MacLayer(userPhy::sendUplink);
        this.rlcData = new RlcLayer(macData::sendUplink);
        this.pdcpData = new PdcpLayer(pdu -> rlcData.sendUplink(pdu), false);

        userPhy.registerGnbUplink(macData::onPhyUplink);
        userPhy.registerGnbDownlink(macData::onPhyDownlink);

        macData.registerUpperLayerHandler(macPdu ->
                rlcData.onMacDownlink(macPdu, pdcpPdu ->
                        pdcpData.onRlcDownlink(pdcpPdu, this::handleUserData)
                )
        );
    }

    public void start() {
//        System.out.println("gNB: 控制面启动，等待 UE 发起 RRC Setup Request");
//        rrc.registerEventCallback(evt -> {
//            if (evt == RrcLayer.RrcEvent.SETUP_REQUEST) {
//                System.out.println("gNB: 收到 RRC Setup Request，发回 Setup Complete");
//                rrc.sendSetupComplete();
//            }
//        });
        System.out.println("gNB: 控制面启动，等待 UE 发起 RRC Setup Request");
        rrc.registerEventCallback(evt -> {
            if (evt == RrcLayer.RrcEvent.SETUP_REQUEST) {
                System.out.println("gNB: 处理 RRC Setup Request");
                // RRC Setup 已在 RrcLayer 中发送
            } else if (evt == RrcLayer.RrcEvent.SETUP_COMPLETE) {
                System.out.println("gNB: RRC连接建立完成");
                // 后续可以在这里处理NAS消息转发等
            }
        });

    }

    private void handleUserData(byte[] payload) {
        System.out.println("gNB App: 收到用户面数据 = " + new String(payload));
    }
}
