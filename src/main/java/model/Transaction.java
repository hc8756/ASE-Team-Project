package model;

import java.time.LocalDateTime;

public class Transaction {

    private Long id;                 // assigned by the service
    private String description;      // e.g., "Coffee"
    private double amount;           // positive (income) or negative (expense)
    private LocalDateTime timestamp; // auto-filled by the service

    public Transaction() {
        this.timestamp = LocalDateTime.now();
    }

    public Transaction(Long id, String description, double amount, LocalDateTime timestamp) {
        this.id = id;
        this.description = description != null ? description.trim() : "";
        this.amount = amount;
        this.timestamp = (timestamp != null) ? timestamp : LocalDateTime.now();
    }

    // ---------- Getters & Setters ----------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description != null ? description.trim() : "";
    }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = (timestamp != null) ? timestamp : LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format(
            "Transaction{id=%d, desc='%s', amount=%.2f, timestamp=%s}",
            id, description, amount, timestamp
        );
    }
}
