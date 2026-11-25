package com.example.monmeteo;

public class Prevision {

    private String jour;
    private String symbole;
    private String condition;
    private String min;
    private String max;

    public Prevision(String jour, String symbole, String condition, String min, String max) {
        this.jour = jour;
        this.symbole = symbole;
        this.condition = condition;
        this.min = min;
        this.max = max;
    }

    public String getJour() {
        return jour;
    }

    public String getSymbole() {
        return symbole;
    }

    public String getCondition() {
        return condition;
    }

    public String getMin() {
        return min;
    }

    public String getMax() {
        return max;
    }
}
