For a Kafka stream, everything is built around an instance of StreamBuilder and a streamConfig:

```java
// stream configuration
Properties streamsConfiguration = new Properties();
streamsConfiguration.put(
  StreamsConfig.APPLICATION_ID_CONFIG,
  "wordcount-live-test");
streamsConfiguration.put(
  StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
  Serdes.String().getClass().getName());
streamsConfiguration.put(
  StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
  Serdes.String().getClass().getName());
...

// define a stream: build a streaming topology
StreamsBuilder builder = new StreamsBuilder();
KStream<String, String> textLines = builder.stream(inputTopic);
Pattern pattern = Pattern.compile("\\W+", Pattern.UNICODE_CHARACTER_CLASS);

KTable<String, Long> wordCounts = textLines
  .flatMapValues(value -> Arrays.asList(pattern.split(value.toLowerCase())))
  .groupBy((key, word) -> word)
  .count();

String outputTopic = "outputTopic";
wordCounts.toStream()
  .to(outputTopic, Produced.with(Serdes.String(), Serdes.Long()));

// run stream topology
Topology topology = builder.build();
KafkaStreams streams = new KafkaStreams(topology, streamsConfiguration);
streams.start();

// wait 30 seconds for the job to finish. In a real-world scenario, that job would be running all the time, processing events from Kafka as they arrive.
Thread.sleep(30000);
streams.close();
```

See a straight example from [Apache Kafka](https://kafka.apache.org/documentation/streams/#:~:text=Kafka%20Streams%20is%20a%20client,Kafka's%20server%2Dside%20cluster%20technology.). Similarly Spring [reference](https://docs.spring.io/spring-kafka/reference/streams.html) clearly explains where Spring does to set up Kafka streams.
Essentially, in Spring, we define a configuration of `streamsConfiguration` for a Kafka application, topologies through the injected `StreamsBuilder`,  then Spring manages `KafkaStreams` for us.
Normally when there is one stream processor, use `@EnableKafkaStreams` annotation to autoconfigure the required components. This autoconfiguration requires a `KafkaStreamsConfiguration` bean with the name as specified by `DEFAULT_STREAMS_CONFIG_BEAN_NAME` (aka `defaultKafkaStreamsConfig`). As a result, Spring Boot uses this configuration and creates a `KafkaStreams` client to manage our application lifecycle.
A `StreamsBuilderFactoryBean` is the actual class to manage `streamsConfiguration` for creating a `KafkaStreams` with a `StreamsBuilder`. See
[Spring.IO](https://docs.spring.io/spring-kafka/api/org/springframework/kafka/annotation/KafkaStreamsDefaultConfiguration.html) for details.
Behind scene, it creates a `StreamBuilderFactoryBean`:
```java
public StreamsBuilderFactoryBean defaultKafkaStreamsBuilder(@Qualifier("defaultKafkaStreamsConfig")
 ObjectProvider<KafkaStreamsConfiguration> streamsConfigProvider,
 ObjectProvider<StreamsBuilderFactoryBeanConfigurer> configurerProvider)
```

A Kafka application can have more than one topologies(stream processors)? For example, [here](https://docs.confluent.io/platform/current/streams/architecture.html) states: *A stream processing application – i.e., your application – may define one or more such topologies, though typically it defines only one.* 

As it shown in [WordCountProcessor.java](WordCountProcessor.java), multiple pipelines can be defined to the same `StreamBuilder`. Do not know what the ramifications are. 

`EmbeddedKafkaIntegrationTest` has a different setup compared with `EmbeddedKafkaTest`. `EmbeddedKafkaTest` is easy to setup and does not cause issues of dependencies or beans because it does not use any beans from the main package by define a empty Configuration.

This is a very strange exception which may means all streams on one machine have the same state dir:
`Unable to initialize state, this can happen if multiple instances of Kafka Streams are running in the same state directory (current state directory is [/var/folders/9d/b2326jv513vglf41k5fz2hmc0000gq/T/kafka-streams/streams-app]`*.

## com.fasterxml.jackson
In a processor, dealing with JSON payload is very common. Usually the source save JSON payloads as strings in a topic, a processor picks up the message and use some tools to process them as a JSON data. [`com.fasterxml.jackson.databind`](https://github.com/FasterXML/jackson-databind) is a good tool for read and write. When `org.apache.avro` is used, no extra dependencies are needed: it has been shipped with `org.apache.avro`.

## Test
*Copied from https://developer.confluent.io/courses/kafka-streams/testing/*
### Test consumers or producers
For these tests, `SpringBoot`'s `@EmbeddedKafka` can be used. Embedded Kafka is an in-memory Kafka instance to run tests against. It contains the `EmbeddedKafkaBroker` class. The annotation has some properties to control how a broker is setup: for example: `partitions = 1` and `topics = {EmbeddedKafkaTest.TOPIC_NAME}`. Because this is a `SpringBoot` test annotation which runs with `@SpringBootTest`, so `SpringBoot` beans will be configured. This can be beneficial:
`@Autowired` can be used to "avoid" setup. On the flip side, this could cause a complex issues to separated context, avoiding conflict beans for tests. Try to simplify a bit, can try this:
```java
// define a nest class to not use any beans
    @Configuration
    @EnableAutoConfiguration
    static class EmptyConfiguration {}
```

Or like this when the application is not very pure:
```java
// define a nest class with needed beans
    @TestConfiguration
    @EnableKafkaStreams
    public static class KafkaStreamsConfiguration {
        // This is important part of setting up because there is a KafkaStreamsDefaultConfiguration which may
        // does not work with EmbeddedKafka: is the broker the cause?
        // EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS = "spring.embedded.kafka.brokers";
        // SPRING_EMBEDDED_KAFKA_BROKERS has been exported/set as property "spring.embedded.kafka.brokers";
        // See https://github.com/spring-projects/spring-kafka/blob/3.0.x/spring-kafka-test/src/main/java/org/springframework/kafka/test/EmbeddedKafkaBroker.java#L108
        @Value("${spring.embedded.kafka.brokers}")
        private String brokerAddresses;

        @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
        public StreamsConfig kStreamsConfigs() {
            final Map<String, Object> props = new HashMap<>();
            props.put(StreamsConfig.APPLICATION_ID_CONFIG, "testStreams");
            props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, this.brokerAddresses);
            return new StreamsConfig(props);
        }
    }
```

### Test Kafka Streams
Kafka Streams connects to brokers. But in unit testing terms, it’s expensive to have all of your tests rely on a broker connection. You do want to have some level of integration testing, but you want to use a unit-test type of framework. Furthermore, with Kafka Streams, you have connected components in a topology, and ideally you'd like to test each one in isolation, but you'd also like an end-to-end test that is as fast as possible.

TopologyTestDriver solves these issues: it lets you unit test a topology in an end-to-end test, but without a broker. Integration testing should still be done with a live broker, but it can be done sparingly, and it's not needed for all tests.

### TopologyTestDriver
A `TopologyTestDriver` takes a topology (input and output topics, and components between) and configurations. It is mainly used to test the topology and serializers to a degree. When use Confluent Avro SerDe, Schema Registry is used with Kafka Streams, TopologyTestDriver still usable as long as **to use MockSchemaRegistry, an in-memory version of Schema Registry, and can be specified by the URL provided in the configuration - instead of http:// you put in mock://, and the test will automatically use the MockSchemaRegistry.**

First, you instantiate your `TopologyTestDriver`, using your topology and configurations as constructor parameters. Then you create `TestInputTopic` and `TestOutputTopic` instances by calling the test driver. Next, you call `TestInputTopic.pipeInput` with KeyValue objects. There are also overloaded methods that allow you to provide timestamps, lists of records, etc.

When you execute `TestInputTopic.pipeInput`, it triggers stream-time punctuation. So if you don't provide timestamps on the records, then under the hood, each record is advanced by current wall-clock time. But you can provide your own timestamps within the records to trigger certain behaviors that would happen within a Kafka Streams application.

Within `TopologyTestDriver`'s wall-clock punctuation, you can trigger punctuation based on stream time using the timestamps that you give to the records. But wall-clock punctuation will only be fired if you call a method called `advanceWallClockTime`.

### TestOutputTopicInstances
`TestOutputTopicInstance`s mock out the sink nodes, the topics to which you will write. After you've called `.pipeInput` for all of the records you've sent through, you call `TestInputTopic.read(Key)Value` and assert the results. There are overloaded methods to read all of the values in a list, read KeyValues in a list, and read KeyValues in a map.

### Testable Applications
The Kafka Streams DSL has several operators that take a SAM interface, so you can use lambda expressions with them. The downside is that you can't easily test the lambdas in isolation; you have to test them with the topology as you've wired it up. So you might want to consider instead writing a concrete class, which would let you write a separate test for it.

### Integration Tests
You always need some level of integration testing against a live broker. For example, you want to see how your stateful operations behave in a real environment. TopologyTestDriver doesn't have caching behavior or commits, and it doesn't write to real topics.

The best choice for brokers in an integration test is to use the TestContainers library with Docker: It has annotations that can easily control the broker lifecycle, and it's possible to share a container across multiple test classes, which leads to reduced testing time.
