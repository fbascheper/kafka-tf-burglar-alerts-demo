package com.github.fbascheper.alerts.model.nuki;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * The basic configuration of a Nuki SmartLock, as returned by the REST API.
 *
 * @author Erik-Berndt Scheper
 * @since 07-11-2018
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@NoArgsConstructor
@ToString
public final class SmartLockConfig {

    private java.lang.String name;

    private double latitude;

    private double longitude;

    private boolean autoUnlatch;

    private boolean pairingEnabled;

    private boolean buttonEnabled;

    private boolean ledEnabled;

    private long ledBrightness;

    private long timezoneOffset;

    private long daylightSavingMode;

    private boolean fobPaired;

    private long fobAction1;

    private long fobAction2;

    private long fobAction3;

    private boolean singleLock;

    private long advertisingMode;

}
