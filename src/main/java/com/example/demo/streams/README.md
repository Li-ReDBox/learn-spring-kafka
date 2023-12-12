For a Kafka stream, everything is built around an instance of StreamBuilder:

```java
StreamsBuilder builder = new StreamsBuilder();

// define a stream: build a streaming topology
KStream<String, String> textLines = builder.stream(inputTopic);
Pattern pattern = Pattern.compile("\\W+", Pattern.UNICODE_CHARACTER_CLASS);

KTable<String, Long> wordCounts = textLines
  .flatMapValues(value -> Arrays.asList(pattern.split(value.toLowerCase())))
  .groupBy((key, word) -> word)
  .count();

String outputTopic = "outputTopic";
wordCounts.toStream()
  .to(outputTopic, Produced.with(Serdes.String(), Serdes.Long()));

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
  
// run stream topology
Topology topology = builder.build();
KafkaStreams streams = new KafkaStreams(topology, streamsConfiguration);
streams.start();

// wait 30 seconds for the job to finish. In a real-world scenario, that job would be running all the time, processing events from Kafka as they arrive.
Thread.sleep(30000);
streams.close();
```