package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/*
 * KProducer bean is merely a wrapper around the KafkaTemplate class. This class provides high-level thread-safe operations,
 * such as sending data to the provided topic, which is exactly what we do in our send method.
*/
@Slf4j
@Component
public class KProducer {
    // https://github.com/eugenp/tutorials/blob/master/spring-kafka/src/main/java/com/baeldung/kafka/embedded/KafkaProducer.java
    @Autowired
    private KafkaTemplate<String, String> template;

    public void send(String topic, String msg) {
        log.info("sending payload={} to topic={}", msg, topic);
        template.send(topic, msg);
    }
}
