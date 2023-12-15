package com.example.demo;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.streams.StreamsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

/*
 * In general, when writing clean integration tests, we shouldn’t depend on external services that we might not
 * be able to control or might suddenly stop working. This could have adverse effects on our test results.
 *
 * Similarly, if we’re dependent on an external service, in this case, a running Kafka broker, we likely won’t
 * be able to set it up, control it and tear it down in the way we want from our tests.
 *
 * The consumer property auto-offset-reset: earliest. This property ensures that our consumer group gets the messages
 * we send because the container might start after the sends have completed.
 *
 * Embedded Kafka: an in-memory Kafka instance to run our tests against. It contains the EmbeddedKafkaBroker class.
 *
 * The @SpringBootTest annotation will ensure that our test bootstraps the Spring application context.
 * I have to use @SpringBootTest instead of @ExtendWith(SpringExtension.class) because the beans in the main packages need to be injected.
 * @ExtendWith(SpringExtension.class) seems did not scan all beans in the main package (non-test packages). Need to confirm.
 * The @DirtiesContext annotation, which will make sure this context is cleaned and reset between different tests.
 * The @EmbeddedKafka annotation to inject an instance of an EmbeddedKafkaBroker into our tests.
 */
// static class KafkaStreamsConfiguration is needed and it is weird. The main package certainly has such a Bean, but, maybe because
// StreamsConfig.BOOTSTRAP_SERVERS_CONFIG in the EmbeddedKafka class should be used in a testing environment, otherwise there will be
// two apps share the same STATE_DIR_CONFIG it throws exception about sharing state which is strange given this is not a stream application.
// Even more strange, when there is an actual Kafka, it is fine running with other tests.
// Certainly need to learn more to get this solved.
@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1)
class EmbeddedKafkaIntegrationTest {
    // https://github.com/eugenp/tutorials/blob/master/spring-kafka/src/test/java/com/baeldung/kafka/embedded/EmbeddedKafkaIntegrationTest.java

    @Autowired
    public KafkaTemplate<String, String> template;

    @Autowired
    private KConsumer consumer;

    @Autowired
    private KProducer producer;

    @Value("${test.topic}")
    private String topic;

    @TestConfiguration
    @EnableKafkaStreams
    public static class KafkaStreamsConfiguration {
        // This is important part of setting up because there is a KafkaStreamsDefaultConfiguration which may
        // does not work with EmbeddedKafka: is the broker the cause?
        @Value("${" + EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS + "}")
        private String brokerAddresses;

        @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
        public StreamsConfig kStreamsConfigs() {
            final Map<String, Object> props = new HashMap<>();
            props.put(StreamsConfig.APPLICATION_ID_CONFIG, "testStreams");
            props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, this.brokerAddresses);
            return new StreamsConfig(props);
        }
    }

    @BeforeEach
    void setup() {
        consumer.resetLatch();
    }

    @Test
    void tlpSendAndReceive() throws Exception {
        String data = "Sending with default template";

        template.send(topic, data);

        boolean messageConsumed = consumer.getLatch()
          .await(10, TimeUnit.SECONDS);
        assertThat(consumer.getPayload(), containsString(data));
        assertTrue(messageConsumed);
    }

    @Test
    void pSendAndReceive() throws Exception {
        String data = "Sending with our own simple KafkaProducer";

        producer.send(topic, data);

        boolean messageConsumed = consumer.getLatch()
          .await(10, TimeUnit.SECONDS);
        assertThat(consumer.getPayload(), containsString(data));
        assertTrue(messageConsumed);
    }
}
