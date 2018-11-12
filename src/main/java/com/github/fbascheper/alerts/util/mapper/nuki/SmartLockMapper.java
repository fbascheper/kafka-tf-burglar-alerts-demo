package com.github.fbascheper.alerts.util.mapper.nuki;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fbascheper.alerts.model.avro.SerializableSmartLock;
import com.github.fbascheper.alerts.model.nuki.LockStates;
import com.github.fbascheper.alerts.model.nuki.NukiRestApiResponse;
import com.github.fbascheper.alerts.model.nuki.SmartLock;
import com.github.fbascheper.alerts.model.state.BurglarAlertState;
import org.apache.kafka.streams.KeyValue;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Nuki smart-lock mapper.
 *
 * @author Erik-Berndt Scheper
 * @since 02-11-2018
 */
public final class SmartLockMapper {

    private static final Logger LOGGER = getLogger(SmartLockMapper.class);

    public static final String ALERT_STATE = "state";

    private SmartLockMapper() {
        // prevent instantiation
    }

    /**
     * Map the JSON response from the Nuki Web API to a domain object.
     *
     * @param value JSON response
     * @return domain object holding the result of the Nuki Web API
     */
    public static NukiRestApiResponse toNukiResponse(String value) {
        NukiRestApiResponse result = null;

        int startIndex = value.indexOf('[');
        int endIndex = value.lastIndexOf(']');
        String json = (startIndex >= 0 && endIndex <= value.length()) ? value.substring(startIndex, endIndex + 1) : value;

        String convertedJson = "{\"locks\": " + json + "}";

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
            result = objectMapper.readValue(convertedJson, NukiRestApiResponse.class);
            result.parseDates();

        } catch (IOException ex) {
            LOGGER.error("Mapping error for JSON = " + convertedJson, ex);
        }

        return result;
    }

    /**
     * Decode the response of the Nuki Web API to individual serializable SmartLock-related key-value pairs.
     *
     * @param key   current key value, is discarded because it's not set by the REST connector.
     * @param value domain object holding the result of the Nuki Web API
     * @return list of individual KeyValue pairs, adapted to relevant and serializable data
     */
    public static List<KeyValue<String, SerializableSmartLock>> toSmartLockKeyValueList(String key, NukiRestApiResponse value) {

        return value.getLocks()
                .stream()
                .map(smartLock -> {
                    SerializableSmartLock newValue = smartLock.toSerializableSmartLock();
                    String newKey = String.format("%d/%d:%d", smartLock.getAccountId(), smartLock.getSmartlockId(), newValue.getUpdateEpochSeconds());
                    return KeyValue.pair(newKey, newValue);
                }).collect(Collectors.toList());
    }

    /**
     * Map the result of the Nuki Web API to a the state of the alerting system (enabled / disabled) for a accountId.
     * <p>
     * The system is disabled for a given account if any lock of that account is unlocked.
     * </p>
     *
     * @param key           current key value, is discarded because it's not set by the REST connector.
     * @param restApiResult domain object holding the result of the Nuki Web API
     * @return list of {@link SmartLock}-instances
     * @see BurglarAlertState for all integer values and the corresponding state.
     */
    public static KeyValue<String, Integer> alertingEnabledMapper(String key, NukiRestApiResponse restApiResult) {
        boolean disabled = restApiResult.getLocks()
                .stream()
                .map(SmartLock::toSerializableSmartLock)
                .anyMatch(lock -> LockStates.UNLOCKED.equals(LockStates.of(lock.getState())));
        KeyValue<String, Integer> result = KeyValue.pair(ALERT_STATE, disabled ? BurglarAlertState.SEND_MESSAGES_DISABLED.getValue() : BurglarAlertState.SEND_MESSAGES_ENABLED.getValue());

        return result;
    }

}
