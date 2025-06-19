package entity.gNB.Pdcp;

import java.util.function.Consumer;

/**
 * PDCP 层：简化的 PDCP 层实现
 * 处理上行和下行数据包，添加 PDCP 序列号（SN）
 * 支持 SRB（Signaling Radio Bearer）和 DRB（Data Radio Bearer）
 */
public class PdcpLayer {
    public enum Direction { UPLINK, DOWNLINK }

    private final Consumer<byte[]> sendToRlc;
    private final boolean isSignaling; // true=SRB, false=DRB
    private int pdcpSn = 0;

    public PdcpLayer(Consumer<byte[]> sendToRlc, boolean isSignaling) {
        this.sendToRlc = sendToRlc;
        this.isSignaling = isSignaling;
    }

    /**
     * 上行：将上层 PDU 加上 PDCP 头并转给 RLC
     */
    public void sendUplink(byte[] payload) {
        // 构造 PDCP PDU: [SN(2B)|Payload]
        byte[] header = new byte[]{ (byte)(pdcpSn >> 8), (byte)pdcpSn };
        pdcpSn = (pdcpSn + 1) & 0xFFFF;
        byte[] pdu = new byte[header.length + payload.length];
        System.arraycopy(header, 0, pdu, 0, header.length);
        System.arraycopy(payload, 0, pdu, header.length, payload.length);
        System.out.println("gNB PDCP(" + (isSignaling?"SRB":"DRB") + ") uplink SN=" + pdcpSn);
        sendToRlc.accept(pdu);
    }

    /**
     * 下行：从 RLC 收到 PDU，去掉头后交给上层
     */
    public void onRlcDownlink(byte[] pdu, Consumer<byte[]> deliverToUpper) {
        if (pdu.length < 2) return;
        int sn = ((pdu[0] & 0xFF) << 8) | (pdu[1] & 0xFF);
        byte[] payload = new byte[pdu.length - 2];
        System.arraycopy(pdu, 2, payload, 0, payload.length);
        System.out.println("gNB PDCP(" + (isSignaling?"SRB":"DRB") + ") downlink SN=" + sn);
        deliverToUpper.accept(payload);
    }
}
