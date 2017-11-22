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
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PreDestroy;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
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
import org.fagazi.enedis.DataResponse;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    // REST access to data
    private Client client = ClientBuilder.newClient().register(JacksonFeature.class);
    private WebTarget webTarget = client.target("https://data.enedis.fr/api");
    private WebTarget employeeWebTarget = webTarget.path("records/1.0/search");

    // Create our producer properties
    //@Value("${producer.topic}")
    private String topic = "input-topic";
    private String topicEnedis = "enedis-topic";

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
        if (configs.get(topicEnedis) == null) {
            AdminUtils.createTopic(zkUtils, topicEnedis, partitions, replication, topicConfig, RackAwareMode.Disabled$.MODULE$);
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
        // open mic 
        TargetDataLine line;
        AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
        float rate = 44100.0f;
        int channels = 1;
        int sampleSize = 8;
        boolean bigEndian = true;
        AudioFormat format = new AudioFormat(encoding, rate, sampleSize, channels, (sampleSize / 8) * channels, rate, bigEndian);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            logger.error("Line matching " + info + " not supported.");
        }
        try {

            line = (TargetDataLine) AudioSystem.getLine(info);
            int buffsize = line.getBufferSize() / 5;
            //buffsize += 512;
            line.open(format);
            line.start();
            int numBytesRead;
            byte[] data = new byte[buffsize];
            while (started) {
                try {
                    numBytesRead = line.read(data, 0, data.length);
                    logger.info("test data {} :  {}", numBytesRead, data);
                    Future<RecordMetadata> send = producerRecord();
                    logger.info("send message {}", send.get());
                } catch (InterruptedException ex) {
                    java.util.logging.Logger.getLogger(KafkaProducer.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ExecutionException ex) {
                    java.util.logging.Logger.getLogger(KafkaProducer.class.getName()).log(Level.SEVERE, null, ex);
                }
                i++;
            }
            line.close();
        } catch (LineUnavailableException ex) {
            java.util.logging.Logger.getLogger(KafkaProducer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void stop() {
        started = false;
    }

    public Future<DataResponse> send() {
        Invocation.Builder invocationBuilder = employeeWebTarget
                .queryParam("dataset", "bilan-electrique-demi-heure")
                .queryParam("rows", 1)
                .queryParam("start", position + 1)
                .queryParam("sort", "horodate")
                .request(MediaType.APPLICATION_JSON);
        Future<DataResponse> response = invocationBuilder.async().get(DataResponse.class);
        Future<RecordMetadata> send = producerRecord();
        try {
            logger.info("send message {}", send.get());
        } catch (InterruptedException ex) {
            java.util.logging.Logger.getLogger(KafkaProducer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            java.util.logging.Logger.getLogger(KafkaProducer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return response;
    }

    @PreDestroy
    public void destroy() {
        producer.close();
    }
}
