
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
    "dataset",
    "timezone",
    "rows",
    "start",
    "sort",
    "format"
})
public class Parameters {

    @JsonProperty("dataset")
    private List<String> dataset = null;
    @JsonProperty("timezone")
    private String timezone;
    @JsonProperty("rows")
    private Integer rows;
    @JsonProperty("start")
    private Integer start;
    @JsonProperty("sort")
    private List<String> sort = null;
    @JsonProperty("format")
    private String format;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("dataset")
    public List<String> getDataset() {
        return dataset;
    }

    @JsonProperty("dataset")
    public void setDataset(List<String> dataset) {
        this.dataset = dataset;
    }

    public Parameters withDataset(List<String> dataset) {
        this.dataset = dataset;
        return this;
    }

    @JsonProperty("timezone")
    public String getTimezone() {
        return timezone;
    }

    @JsonProperty("timezone")
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Parameters withTimezone(String timezone) {
        this.timezone = timezone;
        return this;
    }

    @JsonProperty("rows")
    public Integer getRows() {
        return rows;
    }

    @JsonProperty("rows")
    public void setRows(Integer rows) {
        this.rows = rows;
    }

    public Parameters withRows(Integer rows) {
        this.rows = rows;
        return this;
    }

    @JsonProperty("start")
    public Integer getStart() {
        return start;
    }

    @JsonProperty("start")
    public void setStart(Integer start) {
        this.start = start;
    }

    public Parameters withStart(Integer start) {
        this.start = start;
        return this;
    }

    @JsonProperty("sort")
    public List<String> getSort() {
        return sort;
    }

    @JsonProperty("sort")
    public void setSort(List<String> sort) {
        this.sort = sort;
    }

    public Parameters withSort(List<String> sort) {
        this.sort = sort;
        return this;
    }

    @JsonProperty("format")
    public String getFormat() {
        return format;
    }

    @JsonProperty("format")
    public void setFormat(String format) {
        this.format = format;
    }

    public Parameters withFormat(String format) {
        this.format = format;
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

    public Parameters withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

}
