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
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    // Baeldung says: alternately, we can also create a bean in the configuration class to generate the topology. How?
    @Autowired
    void buildPipeline2(StreamsBuilder streamsBuilder) {
        KStream<String, String> messageStream = streamsBuilder
            .stream("input-topic-2", Consumed.with(STRING_SERDE, STRING_SERDE));

        messageStream
            .mapValues((ValueMapper<String, String>) String::toLowerCase)
            .to("output-topic-2", Produced.with(Serdes.String(), Serdes.String()));

        // Debug a KTable<String, Long>:
        // wordCounts.toStream().foreach((word, count) -> System.out.println("word: " + word + " -> " + count));
        // In production:
        // wordCounts.toStream().to(outputTopic, Produced.with(Serdes.String(), Serdes.Long()));
        // or simply:
    }
}