package entity.CoreNet;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public class Amf {
    private final Ausf ausf;


    public Amf(Ausf ausf) {
        this.ausf = ausf;
    }

    /**
     * 处理 gNB NGAP 上行的 NAS 消息
     */
    public byte[] handleUplinkNas(byte[] nas) {
        if (nas.length > 0 && (nas[0] & 0xFF) == 0x7E) {
            System.out.println("AMF: 收到 Security Mode Complete（受保护）");
            // 简化：直接回复 Registration Accept
            return buildRegistrationAccept();
        }
        byte msgType = nas[0];

        switch (msgType) {
            case 0x01: // Registration Request
                System.out.println("AMF: 收到 Registration Request");
                // 只打印前16字节避免乱码
                String suciPreview = nas.length > 16 ?
                        new String(Arrays.copyOfRange(nas, 1, 17)) :
                        new String(Arrays.copyOfRange(nas, 1, nas.length));
                System.out.println("AMF: 处理 SUCI(预览): " + suciPreview);
                return buildAuthenticationRequest();
            case 0x03: // Authentication Response
                System.out.println("AMF: 收到 Authentication Response");
                // 验证RES（简化验证）
                if (nas.length > 1) {
                    byte[] res = Arrays.copyOfRange(nas, 1, nas.length);
                    return buildSecurityModeCommand();
                }
                return new byte[0];
            case (byte) 0x5E: // Security Mode Complete
                System.out.println("AMF: 收到 Security Mode Complete");
                return buildRegistrationAccept();
            default:
                System.out.println("AMF: 未知 NAS 类型=" + (msgType & 0xFF));
                return new byte[0];
        }
    }

    private byte[] buildSecurityModeCommand() {
        System.out.println("AMF: 发送 Security Mode Command");
        // 0x5C = Security Mode Command
//        return new byte[]{ (byte)0x5C };
        byte[] secHeader = new byte[]{0x5C, 0x00, 0x00, 0x00, 0x00, 0x00}; // 简化安全头
        return secHeader;
    }

    private byte[] buildRegistrationAccept() {
        System.out.println("AMF: 发送 Registration Accept");
        // 0x41 = Registration Accept
        return new byte[]{ 0x41 };
    }
    private byte[] buildAuthenticationRequest() {
        System.out.println("AMF: 发送 Authentication Request");
        // 0x02 = Authentication Request
        // 添加RAND (16字节)
        byte[] rand = new byte[16];
        new java.security.SecureRandom().nextBytes(rand);

        byte[] msg = new byte[1 + rand.length];
        msg[0] = 0x02;
        System.arraycopy(rand, 0, msg, 1, rand.length);
        return msg;
    }



}
