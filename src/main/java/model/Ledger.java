package model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

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

  /** Default constructor for JSON deserialization. */
  public Ledger() {
    // no-op
  }

  /**
   * Full constructor.
   *
   * @param ledgerId unique id
   * @param date transaction date (yyyy-MM-dd)
   * @param amount positive amount spent
   * @param category category name
   * @param description optional description
   */
  public Ledger(
      final long ledgerId,
      final LocalDate date,
      final double amount,
      final String category,
      final String description) {
    this.ledgerId = ledgerId;
    this.date = date;
    this.amount = amount;
    this.category = category;
    this.description = description;
  }

  public long getLedgerId() {
    return ledgerId;
  }

  public void setLedgerId(final long ledgerId) {
    this.ledgerId = ledgerId;
  }

  public LocalDate getDate() {
    return date;
  }

  public void setDate(final LocalDate date) {
    this.date = date;
  }

  public double getAmount() {
    return amount;
  }

  public void setAmount(final double amount) {
    this.amount = amount;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(final String category) {
    this.category = category;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  @Override
  public int compareTo(final Ledger other) {
    return this.date.compareTo(other.date);
  }
}