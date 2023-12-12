package com.example.demo;

import java.util.concurrent.CountDownLatch;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/*
 * KConsumer uses the @KafkaListener annotation on the receive method to listen to messages on a given topic.
 * We’ll see later how we configure the test.topic from our tests.
 *
 * Furthermore, the receive method stores the message content in our bean and decrements the count of the latch variable.
 * This variable is a simple thread-safe counter field that we’ll use later from our tests to ensure we successfully received a message.
 */
@Slf4j
@Component
public class KConsumer {
    private CountDownLatch latch = new CountDownLatch(1);

    private String payload;

    @KafkaListener(topics = "${test.topic}")
    public void receive(ConsumerRecord<?, ?> consumerRecord) {
        log.info("received payload='{}'", consumerRecord);
        payload = consumerRecord.toString();
        latch.countDown();
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public void resetLatch() {
        latch = new CountDownLatch(1);
    }

    public String getPayload() {
        return payload;
    }
}
