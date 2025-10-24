package dev.ase.teamproject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.ase.teamproject.model.User;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class contains the unit tests for the User class.
 * <p>
 * Since this is a simple data class (POJO) with only trivial getters,
 * setters, and constructors, the tests focus on verifying that all
 * field assignments and retrievals work as expected.
 * </p>
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

  @Test
  public void constructor_defaultConstructor_initializesFieldsToNullOrZero() {
    assertNull(user.getUserId());
    assertNull(user.getUsername());
    assertNull(user.getEmail());
    assertEquals(0.0, user.getBudget());
  }

  @Test
  public void constructor_parameterizedConstructor_setsProvidedValues() {
    User u = new User("Alice", "alice@example.com", 100.0);
    assertEquals("Alice", u.getUsername());
    assertEquals("alice@example.com", u.getEmail());
    assertEquals(100.0, u.getBudget());
    assertNull(u.getUserId()); // DB Assigns Later
  }

  // ---------------------------------------------------------------------------
  // setUserId / getUserId
  // ---------------------------------------------------------------------------

  @Test
  public void setUserId_withValidUuid_storesUuid() {
    user.setUserId(testId);
    assertEquals(testId, user.getUserId());
  }

  @Test
  public void getUserId_afterSettingUuid_returnsSameUuid() {
    user.setUserId(testId);
    UUID result = user.getUserId();
    assertEquals(testId, result);
  }

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

  // ---------------------------------------------------------------------------
  // setUsername / getUsername
  // ---------------------------------------------------------------------------

  @Test
  public void setUsername_withValidString_storesUsername() {
    user.setUsername("Bob");
    assertEquals("Bob", user.getUsername());
  }

  @Test
  void getUsername_afterSettingUsername_returnsSameValue() {
    user.setUsername("Bob");
    String result = user.getUsername();
    assertEquals("Bob", result);
  }

  // ---------------------------------------------------------------------------
  // setEmail / getEmail
  // ---------------------------------------------------------------------------

  @Test
  public void setEmail_withValidString_storesEmail() {
    user.setEmail("bob@example.com");
    assertEquals("bob@example.com", user.getEmail());
  }

  @Test
  public void getEmail_afterSettingEmail_returnsSameValue() {
    user.setEmail("bob@example.com");
    String result = user.getEmail();
    assertEquals("bob@example.com", result);
  }

  // ---------------------------------------------------------------------------
  // setBudget / getBudget
  // ---------------------------------------------------------------------------

  @Test
  public void setBudget_withPositiveValue_storesBudget() {
    user.setBudget(250.75);
    assertEquals(250.75, user.getBudget());
  }

  @Test
  public void getBudget_afterSettingBudget_returnsSameValue() {
    user.setBudget(250.75);
    double result = user.getBudget();
    assertEquals(250.75, result);
  }
}
