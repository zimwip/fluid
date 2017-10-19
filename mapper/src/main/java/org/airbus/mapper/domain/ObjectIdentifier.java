package org.airbus.mapper.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ObjectIdentifier {

    // SQL Table backing up the Object
    private final String tableName;
    // PK Column Name / Value used to retrieve the Object
    private final Map<String, Object> pKs; // => <column-name, column-value>

    private ObjectIdentifier(String tableName, Map<String, Object> pKs) {
        this.tableName = tableName;
        this.pKs = Collections.unmodifiableMap(pKs);
    }

    public String getTableName() {
        return tableName;
    }

    public Map<String, Object> getpKs() {
        return pKs;
    }

    public static class OidBuilder {

        private String tableName;
        private final Map<String, Object> pKs = new HashMap<>();

        private OidBuilder() {
        }

        public OidBuilder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public OidBuilder pk(String key, Object value) {
            pKs.put(key, value);
            return this;
        }

        public ObjectIdentifier build() {
            return new ObjectIdentifier(tableName, pKs);
        }
    }

    public static OidBuilder Builder() {
        return new OidBuilder();
    }

    @Override
    public String toString() {
        return "{oid:{" + "tableName:" + tableName + ", pKs:" + pKs + "}}";
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 11 * hash + Objects.hashCode(this.tableName);
        for (Map.Entry<String, Object> entry : this.pKs.entrySet()) {
            hash = 11 * hash + Objects.hashCode(entry.getKey());
            hash = 11 * hash + Objects.hashCode(entry.getValue());
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ObjectIdentifier other = (ObjectIdentifier) obj;
        if (!this.tableName.equals(other.tableName)) {
            return false;
        }
        if (this.pKs.size() != other.pKs.size()) {
            return false;
        }
        // check each key / value to be sure.
        for (Map.Entry<String, Object> entry1 : this.pKs.entrySet()) {
            boolean found = false;
            for (Map.Entry<String, Object> entry2 : other.pKs.entrySet()) {
                if (entry1.getKey().equals(entry2.getKey()) && entry1.getValue().equals(entry2.getValue())) {
                    found = true;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

}
