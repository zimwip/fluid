/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.airbus.mapper.domain;

/**
 *
 * @author tzimmer
 */
public class PrimaryKey {

    private final String type;
    private final String columnName;
        private final String refName;

    public PrimaryKey(String type, String columnName) {
        this.type = type;
        this.columnName = columnName;
        this.refName = "pk645_"+columnName.trim().replaceAll("\t", "_").toLowerCase();
    }

    public String getType() {
        return type;
    }
    
    public String getName() {
        return columnName;
    }

        public String getPkRefName() {
        return refName;
    }
    
}
