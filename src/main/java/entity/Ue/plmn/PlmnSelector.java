package entity.Ue.plmn;

import java.util.HashSet;
import java.util.Set;

public class PlmnSelector {
    //可用PLMN列表，RAT切换，禁用PLMN等功能
    // 当前可用的 PLMN 列表
    private final Set<String> availablePlmns = new HashSet<>();
    // 被禁用的 PLMN 列表
    private final Set<String> forbiddenPlmns = new HashSet<>();
    // 是否禁用 5G 网络
    private boolean fiveGDisabled = false;

    public PlmnSelector() {
        // 初始化可用 PLMN，可从 USIM 或配置读取
        // 示例：假设当前 UE 订阅了两个 PLMN
        availablePlmns.add("MCC123_MNC456");
        availablePlmns.add("MCC789_MNC012");
    }

    /**
     * 处理 Registration Reject 的 5GMM 原因码
     * @param cause5gmm 5GMM Reject 原因码
     */
    public void handleReject(int cause5gmm) {
        System.out.println("PLMN: Received registration rejection reason code=" + cause5gmm);
        switch (cause5gmm) {
            case 7:
                // 禁用 5G，只能使用 4G 及以下
                fiveGDisabled = true;
                System.out.println("PLMN: 5G network is disabled and will switch to LTE or below.");
                break;
            case 11:
                // 禁用当前 PLMN
                // 假设当前正在尝试的 PLMN 在可用列表中的第一个
                String current = availablePlmns.iterator().next();
                forbiddenPlmns.add(current);
                System.out.println("PLMN: disable PLMN=" + current + ", will no longer try this PLMN.");
                break;
            default:
                System.out.println("PLMN: unknown Reject reason，Ignore.");
        }
    }

    /**
     * 获取下一个可用 PLMN
     * @return PLMN 标识 (MCC_MNC) 或 null
     */
    public String selectNextPlmn() {
        for (String plmn : availablePlmns) {
            if (!forbiddenPlmns.contains(plmn)) {
                return plmn;
            }
        }
        return null;
    }

    /**
     * 是否禁用 5G
     */
    public boolean isFiveGDisabled() {
        return fiveGDisabled;
    }

    /**
     * 判断给定 PLMN 是否被禁用
     */
    public boolean isPlmnForbidden(String plmn) {
        return forbiddenPlmns.contains(plmn);
    }
}
