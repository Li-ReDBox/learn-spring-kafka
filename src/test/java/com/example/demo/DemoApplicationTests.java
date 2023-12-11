package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class DemoApplicationTests {

@Autowired
    private ApplicationContext applicationContext;

    @Test
    void loadBeans() {
        assertNotNull(applicationContext.getBean(KProducer.class));
        assertNotNull(applicationContext.getBean(KConsumer.class));
    }
}
