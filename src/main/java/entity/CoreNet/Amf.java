package entity.CoreNet;

import java.util.Arrays;

public class Amf {
    private final Ausf ausf;
    private final Udm udm;

    public Amf(Ausf ausf, Udm udm) {
        this.ausf = ausf;
        this.udm = udm;
    }

    /**
     * 处理 gNB NGAP 上行的 NAS 消息
     */
    public byte[] handleUplinkNas(byte[] nas) {
        if (nas.length > 0 && (nas[0] & 0xFF) == 0x7E) {
            System.out.println("AMF: receive Security Mode Complete（Protected）");
            // 简化：直接回复 Registration Accept
            return buildRegistrationAccept();
        }
        byte msgType = nas[0];
        byte[] res;
        switch (msgType) {
            case 0x01: // Registration Request
                System.out.println("AMF: receive Registration Request");
                // 只打印前16字节避免乱码
                return ausf.authenticate();
            case 0x02: //Authentication Response(RES)
                System.out.println("AMF: receive Authentication Response");
                res = Arrays.copyOfRange(nas,1,nas.length);
                if(udm.verifyXres(res)){
                    System.out.println("AMF: RES Verification passed");
                    return buildSecurityModeCommand();
                }else {
                    System.out.println("AMF: RES Verification failed, registration denied");
                    return buildRegistrationReject((byte)8);
                }
            case 0x03: // Authentication Response(RES) from UE
                System.out.println("AMF: receive Authentication Response");
                res = Arrays.copyOfRange(nas,1,nas.length);
                if(udm.verifyXres(res)){
                    System.out.println("AMF: RES Verification passed");
                    System.out.println("AMF: 5G-AKA Authentication successful, preparing to send Security Mode Command");
                    return buildSecurityModeCommand();
                }else {
                    System.out.println("AMF: RES Verification failed, registration denied");
                    return buildRegistrationReject((byte)8);
                }

            case (byte) 0x5E: // Security Mode Complete
                System.out.println("AMF: receive Security Mode Complete");
                return buildRegistrationAccept();
            default:
                System.out.println("AMF: unknown NAS type=" + (msgType & 0xFF));
                return new byte[0];
        }
    }

    private byte[] buildSecurityModeCommand() {
        System.out.println("AMF: send Security Mode Command");
        // 0x5C = Security Mode Command
//        return new byte[]{ (byte)0x5C };
        byte[] secHeader = new byte[]{0x5C, 0x00, 0x00, 0x00, 0x00, 0x00}; // 简化安全头
        return secHeader;
    }

    private byte[] buildRegistrationAccept() {
        System.out.println("AMF: send Registration Accept");
        // 0x41 = Registration Accept
        return new byte[]{ 0x41 };
    }
    private byte[] buildAuthenticationRequest() {
        System.out.println("AMF: send Authentication Request");
        // 0x02 = Authentication Request
        // 添加RAND (16字节)
        byte[] rand = new byte[16];
        new java.security.SecureRandom().nextBytes(rand);

        byte[] msg = new byte[1 + rand.length];
        msg[0] = 0x02;
        System.arraycopy(rand, 0, msg, 1, rand.length);
        return msg;
    }
    private byte[] buildRegistrationReject(byte cause) {
        System.out.println("AMF: send Registration Reject, cause=" + cause);
        return new byte[]{0x42, cause};
    }


}
