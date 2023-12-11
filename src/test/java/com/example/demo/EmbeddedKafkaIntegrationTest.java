package com.example.demo;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;


@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = EmbeddedKafkaIntegrationTest.topic)
class EmbeddedKafkaIntegrationTest {
    // https://github.com/eugenp/tutorials/blob/master/spring-kafka/src/test/java/com/baeldung/kafka/embedded/EmbeddedKafkaIntegrationTest.java

    //FIXME: this cannot be run with another mbeddedKafkaTest!!! It has to be something to do with a collision between two runs.
    @Autowired
    public KafkaTemplate<String, String> template;

    @Autowired
    private KConsumer consumer;

    @Autowired
    private KProducer producer;

    static final String topic = "mytopic";

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