/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.airbus.mapper.domain;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author tzimmer
 */
public class Filter {
    
    private String clazz;
    private String name;
    private boolean rowData;
    private final Map<String,String> params = new HashMap<>();
    

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRowData(boolean rowDataCriteria) {
        this.rowData = rowDataCriteria;
    }

    public void addParams(String contentParam, String nameParam) {
        params.put(contentParam, nameParam);
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getClazz() {
        return clazz;
    }

    public String getName() {
        return name;
    }

    public boolean isRowData() {
        return rowData;
    }
    
}
