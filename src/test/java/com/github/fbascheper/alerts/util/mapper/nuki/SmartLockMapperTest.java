package com.github.fbascheper.alerts.util.mapper.nuki;

import com.github.fbascheper.alerts.model.avro.SerializableSmartLock;
import com.github.fbascheper.alerts.model.nuki.LockStates;
import com.github.fbascheper.alerts.model.nuki.NukiRestApiResponse;
import com.github.fbascheper.alerts.model.state.BurglarAlertState;
import com.github.fbascheper.alerts.util.common.DateUtils;
import org.apache.kafka.streams.KeyValue;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Scanner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Test class for {@link SmartLockMapper}.
 *
 * @author Erik-Berndt Scheper
 * @since 02-11-2018
 */
public class SmartLockMapperTest {

    @Test
    public void testSmartLockKeyValueMapper1() throws Exception {

        String json = new Scanner(SmartLockMapperTest.class.getClassLoader().getResourceAsStream("n1.json"), "UTF-8").useDelimiter("\\A").next();

        NukiRestApiResponse restApiResult = SmartLockMapper.toNukiResponse(json);
        List<KeyValue<String, SerializableSmartLock>> keyValues =
                SmartLockMapper.toSmartLockKeyValueList(null, restApiResult);
        assertThat(keyValues.size(), is(1));

        KeyValue<String, SerializableSmartLock> keyValue = keyValues.get(0);
        String key = keyValue.key;
        assertThat(key, is("345/234:1536160393"));

        SerializableSmartLock smartLock = keyValue.value;
        assertThat(smartLock, notNullValue());
        assertThat(smartLock, notNullValue());
        assertThat(smartLock.getName(), is("Test name"));
        assertThat(DateUtils.ofEpochSecond(smartLock.getUpdateEpochSeconds(), DateUtils.ZONE_ID_EUROPE_AMSTERDAM),
                is(ZonedDateTime.of(2018, 9, 5, 17, 13, 13, 0, DateUtils.ZONE_ID_EUROPE_AMSTERDAM)));

        assertThat(smartLock.getState(), is(LockStates.UNLOCKED.getValue()));

        KeyValue<String, Integer> alertState = SmartLockMapper.alertingEnabledMapper(null, restApiResult);
        assertThat(alertState, is(KeyValue.pair("state", BurglarAlertState.SEND_MESSAGES_DISABLED.getValue())));
    }

    @Test
    public void testSmartLockKeyValueMapper2() throws Exception {

        String json = new Scanner(SmartLockMapperTest.class.getClassLoader().getResourceAsStream("n2.json"), "UTF-8").useDelimiter("\\A").next();

        NukiRestApiResponse restApiResult = SmartLockMapper.toNukiResponse(json);
        List<KeyValue<String, SerializableSmartLock>> keyValues =
                SmartLockMapper.toSmartLockKeyValueList(null, restApiResult);
        assertThat(keyValues.size(), is(1));

        KeyValue<String, SerializableSmartLock> keyValue = keyValues.get(0);
        String key = keyValue.key;
        assertThat(key, is("345/234:1541348348"));

        SerializableSmartLock smartLock = keyValue.value;
        assertThat(smartLock, notNullValue());
        assertThat(smartLock, notNullValue());
        assertThat(smartLock.getName(), is("Test name"));
        assertThat(DateUtils.ofEpochSecond(smartLock.getUpdateEpochSeconds(), DateUtils.ZONE_ID_EUROPE_AMSTERDAM),
                is(ZonedDateTime.of(2018, 11, 4, 17, 19, 8, 0, DateUtils.ZONE_ID_EUROPE_AMSTERDAM)));

        assertThat(smartLock.getState(), is(LockStates.LOCKED.getValue()));

        KeyValue<String, Integer> alertState = SmartLockMapper.alertingEnabledMapper(null, restApiResult);
        assertThat(alertState, is(KeyValue.pair("state", BurglarAlertState.SEND_MESSAGES_ENABLED.getValue())));
    }

}
