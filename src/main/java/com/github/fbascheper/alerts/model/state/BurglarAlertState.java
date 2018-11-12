package com.github.fbascheper.alerts.model.state;

import java.util.HashMap;
import java.util.Map;

/**
 * State of the Burglar alert system.
 *
 * @author Erik-Berndt Scheper
 * @since 06-11-2018
 */
public enum BurglarAlertState {

    SEND_MESSAGES_ENABLED(-1),

    SEND_MESSAGES_DISABLED(0);

    private final Integer value;
    private static final Map<Integer, BurglarAlertState> VALUES = new HashMap<>();

    static {
        for (BurglarAlertState smartLockState : BurglarAlertState.values()) {
            VALUES.put(smartLockState.value, smartLockState);
        }
    }

    BurglarAlertState(Integer value) {
        this.value = value;
    }

    public static BurglarAlertState of(Integer value) {
        return VALUES.get(value);
    }

    public Integer getValue() {
        return value;
    }

}
