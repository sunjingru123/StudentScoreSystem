package com.student.studentscoresystem;

import com.student.studentscoresystem.utils.LoginFailureTracker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 登录失败跟踪器行为测试。
 */
class LoginFailureTrackerTest {

    @Test
    void blocksAfterMaxFailuresAndRecoversOnClear() {

        LoginFailureTracker tracker =
                new LoginFailureTracker();

        String username = "student";

        for (int i = 0; i < 4; i++) {

            tracker.recordFailure(username);
        }

        assertFalse(
                tracker.isBlocked(username),
                "未达到阈值不应锁定"
        );

        tracker.recordFailure(username);

        assertTrue(
                tracker.isBlocked(username),
                "连续失败达到阈值应临时锁定"
        );

        tracker.clear(username);

        assertFalse(
                tracker.isBlocked(username),
                "登录成功后应清除失败记录"
        );
    }

    @Test
    void isolatesDifferentUsers() {

        LoginFailureTracker tracker =
                new LoginFailureTracker();

        for (int i = 0; i < 5; i++) {

            tracker.recordFailure("attacker");
        }

        assertTrue(tracker.isBlocked("attacker"));
        assertFalse(
                tracker.isBlocked("victim"),
                "锁定不应影响其他账号"
        );
    }
}
