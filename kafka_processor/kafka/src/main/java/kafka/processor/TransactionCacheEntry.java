/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kafka.processor;

import java.util.HashMap;
import java.util.Map;
import kafka.graph.Query;

/**
 *
 * @author TZIMMER
 */
public class TransactionCacheEntry {
    
    private final String key;
    private final long cache_date;
    private final Map<String, Query> queries;
    
    public TransactionCacheEntry(String key)
    {
        this.key = key;
        this.cache_date = System.currentTimeMillis();
        queries = new HashMap<>();
    }

    public String getKey() {
        return key;
    }

    public long getCache_date() {
        return cache_date;
    }

    public Map<String, Query> getQueries() {
        return queries;
    }
    
    
    
}
