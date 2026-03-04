package entity.suciattack;

import entity.CoreNetworkEnvironment;
import entity.Gnb;
import entity.gNB.Phy.PhysicalChannel;
import entity.gNB.Phy.PhysicalLayer;
import entity.Ue.register.RegistrationManager;
import entity.UEs;

import java.util.*;

/**
 * SUCI 随机性攻击模拟器
 * 模拟强随机与弱随机 UE 的注册流程，
 * 并返回弱随机 UE 集合用于后续攻击。
 */
public class SuciAttackSimulator {

    private final CoreNetworkEnvironment core;
    private final Gnb gnb;
    private final PhysicalLayer userPlane;

    public SuciAttackSimulator(CoreNetworkEnvironment core, Gnb gnb, PhysicalLayer userPlane) {
        this.core = core;
        this.gnb = gnb;
        this.userPlane = userPlane;
    }

    /**
     * 运行 SUCI 随机性对比仿真
     * @return 弱随机 UE 的 IMSI 集合
     */
    public Set<String> runSuciRandomnessDemo(List<String> supis,
                                             RegistrationManager regMgr) throws Exception {

        printBanner("SUCI 随机性对比仿真开始");

        Set<String> weakUes = new HashSet<>();

        for (int idx = 0; idx < supis.size(); idx++) {

            String supi = supis.get(idx);

            // 偶数索引作为弱随机 UE
            boolean weakRandom = (idx % 2 == 0);

            if (weakRandom) {
                weakUes.add(supi);   // 直接记录弱随机 UE
            }

            PhysicalChannel ue2gnb = new PhysicalChannel();
            PhysicalChannel gnb2ue = new PhysicalChannel();
            gnb.addUeConnection(ue2gnb, gnb2ue);

            // === 使用 ECIES 生成 SUCI ===
            ECIESEncryptor encryptor = new ECIESEncryptor(weakRandom);
            String suci = encryptor.generateSUCI(supi);

            // 打印 UE SUCI 信息
            System.out.println("------------------------------------------------------------");
            System.out.println("[UE-" + idx + "] SUCI 生成阶段：");
            System.out.println("   IMSI = " + supi);
            System.out.println("   SUCI = " + suci);

            if (weakRandom)
                System.out.println("使用弱随机源（攻击目标）");
            else
                System.out.println("使用强随机源（正常UE）");

            System.out.println("------------------------------------------------------------");

            // 启动注册流程
            UEs ue = new UEs(
                    supi,
                    core.getSubscriberKey(),
                    core.getOpVariant(),
                    gnb2ue, ue2gnb,
                    userPlane
            );
            ue.registerRegistrationListener(regMgr);
            ue.start();

            Thread.sleep(800);
        }

        Thread.sleep(3000);
        printBanner("SUCI 随机性对比仿真结束");

        return weakUes;
    }

    private static void printBanner(String msg) {
        System.out.println("\n============================================================");
        System.out.println(msg);
        System.out.println("============================================================");
    }
}