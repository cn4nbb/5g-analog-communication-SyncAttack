package entity.gNB.Phy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/*
    控制面
 */
public class PhysicalChannel {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final long DELAY_MS = 50;  // 模拟信道延迟
    private final List<Consumer<byte[]>> rrcHandlers  = new ArrayList<>();
    private final List<Consumer<byte[]>> ngapHandlers = new ArrayList<>();
    /**
     * 注册将上行信令送到 gNB RRC 层的处理器
     */
    public void registerToRrc(Consumer<byte[]> handler) {
        rrcHandlers.add(handler);
    }

    /**
     * 注册将上行信令送到 gNB NGAP 层的处理器
     */
    public void registerToNgap(Consumer<byte[]> handler) {
        ngapHandlers.add(handler);
    }

    /**
     * 从 UE RRC/NGAP 发出的上行消息
     */
   public void sendToNgap(byte[] data) {
       scheduler.schedule(() -> {
           System.out.println("PhysicalChannel: 上行转发到 gNB NGAP");
           for (var h : ngapHandlers) h.accept(data);
           }, DELAY_MS, TimeUnit.MILLISECONDS);
   }

    /**
     * 从 gNB RRC/NGAP 发出的下行消息
     */
    public void sendToRrc(byte[] data) {
        scheduler.schedule(() -> {
            System.out.println("PhysicalChannel: 下行转发到 UE RRC/NGAP");
            for (var h : rrcHandlers) h.accept(data);
            }, DELAY_MS, TimeUnit.MILLISECONDS);
    }
}
