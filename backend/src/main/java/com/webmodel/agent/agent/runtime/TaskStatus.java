package com.webmodel.agent.agent.runtime;

import java.util.Set;

public enum TaskStatus {
    CREATED,
    QUEUED,
    CHECKING_MODEL,
    WAITING_LOGIN,
    PLANNING,
    VALIDATING_PLAN,
    WAITING_APPROVAL,
    RUNNING_STEP,
    WAITING_MODEL,
    SUMMARIZING_CONTEXT,
    PAUSED,
    CANCEL_REQUESTED,
    SUCCEEDED,
    FAILED,
    CANCELLED;

    private static final Set<TaskStatus> TERMINAL = Set.of(SUCCEEDED, FAILED, CANCELLED);
    private static final Set<TaskStatus> RUNNING = Set.of(QUEUED, CHECKING_MODEL, PLANNING,
            VALIDATING_PLAN, RUNNING_STEP, WAITING_MODEL, SUMMARIZING_CONTEXT);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    public boolean isRunning() {
        return RUNNING.contains(this);
    }

    public boolean canPause() {
        return !isTerminal() && this != PAUSED && this != CANCEL_REQUESTED;
    }

    public boolean canCancel() {
        return !isTerminal();
    }
}