package dev.coms4156.project.individualproject.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Transaction implements Comparable<Transaction> {
    private UUID transId;
    private UUID userId;
    private String description;
    private double amount;
    private String category;
    private LocalDateTime createdAt;
    
    // Constructors

    // Make sure that category is defined in transaction_category in schema.sql
    public Transaction(UUID userId, double amount, String category) {
        this.userId = userId;
        this.amount = amount;
        this.category = category;
    }
    
    // Getters and Setters
    public UUID getTransId() { return transId; }
    public void setTransId(UUID transId) { this.transId = transId; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}