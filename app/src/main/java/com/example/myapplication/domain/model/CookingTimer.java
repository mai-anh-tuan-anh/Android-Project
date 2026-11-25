package com.example.myapplication.domain.model;

import com.example.myapplication.domain.model.CookingSession.TimerInput;

/** Immutable countdown state. All elapsed timestamps are relative to the same boot. */
public final class CookingTimer {
    public enum Status { READY, RUNNING, PAUSED, FINISHED }

    public final String sessionId;
    public final String stepId;
    public final int durationSeconds;
    public final Status status;
    /** Deadline for RUNNING, or zero when there is no active deadline. */
    public final long deadlineElapsedMillis;
    /** Fixed remaining time in READY or PAUSED, zero in RUNNING or FINISHED. */
    public final long fixedRemainingMillis;

    private CookingTimer(String sessionId, String stepId, int durationSeconds, Status status,
                         long deadlineElapsedMillis, long fixedRemainingMillis) {
        this.sessionId = sessionId;
        this.stepId = stepId;
        this.durationSeconds = durationSeconds;
        this.status = status;
        this.deadlineElapsedMillis = deadlineElapsedMillis;
        this.fixedRemainingMillis = fixedRemainingMillis;
    }

    public static CookingTimer ready(TimerInput input) {
        if (input.durationSeconds <= 0) throw new IllegalArgumentException("Timer requires a positive duration");
        return new CookingTimer(input.sessionId, input.stepId, input.durationSeconds, Status.READY,
                0, input.durationSeconds * 1000L);
    }

    public long remainingMillis(long now) {
        return status == Status.RUNNING ? Math.max(0, deadlineElapsedMillis - now) : fixedRemainingMillis;
    }

    public CookingTimer startOrResume(long now) {
        if (status != Status.READY && status != Status.PAUSED) return this;
        return transition(Status.RUNNING, now + fixedRemainingMillis, 0);
    }

    public CookingTimer pause(long now) {
        if (status != Status.RUNNING) return this;
        long remaining = remainingMillis(now);
        return remaining == 0 ? transition(Status.FINISHED, 0, 0) : transition(Status.PAUSED, 0, remaining);
    }

    public CookingTimer refresh(long now) {
        return status == Status.RUNNING && remainingMillis(now) == 0 ? transition(Status.FINISHED, 0, 0) : this;
    }

    public CookingTimer reset() { return transition(Status.READY, 0, durationSeconds * 1000L); }

    private CookingTimer transition(Status next, long deadline, long remaining) {
        return new CookingTimer(sessionId, stepId, durationSeconds, next, deadline, remaining);
    }
}
