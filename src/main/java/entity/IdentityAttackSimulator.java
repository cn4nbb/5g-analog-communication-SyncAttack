package entity;

import entity.gNB.Phy.PhysicalChannel;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IdentityAttackSimulator {
    private final PhysicalChannel gnb2ue;
    private final PhysicalChannel ue2gnb;
    private final List<byte[]> recordedVectors = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public IdentityAttackSimulator(PhysicalChannel gnb2ue, PhysicalChannel ue2gnb) {
        this.gnb2ue = gnb2ue;
        this.ue2gnb = ue2gnb;
        // 记录网络发给 UE 的 Authentication Request (RAND||AUTN)
        gnb2ue.registerToNgap(this::onNetworkAuthRequest);
        // 监听 UE 发给网络的响应 (MAC Failure/Sync Failure)
        ue2gnb.registerToNgap(this::onUeResponse);
    }

    /** 记录合法向量 */
    private void onNetworkAuthRequest(byte[] nas) {
        if (nas.length > 0 && nas[0] == 0x02) {
            byte[] vec = Arrays.copyOfRange(nas, 1, nas.length);
            recordedVectors.add(vec);
            System.out.println("Attack: record RAND||AUTN (len=" + vec.length + ")");
        }
    }

    /** 启动周期性重放 */
//    public void startAttack(long initialDelayMs, long periodMs) {
//        scheduler.scheduleAtFixedRate(() -> {
//            for (byte[] vec : recordedVectors) {
//                byte[] attackNas = new byte[1 + vec.length];
//                attackNas[0] = 0x02;  // Authentication Request
//                System.arraycopy(vec, 0, attackNas, 1, vec.length);
//                //System.out.println("Attack: 重放 Authentication Request");
//                gnb2ue.sendToNgap(attackNas);
//            }
//        }, initialDelayMs, periodMs, TimeUnit.MILLISECONDS);
//    }
    public void startAttack(long initialDelayMs, long periodMs) {
        scheduler.scheduleAtFixedRate(() -> {
            if (recordedVectors.isEmpty()) return;
            // 只取第一个
            byte[] vec = recordedVectors.get(0);
            byte[] attackNas = new byte[1 + vec.length];
            attackNas[0] = 0x02;  // Authentication Request
            System.arraycopy(vec, 0, attackNas, 1, vec.length);
            System.out.println("Attack: replay first Authentication Request");
            gnb2ue.sendToNgap(attackNas);
        }, initialDelayMs, periodMs, TimeUnit.MILLISECONDS);
    }

    /** 区分 UE 的失败响应 */
    private void onUeResponse(byte[] nas) {
        if (nas.length == 0) return;
        switch (nas[0]) {
            case 0x04:
                System.out.println("Attack: receive MAC Failure → Non-target UE");
                break;
            case 0x06:
                System.out.println("Attack: receive Sync Failure → Target UE confirmed");
                break;
            default:
                // 忽略正常或其它 NAS
        }
    }

    public void stopAttack() {
        scheduler.shutdownNow();
    }
}
