/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kafka.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author TZIMMER
 */
public class Query {

    private final String query;
    private final Map<String, Map<String, Object>> parameters = new HashMap<>();

    public Query(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public Map<String, Object> createParameters(String pks) {
        Map<String, Object> parameter = new HashMap<>();
        parameters.put(pks, parameter);
        return parameter;
    }

    public Map<String, Object> getParameters(String pks) {
        return parameters.get(pks);
    }

    public List<Map<String, Object>> getParameterList() {
        List<Map<String, Object>> parameterList = new ArrayList<>();
        for (Map<String, Object> parameter : parameters.values()) {
            parameterList.add(parameter);
        }
        return parameterList;
    }

}
