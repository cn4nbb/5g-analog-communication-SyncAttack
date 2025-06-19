package entity.gNB.Mac;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * MAC 层：简化调度器，轮询上行队列；下行直通
 */
public class MacLayer {
    private final Consumer<byte[]> toPhy;      // 发送到物理层
    private Consumer<byte[]> upperHandler;     // 从物理层收到后发给 RLC

    public MacLayer(Consumer<byte[]> toPhy) {
        this.toPhy = toPhy;
    }

    /** RLC 上行调用，向物理层发送 MAC PDU */
    public void sendUplink(byte[] macPdu) {
        toPhy.accept(macPdu);
    }

    /** gNB/UE 在下行或上行时都用同一个入口接收物理层调用 */
    public void onPhyUplink(byte[] macPdu) {
        // 物理层上行到达
        if (upperHandler != null) upperHandler.accept(macPdu);
    }

    public void onPhyDownlink(byte[] macPdu) {
        // 物理层下行到达
        if (upperHandler != null) upperHandler.accept(macPdu);
    }

    /**
     * 注册 RLC 层的回调，MAC 收到任何方向的 PDU 后都会交给 RLC
     */
    public void registerUpperLayerHandler(Consumer<byte[]> handler) {
        this.upperHandler = handler;
    }
}
