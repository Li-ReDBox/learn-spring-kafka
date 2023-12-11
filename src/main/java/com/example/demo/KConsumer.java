package com.example.demo;

import java.util.concurrent.CountDownLatch;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KConsumer.class);

    private CountDownLatch latch = new CountDownLatch(1);
    
    private String payload;

    @KafkaListener(topics = "mytopic", groupId = "test")
    public void receive(ConsumerRecord<?, ?> consumerRecord) {
        LOGGER.info("received payload='{}'", consumerRecord);
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
