import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import model.User;
import org.junit.jupiter.api.Test;

/**
 * This class contains the unit tests for the User class.
 */
class UserUnitTests {

  @Test
  public void constructor_defaultValues_allFieldsNullOrZero() {
    User u = new User();
    assertNull(u.getUserId());
    assertNull(u.getUsername());
    assertNull(u.getEmail());
    assertEquals(0.0, u.getBudget());
  }

  @Test
  public void constructor_withValidParameters_setsProvidedFields() {
    User u = new User("lisa", "lisa@x.com", 200.0);
    assertEquals("lisa", u.getUsername());
    assertEquals("lisa@x.com", u.getEmail());
    assertEquals(200.0, u.getBudget());
    assertNull(u.getUserId()); // DB assigns later
  }

  @Test
  public void gettersAndSetters_allFieldsSet_explicitValuesReturned() {
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

  @Test
  public void setters_existingFieldsUpdated_newValuesReflected() {
    User u = new User("alice", "a@b.com", 100);
    u.setBudget(999.99);
    u.setEmail("new@b.com");

    assertEquals(999.99, u.getBudget());
    assertEquals("new@b.com", u.getEmail());
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

  // Note on "invalid input":
  // This model intentionally has no validation logic (e.g., rejects null/empty username),
  // so we cannot meaningfully create a failing “invalid” scenario here. If validation is
  // added later (e.g., non-null username/email, non-negative budget), add tests that
  // assert thrown exceptions or rejected states for those invalid inputs.
}
