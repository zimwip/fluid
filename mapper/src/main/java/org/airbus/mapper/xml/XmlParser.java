package org.airbus.mapper.xml;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.Document;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import org.airbus.mapper.MapperConfig;
import org.airbus.mapper.domain.Filter;
import org.airbus.mapper.domain.ForeignKey;
import org.airbus.mapper.domain.INCLUSION_STRATEGY;
import org.airbus.mapper.domain.PrimaryKey;
import org.airbus.mapper.domain.QueryAttribute;
import org.airbus.mapper.domain.SqlObject;
import org.airbus.mapper.exception.IndexationException;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.xml.sax.SAXException;

public class XmlParser {

    private final static Logger log = LoggerFactory.getLogger(XmlParser.class);
    static Document document;
    static Element racine;
    private static final MapperConfig config = new MapperConfig();
    static final String NAME = "name";
    static final String ROWDATA = "rowData";
    static final String CURRENT = "current";
    static final String MIMETYPE = "mimetype";
    static final String CONTENT = "content";
    static final String DONOTCOPY = "doNotCopy";
    static final String JOIN = "join";
    static final String TYPE = "type";
    static final String OUTER = "outer";
    static final String LINKDEFINITION = "LinkDefinition";
    static final String TRUE = "true";
    static final String FALSE = "false";
    static final String INDEXED = "indexed";
    static final String PATH_CONFIG_XSD = "/search/conf/conf.xsd";

    private static boolean error = false;

    /**
     * Create HashMap with conf information
     *
     *
     * @return Initialized Indexation Engine
     */
    public static MapperConfig initialize(String file) {

        try {
            log.info("***** Parser Initialize *****");
            /* Check the xml file from its xsd file */
            InputStream xml = XmlParser.class.getResourceAsStream(file);
            InputStream xsd = XmlParser.class.getResourceAsStream(PATH_CONFIG_XSD);
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new StreamSource(xsd));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(xml));
            xml.close();
            log.info("***** conf.xml is checked (y) ****");
            /* Instance of SAX builder */
            SAXBuilder sxb = new SAXBuilder();
            document = sxb.build(XmlParser.class.getResourceAsStream(file)); // build a document

            /* get the root node of the xml doc */
            racine = document.getRootElement();

            /* Browse all main object */
            log.info("Browse all main object");
            List<Element> theBusinessObjectList = racine.getChild("businessObjectList").getChildren("MainObject");
            Iterator<Element> theBusinessObjectIt = theBusinessObjectList.iterator();
            while (theBusinessObjectIt.hasNext()) {
                /* Get the object */
                Element theBusinessObjectElement = theBusinessObjectIt.next();
                SqlObject theIndexedObject;
                /*
                 * recursive call to create main query and subqueries for all sub
		 * objects
                 */

                theIndexedObject = buildIndexedObject(null, theBusinessObjectElement);

                config.addMainObject(theBusinessObjectElement.getAttributeValue("tableName").toUpperCase(), theIndexedObject);

            }

        } catch (IOException | JDOMException | SAXException e) {
            throw new IndexationException(e);
        }
        if (error) {
            log.error("Indexation Engine Initialisation failed ! ");
            throw new IndexationException("Indexation Engine Initialisation failed");
        }

        return config;

    }

    /**
     * Return query for the Upper Object *
     */
    private static SqlObject buildIndexedObject(SqlObject parentObject, Element subElement) {

        String tableName = subElement.getAttributeValue("tableName");
        String alias = subElement.getAttributeValue(NAME);
        String join = subElement.getAttributeValue(JOIN) == null ? "" : subElement.getAttributeValue(JOIN);

        boolean isIndexed = subElement.getAttributeValue(INDEXED) == null || (TRUE).equals(subElement.getAttributeValue(INDEXED));
        /* Check if the query must be in a separated Solr document */
        INCLUSION_STRATEGY isOnSeparatedDoc = INCLUSION_STRATEGY.EMBEDDED; // DEfault
        if (("flat").equalsIgnoreCase(subElement.getAttributeValue("inclStrategy"))) {
            isOnSeparatedDoc = INCLUSION_STRATEGY.FLAT;
        }
        if (("tree").equalsIgnoreCase(subElement.getAttributeValue("inclStrategy"))) {
            isOnSeparatedDoc = INCLUSION_STRATEGY.TREE;
        }
        /* check if the subobject is multiple */
        boolean isMultiple = (TRUE).equals(subElement.getAttributeValue("multiple"));

        SqlObject currentObject = new SqlObject(tableName, alias, parentObject, join, isIndexed, isOnSeparatedDoc, isMultiple);

        //  replace by mainObject PK List
        List<Element> thePrimaryKeyList;
        thePrimaryKeyList = subElement.getChild("PrimaryKeyDefinition").getChildren("PrimaryKey");
        Iterator<Element> thePrimaryKeyIt = thePrimaryKeyList.iterator();
        String key;
        PrimaryKey primaryKey;
        while (thePrimaryKeyIt.hasNext()) {
            Element thePrimaryKey = thePrimaryKeyIt.next();
            key = thePrimaryKey.getText();
            primaryKey = new PrimaryKey(thePrimaryKey.getAttributeValue("type"), key);
            currentObject.addPrimaryKey(primaryKey);
        }

        //Forreign Key
        List<Element> theForeignKeyList;
        theForeignKeyList = subElement.getChild(LINKDEFINITION).getChildren("ForeignKey");
        Iterator<Element> theForeignKeyIt = theForeignKeyList.iterator();
        String outer;
        String current;
        ForeignKey foreignKey;
        while (theForeignKeyIt.hasNext()) {
            Element theForeignKey = theForeignKeyIt.next();
            current = theForeignKey.getText();
            outer = theForeignKey.getAttributeValue(OUTER);
            foreignKey = new ForeignKey(current, outer);
            currentObject.addForeignKey(foreignKey);
        }

        //add Filter for evrey object
        Filter filter = new Filter();
        if (subElement.getChild(LINKDEFINITION).getChild("Filter") != null) {
            List<Element> theCriteriaList = subElement.getChild(LINKDEFINITION).getChild("Filter").getChildren("Criteria");
            Iterator<Element> theCriteriaIt = theCriteriaList.iterator();
            boolean rowDataCriteria;
            while (theCriteriaIt.hasNext()) {
                Element theCriteria = theCriteriaIt.next();
                filter.setClazz(theCriteria.getAttributeValue("class"));
                filter.setName(theCriteria.getAttributeValue("name"));
                rowDataCriteria = theCriteria.getAttributeValue("rowData") == null;
                filter.setRowData(rowDataCriteria);
                List<Element> theParamList = theCriteria.getChildren("Param");
                Iterator<Element> theParamIt = theParamList.iterator();
                // check the filter is available
                try {
            //        FilterProvider.getInstance().getFilter(filter.getClazz());
                } catch (IndexationException e) {
                    log.error("Missing type mapper for type [{}] on {}.{}", filter.getClazz(), currentObject.getTableName(), filter.getName());
                    error = true;
                }
                String nameParam;
                String contentParam;
                while (theParamIt.hasNext()) {
                    Element theParam = theParamIt.next();
                    nameParam = theParam.getAttributeValue("name");
                    contentParam = theParam.getText();
                    filter.addParams(contentParam, nameParam);
                }
                currentObject.addFilterList(filter);
            }
        }

        config.addObjectToList(tableName, currentObject);

        // TODO Attribute
        /* Get all attributes */
        List<Element> theAttributsList = subElement.getChild("AttributesList").getChildren("Attribute");
        /* Browse all attributes */
        Iterator<Element> theAttributesIt = theAttributsList.iterator();
        while (theAttributesIt.hasNext()) {
            Element theAttribute = theAttributesIt.next();
            /* Set the name of the attribute to create the solr document */
            QueryAttribute queryAttribute;
            String name = theAttribute.getAttributeValue(NAME);
            boolean rowData = theAttribute.getAttributeValue(ROWDATA) == null ? true : (TRUE).equals(theAttribute.getAttributeValue(ROWDATA));
            String type = theAttribute.getAttributeValue(TYPE);
            String content = theAttribute.getAttributeValue("content");
            String mimeType = theAttribute.getAttributeValue(MIMETYPE);
            String columnDefinition = theAttribute.getText();
            // check the type is available
            try {
    //            MapperProvider.getInstance().getMapper(type);
            } catch (IndexationException e) {
                log.error("Missing type mapper for type [{}] on {}.{}", type, currentObject.getTableName(), columnDefinition);
                error = true;
            }

            if (mimeType == null) {
                mimeType = "";
            }
            boolean doNotCopy = theAttribute.getAttributeValue(DONOTCOPY) != null;
            queryAttribute = new QueryAttribute(currentObject, doNotCopy, rowData, type, mimeType, name, content, columnDefinition);
            /* Set the type of the attribute */
            currentObject.appendAttribute(queryAttribute);
        }


        /* Get all sub objects */
        List<Element> theSubObjectList = subElement.getChild("SubObjectList").getChildren("SubObject");
        Iterator<Element> theSubObjectIt = theSubObjectList.iterator();

        /* Browse all sub objects */
        while (theSubObjectIt.hasNext()) {
            /* Get the sub object */
            Element theSubObject = theSubObjectIt.next();
            if (currentObject != null) {
                SqlObject currentSubObject = buildIndexedObject(currentObject, theSubObject);
                currentObject.addSubIndexedObject(currentSubObject);
            }
        }

        return currentObject;
    }

}
