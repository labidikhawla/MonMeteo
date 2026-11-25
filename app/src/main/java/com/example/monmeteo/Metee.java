package com.example.monmeteo;

import java.util.ArrayList;
import java.util.List;

public class Metee {

    private String ville;
    private String region;
    private String temperature;
    private String condition;
    private String ressenti;
    private String vent;
    private String humidite;
    private String min;
    private String max;
    private String urlApi;
    private final List<Prevision> previsions = new ArrayList<>();

    public String getUrlApi() {
        return urlApi;
    }

    public void setUrlApi(String urlApi) {
        this.urlApi = urlApi;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getRessenti() {
        return ressenti;
    }

    public void setRessenti(String ressenti) {
        this.ressenti = ressenti;
    }

    public String getVent() {
        return vent;
    }

    public void setVent(String vent) {
        this.vent = vent;
    }

    public String getHumidite() {
        return humidite;
    }

    public void setHumidite(String humidite) {
        this.humidite = humidite;
    }

    public String getMin() {
        return min;
    }

    public void setMin(String min) {
        this.min = min;
    }

    public String getMax() {
        return max;
    }

    public void setMax(String max) {
        this.max = max;
    }

    public List<Prevision> getPrevisions() {
        return previsions;
    }
}
