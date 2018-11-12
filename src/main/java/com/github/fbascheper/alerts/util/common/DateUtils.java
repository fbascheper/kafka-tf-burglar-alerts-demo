package com.github.fbascheper.alerts.util.common;


import org.slf4j.Logger;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Basic date utilities.
 *
 * @author Erik-Berndt Scheper
 * @since 02-11-2018
 */
public final class DateUtils {

    private static final Logger LOGGER = getLogger(DateUtils.class);

    public static final ZoneId ZONE_ID_EUROPE_AMSTERDAM = ZoneId.of("Europe/Amsterdam");

    private DateUtils() {
        // prevent instantiation
    }

    /**
     * Return a {@link ZonedDateTime} for a given epoch seconds value and a given {@link ZoneId}.
     *
     * @param epochSecond value to convert
     * @param zoneId      the {@link ZoneId} to use
     * @return the corresponding {@link ZonedDateTime} in Amsterdam.
     */
    public static ZonedDateTime ofEpochSecond(long epochSecond, ZoneId zoneId) {
        Instant instant = Instant.ofEpochSecond(epochSecond);
        return ZonedDateTime.ofInstant(instant, zoneId);
    }

    /**
     * Return a {@link ZonedDateTime} corresponding with the passed ZoneId, from a given string value.
     *
     * @param value  value to convert
     * @param zoneId the {@link ZoneId} to use
     * @return the corresponding {@link ZonedDateTime} in Amsterdam.
     */
    public static ZonedDateTime toZonedDateTime(String value, ZoneId zoneId) {

        ZonedDateTime result;

        try {
            result = ZonedDateTime.parse(value).withZoneSameInstant(zoneId);

        } catch (DateTimeParseException | NullPointerException ex) {
            LOGGER.error("Could not parse date {}", value);
            result = ZonedDateTime.now(zoneId);
        }

        return result;
    }

}
