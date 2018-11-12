package com.github.fbascheper.alerts.model.nuki;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.fbascheper.alerts.model.avro.SerializableSmartLock;
import com.github.fbascheper.alerts.util.common.DateUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.time.ZonedDateTime;

/**
 * The information about a Nuki SmartLock, as returned by the REST API.
 *
 * @author Erik-Berndt Scheper
 * @since 07-11-2018
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@NoArgsConstructor
@ToString
public final class SmartLock {

    private long smartlockId;

    private long accountId;

    private long type;

    private long authId;

    private java.lang.String name;

    private boolean favorite;

    private SmartLockConfig config;

    private SmartLockAdvancedConfig advancedConfig;

    private SmartLockState state;

    private long firmwareVersion;

    private long serverState;

    private long adminPinState;

    private String creationDate;

    private String updateDate;

    private ZonedDateTime zonedCreationDateTime;
    private ZonedDateTime zonedUpdateDateTime;

    public ZonedDateTime getZonedUpdateDateTime() {
        return zonedUpdateDateTime;
    }

    public SerializableSmartLock toSerializableSmartLock(SmartLock this) {
        Instant instant = this.getZonedUpdateDateTime().toInstant();

        SerializableSmartLock result = new SerializableSmartLock();
        result.setSmartlockId(this.getSmartlockId());
        result.setName(this.getName());
        result.setAccountId(this.getAccountId());
        result.setState(this.getState().getState());
        result.setTrigger(this.getState().getTrigger());
        result.setBatteryCritical(this.getState().isBatteryCritical());
        result.setUpdateEpochSeconds(instant.getEpochSecond());

        return result;
    }

    void parseDates() {
        this.zonedCreationDateTime = DateUtils.toZonedDateTime(this.creationDate, DateUtils.ZONE_ID_EUROPE_AMSTERDAM);
        this.zonedUpdateDateTime = DateUtils.toZonedDateTime(this.updateDate, DateUtils.ZONE_ID_EUROPE_AMSTERDAM);
    }
}
