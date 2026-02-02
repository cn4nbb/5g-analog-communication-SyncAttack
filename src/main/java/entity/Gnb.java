package entity;

import entity.gNB.Mac.MacLayer;
import entity.gNB.Ngap.NgapLayer;
import entity.gNB.Pdcp.PdcpLayer;
import entity.gNB.Phy.PhysicalChannel;
import entity.gNB.Phy.PhysicalLayer;
import entity.gNB.Rlc.RlcLayer;
import entity.gNB.Rrc.RrcLayer;

public class Gnb {
//    private final RrcLayer         rrc;
//    private final NgapLayer        ngap;
//    private final PhysicalChannel  upControl;    // UE -> gNB
//    private final PhysicalChannel  downControl;  // gNB -> UE

    private final PhysicalLayer    userPhy;
    private final MacLayer         macData;
    private final RlcLayer         rlcData;
    private final PdcpLayer        pdcpData;
    private final CoreNetworkEnvironment coreNetwork;

    public Gnb(CoreNetworkEnvironment coreNetwork,
//               PhysicalChannel upControl,
//               PhysicalChannel downControl,
               PhysicalLayer userPhy) {
//        this.upControl   = upControl;
//        this.downControl = downControl;

        // 控制面
//        this.rrc  = new RrcLayer();
//        this.ngap = new NgapLayer(coreNetwork);

        // RRC：上行到 gNB
        //upControl.registerToRrc(rrc::onPhyMessage);
        // RRC：gNB 下行到 UE
        //rrc.registerSendHandler(msg -> downControl.sendToRrc(msg));

        // NGAP：上行到 gNB
        //upControl.registerToNgap(ngap::onPhyMessage);
        // NGAP：gNB 下行到 UE
        //ngap.registerSendHandler(msg -> downControl.sendToNgap(msg));
        this.coreNetwork = coreNetwork;
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

    /**
     * 为一对控制面信道注册 RRC/NGAP，上行 ue2gnb, 下行 gnb2ue
     */
    public void addUeConnection(PhysicalChannel ue2gnb, PhysicalChannel gnb2ue) {
        //1、每个连接的RRC
        RrcLayer rrc = new RrcLayer();
        // RRC
        ue2gnb.registerToRrc(rrc::onPhyMessage);
        rrc.registerSendHandler(msg -> gnb2ue.sendToRrc(msg));
        // 可选日志
        rrc.registerEventCallback(evt -> {
            if (evt == RrcLayer.RrcEvent.SETUP_REQUEST) {
                System.out.println("gNB[" + ue2gnb.hashCode() + "]: receive RRC Setup Request");
            } else if (evt == RrcLayer.RrcEvent.SETUP_COMPLETE) {
                System.out.println("gNB[" + ue2gnb.hashCode() + "]: RRC connection completed");
            }
        });
        // 立即启动：监听上行 RRC Setup
        System.out.println("gNB: has added UE RRC connection " + ue2gnb.hashCode());
        //2、每个连接的 NGAP
        NgapLayer ngap = new NgapLayer(coreNetwork);
        ue2gnb.registerToNgap(ngap::onPhyMessage);
        ngap.registerSendHandler(msg -> gnb2ue.sendToNgap(msg));
        System.out.println("gNB: has added UE NGAP connection " + ue2gnb.hashCode());
    }


    private void handleUserData(byte[] payload) {
        System.out.println("gNB App: Received user profile data = " + new String(payload));
    }
}
