package entity.gNB.Phy;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/*
    * 用户面
 */
public class PhysicalLayer {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private Consumer<byte[]> toMac;   // 上行到 gNB MAC
    private Consumer<byte[]> toRlc;   // 下行到 gNB RLC
    private Consumer<byte[]> toUeMac; // 下行到 UE MAC
    private Consumer<byte[]> toUeRlc; // 上行到 UE RLC

    /** 注册上行处理器 */
    public void registerGnbUplink(Consumer<byte[]> toMac) { this.toMac = toMac; }
    /** 注册下行处理器 */
    public void registerGnbDownlink(Consumer<byte[]> toUeMac) { this.toUeMac = toUeMac; }

    /** 上行：UE MAC 发来的 PDU */
    public void sendUplink(byte[] macPdu) {
        scheduler.schedule(() -> {
            System.out.println("PHY: uplink forward to gNB MAC");
            toMac.accept(macPdu);
        }, 50, TimeUnit.MILLISECONDS);
    }

    /** 下行：gNB MAC 发来的 PDU */
    public void sendDownlink(byte[] macPdu) {
        scheduler.schedule(() -> {
            System.out.println("PHY: downlink forward to UE MAC");
            toUeMac.accept(macPdu);
        }, 50, TimeUnit.MILLISECONDS);
    }

}
