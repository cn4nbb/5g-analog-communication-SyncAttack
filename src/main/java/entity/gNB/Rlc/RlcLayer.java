package entity.gNB.Rlc;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * RLC 层：简化的 Unacknowledged Mode (UM)
 */
public class RlcLayer {
    private final Consumer<byte[]> sendToMac;
    private Queue<byte[]> transmitBuffer = new LinkedList<>();

    public RlcLayer(Consumer<byte[]> sendToMac) {
        this.sendToMac = sendToMac;
    }

    /**
     * 上行：从 PDCP 接收 PDU，放入缓冲，立即发 MAC
     */
    public void sendUplink(byte[] pdcpPdu) {
        transmitBuffer.offer(pdcpPdu);
        System.out.println("gNB RLC UM: cache PDU and submit MAC");
        sendToMac.accept(pdcpPdu);
    }

    /**
     * 下行：从 MAC 接收 MAC PDU，直接交给 PDCP
     */
    public void onMacDownlink(byte[] macPdu, Consumer<byte[]> deliverToPdcp) {
        System.out.println("gNB RLC UM: receive MAC PDU and submit PDCP");
        deliverToPdcp.accept(macPdu);
    }
}
