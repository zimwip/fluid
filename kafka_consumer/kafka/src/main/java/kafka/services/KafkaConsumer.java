/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kafka.services;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.message.EventMessage;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 *
 * @author tzimmer
 */
@Service
public class KafkaConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    // Create our producer properties
    private final String topic = "output-topic";
    private final Properties props = new Properties();
    private org.apache.kafka.clients.consumer.KafkaConsumer<String, String> consumer;
    private final AtomicBoolean closed = new AtomicBoolean(true);

    public KafkaConsumer() {
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:29092");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 60000);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "my-consumer");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "my-consumer-group");
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

    public class KafkaConsumerRunner implements Runnable {

        public void run() {
            try {
                consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);
                consumer.subscribe(Arrays.asList(topic));
                logger.info("consumer subscribe to topic {}", topic);
                while (!closed.get()) {
                    ConsumerRecords<String, String> records = consumer.poll(100);
                    // Handle new records
                    for (ConsumerRecord<String, String> record : records) {
                        if (record.topic().equals(topic)) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("partition", record.partition());
                            data.put("offset", record.offset());
                            data.put("timestamp", new Date(record.timestamp()));
                            data.put("key", record.key());
                            data.put("value", record.value());
                            messagingTemplate.convertAndSend("/topic/events", new EventMessage("receive " + data, new Date()));
                            logger.info("get message {}", data);
                        }
                    }
                }
            } catch (WakeupException e) {
                // Ignore exception if closing
                if (!closed.get()) {
                    throw e;
                }
            } finally {
                consumer.close();
            }
        }
    }

    public void start() {
        if (closed.get()) {
            closed.set(false);
            Thread t = new Thread(new KafkaConsumerRunner());
            t.start();
        }
    }

    public void stop() {
        if (!closed.get()) {
            closed.set(true);
            consumer.wakeup();
        }
    }

}
