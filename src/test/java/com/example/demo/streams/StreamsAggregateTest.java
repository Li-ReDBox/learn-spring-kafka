package com.example.demo.streams;

// Reference:
// https://developer.confluent.io/courses/kafka-streams/testing/
// git clone https://github.com/confluentinc/learn-kafka-courses.git
// cd learn-kafka-courses/kafka-streams

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;

import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.data.Breed;
import com.example.data.Cat;

class StreamsAggregateTest {
        private static Serde<String> STRING_SERDE = Serdes.String();

    @Test
    void shouldAggregateRecords() {

        Properties streamsProps = new Properties();
        streamsProps.put(StreamsConfig.APPLICATION_ID_CONFIG, "aggregate-test");
        streamsProps.put("schema.registry.url", "mock://aggregation-test");

        String inputTopicName = "input";
        String outputTopicName = "output";
        Map<String, Object> configMap =
                StreamsUtils.propertiesToMap(streamsProps);

        SpecificAvroSerde<Cat> catSerde =
                StreamsUtils.getSpecificAvroSerde(configMap);

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, Cat> breedStream =
                builder.stream(inputTopicName, Consumed.with(STRING_SERDE, catSerde));
/*
https://kafka.apache.org/36/javadoc/org/apache/kafka/streams/kstream/KStream.html#groupBy(org.apache.kafka.streams.kstream.KeyValueMapper,org.apache.kafka.streams.kstream.Grouped)
<KR> KGroupedStream<KR,V> groupBy(KeyValueMapper<? super K,? super V,KR> selector,
                                  Grouped<KR,V> grouped)
Group the records of this KStream on a new key that is selected using the provided KeyValueMapper and Serdes as specified by Grouped.
Grouping a stream on the record key is required before an aggregation operator can be applied to the data (cf. KGroupedStream).
The KeyValueMapper selects a new key (which may or may not be of the same type) while preserving the original values. 
If the new record key is null the record will not be included in the resulting KGroupedStream.
Because a new key is selected, an internal repartitioning topic may need to be created in Kafka if a later operator depends on the newly selected key. 
This topic will be named "${applicationId}-<name>-repartition", where "applicationId" is user-specified in StreamsConfig via parameter APPLICATION_ID_CONFIG, 
"<name>" is either provided via Grouped.as(String) or an internally generated name.

You can retrieve all generated internal topic names via Topology.describe().

All data of this stream will be redistributed through the repartitioning topic by writing all records to it, and rereading all records from it, such that the resulting KGroupedStream is partitioned on the new key.

This operation is equivalent to calling selectKey(KeyValueMapper) followed by groupByKey().

Type Parameters:
KR - the key type of the result KGroupedStream
Parameters:
selector - a KeyValueMapper that computes a new key for grouping
grouped - the Grouped instance used to specify Serdes and part of the name for a repartition topic if repartitioning is required.
Returns:
a KGroupedStream that contains the grouped records of the original KStream
 */                          
       KTable<String, Long> breadCounts = breedStream
       // map is required to transform value of Cat to a String type before groupBy. groupBy does not types of key or value, no transformation.
        .map((key, cat) ->  KeyValue.pair(cat.getBreed().name(), cat.getName()))
            .groupByKey(Grouped.with(STRING_SERDE, STRING_SERDE))
            .count();

        // Debug a KTable<String, Long>:
        breadCounts.toStream().foreach((breed, count) -> System.out.println("Breed: " + breed + " -> " + count));
        // In production:
        // wordCounts.toStream().to(outputTopic, Produced.with(Serdes.String(), Serdes.Long()));
        // or simply:
        breadCounts.toStream().to(outputTopicName);

        // try-with-resources statement ensures that each resource is closed at the end of the statement,
        // so we don't need to call testDriver.Close() which is required to clean state store.
        // https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
        try ( TopologyTestDriver testDriver = new TopologyTestDriver(builder.build(), streamsProps)) {

            TestInputTopic<String, Cat> inputTopic =
                    testDriver.createInputTopic(inputTopicName,
                            STRING_SERDE.serializer(),
                            catSerde.serializer());
            TestOutputTopic<String, Long> outputTopic =
                    testDriver.createOutputTopic(outputTopicName,
                            STRING_SERDE.deserializer(),
                            new LongDeserializer());

            List<Cat> cats = new ArrayList<>();
            cats.add(Cat.newBuilder().setBreed(Breed.ABYSSINIAN).setName("Cat1").build());
            cats.add(Cat.newBuilder().setBreed(Breed.BIRMAN).setName("Cat3").build());
            cats.add(Cat.newBuilder().setBreed(Breed.ABYSSINIAN).setName("Cat2").build());

            cats.forEach(cat -> inputTopic.pipeInput(null, cat));
        
            assertThat(outputTopic.readKeyValuesToList())
                .containsExactly(
                    KeyValue.pair("ABYSSINIAN", 1L),
                    KeyValue.pair("BIRMAN", 1L),
                    KeyValue.pair("ABYSSINIAN", 2L)
                );
        }
    }
}