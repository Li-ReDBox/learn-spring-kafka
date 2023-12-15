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
