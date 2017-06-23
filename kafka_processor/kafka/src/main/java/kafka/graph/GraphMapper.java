/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kafka.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.airbus.mapper.MapperConfig;
import org.airbus.mapper.domain.ForeignKey;
import org.airbus.mapper.domain.PrimaryKey;
import org.airbus.mapper.domain.SqlObject;
import org.airbus.mapper.xml.XmlParser;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 *
 * @author TZIMMER
 */
@Component
public class GraphMapper {

    private static final Logger logger = LoggerFactory.getLogger(GraphMapper.class);
    private final static String MAX_VALUE = "\"" + Long.MAX_VALUE + "\"";
    private final MapperConfig config;
    private final Map<String, GraphQuery> queries = new HashMap<>();

    @Autowired
    @Qualifier("neo4jDriver")
    protected Driver driver;

    public GraphMapper() {
        config = XmlParser.initialize("/conf.xml");
    }

    @PostConstruct
    private void initialize() {

        for (String table : config.getTableSet()) {
            GraphQuery graphQuery = new GraphQuery();
            List<SqlObject> objects = config.getObjectForTable(table);
            if (objects == null) {
                return;
            }
            queries.put(table, graphQuery);
            Map<String, PrimaryKey> pksh = new HashMap<>();
            Map<String, ForeignKey> fksh = new HashMap<>();
            boolean first;
            for (SqlObject obj : objects) {
                String index = "CREATE INDEX ON :TAB_" + table + "( ";
                first = true;
                for (PrimaryKey pk : obj.getPrimaryKeys()) {
                    pksh.put(pk.getName(), pk);
                    // also build Index on key
                    if (first) {
                        index += " ";
                        first = false;
                    } else {
                        index += ", ";
                    }
                    index += pk.getName();
                }
                index += " )";
                String finalIndex = index;
                try (org.neo4j.driver.v1.Session session = driver.session()) {
                    session.writeTransaction((Transaction tx) -> {
                        logger.info("INDEX Query {}", finalIndex);
                        StatementResult rs = tx.run(finalIndex);
                        logger.info("process index in store index added {}", rs.summary().counters().indexesAdded());
                        return 0L;
                    });
                }
                // handle SubObject Reference
                for (SqlObject subObject : obj.getSubObjects()) {
                    for (ForeignKey fk : subObject.getForeignKeys()) {
                        if (pksh.get(fk.getOuter()) == null) { // do not add fk if already in pk.
                            fksh.put(fk.getOuter(), fk);
                        }
                    }
                }
            }
            List<PrimaryKey> pks = new ArrayList<>(pksh.values());
            List<ForeignKey> fks = new ArrayList<>(fksh.values());

            first = true;
            String query = "MERGE (table:TABLE:" + table + " { tableName:$table}) WITH table MERGE (table) <-[:OF]- (record:RECORD:TAB_" + table + " {";
            graphQuery.addBasicValue("table", table);
            for (PrimaryKey pk : pks) {
                if (first) {
                    query += " ";
                    first = false;
                } else {
                    query += ", ";
                }
                query += pk.getName() + " : $" + pk.getName();
                graphQuery.addKey(pk.getName());
            }
            // Existing version insert merge
            query += " }) WITH record ";
//            query += "OPTIONAL MATCH (:VERSION) -[pv:VERSION_OF]-> (record) WHERE pv.from < $tx AND pv.to > $tx ";
//             // Existing version insert merge
//            query += "WITH record, pv, CASE WHEN pv IS NOT null THEN pv.to";
//            query += "SET pv.to = $tx ";
//            query += "END "; 
//            // First version direct merge
//            query += "CASE WHEN pv IS null THEN ";
//            query += "SET ppv.from = $tx ";
//            query += "END";
            query += "MERGE (version:VERSION {tx: $tx}) -[:VERSION_OF {from:$tx, to:" + MAX_VALUE + "}]-> (record) ";
            query += "WITH record, version ";
            // Insert data here
            if (fks.size() > 0) {
                query = query + "\nSET";
                first = true;
                for (PrimaryKey pk : pks) {
                    if (first) {
                        query += " ";
                        first = false;
                    } else {
                        query += ", ";
                    }
                    query += "version."+pk.getName() + " = $" + pk.getName();
                    graphQuery.addKey(pk.getName());
                }
                for (ForeignKey fk : fks) {
                        query += ",\n ";
                    query += "version." + fk.getOuter() + " = $" + fk.getOuter();
                    graphQuery.addKey(fk.getOuter());
                }
            }
            // end insert data
            String withQuery = "\n WITH record";
            String mergeQuery = "";
            query += withQuery;
            for (SqlObject obj : objects) {
                // link with object.
                query += "\nMERGE (" + obj.getClearAlias().toLowerCase() + ":OBJECT:OBJ_" + obj.getClearAlias() + "  { name:$" + obj.getClearAlias() + "_name })-[:REFERENCE]->(record) ";
                graphQuery.addBasicValue(obj.getClearAlias() + "_name", obj.getAlias());
                withQuery += ", " + obj.getClearAlias().toLowerCase();
            }
            boolean firstParent = true;
            for (SqlObject obj : objects) {
                // link with parent
                first = true;
                if (obj.getParent() == null) {
                    continue;
                }
                if (firstParent) {
                    query += withQuery;
                    firstParent = false;
                }
                query += "\nMATCH (" + obj.getClearAlias().toLowerCase() + "_" + obj.getParent().getClearAlias().toLowerCase() + ":OBJECT:OBJ_" + obj.getParent().getClearAlias() + ") -[:REFERENCE]-> (:RECORD:TAB_" + obj.getParent().getTableName() + " {";
                for (ForeignKey fk : obj.getForeignKeys()) {
                    if (first) {
                        query += " ";
                        first = false;
                    } else {
                        query += ", ";
                    }
                    query += fk.getOuter() + " : $" + fk.getCurrent();
                    graphQuery.addKey(fk.getCurrent());
                }
                query += " }) -[:OF]-> (:TABLE:" + obj.getParent().getTableName() + ")";
                mergeQuery += "\nMERGE (" + obj.getClearAlias().toLowerCase() + ")-[:PART_OF]->(" + obj.getClearAlias().toLowerCase() + "_" + obj.getParent().getClearAlias().toLowerCase() + ") ";
            }
            query += mergeQuery;

            // Now link the child 
            withQuery = "\nWITH record";
            mergeQuery = "";
            boolean firstChild = true;
            for (SqlObject obj2 : objects) {
                for (SqlObject childObj : obj2.getSubObjects()) {
                    // link with parent
                    first = true;
                    if (firstChild) {
                        query += withQuery;
                        firstChild = false;
                    }
                    query += "\nMATCH (" + obj2.getClearAlias().toLowerCase() + "_" + childObj.getClearAlias().toLowerCase() + ":OBJECT:OBJ_" + childObj.getClearAlias() + ") -[:REFERENCE]-> (:RECORD:TAB_" + childObj.getTableName() + " {";
                    for (ForeignKey fk : childObj.getForeignKeys()) {
                        if (first) {
                            query += " ";
                            first = false;
                        } else {
                            query += ", ";
                        }
                        query += fk.getCurrent() + " : $" + fk.getOuter();
                        graphQuery.addKey(fk.getOuter());
                    }
                    query += " }) -[:OF]-> (:TABLE:" + childObj.getTableName() + ")";
                    mergeQuery += "\nMERGE (" + obj2.getClearAlias().toLowerCase() + ")<-[:PART_OF]-(" + obj2.getClearAlias().toLowerCase() + "_" + childObj.getClearAlias().toLowerCase() + ") ";
                }
            }
            query += mergeQuery;
            graphQuery.setQuery(query);
        }

    }

    public GraphQuery getQueryForTable(String table) {
        return queries.get(table);
    }

}
