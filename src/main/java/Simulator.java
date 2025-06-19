import entity.CoreNetworkEnvironment;
import entity.Gnb;
import entity.UEs;
import entity.gNB.Phy.PhysicalChannel;
import entity.gNB.Phy.PhysicalLayer;

public class Simulator {

    public static void main(String[] args) throws InterruptedException {
        // —— 1. 创建控制面上下行两条信道 ——
        // 上行：UE → gNB
        PhysicalChannel ue2gnb = new PhysicalChannel();
        // 下行：gNB → UE
        PhysicalChannel gnb2ue = new PhysicalChannel();

        // —— 2. 创建用户面信道（DRB） ——
        PhysicalLayer userPlane = new PhysicalLayer();

        // —— 3. 创建核心网环境 ——
        CoreNetworkEnvironment core = new CoreNetworkEnvironment();

        // —— 4. 初始化 gNB ——
        // 把 “ue2gnb” 传给 gNB 作为它的 上行 控制面 channel，
        // 把 “gnb2ue” 传给 gNB 作为它的 下行 控制面 channel
        Gnb gnb = new Gnb(core, ue2gnb, gnb2ue, userPlane);
        gnb.start();

        // —— 5. 初始化 UE ——
        byte[] masterKey = new byte[16];
        for (int i = 0; i < masterKey.length; i++) masterKey[i] = 0x01;
        String supi = "imsi-001010123456789";

        // 注意顺序：第一个控制面 channel 一定是 “下行” (gnb2ue)，
        // 第二个一定是 “上行” (ue2gnb)。
        UEs ue = new UEs(supi, masterKey,
                /* downControl = */ gnb2ue,
                /* upControl   = */ ue2gnb,
                userPlane);
        ue.start();

        // —— 6. 等待仿真结束 ——
        Thread.sleep(8000);
        System.out.println("=== 仿真结束 ===");
    }
}
