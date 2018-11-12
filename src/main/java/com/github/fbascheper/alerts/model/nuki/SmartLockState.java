package com.github.fbascheper.alerts.model.nuki;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * The state of a Nuki SmartLock, as returned by the REST API.
 *
 * @author Erik-Berndt Scheper
 * @since 07-11-2018
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@NoArgsConstructor
@ToString
public final class SmartLockState {

    private long mode;

    private long state;

    private long trigger;

    private boolean batteryCritical;


}
