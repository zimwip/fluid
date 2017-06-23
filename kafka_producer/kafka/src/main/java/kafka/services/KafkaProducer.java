/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kafka.services;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PreDestroy;
import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author tzimmer
 */
@Service
public class KafkaProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);

    //@Value("${producer.file}")
    private String file = "C://Work/input.txt";

    private List<String> lines = new ArrayList<>();

    // Create our producer properties
    //@Value("${producer.topic}")
    private String topic = "input-topic";

    private final Properties props = new Properties();
    private final org.apache.kafka.clients.producer.KafkaProducer<String, String> producer;
    private boolean started = false;
    int position = 0;

    public KafkaProducer() throws URISyntaxException {
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:29092");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        producer = new org.apache.kafka.clients.producer.KafkaProducer<>(props);

        // opening  File for read
        logger.info("File name {}", file);
        Path filePath = Paths.get(file);
        if (!Files.exists(filePath, LinkOption.NOFOLLOW_LINKS)) {
            logger.warn("File not found {}, aborting creation", file);
            throw new RuntimeException("File not found");
        }
        try (Stream<String> stream = Files.lines(Paths.get(file))) {

            //1. filter line 3
            //2. convert all content to upper case
            //3. convert it into a List
            lines = stream.collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void config() {
        String zookeeperConnect = "localhost:32181";
        int sessionTimeoutMs = 10 * 1000;
        int connectionTimeoutMs = 8 * 1000;

        ZkClient zkClient = new ZkClient(
                zookeeperConnect,
                sessionTimeoutMs,
                connectionTimeoutMs,
                ZKStringSerializer$.MODULE$);

        // Security for Kafka was added in Kafka 0.9.0.0
        boolean isSecureKafkaCluster = false;
        // ZkUtils for Kafka was used in Kafka 0.9.0.0 for the AdminUtils API
        ZkUtils zkUtils = new ZkUtils(zkClient, new ZkConnection(zookeeperConnect), isSecureKafkaCluster);

        int partitions = 1;
        int replication = 1;

        // Add topic configuration here
        Properties topicConfig = new Properties();

        scala.collection.Map<String, Properties> configs = AdminUtils.fetchAllTopicConfigs(zkUtils);
        if (configs.get(topic) == null) {
            AdminUtils.createTopic(zkUtils, topic, partitions, replication, topicConfig, RackAwareMode.Disabled$.MODULE$);
            logger.info("Topic created");
        }
        zkClient.close();

    }

    private Future<RecordMetadata> producerRecord() {
        String raw = lines.get(position);
        position = (position + 1) % lines.size();
        String[] split = raw.split("-");
        return producer.send(new ProducerRecord<>(topic, split[0], split[1]));
    }

    public void start() {
        started = true;
        long i = 0;
        while (started) {
            Future<RecordMetadata> send = producerRecord();
            try {
                logger.info("send message {}", send.get());
            } catch (InterruptedException ex) {
                java.util.logging.Logger.getLogger(KafkaProducer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                java.util.logging.Logger.getLogger(KafkaProducer.class.getName()).log(Level.SEVERE, null, ex);
            }
            i++;
        }
    }

    public void stop() {
        started = false;
    }

    public void send() {
        Future<RecordMetadata> send = producerRecord();
        try {
            logger.info("send message {}", send.get());
        } catch (InterruptedException ex) {
            java.util.logging.Logger.getLogger(KafkaProducer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            java.util.logging.Logger.getLogger(KafkaProducer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @PreDestroy
    public void destroy() {
        producer.close();
    }
}
