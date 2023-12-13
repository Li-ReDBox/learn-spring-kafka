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

See a straight example from [Apache Kafka](https://kafka.apache.org/documentation/streams/#:~:text=Kafka%20Streams%20is%20a%20client,Kafka's%20server%2Dside%20cluster%20technology.).
Essentially, we define a topology through a `StreamsBuilder`, a configuration of `streamsConfiguration`, then mesh together use `KafkaStreams`
which usually not directly.
Normally when there is one stream processor, use `@EnableKafkaStreams` annotation to autoconfigure the required components. This autoconfiguration requires a `KafkaStreamsConfiguration` bean with the name as specified by `DEFAULT_STREAMS_CONFIG_BEAN_NAME` (aka `defaultKafkaStreamsConfig`). As a result, Spring Boot uses this configuration and creates a `KafkaStreams` client to manage our application lifecycle.
A `StreamsBuilderFactoryBean` is the actual class to manage `streamsConfiguration` for creating a `KafkaStreams` with a `StreamsBuilder`. See
[Spring.IO](https://docs.spring.io/spring-kafka/api/org/springframework/kafka/annotation/KafkaStreamsDefaultConfiguration.html) for details.
Behind scene, it creates a `StreamBuilderFactoryBean`:
```java
public StreamsBuilderFactoryBean defaultKafkaStreamsBuilder(@Qualifier("defaultKafkaStreamsConfig")
 ObjectProvider<KafkaStreamsConfiguration> streamsConfigProvider,
 ObjectProvider<StreamsBuilderFactoryBeanConfigurer> configurerProvider)
```

Question: can a Kafka application has more than one stream processor? The answer is [YES](https://docs.confluent.io/platform/current/streams/architecture.html): *A stream processing application – i.e., your application – may define one or more such topologies, though typically it defines only one.*

// this is still an open question because the failures came from EmbeddedKafkaIntegrationTest. EmbeddedKafkaTest does not cause issues. Interesting.
Without configure correctly a state dir, but just simply use a `StreamBuilder` to create another topology causes error like this:
`Unable to initialize state, this can happen if multiple instances of Kafka Streams are running in the same state directory (current state directory is [/var/folders/9d/b2326jv513vglf41k5fz2hmc0000gq/T/kafka-streams/streams-app]`.

As it shown in [WordCountProcessor.java](WordCountProcessor.java), multiple pipelines can be defined to the same `StreamBuilder`. Do not know what the ramifications are.