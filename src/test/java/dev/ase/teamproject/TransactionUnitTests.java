package dev.ase.teamproject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  // ---------------------------------------------------------------------------
  // Constructors
  // ---------------------------------------------------------------------------

  @Test
  public void constructor_defaultConstructor_initializesNullOrZeroFields() {
    Transaction t = new Transaction();
    assertNull(t.getTransactionId());
    assertNull(t.getUserId());
    assertNull(t.getDescription());
    assertEquals(0.0, t.getAmount());
    assertNull(t.getCategory());
    assertNull(t.getTimestamp());
    assertNull(t.getDate());
  }

  @Test
  public void constructor_parameterizedConstructor_setsProvidedValues() {
    UUID uid = UUID.randomUUID();
    Transaction t = new Transaction(uid, 20.0, "Test Category", "Test Description");
    assertEquals(uid, t.getUserId());
    assertEquals(20.0, t.getAmount());
    assertEquals("Test Category", t.getCategory());
    assertEquals("Test Description", t.getDescription());
  }


  // ---------------------------------------------------------------------------
  // getTransactionId, setTransactionId
  // ---------------------------------------------------------------------------

  /** Incidentally tests getter, getter is not set upon construction. */
  @Test
  public void setTransactionId_validId_transactionIdIsSet() {
    UUID expectedTransactionId = UUID.randomUUID();
    transaction.setTransactionId(expectedTransactionId);
    UUID transactionId = transaction.getTransactionId();
    assertEquals(expectedTransactionId, transactionId);
  }

  // ---------------------------------------------------------------------------
  // getUserId, setUserId
  // ---------------------------------------------------------------------------

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

  // ---------------------------------------------------------------------------
  // getDescription, setDescription
  // ---------------------------------------------------------------------------

  @Test
  public void getDescription_afterConstruction_returnsProvidedDescription() {
    assertEquals(transaction.getDescription(), "Description");
  }

  /** Typical valid input. */  
  @Test
  public void setDescription_validString_descriptionIsUpdated() {
    transaction.setDescription("New Description");
    assertEquals(transaction.getDescription(), "New Description");
  }

  /** Typical valid input. */
  @Test
  public void setDescription_emptyString_descriptionIsEmpty() {
    transaction.setDescription("");
    assertEquals("", transaction.getDescription());
  }

  /** Atypical valid input. */
  @Test
  public void setDescription_withWhitespaceOnly_setsEmptyString() {
    transaction.setDescription("   ");
    assertEquals("", transaction.getDescription());
  }

  /** Invalid input. */
  @Test
  public void setDescription_withNull_setsEmptyString() {
    transaction.setDescription(null);
    assertEquals("", transaction.getDescription());
  }

  // ---------------------------------------------------------------------------
  // getAmount, setAmount
  // ---------------------------------------------------------------------------

  @Test
  public void getAmount_afterConstruction_returnsInitialValue() {
    assertEquals(transaction.getAmount(), 10.0);
  }

  @Test
  public void setAmount_validDouble_amountIsUpdated() {
    transaction.setAmount(5);
    assertEquals(transaction.getAmount(), 5.0);
  }

  // ---------------------------------------------------------------------------
  // getTimestamp, setTimestamp
  // ---------------------------------------------------------------------------

  @Test
  public void getTimestamp_afterSetting_returnsSameValue() {
    LocalDateTime currentTime = LocalDateTime.now();
    transaction.setTimestamp(currentTime);
    assertNotNull(transaction.getTimestamp());
    assertEquals(currentTime, transaction.getTimestamp());
  }

  /** Typical valid input. */
  @Test
  public void setTimestamp_validDateTime_timestampStoredCorrectly() {
    LocalDateTime timestamp = LocalDateTime.of(2025, 10, 20, 10, 15);
    transaction.setTimestamp(timestamp);
    assertEquals(timestamp, transaction.getTimestamp());
  }

  /** Atypical valid input. */
  @Test
  public void setTimestamp_settingTwice_updatesDateToLatest() {
    LocalDateTime first = LocalDateTime.of(2024, 5, 10, 10, 0);
    LocalDateTime second = LocalDateTime.of(2024, 5, 20, 14, 0);
    transaction.setTimestamp(first);
    transaction.setTimestamp(second);
    assertEquals(second, transaction.getTimestamp());
    assertEquals(second.toLocalDate(), transaction.getDate());
  }

  /** Invalid input. */
  @Test
  public void setTimestamp_withNull_setsTimestampAndDateToNull() {
    transaction.setTimestamp(null);
    assertNull(transaction.getTimestamp());
    assertNull(transaction.getDate());
  }

  // ---------------------------------------------------------------------------
  // getDate, setDate
  // ---------------------------------------------------------------------------

  @Test
  public void getDate_afterSetting_returnsSameValue() {
    LocalDate date = LocalDate.of(2025, 10, 20);
    transaction.setDate(date);
    assertEquals(date, transaction.getDate());
  }

  /** Typical valid input. */
  @Test
  public void setDate_validDateTime_dateStoredCorrectly() {
    LocalDate date = LocalDate.of(2025, 10, 20);
    LocalDateTime timestamp = LocalDateTime.of(2025, 10, 20, 10, 15);
    transaction.setTimestamp(timestamp);
    transaction.setDate(date);
    assertEquals(date, transaction.getDate());
    assertEquals(timestamp, transaction.getTimestamp());
  }

  /** Typical valid input. */
  public void setDate_validDateTimestampNull_returnsDateStartOfDay() {
    LocalDate date = LocalDate.of(2025, 10, 20);
    transaction.setDate(date);
    assertEquals(date, transaction.getDate());
    assertEquals(date.atStartOfDay(), transaction.getTimestamp());
  }

  /** Atypical valid input. */
  @Test
  public void setDate_withExistingTimestamp_keepsOriginalTimestamp() {
    LocalDateTime ts = LocalDateTime.of(2024, 6, 1, 18, 30);
    transaction.setTimestamp(ts);
    LocalDate date = LocalDate.of(2024, 6, 2);
    transaction.setDate(date);

    // Timestamp should remain unchanged
    assertEquals(ts, transaction.getTimestamp());
    // Date should still be updated
    assertEquals(date, transaction.getDate());
  }

  /** Invalid input. */
  @Test
  public void setDate_nullDate_dateIsNull() {
    transaction.setDate(null);
    assertNull(transaction.getDate());
    assertNull(transaction.getTimestamp());
  }

  // ---------------------------------------------------------------------------
  // getCategory, setCategory
  // ---------------------------------------------------------------------------

  @Test
  public void getCategory_afterConstruction_returnsInitialCategory() {
    assertEquals(transaction.getCategory(), "Category");
  }

  @Test
  public void setCategory_validString_categoryIsUpdated() {
    transaction.setCategory("New Category");
    assertEquals(transaction.getCategory(), "New Category");
  }

  // ---------------------------------------------------------------------------
  // effectiveInstant
  // ---------------------------------------------------------------------------

  /** Typical valid input. */
  @Test
  public void effectiveInstant_withTimestamp_returnsSameTimestamp() {
    LocalDateTime timestamp = LocalDateTime.of(2025, 10, 20, 10, 15);
    transaction.setTimestamp(timestamp);
    LocalDateTime test = transaction.effectiveInstant();
    assertEquals(timestamp, test);
  }


  /** Atypical valid input. */
  @Test
  public void effectiveInstant_withDateOnly_returnsDateStartOfDay() {
    LocalDate date = LocalDate.of(2025, 10, 20);
    transaction.setDate(date);
    transaction.setTimestamp(null);
    LocalDateTime test = transaction.effectiveInstant();
    assertEquals(date.atStartOfDay(), test);
  }


  /** Invalid input. */
  @Test
  public void effectiveInstant_bothTimestampAndDateNull_returnsNull() {
    transaction.setTimestamp(null);
    transaction.setDate(null);
    assertNull(transaction.effectiveInstant());
  }

  // ---------------------------------------------------------------------------
  // compareTo
  // ---------------------------------------------------------------------------

  @Test
  void compareTo_nullOther_returnsPositive() {
    Transaction t1 = new Transaction();
    t1.setTimestamp(LocalDateTime.now());
    t1.setTransactionId(UUID.randomUUID());
    assertTrue(t1.compareTo(null) > 0);
  }

  @Test
  void compareTo_bothEffectiveInstantNull_compareById() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    Transaction t1 = new Transaction();
    Transaction t2 = new Transaction();
    t1.setTransactionId(id1);
    t2.setTransactionId(id2);
    assertEquals(id1.compareTo(id2), t1.compareTo(t2));
  }

  @Test
  void compareTo_oneEffectiveInstantNull_returnsCorrectOrder() {
    Transaction t1 = new Transaction();
    Transaction t2 = new Transaction();
    t1.setTransactionId(UUID.randomUUID());
    t2.setTimestamp(LocalDateTime.now());
    t2.setTransactionId(UUID.randomUUID());
    assertTrue(t1.compareTo(t2) < 0);
    assertTrue(t2.compareTo(t1) > 0);
  }

  @Test
  void compareTo_differentTimestamps_ordersByTime() {
    Transaction t1 = new Transaction();
    Transaction t2 = new Transaction();  
    t1.setTimestamp(LocalDateTime.of(2025, 1, 1, 0, 0, 0));  
    t1.setTransactionId(UUID.randomUUID());  
    t2.setTimestamp(LocalDateTime.of(2025, 2, 2, 0, 0, 0));  
    t2.setTransactionId(UUID.randomUUID());
    assertTrue(t1.compareTo(t2) < 0);
    assertTrue(t2.compareTo(t1) > 0);
  }

  @Test
  void compareTo_sameTimestamp_ordersById() {
    LocalDateTime now = LocalDateTime.now();
    UUID id1 = UUID.randomUUID();
    Transaction t1 = new Transaction();
    t1.setTimestamp(now);
    t1.setTransactionId(id1);
    UUID id2 = UUID.randomUUID();
    Transaction t2 = new Transaction();
    t2.setTimestamp(now);
    t2.setTransactionId(id2);
    assertEquals(id1.compareTo(id2), t1.compareTo(t2));
  }

  @Test
  void compareTo_bothNullIds_returnsZero() {
    Transaction t1 = new Transaction();
    Transaction t2 = new Transaction();
    assertEquals(0, t1.compareTo(t2));
  }


  // ---------------------------------------------------------------------------
  // equals
  // ---------------------------------------------------------------------------

  @Test
  void equals_sameObject_returnsTrue() {
    Transaction t1 = new Transaction();
    t1.setTransactionId(UUID.randomUUID());
    assertEquals(t1, t1);
  }

  @Test
  void equals_null_returnsFalse() {
    Transaction t1 = new Transaction();
    assertNotEquals(t1, null);
  }

  @Test
  void equals_differentClass_returnsFalse() {
    Transaction t1 = new Transaction();
    assertNotEquals(t1, 1);
  }

  @Test
  void equals_sameId_returnsTrue() {
    UUID id = UUID.randomUUID();
    Transaction t1 = new Transaction();
    Transaction t2 = new Transaction();
    t1.setTransactionId(id);
    t2.setTransactionId(id);
    assertEquals(t1, t2);
  }

  @Test
  void equals_differentIds_returnsFalse() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    Transaction t1 = new Transaction();
    Transaction t2 = new Transaction();
    t1.setTransactionId(id1);
    t2.setTransactionId(id2);
    assertNotEquals(t1, t2);
  }
}