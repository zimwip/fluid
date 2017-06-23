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

/**
 *
 * @author TZIMMER
 */
public class GraphQuery {

    private String query;
    private final Set<String> keys = new HashSet<>();
    private final Map<String, Object> basicVariable = new HashMap<>();

    public GraphQuery() {
    }
    
    public void setQuery(String query)
    {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public void addKey(String key) {
        keys.add(key);
    }
    
    public void addBasicValue(String key, String value)
    {
        basicVariable.put(key, value);
    }
    
    public Map<String, Object> getInitParameter()
    {
        return new HashMap<>(basicVariable);
    }

    public Set<String> getKeys() {
        return Collections.unmodifiableSet(keys);
    }

}
