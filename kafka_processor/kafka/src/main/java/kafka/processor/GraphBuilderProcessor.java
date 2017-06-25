/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kafka.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kafka.avro.CDCEvent;
import kafka.avro.Column;
import kafka.graph.GraphMapper;
import kafka.graph.GraphPreparedQuery;
import kafka.graph.Query;
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
import org.springframework.stereotype.Component;

/**
 *
 * @author tzimmer
 */
@Component
@Scope("prototype")
public class GraphBuilderProcessor implements Processor<String, String> {

    private static final Logger logger = LoggerFactory.getLogger(GraphBuilderProcessor.class);
    private ProcessorContext context;
    private KeyValueStore<String, Long> kvStore;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    @Qualifier("neo4jDriver")
    protected Driver driver;

    @Autowired
    protected GraphMapper graphMapper;

    Map<String, TransactionCacheEntry> transactionCache = new HashMap<>();

    @Override
    public void init(ProcessorContext context) {
        // keep the processor context locally because we need it in punctuate() and commit()
        this.context = context;

        // call this processor's punctuate() method every 1000 milliseconds.
        this.context.schedule(60000);

        // retrieve the key-value store named "Counts"
        this.kvStore = (KeyValueStore<String, Long>) context.getStateStore("Counts");
    }

    @Override
    public void process(String key, String value) {

        try {
            //JSON from String to Object
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
        GraphPreparedQuery query = graphMapper.getQueryForTable(event.getTable().toString());
        if (query == null) {
            return;
        }
        TransactionCacheEntry tx = transactionCache.get(key);
        String pks = query.pks(event);
        Query q;
        if ((q = tx.getQueries().get(event.getTable().toString())) == null) {
            q = new Query(query.query());
            tx.getQueries().put(event.getTable().toString(), q);
        }
        if (q.getParameters(pks) != null) {
            logger.warn("Unexpected entry for tx {}, table {}, keys {}", key, event.getTable(), pks);
        } else {
            Map <String, Object> parameter = q.createParameters(pks);
            parameter.putAll(query.getInitParameter());
            String pkVal = "";
            for (String column : query.keys()) {
                for (Column col : event.getAfter()) {
                    if (col.getName().equals(column)) {
                        pkVal = col.getValue().toString();
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
                for (Query q : entry.getQueries().values()) {
                    for (Map<String, Object> parameters : q.getParameterList()) {
                        parameters.put("tx", context.offset());
                        logger.debug("Merge Query {} \n with param {}", q.getQuery(), parameters);
                        StatementResult rs = tx.run(q.getQuery(), parameters);
                        logger.debug("store modification node created {}, relationship created {}", rs.summary().counters().nodesCreated(), rs.summary().counters().relationshipsCreated());
                    }
                }
                return key;
            });
        }
        // then commit message processing
        context.commit();
    }

    @Override
    public void punctuate(long timestamp) {
        List<String> toBeRemoved = new ArrayList<>();
        for (TransactionCacheEntry entry : transactionCache.values()) {
            if (entry.getCache_date() < timestamp - 5000) {
                toBeRemoved.add(entry.getKey());         
            }
        }
        for (String key : toBeRemoved)
        {
            logger.warn("remove old tx : {}", key);
            transactionCache.remove(key);
        }
    }

    @Override
    public void close() {
        // close any resources managed by this processor.
        // Note: Do not close any StateStores as these are managed
        // by the library
    }

}
