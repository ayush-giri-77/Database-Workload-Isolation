package com.workloadgovernance.model;

public class Tenant {
    private String id;
    private String name;
    private int weight;
    private int maxConcurrency;
    private boolean active;

    public Tenant(String id, String name, int weight, int maxConcurrency, boolean active){
        this.id = id;
        this.name = name;
        this.weight = weight;
        this.maxConcurrency = maxConcurrency;
        this.active = active;
    }

    public String getId(){
        return id;
    }

    public void setId(String id){
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
