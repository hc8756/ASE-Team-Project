package dev.ase.teamproject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ase.teamproject.controller.RouteController;
import dev.ase.teamproject.model.Transaction;
import dev.ase.teamproject.model.User;
import dev.ase.teamproject.service.MockApiService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for the {@link RouteController} class.
 *
 * <p>This test suite validates all public endpoints in the RouteController,
 * covering valid and invalid equivalence partitions including boundary values.
 * Tests use Mockito to mock the MockApiService dependency and share common setup
 * through {@code @BeforeEach} and {@code @AfterEach}.
 *
 * <h2>Equivalence Partitions by Endpoint</h2>
 *
 * <h3>1. GET / or /index</h3>
 * <ul>
 *   <li>P1: (Valid) Users exist - returns HTML with user list</li>
 *   <li>P2: (Valid/Boundary) No users - returns "No users found" message</li>
 *   <li>P3: (Invalid) Service throws exception - propagates RuntimeException</li>
 *   <li>P4: (Edge) Logger disabled - still returns correct HTML</li>
 * </ul>
 *
 * <h3>2. GET /users</h3>
 * <ul>
 *   <li>P1: (Valid) Returns list of users from service</li>
 *   <li>P2: (Edge) Logger disabled - still returns list</li>
 * </ul>
 *
 * <h3>3. GET /users/{userId}</h3>
 * <ul>
 *   <li>P1: (Valid) User exists - returns 200 OK with user</li>
 *   <li>P2: (Valid/Edge) User with minimal fields - returns 200 OK</li>
 *   <li>P3: (Invalid) User not found - throws NoSuchElementException</li>
 *   <li>P4: (Edge) Logger disabled - behavior unchanged</li>
 * </ul>
 *
 * <h3>4. POST /users (JSON)</h3>
 * <ul>
 *   <li>P1: (Valid) Valid user - returns 201 CREATED</li>
 *   <li>P2: (Valid/Edge) Empty email string - returns 201 CREATED</li>
 *   <li>P3: (Invalid) Null username - throws RuntimeException with cause</li>
 *   <li>P4: (Invalid) Null email - throws RuntimeException with cause</li>
 *   <li>P5: (Invalid) Duplicate email - throws IllegalArgumentException</li>
 *   <li>P6: (Invalid) Duplicate username - throws IllegalArgumentException</li>
 *   <li>P7: (Invalid) Generic data integrity violation - throws IllegalArgumentException</li>
 *   <li>P8: (Invalid) Service throws exception - throws IllegalStateException</li>
 *   <li>P9: (Edge) Logger disabled - behavior unchanged</li>
 * </ul>
 *
 * <h3>5. POST /users/form (HTML)</h3>
 * <ul>
 *   <li>P1: (Valid) Valid form data - returns 201 CREATED HTML</li>
 *   <li>P2: (Valid/Boundary) Zero budget - returns 201 CREATED with $0.00</li>
 *   <li>P3: (Invalid) Duplicate username - returns 400 BAD_REQUEST</li>
 *   <li>P4: (Invalid) Duplicate email - returns 400 BAD_REQUEST</li>
 *   <li>P5: (Invalid) Service throws exception - propagates exception</li>
 *   <li>P6: (Edge) Logger disabled - behavior unchanged</li>
 * </ul>
 *
 * <h3>6. PUT /users/{userId} (JSON)</h3>
 * <ul>
 *   <li>P1: (Valid) User exists, all fields updated - returns 200 OK</li>
 *   <li>P2: (Valid/Boundary) Zero budget - returns 200 OK</li>
 *   <li>P3: (Valid/Edge) Null/empty fields use existing values</li>
 *   <li>P4: (Invalid) User not found - throws NoSuchElementException</li>
 *   <li>P5: (Invalid) Duplicate username - throws IllegalArgumentException</li>
 *   <li>P6: (Invalid) Duplicate email - throws IllegalArgumentException</li>
 *   <li>P7: (Edge) Logger disabled - behavior unchanged</li>
 * </ul>
 *
 * <h3>7. POST /users/{userId}/update-form (HTML)</h3>
 * <ul>
 *   <li>P1: (Valid) User exists - returns 200 OK HTML</li>
 *   <li>P2: (Invalid) User not found - throws NoSuchElementException</li>
 *   <li>P3: (Invalid) Duplicate username - returns 400 BAD_REQUEST</li>
 *   <li>P4: (Invalid) Duplicate email - returns 400 BAD_REQUEST</li>
 *   <li>P5: (Edge) Logger disabled - behavior unchanged</li>
 * </ul>
 *
 * <h3>8. GET /users/create-form</h3>
 * <ul>
 *   <li>P1: (Valid) Returns HTML form</li>
 *   <li>P2: (Edge) Logger disabled - still returns form</li>
 * </ul>
 *
 * <h3>9. GET /users/{userId}/edit-form</h3>
 * <ul>
 *   <li>P1: (Valid) User exists - returns populated form</li>
 *   <li>P2: (Invalid) User not found - throws NoSuchElementException</li>
 *   <li>P3: (Edge) Logger disabled - behavior unchanged</li>
 * </ul>
 *
 * <h3>10. DELETE /users/{userId}</h3>
 * <ul>
 *   <li>P1: (Valid) User exists - returns 200 OK with confirmation</li>
 *   <li>P2: (Invalid) User not found/delete fails - throws NoSuchElementException</li>
 *   <li>P3: (Edge) Logger disabled - behavior unchanged</li>
 * </ul>
 *
 * <h3>11. GET /deleteuser/{userId}</h3>
 * <ul>
 *   <li>P1: (Valid) User exists - returns success message</li>
 *   <li>P2: (Invalid) User not found - throws NoSuchElementException</li>
 *   <li>P3: (Edge) Logger disabled - behavior unchanged</li>
 * </ul>
 *
 * <h3>12. GET /users/{userId}/transactions</h3>
 * <ul>
 *   <li>P1: (Valid) User exists with transactions - returns 200 OK list</li>
 *   <li>P2: (Valid/Boundary) User exists, no transactions - returns empty list</li>
 *   <li>P3: (Invalid) User not found - returns 404 NOT_FOUND</li>
 *   <li>P4: (Invalid) Service throws exception - returns 500 INTERNAL_SERVER_ERROR</li>
 *   <li>P5: (Edge) Logger disabled - behavior unchanged</li>
 * </ul>
 *
 * <h3>13. GET /users/{userId}/transactions/{transactionId}</h3>
 * <ul>
 *   <li>P1: (Valid) User and transaction exist, belongs to user - returns 200 OK</li>
 *   <li>P2: (Invalid) User not found - throws NoSuchElementException</li>
 *   <li>P3: (Invalid) Transaction not found - throws NoSuchElementException</li>
 *   <li>P4: (Invalid) Transaction belongs to different user - throws NoSuchElementException</li>
 *   <li>P5: (Edge) Logger disabled - behavior unchanged</li>
 * </ul>
 *
 * <h3>14. POST /users/{userId}/transactions (JSON)</h3>
 * <ul>
 *   <li>P1: (Valid) Valid transaction - returns 201 CREATED</li>
 *   <li>P2: (Invalid) User not found - throws NoSuchElementException</li>
 *   <li>P3: (Invalid) Service throws exception - propagates exception</li>
 *   <li>P4: (Edge) Logger disabled - behavior unchanged</li>
 * </ul>
 *
 * <h3>15. POST /users/{userId}/transactions/form (HTML)</h3>
 * <ul>
 *   <li>P1: (Valid) Valid form data - returns 201 CREATED HTML</li>
 *   <li>P2: (Invalid) User not found - returns 404 NOT_FOUND</li>
 *   <li>P3: (Invalid) Service throws exception - returns 500 INTERNAL_SERVER_ERROR</li>
 *   <li>P4: (Edge) Logger disabled - behavior unchanged</li>
 * </ul>
 *
 * <h3>16. PUT /users/{userId}/transactions/{transactionId} (JSON)</h3>
 * <ul>
 *   <li>P1: (Valid) Valid update - returns 200 OK with updated transaction</li>
 *   <li>P2: (Invalid) User not found - throws NoSuchElementException</li>
 *   <li>P3: (Invalid) Transaction not found - throws NoSuchElementException</li>
 *   <li>P4: (Invalid) Transaction belongs to different user - throws NoSuchElementException</li>
 *   <li>P5: (Invalid) Update returns empty - throws NoSuchElementException</li>
 *   <li>P6: (Invalid) Service throws exception - propagates exception</li>
 *   <li>P7: (Edge) Logger disabled - behavior unchanged</li>
 * </ul>
 *
 * <h3>17. GET /users/{userId}/transactions/create-form</h3>
 * <ul>
 *   <li>P1: (Valid) User exists - returns HTML form</li>
 *   <li>P2: (Valid/Edge) Minimal user - returns HTML form</li>
 *   <li>P3: (Invalid) User not found - throws NoSuchElementException</li>
 *   <li>P4: (Edge) Logger disabled - behavior unchanged</li>
 * </ul>
 *
 * <h3>18. DELETE /users/{userId}/transactions/{transactionId}</h3>
 * <ul>
 *   <li>P1: (Valid) Valid deletion - returns 200 OK with confirmation</li>
 *   <li>P2: (Invalid) User not found - throws NoSuchElementException</li>
 *   <li>P3: (Invalid) Transaction not found - throws NoSuchElementException</li>
 *   <li>P4: (Invalid) Transaction belongs to different user - throws NoSuchElementException</li>
 *   <li>P5: (Invalid) Delete returns false - throws NoSuchElementException</li>
 *   <li>P6: (Edge) Logger disabled - behavior unchanged</li>
 * </ul>
 *
 * <h3>19. GET /users/{userId}/deletetransaction/{transactionId}</h3>
 * <ul>
 *   <li>P1: (Valid) Valid deletion - returns success message</li>
 *   <li>P2: (Invalid) User not found - returns error message</li>
 *   <li>P3: (Invalid) Transaction not found or wrong user - returns error message</li>
 *   <li>P4: (Invalid) Delete fails - returns error message</li>
 *   <li>P5: (Edge) Logger disabled - behavior unchanged</li>
 * </ul>
 *
 * <h3>20. GET /users/{userId}/budget (HTML)</h3>
 * <ul>
 *   <li>P1: (Valid) User exists - returns budget management HTML</li>
 *   <li>P2: (Invalid) User not found - throws NoSuchElementException</li>
 *   <li>P3: (Edge) Logger disabled - behavior unchanged</li>
 * </ul>
 *
 * <h3>21. PUT /users/{userId}/budget (JSON)</h3>
 * <ul>
 *   <li>P1: (Valid) Valid budget update - returns 200 OK with report</li>
 *   <li>P2: (Invalid) User not found - throws NoSuchElementException</li>
 *   <li>P3: (Edge) Logger disabled - behavior unchanged</li>
 * </ul>
 *
 * <h3>22. POST /users/{userId}/update-budget (HTML form)</h3>
 * <ul>
 *   <li>P1: (Valid) Valid budget - returns 200 OK HTML</li>
 *   <li>P2: (Valid/Boundary) Zero budget - returns 200 OK</li>
 *   <li>P3: (Invalid) User not found - throws NoSuchElementException</li>
 *   <li>P4: (Edge) Logger disabled - behavior unchanged</li>
 * </ul>
 *
 * <h3>23. GET /users/{userId}/weekly-summary (JSON)</h3>
 * <ul>
 *   <li>P1: (Valid) User with transactions - returns summary map</li>
 *   <li>P2: (Valid/Boundary) User with no transactions - returns empty summary</li>
 *   <li>P3: (Invalid) User not found - throws NoSuchElementException</li>
 *   <li>P4: (Edge) Logger disabled - behavior unchanged</li>
 * </ul>
 *
 * <h3>24. GET /users/{userId}/monthly-summary (JSON)</h3>
 * <ul>
 *   <li>P1: (Valid) User exists - returns summary map</li>
 *   <li>P2: (Valid/Edge) Service returns null summary - map has no "summary" key</li>
 *   <li>P3: (Valid/Edge) Service returns empty summary - map has empty string</li>
 *   <li>P4: (Invalid) User not found - throws NoSuchElementException</li>
 *   <li>P5: (Edge) Logger disabled - behavior unchanged</li>
 * </ul>
 *
 * <h3>25. GET /users/{userId}/budget-report (JSON)</h3>
 * <ul>
 *   <li>P1: (Valid) User exists - returns 200 OK with report</li>
 *   <li>P2: (Valid/Edge) Empty report - returns 200 OK with empty map</li>
 *   <li>P3: (Invalid) User not found - throws NoSuchElementException</li>
 *   <li>P4: (Edge) Logger disabled - behavior unchanged</li>
 * </ul>
 *
 * <h3>26. Exception Handlers</h3>
 * <ul>
 *   <li>handleNotFound: NoSuchElementException -> 404 NOT_FOUND with error message</li>
 *   <li>handleBadRequest: IllegalArgumentException -> 400 BAD_REQUEST with error message</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
public class RouteControllerTests {

  /**
   * A mock implementation of the service layer.
   */
  @Mock
  private MockApiService mockApiService;

  /**
   * The instance of the controller under test, with mocks injected.
   */
  @InjectMocks
  private RouteController routeController;

  /**
   * Set up logic before each test.
   */
  @BeforeEach
  public void setUp() {
    // Initialization code, if needed
  }

  /**
   * Tear down logic after each test.
   */
  @AfterEach
  public void tearDown() {
    // Cleanup code, if needed
  }

  /**
   * Helper method to temporarily set the log level for the controller's logger.
   *
   * @param level The new level (e.g., Level.OFF)
   * @return The original Level so it can be restored
   */
  private Level setLogLevel(Level level) {
    Logger logger = Logger.getLogger(RouteController.class.getName());
    Level original = logger.getLevel();
    logger.setLevel(level);
    return original;
  }

  // ===========================================================================
  // Tests for index (GET / or /index)
  // ===========================================================================

  /**
   * Tests GET /index when users exist in the database.
   *
   * <p>Partition: P1 (Valid) - Users exist.
   */
  @Test
  public void index_usersExist_returnsHtmlListOfUsers() {
    List<User> users = new ArrayList<>();
    users.add(new User("Alice", "alice@example.com", 500.0));
    users.add(new User("Bob", "bob@example.com", 300.0));
    when(mockApiService.viewAllUsers()).thenReturn(users);

    ResponseEntity<String> response = routeController.index();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(MediaType.TEXT_HTML, response.getHeaders().getContentType());
    assertTrue(response.getBody().contains("Alice"));
    assertTrue(response.getBody().contains("Bob"));
    assertTrue(response.getBody().contains("Welcome to the Personal Finance Tracker"));
  }

  /**
   * Tests GET /index when no users exist.
   *
   * <p>Partition: P2 (Valid/Boundary) - No users.
   */
  @Test
  public void index_noUsers_returnsNoUsersMessage() {
    when(mockApiService.viewAllUsers()).thenReturn(new ArrayList<>());

    ResponseEntity<String> response = routeController.index();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().contains("No users found."));
  }

  /**
   * Tests GET /index when service throws exception.
   *
   * <p>Partition: P3 (Invalid) - Service throws exception.
   */
  @Test
  public void index_serviceThrowsException_throwsRuntimeException() {
    when(mockApiService.viewAllUsers()).thenThrow(new RuntimeException("Database unavailable"));

    RuntimeException thrown = assertThrows(RuntimeException.class, () -> routeController.index());

    assertEquals("Database unavailable", thrown.getMessage());
  }

  /**
   * Tests GET /index with logger disabled for branch coverage.
   *
   * <p>Partition: P4 (Edge) - Logger disabled.
   */
  @Test
  public void index_loggerOff_stillReturnsHtml() {
    Level original = setLogLevel(Level.OFF);
    try {
      when(mockApiService.viewAllUsers()).thenReturn(List.of());
      ResponseEntity<String> response = routeController.index();
      assertEquals(HttpStatus.OK, response.getStatusCode());
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for getAllUsers (GET /users)
  // ===========================================================================

  /**
   * Tests GET /users returns the complete list from service.
   *
   * <p>Partition: P1 (Valid) - Returns list of users.
   */
  @Test
  public void getAllUsers_validCall_returnsListFromService() {
    List<User> users = List.of(
        new User("Alice", "alice@example.com", 1000.0),
        new User("Bob", "bob@example.com", 800.0));
    when(mockApiService.viewAllUsers()).thenReturn(users);

    List<User> result = routeController.getAllUsers();

    assertEquals(users, result);
  }

  /**
   * Tests GET /users with logger disabled.
   *
   * <p>Partition: P2 (Edge) - Logger disabled.
   */
  @Test
  public void getAllUsers_loggerOff_stillReturnsList() {
    Level original = setLogLevel(Level.OFF);
    try {
      List<User> users = List.of(new User("Alice", "a@a.com", 100));
      when(mockApiService.viewAllUsers()).thenReturn(users);
      List<User> result = routeController.getAllUsers();
      assertEquals(users, result);
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for getUser (GET /users/{userId})
  // ===========================================================================

  /**
   * Tests GET /users/{userId} for an existing user.
   *
   * <p>Partition: P1 (Valid) - User exists.
   */
  @Test
  public void getUser_existingUser_returnsUserWith200() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1200.0);
    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));

    ResponseEntity<User> response = routeController.getUser(userId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(user, response.getBody());
  }

  /**
   * Tests GET /users/{userId} for a user with minimal fields.
   *
   * <p>Partition: P2 (Valid/Edge) - Minimal fields.
   */
  @Test
  public void getUser_minimalUserFields_returnsUserWith200() {
    UUID userId = UUID.randomUUID();
    User user = new User("X", "", 0.0);
    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));

    ResponseEntity<User> response = routeController.getUser(userId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("X", response.getBody().getUsername());
  }

  /**
   * Tests GET /users/{userId} for a non-existent user.
   *
   * <p>Partition: P3 (Invalid) - User not found.
   */
  @Test
  public void getUser_nonexistentUser_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.getUser(userId));

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  /**
   * Tests GET /users/{userId} with logger disabled (existing user path).
   *
   * <p>Partition: P4 (Edge) - Logger disabled.
   */
  @Test
  public void getUser_loggerOff_existingUser_returnsUserWith200() {
    Level original = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1200.0);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      ResponseEntity<User> response = routeController.getUser(userId);
      assertEquals(HttpStatus.OK, response.getStatusCode());
    } finally {
      setLogLevel(original);
    }
  }

  /**
   * Tests GET /users/{userId} with logger disabled (not found path).
   *
   * <p>Partition: P4 (Edge) - Logger disabled, not found.
   */
  @Test
  public void getUser_loggerOff_nonexistentUser_throwsNoSuchElementException() {
    Level original = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      when(mockApiService.getUser(userId)).thenReturn(Optional.empty());
      assertThrows(NoSuchElementException.class, () -> routeController.getUser(userId));
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for createUserJson (POST /users)
  // ===========================================================================

  /**
   * Tests POST /users with a valid user.
   *
   * <p>Partition: P1 (Valid) - Valid user.
   */
  @Test
  public void createUserJson_validUser_returnsCreatedUserWith201() {
    User user = new User("Alice", "alice@example.com", 1500.0);
    when(mockApiService.addUser(user)).thenReturn(user);

    ResponseEntity<User> response = routeController.createUserJson(user);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(user, response.getBody());
  }

  /**
   * Tests POST /users with empty email string (not null).
   *
   * <p>Partition: P2 (Valid/Edge) - Empty email string.
   */
  @Test
  public void createUserJson_emptyEmailString_returnsCreatedUserWith201() {
    User user = new User("Bob", "", 1000.0);
    when(mockApiService.addUser(user)).thenReturn(user);

    ResponseEntity<User> response = routeController.createUserJson(user);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
  }

  /**
   * Tests POST /users with null username.
   *
   * <p>Partition: P3 (Invalid) - Null username.
   */
  @Test
  public void createUserJson_nullUsername_throwsRuntimeException() {
    User user = new User(null, "alice@example.com", 1000.0);

    RuntimeException ex = assertThrows(
        RuntimeException.class,
        () -> routeController.createUserJson(user));

    assertEquals("Failed to create user", ex.getMessage());
    assertTrue(ex.getCause() instanceof IllegalArgumentException);
    assertEquals("Username field is required", ex.getCause().getMessage());
  }

  /**
   * Tests POST /users with null email.
   *
   * <p>Partition: P4 (Invalid) - Null email.
   */
  @Test
  public void createUserJson_nullEmail_throwsRuntimeException() {
    User user = new User("Alice", null, 1000.0);

    RuntimeException ex = assertThrows(
        RuntimeException.class,
        () -> routeController.createUserJson(user));

    assertEquals("Failed to create user", ex.getMessage());
    assertTrue(ex.getCause() instanceof IllegalArgumentException);
    assertEquals("Email field is required", ex.getCause().getMessage());
  }

  /**
   * Tests POST /users with duplicate email.
   *
   * <p>Partition: P5 (Invalid) - Duplicate email.
   */
  @Test
  public void createUserJson_duplicateEmail_throwsIllegalArgumentException() {
    User user = new User("Alice", "alice@example.com", 1000.0);
    DataIntegrityViolationException ex = new DataIntegrityViolationException(
        "constraint violation", new RuntimeException("users_email_key"));
    when(mockApiService.addUser(user)).thenThrow(ex);

    IllegalArgumentException thrown = assertThrows(
        IllegalArgumentException.class,
        () -> routeController.createUserJson(user));

    assertEquals("Email already exists: alice@example.com", thrown.getMessage());
  }

  /**
   * Tests POST /users with duplicate username.
   *
   * <p>Partition: P6 (Invalid) - Duplicate username.
   */
  @Test
  public void createUserJson_duplicateUsername_throwsIllegalArgumentException() {
    User user = new User("Alice", "alice@example.com", 1000.0);
    DataIntegrityViolationException ex = new DataIntegrityViolationException(
        "constraint violation", new RuntimeException("users_username_key"));
    when(mockApiService.addUser(user)).thenThrow(ex);

    IllegalArgumentException thrown = assertThrows(
        IllegalArgumentException.class,
        () -> routeController.createUserJson(user));

    assertEquals("Username already exists: Alice", thrown.getMessage());
  }

  /**
   * Tests POST /users with generic data integrity violation.
   *
   * <p>Partition: P7 (Invalid) - Generic data integrity violation.
   */
  @Test
  public void createUserJson_genericDataIntegrityViolation_throwsIllegalArgumentException() {
    User user = new User("Alice", "alice@example.com", 1000.0);
    DataIntegrityViolationException ex = new DataIntegrityViolationException(
        "constraint violation", new RuntimeException("some_other_constraint"));
    when(mockApiService.addUser(user)).thenThrow(ex);

    IllegalArgumentException thrown = assertThrows(
        IllegalArgumentException.class,
        () -> routeController.createUserJson(user));

    assertEquals("Data integrity violation", thrown.getMessage());
  }

  /**
   * Tests POST /users when service throws generic exception.
   *
   * <p>Partition: P8 (Invalid) - Service throws exception.
   */
  @Test
  public void createUserJson_serviceThrowsException_throwsIllegalStateException() {
    User user = new User("Charlie", "charlie@example.com", 800.0);
    when(mockApiService.addUser(user)).thenThrow(new RuntimeException("Database error"));

    RuntimeException thrown = assertThrows(
        RuntimeException.class,
        () -> routeController.createUserJson(user));

    assertEquals("Failed to create user", thrown.getMessage());
  }

  /**
   * Tests POST /users with logger disabled.
   *
   * <p>Partition: P9 (Edge) - Logger disabled.
   */
  @Test
  public void createUserJson_loggerOff_validUser_returnsCreatedUserWith201() {
    Level original = setLogLevel(Level.OFF);
    try {
      User user = new User("Alice", "alice@example.com", 1500.0);
      when(mockApiService.addUser(user)).thenReturn(user);
      ResponseEntity<User> response = routeController.createUserJson(user);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
    } finally {
      setLogLevel(original);
    }
  }

  /**
   * Tests POST /users with logger disabled (duplicate email path).
   *
   * <p>Partition: P9 (Edge) - Logger disabled, duplicate email.
   */
  @Test
  public void createUserJson_loggerOff_duplicateEmail_throwsIllegalArgumentException() {
    Level original = setLogLevel(Level.OFF);
    try {
      User user = new User("Alice", "alice@example.com", 1000.0);
      DataIntegrityViolationException ex = new DataIntegrityViolationException(
          "constraint violation", new RuntimeException("users_email_key"));
      when(mockApiService.addUser(user)).thenThrow(ex);
      assertThrows(IllegalArgumentException.class, () -> routeController.createUserJson(user));
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for createUserFromFormHtml (POST /users/form)
  // ===========================================================================

  /**
   * Tests POST /users/form with valid form data.
   *
   * <p>Partition: P1 (Valid) - Valid form data.
   */
  @Test
  public void createUserFromFormHtml_validForm_returnsHtmlSuccessWith201() {
    User saved = new User("Alice", "alice@example.com", 1500.0);
    when(mockApiService.addUser(any(User.class))).thenReturn(saved);

    ResponseEntity<String> response =
        routeController.createUserFromFormHtml("Alice", "alice@example.com", 1500.0);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertTrue(response.getBody().contains("User Created Successfully"));
  }

  /**
   * Tests POST /users/form with zero budget.
   *
   * <p>Partition: P2 (Valid/Boundary) - Zero budget.
   */
  @Test
  public void createUserFromFormHtml_zeroBudget_returnsHtmlSuccessWith201() {
    User saved = new User("Bob", "bob@example.com", 0.0);
    when(mockApiService.addUser(any(User.class))).thenReturn(saved);

    ResponseEntity<String> response =
        routeController.createUserFromFormHtml("Bob", "bob@example.com", 0.0);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertTrue(response.getBody().contains("$0.00"));
  }

  /**
   * Tests POST /users/form with duplicate username.
   *
   * <p>Partition: P3 (Invalid) - Duplicate username.
   */
  @Test
  public void createUserFromFormHtml_duplicateUsername_returns400() {
    when(mockApiService.isUsernameExists("Alice", null)).thenReturn(true);

    ResponseEntity<String> response =
        routeController.createUserFromFormHtml("Alice", "alice@example.com", 500.0);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().contains("Username already in use"));
  }

  /**
   * Tests POST /users/form with duplicate email.
   *
   * <p>Partition: P4 (Invalid) - Duplicate email.
   */
  @Test
  public void createUserFromFormHtml_duplicateEmail_returns400() {
    when(mockApiService.isUsernameExists("Alice", null)).thenReturn(false);
    when(mockApiService.isEmailExists("alice@example.com", null)).thenReturn(true);

    ResponseEntity<String> response =
        routeController.createUserFromFormHtml("Alice", "alice@example.com", 500.0);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().contains("User email already in use"));
  }

  /**
   * Tests POST /users/form when service throws exception.
   *
   * <p>Partition: P5 (Invalid) - Service throws exception.
   */
  @Test
  public void createUserFromFormHtml_serviceThrowsException_throwsRuntimeException() {
    when(mockApiService.addUser(any(User.class))).thenThrow(new RuntimeException("Save failed"));

    RuntimeException thrown = assertThrows(
        RuntimeException.class,
        () -> routeController.createUserFromFormHtml("Eve", "eve@example.com", 100.0));

    assertEquals("Save failed", thrown.getMessage());
  }

  /**
   * Tests POST /users/form with logger disabled.
   *
   * <p>Partition: P6 (Edge) - Logger disabled.
   */
  @Test
  public void createUserFromFormHtml_loggerOff_validForm_returnsHtmlSuccessWith201() {
    Level original = setLogLevel(Level.OFF);
    try {
      User saved = new User("Alice", "alice@example.com", 1500.0);
      when(mockApiService.addUser(any(User.class))).thenReturn(saved);
      ResponseEntity<String> response =
          routeController.createUserFromFormHtml("Alice", "alice@example.com", 1500.0);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for updateUserJson (PUT /users/{userId})
  // ===========================================================================

  /**
   * Tests PUT /users/{userId} with valid update.
   *
   * <p>Partition: P1 (Valid) - User exists, all fields updated.
   */
  @Test
  public void updateUserJson_existingUser_returnsUpdatedUserWith200() {
    UUID userId = UUID.randomUUID();
    User existing = new User("Alice", "alice@example.com", 1000.0);
    User updated = new User("AliceNew", "alice_new@example.com", 1200.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(existing));
    when(mockApiService.deleteUser(userId)).thenReturn(true);
    when(mockApiService.addUser(any(User.class))).thenReturn(updated);

    ResponseEntity<User> response = routeController.updateUserJson(userId, updated);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  /**
   * Tests PUT /users/{userId} with zero budget boundary.
   *
   * <p>Partition: P2 (Valid/Boundary) - Zero budget.
   */
  @Test
  public void updateUserJson_zeroBudget_returnsUpdatedUserWith200() {
    UUID userId = UUID.randomUUID();
    User existing = new User("Alice", "alice@example.com", 1000.0);
    User updated = new User("Alice", "alice@example.com", 0.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(existing));
    when(mockApiService.deleteUser(userId)).thenReturn(true);
    when(mockApiService.addUser(any(User.class))).thenReturn(updated);

    ResponseEntity<User> response = routeController.updateUserJson(userId, updated);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  /**
   * Tests PUT /users/{userId} with null/empty fields (uses existing values).
   *
   * <p>Partition: P3 (Valid/Edge) - Null/empty fields.
   */
  @Test
  public void updateUserJson_nullFields_usesExistingValues() {
    UUID userId = UUID.randomUUID();
    User existing = new User("Alice", "alice@example.com", 1000.0);
    User updates = new User(null, null, 0.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(existing));
    when(mockApiService.deleteUser(userId)).thenReturn(true);
    when(mockApiService.addUser(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    ResponseEntity<User> response = routeController.updateUserJson(userId, updates);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("Alice", response.getBody().getUsername());
  }

  /**
   * Tests PUT /users/{userId} when user not found.
   *
   * <p>Partition: P4 (Invalid) - User not found.
   */
  @Test
  public void updateUserJson_userNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    User updates = new User("Eve", "eve@example.com", 900.0);
    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.updateUserJson(userId, updates));

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  /**
   * Tests PUT /users/{userId} with duplicate username.
   *
   * <p>Partition: P5 (Invalid) - Duplicate username.
   */
  @Test
  public void updateUserJson_duplicateUsername_throwsIllegalArgumentException() {
    UUID userId = UUID.randomUUID();
    User existing = new User("Alice", "alice@example.com", 1000.0);
    User updates = new User("TakenName", "alice@example.com", 1000.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(existing));
    when(mockApiService.isUsernameExists("TakenName", userId)).thenReturn(true);

    IllegalArgumentException thrown = assertThrows(
        IllegalArgumentException.class,
        () -> routeController.updateUserJson(userId, updates));

    assertTrue(thrown.getMessage().contains("Username already exists"));
  }

  /**
   * Tests PUT /users/{userId} with duplicate email.
   *
   * <p>Partition: P6 (Invalid) - Duplicate email.
   */
  @Test
  public void updateUserJson_duplicateEmail_throwsIllegalArgumentException() {
    UUID userId = UUID.randomUUID();
    User existing = new User("Alice", "alice@example.com", 1000.0);
    User updates = new User("Alice", "taken@example.com", 1000.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(existing));
    when(mockApiService.isUsernameExists("Alice", userId)).thenReturn(false);
    when(mockApiService.isEmailExists("taken@example.com", userId)).thenReturn(true);

    IllegalArgumentException thrown = assertThrows(
        IllegalArgumentException.class,
        () -> routeController.updateUserJson(userId, updates));

    assertTrue(thrown.getMessage().contains("Email already exists"));
  }

  /**
   * Tests PUT /users/{userId} with logger disabled.
   *
   * <p>Partition: P7 (Edge) - Logger disabled.
   */
  @Test
  public void updateUserJson_loggerOff_existingUser_returnsUpdatedUserWith200() {
    Level original = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User existing = new User("Alice", "alice@example.com", 1000.0);
      User updated = new User("Alice", "new@example.com", 1200.0);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(existing));
      when(mockApiService.addUser(any(User.class))).thenReturn(updated);
      ResponseEntity<User> response = routeController.updateUserJson(userId, updated);
      assertEquals(HttpStatus.OK, response.getStatusCode());
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for updateUserFromFormHtml (POST /users/{userId}/update-form)
  // ===========================================================================

  /**
   * Tests POST /users/{userId}/update-form with valid data.
   *
   * <p>Partition: P1 (Valid) - User exists.
   */
  @Test
  public void updateUserFromFormHtml_validData_returnsHtmlSuccessWith200() {
    UUID userId = UUID.randomUUID();
    User existing = new User("Alice", "alice@example.com", 1000.0);
    User saved = new User("Alice", "alice_new@example.com", 1200.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(existing));
    when(mockApiService.deleteUser(userId)).thenReturn(true);
    when(mockApiService.addUser(any(User.class))).thenReturn(saved);

    ResponseEntity<String> response =
        routeController.updateUserFromFormHtml(userId, "Alice", "alice_new@example.com", 1200.0);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().contains("User Updated Successfully"));
  }

  /**
   * Tests POST /users/{userId}/update-form when user not found.
   *
   * <p>Partition: P2 (Invalid) - User not found.
   */
  @Test
  public void updateUserFromFormHtml_userNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.updateUserFromFormHtml(userId, "Eve", "eve@example.com", 100.0));

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  /**
   * Tests POST /users/{userId}/update-form with duplicate username.
   *
   * <p>Partition: P3 (Invalid) - Duplicate username.
   */
  @Test
  public void updateUserFromFormHtml_duplicateUsername_returns400() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.isUsernameExists("TakenName", userId)).thenReturn(true);

    ResponseEntity<String> response =
        routeController.updateUserFromFormHtml(userId, "TakenName", "new@example.com", 100.0);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().contains("Username already in use"));
  }

  /**
   * Tests POST /users/{userId}/update-form with duplicate email.
   *
   * <p>Partition: P4 (Invalid) - Duplicate email.
   */
  @Test
  public void updateUserFromFormHtml_duplicateEmail_returns400() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.isUsernameExists("NewName", userId)).thenReturn(false);
    when(mockApiService.isEmailExists("taken@example.com", userId)).thenReturn(true);

    ResponseEntity<String> response =
        routeController.updateUserFromFormHtml(userId, "NewName", "taken@example.com", 100.0);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().contains("User email already in use"));
  }

  /**
   * Tests POST /users/{userId}/update-form with logger disabled.
   *
   * <p>Partition: P5 (Edge) - Logger disabled.
   */
  @Test
  public void updateUserFromFormHtml_loggerOff_existingUser_returnsHtmlSuccessWith200() {
    Level original = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User existing = new User("Alice", "alice@example.com", 1000.0);
      User saved = new User("Alice", "alice_new@example.com", 1200.0);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(existing));
      when(mockApiService.addUser(any(User.class))).thenReturn(saved);
      ResponseEntity<String> response =
          routeController.updateUserFromFormHtml(userId, "Alice", "alice_new@example.com", 1200.0);
      assertEquals(HttpStatus.OK, response.getStatusCode());
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for showCreateUserForm (GET /users/create-form)
  // ===========================================================================

  /**
   * Tests GET /users/create-form returns HTML form.
   *
   * <p>Partition: P1 (Valid) - Returns HTML form.
   */
  @Test
  public void showCreateUserForm_returnsHtmlForm() {
    String html = routeController.showCreateUserForm();

    assertTrue(html.contains("<h2>Create New User</h2>"));
    assertTrue(html.contains("<form"));
    assertTrue(html.contains("name='username'"));
    assertTrue(html.contains("name='email'"));
    assertTrue(html.contains("name='budget'"));
  }

  /**
   * Tests GET /users/create-form with logger disabled.
   *
   * <p>Partition: P2 (Edge) - Logger disabled.
   */
  @Test
  public void showCreateUserForm_loggerOff_returnsExpectedHtmlForm() {
    Level original = setLogLevel(Level.OFF);
    try {
      String html = routeController.showCreateUserForm();
      assertTrue(html.contains("<h2>Create New User</h2>"));
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for showEditUserForm (GET /users/{userId}/edit-form)
  // ===========================================================================

  /**
   * Tests GET /users/{userId}/edit-form for existing user.
   *
   * <p>Partition: P1 (Valid) - User exists.
   */
  @Test
  public void showEditUserForm_existingUser_returnsPopulatedFormHtml() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));

    String html = routeController.showEditUserForm(userId);

    assertTrue(html.contains("<h2>Edit User</h2>"));
    assertTrue(html.contains("Alice"));
    assertTrue(html.contains("alice@example.com"));
  }

  /**
   * Tests GET /users/{userId}/edit-form when user not found.
   *
   * <p>Partition: P2 (Invalid) - User not found.
   */
  @Test
  public void showEditUserForm_userNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.showEditUserForm(userId));

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  /**
   * Tests GET /users/{userId}/edit-form with logger disabled.
   *
   * <p>Partition: P3 (Edge) - Logger disabled.
   */
  @Test
  public void showEditUserForm_loggerOff_existingUser_returnsPopulatedFormHtml() {
    Level original = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.0);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      String html = routeController.showEditUserForm(userId);
      assertTrue(html.contains("<h2>Edit User</h2>"));
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for deleteUser (DELETE /users/{userId})
  // ===========================================================================

  /**
   * Tests DELETE /users/{userId} for existing user.
   *
   * <p>Partition: P1 (Valid) - User exists.
   */
  @Test
  public void deleteUser_existingUser_returnsConfirmationMapWith200() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.deleteUser(userId)).thenReturn(true);

    ResponseEntity<Map<String, Object>> response = routeController.deleteUser(userId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue((Boolean) response.getBody().get("deleted"));
    assertEquals(userId, response.getBody().get("userId"));
  }

  /**
   * Tests DELETE /users/{userId} when delete fails.
   *
   * <p>Partition: P2 (Invalid) - Delete fails.
   */
  @Test
  public void deleteUser_deleteFails_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.deleteUser(userId)).thenReturn(false);

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.deleteUser(userId));

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  /**
   * Tests DELETE /users/{userId} with logger disabled.
   *
   * <p>Partition: P3 (Edge) - Logger disabled.
   */
  @Test
  public void deleteUser_loggerOff_existingUser_returnsConfirmationMapWith200() {
    Level original = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      when(mockApiService.deleteUser(userId)).thenReturn(true);
      ResponseEntity<Map<String, Object>> response = routeController.deleteUser(userId);
      assertEquals(HttpStatus.OK, response.getStatusCode());
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for deleteUserViaGet (GET /deleteuser/{userId})
  // ===========================================================================

  /**
   * Tests GET /deleteuser/{userId} for existing user.
   *
   * <p>Partition: P1 (Valid) - User exists.
   */
  @Test
  public void deleteUserViaGet_existingUser_returnsSuccessMessage() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.deleteUser(userId)).thenReturn(true);

    String result = routeController.deleteUserViaGet(userId);

    assertEquals("User deleted successfully", result);
  }

  /**
   * Tests GET /deleteuser/{userId} when delete fails.
   *
   * <p>Partition: P2 (Invalid) - Delete fails.
   */
  @Test
  public void deleteUserViaGet_deleteFails_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.deleteUser(userId)).thenReturn(false);

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.deleteUserViaGet(userId));

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  /**
   * Tests GET /deleteuser/{userId} with logger disabled.
   *
   * <p>Partition: P3 (Edge) - Logger disabled.
   */
  @Test
  public void deleteUserViaGet_loggerOff_existingUser_returnsSuccessMessage() {
    Level original = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      when(mockApiService.deleteUser(userId)).thenReturn(true);
      String result = routeController.deleteUserViaGet(userId);
      assertEquals("User deleted successfully", result);
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for getUserTransactions (GET /users/{userId}/transactions)
  // ===========================================================================

  /**
   * Tests GET /users/{userId}/transactions for user with transactions.
   *
   * <p>Partition: P1 (Valid) - User exists with transactions.
   */
  @Test
  public void getUserTransactions_userWithTransactions_returnsList() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    List<Transaction> transactions = List.of(
        new Transaction(userId, 50.0, "FOOD", "Lunch"),
        new Transaction(userId, 100.0, "SHOPPING", "Shoes"));

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransactionsByUser(userId)).thenReturn(transactions);

    ResponseEntity<?> response = routeController.getUserTransactions(userId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(2, ((List<?>) response.getBody()).size());
  }

  /**
   * Tests GET /users/{userId}/transactions for user with no transactions.
   *
   * <p>Partition: P2 (Valid/Boundary) - No transactions.
   */
  @Test
  public void getUserTransactions_noTransactions_returnsEmptyList() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransactionsByUser(userId)).thenReturn(new ArrayList<>());

    ResponseEntity<?> response = routeController.getUserTransactions(userId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(((List<?>) response.getBody()).isEmpty());
  }

  /**
   * Tests GET /users/{userId}/transactions when user not found.
   *
   * <p>Partition: P3 (Invalid) - User not found.
   */
  @Test
  public void getUserTransactions_userNotFound_returns404() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    ResponseEntity<?> response = routeController.getUserTransactions(userId);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  /**
   * Tests GET /users/{userId}/transactions when service throws exception.
   *
   * <p>Partition: P4 (Invalid) - Service throws exception.
   */
  @Test
  public void getUserTransactions_serviceThrowsException_returns500() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransactionsByUser(userId))
        .thenThrow(new RuntimeException("Database error"));

    ResponseEntity<?> response = routeController.getUserTransactions(userId);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  /**
   * Tests GET /users/{userId}/transactions with logger disabled.
   *
   * <p>Partition: P5 (Edge) - Logger disabled.
   */
  @Test
  public void getUserTransactions_loggerOff_existingUser_returnsTransactionsWith200() {
    Level original = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.0);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.getTransactionsByUser(userId)).thenReturn(new ArrayList<>());
      ResponseEntity<?> response = routeController.getUserTransactions(userId);
      assertEquals(HttpStatus.OK, response.getStatusCode());
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for getTransaction (GET /users/{userId}/transactions/{transactionId})
  // ===========================================================================

  /**
   * Tests GET .../transactions/{txId} for valid user and transaction.
   *
   * <p>Partition: P1 (Valid) - User and transaction exist, transaction belongs to user.
   */
  @Test
  public void getTransaction_validUserAndTransaction_returnsTransactionWith200() {
    UUID userId = UUID.randomUUID();
    UUID txId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    Transaction tx = new Transaction(userId, 50.0, "FOOD", "Lunch");
    tx.setTransactionId(txId);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(txId)).thenReturn(Optional.of(tx));

    ResponseEntity<Transaction> response = routeController.getTransaction(userId, txId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(tx, response.getBody());
  }

  /**
   * Tests GET .../transactions/{txId} when user not found.
   *
   * <p>Partition: P2 (Invalid) - User not found.
   */
  @Test
  public void getTransaction_userNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    UUID txId = UUID.randomUUID();

    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.getTransaction(userId, txId));

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  /**
   * Tests GET .../transactions/{txId} when transaction not found.
   *
   * <p>Partition: P3 (Invalid) - Transaction not found.
   */
  @Test
  public void getTransaction_transactionNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    UUID txId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(txId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.getTransaction(userId, txId));

    assertTrue(thrown.getMessage().contains("Transaction " + txId + " not found"));
  }

  /**
   * Tests GET .../transactions/{txId} when transaction belongs to different user.
   *
   * <p>Partition: P4 (Invalid) - Transaction belongs to different user.
   */
  @Test
  public void getTransaction_transactionBelongsToDifferentUser_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    UUID txId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    Transaction tx = new Transaction(otherUserId, 50.0, "FOOD", "Lunch");
    tx.setTransactionId(txId);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(txId)).thenReturn(Optional.of(tx));

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.getTransaction(userId, txId));

    assertTrue(thrown.getMessage().contains("Transaction " + txId + " not found for user"));
  }

  /**
   * Tests GET .../transactions/{txId} with logger disabled.
   *
   * <p>Partition: P5 (Edge) - Logger disabled.
   */
  @Test
  public void getTransaction_loggerOff_existingUserAndTransaction_returnsTransactionWith200() {
    Level original = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      UUID txId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.0);
      Transaction tx = new Transaction(userId, 50.0, "FOOD", "Lunch");
      tx.setTransactionId(txId);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.getTransaction(txId)).thenReturn(Optional.of(tx));
      ResponseEntity<Transaction> response = routeController.getTransaction(userId, txId);
      assertEquals(HttpStatus.OK, response.getStatusCode());
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for createTransactionJson (POST /users/{userId}/transactions)
  // ===========================================================================

  /**
   * Tests POST .../transactions with valid transaction.
   *
   * <p>Partition: P1 (Valid) - Valid transaction.
   */
  @Test
  public void createTransactionJson_validTransaction_returnsCreatedWith201() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    Transaction tx = new Transaction(userId, 50.0, "FOOD", "Lunch");
    UUID txId = UUID.randomUUID();
    tx.setTransactionId(txId);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.addTransaction(any(Transaction.class))).thenReturn(tx);

    ResponseEntity<Transaction> response = routeController.createTransactionJson(userId, tx);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(txId, response.getBody().getTransactionId());
  }

  /**
   * Tests POST .../transactions when user not found.
   *
   * <p>Partition: P2 (Invalid) - User not found.
   */
  @Test
  public void createTransactionJson_userNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    Transaction tx = new Transaction(userId, 50.0, "FOOD", "Lunch");

    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.createTransactionJson(userId, tx));

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  /**
   * Tests POST .../transactions when service throws exception.
   *
   * <p>Partition: P3 (Invalid) - Service throws exception.
   */
  @Test
  public void createTransactionJson_serviceThrowsException_propagatesException() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    Transaction tx = new Transaction(userId, 50.0, "FOOD", "Lunch");

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.addTransaction(any(Transaction.class)))
        .thenThrow(new IllegalArgumentException("Invalid category"));

    IllegalArgumentException thrown = assertThrows(
        IllegalArgumentException.class,
        () -> routeController.createTransactionJson(userId, tx));

    assertEquals("Invalid category", thrown.getMessage());
  }

  /**
   * Tests POST .../transactions with logger disabled.
   *
   * <p>Partition: P4 (Edge) - Logger disabled.
   */
  @Test
  public void createTransactionJson_loggerOff_validTransaction_returnsCreatedWith201() {
    Level original = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.0);
      Transaction tx = new Transaction(userId, 50.0, "FOOD", "Lunch");
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.addTransaction(any(Transaction.class))).thenReturn(tx);
      ResponseEntity<Transaction> response = routeController.createTransactionJson(userId, tx);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for createTransactionFromFormHtml (POST /users/{userId}/transactions/form)
  // ===========================================================================

  /**
   * Tests POST .../transactions/form with valid form data.
   *
   * <p>Partition: P1 (Valid) - Valid form data.
   */
  @Test
  public void createTransactionFromFormHtml_validForm_returnsCreatedWith201() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    Transaction saved = new Transaction(userId, 50.0, "FOOD", "Lunch");

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.addTransaction(any(Transaction.class))).thenReturn(saved);

    ResponseEntity<String> response =
        routeController.createTransactionFromFormHtml(userId, "Lunch", 50.0, "FOOD");

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertTrue(response.getBody().contains("Transaction Created Successfully"));
  }

  /**
   * Tests POST .../transactions/form when user not found.
   *
   * <p>Partition: P2 (Invalid) - User not found.
   */
  @Test
  public void createTransactionFromFormHtml_userNotFound_returns404() {
    UUID userId = UUID.randomUUID();

    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    ResponseEntity<String> response =
        routeController.createTransactionFromFormHtml(userId, "Lunch", 50.0, "FOOD");

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  /**
   * Tests POST .../transactions/form when service throws exception.
   *
   * <p>Partition: P3 (Invalid) - Service throws exception.
   */
  @Test
  public void createTransactionFromFormHtml_serviceThrowsException_returns500() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.addTransaction(any(Transaction.class)))
        .thenThrow(new RuntimeException("Database error"));

    ResponseEntity<String> response =
        routeController.createTransactionFromFormHtml(userId, "Lunch", 50.0, "FOOD");

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  /**
   * Tests POST .../transactions/form with logger disabled.
   *
   * <p>Partition: P4 (Edge) - Logger disabled.
   */
  @Test
  public void createTransactionFromFormHtml_loggerOff_validForm_returnsCreatedWith201() {
    Level original = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.0);
      Transaction saved = new Transaction(userId, 50.0, "FOOD", "Lunch");
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.addTransaction(any(Transaction.class))).thenReturn(saved);
      ResponseEntity<String> response =
          routeController.createTransactionFromFormHtml(userId, "Lunch", 50.0, "FOOD");
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for updateTransactionJson (PUT /users/{userId}/transactions/{txId})
  // ===========================================================================

  /**
   * Tests PUT .../transactions/{txId} with valid update.
   *
   * <p>Partition: P1 (Valid) - Valid update.
   */
  @Test
  public void updateTransactionJson_validUpdate_returnsUpdatedTransactionWith200() {
    UUID userId = UUID.randomUUID();
    UUID txId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    Transaction existing = new Transaction(userId, 100.0, "FOOD", "Lunch");
    existing.setTransactionId(txId);
    Transaction updated = new Transaction(userId, 120.0, "FOOD", "Dinner");
    updated.setTransactionId(txId);

    Map<String, Object> updates = Map.of("amount", 120.0, "description", "Dinner");

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(txId)).thenReturn(Optional.of(existing));
    when(mockApiService.updateTransaction(txId, updates)).thenReturn(Optional.of(updated));

    ResponseEntity<Transaction> response =
        routeController.updateTransactionJson(userId, txId, updates);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(120.0, response.getBody().getAmount());
  }

  /**
   * Tests PUT .../transactions/{txId} when user not found.
   *
   * <p>Partition: P2 (Invalid) - User not found.
   */
  @Test
  public void updateTransactionJson_userNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    UUID txId = UUID.randomUUID();
    Map<String, Object> updates = Map.of("amount", 100.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.updateTransactionJson(userId, txId, updates));

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  /**
   * Tests PUT .../transactions/{txId} when transaction not found.
   *
   * <p>Partition: P3 (Invalid) - Transaction not found.
   */
  @Test
  public void updateTransactionJson_transactionNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    UUID txId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    Map<String, Object> updates = Map.of("amount", 100.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(txId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.updateTransactionJson(userId, txId, updates));

    assertTrue(thrown.getMessage().contains("Transaction " + txId + " not found"));
  }

  /**
   * Tests PUT .../transactions/{txId} when transaction belongs to different user.
   *
   * <p>Partition: P4 (Invalid) - Transaction belongs to different user.
   */
  @Test
  public void
      updateTransactionJson_transactionBelongsToDifferentUser_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    UUID txId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    Transaction tx = new Transaction(otherUserId, 50.0, "FOOD", "Lunch");
    tx.setTransactionId(txId);
    Map<String, Object> updates = Map.of("amount", 100.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(txId)).thenReturn(Optional.of(tx));

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.updateTransactionJson(userId, txId, updates));

    assertTrue(thrown.getMessage().contains("Transaction " + txId + " not found for user"));
  }

  /**
   * Tests PUT .../transactions/{txId} when update returns empty.
   *
   * <p>Partition: P5 (Invalid) - Update returns empty.
   */
  @Test
  public void updateTransactionJson_updateReturnsEmpty_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    UUID txId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    Transaction existing = new Transaction(userId, 100.0, "FOOD", "Lunch");
    existing.setTransactionId(txId);
    Map<String, Object> updates = Map.of("amount", 120.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(txId)).thenReturn(Optional.of(existing));
    when(mockApiService.updateTransaction(txId, updates)).thenReturn(Optional.empty());

    assertThrows(NoSuchElementException.class,
        () -> routeController.updateTransactionJson(userId, txId, updates));
  }

  /**
   * Tests PUT .../transactions/{txId} when service throws exception.
   *
   * <p>Partition: P6 (Invalid) - Service throws exception.
   */
  @Test
  public void updateTransactionJson_serviceThrowsException_propagatesException() {
    UUID userId = UUID.randomUUID();
    UUID txId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    Transaction existing = new Transaction(userId, 100.0, "FOOD", "Lunch");
    existing.setTransactionId(txId);
    Map<String, Object> updates = Map.of("category", "INVALID");

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(txId)).thenReturn(Optional.of(existing));
    when(mockApiService.updateTransaction(txId, updates))
        .thenThrow(new IllegalArgumentException("Invalid category"));

    IllegalArgumentException thrown = assertThrows(
        IllegalArgumentException.class,
        () -> routeController.updateTransactionJson(userId, txId, updates));

    assertEquals("Invalid category", thrown.getMessage());
  }

  /**
   * Tests PUT .../transactions/{txId} with logger disabled.
   *
   * <p>Partition: P7 (Edge) - Logger disabled.
   */
  @Test
  public void updateTransactionJson_loggerOff_validUpdate_returnsUpdatedTransactionWith200() {
    Level original = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      UUID txId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.0);
      Transaction existing = new Transaction(userId, 100.0, "FOOD", "Lunch");
      existing.setTransactionId(txId);
      Transaction updated = new Transaction(userId, 120.0, "FOOD", "Dinner");
      updated.setTransactionId(txId);
      Map<String, Object> updates = Map.of("amount", 120.0);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.getTransaction(txId)).thenReturn(Optional.of(existing));
      when(mockApiService.updateTransaction(txId, updates)).thenReturn(Optional.of(updated));
      ResponseEntity<Transaction> response =
          routeController.updateTransactionJson(userId, txId, updates);
      assertEquals(HttpStatus.OK, response.getStatusCode());
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for showCreateTransactionForm (GET /users/{userId}/transactions/create-form)
  // ===========================================================================

  /**
   * Tests GET .../transactions/create-form for existing user.
   *
   * <p>Partition: P1 (Valid) - User exists.
   */
  @Test
  public void showCreateTransactionForm_validUser_returnsHtmlForm() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));

    String html = routeController.showCreateTransactionForm(userId);

    assertTrue(html.contains("Create New Transaction"));
    assertTrue(html.contains("<form"));
  }

  /**
   * Tests GET .../transactions/create-form for minimal user.
   *
   * <p>Partition: P2 (Valid/Edge) - Minimal user.
   */
  @Test
  public void showCreateTransactionForm_minimalUser_returnsHtmlForm() {
    UUID userId = UUID.randomUUID();
    User user = new User("X", "", 0.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));

    String html = routeController.showCreateTransactionForm(userId);

    assertTrue(html.contains("Create New Transaction"));
  }

  /**
   * Tests GET .../transactions/create-form when user not found.
   *
   * <p>Partition: P3 (Invalid) - User not found.
   */
  @Test
  public void showCreateTransactionForm_userNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();

    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.showCreateTransactionForm(userId));

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  /**
   * Tests GET .../transactions/create-form with logger disabled.
   *
   * <p>Partition: P4 (Edge) - Logger disabled.
   */
  @Test
  public void showCreateTransactionForm_loggerOff_validUser_returnsHtmlForm() {
    Level original = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.0);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      String html = routeController.showCreateTransactionForm(userId);
      assertTrue(html.contains("Create New Transaction"));
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for deleteTransaction (DELETE /users/{userId}/transactions/{txId})
  // ===========================================================================

  /**
   * Tests DELETE .../transactions/{txId} for valid deletion.
   *
   * <p>Partition: P1 (Valid) - Valid deletion.
   */
  @Test
  public void deleteTransaction_validDeletion_returnsConfirmationWith200() {
    UUID userId = UUID.randomUUID();
    UUID txId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    Transaction tx = new Transaction(userId, 50.0, "FOOD", "Lunch");
    tx.setTransactionId(txId);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(txId)).thenReturn(Optional.of(tx));
    when(mockApiService.deleteTransaction(txId)).thenReturn(true);

    ResponseEntity<Map<String, Object>> response = routeController.deleteTransaction(userId, txId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue((Boolean) response.getBody().get("deleted"));
  }

  /**
   * Tests DELETE .../transactions/{txId} when user not found.
   *
   * <p>Partition: P2 (Invalid) - User not found.
   */
  @Test
  public void deleteTransaction_userNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    UUID txId = UUID.randomUUID();

    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.deleteTransaction(userId, txId));

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  /**
   * Tests DELETE .../transactions/{txId} when transaction belongs to different user.
   *
   * <p>Partition: P4 (Invalid) - Transaction belongs to different user.
   */
  @Test
  public void deleteTransaction_transactionBelongsToDifferentUser_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    UUID txId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    Transaction tx = new Transaction(otherUserId, 50.0, "FOOD", "Lunch");
    tx.setTransactionId(txId);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(txId)).thenReturn(Optional.of(tx));

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.deleteTransaction(userId, txId));

    assertTrue(thrown.getMessage().contains("Transaction " + txId + " not found for user"));
  }

  /**
   * Tests DELETE .../transactions/{txId} when delete returns false.
   *
   * <p>Partition: P5 (Invalid) - Delete returns false.
   */
  @Test
  public void deleteTransaction_deleteFails_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    UUID txId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    Transaction tx = new Transaction(userId, 50.0, "FOOD", "Lunch");
    tx.setTransactionId(txId);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(txId)).thenReturn(Optional.of(tx));
    when(mockApiService.deleteTransaction(txId)).thenReturn(false);

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.deleteTransaction(userId, txId));

    assertTrue(thrown.getMessage().contains("Transaction " + txId + " not found"));
  }

  /**
   * Tests DELETE .../transactions/{txId} with logger disabled.
   *
   * <p>Partition: P6 (Edge) - Logger disabled.
   */
  @Test
  public void deleteTransaction_loggerOff_validDeletion_returnsConfirmationWith200() {
    Level original = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      UUID txId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.0);
      Transaction tx = new Transaction(userId, 50.0, "FOOD", "Lunch");
      tx.setTransactionId(txId);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.getTransaction(txId)).thenReturn(Optional.of(tx));
      when(mockApiService.deleteTransaction(txId)).thenReturn(true);
      ResponseEntity<Map<String, Object>> response =
          routeController.deleteTransaction(userId, txId);
      assertEquals(HttpStatus.OK, response.getStatusCode());
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for deleteTransactionViaGet (GET /users/{userId}/deletetransaction/{txId})
  // ===========================================================================

  /**
   * Tests GET .../deletetransaction/{txId} for valid deletion.
   *
   * <p>Partition: P1 (Valid) - Valid deletion.
   */
  @Test
  public void deleteTransactionViaGet_validDeletion_returnsSuccessMessage() {
    UUID userId = UUID.randomUUID();
    UUID txId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    Transaction tx = new Transaction(userId, 50.0, "FOOD", "Lunch");
    tx.setTransactionId(txId);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(txId)).thenReturn(Optional.of(tx));
    when(mockApiService.deleteTransaction(txId)).thenReturn(true);

    ResponseEntity<String> result = routeController.deleteTransactionViaGet(userId, txId);

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals("Transaction deleted successfully!", result.getBody());
  }

  /**
   * Tests GET .../deletetransaction/{txId} when user not found.
   *
   * <p>Partition: P2 (Invalid) - User not found.
   */
  @Test
  public void deleteTransactionViaGet_userNotFound_returns404() {
    UUID userId = UUID.randomUUID();
    UUID txId = UUID.randomUUID();

    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    ResponseEntity<String> result = routeController.deleteTransactionViaGet(userId, txId);

    assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    assertTrue(result.getBody().contains("Error: User " + userId + " not found"));
  }

  /**
   * Tests GET .../deletetransaction/{txId} when transaction not found or wrong user.
   *
   * <p>Partition: P3 (Invalid) - Transaction not found or wrong user.
   */
  @Test
  public void deleteTransactionViaGet_transactionNotFound_returns404() {
    UUID userId = UUID.randomUUID();
    UUID txId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(txId)).thenReturn(Optional.empty());

    ResponseEntity<String> result = routeController.deleteTransactionViaGet(userId, txId);

    assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    assertTrue(result.getBody().contains("Error: Transaction " + txId + " not found"));
  }

  /**
   * Tests GET .../deletetransaction/{txId} when delete fails.
   *
   * <p>Partition: P4 (Invalid) - Delete fails.
   */
  @Test
  public void deleteTransactionViaGet_deleteFails_returns500() {
    UUID userId = UUID.randomUUID();
    UUID txId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    Transaction tx = new Transaction(userId, 50.0, "FOOD", "Lunch");
    tx.setTransactionId(txId);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(txId)).thenReturn(Optional.of(tx));
    when(mockApiService.deleteTransaction(txId)).thenReturn(false);

    ResponseEntity<String> result = routeController.deleteTransactionViaGet(userId, txId);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    assertTrue(result.getBody().contains("Error: Failed to delete transaction"));
  }

  /**
   * Tests GET .../deletetransaction/{txId} with logger disabled.
   *
   * <p>Partition: P5 (Edge) - Logger disabled.
   */
  @Test
  public void deleteTransactionViaGet_loggerOff_validDeletion_returnsSuccessMessage() {
    Level original = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      UUID txId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.0);
      Transaction tx = new Transaction(userId, 50.0, "FOOD", "Lunch");
      tx.setTransactionId(txId);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.getTransaction(txId)).thenReturn(Optional.of(tx));
      when(mockApiService.deleteTransaction(txId)).thenReturn(true);
      ResponseEntity<String> result = routeController.deleteTransactionViaGet(userId, txId);
      assertEquals(HttpStatus.OK, result.getStatusCode());
      assertEquals("Transaction deleted successfully!", result.getBody());
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for budgetManagement (GET /users/{userId}/budget)
  // ===========================================================================

  /**
   * Tests GET /users/{userId}/budget for existing user.
   *
   * <p>Partition: P1 (Valid) - User exists.
   */
  @Test
  public void budgetManagement_validUser_returnsBudgetPageHtml() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    Map<String, Object> report = Map.of("totalSpent", 500.0, "remaining", 500.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getBudgetReport(userId)).thenReturn(report);
    when(mockApiService.totalLast7Days(userId)).thenReturn(100.0);

    String html = routeController.budgetManagement(userId);

    assertTrue(html.contains("Budget Management"));
    assertTrue(html.contains("Alice"));
  }

  /**
   * Tests GET /users/{userId}/budget when user not found.
   *
   * <p>Partition: P2 (Invalid) - User not found.
   */
  @Test
  public void budgetManagement_userNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();

    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.budgetManagement(userId));

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  /**
   * Tests GET /users/{userId}/budget with logger disabled.
   *
   * <p>Partition: P3 (Edge) - Logger disabled.
   */
  @Test
  public void budgetManagement_loggerOff_validUser_returnsBudgetPageHtml() {
    Level original = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.0);
      Map<String, Object> report = Map.of("totalSpent", 500.0, "remaining", 500.0);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.getBudgetReport(userId)).thenReturn(report);
      when(mockApiService.totalLast7Days(userId)).thenReturn(100.0);
      String html = routeController.budgetManagement(userId);
      assertTrue(html.contains("Budget Management"));
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for updateBudgetJson (PUT /users/{userId}/budget)
  // ===========================================================================

  /**
   * Tests PUT /users/{userId}/budget with valid update.
   *
   * <p>Partition: P1 (Valid) - Valid budget update.
   */
  @Test
  public void updateBudgetJson_validUpdate_returnsReportWith200() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    Map<String, Object> budgetUpdate = Map.of("budget", 1500.0);
    Map<String, Object> report = Map.of("totalBudget", 1500.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getBudgetReport(userId)).thenReturn(report);

    ResponseEntity<Map<String, Object>> response =
        routeController.updateBudgetJson(userId, budgetUpdate);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  /**
   * Tests PUT /users/{userId}/budget when user not found.
   *
   * <p>Partition: P2 (Invalid) - User not found.
   */
  @Test
  public void updateBudgetJson_userNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    Map<String, Object> budgetUpdate = Map.of("budget", 1500.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.updateBudgetJson(userId, budgetUpdate));

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  /**
   * Tests PUT /users/{userId}/budget with logger disabled.
   *
   * <p>Partition: P3 (Edge) - Logger disabled.
   */
  @Test
  public void updateBudgetJson_loggerOff_validUpdate_returnsReportWith200() {
    Level original = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.0);
      Map<String, Object> budgetUpdate = Map.of("budget", 1500.0);
      Map<String, Object> report = Map.of("totalBudget", 1500.0);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.getBudgetReport(userId)).thenReturn(report);
      ResponseEntity<Map<String, Object>> response =
          routeController.updateBudgetJson(userId, budgetUpdate);
      assertEquals(HttpStatus.OK, response.getStatusCode());
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for updateBudget (POST /users/{userId}/update-budget)
  // ===========================================================================

  /**
   * Tests POST /users/{userId}/update-budget with valid budget.
   *
   * <p>Partition: P1 (Valid) - Valid budget.
   */
  @Test
  public void updateBudget_validBudget_returnsHtmlSuccessWith200() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));

    ResponseEntity<String> response = routeController.updateBudget(userId, 1500.0);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().contains("Budget Updated Successfully"));
    assertTrue(response.getBody().contains("$1500.00"));
  }

  /**
   * Tests POST /users/{userId}/update-budget with zero budget.
   *
   * <p>Partition: P2 (Valid/Boundary) - Zero budget.
   */
  @Test
  public void updateBudget_zeroBudget_returnsHtmlSuccessWith200() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));

    ResponseEntity<String> response = routeController.updateBudget(userId, 0.0);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().contains("$0.00"));
  }

  /**
   * Tests POST /users/{userId}/update-budget when user not found.
   *
   * <p>Partition: P3 (Invalid) - User not found.
   */
  @Test
  public void updateBudget_userNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();

    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.updateBudget(userId, 1500.0));

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  /**
   * Tests POST /users/{userId}/update-budget with logger disabled.
   *
   * <p>Partition: P4 (Edge) - Logger disabled.
   */
  @Test
  public void updateBudget_loggerOff_validBudget_returnsHtmlSuccessWith200() {
    Level original = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.0);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      ResponseEntity<String> response = routeController.updateBudget(userId, 1500.0);
      assertEquals(HttpStatus.OK, response.getStatusCode());
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for weeklySummary (GET /users/{userId}/weekly-summary)
  // ===========================================================================

  /**
   * Tests GET /users/{userId}/weekly-summary for user with transactions.
   *
   * <p>Partition: P1 (Valid) - User with transactions.
   */
  @Test
  public void weeklySummary_userWithTransactions_returnsSummaryMap() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    List<Transaction> transactions = List.of(
        new Transaction(userId, 50.0, "FOOD", "Lunch"));

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.weeklySummary(userId)).thenReturn(transactions);
    when(mockApiService.totalLast7Days(userId)).thenReturn(50.0);

    Map<String, Object> summary = routeController.weeklySummary(userId);

    assertEquals("Alice", summary.get("username"));
    assertEquals(50.0, summary.get("weeklyTotal"));
    assertEquals(1, summary.get("transactionCount"));
  }

  /**
   * Tests GET /users/{userId}/weekly-summary for user with no transactions.
   *
   * <p>Partition: P2 (Valid/Boundary) - No transactions.
   */
  @Test
  public void weeklySummary_noTransactions_returnsEmptySummary() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.weeklySummary(userId)).thenReturn(new ArrayList<>());
    when(mockApiService.totalLast7Days(userId)).thenReturn(0.0);

    Map<String, Object> summary = routeController.weeklySummary(userId);

    assertEquals(0.0, summary.get("weeklyTotal"));
    assertEquals(0, summary.get("transactionCount"));
  }

  /**
   * Tests GET /users/{userId}/weekly-summary when user not found.
   *
   * <p>Partition: P3 (Invalid) - User not found.
   */
  @Test
  public void weeklySummary_userNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();

    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.weeklySummary(userId));

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  /**
   * Tests GET /users/{userId}/weekly-summary with logger disabled.
   *
   * <p>Partition: P4 (Edge) - Logger disabled.
   */
  @Test
  public void weeklySummary_loggerOff_userWithTransactions_returnsSummaryMap() {
    Level original = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.0);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.weeklySummary(userId)).thenReturn(new ArrayList<>());
      when(mockApiService.totalLast7Days(userId)).thenReturn(0.0);
      Map<String, Object> summary = routeController.weeklySummary(userId);
      assertNotNull(summary);
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for monthlySummary (GET /users/{userId}/monthly-summary)
  // ===========================================================================

  /**
   * Tests GET /users/{userId}/monthly-summary for valid user.
   *
   * <p>Partition: P1 (Valid) - User exists.
   */
  @Test
  public void monthlySummary_validUser_returnsSummaryMap() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    String mockSummary = "Total spent this month: $400";

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getMonthlySummary(userId)).thenReturn(mockSummary);

    Map<String, Object> response = routeController.monthlySummary(userId);

    assertEquals(mockSummary, response.get("summary"));
  }

  /**
   * Tests GET /users/{userId}/monthly-summary when service returns null.
   *
   * <p>Partition: P2 (Valid/Edge) - Service returns null summary.
   */
  @Test
  public void monthlySummary_nullSummary_returnsMapWithoutSummaryKey() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getMonthlySummary(userId)).thenReturn(null);

    Map<String, Object> response = routeController.monthlySummary(userId);

    assertNull(response.get("summary"));
  }

  /**
   * Tests GET /users/{userId}/monthly-summary when service returns empty string.
   *
   * <p>Partition: P3 (Valid/Edge) - Service returns empty summary.
   */
  @Test
  public void monthlySummary_emptySummary_returnsMapWithEmptyString() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getMonthlySummary(userId)).thenReturn("");

    Map<String, Object> response = routeController.monthlySummary(userId);

    assertEquals("", response.get("summary"));
  }

  /**
   * Tests GET /users/{userId}/monthly-summary when user not found.
   *
   * <p>Partition: P4 (Invalid) - User not found.
   */
  @Test
  public void monthlySummary_userNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();

    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.monthlySummary(userId));

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  /**
   * Tests GET /users/{userId}/monthly-summary with logger disabled.
   *
   * <p>Partition: P5 (Edge) - Logger disabled.
   */
  @Test
  public void monthlySummary_loggerOff_validUser_returnsSummaryMap() {
    Level original = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.0);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.getMonthlySummary(userId)).thenReturn("Summary");
      Map<String, Object> response = routeController.monthlySummary(userId);
      assertNotNull(response);
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for budgetReport (GET /users/{userId}/budget-report)
  // ===========================================================================

  /**
   * Tests GET /users/{userId}/budget-report for valid user.
   *
   * <p>Partition: P1 (Valid) - User exists.
   */
  @Test
  public void budgetReport_validUser_returnsReportWith200() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    Map<String, Object> report = Map.of("totalSpent", 500.0, "remaining", 500.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getBudgetReport(userId)).thenReturn(report);

    ResponseEntity<Map<String, Object>> response = routeController.budgetReport(userId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(500.0, response.getBody().get("totalSpent"));
  }

  /**
   * Tests GET /users/{userId}/budget-report with empty report.
   *
   * <p>Partition: P2 (Valid/Edge) - Empty report.
   */
  @Test
  public void budgetReport_emptyReport_returnsReportWith200() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 0.0);
    Map<String, Object> report = new HashMap<>();

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getBudgetReport(userId)).thenReturn(report);

    ResponseEntity<Map<String, Object>> response = routeController.budgetReport(userId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().isEmpty());
  }

  /**
   * Tests GET /users/{userId}/budget-report when user not found.
   *
   * <p>Partition: P3 (Invalid) - User not found.
   */
  @Test
  public void budgetReport_userNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();

    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.budgetReport(userId));

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  /**
   * Tests GET /users/{userId}/budget-report with logger disabled.
   *
   * <p>Partition: P4 (Edge) - Logger disabled.
   */
  @Test
  public void budgetReport_loggerOff_validUser_returnsReportWith200() {
    Level original = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.0);
      Map<String, Object> report = Map.of("totalSpent", 500.0);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.getBudgetReport(userId)).thenReturn(report);
      ResponseEntity<Map<String, Object>> response = routeController.budgetReport(userId);
      assertEquals(HttpStatus.OK, response.getStatusCode());
    } finally {
      setLogLevel(original);
    }
  }

  // ===========================================================================
  // Tests for Exception Handlers
  // ===========================================================================

  /**
   * Tests the exception handler for NoSuchElementException.
   *
   * <p>Partition: handleNotFound maps to 404 NOT_FOUND.
   */
  @Test
  public void handleNotFound_returns404WithErrorMessage() {
    NoSuchElementException ex = new NoSuchElementException("User not found");

    ResponseEntity<Map<String, String>> response = routeController.handleNotFound(ex);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("User not found", response.getBody().get("error"));
  }

  /**
   * Tests the exception handler for IllegalArgumentException.
   *
   * <p>Partition: handleBadRequest maps to 400 BAD_REQUEST.
   */
  @Test
  public void handleBadRequest_returns400WithErrorMessage() {
    IllegalArgumentException ex = new IllegalArgumentException("Invalid budget amount");

    ResponseEntity<Map<String, String>> response = routeController.handleBadRequest(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("Invalid budget amount", response.getBody().get("error"));
  }
}