package com.example.demo;

/*
 * https://blog.mimacom.com/embeddedkafka-kafka-auto-configure-springboottest-bootstrapserversproperty/
 * Unique consumer group.ids
Whenever there is more than one consumer, no matter whether created explicitly or indirectly by a @KafkaListener, each consumer's group.id has to be specified in order to be unique. There are plenty other ways possible, but it can be achieved for example like this:

@KafkaListener(..., groupId = "unique-listener-group-id")

or

consumerFactory.createConsumer("unique-consumer-group-id", null);

By no means is this meant to comprehensively explain consumer groups, just as a potentially useful hint.

Applying

@TestInstance(Lifecycle.PER_CLASS)

to the test classes prevents JUnit from instantiating for each @Test method another instance of the test class and in turn Spring instantiating another context including each time another @KafkaListener the always same group.id of which then not any longer being unique because always based on the very same configuration (obviously except when the test class has not more than one @Test method).

auto.offset.reset = earliest
Also useful might be:

@TestPropertySource(properties = "spring.kafka.consumer.auto-offset-reset = earliest")
 */
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(
    bootstrapServersProperty = "spring.kafka.bootstrap-servers",
    topics = EmbeddedKafkaTest.TOPIC_NAME
)
@TestPropertySource(properties = "spring.kafka.consumer.auto-offset-reset = earliest")
@TestInstance(Lifecycle.PER_CLASS)
class EmbeddedKafkaTest {

    @Configuration
    @EnableAutoConfiguration
    static class TestConfiguration {
    }

    static final String TOPIC_NAME = "topic";

    @Autowired
    private KafkaAdmin admin;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    @Autowired
    private ProducerFactory<String, String> producerFactory;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private BlockingQueue<ConsumerRecord<String, String>> consumptionQueue = new LinkedBlockingDeque<>();

    @KafkaListener(topics = TOPIC_NAME, groupId = "listener")
    private void listen(ConsumerRecord<String, String> consumerRecord) throws InterruptedException {
        consumptionQueue.put(consumerRecord);
    }

    @Test
    void testProducerAndConsumer() throws Exception {
        final String KEY = "key1", VALUE = "value1";
        try (
            Consumer<String, String> consumer = consumerFactory.createConsumer("consumer", null);
            Producer<String, String> producer = producerFactory.createProducer();
        ) {
            consumer.subscribe(asList(TOPIC_NAME));

            producer.send(new ProducerRecord<>(TOPIC_NAME, KEY, VALUE), (metadata, exception) -> {
            }).get();
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(3));

            assertThat(records).singleElement().satisfies(singleRecord -> {
                assertThat(singleRecord.key()).isEqualTo(KEY);
                assertThat(singleRecord.value()).isEqualTo(VALUE);
            });
            consumer.commitSync();
            consumer.unsubscribe();
        }
    }

    @Test
    void testTemplateAndListener() throws Exception {
        final String KEY = "key2", VALUE = "value2";
        consumptionQueue.clear();

        kafkaTemplate.send(TOPIC_NAME, KEY, VALUE).get();
        ConsumerRecord<String, String> consumerRecord = consumptionQueue.poll(3, TimeUnit.SECONDS);

        assertThat(consumerRecord.key()).isEqualTo(KEY);
        assertThat(consumerRecord.value()).isEqualTo(VALUE);
        assertThat(consumptionQueue).isEmpty();
    }

    @Test
    void checkBootstrapServersParameterResolutionExample(
        @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
        @Autowired EmbeddedKafkaBroker broker
    ) throws Exception {
        assertThat(broker.getBrokersAsString()).isEqualTo(bootstrapServers);
    }

    @Test
    void testAdmin() {
        assertThat(admin.describeTopics(TOPIC_NAME)).containsKey(TOPIC_NAME);
    }

}