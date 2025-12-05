package dev.ase.teamproject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Unit tests for the {@link Transaction} model class.
 *
 * <p>This test suite validates all public methods of the Transaction POJO,
 * covering valid and invalid equivalence partitions including boundary values
 * where applicable. Tests are organized by method and share common setup
 * through {@code @BeforeEach}.
 *
 * <h2>Equivalence Partitions by Method</h2>
 *
 * <h3>1. Transaction() - Default Constructor</h3>
 * <ul>
 *   <li>P1: (Valid) Instantiation - All fields should be null or 0.0</li>
 *   <li>Covered by: {@link #constructor_defaultConstructor_initializesNullOrZeroFields}</li>
 * </ul>
 *
 * <h3>2. Transaction(userId, amount, category, description) - Parameterized Constructor</h3>
 * <ul>
 *   <li>P1: (Valid) All parameters provided with typical values</li>
 *   <li>Covered by: {@link #constructor_parameterizedConstructor_setsProvidedValues}</li>
 * </ul>
 *
 * <h3>3. setTransactionId(UUID)</h3>
 * <ul>
 *   <li>P1: (Valid) Non-null UUID</li>
 *   <li>Covered by: {@link #setTransactionId_validId_transactionIdIsSet}</li>
 *   <li>P2: (Invalid/Boundary) Null UUID</li>
 *   <li>Covered by: {@link #setTransactionId_nullId_transactionIdIsNull}</li>
 * </ul>
 *
 * <h3>4. setUserId(UUID)</h3>
 * <ul>
 *   <li>P1: (Valid) Non-null UUID</li>
 *   <li>Covered by: {@link #setUserId_validId_userIdIsUpdated}</li>
 *   <li>P2: (Invalid/Boundary) Null UUID</li>
 *   <li>Covered by: {@link #setUserId_nullId_userIdIsNull}</li>
 * </ul>
 *
 * <h3>5. setDescription(String)</h3>
 * <ul>
 *   <li>P1: (Valid) Normal non-empty string</li>
 *   <li>Covered by: {@link #setDescription_validString_descriptionIsUpdated}</li>
 *   <li>P2: (Valid/Boundary) Empty string ""</li>
 *   <li>Covered by: {@link #setDescription_emptyString_descriptionIsEmpty}</li>
 *   <li>P3: (Valid/Edge) String with leading/trailing whitespace - triggers trim()</li>
 *   <li>Covered by: {@link #setDescription_withLeadingTrailingWhitespace_trimsWhitespace}</li>
 *   <li>P4: (Valid/Edge) Whitespace-only string - triggers trim() to empty</li>
 *   <li>Covered by: {@link #setDescription_withWhitespaceOnly_setsEmptyString}</li>
 *   <li>P5: (Invalid) Null input - triggers null-check branch</li>
 *   <li>Covered by: {@link #setDescription_withNull_setsEmptyString}</li>
 * </ul>
 *
 * <h3>6. setAmount(double)</h3>
 * <ul>
 *   <li>P1: (Valid) Positive amount (expense)</li>
 *   <li>Covered by: {@link #setAmount_positiveValue_amountIsUpdated}</li>
 *   <li>P2: (Valid) Negative amount (income/refund)</li>
 *   <li>Covered by: {@link #setAmount_negativeValue_amountIsUpdated}</li>
 *   <li>P3: (Valid/Boundary) Zero amount</li>
 *   <li>Covered by: {@link #setAmount_zeroValue_amountIsZero}</li>
 *   <li>P4: (Valid/Boundary) Very large positive value</li>
 *   <li>Covered by: {@link #setAmount_largePositiveValue_amountIsUpdated}</li>
 *   <li>P5: (Valid/Boundary) Very large negative value</li>
 *   <li>Covered by: {@link #setAmount_largeNegativeValue_amountIsUpdated}</li>
 *   <li>P6: (Invalid/Boundary) Double.MAX_VALUE</li>
 *   <li>Covered by: {@link #setAmount_maxDoubleValue_amountIsUpdated}</li>
 *   <li>P7: (Invalid/Boundary) Double.MIN_VALUE (smallest positive)</li>
 *   <li>Covered by: {@link #setAmount_minDoubleValue_amountIsUpdated}</li>
 *   <li>P8: (Invalid/Edge) Double.NaN</li>
 *   <li>Covered by: {@link #setAmount_nanValue_amountIsNan}</li>
 *   <li>P9: (Invalid/Edge) Double.POSITIVE_INFINITY</li>
 *   <li>Covered by: {@link #setAmount_positiveInfinity_amountIsInfinity}</li>
 *   <li>P10: (Invalid/Edge) Double.NEGATIVE_INFINITY</li>
 *   <li>Covered by: {@link #setAmount_negativeInfinity_amountIsNegativeInfinity}</li>
 * </ul>
 *
 * <h3>7. setCategory(String)</h3>
 * <ul>
 *   <li>P1: (Valid) Normal non-empty string</li>
 *   <li>Covered by: {@link #setCategory_validString_categoryIsUpdated}</li>
 *   <li>P2: (Valid/Boundary) Empty string</li>
 *   <li>Covered by: {@link #setCategory_emptyString_categoryIsEmpty}</li>
 *   <li>P3: (Invalid) Null input</li>
 *   <li>Covered by: {@link #setCategory_nullValue_categoryIsNull}</li>
 * </ul>
 *
 * <h3>8. setTimestamp(LocalDateTime)</h3>
 * <ul>
 *   <li>P1: (Valid) Non-null timestamp - also updates date field</li>
 *   <li>Covered by: {@link #setTimestamp_validDateTime_timestampStoredCorrectly}</li>
 *   <li>P2: (Valid/Edge) Setting timestamp twice - date updates to latest</li>
 *   <li>Covered by: {@link #setTimestamp_settingTwice_updatesDateToLatest}</li>
 *   <li>P3: (Invalid) Null timestamp - timestamp becomes null, date unchanged</li>
 *   <li>Covered by: {@link #setTimestamp_withNull_setsTimestampToNull_leavesDateUnchanged}</li>
 * </ul>
 *
 * <h3>9. setDate(LocalDate)</h3>
 * <ul>
 *   <li>P1: (Valid) Date set when timestamp is null - auto-sets timestamp to start of day</li>
 *   <li>Covered by: {@link #setDate_validDateTimestampNull_returnsDateStartOfDay}</li>
 *   <li>P2: (Valid) Date set when timestamp exists - timestamp unchanged</li>
 *   <li>Covered by: {@link #setDate_withExistingTimestamp_keepsOriginalTimestamp}</li>
 *   <li>P3: (Invalid) Null date - date becomes null, timestamp unchanged</li>
 *   <li>Covered by: {@link #setDate_nullDate_dateIsNull}</li>
 * </ul>
 *
 * <h3>10. effectiveInstant()</h3>
 * <ul>
 *   <li>P1: (Valid) Timestamp exists - returns timestamp</li>
 *   <li>Covered by: {@link #effectiveInstant_withTimestamp_returnsSameTimestamp}</li>
 *   <li>P2: (Valid) Timestamp null, date exists - returns date.atStartOfDay()</li>
 *   <li>Covered by: {@link #effectiveInstant_withDateOnly_returnsDateStartOfDay}</li>
 *   <li>P3: (Invalid) Both timestamp and date null - returns null</li>
 *   <li>Covered by: {@link #effectiveInstant_bothTimestampAndDateNull_returnsNull}</li>
 * </ul>
 *
 * <h3>11. compareTo(Transaction)</h3>
 * <ul>
 *   <li>P1: (Invalid) Other is null - returns positive</li>
 *   <li>Covered by: {@link #compareTo_nullOther_returnsPositive}</li>
 *   <li>P2: (Valid) Different non-null instants - orders by time</li>
 *   <li>Covered by: {@link #compareTo_differentTimestamps_ordersByTime}</li>
 *   <li>P3: (Valid) Same non-null instants - fallback to ID comparison</li>
 *   <li>Covered by: {@link #compareTo_sameTimestamp_ordersById}</li>
 *   <li>P4: (Edge) This instant null, other instant non-null - this comes first</li>
 *   <li>Covered by: {@link #compareTo_oneEffectiveInstantNull_returnsCorrectOrder}</li>
 *   <li>P5: (Edge) This instant non-null, other instant null - other comes first</li>
 *   <li>Covered by: {@link #compareTo_oneEffectiveInstantNull_returnsCorrectOrder}</li>
 *   <li>P6: (Edge) Both instants null - fallback to ID comparison</li>
 *   <li>Covered by: {@link #compareTo_bothEffectiveInstantNull_compareById}</li>
 *   <li>P7: (Edge) Both instants null, both IDs null - returns zero</li>
 *   <li>Covered by: {@link #compareTo_bothNullIds_returnsZero}</li>
 *   <li>P8: (Edge) Both instants null, one ID null - null ID comes first</li>
 *   <li>Covered by: {@link #compareTo_noTime_oneNullId_ordersNullFirst}</li>
 *   <li>P9: (Edge) Same instant, one ID null - null ID comes first</li>
 *   <li>Covered by: {@link #compareTo_sameTime_oneNullId_ordersNullFirst}</li>
 *   <li>P10: (Edge) Same instant, both IDs null - returns zero</li>
 *   <li>Covered by: {@link #compareTo_sameTime_bothNullId_returnsZero}</li>
 * </ul>
 *
 * <h3>12. equals(Object)</h3>
 * <ul>
 *   <li>P1: (Valid) Same object reference - returns true</li>
 *   <li>Covered by: {@link #equals_sameObject_returnsTrue}</li>
 *   <li>P2: (Invalid) Other is null - returns false</li>
 *   <li>Covered by: {@link #equals_null_returnsFalse}</li>
 *   <li>P3: (Invalid) Other is different class - returns false</li>
 *   <li>Covered by: {@link #equals_differentClass_returnsFalse}</li>
 *   <li>P4: (Valid) Same class, same non-null IDs - returns true</li>
 *   <li>Covered by: {@link #equals_sameId_returnsTrue}</li>
 *   <li>P5: (Valid) Same class, different non-null IDs - returns false</li>
 *   <li>Covered by: {@link #equals_differentIds_returnsFalse}</li>
 *   <li>P6: (Edge) Same class, both IDs null - returns true</li>
 *   <li>Covered by: {@link #equals_bothNullId_returnsTrue}</li>
 *   <li>P7: (Edge) Same class, this ID non-null, other ID null - returns false</li>
 *   <li>Covered by: {@link #equals_oneNullId_returnsFalse}</li>
 *   <li>P8: (Edge) Same class, this ID null, other ID non-null - returns false</li>
 *   <li>Covered by: {@link #equals_oneNullId_returnsFalse_reverse}</li>
 * </ul>
 *
 * <h3>13. hashCode()</h3>
 * <ul>
 *   <li>P1: (Valid) Non-null transactionId - returns hash of ID</li>
 *   <li>Covered by: {@link #hashCode_withId_returnsIdHash}</li>
 *   <li>P2: (Edge) Null transactionId - returns hash of null (0)</li>
 *   <li>Covered by: {@link #hashCode_withNullId_returnsZeroHash}</li>
 *   <li>P3: (Contract) Equal objects have equal hash codes</li>
 *   <li>Covered by: {@link #hashCode_equalObjects_haveSameHashCode}</li>
 * </ul>
 *
 * <h3>14. toString()</h3>
 * <ul>
 *   <li>P1: (Valid) Populated object - contains all field values</li>
 *   <li>Covered by: {@link #toString_containsAllFields}</li>
 *   <li>P2: (Edge) Default object - contains null/default values</li>
 *   <li>Covered by: {@link #toString_defaultObject_containsNullValues}</li>
 * </ul>
 */
public class TransactionUnitTests {

  private Transaction transaction;
  private UUID userId;

  /**
   * Sets up a fresh Transaction object before each test.
   * Initializes with typical valid values for most tests.
   */
  @BeforeEach
  public void setUp() {
    userId = UUID.randomUUID();
    transaction = new Transaction(userId, 10.0, "Category", "Description");
  }

  // ===========================================================================
  // Constructors
  // ===========================================================================

  /**
   * Tests the default constructor initializes all fields to null or 0.0.
   *
   * <p>Partition: P1 (Valid) - Default constructor instantiation.
   */
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

  /**
   * Tests the parameterized constructor sets all provided values correctly.
   *
   * <p>Partition: P1 (Valid) - All parameters provided with typical values.
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

  // ===========================================================================
  // getTransactionId / setTransactionId
  // ===========================================================================

  /**
   * Tests setting a valid non-null transaction ID.
   *
   * <p>Partition: P1 (Valid) - Non-null UUID.
   */
  @Test
  public void setTransactionId_validId_transactionIdIsSet() {
    UUID expectedTransactionId = UUID.randomUUID();
    transaction.setTransactionId(expectedTransactionId);
    assertEquals(expectedTransactionId, transaction.getTransactionId());
  }

  /**
   * Tests setting a null transaction ID.
   *
   * <p>Partition: P2 (Invalid/Boundary) - Null UUID.
   */
  @Test
  public void setTransactionId_nullId_transactionIdIsNull() {
    transaction.setTransactionId(UUID.randomUUID());
    assertNotNull(transaction.getTransactionId());
    transaction.setTransactionId(null);
    assertNull(transaction.getTransactionId());
  }

  // ===========================================================================
  // getUserId / setUserId
  // ===========================================================================

  /**
   * Tests that the parameterized constructor correctly sets the userId.
   *
   * <p>Partition: P1 (Valid) - Non-null UUID via constructor.
   */
  @Test
  public void getUserId_afterConstruction_returnsNonNullUserId() {
    assertNotNull(transaction.getUserId());
    assertEquals(userId, transaction.getUserId());
  }

  /**
   * Tests setting a valid non-null user ID.
   *
   * <p>Partition: P1 (Valid) - Non-null UUID.
   */
  @Test
  public void setUserId_validId_userIdIsUpdated() {
    UUID expectedUserId = UUID.randomUUID();
    transaction.setUserId(expectedUserId);
    assertEquals(expectedUserId, transaction.getUserId());
  }

  /**
   * Tests setting a null user ID.
   *
   * <p>Partition: P2 (Invalid/Boundary) - Null UUID.
   */
  @Test
  public void setUserId_nullId_userIdIsNull() {
    assertNotNull(transaction.getUserId());
    transaction.setUserId(null);
    assertNull(transaction.getUserId());
  }

  // ===========================================================================
  // getDescription / setDescription
  // ===========================================================================

  /**
   * Tests that the parameterized constructor correctly sets the description.
   *
   * <p>Partition: P1 (Valid) - Normal non-empty string via constructor.
   */
  @Test
  public void getDescription_afterConstruction_returnsProvidedDescription() {
    assertEquals("Description", transaction.getDescription());
  }

  /**
   * Tests setting a typical valid string description.
   *
   * <p>Partition: P1 (Valid) - Normal non-empty string.
   */
  @Test
  public void setDescription_validString_descriptionIsUpdated() {
    transaction.setDescription("New Description");
    assertEquals("New Description", transaction.getDescription());
  }

  /**
   * Tests setting an empty string description.
   *
   * <p>Partition: P2 (Valid/Boundary) - Empty string "".
   */
  @Test
  public void setDescription_emptyString_descriptionIsEmpty() {
    transaction.setDescription("");
    assertEquals("", transaction.getDescription());
  }

  /**
   * Tests that leading and trailing whitespace is trimmed.
   *
   * <p>Partition: P3 (Valid/Edge) - String with leading/trailing whitespace.
   */
  @Test
  public void setDescription_withLeadingTrailingWhitespace_trimsWhitespace() {
    transaction.setDescription("  Trimmed Description  ");
    assertEquals("Trimmed Description", transaction.getDescription());
  }

  /**
   * Tests that whitespace-only string results in empty string after trim.
   *
   * <p>Partition: P4 (Valid/Edge) - Whitespace-only string.
   */
  @Test
  public void setDescription_withWhitespaceOnly_setsEmptyString() {
    transaction.setDescription("   ");
    assertEquals("", transaction.getDescription());
  }

  /**
   * Tests that null input is converted to empty string.
   *
   * <p>Partition: P5 (Invalid) - Null input.
   */
  @Test
  public void setDescription_withNull_setsEmptyString() {
    transaction.setDescription(null);
    assertEquals("", transaction.getDescription());
  }

  // ===========================================================================
  // getAmount / setAmount
  // ===========================================================================

  /**
   * Tests that the parameterized constructor correctly sets the amount.
   *
   * <p>Partition: P1 (Valid) - Positive amount via constructor.
   */
  @Test
  public void getAmount_afterConstruction_returnsInitialValue() {
    assertEquals(10.0, transaction.getAmount());
  }

  /**
   * Tests setting a typical positive amount (expense).
   *
   * <p>Partition: P1 (Valid) - Positive amount.
   */
  @Test
  public void setAmount_positiveValue_amountIsUpdated() {
    transaction.setAmount(99.99);
    assertEquals(99.99, transaction.getAmount());
  }

  /**
   * Tests setting a negative amount (income/refund).
   *
   * <p>Partition: P2 (Valid) - Negative amount.
   */
  @Test
  public void setAmount_negativeValue_amountIsUpdated() {
    transaction.setAmount(-50.25);
    assertEquals(-50.25, transaction.getAmount());
  }

  /**
   * Tests setting amount to zero (boundary).
   *
   * <p>Partition: P3 (Valid/Boundary) - Zero amount.
   */
  @Test
  public void setAmount_zeroValue_amountIsZero() {
    transaction.setAmount(0.0);
    assertEquals(0.0, transaction.getAmount());
  }

  /**
   * Tests setting a very large positive amount.
   *
   * <p>Partition: P4 (Valid/Boundary) - Very large positive value.
   */
  @Test
  public void setAmount_largePositiveValue_amountIsUpdated() {
    transaction.setAmount(1_000_000_000.99);
    assertEquals(1_000_000_000.99, transaction.getAmount());
  }

  /**
   * Tests setting a very large negative amount.
   *
   * <p>Partition: P5 (Valid/Boundary) - Very large negative value.
   */
  @Test
  public void setAmount_largeNegativeValue_amountIsUpdated() {
    transaction.setAmount(-1_000_000_000.99);
    assertEquals(-1_000_000_000.99, transaction.getAmount());
  }

  /**
   * Tests setting amount to Double.MAX_VALUE.
   *
   * <p>Partition: P6 (Invalid/Boundary) - Maximum double value.
   */
  @Test
  public void setAmount_maxDoubleValue_amountIsUpdated() {
    transaction.setAmount(Double.MAX_VALUE);
    assertEquals(Double.MAX_VALUE, transaction.getAmount());
  }

  /**
   * Tests setting amount to Double.MIN_VALUE (smallest positive).
   *
   * <p>Partition: P7 (Invalid/Boundary) - Minimum positive double value.
   */
  @Test
  public void setAmount_minDoubleValue_amountIsUpdated() {
    transaction.setAmount(Double.MIN_VALUE);
    assertEquals(Double.MIN_VALUE, transaction.getAmount());
  }

  /**
   * Tests setting amount to Double.NaN.
   *
   * <p>Partition: P8 (Invalid/Edge) - Not a Number value.
   */
  @Test
  public void setAmount_nanValue_amountIsNan() {
    transaction.setAmount(Double.NaN);
    assertTrue(Double.isNaN(transaction.getAmount()));
  }

  /**
   * Tests setting amount to positive infinity.
   *
   * <p>Partition: P9 (Invalid/Edge) - Positive infinity.
   */
  @Test
  public void setAmount_positiveInfinity_amountIsInfinity() {
    transaction.setAmount(Double.POSITIVE_INFINITY);
    assertTrue(Double.isInfinite(transaction.getAmount()));
    assertTrue(transaction.getAmount() > 0);
  }

  /**
   * Tests setting amount to negative infinity.
   *
   * <p>Partition: P10 (Invalid/Edge) - Negative infinity.
   */
  @Test
  public void setAmount_negativeInfinity_amountIsNegativeInfinity() {
    transaction.setAmount(Double.NEGATIVE_INFINITY);
    assertTrue(Double.isInfinite(transaction.getAmount()));
    assertTrue(transaction.getAmount() < 0);
  }

  // ===========================================================================
  // getCategory / setCategory
  // ===========================================================================

  /**
   * Tests that the parameterized constructor correctly sets the category.
   *
   * <p>Partition: P1 (Valid) - Normal non-empty string via constructor.
   */
  @Test
  public void getCategory_afterConstruction_returnsInitialCategory() {
    assertEquals("Category", transaction.getCategory());
  }

  /**
   * Tests setting a typical valid string category.
   *
   * <p>Partition: P1 (Valid) - Normal non-empty string.
   */
  @Test
  public void setCategory_validString_categoryIsUpdated() {
    transaction.setCategory("New Category");
    assertEquals("New Category", transaction.getCategory());
  }

  /**
   * Tests setting an empty string category.
   *
   * <p>Partition: P2 (Valid/Boundary) - Empty string.
   */
  @Test
  public void setCategory_emptyString_categoryIsEmpty() {
    transaction.setCategory("");
    assertEquals("", transaction.getCategory());
  }

  /**
   * Tests setting a null category.
   *
   * <p>Partition: P3 (Invalid) - Null input.
   */
  @Test
  public void setCategory_nullValue_categoryIsNull() {
    transaction.setCategory(null);
    assertNull(transaction.getCategory());
  }

  // ===========================================================================
  // getTimestamp / setTimestamp
  // ===========================================================================

  /**
   * Tests the setter and getter for timestamp.
   *
   * <p>Partition: P1 (Valid) - Non-null timestamp.
   */
  @Test
  public void getTimestamp_afterSetting_returnsSameValue() {
    LocalDateTime currentTime = LocalDateTime.now();
    transaction.setTimestamp(currentTime);
    assertNotNull(transaction.getTimestamp());
    assertEquals(currentTime, transaction.getTimestamp());
  }

  /**
   * Tests that setting a timestamp also updates the date field.
   *
   * <p>Partition: P1 (Valid) - Non-null timestamp updates date field.
   */
  @Test
  public void setTimestamp_validDateTime_timestampStoredCorrectly() {
    LocalDateTime timestamp = LocalDateTime.of(2025, 10, 20, 10, 15);
    transaction.setTimestamp(timestamp);
    assertEquals(timestamp, transaction.getTimestamp());
    assertEquals(timestamp.toLocalDate(), transaction.getDate());
  }

  /**
   * Tests that setting the timestamp multiple times updates date to latest.
   *
   * <p>Partition: P2 (Valid/Edge) - Setting timestamp twice.
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

  /**
   * Tests that setting a null timestamp sets timestamp to null but leaves date unchanged.
   *
   * <p>Partition: P3 (Invalid) - Null timestamp input.
   */
  @Test
  public void setTimestamp_withNull_setsTimestampToNull_leavesDateUnchanged() {
    LocalDateTime currentTime = LocalDateTime.now();
    LocalDate expectedDate = currentTime.toLocalDate();

    transaction.setTimestamp(currentTime);
    assertEquals(expectedDate, transaction.getDate());

    transaction.setTimestamp(null);

    assertNull(transaction.getTimestamp());
    assertEquals(expectedDate, transaction.getDate());
  }

  // ===========================================================================
  // getDate / setDate
  // ===========================================================================

  /**
   * Tests the setter and getter for date.
   *
   * <p>Partition: P1 (Valid) - Non-null date.
   */
  @Test
  public void getDate_afterSetting_returnsSameValue() {
    LocalDate date = LocalDate.of(2025, 10, 20);
    transaction.setDate(date);
    assertEquals(date, transaction.getDate());
  }

  /**
   * Tests setting date when timestamp exists - timestamp remains unchanged.
   *
   * <p>Partition: P2 (Valid) - Date set when timestamp exists.
   */
  @Test
  public void setDate_withExistingTimestamp_keepsOriginalTimestamp() {
    LocalDateTime ts = LocalDateTime.of(2024, 6, 1, 18, 30);
    transaction.setTimestamp(ts);
    LocalDate date = LocalDate.of(2024, 6, 2);
    transaction.setDate(date);

    assertEquals(ts, transaction.getTimestamp());
    assertEquals(date, transaction.getDate());
  }

  /**
   * Tests setting date when timestamp is null - auto-sets timestamp to start of day.
   *
   * <p>Partition: P1 (Valid) - Date set when timestamp is null.
   */
  @Test
  public void setDate_validDateTimestampNull_returnsDateStartOfDay() {
    Transaction t = new Transaction();
    LocalDate date = LocalDate.of(2025, 10, 20);
    t.setDate(date);
    assertEquals(date, t.getDate());
    assertEquals(date.atStartOfDay(), t.getTimestamp());
  }

  /**
   * Tests setting a null date - date becomes null, timestamp unchanged.
   *
   * <p>Partition: P3 (Invalid) - Null date input.
   */
  @Test
  public void setDate_nullDate_dateIsNull() {
    transaction.setTimestamp(LocalDateTime.now());
    assertNotNull(transaction.getDate());
    assertNotNull(transaction.getTimestamp());

    transaction.setDate(null);

    assertNull(transaction.getDate());
    assertNotNull(transaction.getTimestamp());
  }

  // ===========================================================================
  // effectiveInstant
  // ===========================================================================

  /**
   * Tests effectiveInstant when timestamp exists - returns timestamp.
   *
   * <p>Partition: P1 (Valid) - Timestamp exists.
   */
  @Test
  public void effectiveInstant_withTimestamp_returnsSameTimestamp() {
    LocalDateTime timestamp = LocalDateTime.of(2025, 10, 20, 10, 15);
    transaction.setTimestamp(timestamp);
    assertEquals(timestamp, transaction.effectiveInstant());
  }

  /**
   * Tests effectiveInstant when only date exists - returns date at start of day.
   *
   * <p>Partition: P2 (Valid) - Timestamp null, date exists.
   */
  @Test
  public void effectiveInstant_withDateOnly_returnsDateStartOfDay() {
    Transaction t = new Transaction();
    LocalDate date = LocalDate.of(2025, 10, 20);

    // Set date without triggering auto-timestamp
    t.setDate(date);
    t.setTimestamp(null);
    // Set date directly to the field
    t.setDate(date);

    assertEquals(date.atStartOfDay(), t.effectiveInstant());
  }

  /**
   * Tests effectiveInstant when both timestamp and date are null - returns null.
   *
   * <p>Partition: P3 (Invalid) - Both timestamp and date null.
   */
  @Test
  public void effectiveInstant_bothTimestampAndDateNull_returnsNull() {
    Transaction t = new Transaction();
    assertNull(t.effectiveInstant());
  }

  // ===========================================================================
  // compareTo
  // ===========================================================================

  /**
   * Tests compareTo when other is null - returns positive.
   *
   * <p>Partition: P1 (Invalid) - Other is null.
   */
  @Test
  public void compareTo_nullOther_returnsPositive() {
    Transaction t1 = new Transaction();
    t1.setTimestamp(LocalDateTime.now());
    t1.setTransactionId(UUID.randomUUID());
    assertTrue(t1.compareTo(null) > 0);
  }

  /**
   * Tests compareTo when both have null effective instants - falls back to ID comparison.
   *
   * <p>Partition: P6 (Edge) - Both instants null.
   */
  @Test
  public void compareTo_bothEffectiveInstantNull_compareById() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    Transaction t1 = new Transaction();
    Transaction t2 = new Transaction();
    t1.setTransactionId(id1);
    t2.setTransactionId(id2);
    assertEquals(id1.compareTo(id2), t1.compareTo(t2));
  }

  /**
   * Tests compareTo when one has null effective instant.
   *
   * <p>Partition: P4/P5 (Edge) - One instant null, other non-null.
   */
  @Test
  public void compareTo_oneEffectiveInstantNull_returnsCorrectOrder() {
    Transaction t1 = new Transaction();
    Transaction t2 = new Transaction();
    t1.setTransactionId(UUID.randomUUID());
    t2.setTimestamp(LocalDateTime.now());
    t2.setTransactionId(UUID.randomUUID());

    assertTrue(t1.compareTo(t2) < 0);
    assertTrue(t2.compareTo(t1) > 0);
  }

  /**
   * Tests compareTo when transactions have different timestamps - orders by time.
   *
   * <p>Partition: P2 (Valid) - Different non-null instants.
   */
  @Test
  public void compareTo_differentTimestamps_ordersByTime() {
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
   * Tests compareTo when transactions have same timestamp - falls back to ID comparison.
   *
   * <p>Partition: P3 (Valid) - Same non-null instants.
   */
  @Test
  public void compareTo_sameTimestamp_ordersById() {
    LocalDateTime now = LocalDateTime.now();
    Transaction t1 = new Transaction();
    Transaction t2 = new Transaction();
    t1.setTimestamp(now);
    UUID id1 = UUID.randomUUID();
    t1.setTransactionId(id1);
    t2.setTimestamp(now);
    UUID id2 = UUID.randomUUID();
    t2.setTransactionId(id2);

    assertEquals(id1.compareTo(id2), t1.compareTo(t2));
  }

  /**
   * Tests compareTo when both instants and IDs are null - returns zero.
   *
   * <p>Partition: P7 (Edge) - Both instants null, both IDs null.
   */
  @Test
  public void compareTo_bothNullIds_returnsZero() {
    Transaction t1 = new Transaction();
    Transaction t2 = new Transaction();
    assertEquals(0, t1.compareTo(t2));
  }

  /**
   * Tests compareTo when instants are null and one ID is null - null ID comes first.
   *
   * <p>Partition: P8 (Edge) - Both instants null, one ID null.
   */
  @Test
  public void compareTo_noTime_oneNullId_ordersNullFirst() {
    Transaction t1 = new Transaction();
    Transaction t2 = new Transaction();
    t2.setTransactionId(UUID.randomUUID());

    assertTrue(t1.compareTo(t2) < 0);
    assertTrue(t2.compareTo(t1) > 0);
  }

  /**
   * Tests compareTo when instants are equal and one ID is null - null ID comes first.
   *
   * <p>Partition: P9 (Edge) - Same instant, one ID null.
   */
  @Test
  public void compareTo_sameTime_oneNullId_ordersNullFirst() {
    LocalDateTime now = LocalDateTime.now();
    Transaction t1 = new Transaction();
    Transaction t2 = new Transaction();
    t1.setTimestamp(now);
    t2.setTimestamp(now);
    t2.setTransactionId(UUID.randomUUID());

    assertTrue(t1.compareTo(t2) < 0);
    assertTrue(t2.compareTo(t1) > 0);
  }

  /**
   * Tests compareTo when instants are equal and both IDs are null - returns zero.
   *
   * <p>Partition: P10 (Edge) - Same instant, both IDs null.
   */
  @Test
  public void compareTo_sameTime_bothNullId_returnsZero() {
    LocalDateTime now = LocalDateTime.now();
    Transaction t1 = new Transaction();
    Transaction t2 = new Transaction();
    t1.setTimestamp(now);
    t2.setTimestamp(now);

    assertEquals(0, t1.compareTo(t2));
  }

  // ===========================================================================
  // equals
  // ===========================================================================

  /**
   * Tests equals when comparing object to itself - returns true.
   *
   * <p>Partition: P1 (Valid) - Same object reference.
   */
  @Test
  public void equals_sameObject_returnsTrue() {
    Transaction t1 = new Transaction();
    t1.setTransactionId(UUID.randomUUID());
    assertEquals(t1, t1);
  }

  /**
   * Tests equals when other is null - returns false.
   *
   * <p>Partition: P2 (Invalid) - Other is null.
   */
  @Test
  public void equals_null_returnsFalse() {
    Transaction t1 = new Transaction();
    assertNotEquals(null, t1);
  }

  /**
   * Tests equals when other is different class - returns false.
   *
   * <p>Partition: P3 (Invalid) - Other is different class.
   */
  @Test
  public void equals_differentClass_returnsFalse() {
    Transaction t1 = new Transaction();
    assertNotEquals(t1, "not a transaction");
    assertNotEquals(t1, 123);
  }

  /**
   * Tests equals when transactions have same non-null IDs - returns true.
   *
   * <p>Partition: P4 (Valid) - Same class, same non-null IDs.
   */
  @Test
  public void equals_sameId_returnsTrue() {
    UUID id = UUID.randomUUID();
    Transaction t1 = new Transaction();
    Transaction t2 = new Transaction();
    t1.setTransactionId(id);
    t2.setTransactionId(id);
    assertEquals(t1, t2);
  }

  /**
   * Tests equals when transactions have different non-null IDs - returns false.
   *
   * <p>Partition: P5 (Valid) - Same class, different non-null IDs.
   */
  @Test
  public void equals_differentIds_returnsFalse() {
    Transaction t1 = new Transaction();
    Transaction t2 = new Transaction();
    t1.setTransactionId(UUID.randomUUID());
    t2.setTransactionId(UUID.randomUUID());
    assertNotEquals(t1, t2);
  }

  /**
   * Tests equals when both transactions have null IDs - returns true.
   *
   * <p>Partition: P6 (Edge) - Same class, both IDs null.
   */
  @Test
  public void equals_bothNullId_returnsTrue() {
    Transaction t1 = new Transaction();
    Transaction t2 = new Transaction();
    assertEquals(t1, t2);
  }

  /**
   * Tests equals when this has non-null ID and other has null ID - returns false.
   *
   * <p>Partition: P7 (Edge) - This ID non-null, other ID null.
   */
  @Test
  public void equals_oneNullId_returnsFalse() {
    Transaction t1 = new Transaction();
    Transaction t2 = new Transaction();
    t1.setTransactionId(UUID.randomUUID());
    assertNotEquals(t1, t2);
  }

  /**
   * Tests equals when this has null ID and other has non-null ID - returns false.
   *
   * <p>Partition: P8 (Edge) - This ID null, other ID non-null.
   */
  @Test
  public void equals_oneNullId_returnsFalse_reverse() {
    Transaction t1 = new Transaction();
    Transaction t2 = new Transaction();
    t2.setTransactionId(UUID.randomUUID());
    assertNotEquals(t1, t2);
  }

  // ===========================================================================
  // hashCode
  // ===========================================================================

  /**
   * Tests hashCode with non-null transactionId - returns hash of ID.
   *
   * <p>Partition: P1 (Valid) - Non-null transactionId.
   */
  @Test
  public void hashCode_withId_returnsIdHash() {
    UUID id = UUID.randomUUID();
    transaction.setTransactionId(id);
    assertEquals(Objects.hash(id), transaction.hashCode());
  }

  /**
   * Tests hashCode with null transactionId - returns hash of null (0).
   *
   * <p>Partition: P2 (Edge) - Null transactionId.
   */
  @Test
  public void hashCode_withNullId_returnsZeroHash() {
    Transaction t1 = new Transaction();
    assertEquals(Objects.hash((UUID) null), t1.hashCode());
  }

  /**
   * Tests that equal objects have equal hash codes (contract requirement).
   *
   * <p>Partition: P3 (Contract) - Equal objects have equal hash codes.
   */
  @Test
  public void hashCode_equalObjects_haveSameHashCode() {
    UUID id = UUID.randomUUID();
    Transaction t1 = new Transaction();
    Transaction t2 = new Transaction();
    t1.setTransactionId(id);
    t2.setTransactionId(id);

    assertEquals(t1, t2);
    assertEquals(t1.hashCode(), t2.hashCode());
  }

  // ===========================================================================
  // toString
  // ===========================================================================

  /**
   * Tests that toString includes all key fields for a populated object.
   *
   * <p>Partition: P1 (Valid) - Populated object.
   */
  @Test
  public void toString_containsAllFields() {
    UUID id = UUID.randomUUID();
    transaction.setTransactionId(id);
    transaction.setAmount(123.45);
    transaction.setDescription("Test");
    LocalDateTime timestamp = LocalDateTime.of(2025, 6, 15, 10, 30);
    transaction.setTimestamp(timestamp);

    String str = transaction.toString();

    assertTrue(str.contains("id=" + id));
    assertTrue(str.contains("amount=123.45"));
    assertTrue(str.contains("description='Test'"));
    assertTrue(str.contains("category='Category'"));
    assertTrue(str.contains("timestamp=" + timestamp));
    assertTrue(str.contains("date=" + timestamp.toLocalDate()));
  }

  /**
   * Tests that toString handles null/default values gracefully.
   *
   * <p>Partition: P2 (Edge) - Default object with null values.
   */
  @Test
  public void toString_defaultObject_containsNullValues() {
    Transaction t = new Transaction();
    String str = t.toString();

    assertTrue(str.contains("id=null"));
    assertTrue(str.contains("description='null'"));
    assertTrue(str.contains("amount=0.0"));
    assertTrue(str.contains("timestamp=null"));
    assertTrue(str.contains("date=null"));
    assertTrue(str.contains("category='null'"));
  }
}