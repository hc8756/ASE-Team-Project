import org.junit.jupiter.api.Test;

import model.User;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the simple User POJO.
 *
 * Rubric mapping:
 * - For each “unit” (constructor and getters/setters), we include cases that represent
 *   typical values and edge-ish cases (e.g., default/zero), and we point out where an
 *   “invalid” concept would normally live (this class itself has no validation logic).
 * - Tests are grouped in one class for this model (related tests grouped together).
 * - Tests run via JUnit/Maven (automated test runner).
 */
class UserUnitTests {

  /**
   * Edge / boundary case: default constructor should leave reference fields null
   * and primitives at their default (0.0). This is our "empty object" baseline.
   */
  @Test
  void defaultConstructor_allFieldsNullOrZero() {
    User u = new User();
    assertNull(u.getUserId());
    assertNull(u.getUsername());
    assertNull(u.getEmail());
    assertEquals(0.0, u.getBudget());
  }

  /**
   * Typical valid input: parameterized constructor sets fields that are provided.
   * Note: userId is intentionally left null (DB would assign it later).
   */
  @Test
  void parameterizedConstructor_setsFieldsCorrectly() {
    User u = new User("lisa", "lisa@x.com", 200.0);
    assertEquals("lisa", u.getUsername());
    assertEquals("lisa@x.com", u.getEmail());
    assertEquals(200.0, u.getBudget());
    assertNull(u.getUserId()); // DB assigns later
  }

  /**
   * Typical valid input for setters/getters. This also covers the “equivalence class”
   * where all fields are explicitly set after construction.
   */
  @Test
  void gettersAndSetters_workIndividually() {
    User u = new User();

    UUID id = UUID.randomUUID();
    u.setUserId(id);
    u.setUsername("neo");
    u.setEmail("n@x.com");
    u.setBudget(500.5);

    assertEquals(id, u.getUserId());
    assertEquals("neo", u.getUsername());
    assertEquals("n@x.com", u.getEmail());
    assertEquals(500.5, u.getBudget());
  }

  /**
   * Atypical but valid: fields can be changed after creation (mutability).
   * This checks that later updates are reflected (e.g., budget and email changes).
   */
  @Test
  void canModifyFieldsAfterCreation() {
    User u = new User("alice", "a@b.com", 100);
    u.setBudget(999.99);
    u.setEmail("new@b.com");

    assertEquals(999.99, u.getBudget());
    assertEquals("new@b.com", u.getEmail());
  }

  /**
   * Another typical case: distinct users can be assigned unique IDs.
   * (If the app later enforces constraints, this test documents expected distinctness.)
   */
  @Test
  void uniqueUserIdsCanBeAssigned() {
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

  // Note on "invalid input":
  // This model intentionally has no validation logic (e.g., rejects null/empty username),
  // so we cannot meaningfully create a failing “invalid” scenario here. If validation is
  // added later (e.g., non-null username/email, non-negative budget), add tests that
  // assert thrown exceptions or rejected states for those invalid inputs.
}
