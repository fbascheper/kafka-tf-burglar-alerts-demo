package com.github.fbascheper.alerts.util.kafka;

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Configuration of the Kafka Streams application.
 *
 * @author Erik-Berndt Scheper
 * @since 02-11-2018
 */
public final class KafkaStreamsConfig {

    /**
     * Bootstrap server(s) for our Kafka instance.
     */
    public static final String KAFKA_BOOTSTRAP_SERVERS = "localhost:32081";

    /**
     * Confluent schema registry URL for our Kafka instance.
     */
    public static final String KAFKA_SCHEMA_REGISTRY_URL = "http://localhost:32281";

    private KafkaStreamsConfig() {
        // prevent instantiation
    }

    /**
     * Build the Kafka Streams configuration.
     *
     * @param applicationName   Application name
     * @param stateDir          Directory location where the state of the Kafka Streams app is saved
     * @param bootstrapServers  Kafka bootstrap servers
     * @param schemaRegistryUrl Confluent schema registry
     * @return the configuration, as a set of {@link Properties}.
     */
    public static Properties buildStreamsConfiguration(final String applicationName,
                                                       final String stateDir,
                                                       final String bootstrapServers,
                                                       final String schemaRegistryUrl) {
        final Properties streamsConfiguration = new Properties();

        // Give the Streams application a unique name.
        // The name must be unique in the Kafka cluster against which the application is run.
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationName);
        streamsConfiguration.put(StreamsConfig.CLIENT_ID_CONFIG, applicationName + "-client");

        // Where to find Kafka broker(s).
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        streamsConfiguration.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);

        streamsConfiguration.put(StreamsConfig.STATE_DIR_CONFIG, stateDir);

        // Set to earliest so we don't miss any data that arrived in the topics before the process started
        streamsConfiguration.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        streamsConfiguration.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, TimeUnit.SECONDS.toMillis(30));

        // Specify default (de)serializers for record keys and for record values.
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        return streamsConfiguration;
    }
}
