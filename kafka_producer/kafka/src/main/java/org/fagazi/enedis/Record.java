
package org.fagazi.enedis;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "datasetid",
    "recordid",
    "fields",
    "record_timestamp"
})
public class Record {

    @JsonProperty("datasetid")
    private String datasetid;
    @JsonProperty("recordid")
    private String recordid;
    @JsonProperty("fields")
    private Fields fields;
    @JsonProperty("record_timestamp")
    private String recordTimestamp;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("datasetid")
    public String getDatasetid() {
        return datasetid;
    }

    @JsonProperty("datasetid")
    public void setDatasetid(String datasetid) {
        this.datasetid = datasetid;
    }

    public Record withDatasetid(String datasetid) {
        this.datasetid = datasetid;
        return this;
    }

    @JsonProperty("recordid")
    public String getRecordid() {
        return recordid;
    }

    @JsonProperty("recordid")
    public void setRecordid(String recordid) {
        this.recordid = recordid;
    }

    public Record withRecordid(String recordid) {
        this.recordid = recordid;
        return this;
    }

    @JsonProperty("fields")
    public Fields getFields() {
        return fields;
    }

    @JsonProperty("fields")
    public void setFields(Fields fields) {
        this.fields = fields;
    }

    public Record withFields(Fields fields) {
        this.fields = fields;
        return this;
    }

    @JsonProperty("record_timestamp")
    public String getRecordTimestamp() {
        return recordTimestamp;
    }

    @JsonProperty("record_timestamp")
    public void setRecordTimestamp(String recordTimestamp) {
        this.recordTimestamp = recordTimestamp;
    }

    public Record withRecordTimestamp(String recordTimestamp) {
        this.recordTimestamp = recordTimestamp;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public Record withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

}
