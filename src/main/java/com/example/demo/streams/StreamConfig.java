package com.example.demo.streams;

import static org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG;
// import static org.apache.kafka.streams.StreamsConfig.STATE_DIR_CONFIG;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableKafka
@EnableKafkaStreams
public class StreamConfig {

    @Value(value = "${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapAddress;

    // Be careful to set stateStoreLocation. If I set this, EmbeddedKafkaIntegrationTest will fail to launch beans then tests.
    // @Value(value = "${spring.kafka.streams.state.dir:/tmp/kafka}")
    // private String stateStoreLocation;

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    KafkaStreamsConfiguration kStreamsConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(APPLICATION_ID_CONFIG, "streams-app");
        props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        // This is questionable: by default, Kafka manage each application differently, this may cause everything store together, not clean??
        // Example of auto generated: Resolved state.dir: [/var/folders/9d/b2326jv513vglf41k5fz2hmc0000gq/T//kafka-streams
        // configure the state location to allow tests to use clean state for every run
        // props.put(STATE_DIR_CONFIG, stateStoreLocation);

        return new KafkaStreamsConfiguration(props);
    }

    @Bean
    NewTopic inputTopic() {
        return TopicBuilder.name("input-topic")
            .partitions(1)
            .replicas(1)
            .build();
    }

}