package org.airbus.mapper;

import java.util.ArrayList;
import java.util.List;
import org.airbus.mapper.domain.SqlObject;

public class QueryBuilder {
	private StringBuilder selectList = null;
	// private StringBuilder fromList = null;
	private List<String> condList = null;
	private List<String> fromList = null;
	private SqlObject indexedObject;

	/**
	 * @return the indexedObject
	 */
	public SqlObject getIndexedObject() {
		return indexedObject;
	}

	/**
	 * @param indexedObject
	 *            the indexedObject to set
	 */
	public void setIndexedObject(SqlObject indexedObject) {
		this.indexedObject = indexedObject;
	}

	public void addSelectList(String value) {
		if (selectList != null) {
			selectList.append(", ");
		} else {
			selectList = new StringBuilder("select ");
		}
		selectList.append(value + " ");
	}

	public void addFromList(String value) {
		if (fromList != null) {
			if (!fromList.contains((String) value + " ")) {
				fromList.add(", ");
			}
		} else {
			fromList = new ArrayList<String>();
			fromList.add("from ");
		}

		if (!fromList.contains((String) value + " ")) {
			fromList.add(value + " ");
		}
	}

	public void addCondList(String value) {
		if (condList != null) {
			if (!condList.contains((String) value + " ")) {
				condList.add("and ");
			}
		} else {
			condList = new ArrayList<String>();
			condList.add(" where ");
		}

		if (!condList.contains((String) value + " ")) {
			int index = condList.indexOf((String) value + "(+) ");
			if (index != -1) {
				condList.remove(index);
				condList.remove(condList.lastIndexOf("and "));
				condList.add(index, value + " ");

			}
			else{
				condList.add(value + " ");
			}
			

		}
	}

	/**
	 * @return the selectList
	 */
	public String getSelectList() {
		if (selectList == null) {
			selectList = new StringBuilder();
		}
		return selectList.toString();
	}

	/**
	 * @return the fromList
	 */
	public String getFromList() {
		String str = "";
		if (fromList == null) {
			fromList = new ArrayList<String>();
		}
		for (String value : fromList) {
			str += value;
		}
		return str;
	}

	/**
	 * @return the condList
	 */
	public String getCondList() {
		String str = "";
		if (condList == null) {
			condList = new ArrayList<String>();
		}
		for (String value : condList) {
			str += value;
		}
		return str;
	}
        
        public String build()
        {
            return selectList.append(getFromList()).append(getCondList()).toString();
        }

}
