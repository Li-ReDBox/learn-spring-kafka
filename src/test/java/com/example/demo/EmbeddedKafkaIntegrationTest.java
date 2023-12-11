package com.example.demo;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
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
 * The @DirtiesContext annotation, which will make sure this context is cleaned and reset between different tests.
 * The @EmbeddedKafka annotation to inject an instance of an EmbeddedKafkaBroker into our tests.
 */
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