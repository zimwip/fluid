package org.airbus.mapper.domain;

import java.util.ArrayList;
import java.util.List;

public class SqlObject {
   

    private final String tableName;
    private final String alias;
    private final String clear_alias;
    private final SqlObject parent;
    private final String join;
    private final boolean isIndexed, isMultiple;
    private final INCLUSION_STRATEGY inclusionStrategy;
    private final List<QueryAttribute> attributeList = new ArrayList<>();
    private final List<SqlObject> subObjects = new ArrayList<>();
    private final List<PrimaryKey> primaryKeys = new ArrayList<>();
    private final List<ForeignKey> foreignKeys = new ArrayList<>();
    private final List<Filter> filterList = new ArrayList<>();

    public SqlObject(String tableName, String alias, SqlObject parent, String join, boolean isIndexed,
            INCLUSION_STRATEGY inclusionStrategy, boolean isMultiple) {
        this.tableName = tableName;
        this.alias = alias;
        this.clear_alias = alias.toUpperCase().replaceAll("[ -]", "_");
        this.parent = parent;
        this.isIndexed = isIndexed;
        this.inclusionStrategy = inclusionStrategy;
        this.isMultiple = isMultiple;
        if (join == null) {
            this.join = "";
        } else {
            this.join = join;
        }
    }

    public void addPrimaryKey(PrimaryKey primaryKey) {
        this.primaryKeys.add(primaryKey);
    }

    public void addForeignKey(ForeignKey foreignKey) {
        this.foreignKeys.add(foreignKey);
    }

    public void addSubIndexedObject(SqlObject subIndexedObject) {
        this.subObjects.add(subIndexedObject);
    }

    public void addFilterList(Filter filter) {
        this.filterList.add(filter);
    }

    /**
     * Add attribute into the list
     *
     * @param value *
     */
    public void appendAttribute(QueryAttribute value) {
        this.attributeList.add(value);
    }

    /**
     * Getters and setters part
     *
     * @return
     */
    public boolean isIndexed() {
        return this.isIndexed;
    }

    public String getTableName() {
        return tableName;
    }

    public SqlObject getParent() {
        return parent;
    }

    public List<SqlObject> getSubObjects() {
        return subObjects;
    }

    public List<PrimaryKey> getPrimaryKeys() {
        return primaryKeys;
    }

    public List<QueryAttribute> getAttributeList() {
        return attributeList;
    }

    public List<ForeignKey> getForeignKeys() {
        return foreignKeys;
    }

    public List<Filter> getFilterList() {
        return filterList;
    }

    public String getAlias() {
        return alias;
    }
    
    public String getClearAlias() {
        return clear_alias;
    }

    public String getJoin() {
        return join;
    }

    public boolean isOnSeparatedDoc() {
        return inclusionStrategy != INCLUSION_STRATEGY.EMBEDDED;
    }
    
    public INCLUSION_STRATEGY getInclusionStrategyOption(){
        return inclusionStrategy;
    }

    public boolean isMultiple() {
        return isMultiple;
    }

    public SqlObject getMainObject() {
        return this.parent == null ? this : this.parent.getMainObject(); // return														// object
    }

    public SqlObject getParentObject() {
        SqlObject current = this;
        while (!(current.isMultiple || current.inclusionStrategy.equals(INCLUSION_STRATEGY.EMBEDDED))) {
            current = current.getParent();
        }
        return current;														// object
    }

}
