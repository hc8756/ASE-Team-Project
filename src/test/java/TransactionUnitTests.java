import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class contains the unit tests for the Ledger class.
 */

public class TransactionUnitTests {

  private Transaction transaction;  
  private UUID userId;

  @BeforeEach
  public void setUpTransactionForTesting() {
    userId = UUID.randomUUID();
    transaction = new Transaction(userId, 10, "Category", "Description");
  }

  
  // ---------- Getters / Setters Test ----------

  @Test
  public void testSetTransactionId() {
    UUID expectedTransactionId = UUID.randomUUID();
    transaction.setTransactionId(expectedTransactionId);
    UUID transactionId = transaction.getTransactionId();
    assertEquals(expectedTransactionId, transactionId);
  }

  @Test
  public void testGetUserId() {
    assertNotNull(transaction.getUserId());
  }

  @Test
  public void testSetUserId() {
    UUID expectedUserId = UUID.randomUUID();
    transaction.setUserId(expectedUserId);
    UUID userId = transaction.getUserId();
    assertEquals(expectedUserId, userId);
    
  }

  @Test
  public void testGetDescription() {
    assertEquals(transaction.getDescription(), "Description");
  }

  @Test
  public void testSetDescription() {
    transaction.setDescription("New Description");
    assertEquals(transaction.getDescription(), "New Description");
  }

  @Test
  public void testSetEmptyDescription() {
    transaction.setDescription("");
    assertEquals("", transaction.getDescription());
  }


  @Test
  public void testGetAmount() {
    assertEquals(transaction.getAmount(), 10.0);
  }

  @Test
  public void testSetAmount() {
    transaction.setAmount(5);
    assertEquals(transaction.getAmount(), 5.0);
  }

  @Test
  public void testGetTimestamp() {
    LocalDateTime currentTime = LocalDateTime.now();
    transaction.setTimestamp(currentTime);
    assertNotNull(transaction.getTimestamp());
    assertEquals(currentTime, transaction.getTimestamp());
  }

  @Test
  public void testSetTimestamp() {
    LocalDateTime timestamp = LocalDateTime.of(2025, 10, 20, 10, 15);
    transaction.setTimestamp(timestamp);
    assertEquals(timestamp, transaction.getTimestamp());
  }

  @Test
  public void testGetCategory() {
    assertEquals(transaction.getCategory(), "Category");
  }

  @Test
  public void testSetCategory() {
    transaction.setCategory("New Category");
    assertEquals(transaction.getCategory(), "New Category");
  }

  // ---------- Helpers Test ----------

  @Test
  void testEffectiveInstantTimestampNotNull() {
    LocalDateTime timestamp = LocalDateTime.of(2025, 10, 20, 10, 15);
    transaction.setTimestamp(timestamp);
    LocalDateTime test = transaction.effectiveInstant();
    assertEquals(timestamp, test);
  }

  @Test
  void testEffectiveInstantTimeStampNullDateNotNull() { 
    LocalDate date = LocalDate.of(2025, 10, 20);
    transaction.setDate(date);
    transaction.setTimestamp(null);
    LocalDateTime test = transaction.effectiveInstant();
    assertEquals(date.atStartOfDay(), test);
  }

  @Test
  void testEffectiveInstantAllNull() {
    transaction.setTimestamp(null);
    transaction.setDate(null);
    assertNull(transaction.effectiveInstant());
  }

  // ---------- Comparables Test ----------


  // ---------- Equality / Hash Test ----------

}