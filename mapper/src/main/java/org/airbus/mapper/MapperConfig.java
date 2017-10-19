/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.airbus.mapper;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import org.airbus.mapper.domain.ForeignKey;
import org.airbus.mapper.domain.ObjectIdentifier;
import org.airbus.mapper.domain.PrimaryKey;
import org.airbus.mapper.domain.SqlObject;
import org.airbus.mapper.exception.IndexationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author tzimmer
 */
public class MapperConfig {

    private final static Logger log = LoggerFactory.getLogger(MapperConfig.class);

    // Map containing all SQLObject.
    private final Map<String, SqlObject> mainObjectMap = new HashMap<>();
    private final Map<SqlObject, ObjectQuery> mainObjectObjectQueryMap = new HashMap<>();
    private final Map<SqlObject, String> objectMainObjectQueryMap = new HashMap<>();
    // Table name to SQLObject map
    private final Map<String, List<SqlObject>> objectLinkMap = new HashMap<>();

    public void addMainObject(String key, SqlObject object) {
        mainObjectMap.put(key, object);
    }

    public void addObjectToList(String key, SqlObject object) {
        List<SqlObject> theList = objectLinkMap.get(key.toUpperCase());
        if (theList == null) {
            theList = new ArrayList<>();
            objectLinkMap.put(key.toUpperCase(), theList);
        }
        theList.add(object);
    }
    
    public Set<String> getTableSet()
    {
        return Collections.unmodifiableSet(objectLinkMap.keySet());
    }

    private String getQueryForMainObject(SqlObject childObject) {
        if (!objectMainObjectQueryMap.containsKey(childObject)) {
            QueryBuilder theQueryBuilder = new QueryBuilder();
            Stack<SqlObject> lifo = new Stack<>();

            // Build parent object hierarchie
            // push the object in the stack
            lifo.push(childObject);
            SqlObject myParent = childObject.getParent();
            while (myParent != null) {
                lifo.push(myParent);
                myParent = myParent.getParent();
            }

            // descend down the hierarchy
            SqlObject previousObject;
            SqlObject currentObject = lifo.pop();
            // build select part from main

            // Here working on parent object.
            for (PrimaryKey primaryKey : currentObject.getPrimaryKeys()) {
                theQueryBuilder.addSelectList(currentObject.getAlias() + "." + primaryKey.getName());
            }

            // build from and where part
            // from
            theQueryBuilder.addFromList(currentObject.getTableName() + " " + currentObject.getAlias());
            while (!lifo.isEmpty()) {
                previousObject = currentObject;
                currentObject = lifo.pop();
                // from
                theQueryBuilder.addFromList(currentObject.getTableName() + " " + currentObject.getAlias());

                // where
                for (ForeignKey foreignKey : currentObject.getForeignKeys()) {
                    String outer = foreignKey.getOuter();
                    StringBuilder condition = new StringBuilder();
                    condition.append(previousObject.getAlias()).append(".").append(outer);
                    if (!currentObject.isMultiple() && ("right").equals(currentObject.getJoin())) {
                        condition.append("(+)");
                    }
                    condition.append("=")
                            .append(currentObject.getAlias())
                            .append(".")
                            .append(foreignKey.getCurrent());
                    if (!currentObject.isMultiple() && ("left").equals(currentObject.getJoin())) {
                        condition.append("(+)");
                    }
                    theQueryBuilder.addCondList(condition.toString());
                }
            }

            for (PrimaryKey primaryKey : currentObject.getPrimaryKeys()) {
                theQueryBuilder.addCondList(currentObject.getAlias() + "." + primaryKey.getName() + "=:" + primaryKey.getName().toLowerCase());
            }
            objectMainObjectQueryMap.put(childObject, theQueryBuilder.build());
        }
        return objectMainObjectQueryMap.get(childObject);
    }
    
    public List<SqlObject> getObjectForTable(String table_name)
    {
        return objectLinkMap.get(table_name);
    }

    private List<ObjectIdentifier> getMainObjectList(ObjectIdentifier source) throws SQLException {

        List<SqlObject> myObjectList = objectLinkMap.get(source.getTableName().toUpperCase());
        if (myObjectList == null) {
            return Collections.EMPTY_LIST;
        } else {
            Connection conn = null;
            try {
                List<ObjectIdentifier> mainObjectIdentifierList = new ArrayList<>();
                // Iterate for all IndexedObject and query mainObject.
                for (SqlObject obj : myObjectList) {
                    String mainObjectQuery = getQueryForMainObject(obj);
                    log.debug(mainObjectQuery);
                }
                return mainObjectIdentifierList;
            } finally {
                if (conn != null) {
                    conn.close();
                }
            }
        }
    }


    /* Return true if the object is indexable */
    public boolean isIndexable(ObjectIdentifier objectToIndex) {
        return mainObjectMap.get(objectToIndex.getTableName()) != null;
    }

    /**
     *
     * @param source
     * @return
     */
    public List<ObjectIdentifier> getMainObjects(ObjectIdentifier source) {
        List<ObjectIdentifier> theList = new ArrayList<>();
        try {
            /* sub object case */
            theList.addAll(getMainObjectList(source));
        } catch (SQLException ex) {
            throw new IndexationException(ex);
        }
        /* main object case */
        if (theList.isEmpty()) {
            theList.add(source);
        }
        return theList;
    }

    // Get the longest chain of Object without needing a break starting from parameter Object
    private void buildBodyObjectListLifo(SqlObject currentObject, Stack<SqlObject> lifo) {
        lifo.push(currentObject);
        for (SqlObject myIndexedObject : currentObject.getSubObjects()) {
            if (!myIndexedObject.isMultiple() && !myIndexedObject.isOnSeparatedDoc()) {
                buildBodyObjectListLifo(myIndexedObject, lifo);
            }
        }
    }

    private void buildMultipleObject(SqlObject currentObject, List<SqlObject> result) {
        // retrieve Multiple Objects
        for (SqlObject indexObject : currentObject.getSubObjects()) {
            if (indexObject.isMultiple() || indexObject.isOnSeparatedDoc()) {
                result.add(indexObject);
            }
            buildMultipleObject(indexObject, result);
        }
    }

    /**
     * Build the query hierarchy needed to fill in Document.
     *
     * @param mainObject
     * @return
     */
    public ObjectQuery getDocumentQuery(SqlObject mainObject) {
        if (!mainObjectObjectQueryMap.containsKey(mainObject)) {
            // retrieve Multiple Objects
            List<SqlObject> multipleObjectList = new ArrayList<>();
            buildMultipleObject(mainObject, multipleObjectList);

            // Declaration
            ObjectQuery theObjectQuery = populateObjectQuery(mainObject);

            // get query for multiple objects
            for (SqlObject mainObjectMultiple : multipleObjectList) {
                theObjectQuery.addSubObjectQuery(populateObjectQuery(mainObjectMultiple));
            }
            mainObjectObjectQueryMap.put(mainObject, theObjectQuery);
        }
        return mainObjectObjectQueryMap.get(mainObject);
    }

    private ObjectQuery populateObjectQuery(SqlObject mainObject) {

        ObjectQuery theObjectQuery = new ObjectQuery(mainObject);
        // Instructions
        // push in the stack lifo
        Stack<SqlObject> lifoIndexedObject = new Stack<>();
        buildBodyObjectListLifo(mainObject, lifoIndexedObject);

        // Query for main Object
        // add attributes for the main Object
        while (!lifoIndexedObject.isEmpty()) {
            SqlObject currentObject = lifoIndexedObject.pop();
            theObjectQuery.appendBodyObjectQuery(currentObject);
        }
        return theObjectQuery;
    }

    private ObjectIdentifier getIdentifier(ResultSet results, ObjectQuery query) throws SQLException {
        ObjectIdentifier.OidBuilder builder = ObjectIdentifier.Builder().tableName(query.getTopObject().getTableName());
        for (PrimaryKey pk : query.getTopObject().getPrimaryKeys()) {
            Object value = results.getObject(pk.getPkRefName());
            builder.pk(pk.getName(), value);
        }
        return builder.build();
    }

    private ObjectIdentifier getParentIdentifier(ResultSet results, ObjectQuery query) throws SQLException {
        ObjectIdentifier.OidBuilder builder = ObjectIdentifier.Builder().tableName(query.getParentTopObject().getTableName());
        for (PrimaryKey pk : query.getParentTopObject().getPrimaryKeys()) {
            Object value = results.getObject("parent_" + pk.getPkRefName());
            builder.pk(pk.getName(), value);
        }
        return builder.build();
    }

    /**
     * Get mandatory string value from a query *
     */
    private static String getStringMandatoryValueForSpecifiedName(ResultSet aRes, String aColName) throws SQLException {
        String theValue;
        theValue = aRes.getString(aColName);
        if (aRes.wasNull()) {
            theValue = null;
        }
        return theValue;
    }

}
