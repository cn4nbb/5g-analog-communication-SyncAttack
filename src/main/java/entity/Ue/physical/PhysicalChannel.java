//package entity.Ue.physical;
//
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//import java.util.function.Consumer;
//
//public class PhysicalChannel {
//    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
//    private Consumer<byte[]> toRrc;
//    private Consumer<byte[]> toNgap;
//
//    public void registerToRrc(Consumer<byte[]> handler) {
//        this.toRrc = handler;
//    }
//
//    public void registerToNgap(Consumer<byte[]> handler) {
//        this.toNgap = handler;
//    }
//
//    public void sendToNgap(byte[] data) {
//        // 模拟信道延迟
//        scheduler.schedule(() -> {
//            System.out.println("Physical: 转发到 NGAP");
//            toNgap.accept(data);
//        }, 100, TimeUnit.MILLISECONDS);
//    }
//
//    public void sendToRrc(byte[] data) {
//        scheduler.schedule(() -> {
//            System.out.println("Physical: 转发到 RRC");
//            toRrc.accept(data);
//        }, 100, TimeUnit.MILLISECONDS);
//    }
//}
