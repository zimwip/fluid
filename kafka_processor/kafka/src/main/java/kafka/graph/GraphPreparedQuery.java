/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kafka.graph;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import kafka.avro.CDCEvent;
import kafka.avro.Column;

/**
 *
 * @author TZIMMER
 */
public class GraphPreparedQuery {

    private final String query;
    private final Set<String> keys;
    private final Set<String> pKeys;
    private final Map<String, Object> baseVariable;

    public GraphPreparedQuery(String query, Set<String> pKeys, Set<String> keys, Map<String, Object> baseVariable) {
        this.query = query;
        this.keys = Collections.unmodifiableSet(keys);
        this.pKeys = Collections.unmodifiableSet(pKeys);
        this.baseVariable = Collections.unmodifiableMap(baseVariable);
    }

    public String query() {
        return query;
    }

    public Map<String, Object> getInitParameter() {
        return new HashMap<>(baseVariable);
    }

    public String pks(CDCEvent event) {
        String pkVal = "";
        for (String pk : pKeys) {
            for (Column col : event.getAfter()) {
                if (col.getName().equals(pk)) {
                    pkVal += col.getValue().toString();
                    break;
                }
            }
        }
        return pkVal;
    }

    public Set<String> keys() {
        return keys;
    }

    public static class Builder {

        private String query;
        private final Set<String> keys = new HashSet<>();
        private final Set<String> pKeys = new HashSet<>();
        private final Map<String, Object> baseVariable = new HashMap<>();

        public Builder() {
        }

        public void query(String query) {
            this.query = query;
        }

        public void key(String key) {
            this.keys.add(key);
        }

        public void pk(String key) {
            this.pKeys.add(key);
        }

        public void baseVal(String key, Object value) {
            this.baseVariable.put(key, value);
        }

        public GraphPreparedQuery build() {
            return new GraphPreparedQuery(query, pKeys, keys, baseVariable);
        }

    }

}
