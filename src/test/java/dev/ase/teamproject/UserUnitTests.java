package dev.ase.teamproject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.ase.teamproject.model.User;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the User class.
 * <p>
 * This test suite validates all getter and setter methods in the User POJO,
 * covering both valid and invalid equivalence partitions with boundary testing
 * where applicable. Since User is a simple data class, tests focus on field
 * assignment, retrieval, and edge case handling.
 * </p>
 *
 * <h2>Equivalence Partitions Defined:</h2>
 * <ul>
 *   <li><b>userId</b>:
 *     <ul>
 *       <li>Valid: Non-null UUID</li>
 *       <li>Invalid: null</li>
 *     </ul>
 *   </li>
 *   <li><b>username</b>:
 *     <ul>
 *       <li>Valid: Non-empty string, typical alphanumeric string</li>
 *       <li>Boundary: Empty string, single character, very long string</li>
 *       <li>Edge: String with special characters, whitespace</li>
 *       <li>Invalid: null</li>
 *     </ul>
 *   </li>
 *   <li><b>email</b>:
 *     <ul>
 *       <li>Valid: Standard email format string</li>
 *       <li>Boundary: Empty string, single character</li>
 *       <li>Edge: String without @ symbol (malformed), very long string</li>
 *       <li>Invalid: null</li>
 *     </ul>
 *   </li>
 *   <li><b>budget</b>:
 *     <ul>
 *       <li>Valid: Positive values (including decimals)</li>
 *       <li>Boundary: Zero, very small positive (0.01), very large positive</li>
 *       <li>Edge: Negative values, Double.MAX_VALUE, Double.MIN_VALUE</li>
 *       <li>Special: Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public class UserUnitTests {

  private User user;
  private UUID testId;

  @BeforeEach
  public void setUp() {
    testId = UUID.randomUUID();
    user = new User();
  }

  // ---------------------------------------------------------------------------
  // Constructors
  // ---------------------------------------------------------------------------

  /**
   * Tests default constructor initialization.
   * <p>Partition: Default initialization (null/zero values)</p>
   */
  @Test
  public void constructor_defaultConstructor_initializesFieldsToNullOrZero() {
    assertNull(user.getUserId());
    assertNull(user.getUsername());
    assertNull(user.getEmail());
    assertEquals(0.0, user.getBudget());
  }

  /**
   * Tests parameterized constructor with valid typical values.
   * <p>Partition: Valid username, valid email, positive budget</p>
   */
  @Test
  public void constructor_parameterizedConstructor_setsProvidedValues() {
    User u = new User("Alice", "alice@example.com", 100.0);
    assertEquals("Alice", u.getUsername());
    assertEquals("alice@example.com", u.getEmail());
    assertEquals(100.0, u.getBudget());
    assertNull(u.getUserId()); // DB assigns later
  }

  /**
   * Tests parameterized constructor with boundary values.
   * <p>Partition: Empty strings, zero budget</p>
   */
  @Test
  public void constructor_parameterizedWithBoundaryValues_setsValues() {
    User u = new User("", "", 0.0);
    assertEquals("", u.getUsername());
    assertEquals("", u.getEmail());
    assertEquals(0.0, u.getBudget());
  }

  /**
   * Tests parameterized constructor with null values.
   * <p>Partition: null username, null email, negative budget</p>
   */
  @Test
  public void constructor_parameterizedWithNullValues_acceptsNulls() {
    User u = new User(null, null, -50.0);
    assertNull(u.getUsername());
    assertNull(u.getEmail());
    assertEquals(-50.0, u.getBudget());
  }

  // ---------------------------------------------------------------------------
  // setUserId / getUserId
  // ---------------------------------------------------------------------------

  /**
   * Tests setUserId with a valid UUID.
   * <p>Partition: Valid non-null UUID</p>
   */
  @Test
  public void setUserId_withValidUuid_storesUuid() {
    user.setUserId(testId);
    assertEquals(testId, user.getUserId());
  }

  /**
   * Tests getUserId returns the same UUID that was set.
   * <p>Partition: Valid non-null UUID</p>
   */
  @Test
  public void getUserId_afterSettingUuid_returnsSameUuid() {
    user.setUserId(testId);
    UUID result = user.getUserId();
    assertEquals(testId, result);
  }

  /**
   * Tests that different users can have distinct UUIDs.
   * <p>Partition: Multiple valid UUIDs remain unique</p>
   */
  @Test
  public void setUserId_twoDistinctIds_assignedIdsRemainUnique() {
    User u1 = new User();
    User u2 = new User();
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();

    u1.setUserId(id1);
    u2.setUserId(id2);

    assertNotEquals(u1.getUserId(), u2.getUserId());
    assertEquals(id1, u1.getUserId());
    assertEquals(id2, u2.getUserId());
  }

  /**
   * Tests setUserId with null value.
   * <p>Partition: Invalid null UUID</p>
   */
  @Test
  public void setUserId_withNull_storesNull() {
    user.setUserId(testId);
    user.setUserId(null);
    assertNull(user.getUserId());
  }

  /**
   * Tests overwriting an existing UUID.
   * <p>Partition: Valid UUID replacement</p>
   */
  @Test
  public void setUserId_overwritingExistingId_replacesWithNewId() {
    UUID firstId = UUID.randomUUID();
    UUID secondId = UUID.randomUUID();

    user.setUserId(firstId);
    assertEquals(firstId, user.getUserId());

    user.setUserId(secondId);
    assertEquals(secondId, user.getUserId());
    assertNotEquals(firstId, user.getUserId());
  }

  // ---------------------------------------------------------------------------
  // setUsername / getUsername
  // ---------------------------------------------------------------------------

  /**
   * Tests setUsername with a typical valid string.
   * <p>Partition: Valid non-empty alphanumeric string</p>
   */
  @Test
  public void setUsername_withValidString_storesUsername() {
    user.setUsername("Bob");
    assertEquals("Bob", user.getUsername());
  }

  /**
   * Tests getUsername returns the same value that was set.
   * <p>Partition: Valid non-empty string</p>
   */
  @Test
  public void getUsername_afterSettingUsername_returnsSameValue() {
    user.setUsername("Bob");
    String result = user.getUsername();
    assertEquals("Bob", result);
  }

  /**
   * Tests setUsername with empty string boundary.
   * <p>Partition: Boundary - empty string</p>
   */
  @Test
  public void setUsername_withEmptyString_storesEmptyString() {
    user.setUsername("");
    assertEquals("", user.getUsername());
  }

  /**
   * Tests setUsername with single character boundary.
   * <p>Partition: Boundary - single character string</p>
   */
  @Test
  public void setUsername_withSingleCharacter_storesSingleCharacter() {
    user.setUsername("A");
    assertEquals("A", user.getUsername());
  }

  /**
   * Tests setUsername with a very long string.
   * <p>Partition: Boundary - very long string (1000 characters)</p>
   */
  @Test
  public void setUsername_withVeryLongString_storesLongString() {
    String longName = "A".repeat(1000);
    user.setUsername(longName);
    assertEquals(longName, user.getUsername());
    assertEquals(1000, user.getUsername().length());
  }

  /**
   * Tests setUsername with special characters.
   * <p>Partition: Edge - string with special characters</p>
   */
  @Test
  public void setUsername_withSpecialCharacters_storesSpecialCharacters() {
    String specialName = "User@123!#$%";
    user.setUsername(specialName);
    assertEquals(specialName, user.getUsername());
  }

  /**
   * Tests setUsername with whitespace.
   * <p>Partition: Edge - string with spaces</p>
   */
  @Test
  public void setUsername_withWhitespace_storesWhitespace() {
    user.setUsername("John Doe");
    assertEquals("John Doe", user.getUsername());
  }

  /**
   * Tests setUsername with null value.
   * <p>Partition: Invalid - null string</p>
   */
  @Test
  public void setUsername_withNull_storesNull() {
    user.setUsername("Bob");
    user.setUsername(null);
    assertNull(user.getUsername());
  }

  /**
   * Tests overwriting an existing username.
   * <p>Partition: Valid username replacement</p>
   */
  @Test
  public void setUsername_overwritingExistingUsername_replacesWithNewValue() {
    user.setUsername("Alice");
    assertEquals("Alice", user.getUsername());

    user.setUsername("Bob");
    assertEquals("Bob", user.getUsername());
  }

  // ---------------------------------------------------------------------------
  // setEmail / getEmail
  // ---------------------------------------------------------------------------

  /**
   * Tests setEmail with a typical valid email string.
   * <p>Partition: Valid standard email format</p>
   */
  @Test
  public void setEmail_withValidString_storesEmail() {
    user.setEmail("bob@example.com");
    assertEquals("bob@example.com", user.getEmail());
  }

  /**
   * Tests getEmail returns the same value that was set.
   * <p>Partition: Valid email string</p>
   */
  @Test
  public void getEmail_afterSettingEmail_returnsSameValue() {
    user.setEmail("bob@example.com");
    String result = user.getEmail();
    assertEquals("bob@example.com", result);
  }

  /**
   * Tests setEmail with empty string boundary.
   * <p>Partition: Boundary - empty string</p>
   */
  @Test
  public void setEmail_withEmptyString_storesEmptyString() {
    user.setEmail("");
    assertEquals("", user.getEmail());
  }

  /**
   * Tests setEmail with single character boundary.
   * <p>Partition: Boundary - single character</p>
   */
  @Test
  public void setEmail_withSingleCharacter_storesSingleCharacter() {
    user.setEmail("a");
    assertEquals("a", user.getEmail());
  }

  /**
   * Tests setEmail with malformed email (no @ symbol).
   * <p>Partition: Edge - malformed email string</p>
   */
  @Test
  public void setEmail_withMalformedEmail_storesMalformedEmail() {
    user.setEmail("notanemail");
    assertEquals("notanemail", user.getEmail());
  }

  /**
   * Tests setEmail with very long email string.
   * <p>Partition: Boundary - very long string (500 characters)</p>
   */
  @Test
  public void setEmail_withVeryLongString_storesLongEmail() {
    String longEmail = "a".repeat(490) + "@test.com";
    user.setEmail(longEmail);
    assertEquals(longEmail, user.getEmail());
  }

  /**
   * Tests setEmail with special characters in email.
   * <p>Partition: Edge - email with special characters</p>
   */
  @Test
  public void setEmail_withSpecialCharacters_storesSpecialEmail() {
    String specialEmail = "user+tag@sub-domain.example.com";
    user.setEmail(specialEmail);
    assertEquals(specialEmail, user.getEmail());
  }

  /**
   * Tests setEmail with null value.
   * <p>Partition: Invalid - null string</p>
   */
  @Test
  public void setEmail_withNull_storesNull() {
    user.setEmail("bob@example.com");
    user.setEmail(null);
    assertNull(user.getEmail());
  }

  /**
   * Tests overwriting an existing email.
   * <p>Partition: Valid email replacement</p>
   */
  @Test
  public void setEmail_overwritingExistingEmail_replacesWithNewValue() {
    user.setEmail("first@example.com");
    assertEquals("first@example.com", user.getEmail());

    user.setEmail("second@example.com");
    assertEquals("second@example.com", user.getEmail());
  }

  // ---------------------------------------------------------------------------
  // setBudget / getBudget
  // ---------------------------------------------------------------------------

  /**
   * Tests setBudget with a typical positive value.
   * <p>Partition: Valid positive decimal value</p>
   */
  @Test
  public void setBudget_withPositiveValue_storesBudget() {
    user.setBudget(250.75);
    assertEquals(250.75, user.getBudget());
  }

  /**
   * Tests getBudget returns the same value that was set.
   * <p>Partition: Valid positive value</p>
   */
  @Test
  public void getBudget_afterSettingBudget_returnsSameValue() {
    user.setBudget(250.75);
    double result = user.getBudget();
    assertEquals(250.75, result);
  }

  /**
   * Tests setBudget with zero boundary value.
   * <p>Partition: Boundary - zero</p>
   */
  @Test
  public void setBudget_withZero_storesZero() {
    user.setBudget(0.0);
    assertEquals(0.0, user.getBudget());
  }

  /**
   * Tests setBudget with very small positive boundary value.
   * <p>Partition: Boundary - smallest positive practical value (0.01)</p>
   */
  @Test
  public void setBudget_withVerySmallPositive_storesSmallValue() {
    user.setBudget(0.01);
    assertEquals(0.01, user.getBudget(), 0.001);
  }

  /**
   * Tests setBudget with very large positive value.
   * <p>Partition: Boundary - large positive value</p>
   */
  @Test
  public void setBudget_withVeryLargePositive_storesLargeValue() {
    user.setBudget(999999999.99);
    assertEquals(999999999.99, user.getBudget());
  }

  /**
   * Tests setBudget with negative value.
   * <p>Partition: Edge - negative value</p>
   */
  @Test
  public void setBudget_withNegativeValue_storesNegativeValue() {
    user.setBudget(-100.50);
    assertEquals(-100.50, user.getBudget());
  }

  /**
   * Tests setBudget with Double.MAX_VALUE boundary.
   * <p>Partition: Boundary - Double.MAX_VALUE</p>
   */
  @Test
  public void setBudget_withMaxDouble_storesMaxValue() {
    user.setBudget(Double.MAX_VALUE);
    assertEquals(Double.MAX_VALUE, user.getBudget());
  }

  /**
   * Tests setBudget with Double.MIN_VALUE boundary.
   * <p>Partition: Boundary - Double.MIN_VALUE (smallest positive non-zero)</p>
   */
  @Test
  public void setBudget_withMinDouble_storesMinValue() {
    user.setBudget(Double.MIN_VALUE);
    assertEquals(Double.MIN_VALUE, user.getBudget());
  }

  /**
   * Tests setBudget with positive infinity.
   * <p>Partition: Special - Double.POSITIVE_INFINITY</p>
   */
  @Test
  public void setBudget_withPositiveInfinity_storesInfinity() {
    user.setBudget(Double.POSITIVE_INFINITY);
    assertEquals(Double.POSITIVE_INFINITY, user.getBudget());
  }

  /**
   * Tests setBudget with negative infinity.
   * <p>Partition: Special - Double.NEGATIVE_INFINITY</p>
   */
  @Test
  public void setBudget_withNegativeInfinity_storesNegativeInfinity() {
    user.setBudget(Double.NEGATIVE_INFINITY);
    assertEquals(Double.NEGATIVE_INFINITY, user.getBudget());
  }

  /**
   * Tests setBudget with NaN (Not a Number).
   * <p>Partition: Special - Double.NaN</p>
   */
  @Test
  public void setBudget_withNaN_storesNaN() {
    user.setBudget(Double.NaN);
    assertEquals(Double.NaN, user.getBudget());
  }

  /**
   * Tests overwriting an existing budget value.
   * <p>Partition: Valid budget replacement</p>
   */
  @Test
  public void setBudget_overwritingExistingBudget_replacesWithNewValue() {
    user.setBudget(100.0);
    assertEquals(100.0, user.getBudget());

    user.setBudget(200.0);
    assertEquals(200.0, user.getBudget());
  }

  /**
   * Tests budget precision with multiple decimal places.
   * <p>Partition: Valid - decimal precision testing</p>
   */
  @Test
  public void setBudget_withMultipleDecimalPlaces_maintainsPrecision() {
    user.setBudget(123.456789);
    assertEquals(123.456789, user.getBudget(), 0.000001);
  }
}