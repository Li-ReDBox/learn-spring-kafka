package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KProducer {
    // https://github.com/eugenp/tutorials/blob/master/spring-kafka/src/main/java/com/baeldung/kafka/embedded/KafkaProducer.java
    private static final Logger LOGGER = LoggerFactory.getLogger(KProducer.class);

    @Autowired
    private KafkaTemplate<String, String> template;

    public void send(String topic, String msg) {
        LOGGER.info("sending payload={} to topic={}", msg, topic);
        template.send(topic, msg);
    }
}
