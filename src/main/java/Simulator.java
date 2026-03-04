import entity.*;
import entity.Ue.register.RegistrationManager;
import entity.gNB.Phy.PhysicalChannel;
import entity.gNB.Phy.PhysicalLayer;
import entity.suciattack.SuciAttackSimulator;
import entity.visual.BroadcastAttackDiagram;
import entity.visual.SuciAttackDiagram;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Simulator {

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            return;
        }

        String mode = args[0].toLowerCase();

        switch (mode) {
            case "broadcast":
                runBroadCastAttack();
                break;
            case "suci":
                runSUCIAttack();
                break;
            default:
                System.out.println("未知指令: " + mode);
                System.out.println("可用指令: broadcast | suci");
        }
    }

    /**
     * 运行广播重放攻击
     */
    public static void runBroadCastAttack() throws Exception{
        // === 日志重定向 ===
        File logFile = new File("log/simulation.log");
        PrintStream logStream = new PrintStream(new FileOutputStream(logFile, false), true,"UTF-8");
        PrintStream console = System.out;
        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                console.write(b);
                logStream.write(b);
            }
        }, true));

        BroadcastAttackDiagram.showDiagram();
        BroadcastAttackDiagram.redirectSystemOut(logStream);

        PhysicalLayer userPlane = new PhysicalLayer();

        CoreNetworkEnvironment core = new CoreNetworkEnvironment();

        Gnb gnb = new Gnb(core, userPlane);


        List<String> supis = List.of(
                "imsi-001010123456001",
                "imsi-001010123456002",
                "imsi-001010123456003");

        RegistrationManager regMgr = new RegistrationManager("registration_results.csv");
        // **在第一个 UE 的 gnb2ueChannel 上记录向量**
//        BroadcastAttackSimulator attacker = null;
//
//        for (int idx = 0; idx < supis.size(); idx++) {
//            String supi = supis.get(idx);
//
//            PhysicalChannel ue2gnb = new PhysicalChannel();
//            PhysicalChannel gnb2ue = new PhysicalChannel();
//            gnb.addUeConnection(ue2gnb, gnb2ue);
//
//            if (idx == 0) {
//                // 只在第一个 UE 的 downlink channel 上记录
//                attacker = new BroadcastAttackSimulator(gnb2ue);
//            }
//            // 将所有 UE 的 downlink channel 都加到“受害者”列表
//            if (attacker != null) {
//                attacker.addVictimChannel(gnb2ue);
//            }
//
//            // Instantiate and start UE
//            byte[] masterKey = core.getSubscriberKey();
//            byte[] opVariant = core.getOpVariant();
//            UEs ue = new UEs(supi, masterKey, opVariant, gnb2ue, ue2gnb, userPlane);
//            ue.registerRegistrationListener(regMgr);
//            ue.start();
//
//            Thread.sleep(500);  // 错开启动
//        }
//
//        // **最后启动广播攻击**
//        if (attacker != null) {
//            attacker.startAttack(1000, 2000);  // 1s 后开始，每2s重放到所有 UE
//        }

        {
            String supi0 = supis.get(0);
            PhysicalChannel ue2gnb0 = new PhysicalChannel();
            PhysicalChannel gnb2ue0 = new PhysicalChannel();
            gnb.addUeConnection(ue2gnb0, gnb2ue0);

            // 攻击器，记录第一个向量
            BroadcastAttackSimulator attacker = new BroadcastAttackSimulator(gnb2ue0);
            attacker.addVictimChannel(gnb2ue0);

            // 启动 UE0
            UEs ue0 = new UEs(
                    supi0,
                    core.getSubscriberKey(),
                    core.getOpVariant(),
                    gnb2ue0, ue2gnb0,
                    userPlane
            );
            ue0.registerRegistrationListener(regMgr);
            ue0.start();

            // 等待 3 秒：让 UE0 完成一次正常的 AKA 并触发记录
            Thread.sleep(3000);

            // --- 2) 启动广播攻击，重放给后续的 UE ---
            attacker.startAttack(0, 2000);

            // --- 3) 再依次启动其余 UE，将它们也加入“受害者”列表 ---
            for (int idx = 1; idx < supis.size(); idx++) {
                String supi = supis.get(idx);
                PhysicalChannel ue2gnb = new PhysicalChannel();
                PhysicalChannel gnb2ue = new PhysicalChannel();
                gnb.addUeConnection(ue2gnb, gnb2ue);

                // 把新 UE 的 downlink 信道加入 attacker
                attacker.addVictimChannel(gnb2ue);

                UEs ue = new UEs(
                        supi,
                        core.getSubscriberKey(),
                        core.getOpVariant(),
                        gnb2ue, ue2gnb,
                        userPlane
                );
                ue.registerRegistrationListener(regMgr);
                ue.start();

                Thread.sleep(500);  // 错开启动
            }
            // 等待所有流程走完
            Thread.sleep(10000);
            attacker.stopAttack();
        }
        // —— 6. 等待仿真结束 ——
        Thread.sleep(8000);
        System.out.println("=== Simulation ended ===");

        logStream.close();

        System.exit(0);
    }

    /**
     * 运行SUCI攻击
     */
    public static void runSUCIAttack() throws Exception {
        // === 输出与日志重定向 ===
        System.setProperty("file.encoding", "UTF-8");
        File logFile = new File("log/simulation.log");
        if (!logFile.getParentFile().exists()) logFile.getParentFile().mkdirs();
        PrintStream logStream = new PrintStream(new FileOutputStream(logFile, false), true, "UTF-8");
        PrintStream console = System.out;
        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                console.write(b);
                logStream.write(b);
            }
        }, true));

        // 显示SUCI攻击界面
        SuciAttackDiagram.showDiagram();
        SuciAttackDiagram.redirectSystemOut(logStream);

        // === 构建仿真环境 ===
        PhysicalLayer userPlane = new PhysicalLayer();
        CoreNetworkEnvironment core = new CoreNetworkEnvironment();
        Gnb gnb = new Gnb(core, userPlane);
        RegistrationManager regMgr = new RegistrationManager("registration_results.csv");

        // === UE 列表 ===
        List<String> supis = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            supis.add(String.format("imsi-001010123456%03d", i));
        }

        // === 执行SUCI攻击 ===
        SuciAttackSimulator attack = new SuciAttackSimulator(core, gnb, userPlane);
        Set<String> weakUes = attack.runSuciRandomnessDemo(supis, regMgr);

        // 将弱随机UE传入广播攻击
        runBroadCastAttackForWeakUEs(weakUes);

        Thread.sleep(2000);
        System.out.println("=== SUCI Simulation End ===");

        logStream.close();
        System.exit(0);
    }

    public static void runBroadCastAttackForWeakUEs(Set<String> weakUes) throws Exception {

        System.out.println("\n========== 针对弱随机UE执行广播重放攻击 ==========\n");

        PhysicalLayer userPlane = new PhysicalLayer();
        CoreNetworkEnvironment core = new CoreNetworkEnvironment();
        Gnb gnb = new Gnb(core, userPlane);

        RegistrationManager regMgr =
                new RegistrationManager("registration_results.csv");

        if (weakUes.isEmpty()) {
            System.out.println("未检测到弱随机UE，广播攻击取消。");
            return;
        }

        Iterator<String> iterator = weakUes.iterator();

        // 1️⃣ 选择第一个弱随机 UE 作为“被记录向量的源”
        String supi0 = iterator.next();

        PhysicalChannel ue2gnb0 = new PhysicalChannel();
        PhysicalChannel gnb2ue0 = new PhysicalChannel();
        gnb.addUeConnection(ue2gnb0, gnb2ue0);

        BroadcastAttackSimulator attacker =
                new BroadcastAttackSimulator(gnb2ue0);
        attacker.addVictimChannel(gnb2ue0);

        UEs ue0 = new UEs(
                supi0,
                core.getSubscriberKey(),
                core.getOpVariant(),
                gnb2ue0, ue2gnb0,
                userPlane
        );

        ue0.registerRegistrationListener(regMgr);
        ue0.start();

        // 等待完成一次完整 AKA
        Thread.sleep(3000);

        // 2️⃣ 启动广播攻击
        attacker.startAttack(0, 2000);

        // 3️⃣ 启动其余弱随机 UE
        while (iterator.hasNext()) {

            String supi = iterator.next();

            PhysicalChannel ue2gnb = new PhysicalChannel();
            PhysicalChannel gnb2ue = new PhysicalChannel();
            gnb.addUeConnection(ue2gnb, gnb2ue);

            attacker.addVictimChannel(gnb2ue);

            UEs ue = new UEs(
                    supi,
                    core.getSubscriberKey(),
                    core.getOpVariant(),
                    gnb2ue, ue2gnb,
                    userPlane
            );

            ue.registerRegistrationListener(regMgr);
            ue.start();

            Thread.sleep(500);
        }

        Thread.sleep(10000);
        attacker.stopAttack();

        System.out.println("\n========== 广播攻击结束 ==========\n");
    }
}
