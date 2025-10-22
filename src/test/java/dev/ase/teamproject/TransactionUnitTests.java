package dev.ase.teamproject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.ase.teamproject.model.Transaction;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class contains the unit tests for the Transaction class.
 */
public class TransactionUnitTests {

  private Transaction transaction;  
  private UUID userId;

  @BeforeEach
  public void setUp() {
    userId = UUID.randomUUID();
    transaction = new Transaction(userId, 10, "Category", "Description");
  }

  @Test
  public void setTransactionId_validId_transactionIdIsSet() {
    UUID expectedTransactionId = UUID.randomUUID();
    transaction.setTransactionId(expectedTransactionId);
    UUID transactionId = transaction.getTransactionId();
    assertEquals(expectedTransactionId, transactionId);
  }

  @Test
  public void getUserId_afterConstruction_returnsNonNullUserId() {
    assertNotNull(transaction.getUserId());
  }

  @Test
  public void setUserId_validId_userIdIsUpdated() {
    UUID expectedUserId = UUID.randomUUID();
    transaction.setUserId(expectedUserId);
    UUID userId = transaction.getUserId();
    assertEquals(expectedUserId, userId);
    
  }

  @Test
  public void getDescription_afterConstruction_returnsProvidedDescription() {
    assertEquals(transaction.getDescription(), "Description");
  }

  @Test
  public void setDescription_validString_descriptionIsUpdated() {
    transaction.setDescription("New Description");
    assertEquals(transaction.getDescription(), "New Description");
  }

  @Test
  public void setDescription_emptyString_descriptionIsEmpty() {
    transaction.setDescription("");
    assertEquals("", transaction.getDescription());
  }


  @Test
  public void getAmount_afterConstruction_returnsInitialValue() {
    assertEquals(transaction.getAmount(), 10.0);
  }

  @Test
  public void setAmount_validDouble_amountIsUpdated() {
    transaction.setAmount(5);
    assertEquals(transaction.getAmount(), 5.0);
  }

  @Test
  public void getTimestamp_afterSetting_returnsSameValue() {
    LocalDateTime currentTime = LocalDateTime.now();
    transaction.setTimestamp(currentTime);
    assertNotNull(transaction.getTimestamp());
    assertEquals(currentTime, transaction.getTimestamp());
  }

  @Test
  public void setTimestamp_validDateTime_timestampStoredCorrectly() {
    LocalDateTime timestamp = LocalDateTime.of(2025, 10, 20, 10, 15);
    transaction.setTimestamp(timestamp);
    assertEquals(timestamp, transaction.getTimestamp());
  }

  @Test
  public void getCategory_afterConstruction_returnsInitialCategory() {
    assertEquals(transaction.getCategory(), "Category");
  }

  @Test
  public void setCategory_validString_categoryIsUpdated() {
    transaction.setCategory("New Category");
    assertEquals(transaction.getCategory(), "New Category");
  }

  @Test
  public void effectiveInstant_timestampNotNull_returnsSameTimestamp() {
    LocalDateTime timestamp = LocalDateTime.of(2025, 10, 20, 10, 15);
    transaction.setTimestamp(timestamp);
    LocalDateTime test = transaction.effectiveInstant();
    assertEquals(timestamp, test);
  }

  @Test
  public void effectiveInstant_timestampNullDateNotNull_returnsDateStartOfDay() {
    LocalDate date = LocalDate.of(2025, 10, 20);
    transaction.setDate(date);
    transaction.setTimestamp(null);
    LocalDateTime test = transaction.effectiveInstant();
    assertEquals(date.atStartOfDay(), test);
  }

  @Test
  public void effectiveInstant_bothTimestampAndDateNull_returnsNull() {
    transaction.setTimestamp(null);
    transaction.setDate(null);
    assertNull(transaction.effectiveInstant());
  }
}