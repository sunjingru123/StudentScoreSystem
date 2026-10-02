package com.student.studentscoresystem.utils;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * =========================================================
 * 登录失败跟踪器
 *
 * 目标：
 *
 * 1. 有界：条目带 TTL，并由后台线程定时淘汰，
 *    避免无界 ConcurrentHashMap 被大量伪造用户名撑爆内存。
 *
 * 2. 有时间窗口：只有连续失败（失败间隔在窗口内）才累计，
 *    达到阈值后临时锁定 5 分钟，锁定期满自动恢复，
 *    不再永久锁死合法账号。
 * =========================================================
 */
public class LoginFailureTracker {

    /**
     * 失败统计窗口：
     * 相邻失败超过该间隔则重新计数。
     */
    private static final long WINDOW_MILLIS = 5 * 60 * 1000L;

    /**
     * 触发锁定的连续失败次数。
     */
    private static final int MAX_FAILURES = 5;

    /**
     * 锁定时长：临时锁定 5 分钟。
     */
    private static final long LOCK_MILLIS = 5 * 60 * 1000L;

    /**
     * 空闲条目存活时间：
     * 未处于锁定状态且超过该时间未更新则淘汰。
     */
    private static final long ENTRY_TTL_MILLIS = 10 * 60 * 1000L;

    /**
     * 条目数量上限，兜底防止内存膨胀。
     */
    private static final int MAX_ENTRIES = 10_000;

    /**
     * 后台清理周期。
     */
    private static final long CLEAN_INTERVAL_MILLIS = 60 * 1000L;

    private final Map<String, FailureState> failures =
            new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleaner;

    public LoginFailureTracker() {

        this.cleaner =
                Executors.newSingleThreadScheduledExecutor(
                        new ThreadFactory() {

                            @Override
                            public Thread newThread(Runnable runnable) {

                                Thread thread =
                                        new Thread(
                                                runnable,
                                                "login-failure-cleaner"
                                        );

                                thread.setDaemon(true);

                                return thread;
                            }
                        }
                );

        this.cleaner.scheduleWithFixedDelay(
                this::evictExpired,
                CLEAN_INTERVAL_MILLIS,
                CLEAN_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 当前用户名是否处于锁定期。
     */
    public boolean isBlocked(String username) {

        if (username == null) {

            return false;
        }

        FailureState state =
                failures.get(username);

        if (state == null) {

            return false;
        }

        synchronized (state) {

            return state.blockedUntil
                    > System.currentTimeMillis();
        }
    }

    /**
     * 记录一次登录失败。
     */
    public void recordFailure(String username) {

        if (username == null) {

            return;
        }

        long now =
                System.currentTimeMillis();

        FailureState state =
                failures.computeIfAbsent(
                        username,
                        key -> new FailureState()
                );

        synchronized (state) {

            /*
             * 超出统计窗口，重新开始计数。
             */
            if (state.windowStart <= 0
                    || now - state.windowStart > WINDOW_MILLIS) {

                state.windowStart = now;
                state.count = 0;
            }

            state.count++;
            state.lastUpdated = now;

            if (state.count >= MAX_FAILURES) {

                state.blockedUntil = now + LOCK_MILLIS;
                state.count = 0;
                state.windowStart = now;
            }
        }

        /*
         * 兜底防止内存膨胀。
         */
        if (failures.size() > MAX_ENTRIES) {

            evictExpired();

            enforceMaxSize();
        }
    }

    /**
     * 登录成功后清除失败记录。
     */
    public void clear(String username) {

        if (username == null) {

            return;
        }

        failures.remove(username);
    }

    /**
     * 淘汰空闲过期条目。
     */
    private void evictExpired() {

        long now =
                System.currentTimeMillis();

        Iterator<Map.Entry<String, FailureState>> iterator =
                failures.entrySet().iterator();

        while (iterator.hasNext()) {

            FailureState state =
                    iterator.next().getValue();

            boolean blocked;
            long idle;

            synchronized (state) {

                blocked =
                        state.blockedUntil > now;

                idle =
                        now - state.lastUpdated;
            }

            if (!blocked
                    && idle > ENTRY_TTL_MILLIS) {

                iterator.remove();
            }
        }
    }

    /**
     * 超过上限时优先淘汰未锁定的条目。
     */
    private void enforceMaxSize() {

        if (failures.size() <= MAX_ENTRIES) {

            return;
        }

        long now =
                System.currentTimeMillis();

        Iterator<Map.Entry<String, FailureState>> iterator =
                failures.entrySet().iterator();

        while (iterator.hasNext()
                && failures.size() > MAX_ENTRIES) {

            FailureState state =
                    iterator.next().getValue();

            synchronized (state) {

                if (state.blockedUntil <= now) {

                    iterator.remove();
                }
            }
        }
    }

    /**
     * 单个用户名的失败状态。
     */
    private static final class FailureState {

        private int count;

        private long windowStart;

        private long blockedUntil;

        private long lastUpdated;
    }
}
