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
 * 并展示弱随机导致的 SUCI 公钥重复问题。
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

    public void runSuciRandomnessDemo(List<String> supis, RegistrationManager regMgr) throws Exception {
        printBanner("SUCI 随机性对比仿真开始");

        Map<String, String> suciMap = new LinkedHashMap<>();

        for (int idx = 0; idx < supis.size(); idx++) {
            String supi = supis.get(idx);
            boolean weakRandom = (idx % 2 == 0);

            PhysicalChannel ue2gnb = new PhysicalChannel();
            PhysicalChannel gnb2ue = new PhysicalChannel();
            gnb.addUeConnection(ue2gnb, gnb2ue);

            // === 使用 ECIES 生成 SUCI ===
            ECIESEncryptor encryptor = new ECIESEncryptor(weakRandom);
            String suci = encryptor.generateSUCI(supi);
            suciMap.put(supi, suci);

            // 打印 UE SUCI 信息
            System.out.println("------------------------------------------------------------");
            System.out.println("[UE-" + idx + "] SUCI 生成阶段：");
            System.out.println("   IMSI = " + supi);
            System.out.println("   SUCI = " + suci);
            if (weakRandom)
                System.out.println("使用弱随机源（低熵），R 部分可能重复，匿名性风险存在！");
            else
                System.out.println("使用强随机源（高熵），SUCI 唯一且不可预测。");
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

        // === 模拟攻击者对 SUCI 公钥前缀的分析 ===
        Thread.sleep(2000);
        analyzePartialReuse(suciMap);

        Thread.sleep(3000);
        printBanner("SUCI 随机性对比仿真结束");
    }

    private void analyzePartialReuse(Map<String, String> suciMap) {
        System.out.println("\n====================== 弱随机 SUCI 分析阶段 ======================");
        Map<String, List<String>> prefixGroups = new HashMap<>();

        // 取 SUCI 前 20 字节作为“公钥前缀”
        for (Map.Entry<String, String> e : suciMap.entrySet()) {
            String suci = e.getValue();
            String prefix = suci.substring(0, 20);
            prefixGroups.computeIfAbsent(prefix, k -> new ArrayList<>()).add(e.getKey());
        }

        boolean foundReuse = false;
        for (var entry : prefixGroups.entrySet()) {
            if (entry.getValue().size() > 1) {
                foundReuse = true;
                System.out.println("检测到 SUCI 公钥前缀重复 → 这些UE可能属于同一随机源：");
                for (String supi : entry.getValue())
                    System.out.println("   ↳ " + supi);
            }
        }

        if (!foundReuse)
            System.out.println("未检测到重复前缀，系统随机性良好。");
        else
            System.out.println("弱随机导致部分 UE 的 SUCI 公钥前缀相同，匿名性丧失。");

        System.out.println("=================================================================");
    }

    private static void printBanner(String msg) {
        System.out.println("\n============================================================");
        System.out.println(msg);
        System.out.println("============================================================");
    }
}
