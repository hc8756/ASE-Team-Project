package model;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * This class defines a single ledger transaction.
 */
public class Ledger implements Comparable<Ledger> {
    /** Unique identifier for the transaction. */
    @JsonAlias({"id", "ledgerId"})
    private long ledgerId;
    /** ISO date (yyyy-MM-dd) of the transaction. */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    /** Positive amount means money spent. */
    private double amount;
    /** Category name, e.g., Dining, Groceries, etc. */
    private String category;
    /** Optional description/merchant. */
    private String description;

    public Ledger() {
        // Default constructor
    }

    public Ledger(long ledgerId, LocalDate date, double amount, String category, String description) {
        this.ledgerId = ledgerId;
        this.date = date;
        this.amount = amount;
        this.category = category;
        this.description = description;
    }

    public long getLedgerId() { return ledgerId; }
    public void setLedgerId(long ledgerId) { this.ledgerId = ledgerId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public int compareTo(Ledger other) {
        return this.date.compareTo(other.date);
    }
}