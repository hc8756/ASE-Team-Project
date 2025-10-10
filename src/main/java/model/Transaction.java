package model;

import java.time.LocalDateTime;

public class Transaction {
    private Long id;                // assigned by the service
    private String description;     // e.g., "Coffee"
    private double amount;          // positive or negative
    private LocalDateTime timestamp; // auto-filled by the service

    public Transaction() {}

    public Transaction(Long id, String description, double amount, LocalDateTime timestamp) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    // Getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
