package com.example.demo.streams;

import java.util.Arrays;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// This will not run as a component in a Springboot app but merely a setup for the tests: ./gradlew check has:
// Caused by: org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 
// 'wordCountProcessor': Unsatisfied dependency expressed through method 'buildPipeline' parameter 0: No qualifying
// bean of type 'org.apache.kafka.streams.StreamsBuilder' available: expected at least 1 bean which qualifies
//as autowire candidate. Dependency annotations: {}
@Component
public class WordCountProcessor {

    private static final Serde<String> STRING_SERDE = Serdes.String();

    @Autowired
    void buildPipeline(StreamsBuilder streamsBuilder) {
        KStream<String, String> messageStream = streamsBuilder
            .stream("input-topic", Consumed.with(STRING_SERDE, STRING_SERDE));

        KTable<String, Long> wordCounts = messageStream
            .mapValues((ValueMapper<String, String>) String::toLowerCase)
            .flatMapValues(value -> Arrays.asList(value.split("\\W+")))
            .groupBy((key, word) -> word, Grouped.with(STRING_SERDE, STRING_SERDE))
            .count(Materialized.as("counts"));

        // Debug a KTable<String, Long>:
        // wordCounts.toStream().foreach((word, count) -> System.out.println("word: " + word + " -> " + count));
        // In production:
        // wordCounts.toStream().to(outputTopic, Produced.with(Serdes.String(), Serdes.Long()));
        // or simply:
        wordCounts.toStream().to("output-topic");
    }

    @Autowired
    void buildPipeline2(StreamsBuilder streamsBuilder) {
        KStream<String, String> messageStream = streamsBuilder
            .stream("input-topic", Consumed.with(STRING_SERDE, STRING_SERDE));

        messageStream
            .mapValues((ValueMapper<String, String>) String::toLowerCase);

        // Debug a KTable<String, Long>:
        // wordCounts.toStream().foreach((word, count) -> System.out.println("word: " + word + " -> " + count));
        // In production:
        // wordCounts.toStream().to(outputTopic, Produced.with(Serdes.String(), Serdes.Long()));
        // or simply:
    }
}