package Utils;

public class RateLimiter {
    private int failedAttempts = 0;
    private long lockoutEndTime = 0;
    private final int maxFailedAttempts;
    private final long lockoutDurationMs;

    public RateLimiter(int maxFailedAttempts, long lockoutDurationMs) {
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockoutDurationMs = lockoutDurationMs;
    }

    public boolean isLockedOut() {
        return System.currentTimeMillis() < lockoutEndTime;
    }

    public long getSecondsLeft() {
        if (!isLockedOut()) return 0;
        return (lockoutEndTime - System.currentTimeMillis()) / 1000;
    }

    public void recordFailure() {
        failedAttempts++;
        if (failedAttempts >= maxFailedAttempts) {
            lockoutEndTime = System.currentTimeMillis() + lockoutDurationMs;
        }
    }

    public void recordSuccess() {
        failedAttempts = 0;
        lockoutEndTime = 0;
    }

    public int getAttemptsLeft() {
        return Math.max(0, maxFailedAttempts - failedAttempts);
    }
    
    public boolean hasReachedMaxAttempts() {
        return failedAttempts >= maxFailedAttempts;
    }
}
