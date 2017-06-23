/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kafka.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import kafka.avro.CDCEvent;
import kafka.avro.Column;
import kafka.graph.GraphMapper;
import kafka.graph.GraphQuery;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueIterator;
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

    @Override
    public void init(ProcessorContext context) {
        // keep the processor context locally because we need it in punctuate() and commit()
        this.context = context;

        // call this processor's punctuate() method every 1000 milliseconds.
        this.context.schedule(1000);

        // retrieve the key-value store named "Counts"
        this.kvStore = (KeyValueStore<String, Long>) context.getStateStore("Counts");
    }

    @Override
    public void process(String key, String value) {

        try {
            //JSON from String to Object
            CDCEvent event = mapper.readValue(value, CDCEvent.class);
            logger.debug("process event {}", event);
            GraphQuery query = graphMapper.getQueryForTable(event.getTable().toString());
            if (query == null) {
                return;
            }
            Map<String, Object> pksVal = query.getInitParameter();
            String pkVal = "";
            pksVal.put("tx", context.offset());
            for (String column : query.getKeys()) {
                for (Column col : event.getAfter()) {
                    if (col.getName().equals(column)) {
                        pkVal = col.getValue().toString();
                        break;
                    }
                }
                pksVal.put(column, pkVal);
            }
            String finalQuery = query.getQuery();
            try (org.neo4j.driver.v1.Session session = driver.session()) {
                session.writeTransaction((Transaction tx) -> {
                    logger.debug("Merge Query {} \n with param {}", finalQuery, pksVal);
                    StatementResult rs = tx.run(finalQuery, pksVal);
                    logger.debug("store modification node created {}, relationship created {}", rs.summary().counters().nodesCreated(), rs.summary().counters().relationshipsCreated());
                    return event;
                });
                // then commit message processing
                context.commit();
            }

//        for (String word : words) {
            Long oldValue = this.kvStore.get(key);

            if (oldValue
                    == null) {
                this.kvStore.put(key, 1L);
            } else {
                this.kvStore.put(key, oldValue + 1L);
            }
//        }
        } catch (IOException ex) {
            logger.info("Error", ex);
        }
    }

    @Override
    public void punctuate(long timestamp
    ) {
        KeyValueIterator<String, Long> iter = this.kvStore.all();

        while (iter.hasNext()) {
            KeyValue<String, Long> entry = iter.next();
            context.forward(entry.key, "Aggregated " + entry.value.toString());
        }

        iter.close();
        // commit the current processing progress
        context.commit();
    }

    @Override
    public void close() {
        // close any resources managed by this processor.
        // Note: Do not close any StateStores as these are managed
        // by the library
    }

}
