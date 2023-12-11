package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/*
 * KProducer bean is merely a wrapper around the KafkaTemplate class. This class provides high-level thread-safe operations,
 * such as sending data to the provided topic, which is exactly what we do in our send method.
*/
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
