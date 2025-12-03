package dev.ase.teamproject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ase.teamproject.model.Transaction;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test Plan / Equivalence Partitions for Transaction (POJO / Data Class)
 *
 * <p>Class Under Test: dev.ase.teamproject.model.Transaction
 *
 * <p>This plan covers the unit tests for the Transaction model. Since this is a
 * data class, testing focuses on constructors, getter/setter pairs, and
 * the branch logic within business methods (like compareTo, equals, etc.).
 *
 * <p>1. Transaction() (Default Constructor)
 * - P1: (Valid) Call constructor.
 * - Expects: All fields null or 0.0.
 * - Covered by: constructor_defaultConstructor_initializesNullOrZeroFields
 *
 * <p>2. Transaction(userId, amount, category, description) (Parameterized)
 * - P1: (Valid) Call with valid data.
 * - Expects: Fields are set, description is handled by setDescription.
 * - Covered by: constructor_parameterizedConstructor_setsProvidedValues
 *
 * <p>3. setDescription(String description)
 * - P1: (Valid) Normal string.
 * - Covered by: setDescription_validString_descriptionIsUpdated
 * - P2: (Valid, Boundary) Empty string "".
 * - Covered by: setDescription_emptyString_descriptionIsEmpty
 * - P3: (Valid, Branch) String with whitespace only (triggers trim()).
 * - Covered by: setDescription_withWhitespaceOnly_setsEmptyString
 * - P4: (Invalid, Branch) null input (triggers null check).
 * - Covered by: setDescription_withNull_setsEmptyString
 *
 * <p>4. setTimestamp(LocalDateTime timestamp)
 * - P1: (Valid, Branch) timestamp != null.
 * - Expects: this.timestamp and this.date are set.
 * - Covered by: setTimestamp_validDateTime_timestampStoredCorrectly
 * - P2: (Invalid, Branch) timestamp == null.
 * - Expects: this.timestamp is set to null, this.date is unchanged.
 * - Covered by: setTimestamp_withNull_setsTimestampToNull_leavesDateUnchanged
 *
 * <p>5. setDate(LocalDate date)
 * - P1: (Valid, Branch) date != null && this.timestamp == null.
 * - Expects: date is set, timestamp is set to date.atStartOfDay().
 * - Covered by: setDate_validDateTimestampNull_returnsDateStartOfDay
 * - P2: (Valid, Branch) date != null && this.timestamp != null.
 * - Expects: date is set, timestamp is unchanged.
 * - Covered by: setDate_withExistingTimestamp_keepsOriginalTimestamp
 * - P3: (Invalid, Branch) date == null.
 * - Expects: date is set to null (timestamp remains).
 * - Covered by: setDate_nullDate_dateIsNull
 *
 * <p>6. effectiveInstant()
 * - P1: (Branch) timestamp != null.
 * - Covered by: effectiveInstant_withTimestamp_returnsSameTimestamp
 * - P2: (Branch) timestamp == null && date != null.
 * - Covered by: effectiveInstant_withDateOnly_returnsDateStartOfDay
 * - P3: (Branch) timestamp == null && date == null.
 * - Covered by: effectiveInstant_bothTimestampAndDateNull_returnsNull
 *
 * <p>7. compareTo(Transaction other)
 * - P1: (Invalid) other == null.
 * - Covered by: compareTo_nullOther_returnsPositive
 * - P2: (Branch) Different non-null instants (cmp != 0).
 * - Covered by: compareTo_differentTimestamps_ordersByTime
 * - P3: (Branch) Same non-null instants (cmp == 0) -> fallback to ID compare.
 * - Covered by: compareTo_sameTimestamp_ordersById
 * - P4: (Branch) this.instant == null, other.instant != null.
 * - Covered by: compareTo_oneEffectiveInstantNull_returnsCorrectOrder
 * - P5: (Branch) this.instant != null, other.instant == null.
 * - Covered by: compareTo_oneEffectiveInstantNull_returnsCorrectOrder (reverse)
 * - P6: (Branch) Both instants null -> fallback to ID compare.
 * - Covered by: compareTo_bothEffectiveInstantNull_compareById
 * - P7: (Sub-Branch) Both instants null, both IDs null.
 * - Covered by: compareTo_bothNullIds_returnsZero
 * - P8: (Sub-Branch) Both instants null, one ID null.
 * - Covered by: compareTo_noTime_oneNullId_ordersNullFirst
 * - P9: (Sub-Branch) Same instant, one ID null.
 * - Covered by: compareTo_sameTime_oneNullId_ordersNullFirst
 *
 * <p>8. equals(Object other)
 * - P1: (Branch) this == other.
 * - Covered by: equals_sameObject_returnsTrue
 * - P2: (Branch) other == null.
 * - Covered by: equals_null_returnsFalse
 * - P3: (Branch) !(other instanceof Transaction).
 * - Covered by: equals_differentClass_returnsFalse
 * - P4: (Branch) Same class, Objects.equals(id1, id2) (different, non-null).
 * - Covered by: equals_differentIds_returnsFalse
 * - P5: (Branch) Same class, Objects.equals(id1, id2) (same, non-null).
 * - Covered by: equals_sameId_returnsTrue
 * - P6: (Branch) Same class, Objects.equals(null, null).
 * - Covered by: equals_bothNullId_returnsTrue
 * - P7: (Branch) Same class, Objects.equals(id, null).
 * - Covered by: equals_oneNullId_returnsFalse
 * - P8: (Branch) Same class, Objects.equals(null, id).
 * - Covered by: equals_oneNullId_returnsFalse_reverse
 *
 * <p>9. hashCode()
 * - P1: (Branch) transactionId != null.
 * - Covered by: hashCode_withId_returnsIdHash
 * - P2: (Branch) transactionId == null.
 * - Covered by: hashCode_withNullId_returnsZeroHash
 *
 * <p>10. toString()
 * - P1: (Valid) Call on a populated object.
 * - Covered by: toString_containsAllFields
 */
public class TransactionUnitTests {

  private Transaction transaction;  
  private UUID userId;

  /**
   * Sets up a fresh Transaction object before each test.
   */
  @BeforeEach
  public void setUp() {
    userId = UUID.randomUUID();
    transaction = new Transaction(userId, 10, "Category", "Description");
  }

  // ---------------------------------------------------------------------------
  // Constructors
  // ---------------------------------------------------------------------------

  /**
   * Tests the default constructor to ensure all fields are initialized
   * to their default (null or 0.0) values.
   */
  @Test
  public void constructor_defaultConstructor_initializesNullOrZeroFields() {
    Transaction t = new Transaction();
    assertNull(t.getTransactionId());
    assertNull(t.getUserId());
    assertNull(t.getDescription()); // <-- FIX: Was assertEquals("", t.getDescription())
    assertEquals(0.0, t.getAmount());
    assertNull(t.getCategory());
    assertNull(t.getTimestamp());
    assertNull(t.getDate());
  }

  /**
   * Tests the parameterized constructor to ensure all provided
   * values are set correctly.
   */
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

  /**
   * Tests the setter and getter for transactionId.
   */
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

  /**
   * Tests that the parameterized constructor correctly sets the userId.
   */
  @Test
  public void getUserId_afterConstruction_returnsNonNullUserId() {
    assertNotNull(transaction.getUserId());
  }

  /**
   * Tests the setter and getter for userId.
   */
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

  /**
   * Tests that the parameterized constructor correctly sets the description.
   */
  @Test
  public void getDescription_afterConstruction_returnsProvidedDescription() {
    assertEquals(transaction.getDescription(), "Description");
  }

  /**
   * (Typical valid input) Tests setting a standard string.
   */  
  @Test
  public void setDescription_validString_descriptionIsUpdated() {
    transaction.setDescription("New Description");
    assertEquals(transaction.getDescription(), "New Description");
  }

  /**
   * (Typical valid input) Tests setting an empty string.
   */
  @Test
  public void setDescription_emptyString_descriptionIsEmpty() {
    transaction.setDescription("");
    assertEquals("", transaction.getDescription());
  }

  /**
   * (Atypical valid input) Tests the trim() branch of the setter.
   */
  @Test
  public void setDescription_withWhitespaceOnly_setsEmptyString() {
    transaction.setDescription("   ");
    assertEquals("", transaction.getDescription());
  }

  /**
   * (Invalid input) Tests the null-check branch of the setter.
   */
  @Test
  public void setDescription_withNull_setsEmptyString() {
    transaction.setDescription(null);
    assertEquals("", transaction.getDescription());
  }

  // ---------------------------------------------------------------------------
  // getAmount, setAmount
  // ---------------------------------------------------------------------------

  /**
   * Tests that the parameterized constructor correctly sets the amount.
   */
  @Test
  public void getAmount_afterConstruction_returnsInitialValue() {
    assertEquals(transaction.getAmount(), 10.0);
  }

  /**
   * Tests the setter and getter for amount.
   */
  @Test
  public void setAmount_validDouble_amountIsUpdated() {
    transaction.setAmount(5);
    assertEquals(transaction.getAmount(), 5.0);
  }

  // ---------------------------------------------------------------------------
  // getTimestamp, setTimestamp
  // ---------------------------------------------------------------------------

  /**
   * Tests the setter and getter for timestamp.
   */
  @Test
  public void getTimestamp_afterSetting_returnsSameValue() {
    LocalDateTime currentTime = LocalDateTime.now();
    transaction.setTimestamp(currentTime);
    assertNotNull(transaction.getTimestamp());
    assertEquals(currentTime, transaction.getTimestamp());
  }

  /**
   * (Typical valid input) Tests that setting a timestamp also updates the date.
   * Covers the `if (timestamp != null)` branch.
   */
  @Test
  public void setTimestamp_validDateTime_timestampStoredCorrectly() {
    LocalDateTime timestamp = LocalDateTime.of(2025, 10, 20, 10, 15);
    transaction.setTimestamp(timestamp);
    assertEquals(timestamp, transaction.getTimestamp());
    assertEquals(timestamp.toLocalDate(), transaction.getDate());
  }

  /**
   * (Atypical valid input) Tests that setting the timestamp multiple times
   * correctly updates the date to the latest timestamp.
   */
  @Test
  public void setTimestamp_settingTwice_updatesDateToLatest() {
    LocalDateTime first = LocalDateTime.of(2024, 5, 10, 10, 0);
    LocalDateTime second = LocalDateTime.of(2024, 5, 20, 14, 0);
    transaction.setTimestamp(first);
    transaction.setTimestamp(second);
    assertEquals(second, transaction.getTimestamp());
    assertEquals(second.toLocalDate(), transaction.getDate());
  }

  /** * (Invalid input) 
   * Tests that setting a null timestamp sets timestamp to null
   * but leaves the date field unchanged from its previous value.
   */
  @Test
  public void setTimestamp_withNull_setsTimestampToNull_leavesDateUnchanged() {
    LocalDateTime currentTime = LocalDateTime.now();
    LocalDate expectedDate = currentTime.toLocalDate();

    // Set an initial state
    transaction.setTimestamp(currentTime);
    assertEquals(expectedDate, transaction.getDate());
    
    // Now, call with null
    transaction.setTimestamp(null);
    
    // Assert timestamp is null but the date remains
    assertNull(transaction.getTimestamp());
    assertEquals(expectedDate, transaction.getDate());
  }

  // ---------------------------------------------------------------------------
  // getDate, setDate
  // ---------------------------------------------------------------------------

  /**
   * Tests the setter and getter for date.
   */
  @Test
  public void getDate_afterSetting_returnsSameValue() {
    LocalDate date = LocalDate.of(2025, 10, 20);
    transaction.setDate(date);
    assertEquals(date, transaction.getDate());
  }

  /**
   * (Typical valid input) Tests setting the date when a timestamp already exists.
   * Covers the `date != null && this.timestamp != null` branch.
   */
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

  /**
   * (Typical valid input) Tests setting the date when timestamp is null.
   * Covers the `if (date != null && this.timestamp == null)` branch.
   */
  @Test
  public void setDate_validDateTimestampNull_returnsDateStartOfDay() {
    LocalDate date = LocalDate.of(2025, 10, 20);
    transaction.setTimestamp(null); // Ensure timestamp is null
    transaction.setDate(date);
    assertEquals(date, transaction.getDate());
    // Verifies that the timestamp was auto-set
    assertEquals(date.atStartOfDay(), transaction.getTimestamp());
  }

  /**
   * (Invalid input) Tests setting a null date.
   * Covers the `date == null` branch.
   */
  @Test
  public void setDate_nullDate_dateIsNull() {
    // Set initial values
    transaction.setTimestamp(LocalDateTime.now());
    assertNotNull(transaction.getDate());
    assertNotNull(transaction.getTimestamp());

    transaction.setDate(null);
    assertNull(transaction.getDate());
    // Note: setDate(null) does *not* clear the timestamp
    assertNotNull(transaction.getTimestamp());
  }

  // ---------------------------------------------------------------------------
  // getCategory, setCategory
  // ---------------------------------------------------------------------------

  /**
   * Tests that the parameterized constructor correctly sets the category.
   */
  @Test
  public void getCategory_afterConstruction_returnsInitialCategory() {
    assertEquals(transaction.getCategory(), "Category");
  }

  /**
   * Tests the setter and getter for category.
   */
  @Test
  public void setCategory_validString_categoryIsUpdated() {
    transaction.setCategory("New Category");
    assertEquals(transaction.getCategory(), "New Category");
  }

  // ---------------------------------------------------------------------------
  // effectiveInstant
  // ---------------------------------------------------------------------------

  /**
   * (Typical valid input) Tests the first branch of `effectiveInstant`.
   * Covers `timestamp != null`.
   */
  @Test
  public void effectiveInstant_withTimestamp_returnsSameTimestamp() {
    LocalDateTime timestamp = LocalDateTime.of(2025, 10, 20, 10, 15);
    transaction.setTimestamp(timestamp);
    LocalDateTime test = transaction.effectiveInstant();
    assertEquals(timestamp, test);
  }


  /**
   * (Atypical valid input) Tests the second branch of `effectiveInstant`.
   * Covers `timestamp == null && date != null`.
   */
  @Test
  public void effectiveInstant_withDateOnly_returnsDateStartOfDay() {
    LocalDate date = LocalDate.of(2025, 10, 20);
    
    // Rerun setup for this test case
    transaction = new Transaction(userId, 10, "Cat", "Desc");
    transaction.setDate(date); // This will set timestamp to atStartOfDay()
    transaction.setTimestamp(null); // Manually clear timestamp
    transaction.setDate(date); // Set *only* date

    LocalDateTime test = transaction.effectiveInstant();
    assertEquals(date.atStartOfDay(), test);
  }


  /**
   * (Invalid input) Tests the final branch of `effectiveInstant`.
   * Covers `timestamp == null && date == null`.
   */
  @Test
  public void effectiveInstant_bothTimestampAndDateNull_returnsNull() {
    transaction.setTimestamp(null);
    // Note: setTimestamp(null) also nulls date
    assertNull(transaction.effectiveInstant());
  }

  // ---------------------------------------------------------------------------
  // compareTo
  // ---------------------------------------------------------------------------

  /**
   * Tests compareTo branch: `if (other == null)`.
   */
  @Test
  void compareTo_nullOther_returnsPositive() {
    Transaction t1 = new Transaction();
    t1.setTimestamp(LocalDateTime.now());
    t1.setTransactionId(UUID.randomUUID());
    assertTrue(t1.compareTo(null) > 0);
  }

  /**
   * Tests compareTo branch: `if (thisDateTime == null && otherDateTime == null)`.
   * Fallback to ID comparison.
   */
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

  /**
   * Tests compareTo branches: `if (thisDateTime == null)` and `if (otherDateTime == null)`.
   */
  @Test
  void compareTo_oneEffectiveInstantNull_returnsCorrectOrder() {
    Transaction t1 = new Transaction(); // null instant
    Transaction t2 = new Transaction(); // non-null instant
    t1.setTransactionId(UUID.randomUUID());
    t2.setTimestamp(LocalDateTime.now());
    t2.setTransactionId(UUID.randomUUID());

    // t1 (null) should come before t2 (non-null)
    assertTrue(t1.compareTo(t2) < 0);
    // t2 (non-null) should come after t1 (null)
    assertTrue(t2.compareTo(t1) > 0);
  }

  /**
   * Tests compareTo branch: `if (cmp != 0)`.
   */
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

  /**
   * Tests compareTo branch: `if (cmp == 0)` fallback to ID compare.
   */
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

  /**
   * Tests compareTo branch: `thisDateTime == null && otherDateTime == null`
   * and `thisUuid == null && otherUuid == null`.
   */
  @Test
  void compareTo_bothNullIds_returnsZero() {
    Transaction t1 = new Transaction();
    Transaction t2 = new Transaction();
    assertEquals(0, t1.compareTo(t2));
  }

  /**
   * Tests compareTo branch: `thisDateTime == null && ... && thisUuid == null`.
   */
  @Test
  void compareTo_noTime_oneNullId_ordersNullFirst() {
    Transaction t1 = new Transaction(); // No time, no ID
    Transaction t2 = new Transaction(); // No time, has ID
    t2.setTransactionId(UUID.randomUUID());
    
    // t1 (null id) should come before t2 (non-null id)
    assertTrue(t1.compareTo(t2) < 0);
    assertTrue(t2.compareTo(t1) > 0);
  }

  /**
   * Tests compareTo branch: `cmp == 0 && thisUuid == null`.
   */
  @Test
  void compareTo_sameTime_oneNullId_ordersNullFirst() {
    LocalDateTime now = LocalDateTime.now();
    Transaction t1 = new Transaction(); // Has time, no ID
    t1.setTimestamp(now);
    Transaction t2 = new Transaction(); // Has time, has ID
    t2.setTimestamp(now);
    t2.setTransactionId(UUID.randomUUID());

    // t1 (null id) should come before t2 (non-null id)
    assertTrue(t1.compareTo(t2) < 0);
    assertTrue(t2.compareTo(t1) > 0);
  }

  /**
   * Tests compareTo branch: `cmp == 0 && thisUuid == null && otherUuid == null`.
   */
  @Test
  void compareTo_sameTime_bothNullId_returnsZero() {
    LocalDateTime now = LocalDateTime.now();
    Transaction t1 = new Transaction(); // Has time, no ID
    t1.setTimestamp(now);
    Transaction t2 = new Transaction(); // Has time, no ID
    t2.setTimestamp(now);

    assertEquals(0, t1.compareTo(t2));
  }

  // ---------------------------------------------------------------------------
  // equals
  // ---------------------------------------------------------------------------

  /**
   * Tests equals branch: `if (this == other)`.
   */
  @Test
  void equals_sameObject_returnsTrue() {
    Transaction t1 = new Transaction();
    t1.setTransactionId(UUID.randomUUID());
    assertEquals(t1, t1);
  }

  /**
   * Tests equals branch: `if (!(other instanceof Transaction))` with null.
   */
  @Test
  void equals_null_returnsFalse() {
    Transaction t1 = new Transaction();
    assertNotEquals(t1, null);
  }

  /**
   * Tests equals branch: `if (!(other instanceof Transaction))` with different class.
   */
  @Test
  void equals_differentClass_returnsFalse() {
    Transaction t1 = new Transaction();
    assertNotEquals(t1, 1);
  }

  /**
   * Tests equals branch: `Objects.equals(transactionId, transaction.transactionId)`
   * where both are non-null and equal.
   */
  @Test
  void equals_sameId_returnsTrue() {
    UUID id = UUID.randomUUID();
    Transaction t1 = new Transaction();
    Transaction t2 = new Transaction();
    t1.setTransactionId(id);
    t2.setTransactionId(id);
    assertEquals(t1, t2);
  }

  /**
   * Tests equals branch: `Objects.equals(transactionId, transaction.transactionId)`
   * where both are non-null and different.
   */
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

  /**
   * Tests equals branch: `Objects.equals(null, null)` which is true.
   */
  @Test
  void equals_bothNullId_returnsTrue() {
    Transaction t1 = new Transaction();
    Transaction t2 = new Transaction();
    assertEquals(t1, t2);
  }

  /**
   * Tests equals branch: `Objects.equals(id, null)` which is false.
   */
  @Test
  void equals_oneNullId_returnsFalse() {
    Transaction t1 = new Transaction();
    t1.setTransactionId(UUID.randomUUID());
    Transaction t2 = new Transaction();
    assertNotEquals(t1, t2);
  }

  /**
   * Tests equals branch: `Objects.equals(null, id)` which is false.
   */
  @Test
  void equals_oneNullId_returnsFalse_reverse() {
    Transaction t1 = new Transaction();
    Transaction t2 = new Transaction();
    t2.setTransactionId(UUID.randomUUID());
    assertNotEquals(t1, t2);
  }

  // ---------------------------------------------------------------------------
  // hashCode
  // ---------------------------------------------------------------------------

  /**
   * Tests hashCode() with a non-null transactionId.
   */
  @Test
  void hashCode_withId_returnsIdHash() {
    UUID id = UUID.randomUUID();
    transaction.setTransactionId(id);
    assertEquals(Objects.hash(id), transaction.hashCode());
  }

  /**
   * Tests hashCode() with a null transactionId.
   */
  @Test
  void hashCode_withNullId_returnsZeroHash() {
    Transaction t1 = new Transaction();
    // The hash of a single null object is 0
    assertEquals(Objects.hash((UUID) null), t1.hashCode());
  }
  
  // ---------------------------------------------------------------------------
  // toString
  // ---------------------------------------------------------------------------

  /**
   * Tests that the toString() method includes key fields.
   */
  @Test
  void toString_containsAllFields() {
    UUID id = UUID.randomUUID();
    transaction.setTransactionId(id);
    transaction.setAmount(123.45);
    transaction.setDescription("Test");

    String str = transaction.toString();

    assertTrue(str.contains("id=" + id));
    assertTrue(str.contains("amount=123.45"));
    assertTrue(str.contains("description='Test'"));
    assertTrue(str.contains("category='Category'"));
  }
}