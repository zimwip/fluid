
package org.fagazi.enedis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "nhits",
    "parameters",
    "records"
})
public class DataResponse {

    @JsonProperty("nhits")
    private Integer nhits;
    @JsonProperty("parameters")
    private Parameters parameters;
    @JsonProperty("records")
    private List<Record> records = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("nhits")
    public Integer getNhits() {
        return nhits;
    }

    @JsonProperty("nhits")
    public void setNhits(Integer nhits) {
        this.nhits = nhits;
    }

    public DataResponse withNhits(Integer nhits) {
        this.nhits = nhits;
        return this;
    }

    @JsonProperty("parameters")
    public Parameters getParameters() {
        return parameters;
    }

    @JsonProperty("parameters")
    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

    public DataResponse withParameters(Parameters parameters) {
        this.parameters = parameters;
        return this;
    }

    @JsonProperty("records")
    public List<Record> getRecords() {
        return records;
    }

    @JsonProperty("records")
    public void setRecords(List<Record> records) {
        this.records = records;
    }

    public DataResponse withRecords(List<Record> records) {
        this.records = records;
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

    public DataResponse withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

}
