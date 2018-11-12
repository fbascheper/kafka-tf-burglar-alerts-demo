package com.github.fbascheper.alerts.model.nuki;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of all Nuki smart-lock states.
 *
 * @author Erik-Berndt Scheper
 * @since 02-11-2018
 */
public enum LockStates {

    UNCALIBRATED(0L),
    LOCKED(1L),
    UNLOCKED(3L),
    LOCKING(4L),
    UNLATCHED(5L),
    UNLOCKED_LOCK_N_GO(6L),
    UNLATCHING(7L),
    MOTOR_BLOCKED(254L),
    UNDEFINED(255L);

    private static final Map<Long, LockStates> VALUES = new HashMap<>();

    static {
        for (LockStates smartLockState : LockStates.values()) {
            VALUES.put(smartLockState.value, smartLockState);
        }
    }

    private final long value;

    LockStates(long value) {
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    public static LockStates of(long value) {
        return VALUES.getOrDefault(value, UNDEFINED);
    }
}
