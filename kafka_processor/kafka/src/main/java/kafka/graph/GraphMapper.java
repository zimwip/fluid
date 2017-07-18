/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kafka.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final MapperConfig config;
    private final Map<String, GraphPreparedQuery> queries = new HashMap<>();

    @Autowired
    @Qualifier("neo4jDriver")
    protected Driver driver;

    public GraphMapper() {
        config = XmlParser.initialize("/conf.xml");
    }

    @PostConstruct
    private void initialize() {
        Map<String, String> indexQuery = new HashMap<>();
        indexQuery.put("TABLE", "CREATE INDEX ON :TABLE (tableName)");
        indexQuery.put("VERSION_FROM", "CREATE INDEX ON :VERSION (tx)");
        indexQuery.put("VERSION_TO", "CREATE INDEX ON :VERSION (to)");
        for (String table : config.getTableSet()) {
            GraphPreparedQuery.Builder builder = new GraphPreparedQuery.Builder();
            builder.baseVal("max_val", Long.MAX_VALUE);
            List<SqlObject> objects = config.getObjectForTable(table);
            if (objects == null) {
                return;
            }

            Map<String, PrimaryKey> pksh = new HashMap<>();
            Set<String> fksh = new HashSet<>();
            boolean first;
            for (SqlObject obj : objects) {
                String index = "CREATE INDEX ON :TAB_" + table + "( ";
                first = true;
                for (PrimaryKey pk : obj.getPrimaryKeys()) {
                    pksh.put(pk.getName(), pk);
                    builder.pk(pk.getName());
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
                indexQuery.put("TAB_" + table, finalIndex);
                // handle SubObject Reference
                for (ForeignKey fk : obj.getForeignKeys()) {
                    if (pksh.get(fk.getCurrent()) == null) { // do not add fk if already in pk.
                        fksh.add(fk.getCurrent());
                    }
                }
                for (SqlObject subObject : obj.getSubObjects()) {
                    for (ForeignKey fk : subObject.getForeignKeys()) {
                        if (pksh.get(fk.getOuter()) == null) { // do not add fk if already in pk.
                            fksh.add(fk.getOuter());
                        }
                    }
                }
            }
            List<PrimaryKey> pks = new ArrayList<>(pksh.values());
            List<String> fks = new ArrayList<>(fksh);

            first = true;
            // create (TABLE) - (RECORD) level.
            String query = "MERGE (table:TABLE { tableName:$table}) WITH table MERGE (table) <-[:OF]- (record:RECORD:TAB_" + table + " {";
            builder.baseVal("table", table);
            for (PrimaryKey pk : pks) {
                if (first) {
                    query += " ";
                    first = false;
                } else {
                    query += ", ";
                }
                query += pk.getName() + " : $" + pk.getName();
                builder.key(pk.getName());
            }
            // Existing version insert merge
            query += " }) WITH record\n";
            query += "MERGE (tx:TX {tx:$tx})\n";
            query += "CREATE (tx) <-[:INCLUDE_IN]- (version:VERSION {tx: $tx}) -[:VERSION_OF]-> (record)";
            query = query + "\nSET version.to = $max_val ";
            // Insert data here
//            query = query + " SET";
//            first = true;
            for (PrimaryKey pk : pks) {
//                    if (first) {
//                        query += " ";
//                        first = false;
//                    } else {
//                        query += ", ";
//                    }
                query += ", ";
                query += "version." + pk.getName() + " = $" + pk.getName();
                builder.key(pk.getName());
            }
            for (String fk : fks) {
                query += ",\n ";
                query += "version." + fk + " = $" + fk;
                builder.key(fk);
            }
            // Now create record.
            query += "\nWITH record, version\n";
            query += "OPTIONAL MATCH (pversion:VERSION) -[:VERSION_OF]-> (record) WHERE pversion.tx < $tx AND pversion.to > $tx ";
            // Existing version insert merge
            query += "WITH record, version, collect(pversion) as previous\n";
            query += "FOREACH (prev IN previous | ";
            query += "CREATE (version) -[:PREVIOUS]-> (pversion) ";
            query = query + "\nSET version.to = pversion.to";
            query += ", pversion.to = $tx )";
            // end insert data
            String withQuery = "\n WITH record";
            String mergeQuery = "";
            query += withQuery;
            for (SqlObject obj : objects) {
                // link with object.
                query += "\nMERGE (" + obj.getClearAlias().toLowerCase() + ":OBJECT:OBJ_" + obj.getClearAlias() + "  { name:$" + obj.getClearAlias() + "_name })-[:REFERENCE]->(record) ";
                builder.baseVal(obj.getClearAlias() + "_name", obj.getAlias());
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
                    builder.key(fk.getCurrent());
                }
                query += " }) -[:OF]-> (:TABLE {tableName:$" + obj.getParent().getClearAlias().toLowerCase() + "Table})";
                builder.baseVal(obj.getParent().getClearAlias().toLowerCase() + "Table", obj.getParent().getTableName());
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
                        builder.key(fk.getOuter());
                    }
                    query += " }) -[:OF]-> (:TABLE {tableName:$" + childObj.getClearAlias().toLowerCase() + "Table})";
                    builder.baseVal(childObj.getClearAlias().toLowerCase() + "Table", childObj.getTableName());
                    mergeQuery += "\nMERGE (" + obj2.getClearAlias().toLowerCase() + ")<-[:PART_OF]-(" + obj2.getClearAlias().toLowerCase() + "_" + childObj.getClearAlias().toLowerCase() + ") ";
                }
            }
            query += mergeQuery;
            builder.query(query);
            queries.put(table, builder.build());
        }
        // Update INDEX if needed
        try (org.neo4j.driver.v1.Session session = driver.session()) {
            session.writeTransaction((Transaction tx) -> {
                for (String query : indexQuery.values()) {
                    StatementResult rs = tx.run(query);
                    logger.info("INDEX Query {} : process index in store index added {}", query, rs.summary().counters().indexesAdded());
                }
                return 0L;
            });
        }
    }

    public GraphPreparedQuery getQueryForTable(String table) {
        return queries.get(table);
    }

}
