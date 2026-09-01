package com.agrosense.model;

public class Product {
    private int id;
    private String modelName;
    private String description;

    public Product() {}

    public Product(int id, String modelName, String description) {
        this.id = id;
        this.modelName = modelName;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() { return modelName; }
}
