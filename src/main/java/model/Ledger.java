package model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Unified Transaction/Ledger model.
 * Positive amount = expense; negative = income/refund.
 * Supports both date-only (for summaries) and timestamp (for precise ordering).
 */
public class Ledger implements Comparable<Ledger> {

    // ---------- Identity ----------
    /** Unique identifier. Accepts JSON "id" or "ledgerId". */
    @JsonAlias({"id", "ledgerId"})
    private Long id;

    // ---------- Core ----------
    private String description;
    private double amount;

    // ---------- Date/Time ----------
    /** Optional full timestamp (for precise ordering). */
    private LocalDateTime timestamp;

    /** Optional date-only (yyyy-MM-dd) for summaries & budgets. */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    // ---------- Category ----------
    /** Category label (e.g., Dining, Groceries, Utilities). */
    private String category;

    // ---------- Constructors ----------
    public Ledger() {
        // Leave fields null by default; service will populate as needed.
    }

    public Ledger(Long id, LocalDate date, double amount, String category, String description) {
        this.id = id;
        setDate(date);
        this.amount = amount;
        this.category = category;
        setDescription(description);
        // If timestamp is still null, derive from date
        if (this.timestamp == null && this.date != null) {
            this.timestamp = this.date.atStartOfDay();
        }
    }

    public Ledger(Long id, String description, double amount, LocalDateTime timestamp, String category) {
        this.id = id;
        setDescription(description);
        this.amount = amount;
        setTimestamp(timestamp);
        this.category = category;
    }

    // ---------- Getters / Setters ----------
    /** Alias for compatibility with code that calls getLedgerId(). */
    public Long getLedgerId() { return id; }
    public void setLedgerId(Long id) { this.id = id; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = (description == null) ? "" : description.trim();
    }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = (timestamp != null) ? timestamp : null;
        // Keep date in sync if possible
        if (this.timestamp != null) {
            this.date = this.timestamp.toLocalDate();
        }
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) {
        this.date = (date != null) ? date : null;
        // If we have a date but no timestamp, set timestamp to start of that day
        if (this.date != null && this.timestamp == null) {
            this.timestamp = this.date.atStartOfDay();
        }
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    // ---------- Helpers ----------
    /** Preferred ordering instant: timestamp if present, else date at start-of-day, else null. */
    public LocalDateTime effectiveInstant() {
        if (timestamp != null) return timestamp;
        if (date != null) return date.atStartOfDay();
        return null;
    }

    // ---------- Comparable ----------
    @Override
    public int compareTo(Ledger other) {
        if (other == null) return 1;
        LocalDateTime a = this.effectiveInstant();
        LocalDateTime b = other.effectiveInstant();
        if (a == null && b == null) {
            long ai = (this.id == null ? -1L : this.id);
            long bi = (other.id == null ? -1L : other.id);
            return Long.compare(ai, bi);
        }
        if (a == null) return -1;
        if (b == null) return 1;
        int cmp = a.compareTo(b);
        if (cmp != 0) return cmp;
        long ai = (this.id == null ? -1L : this.id);
        long bi = (other.id == null ? -1L : other.id);
        return Long.compare(ai, bi);
    }

    // ---------- Equality / Hash / String ----------
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ledger)) return false;
        Ledger ledger = (Ledger) o;
        return Objects.equals(id, ledger.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "Ledger{" +
                "id=" + id +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                ", date=" + date +
                ", category='" + category + '\'' +
                '}';
    }
}
