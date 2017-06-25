/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kafka.services;

import java.util.Properties;
import kafka.processor.GraphBuilderProcessor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.processor.StateStoreSupplier;
import org.apache.kafka.streams.processor.TopologyBuilder;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

/**
 *
 * @author tzimmer
 */
@Service
public class KafkaProcessor implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProcessor.class);

    // Create our producer properties
    private final String topic = "input-topic";
    private final Properties props = new Properties();
    private final StreamsConfig config;
    private final TopologyBuilder builder;
    //private final KStreamBuilder builder;
    private KafkaStreams streams;
    private ApplicationContext applicationContext;

    public KafkaProcessor() {
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "my-stream-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:29092");
        props.put(StreamsConfig.KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        props.put(StreamsConfig.VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);

        // Create an instance of StreamsConfig from the Properties instance
        config = new StreamsConfig(props);

        StateStoreSupplier countStore = Stores.create("Counts")
                .withKeys(Serdes.String())
                .withValues(Serdes.Long())
                .inMemory()
                .build();

        builder = new TopologyBuilder();

        builder.addSource("SOURCE", topic)
                // add "PROCESS1" node which takes the source processor "SOURCE" as its upstream processor
                .addProcessor("PROCESS1", () -> applicationContext.getBean(GraphBuilderProcessor.class), "SOURCE")
                // add the created state store "COUNTS" associated with processor "PROCESS1"
                .addStateStore(countStore, "PROCESS1")
                // add the sink processor node "SINK1" that takes Kafka topic "sink-topic1"
                // as output and the "PROCESS1" node as its upstream processor
                .addSink("SINK1", "output-topic", "PROCESS1");

    }

    public void start() {
        // Start the Kafka Streams instance
        streams = new KafkaStreams(builder, config);
        streams.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
        streams.start();
    }

    public void stop() {
        // Stop the Kafka Streams instance
        streams.close();
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        this.applicationContext = ac;
    }

}
