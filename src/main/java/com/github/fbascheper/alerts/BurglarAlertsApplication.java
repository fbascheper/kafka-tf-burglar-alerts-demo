package com.github.fbascheper.alerts;

import com.github.fbascheper.alerts.model.avro.SerializableImage;
import com.github.fbascheper.alerts.model.avro.SerializableSmartLock;
import com.github.fbascheper.alerts.model.nuki.NukiRestApiResponse;
import com.github.fbascheper.alerts.util.common.FileUtils;
import com.github.fbascheper.alerts.util.kafka.KafkaStreamsConfig;
import com.github.fbascheper.alerts.util.kafka.KafkaTopic;
import com.github.fbascheper.alerts.util.mapper.nuki.SmartLockMapper;
import com.github.fbascheper.alerts.util.mapper.telegram.TelegramMessageMapper;
import com.github.fbascheper.alerts.util.tensorflow.TensorFlowMatcher;
import com.github.fbascheper.kafka.connect.telegram.TgMessage;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import lombok.val;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.github.fbascheper.alerts.util.mapper.nuki.SmartLockMapper.ALERT_STATE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Kafka Streams based burglar alerts application.
 *
 * @author Erik-Berndt Scheper
 * @since 02-11-2018
 */
public class BurglarAlertsApplication {
    private static final Logger LOGGER = getLogger(BurglarAlertsApplication.class);

    private static final String SOURCE_TOPIC_CAMERA_IMAGES = KafkaTopic.IP_CAMERA_IMAGE.getName();
    private static final String SOURCE_TOPIC_SMARTLOCK_POLL = KafkaTopic.NUKI_REST_API_RESULT.getName();

    private static final String SINK_TOPIC_BURGLAR_ALERTS = KafkaTopic.TELEGRAM_BURGLAR_ALERT.getName();

    // topics used to post data for our global KTables
    private static final String BURGLAR_ALERTING_STATE_TOPIC = KafkaTopic.ALERTING_ENABLED_STATE.getName();
    private static final String BURGLAR_ALERTING_STATE_STORE = "burglar-alerting-state-store";

    @SuppressWarnings("Duplicates")
    public static void main(final String[] args) {

        // Create TensorFlow objects
        List<String> tfLabels = FileUtils.readLines("tensorflow/demo/CNN_inception5h/imagenet_comp_graph_label_strings.txt");
        byte[] tfGgraphDef = FileUtils.readFile("tensorflow/demo/CNN_inception5h/tensorflow_inception_graph.pb");

        Properties streamsConfiguration = KafkaStreamsConfig.buildStreamsConfiguration(
                "tf-burglar-alerts", " /tmp",
                KafkaStreamsConfig.KAFKA_BOOTSTRAP_SERVERS,
                KafkaStreamsConfig.KAFKA_SCHEMA_REGISTRY_URL);

        KafkaStreams streams = createStreams(
                streamsConfiguration, KafkaStreamsConfig.KAFKA_SCHEMA_REGISTRY_URL, tfLabels, tfGgraphDef);

        streams.cleanUp();
        // start processing
        streams.start();
        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private static KafkaStreams createStreams(Properties streamsConfiguration,
                                              String schemaRegistryUrl,
                                              List<String> tfLabels,
                                              byte[] tfGraphDef) {

        // create and configure Serdes required
        Map<String, String> serdeConfig =
                Collections.singletonMap(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);

        SpecificAvroSerde<SerializableSmartLock> smartLockSerde = new SpecificAvroSerde<>();
        smartLockSerde.configure(serdeConfig, false);

        SpecificAvroSerde<TgMessage> telegramMessageSerde = new SpecificAvroSerde<>();
        telegramMessageSerde.configure(serdeConfig, false);

        // In the subsequent lines we define the processing topology of the Streams application.
        StreamsBuilder builder = new StreamsBuilder();

        // ----------------------------------------------------------------------------------------------------
        // Construct a KStream from the camera images topic using (filename, image) as key,value pair.
        val cameraSourceStream = builder
                .stream(SOURCE_TOPIC_CAMERA_IMAGES, Consumed.with(Serdes.String(), Serdes.ByteArray()));

        // ----------------------------------------------------------------------------------------------------
        // Construct KStream with SmartLock data from REST Polls to the Nuki Web API
        //
        // - map json response from Nuki API call to to a domain object
        // - filter all values with empty value (potential errors)
        KStream<String, NukiRestApiResponse> nukiRestApiResponseStream =
                builder.stream(SOURCE_TOPIC_SMARTLOCK_POLL, Consumed.with(Serdes.String(), Serdes.String()))
                        .mapValues(SmartLockMapper::toNukiResponse)
                        .filter((key, value) -> value != null);

        // ----------------------------------------------------------------------------------------------------
        // Determine current state of alerting (enabled, disabled) based on the result of the last REST poll
        KStream<String, Integer> alertingEnabledStateStream =
                nukiRestApiResponseStream.map(SmartLockMapper::alertingEnabledMapper);

        // Send this to a KStream
        alertingEnabledStateStream.to(BURGLAR_ALERTING_STATE_TOPIC, Produced.with(Serdes.String(), Serdes.Integer()));

        // Create a global table containing the current state of alerting (enabled, disabled).
        // The data from this global table will be fully replicated on each instance of this application.
        val alertingEnabledGlobalKTable =
                builder.globalTable(BURGLAR_ALERTING_STATE_TOPIC, Materialized.<String, Integer, KeyValueStore<Bytes, byte[]>>as(BURGLAR_ALERTING_STATE_STORE)
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Integer()));

        // Filter out all images when alert state is disabled (i.e. any lock is open)
        val filteredSourceStream = cameraSourceStream
                .leftJoin(alertingEnabledGlobalKTable,
                        (filename, image) -> ALERT_STATE,
                        (image, enabled) -> enabled.equals(-1) ? image : new byte[]{})
                .filter((key, value) -> value.length > 0);

        // Map images into Avro object for serialization
        val imageStream = filteredSourceStream
                .mapValues((readOnlyKey, value) -> new SerializableImage(readOnlyKey, ByteBuffer.wrap(value)));

        // Stream Processor applying the analytic model
        val telegramPhotoMessage = imageStream
                .mapValues((image) -> {
                    String caption = TensorFlowMatcher.matchImage(tfLabels, tfGraphDef, image);
                    LOGGER.debug(">>> Sending telegram message with caption {}", caption);
                    return TelegramMessageMapper.photoMessage(image, caption);
                });

        // Send prediction information to telegram topic (sink)
        telegramPhotoMessage.to(SINK_TOPIC_BURGLAR_ALERTS, Produced.with(Serdes.String(), telegramMessageSerde));

        return new KafkaStreams(builder.build(), streamsConfiguration);
    }

}