package com.example.demo.streams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;

import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WordCountProcessorTest {
    private WordCountProcessor wordCountProcessor;

    @BeforeEach
    void setUp() {
        wordCountProcessor = new WordCountProcessor();
    }

    @Test
    void wordCounter() {
        StreamsBuilder streamsBuilder = new StreamsBuilder();
        wordCountProcessor.buildPipeline(streamsBuilder);
        Topology topology = streamsBuilder.build();

        try (TopologyTestDriver topologyTestDriver = new TopologyTestDriver(topology, new Properties())) {

            TestInputTopic<String, String> inputTopic = topologyTestDriver
                .createInputTopic("input-topic", new StringSerializer(), new StringSerializer());

            TestOutputTopic<String, Long> outputTopic = topologyTestDriver
                .createOutputTopic("output-topic", new StringDeserializer(), new LongDeserializer());

            inputTopic.pipeInput("key", "hello world");
            inputTopic.pipeInput("key2", "hello");

            assertThat(outputTopic.readKeyValuesToList())
                .containsExactly(
                    KeyValue.pair("hello", 1L),
                    KeyValue.pair("world", 1L),
                    KeyValue.pair("hello", 2L)
                );
        }
    }

    @Test
    void multipleProcessors() {
        StreamsBuilder streamsBuilder = new StreamsBuilder();
        wordCountProcessor.buildPipeline(streamsBuilder);
        wordCountProcessor.buildPipeline2(streamsBuilder);
        Topology topology = streamsBuilder.build();

        // when a topology does not have Produced<>, a DEFAULT_VALUE_SERDE_CLASS_CONFIG is needed, otherwise
        // empty Properties is acceptable.
        try (TopologyTestDriver topologyTestDriver = new TopologyTestDriver(topology, new Properties())) {
           TestInputTopic<String, String> inputTopic = topologyTestDriver
                .createInputTopic("input-topic", new StringSerializer(), new StringSerializer());

            TestOutputTopic<String, Long> outputTopic = topologyTestDriver
                .createOutputTopic("output-topic", new StringDeserializer(), new LongDeserializer());

            inputTopic.pipeInput(null, "hello world");
            inputTopic.pipeInput(null, "hello");

            assertThat(outputTopic.readKeyValuesToList())
                .containsExactly(
                    KeyValue.pair("hello", 1L),
                    KeyValue.pair("world", 1L),
                    KeyValue.pair("hello", 2L)
                );

            TestInputTopic<String, String> inputTopic2 = topologyTestDriver
                .createInputTopic("input-topic-2", new StringSerializer(), new StringSerializer());

            TestOutputTopic<String, String> outputTopic2 = topologyTestDriver
                .createOutputTopic("output-topic-2", new StringDeserializer(), new StringDeserializer());

            inputTopic2.pipeInput("key", "Hello World");
            assertEquals("hello world", outputTopic2.readValue());
        } 
    }

}