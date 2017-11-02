/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kafka.services;

import java.util.Properties;
import java.util.concurrent.Future;
import javax.annotation.PreDestroy;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Service;

/**
 *
 * @author tzimmer
 */
@Service
public class KafkaProducer {

    // Create our producer properties
    //@Value("${producer.topic}")
    private String topic = "stomp-topic";

    private final Properties props = new Properties();
    private final org.apache.kafka.clients.producer.KafkaProducer<String, String> producer;
    private boolean started = false;
    int position = 0;

    public KafkaProducer() {
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:29092");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        producer = new org.apache.kafka.clients.producer.KafkaProducer<>(props);
    }


    public Future<RecordMetadata> send(String topicKey, String frame) {
        Future<RecordMetadata> result = producer.send(new ProducerRecord<>(topic, topicKey, frame));
        return result;
    }

    @PreDestroy
    public void destroy() {
        producer.close();
    }
}
