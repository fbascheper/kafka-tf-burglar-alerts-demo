package com.github.fbascheper.alerts;

import com.github.fbascheper.alerts.model.avro.SerializableSmartLock;
import com.github.fbascheper.alerts.model.nuki.LockStates;
import com.github.fbascheper.alerts.model.nuki.NukiRestApiResponse;
import com.github.fbascheper.alerts.util.common.DateUtils;
import com.github.fbascheper.alerts.util.kafka.KafkaStreamsConfig;
import com.github.fbascheper.alerts.util.kafka.KafkaTopic;
import com.github.fbascheper.alerts.util.mapper.nuki.SmartLockMapper;
import com.github.fbascheper.alerts.util.mapper.telegram.TelegramMessageMapper;
import com.github.fbascheper.kafka.connect.telegram.TgMessage;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Kafka Streams based burglar alerts application.
 *
 * @author Erik-Berndt Scheper
 * @since 05-11-2018
 */
public class DemoAlertApplication2NukiLockChangeAlert {
    private static final Logger LOGGER = getLogger(DemoAlertApplication2NukiLockChangeAlert.class);

    private static final String SOURCE_TOPIC_SMARTLOCK_POLL = KafkaTopic.NUKI_REST_API_RESULT.getName();
    private static final String SINK_TOPIC_BURGLAR_ALERTS = KafkaTopic.TELEGRAM_BURGLAR_ALERT.getName();

    public static void main(String[] args) {
        Properties streamsConfiguration = KafkaStreamsConfig.buildStreamsConfiguration(
                "tf-burglar-locks", " /tmp",
                KafkaStreamsConfig.KAFKA_BOOTSTRAP_SERVERS,
                KafkaStreamsConfig.KAFKA_SCHEMA_REGISTRY_URL);

        KafkaStreams streams = createStreams(
                streamsConfiguration, KafkaStreamsConfig.KAFKA_SCHEMA_REGISTRY_URL);

        streams.cleanUp();
        // start processing
        streams.start();
        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private static KafkaStreams createStreams(Properties streamsConfiguration,
                                              String schemaRegistryUrl) {

        // create and configure Serdes required
        Map<String, String> serdeConfig =
                Collections.singletonMap(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);

        SpecificAvroSerde<SerializableSmartLock> smartLockSerde = new SpecificAvroSerde<>();
        smartLockSerde.configure(serdeConfig, false);

        SpecificAvroSerde<TgMessage> telegramMessageSerde = new SpecificAvroSerde<>();
        telegramMessageSerde.configure(serdeConfig, false);

        StreamsBuilder builder = new StreamsBuilder();

        // ----------------------------------------------------------------------------------------------------
        // Construct KStream with SmartLock data from REST Polls to the Nuki Web API
        //
        // - map json response from Nuki API call to to a domain object
        // - filter all values with empty value (potential errors)
        KStream<String, NukiRestApiResponse> nukiRestApiResponseStream =
                builder.stream(SOURCE_TOPIC_SMARTLOCK_POLL, Consumed.with(Serdes.String(), Serdes.String()))
                        .mapValues(SmartLockMapper::toNukiResponse)
                        .filter((key, value) -> value != null);

        // - map the Nuki response (holding a list of locks) to a list of key-value pairs
        // - flatten this back into a stream of key-value pairs
        // key   == smartLockAccountId / smartlockId : smartLockUpdateEpochSeconds
        // value == SmartLock data (avro-serializable)
        KStream<String, SerializableSmartLock> smartLockPollStream =
                nukiRestApiResponseStream.flatMap(SmartLockMapper::toSmartLockKeyValueList);

        // ----------------------------------------------------------------------------------------------------
        // Send the updates of all lock states as messages to telegram.
        //
        // 1. Reduce the poll results into a KTable with all updates
        KTable<String, SerializableSmartLock> smartLockUpdatesTable =
                smartLockPollStream
                        .groupByKey(Serialized.with(Serdes.String(), smartLockSerde))
                        .reduce((aggValue, newValue) -> {
                            LOGGER.debug("    --- Dropping SmartLock {}/{} ({}) updated at {}, with state = {}",
                                    newValue.getAccountId(),
                                    newValue.getSmartlockId(),
                                    newValue.getName(),
                                    DateUtils.ofEpochSecond(newValue.getUpdateEpochSeconds(), DateUtils.ZONE_ID_EUROPE_AMSTERDAM).toString(),
                                    LockStates.of(newValue.getState()).name());
                            return newValue;
                        });

        // 2. Convert the changelog table into a stream of telegram text messages
        KStream<String, TgMessage> telegramTextMessage = smartLockUpdatesTable
                .toStream()
                .mapValues((readOnlyKey, lock) -> {
                    String message = String.format("Stream key = %s --> SmartLock %d/%d (%s) updated at %s, new state = %s",
                            readOnlyKey,
                            lock.getAccountId(),
                            lock.getSmartlockId(),
                            lock.getName(),
                            DateUtils.ofEpochSecond(lock.getUpdateEpochSeconds(), DateUtils.ZONE_ID_EUROPE_AMSTERDAM).toString(),
                            LockStates.of(lock.getState()).name());
                    LOGGER.debug(">>> Sending telegram message {}", message);

                    return TelegramMessageMapper.textMessage(message);
                });

        // 3. Send this to the telegram sink.
        telegramTextMessage.to(SINK_TOPIC_BURGLAR_ALERTS, Produced.with(Serdes.String(), telegramMessageSerde));

        // ----------------------------------------------------------------------------------------------------
        // RESULT of the above:
        // - Some duplicate messages will be dropped
        // - You will receive updates of your lock state
        // - But... every now and then you will get a (duplicate) telegram message with the current state
        //
        //   ---> It's a streaming system after all and the grouping is for a given period in time ...
        // ----------------------------------------------------------------------------------------------------

        /*
[2018-11-09 12:36:19,665] DEBUG [tf-burglar-locks-client-StreamThread-1]     --- Dropping SmartLock 999999999/999999999 (Lock name) updated at 2018-11-09T12:09:05+01:00[Europe/Amsterdam], with state = LOCKED (com.github.fbascheper.alerts.NukiLockChangeAlertExample)
[2018-11-09 12:36:19,665] DEBUG [tf-burglar-locks-client-StreamThread-1]     --- Dropping SmartLock 999999999/999999999 (Lock name) updated at 2018-11-09T12:09:05+01:00[Europe/Amsterdam], with state = LOCKED (com.github.fbascheper.alerts.NukiLockChangeAlertExample)
[2018-11-09 12:36:19,665] DEBUG [tf-burglar-locks-client-StreamThread-1]     --- Dropping SmartLock 999999999/999999999 (Lock name) updated at 2018-11-09T12:09:05+01:00[Europe/Amsterdam], with state = LOCKED (com.github.fbascheper.alerts.NukiLockChangeAlertExample)
[2018-11-09 12:36:19,666] DEBUG [tf-burglar-locks-client-StreamThread-1]     --- Dropping SmartLock 999999999/999999999 (Lock name) updated at 2018-11-09T12:09:05+01:00[Europe/Amsterdam], with state = LOCKED (com.github.fbascheper.alerts.NukiLockChangeAlertExample)
[2018-11-09 12:36:21,808] DEBUG [tf-burglar-locks-client-StreamThread-1]     --- Dropping SmartLock 999999999/999999999 (Lock name) updated at 2018-11-09T12:09:05+01:00[Europe/Amsterdam], with state = LOCKED (com.github.fbascheper.alerts.NukiLockChangeAlertExample)
[2018-11-09 12:36:49,100] DEBUG [tf-burglar-locks-client-StreamThread-1] >>> Sending telegram message Stream key = 999999999/999999999:999999999 --> SmartLock 999999999/999999999 (Lock name) updated at 2018-11-09T12:09:05+01:00[Europe/Amsterdam], new state = LOCKED (com.github.fbascheper.alerts.NukiLockChangeAlertExample)
[2018-11-09 12:37:49,258] DEBUG [tf-burglar-locks-client-StreamThread-1] >>> Sending telegram message Stream key = 999999999/999999999:999999999 --> SmartLock 999999999/999999999 (Lock name) updated at 2018-11-09T12:36:57+01:00[Europe/Amsterdam], new state = UNLOCKED (com.github.fbascheper.alerts.NukiLockChangeAlertExample)
[2018-11-09 12:38:22,094] DEBUG [tf-burglar-locks-client-StreamThread-1]     --- Dropping SmartLock 999999999/999999999 (Lock name) updated at 2018-11-09T12:36:57+01:00[Europe/Amsterdam], with state = UNLOCKED (com.github.fbascheper.alerts.NukiLockChangeAlertExample)
[2018-11-09 12:38:30,752] DEBUG [tf-burglar-locks-client-StreamThread-1] >>> Sending telegram message Stream key = 999999999/999999999:999999999 --> SmartLock 999999999/999999999 (Lock name) updated at 2018-11-09T12:36:57+01:00[Europe/Amsterdam], new state = UNLOCKED (com.github.fbascheper.alerts.NukiLockChangeAlertExample)
         */

        return new KafkaStreams(builder.build(), streamsConfiguration);
    }

}
