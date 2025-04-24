package org.crmsneakers.crmsneakers.model;

import org.bson.types.ObjectId;

public class Product {
    private ObjectId id;
    private String name;
    private String brand;
    private String model;
    private String size;
    private double rentalPrice;
    private ProductStatus status;
    private String imageUrl;
    private String description;

    public enum ProductStatus {
        ACTIVE,       // Available for rental
        RENTED,       // Currently rented
        UNDER_REPAIR, // Being repaired
        INACTIVE,     // Not available for rental (but kept in database)
        DISCARDED     // Permanently removed from inventory due to damage
, DAMAGED, DISABLED
    }

    public Product() {}

    public Product(String name, String brand, String model, String size, 
                  double rentalPrice, ProductStatus status, String imageUrl, String description) {
        this.name = name;
        this.brand = brand;
        this.model = model;
        this.size = size;
        this.rentalPrice = rentalPrice;
        this.status = status;
        this.imageUrl = imageUrl;
        this.description = description;
    }

    // Getters and Setters
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public double getRentalPrice() {
        return rentalPrice;
    }

    public void setRentalPrice(double rentalPrice) {
        this.rentalPrice = rentalPrice;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return brand + " " + model + " (" + size + ")";
    }
}