package entity;

public class Attacker {
    /**
     * 拦截并打印消息内容
     * @param message
     * @return 篡改后的消息
     */
    public String interceptMessage(String message) {
        System.out.println("攻击者: 拦截消息 - " + message);
        // 示例：篡改SUCI或认证令牌
        if (message.contains("SUCI")) {
            String alteredSUCI = "Fake_SUCI_Encrypted_Data";
            System.out.println("攻击者: 修改SUCI为 - " + alteredSUCI);
            return "Registration Request: SUCI=" + alteredSUCI;
        } else if (message.contains("RAND")) {
            String alteredRAND = "Fake_RAND_Value";
            String alteredAUTN = "Fake_AUTN_Value";
            System.out.println("攻击者: 修改RAND和AUTN为 - " + alteredRAND + ", " + alteredAUTN);
            return alteredRAND + "," + alteredAUTN;
        }
        return message;
    }

    /**
     * 监听认证失败消息并分析目标UE
     * @param failureMessage
     */
    public void analyzeFailureMessage(String failureMessage) {
        System.out.println("攻击者: 接收到认证失败消息 - " + failureMessage);
        if (failureMessage.contains("MAC_Failure")) {
            System.out.println("攻击者: 可能的原因 - MAC同步错误，推测目标UE存在。");
        } else if (failureMessage.contains("Sync_Failure")) {
            System.out.println("攻击者: 可能的原因 - 时间同步问题，进一步确认目标UE身份。");
        }
    }

    /**
     * 模拟篡改后的认证响应
     * @return
     */
    public String fakeAuthResponse() {
        String fakeRand = "Fake_RAND_Value";
        String fakeAutn = "Fake_AUTN_Value";
        System.out.println("攻击者: 发送伪造认证响应 - " + fakeRand + ", " + fakeAutn);
        return fakeRand + "," + fakeAutn;
    }
}
