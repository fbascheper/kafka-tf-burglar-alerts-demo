package com.github.fbascheper.alerts.model.nuki;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Holder of the result of a Nuki Web API call.
 *
 * @author Erik-Berndt Scheper
 * @since 07-11-2018
 */
@Getter
@NoArgsConstructor
@ToString
public final class NukiRestApiResponse {

    private List<SmartLock> locks = new ArrayList<>();

    public void parseDates() {
        locks.forEach(SmartLock::parseDates);
    }
}
