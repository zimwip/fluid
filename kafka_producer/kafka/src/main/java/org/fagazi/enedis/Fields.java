
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
public class Fields {

    @JsonProperty("production_telerelevee")
    private Long productionTelerelevee;
    @JsonProperty("production_profilee")
    private Long productionProfilee;
    @JsonProperty("mois")
    private String mois;
    @JsonProperty("production_totale")
    private Long productionTotale;
    @JsonProperty("pseudo_rayonnement")
    private Long pseudoRayonnement;
    @JsonProperty("consommation_profilee_ent_bt")
    private Long consommationProfileeEntBt;
    @JsonProperty("consommation_totale")
    private Long consommationTotale;
    @JsonProperty("temperature_reelle_lissee")
    private Double temperatureReelleLissee;
    @JsonProperty("consommation_profilee_ent_hta")
    private Long consommationProfileeEntHta;
    @JsonProperty("consommation_profilee")
    private Long consommationProfilee;
    @JsonProperty("production_profilee_photovoltaique")
    private Long productionProfileePhotovoltaique;
    @JsonProperty("pertes")
    private Long pertes;
    @JsonProperty("soutirage_vers_autres_grd")
    private Long soutirageVersAutresGrd;
    @JsonProperty("production_eolien")
    private Long productionEolien;
    @JsonProperty("soutirage_rte")
    private Long soutirageRte;
    @JsonProperty("production_photovoltaique")
    private Long productionPhotovoltaique;
    @JsonProperty("consommation_telerelevee")
    private Long consommationTelerelevee;
    @JsonProperty("horodate")
    private String horodate;
    @JsonProperty("consommation_profilee_pro")
    private Long consommationProfileePro;
    @JsonProperty("temperature_normale_lissee")
    private Double temperatureNormaleLissee;
    @JsonProperty("injection_rte")
    private Long injectionRte;
    @JsonProperty("consommation_profilee_res")
    private Long consommationProfileeRes;
    @JsonProperty("production_profilee_aut")
    private Long productionProfileeAut;
    @JsonProperty("consommation_hta")
    private Long consommationHta;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("production_telerelevee")
    public Long getProductionTelerelevee() {
        return productionTelerelevee;
    }

    @JsonProperty("production_telerelevee")
    public void setProductionTelerelevee(Long productionTelerelevee) {
        this.productionTelerelevee = productionTelerelevee;
    }

    public Fields withProductionTelerelevee(Long productionTelerelevee) {
        this.productionTelerelevee = productionTelerelevee;
        return this;
    }

    @JsonProperty("production_profilee")
    public Long getProductionProfilee() {
        return productionProfilee;
    }

    @JsonProperty("production_profilee")
    public void setProductionProfilee(Long productionProfilee) {
        this.productionProfilee = productionProfilee;
    }

    public Fields withProductionProfilee(Long productionProfilee) {
        this.productionProfilee = productionProfilee;
        return this;
    }

    @JsonProperty("mois")
    public String getMois() {
        return mois;
    }

    @JsonProperty("mois")
    public void setMois(String mois) {
        this.mois = mois;
    }

    public Fields withMois(String mois) {
        this.mois = mois;
        return this;
    }

    @JsonProperty("production_totale")
    public Long getProductionTotale() {
        return productionTotale;
    }

    @JsonProperty("production_totale")
    public void setProductionTotale(Long productionTotale) {
        this.productionTotale = productionTotale;
    }

    public Fields withProductionTotale(Long productionTotale) {
        this.productionTotale = productionTotale;
        return this;
    }

    @JsonProperty("pseudo_rayonnement")
    public Long getPseudoRayonnement() {
        return pseudoRayonnement;
    }

    @JsonProperty("pseudo_rayonnement")
    public void setPseudoRayonnement(Long pseudoRayonnement) {
        this.pseudoRayonnement = pseudoRayonnement;
    }

    public Fields withPseudoRayonnement(Long pseudoRayonnement) {
        this.pseudoRayonnement = pseudoRayonnement;
        return this;
    }

    @JsonProperty("consommation_profilee_ent_bt")
    public Long getConsommationProfileeEntBt() {
        return consommationProfileeEntBt;
    }

    @JsonProperty("consommation_profilee_ent_bt")
    public void setConsommationProfileeEntBt(Long consommationProfileeEntBt) {
        this.consommationProfileeEntBt = consommationProfileeEntBt;
    }

    public Fields withConsommationProfileeEntBt(Long consommationProfileeEntBt) {
        this.consommationProfileeEntBt = consommationProfileeEntBt;
        return this;
    }

    @JsonProperty("consommation_totale")
    public Long getConsommationTotale() {
        return consommationTotale;
    }

    @JsonProperty("consommation_totale")
    public void setConsommationTotale(Long consommationTotale) {
        this.consommationTotale = consommationTotale;
    }

    public Fields withConsommationTotale(Long consommationTotale) {
        this.consommationTotale = consommationTotale;
        return this;
    }

    @JsonProperty("temperature_reelle_lissee")
    public Double getTemperatureReelleLissee() {
        return temperatureReelleLissee;
    }

    @JsonProperty("temperature_reelle_lissee")
    public void setTemperatureReelleLissee(Double temperatureReelleLissee) {
        this.temperatureReelleLissee = temperatureReelleLissee;
    }

    public Fields withTemperatureReelleLissee(Double temperatureReelleLissee) {
        this.temperatureReelleLissee = temperatureReelleLissee;
        return this;
    }

    @JsonProperty("consommation_profilee_ent_hta")
    public Long getConsommationProfileeEntHta() {
        return consommationProfileeEntHta;
    }

    @JsonProperty("consommation_profilee_ent_hta")
    public void setConsommationProfileeEntHta(Long consommationProfileeEntHta) {
        this.consommationProfileeEntHta = consommationProfileeEntHta;
    }

    public Fields withConsommationProfileeEntHta(Long consommationProfileeEntHta) {
        this.consommationProfileeEntHta = consommationProfileeEntHta;
        return this;
    }

    @JsonProperty("consommation_profilee")
    public Long getConsommationProfilee() {
        return consommationProfilee;
    }

    @JsonProperty("consommation_profilee")
    public void setConsommationProfilee(Long consommationProfilee) {
        this.consommationProfilee = consommationProfilee;
    }

    public Fields withConsommationProfilee(Long consommationProfilee) {
        this.consommationProfilee = consommationProfilee;
        return this;
    }

    @JsonProperty("production_profilee_photovoltaique")
    public Long getProductionProfileePhotovoltaique() {
        return productionProfileePhotovoltaique;
    }

    @JsonProperty("production_profilee_photovoltaique")
    public void setProductionProfileePhotovoltaique(Long productionProfileePhotovoltaique) {
        this.productionProfileePhotovoltaique = productionProfileePhotovoltaique;
    }

    public Fields withProductionProfileePhotovoltaique(Long productionProfileePhotovoltaique) {
        this.productionProfileePhotovoltaique = productionProfileePhotovoltaique;
        return this;
    }

    @JsonProperty("pertes")
    public Long getPertes() {
        return pertes;
    }

    @JsonProperty("pertes")
    public void setPertes(Long pertes) {
        this.pertes = pertes;
    }

    public Fields withPertes(Long pertes) {
        this.pertes = pertes;
        return this;
    }

    @JsonProperty("soutirage_vers_autres_grd")
    public Long getSoutirageVersAutresGrd() {
        return soutirageVersAutresGrd;
    }

    @JsonProperty("soutirage_vers_autres_grd")
    public void setSoutirageVersAutresGrd(Long soutirageVersAutresGrd) {
        this.soutirageVersAutresGrd = soutirageVersAutresGrd;
    }

    public Fields withSoutirageVersAutresGrd(Long soutirageVersAutresGrd) {
        this.soutirageVersAutresGrd = soutirageVersAutresGrd;
        return this;
    }

    @JsonProperty("production_eolien")
    public Long getProductionEolien() {
        return productionEolien;
    }

    @JsonProperty("production_eolien")
    public void setProductionEolien(Long productionEolien) {
        this.productionEolien = productionEolien;
    }

    public Fields withProductionEolien(Long productionEolien) {
        this.productionEolien = productionEolien;
        return this;
    }

    @JsonProperty("soutirage_rte")
    public Long getSoutirageRte() {
        return soutirageRte;
    }

    @JsonProperty("soutirage_rte")
    public void setSoutirageRte(Long soutirageRte) {
        this.soutirageRte = soutirageRte;
    }

    public Fields withSoutirageRte(Long soutirageRte) {
        this.soutirageRte = soutirageRte;
        return this;
    }

    @JsonProperty("production_photovoltaique")
    public Long getProductionPhotovoltaique() {
        return productionPhotovoltaique;
    }

    @JsonProperty("production_photovoltaique")
    public void setProductionPhotovoltaique(Long productionPhotovoltaique) {
        this.productionPhotovoltaique = productionPhotovoltaique;
    }

    public Fields withProductionPhotovoltaique(Long productionPhotovoltaique) {
        this.productionPhotovoltaique = productionPhotovoltaique;
        return this;
    }

    @JsonProperty("consommation_telerelevee")
    public Long getConsommationTelerelevee() {
        return consommationTelerelevee;
    }

    @JsonProperty("consommation_telerelevee")
    public void setConsommationTelerelevee(Long consommationTelerelevee) {
        this.consommationTelerelevee = consommationTelerelevee;
    }

    public Fields withConsommationTelerelevee(Long consommationTelerelevee) {
        this.consommationTelerelevee = consommationTelerelevee;
        return this;
    }

    @JsonProperty("horodate")
    public String getHorodate() {
        return horodate;
    }

    @JsonProperty("horodate")
    public void setHorodate(String horodate) {
        this.horodate = horodate;
    }

    public Fields withHorodate(String horodate) {
        this.horodate = horodate;
        return this;
    }

    @JsonProperty("consommation_profilee_pro")
    public Long getConsommationProfileePro() {
        return consommationProfileePro;
    }

    @JsonProperty("consommation_profilee_pro")
    public void setConsommationProfileePro(Long consommationProfileePro) {
        this.consommationProfileePro = consommationProfileePro;
    }

    public Fields withConsommationProfileePro(Long consommationProfileePro) {
        this.consommationProfileePro = consommationProfileePro;
        return this;
    }

    @JsonProperty("temperature_normale_lissee")
    public Double getTemperatureNormaleLissee() {
        return temperatureNormaleLissee;
    }

    @JsonProperty("temperature_normale_lissee")
    public void setTemperatureNormaleLissee(Double temperatureNormaleLissee) {
        this.temperatureNormaleLissee = temperatureNormaleLissee;
    }

    public Fields withTemperatureNormaleLissee(Double temperatureNormaleLissee) {
        this.temperatureNormaleLissee = temperatureNormaleLissee;
        return this;
    }

    @JsonProperty("injection_rte")
    public Long getInjectionRte() {
        return injectionRte;
    }

    @JsonProperty("injection_rte")
    public void setInjectionRte(Long injectionRte) {
        this.injectionRte = injectionRte;
    }

    public Fields withInjectionRte(Long injectionRte) {
        this.injectionRte = injectionRte;
        return this;
    }

    @JsonProperty("consommation_profilee_res")
    public Long getConsommationProfileeRes() {
        return consommationProfileeRes;
    }

    @JsonProperty("consommation_profilee_res")
    public void setConsommationProfileeRes(Long consommationProfileeRes) {
        this.consommationProfileeRes = consommationProfileeRes;
    }

    public Fields withConsommationProfileeRes(Long consommationProfileeRes) {
        this.consommationProfileeRes = consommationProfileeRes;
        return this;
    }

    @JsonProperty("production_profilee_aut")
    public Long getProductionProfileeAut() {
        return productionProfileeAut;
    }

    @JsonProperty("production_profilee_aut")
    public void setProductionProfileeAut(Long productionProfileeAut) {
        this.productionProfileeAut = productionProfileeAut;
    }

    public Fields withProductionProfileeAut(Long productionProfileeAut) {
        this.productionProfileeAut = productionProfileeAut;
        return this;
    }

    @JsonProperty("consommation_hta")
    public Long getConsommationHta() {
        return consommationHta;
    }

    @JsonProperty("consommation_hta")
    public void setConsommationHta(Long consommationHta) {
        this.consommationHta = consommationHta;
    }

    public Fields withConsommationHta(Long consommationHta) {
        this.consommationHta = consommationHta;
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

    public Fields withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

}
