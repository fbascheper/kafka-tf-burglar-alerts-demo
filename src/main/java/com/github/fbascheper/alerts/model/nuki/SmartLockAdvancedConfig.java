package com.github.fbascheper.alerts.model.nuki;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * The advanced configuration of a Nuki SmartLock, as returned by the REST API.
 *
 * @author Erik-Berndt Scheper
 * @since 07-11-2018
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@NoArgsConstructor
@ToString
public final class SmartLockAdvancedConfig {

    private long totalDegrees;

    private long unlockedPositionOffsetDegrees;

    private long lockedPositionOffsetDegrees;

    private long singleLockedPositionOffsetDegrees;

    private long unlockedToLockedTransitionOffsetDegrees;

    private long lngTimeout;

    private long singleButtonPressAction;

    private long doubleButtonPressAction;

    private boolean detachedCylinder;

    private long batteryType;

    private boolean automaticBatteryTypeDetection;

    private long unlatchDuration;

    private long autoLockTimeout;

}
