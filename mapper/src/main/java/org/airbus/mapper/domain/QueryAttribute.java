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
public class QueryAttribute {

    private final SqlObject parent;
    private final boolean doNotCopy;
    private final boolean rowData;
    private final String type;
    private final String mimeType;
    private final String idxName;
    private final String content;
    private final String columnDefinition;

    public QueryAttribute(SqlObject parent, boolean doNotCopy, boolean rowData, String type, String mimeType, String idxName, String content, String columnDefinition) {
        this.parent = parent;
        this.doNotCopy = doNotCopy;
        this.rowData = rowData;
        this.type = type;
        this.mimeType = mimeType;
        this.idxName = idxName;
        this.content = content;
        this.columnDefinition = columnDefinition;
    }

    public boolean isRowData() {
        return rowData;
    }

    public String getColumnDefinition() {
        return columnDefinition;
    }

    public String getIdxName() {
        return idxName;
    }

    public boolean isDoNotCopy() {
        return doNotCopy;
    }

    public String getType() {
        return type;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "QueryAttribute{" + "type=" + type + ", mimeType=" + mimeType + ", idxName=" + idxName + ", columnDefinition=" + columnDefinition + '}';
    }

    public String getObjectAlias() {
        return parent.getAlias();
    }
    

}
