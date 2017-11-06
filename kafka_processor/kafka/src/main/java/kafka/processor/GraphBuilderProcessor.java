/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kafka.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import kafka.avro.CDCEvent;
import kafka.avro.Column;
import kafka.graph.GraphMapper;
import kafka.graph.GraphPreparedQuery;
import kafka.graph.Query;
import kafka.message.EventMessage;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 *
 * @author tzimmer
 */
@Component
@Scope("prototype")
public class GraphBuilderProcessor implements Processor<String, String> {

    private static final Logger logger = LoggerFactory.getLogger(GraphBuilderProcessor.class);
    private static final String LOST_TX = "Lost-Tx-Error";

    private ProcessorContext context;
    private KeyValueStore<String, Long> kvStore;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    @Qualifier("neo4jDriver")
    protected Driver driver;
    
    private long nbMessages = 0;

    @Autowired
    protected GraphMapper graphMapper;

    Map<String, TransactionCacheEntry> transactionCache = new HashMap<>();

    @Override
    public void init(ProcessorContext context) {
        // keep the processor context locally because we need it in punctuate() and commit()
        this.context = context;

        // call this processor's punctuate() method every 60000 milliseconds.
        this.context.schedule(1000);

        // retrieve the key-value store named "Counts"
        this.kvStore = (KeyValueStore<String, Long>) context.getStateStore("Counts");
    }

    @Override
    public void process(String key, String value) {

        try {
            //JSON from String to Object
            nbMessages++;
            CDCEvent event = mapper.readValue(value, CDCEvent.class);
            logger.debug("process event {}", value);
            switch (event.getAction()) {
                case START:
                    // Load Cache
                    if (!transactionCache.containsKey(key)) {
                        transactionCache.put(key, new TransactionCacheEntry(key));
                    } else {
                        handleError(key, event);
                        return;
                    }
                    break;
                case END:
                    if (!transactionCache.containsKey(key)) {
                        handleError(key, event);
                        return;
                    }
                    terminateTransation(key);
                    break;
                case INSERT:
                    if (!transactionCache.containsKey(key)) {
                        handleError(key, event);
                        return;
                    }
                    handleInsert(key, event);
                    break;
                case UPDATE:
                    if (!transactionCache.containsKey(key)) {
                        handleError(key, event);
                        return;
                    }
                    handleUpdate(key, event);
                    break;
                case DELETE:
                    if (!transactionCache.containsKey(key)) {
                        handleError(key, event);
                        return;
                    }
                    handleDelete(key, event);
                    break;
                default:
                    break;

            }
        } catch (IOException ex) {
            logger.info("Error", ex);
        }
    }

    private void handleError(String key, CDCEvent event) {
        logger.warn("Handling error for {}, {}", key, event);
    }

    private void handleInsert(String key, CDCEvent event) {
        logger.debug("process insert {}", event);
        GraphPreparedQuery query = graphMapper.getQueryForTable(event.getTable());
        if (query == null) {
            return;
        }
        TransactionCacheEntry tx = transactionCache.get(key);
        String pks = query.pks(event);
        Query q;
        if ((q = tx.getQueries().get(event.getTable())) == null) {
            q = new Query(query.query());
            tx.getQueries().put(event.getTable(), q);
        }
        if (q.getParameters(pks) != null) {
            logger.warn("Unexpected entry for tx {}, table {}, keys {}", key, event.getTable(), pks);
        } else {
            Map<String, Object> parameter = q.createParameters(pks);
            parameter.putAll(query.getInitParameter());
            String pkVal = "";
            for (String column : query.keys()) {
                for (Column col : event.getAfter()) {
                    if (col.getName().equals(column)) {
                        pkVal = col.getValue();
                        break;
                    }
                }
                parameter.put(column, pkVal);
            }
        }

    }

    private void handleUpdate(String key, CDCEvent event) {
        logger.debug("process update {}", event);
    }

    private void handleDelete(String key, CDCEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void terminateTransation(String key) {
        logger.info("Terminating Tx {}, offset {}", key, context.offset());
        try (org.neo4j.driver.v1.Session session = driver.session()) {
            session.writeTransaction((Transaction tx) -> {
                TransactionCacheEntry entry = transactionCache.remove(key);
                long timeAvailable = 0L;
                for (Query q : entry.getQueries().values()) {
                    for (Map<String, Object> parameters : q.getParameterList()) {
                        long start = System.currentTimeMillis();
                        parameters.put("tx", context.offset());
                        logger.debug("Merge Query {} \n with param {}", q.getQuery(), parameters);
                        StatementResult rs = tx.run(q.getQuery(), parameters);
                        //logger.debug("store modification node created {}, relationship created {}", rs.summary().counters().nodesCreated(), rs.summary().counters().relationshipsCreated());
                        timeAvailable += System.currentTimeMillis() - start;
                    }
                }
                logger.info("Process TX : in {} ms", timeAvailable);
                return key;
            });
        }
        // then commit message processing
        Date currentDate = new Date();
        this.messagingTemplate.convertAndSend("/topic/events", new EventMessage("processed tx", currentDate));
        context.forward("/topic/events", "{\"event\":\"Process TX ["+key+"] \",\"date\":\""+currentDate.toString()+"\"}");
        context.commit();
    }

    @Override
    public void punctuate(long timestamp) {
        List<String> toBeRemoved = new ArrayList<>();
        Long nbError = kvStore.get(LOST_TX);
        if (nbError == null) {
            nbError = 0l;
        }
        for (TransactionCacheEntry entry : transactionCache.values()) {
            if (entry.getCache_date() < timestamp - 5000) {
                toBeRemoved.add(entry.getKey());
            }
        }
        for (String key : toBeRemoved) {
            logger.warn("remove old tx : {}", key);
            this.messagingTemplate.convertAndSend("/topic/events", new EventMessage("remove tx "+key, new Date()));
            transactionCache.remove(key);
            nbError++;
        }
        kvStore.put(LOST_TX, nbError);
        logger.warn("total tx removed : {}", nbError);
        context.forward("/topic/events", "{\"event\":\"Process "+nbMessages+" messages / second\",\"date\":\""+new Date().toString()+"\"}");
        nbMessages = 0;
    }

    @Override
    public void close() {
        // close any resources managed by this processor.
        // Note: Do not close any StateStores as these are managed
        // by the library
    }

}
