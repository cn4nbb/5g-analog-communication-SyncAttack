package entity;

import entity.gNB.Phy.PhysicalChannel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BroadcastAttackSimulator {
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    private final List<PhysicalChannel> victimChannels = new CopyOnWriteArrayList<>();
    private volatile byte[] firstVector = null;

    public BroadcastAttackSimulator(PhysicalChannel firstGnb2ue) {
        // 只在第一个 UE 的 gNB→UE 通道上记录
        firstGnb2ue.registerToNgap(this::onNetworkAuthRequest);
    }

    /** 将一个新的 gNB→UE 通道加入“受害者”列表，用于后续广播 */
    public void addVictimChannel(PhysicalChannel gnb2ue) {
        victimChannels.add(gnb2ue);
    }

    /** 看到首个 Authentication Request 时记录 RAND||AUTN */
    private void onNetworkAuthRequest(byte[] nas) {
        if (firstVector != null) return;             // 只取首个
        if (nas.length > 0 && nas[0] == 0x02) {
            // strip type byte
            firstVector = new byte[nas.length - 1];
            System.arraycopy(nas, 1, firstVector, 0, firstVector.length);
            System.out.println("Attack: record first RAND||AUTN (len=" + firstVector.length + ")");
        }
    }

    /** 周期性广播重放到所有 victimChannels */
    public void startAttack(long initialDelayMs, long periodMs) {
        scheduler.scheduleAtFixedRate(() -> {
            if (firstVector == null) return;
            byte[] attackNas = new byte[1 + firstVector.length];
            attackNas[0] = 0x02;  // Authentication Request
            System.arraycopy(firstVector, 0, attackNas, 1, firstVector.length);

            for (PhysicalChannel ch : victimChannels) {
                System.out.println("Attack: towards UEChannel " + ch.hashCode() + " replay Authentication Request");
                ch.sendToNgap(attackNas);
            }
        }, initialDelayMs, periodMs, TimeUnit.MILLISECONDS);
    }

    public void stopAttack() {
        scheduler.shutdownNow();
    }
}
