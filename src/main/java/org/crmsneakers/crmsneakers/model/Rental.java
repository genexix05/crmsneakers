package org.crmsneakers.crmsneakers.model;

import org.bson.types.ObjectId;
import java.time.LocalDate;

public class Rental {
    private ObjectId id;
    private ObjectId customerId;
    private ObjectId productId;
    private LocalDate startDate;
    private LocalDate endDate;
    private double rentalPrice;
    private double depositAmount;
    private RentalStatus status;
    private String notes;
    
    public enum RentalStatus {
        ACTIVE,       // Currently active rental
        COMPLETED,    // Rental completed and product returned in good condition
        DAMAGED,      // Rental completed but product returned damaged
        CANCELLED,    // Rental was cancelled
        UNDER_REPAIR  // Product sent to repair during rental
    }
    
    public Rental() {}
    
    public Rental(ObjectId customerId, ObjectId productId, LocalDate startDate, 
                 LocalDate endDate, double rentalPrice, double depositAmount) {
        this.customerId = customerId;
        this.productId = productId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.rentalPrice = rentalPrice;
        this.depositAmount = depositAmount;
        this.status = RentalStatus.ACTIVE;
    }
    
    // Getters and Setters
    public ObjectId getId() {
        return id;
    }
    
    public void setId(ObjectId id) {
        this.id = id;
    }
    
    public ObjectId getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(ObjectId customerId) {
        this.customerId = customerId;
    }
    
    public ObjectId getProductId() {
        return productId;
    }
    
    public void setProductId(ObjectId productId) {
        this.productId = productId;
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public double getRentalPrice() {
        return rentalPrice;
    }
    
    public void setRentalPrice(double rentalPrice) {
        this.rentalPrice = rentalPrice;
    }
    
    public double getDepositAmount() {
        return depositAmount;
    }
    
    public void setDepositAmount(double depositAmount) {
        this.depositAmount = depositAmount;
    }
    
    public RentalStatus getStatus() {
        return status;
    }
    
    public void setStatus(RentalStatus status) {
        this.status = status;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    @Override
    public String toString() {
        return "Rental from " + startDate + " to " + endDate;
    }
}