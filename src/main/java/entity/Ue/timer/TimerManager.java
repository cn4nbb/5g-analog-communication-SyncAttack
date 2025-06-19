package entity.Ue.timer;

import java.util.Map;
import java.util.concurrent.*;

public class TimerManager {
    // T3560/T3561等定时器
    // 定时器标识
    public static final int T3560 = 3560;
    public static final int T3561 = 3561;
    // 可根据需要扩展更多定时器

    // 定时器超时时间（毫秒），真实可基于配置或 TS 24.501 定义
    private static final long DURATION_T3560_MS = 5000;
    private static final long DURATION_T3561_MS = 5000;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<Integer, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    /**
     * 启动指定定时器，如果已经存在则先取消再重启
     * @param timerId 定时器标识
     */
    public void startTimer(int timerId) {
        cancelTimer(timerId);
        long delay = getDuration(timerId);
        ScheduledFuture<?> future = scheduler.schedule(() -> onTimeout(timerId), delay, TimeUnit.MILLISECONDS);
        timers.put(timerId, future);
        System.out.println("TimerManager: 启动定时器 T" + timerId + ", 超时=" + delay + "ms");
    }

    /**
     * 取消所有活跃定时器
     */
    public void stopActiveTimers() {
        for (Map.Entry<Integer, ScheduledFuture<?>> entry : timers.entrySet()) {
            entry.getValue().cancel(false);
            System.out.println("TimerManager: 取消定时器 T" + entry.getKey());
        }
        timers.clear();
    }

    /**
     * 取消指定定时器
     * @param timerId 定时器标识
     */
    public void cancelTimer(int timerId) {
        ScheduledFuture<?> future = timers.remove(timerId);
        if (future != null) {
            future.cancel(false);
            System.out.println("TimerManager: 取消定时器 T" + timerId);
        }
    }

    /**
     * 定时器到期回调
     * @param timerId 定时器标识
     */
    private void onTimeout(int timerId) {
        timers.remove(timerId);
        System.err.println("TimerManager: 定时器 T" + timerId + " 到期");
        // TODO: 通知上层（如 NasLayer 或 StateMachine）执行超时处理，如重发或失败上报
    }

    private long getDuration(int timerId) {
        switch (timerId) {
            case T3560:
                return DURATION_T3560_MS;
            case T3561:
                return DURATION_T3561_MS;
            default:
                throw new IllegalArgumentException("未知定时器: T" + timerId);
        }
    }
}
