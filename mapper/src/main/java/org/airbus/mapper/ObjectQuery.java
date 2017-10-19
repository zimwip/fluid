/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.airbus.mapper;


import java.util.ArrayList;
import java.util.List;
import org.airbus.mapper.domain.ForeignKey;
import org.airbus.mapper.domain.QueryAttribute;
import org.airbus.mapper.domain.SqlObject;
import org.airbus.mapper.exception.IndexationException;

/**
 *
 * @author tzimmer
 */
public class ObjectQuery {

    // Document parent object
    private final SqlObject parentTopIndexedObject;
    private final SqlObject topIndexedObject;
    private final SqlObject mainIndexedObject;
    private final List<SqlObject> bodyIndexedObjectList;
    private final List<ObjectQuery> subQuery = new ArrayList<>();
    private final List<QueryAttribute> attributes = new ArrayList<>();
    private String query;

    public ObjectQuery(SqlObject mainIndexedObject) {
        this.mainIndexedObject = mainIndexedObject;
        SqlObject tempObject = mainIndexedObject.getParent();
        while (tempObject != null && tempObject.getParent() != null && !tempObject.isOnSeparatedDoc()) {
            tempObject = tempObject.getParent();
        }
        this.parentTopIndexedObject = tempObject;
        tempObject = mainIndexedObject;
        while (tempObject.getParent() != null && !tempObject.isOnSeparatedDoc()) {
            tempObject = tempObject.getParent();
        }
        this.topIndexedObject = tempObject;
        this.bodyIndexedObjectList = new ArrayList<>();
        this.attributes.addAll(mainIndexedObject.getAttributeList());
    }

    public void appendBodyObjectQuery(SqlObject body) {
        if (this.bodyIndexedObjectList.contains(body)) {
            throw new IndexationException("Object Already put in body !");
        }
        if (mainIndexedObject != body) {
            this.bodyIndexedObjectList.add(body);
            this.attributes.addAll(body.getAttributeList());
        }
    }

    public void addSubObjectQuery(ObjectQuery subObjectQuery) {
        this.subQuery.add(subObjectQuery);
    }

    public SqlObject getTopObject() {
        return topIndexedObject;
    }

    public SqlObject getParentTopObject() {
        return parentTopIndexedObject;
    }

    public SqlObject getMainObject() {
        return mainIndexedObject;
    }

    public List<ObjectQuery> getSubQuery() {
        return subQuery;
    }

    public List<QueryAttribute> getAttributes() {
        return attributes;
    }

    /**
     * Build the query based on SQLObject main and attribute.
     *
     * @return
     */
    public String getQuery() {
        if (query == null) {
            QueryBuilder theIndexedQuery = new QueryBuilder();
            // build the select part.
            attributes.forEach((queryAttribute) -> {
                if (queryAttribute.isRowData()) {
                    theIndexedQuery.addSelectList(queryAttribute.getObjectAlias() + "." + queryAttribute.getColumnDefinition() + " ");
                } else {
                    theIndexedQuery.addSelectList(queryAttribute.getColumnDefinition() + " ");
                }
            });
            // add topObject Identifier
            topIndexedObject.getPrimaryKeys().forEach((pk) -> {
                theIndexedQuery.addSelectList(topIndexedObject.getAlias() + "." + pk.getName() + " as " + pk.getPkRefName());
            });

            // add parent topObject Identifier if onSeparateDoc
            if (mainIndexedObject.isOnSeparatedDoc()) {
                parentTopIndexedObject.getPrimaryKeys().forEach((pk) -> {
                    theIndexedQuery.addSelectList(parentTopIndexedObject.getAlias() + "." + pk.getName() + " as parent_" + pk.getPkRefName());
                });
            }

            // build the from part and cond part towards parents onject.
            // build the upper part of SQL to get the mainObject 
            SqlObject toParentObject = this.mainIndexedObject.getParent();
            while (toParentObject != null) {
                // fromList for multiple objects
                theIndexedQuery.addFromList(toParentObject.getTableName() + " " + toParentObject.getAlias());

                // condList for multiple objects
                if (toParentObject.getParent() != null) {
                    for (ForeignKey foreignKey : toParentObject.getForeignKeys()) {
                        String outer = foreignKey.getOuter();
                        String cond = toParentObject.getParent().getAlias() + "." + outer;
                        cond += "=" + toParentObject.getAlias() + "." + foreignKey.getCurrent();
                        theIndexedQuery.addCondList(cond);
                    }
                }
                toParentObject = toParentObject.getParent();
            }

            // now build the body part from 
            // add attributes for the multiple Object
            theIndexedQuery.addFromList(mainIndexedObject.getTableName() + " " + mainIndexedObject.getAlias());
            bodyIndexedObjectList.forEach((body) -> {
                theIndexedQuery.addFromList(body.getTableName() + " " + body.getAlias());
            });
            // now build the cond body part.
            // fromList for main Object
            theIndexedQuery.addFromList(mainIndexedObject.getTableName() + " " + mainIndexedObject.getAlias());
            // condList for main Object
            for (ForeignKey foreignKey : mainIndexedObject.getForeignKeys()) {
                if (mainIndexedObject.getParent() != null) {
                   String outer = foreignKey.getOuter();
                        StringBuilder cond = new StringBuilder(mainIndexedObject.getParent().getAlias())
                                .append(".")
                                .append(outer);
                        if (!mainIndexedObject.isMultiple() && ("right").equals(mainIndexedObject.getJoin())) {
                            cond.append("(+)");
                        }
                        cond.append("=")
                                .append(mainIndexedObject.getAlias())
                                .append(".")
                                .append(foreignKey.getCurrent());
                        if (!mainIndexedObject.isMultiple() && ("left").equals(mainIndexedObject.getJoin()) && mainIndexedObject.getParent() != null) {
                            cond.append("(+)");
                        }
                        theIndexedQuery.addCondList(cond.toString());
                }
            }
            for (SqlObject body : bodyIndexedObjectList) {
                // condList for multiple objects
                for (ForeignKey foreignKey : body.getForeignKeys()) {
                    if (body.getParent() != null) {
                        String outer = foreignKey.getOuter();
                        StringBuilder cond = new StringBuilder(body.getParent().getAlias())
                                .append(".")
                                .append(outer);
                        if (!body.isMultiple() && ("right").equals(body.getJoin())) {
                            cond.append("(+)");
                        }
                        cond.append("=")
                                .append(body.getAlias())
                                .append(".")
                                .append(foreignKey.getCurrent());
                        if (!body.isMultiple() && ("left").equals(body.getJoin()) && body.getParent() != null) {
                            cond.append("(+)");
                        }
                        theIndexedQuery.addCondList(cond.toString());
                    }
                }
            }

            // addind primary key from our main object
            SqlObject mainObject = this.getMainObject().getMainObject();
            mainObject.getPrimaryKeys().forEach((primaryKey) -> {
                theIndexedQuery.addCondList(mainObject.getAlias() + "." + primaryKey.getName() + "=:"
                        + primaryKey.getName().toLowerCase());
            });
            query = theIndexedQuery.build();
        }
        return query;
    }

    @Override
    public String toString() {
        return "ObjectQuery{" + "attributes=" + attributes + ", query=" + query + '}';
    }

}
