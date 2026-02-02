package entity.Ue.register;

public interface RegistrationListener {
    /**
     * @param supi UE 标识
     * @param success 是否注册成功
     * @param cause 注册失败原因（0 表示成功）
     */
    void onRegistrationResult(String supi, boolean success, int cause);
}
