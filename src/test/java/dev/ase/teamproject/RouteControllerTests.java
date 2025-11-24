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

/*
 * Test Plan / Equivalence Partitions for RouteController
 *
 * Endpoints:
 * 1. GET  /index
 * - P1: users list non-empty          -> HTML with user entries
 * - Covered by: index_usersExist_returnsHtmlListOfUsers
 * - P2: users list empty              -> HTML with "No users found"
 * - Covered by: index_noUsers_returnsNoUsersMessage
 * - P3: service throws RuntimeException -> controller propagates RuntimeException
 * - Covered by: index_serviceThrowsException_throwsRuntimeException
 *
 * 2. GET  /users/{userId}
 * - P1: userId exists                 -> 200 OK, User in body
 * - Covered by: getUser_existingUser_returnsUserWith200
 * - P2: userId exists, minimal fields -> 200 OK, minimal user
 * - Covered by: getUser_minimalUserFields_returnsUserWith200
 * - P3: userId does not exist         -> NoSuchElementException
 * - Covered by: getUser_nonexistentUser_throwsNoSuchElementException
 *
 * 3. POST /users (JSON)
 * Input User:
 * - username: { non-null, null }
 * - email:    { non-null, null }
 * - DB constraints: { unique OK, duplicate email, duplicate username, other violation }
 * Partitions:
 * - P1: valid user, service OK        -> 201 CREATED, user returned
 * - Covered by: createUserJson_validUser_returnsCreatedUserWith201
 * - P2: valid user, service RuntimeException -> wrapped RuntimeException
 * - Covered by: createUserJson_serviceThrowsException_throwsRuntimeException
 * - P3: username == null              -> IllegalArgumentException("Username field is required")
 * - Covered by: createUserJson_missingUsername_throwsIllegalArgumentException
 * - P4: email == null                 -> IllegalArgumentException("Email field is required")
 * - Covered by: createUserJson_missingEmail_throwsIllegalArgumentException
 * - P5: DataIntegrityViolation: duplicate email -> 
 *   IllegalArgumentException("Email already exists: ...")
 * - Covered by: createUserJson_duplicateEmail_throwsIllegalArgumentException
 * - P6: DataIntegrityViolation: duplicate username -> 
 *   IllegalArgumentException("Username already exists: ...")
 * - Covered by: createUserJson_duplicateUsername_throwsIllegalArgumentException
 * - P7: DataIntegrityViolation: other DB constraint -> 
 *   IllegalArgumentException("Data integrity violation")
 * - Covered by: createUserJson_genericDataIntegrityViolation_throwsIllegalArgumentException
 *
 * 4. POST /users/form (HTML)
 * - P1: new username/email, OK         -> 201 CREATED HTML success
 * - Covered by: createUserFromFormHtml_validForm_returnsHtmlSuccessWith201
 * - P2: budget == 0 boundary           -> 201 CREATED HTML with $0.00
 * - Covered by: createUserFromFormHtml_zeroBudget_returnsHtmlSuccessWith201
 * - P3: duplicate username             -> 400 BAD_REQUEST, "Username already in use"
 * - Covered by: createUserFromFormHtml_duplicateUsername_returns400
 * - P4: duplicate email                -> 400 BAD_REQUEST, "User email already in use"
 * - Covered by: createUserFromFormHtml_duplicateEmail_returns400
 * - P5: service throws RuntimeException -> propagated RuntimeException
 * - Covered by: createUserFromFormHtml_serviceThrowsException_throwsRuntimeException
 *
 * 5. PUT /users/{userId} (JSON)
 * - P1: user exists, all fields updated       -> 200 OK with updated user
 * - Covered by: updateUserJson_existingUser_returnsUpdatedUserWith200
 * - P2: user exists, budget boundary 0.0      -> 200 OK, 0 budget allowed
 * - Covered by: updateUserJson_zeroBudget_returnsUpdatedUserWith200
 * - P3: userId not found                      -> NoSuchElementException
 * - Covered by: updateUserJson_userNotFound_throwsNoSuchElementException
 * - P4: duplicate username (isUsernameExists) -> IllegalArgumentException
 * - Covered by: updateUserJson_duplicateUsername_throwsIllegalArgumentException
 * - P5: duplicate email (isEmailExists)       -> IllegalArgumentException
 * - Covered by: updateUserJson_duplicateEmail_throwsIllegalArgumentException
 *
 * 6. Similar equivalence partitions are defined and covered for:
 * - /users/{userId}/transactions (list, get, create, update, delete)
 * - /users/{userId}/budget, /weekly-summary, /monthly-summary, /budget-report
 * using the "Typical / Atypical / Invalid input" pattern in each test method name.
 *
 * Exception Handlers:
 * - handleNotFound: maps NoSuchElementException -> 404 with {"error": msg}
 * - Covered by: handleNotFound_returns404WithErrorMessage
 * - handleBadRequest: maps IllegalArgumentException -> 400 with {"error": msg}
 * - Covered by: handleBadRequest_returns400WithErrorMessage
 */

/**
 * This class contains the unit tests for the RouteController class.
 * It uses Mockito to mock the MockApiService dependency, allowing the controller's
 * logic to be tested in isolation.
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
   * Set up logic before each test (if any).
   */
  @BeforeEach
  public void setUp() {
    // Initialization code, if needed
  }

  /**
   * Tear down logic after each test (if any).
   */
  @AfterEach
  public void tearDown() {
    // Cleanup code, if needed
  }

  /**
   * Helper method to temporarily set the log level for the controller's logger.
   * Returns the original level so it can be restored.
   *
   * @param level The new level (e.g., Level.OFF)
   * @return The original Level
   */
  private Level setLogLevel(Level level) {
    Logger logger = Logger.getLogger(RouteController.class.getName());
    Level original = logger.getLevel();
    logger.setLevel(level);
    return original;
  }

  /**
   * Tests GET /index with the logger disabled to cover the 'isLoggable' branch.
   */
  @Test
  public void index_loggerOff_stillReturnsHtml() {
    Logger logger = Logger.getLogger(RouteController.class.getName());
    Level original = logger.getLevel();
    try {
      logger.setLevel(Level.OFF);  // isLoggable(Level.INFO) -> false

      when(mockApiService.viewAllUsers()).thenReturn(List.of());
      ResponseEntity<String> response = routeController.index();

      assertEquals(HttpStatus.OK, response.getStatusCode());
    } finally {
      logger.setLevel(original);   // restore
    }
  }

  // ---------------------------------------------------------------------------
  // New Tests for Logger Branch Coverage (Level.OFF)
  // These tests ensure all `if (LOGGER.isLoggable(...))` branches are covered
  // by running scenarios with the logger level set to OFF.
  // ---------------------------------------------------------------------------

  /**
   * Tests GET /users with the logger disabled.
   */
  @Test
  public void getAllUsers_loggerOff_stillReturnsList() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      List<User> users = List.of(new User("Alice", "a@a.com", 100));
      when(mockApiService.viewAllUsers()).thenReturn(users);
      List<User> result = routeController.getAllUsers();
      assertEquals(users, result);
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /users/{userId} (happy path) with the logger disabled.
   */
  @Test
  public void getUser_loggerOff_existingUser_returnsUserWith200() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1200.0);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      ResponseEntity<User> response = routeController.getUser(userId);
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(user, response.getBody());
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /users/{userId} (not found path) with the logger disabled.
   */
  @Test
  public void getUser_loggerOff_nonexistentUser_throwsNoSuchElementException() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      when(mockApiService.getUser(userId)).thenReturn(Optional.empty());
      assertThrows(NoSuchElementException.class, () -> routeController.getUser(userId));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests POST /users (happy path) with the logger disabled.
   */
  @Test
  public void createUserJson_loggerOff_validUser_returnsCreatedUserWith201() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      User user = new User("Alice", "alice@example.com", 1500.0);
      when(mockApiService.addUser(user)).thenReturn(user);
      ResponseEntity<User> response = routeController.createUserJson(user);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests POST /users (duplicate email path) with the logger disabled.
   */
  @Test
  public void createUserJson_loggerOff_duplicateEmail_throwsIllegalArgumentException() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      User user = new User("Alice", "alice@example.com", 1000.0);
      DataIntegrityViolationException ex = new DataIntegrityViolationException(
          "constraint violation", new RuntimeException("users_email_key"));
      when(mockApiService.addUser(user)).thenThrow(ex);
      assertThrows(IllegalArgumentException.class, () -> routeController.createUserJson(user));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests POST /users (generic exception path) with the logger disabled.
   */
  @Test
  public void createUserJson_loggerOff_serviceThrowsException_throwsRuntimeException() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      User user = new User("Charlie", "charlie@example.com", 800.0);
      when(mockApiService.addUser(user)).thenThrow(new RuntimeException("Database error"));
      assertThrows(RuntimeException.class, () -> routeController.createUserJson(user));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests POST /users/form (happy path) with the logger disabled.
   */
  @Test
  public void createUserFromFormHtml_loggerOff_validForm_returnsHtmlSuccessWith201() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      User saved = new User("Alice", "alice@example.com", 1500.0);
      when(mockApiService.addUser(any(User.class))).thenReturn(saved);
      ResponseEntity<String> response =
          routeController.createUserFromFormHtml(
          "Alice",
          "alice@example.com",
          1500.0
      );
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests POST /users/form (duplicate username path) with the logger disabled.
   */
  @Test
  public void createUserFromFormHtml_loggerOff_duplicateUsername_returns400() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      when(mockApiService.isUsernameExists("Alice", null)).thenReturn(true);
      ResponseEntity<String> response =
          routeController.createUserFromFormHtml(
          "Alice",
          "alice@example.com",
          500.0
      );
      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests PUT /users/{userId} (happy path) with the logger disabled.
   */
  @Test
  public void updateUserJson_loggerOff_existingUser_returnsUpdatedUserWith200() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User existingUser = new User("Alice", "old@example.com", 1000.0);
      User updatedUser = new User("Alice", "new@example.com", 1200.0);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(existingUser));
      when(mockApiService.addUser(updatedUser)).thenReturn(updatedUser);
      ResponseEntity<User> response = routeController.updateUserJson(userId, updatedUser);
      assertEquals(HttpStatus.OK, response.getStatusCode());
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests PUT /users/{userId} (not found path) with the logger disabled.
   */
  @Test
  public void updateUserJson_loggerOff_userNotFound_throwsNoSuchElementException() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User updatedUser = new User("Eve", "eve@example.com", 900.0);
      when(mockApiService.getUser(userId)).thenReturn(Optional.empty());
      assertThrows(
          NoSuchElementException.class,
          () -> routeController.updateUserJson(userId, updatedUser)
      );      
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests POST /users/{userId}/update-form (happy path) with the logger disabled.
   */
  @Test
  public void updateUserFromFormHtml_loggerOff_existingUser_returnsHtmlSuccessWith200() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User existingUser = new User("Alice", "alice@example.com", 1000.0);
      User savedUser = new User("Alice", "alice_new@example.com", 1200.0);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(existingUser));
      when(mockApiService.addUser(any(User.class))).thenReturn(savedUser);
      ResponseEntity<String> response =
          routeController.updateUserFromFormHtml(
          userId,
          "Alice",
          "alice_new@example.com",
          1200.0
      );
      assertEquals(HttpStatus.OK, response.getStatusCode());
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests POST /users/{userId}/update-form (not found path) with the logger disabled.
   */
  @Test
  public void updateUserFromFormHtml_loggerOff_userNotFound_throwsNoSuchElementException() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      when(mockApiService.getUser(userId)).thenReturn(Optional.empty());
      assertThrows(
          NoSuchElementException.class,
          () -> routeController.updateUserFromFormHtml(
              userId,
              "Eve",
              "eve@example.com",
              100.0
          )
      );
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /users/create-form with the logger disabled.
   */
  @Test
  public void showCreateUserForm_loggerOff_returnsExpectedHtmlForm() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      String html = routeController.showCreateUserForm();
      assertTrue(html.contains("<h2>Create New User</h2>"));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /users/{userId}/edit-form (happy path) with the logger disabled.
   */
  @Test
  public void showEditUserForm_loggerOff_existingUser_returnsPopulatedFormHtml() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.0);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      String html = routeController.showEditUserForm(userId);
      assertTrue(html.contains("<h2>Edit User</h2>"));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /users/{userId}/edit-form (not found path) with the logger disabled.
   */
  @Test
  public void showEditUserForm_loggerOff_userNotFound_throwsNoSuchElementException() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      when(mockApiService.getUser(userId)).thenReturn(Optional.empty());
      assertThrows(NoSuchElementException.class, () -> routeController.showEditUserForm(userId));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests DELETE /users/{userId} (happy path) with the logger disabled.
   */
  @Test
  public void deleteUser_loggerOff_existingUser_returnsConfirmationMapWith200() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      when(mockApiService.deleteUser(userId)).thenReturn(true);
      ResponseEntity<Map<String, Object>> response = routeController.deleteUser(userId);
      assertEquals(HttpStatus.OK, response.getStatusCode());
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests DELETE /users/{userId} (not found path) with the logger disabled.
   */
  @Test
  public void deleteUser_loggerOff_alreadyDeletedUser_throwsNoSuchElementException() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      when(mockApiService.deleteUser(userId)).thenReturn(false);
      assertThrows(NoSuchElementException.class, () -> routeController.deleteUser(userId));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /deleteuser/{userId} (happy path) with the logger disabled.
   */
  @Test
  public void deleteUserViaGet_loggerOff_existingUser_returnsSuccessMessage() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      when(mockApiService.deleteUser(userId)).thenReturn(true);
      String result = routeController.deleteUserViaGet(userId);
      assertEquals("User deleted successfully", result);
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /deleteuser/{userId} (not found path) with the logger disabled.
   */
  @Test
  public void deleteUserViaGet_loggerOff_alreadyDeletedUser_throwsNoSuchElementException() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      when(mockApiService.deleteUser(userId)).thenReturn(false);
      assertThrows(NoSuchElementException.class, () -> routeController.deleteUserViaGet(userId));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /users/{userId}/transactions (happy path) with the logger disabled.
   */
  @Test
  public void getUserTransactions_loggerOff_existingUser_returnsTransactionsWith200() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.0);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.getTransactionsByUser(userId)).thenReturn(new ArrayList<>());
      ResponseEntity<?> response = routeController.getUserTransactions(userId);
      assertEquals(HttpStatus.OK, response.getStatusCode());
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /users/{userId}/transactions (user not found path) with the logger disabled.
   */
  @Test
  public void getUserTransactions_loggerOff_userDoesNotExist_returns404Error() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      when(mockApiService.getUser(userId)).thenReturn(Optional.empty());
      ResponseEntity<?> response = routeController.getUserTransactions(userId);
      assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /users/{userId}/transactions (service error path) with the logger disabled.
   */
  @Test
  public void getUserTransactions_loggerOff_serviceThrowsException_returns500Error() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Carol", "carol@example.com", 700.0);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.getTransactionsByUser(userId))
          .thenThrow(new RuntimeException("Database error"));
      ResponseEntity<?> response = routeController.getUserTransactions(userId);
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /users/{userId}/transactions/{txId} (happy path) with the logger disabled.
   */
  @Test
  public void getTransaction_loggerOff_existingUserAndTransaction_returnsTransactionWith200() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      UUID transactionId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.0);
      Transaction tx = new Transaction(userId, 50.0, "Food", "Lunch");
      tx.setTransactionId(transactionId);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.getTransaction(transactionId)).thenReturn(Optional.of(tx));
      ResponseEntity<Transaction> response = routeController.getTransaction(userId, transactionId);
      assertEquals(HttpStatus.OK, response.getStatusCode());
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
  * Tests GET /users/{userId}/transactions/{txId} (user not found path) with the logger disabled.
  */
  @Test
  public void getTransaction_loggerOff_userDoesNotExist_throwsNoSuchElementException() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      UUID transactionId = UUID.randomUUID();
      when(mockApiService.getUser(userId)).thenReturn(Optional.empty());
      assertThrows(
          NoSuchElementException.class,
          () -> routeController.getTransaction(userId, transactionId)
      );      
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests POST /users/{userId}/transactions (happy path) with the logger disabled.
   */
  @Test
  public void createTransactionJson_loggerOff_allValid_returnsCreatedTransactionWith201() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.0);
      Transaction transaction = new Transaction(userId, 75.0, "Food", "Lunch");
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.addTransaction(transaction)).thenReturn(transaction);
      ResponseEntity<Transaction> response = 
          routeController.createTransactionJson(userId, transaction);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests POST /users/{userId}/transactions (user not found path) with the logger disabled.
   */
  @Test
  public void createTransactionJson_loggerOff_userDoesNotExist_throwsNoSuchElementException() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      Transaction transaction = new Transaction(userId, 100.0, "Travel", "Trip");
      when(mockApiService.getUser(userId)).thenReturn(Optional.empty());
      assertThrows(NoSuchElementException.class, () -> routeController
          .createTransactionJson(userId, transaction));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests POST /users/{userId}/transactions (service error path) with the logger disabled.
   */
  @Test
  public void createTransactionJson_loggerOff_serviceThrows_propagatesException() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      Transaction tx = new Transaction();
      when(mockApiService.getUser(userId))
          .thenReturn(Optional.of(new User("u", "u@x.com", 100)));
      when(mockApiService.addTransaction(any(Transaction.class)))
          .thenThrow(new RuntimeException("DB insert broke"));
      assertThrows(RuntimeException.class, () -> routeController.createTransactionJson(userId, tx));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests POST /users/{userId}/transactions/form (happy path) with the logger disabled.
   */
  @Test
  public void createTransactionFromFormHtml_loggerOff_validUser_returnsHtmlSuccessWith201() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.0);
      Transaction saved = new Transaction(userId, 120.0, "Shopping", "Clothes");
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.addTransaction(any(Transaction.class))).thenReturn(saved);
      ResponseEntity<String> response = routeController
          .createTransactionFromFormHtml(userId, "Clothes", 120.0, "Shopping");
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests POST /users/{userId}/transactions/form (user not found path) with the logger disabled.
   */
  @Test
public void createTransactionFromFormHtml_loggerOff_userDoesNotExist_returns404Error() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      when(mockApiService.getUser(userId)).thenReturn(Optional.empty());
      ResponseEntity<String> response = routeController
          .createTransactionFromFormHtml(userId, "Test", 40.0, "Misc");
      assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests POST /users/{userId}/transactions/form (service error path) with the logger disabled.
   */
  @Test
  public void createTransactionFromFormHtml_loggerOff_serviceThrowsException_returns500Error() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Carol", "carol@example.com", 700.0);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.addTransaction(any(Transaction.class)))
          .thenThrow(new RuntimeException("Database error"));
      ResponseEntity<String> response = routeController
          .createTransactionFromFormHtml(userId, "Groceries", 45.0, "Food");
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests PUT /users/{userId}/transactions/{txId} (happy path) with the logger disabled.
   */
  @Test
  public void updateTransactionJson_loggerOff_allValid_returnsUpdatedTransaction200() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      UUID transactionId = UUID.randomUUID();
      Transaction existing = new Transaction(userId, 100.0, "Food", "Lunch");
      existing.setTransactionId(transactionId);
      Transaction updatedTx = new Transaction(userId, 120.0, "Food", "Dinner");
      updatedTx.setTransactionId(transactionId);
      Map<String, Object> updates = Map.of("amount", 120.0);
      User user = new User("Alice", "alice@example.com", 1000.0);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.getTransaction(transactionId)).thenReturn(Optional.of(existing));
      when(mockApiService.updateTransaction(transactionId, updates))
          .thenReturn(Optional.of(updatedTx));
      ResponseEntity<Transaction> response = routeController
          .updateTransactionJson(userId, transactionId, updates);
      assertEquals(HttpStatus.OK, response.getStatusCode());
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests PUT /users/{userId}/transactions/{txId} (user not found path) with the logger disabled.
   */
  @Test
  public void updateTransactionJson_loggerOff_userDoesNotExist_throwsNoSuchElementException() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      UUID transactionId = UUID.randomUUID();
      Map<String, Object> updates = new HashMap<>();
      when(mockApiService.getUser(userId)).thenReturn(Optional.empty());
      assertThrows(NoSuchElementException.class, () -> routeController
          .updateTransactionJson(userId, transactionId, updates));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests PUT /users/{userId}/transactions/{txId} (tx not found path) with the logger disabled.
   */
  @Test
  public void updateTransactionJson_loggerOff_transactionMissing_throwsNoSuchElementException() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      UUID transactionId = UUID.randomUUID();
      User user = new User("Dave", "dave@example.com", 400.0);
      Map<String, Object> updates = new HashMap<>();
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.getTransaction(transactionId)).thenReturn(Optional.empty());
      assertThrows(NoSuchElementException.class, () -> routeController
          .updateTransactionJson(userId, transactionId, updates));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /users/{userId}/transactions/create-form (happy path) with the logger disabled.
   */
  @Test
  public void showCreateTransactionForm_loggerOff_validUser_returnsHtmlForm() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.0);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      String html = routeController.showCreateTransactionForm(userId);
      assertTrue(html.contains("Create New Transaction"));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /users/{userId}/transactions/create-form 
   * (user not found path) with the logger disabled.
   */
  @Test
  public void showCreateTransactionForm_loggerOff_userDoesNotExist_throwsNoSuchElementException() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      when(mockApiService.getUser(userId)).thenReturn(Optional.empty());
      assertThrows(NoSuchElementException.class, () -> routeController
          .showCreateTransactionForm(userId));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests DELETE /users/{userId}/transactions/{txId} (happy path) with the logger disabled.
   */
  @Test
  public void deleteTransaction_loggerOff_validUserAndTransaction_returns200AndConfirmationMap() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      UUID transactionId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.0);
      Transaction tx = new Transaction(userId, 50.0, "Food", "Lunch");
      tx.setTransactionId(transactionId);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.getTransaction(transactionId)).thenReturn(Optional.of(tx));
      when(mockApiService.deleteTransaction(transactionId)).thenReturn(true);
      ResponseEntity<Map<String, Object>> response =
          routeController.deleteTransaction(userId, transactionId);
      assertEquals(HttpStatus.OK, response.getStatusCode());
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests DELETE /users/{userId}/transactions/{txId}
   * (user not found path) with the logger disabled.
   */
  @Test
  public void deleteTransaction_loggerOff_userDoesNotExist_throwsNoSuchElementException() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      UUID transactionId = UUID.randomUUID();
      when(mockApiService.getUser(userId)).thenReturn(Optional.empty());
      assertThrows(
          NoSuchElementException.class,
          () -> routeController.deleteTransaction(userId, transactionId)
      );      
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests DELETE /users/{userId}/transactions/{txId} (tx mismatch path) with the logger disabled.
   */
  @Test
  public void deleteTransaction_loggerOff_transactionToDiffUser_throwsNoSuchElementException() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      UUID otherUserId = UUID.randomUUID();
      UUID transactionId = UUID.randomUUID();
      User user = new User("Carol", "carol@example.com", 800.0);
      Transaction tx = new Transaction(otherUserId, 75.0, "Travel", "Bus");
      tx.setTransactionId(transactionId);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.getTransaction(transactionId)).thenReturn(Optional.of(tx));
      assertThrows(
          NoSuchElementException.class,
          () -> routeController.deleteTransaction(userId, transactionId)
      );      
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests DELETE /users/{userId}/transactions/{txId} (delete fails path) with the logger disabled.
   */
  @Test
  public void deleteTransaction_loggerOff_deleteFails_throwsNoSuchElementException() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      UUID transactionId = UUID.randomUUID();
      User user = new User("Bob", "bob@example.com", 500.0);
      Transaction tx = new Transaction(userId, 25.0, "Misc", "Test");
      tx.setTransactionId(transactionId);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.getTransaction(transactionId)).thenReturn(Optional.of(tx));
      when(mockApiService.deleteTransaction(transactionId)).thenReturn(false);
      assertThrows(
          NoSuchElementException.class,
          () -> routeController.deleteTransaction(userId, transactionId)
      );      
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /users/{userId}/deletetransaction/{txId} (happy path) with the logger disabled.
   */
  @Test
  public void deleteTransactionViaGet_loggerOff_validUserAndTransaction_returnsSuccessMessage() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      UUID transactionId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.0);
      Transaction tx = new Transaction(userId, 60.0, "Food", "Dinner");
      tx.setTransactionId(transactionId);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.getTransaction(transactionId)).thenReturn(Optional.of(tx));
      when(mockApiService.deleteTransaction(transactionId)).thenReturn(true);
      String result = routeController.deleteTransactionViaGet(userId, transactionId);
      assertTrue(result.contains("Transaction deleted successfully"));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /users/{userId}/deletetransaction/{txId} 
   * (user not found path) with the logger disabled.
   */
  @Test
  public void deleteTransactionViaGet_loggerOff_userDoesNotExist_returnsUserNotFoundError() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      UUID transactionId = UUID.randomUUID();
      when(mockApiService.getUser(userId)).thenReturn(Optional.empty());
      String result = routeController.deleteTransactionViaGet(userId, transactionId);
      assertTrue(result.contains("Error: User " + userId + " not found"));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /users/{userId}/deletetransaction/{txId} (tx mismatch path) with the logger disabled.
   */
  @Test
  public void deleteTransactionViaGet_loggerOff_DiffUser_returnsTransactionNotFoundError() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      UUID otherUserId = UUID.randomUUID();
      UUID transactionId = UUID.randomUUID();
      User user = new User("Carol", "carol@example.com", 700.0);
      Transaction tx = new Transaction(otherUserId, 30.0, "Travel", "Train");
      tx.setTransactionId(transactionId);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.getTransaction(transactionId)).thenReturn(Optional.of(tx));
      String result = routeController.deleteTransactionViaGet(userId, transactionId);
      assertTrue(
          result.contains("Error: Transaction " + transactionId + " not found for user " + userId)
      );      
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /users/{userId}/deletetransaction/{txId} 
   * (delete fails path) with the logger disabled.
   */
  @Test
  public void deleteTransactionViaGet_loggerOff_deleteFails_returnsErrorMessage() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      UUID transactionId = UUID.randomUUID();
      User user = new User("Bob", "bob@example.com", 500.0);
      Transaction tx = new Transaction(userId, 40.0, "Misc", "Test");
      tx.setTransactionId(transactionId);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.getTransaction(transactionId)).thenReturn(Optional.of(tx));
      when(mockApiService.deleteTransaction(transactionId)).thenReturn(false);
      String result = routeController.deleteTransactionViaGet(userId, transactionId);
      assertTrue(result.contains("Error: Failed to delete transaction"));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /users/{userId}/budget (happy path) with the logger disabled.
   */
  @Test
  public void budgetManagement_loggerOff_userFoundWithWeeklySpending_returnsHtml() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Alice", "a@a.com", 1000.00);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.getBudgetReport(userId))
          .thenReturn(Map.of("totalSpent", 200.0, "remaining", 800.0));
      when(mockApiService.totalLast7Days(userId)).thenReturn(150.00);
      String html = routeController.budgetManagement(userId);
      assertTrue(html.contains("Budget Management - Alice"));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /users/{userId}/budget (user not found path) with the logger disabled.
   */
  @Test
  public void budgetManagement_loggerOff_userNotFound_throwsNoSuchElementException() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      when(mockApiService.getUser(userId)).thenReturn(Optional.empty());
      assertThrows(NoSuchElementException.class, () -> routeController.budgetManagement(userId));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests PUT /users/{userId}/budget (happy path) with the logger disabled.
   */
  @Test
  public void updateBudgetJson_loggerOff_validUser_returnsUpdatedBudgetReport() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1500.00);
      Map<String, Object> budgetUpdate = Map.of("budget", 2000.00);
      Map<String, Object> updatedReport = Map.of("totalSpent", 500.00, "remaining", 1500.00);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      doNothing().when(mockApiService).setBudgets(userId, budgetUpdate);
      when(mockApiService.getBudgetReport(userId)).thenReturn(updatedReport);
      ResponseEntity<Map<String, Object>> response = routeController
          .updateBudgetJson(userId, budgetUpdate);
      assertEquals(HttpStatus.OK, response.getStatusCode());
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests PUT /users/{userId}/budget (user not found path) with the logger disabled.
   */
  @Test
  public void updateBudgetJson_loggerOff_userNotFound_throwsNoSuchElementException() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      Map<String, Object> budgetUpdate = Map.of("budget", 1000.00);
      when(mockApiService.getUser(userId)).thenReturn(Optional.empty());
      assertThrows(
          NoSuchElementException.class,
          () -> routeController.updateBudgetJson(userId, budgetUpdate)
      );      
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests POST /users/{userId}/update-budget (happy path) with the logger disabled.
   */
  @Test
  public void updateBudget_loggerOff_validUser_returnsHtmlConfirmation() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1000.00);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      doNothing().when(mockApiService).setBudgets(userId, Map.of("budget", 1200.00));
      ResponseEntity<String> response = routeController.updateBudget(userId, 1200.00);
      assertEquals(HttpStatus.OK, response.getStatusCode());
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests POST /users/{userId}/update-budget (user not found path) with the logger disabled.
   */
  @Test
  public void updateBudget_loggerOff_userNotFound_throwsNoSuchElementException() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      when(mockApiService.getUser(userId)).thenReturn(Optional.empty());
      assertThrows(NoSuchElementException.class, 
          () -> routeController.updateBudget(userId, 1500.00));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /users/{userId}/weekly-summary (happy path) with the logger disabled.
   */
  @Test
  public void weeklySummary_loggerOff_withTransactions_rendersTable() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Alice", "a@a.com", 100);
      Transaction t = new Transaction(userId, 10, "FOOD", "Lunch");
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.weeklySummary(userId)).thenReturn(List.of(t));
      when(mockApiService.totalLast7Days(userId)).thenReturn(10.0);
      Map<String, Object> summary = routeController.weeklySummary(userId);
      assertEquals("Alice", summary.get("username"));
      assertEquals(10.0, (Double) summary.get("weeklyTotal"));
      assertEquals(1, summary.get("transactionCount"));
      assertEquals(1, ((List<?>) summary.get("transactions")).size());
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /users/{userId}/weekly-summary (user not found path) with the logger disabled.
   */
  @Test
  public void weeklySummary_loggerOff_userNotFound_throwsNoSuchElementException() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      when(mockApiService.getUser(userId)).thenReturn(Optional.empty());
      assertThrows(NoSuchElementException.class, () -> routeController.weeklySummary(userId));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /users/{userId}/monthly-summary (happy path) with the logger disabled.
   */
  @Test
  public void monthlySummary_loggerOff_validUser_returnsHtmlWithSummaryText() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Charlie", "charlie@example.com", 1200.0);
      String mockSummary = "Total spent this month: $400";
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.getMonthlySummary(userId)).thenReturn(mockSummary);
      Map<String, Object> response = routeController.monthlySummary(userId);
      assertEquals(mockSummary, response.get("summary"));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /users/{userId}/monthly-summary (user not found path) with the logger disabled.
   */
  @Test
  public void monthlySummary_loggerOff_userNotFound_throwsNoSuchElementException() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      when(mockApiService.getUser(userId)).thenReturn(Optional.empty());
      assertThrows(NoSuchElementException.class, () -> routeController.monthlySummary(userId));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /users/{userId}/budget-report (happy path) with the logger disabled.
   */
  @Test
  public void budgetReport_loggerOff_validUser_returnsJsonBudgetReport() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      User user = new User("Alice", "alice@example.com", 1500.00);
      Map<String, Object> report = Map.of("totalSpent", 500.00, "remaining", 1000.00);
      when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
      when(mockApiService.getBudgetReport(userId)).thenReturn(report);
      ResponseEntity<Map<String, Object>> response = routeController.budgetReport(userId);
      assertEquals(HttpStatus.OK, response.getStatusCode());
    } finally {
      setLogLevel(originalLevel);
    }
  }

  /**
   * Tests GET /users/{userId}/budget-report (user not found path) with the logger disabled.
   */
  @Test
  public void budgetReport_loggerOff_userNotFound_throwsNoSuchElementException() {
    Level originalLevel = setLogLevel(Level.OFF);
    try {
      UUID userId = UUID.randomUUID();
      when(mockApiService.getUser(userId)).thenReturn(Optional.empty());
      assertThrows(NoSuchElementException.class, () -> routeController.budgetReport(userId));
    } finally {
      setLogLevel(originalLevel);
    }
  }

  // ---------------------------------------------------------------------------
  // Tests for index
  // ---------------------------------------------------------------------------

  /**
   * Tests GET /index when users exist in the database. Expects HTML list.
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
   * Tests GET /index when no users exist. Expects "No users found" message.
   */
  @Test
  public void index_noUsers_returnsNoUsersMessage() {
    when(mockApiService.viewAllUsers()).thenReturn(new ArrayList<>());

    ResponseEntity<String> response = routeController.index();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().contains("No users found."));
  }

  /**
   * Tests that GET /index propagates RuntimeExceptions from the service layer.
   */
  @Test
  public void index_serviceThrowsException_throwsRuntimeException() {
    when(mockApiService.viewAllUsers()).thenThrow(new RuntimeException("Database unavailable"));

    RuntimeException thrown = assertThrows(RuntimeException.class, () -> routeController.index());

    assertEquals("Database unavailable", thrown.getMessage());
  }

  // ---------------------------------------------------------------------------
  // Tests for getAllUsers (trivial)
  // ---------------------------------------------------------------------------

  /**
   * Tests GET /users returns the complete list of users from the service.
   */
  @Test
  public void getAllUsers_validCall_returnsListFromService() {
    List<User> users = new ArrayList<>();
    users.add(new User("Alice", "alice@example.com", 1000.0));
    users.add(new User("Bob", "bob@example.com", 800.0));
    when(mockApiService.viewAllUsers()).thenReturn(users);

    List<User> result = routeController.getAllUsers();

    assertEquals(users, result);
  }

  // ---------------------------------------------------------------------------
  // Tests for getUser
  // ---------------------------------------------------------------------------
  /**
   * (Typical valid input) Tests GET /users/{userId} for an existing user.
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
   * (Atypical valid input) Tests GET /users/{userId} for a user with minimal/empty fields.
   */
  @Test
  public void getUser_minimalUserFields_returnsUserWith200() {
    UUID userId = UUID.randomUUID();
    User user = new User("X", "", 0.0);
    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));

    ResponseEntity<User> response = routeController.getUser(userId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(user, response.getBody());
    assertEquals("X", response.getBody().getUsername());
  }

  /**
   * (Invalid input) Tests GET /users/{userId} for a non-existent user ID.
   */
  @Test
  public void getUser_nonexistentUser_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.getUser(userId)
    );

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  // ---------------------------------------------------------------------------
  // Tests for createUserJson
  // ---------------------------------------------------------------------------
  /**
   * (Typical valid input) Tests POST /users with a valid JSON payload.
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
   * (Atypical valid input) Tests POST /users with an empty string for email,
   * which is distinct from a null email.
   */

  @Test
  public void createUserJson_missingEmail_returnsCreatedUserWith201() {
    User user = new User("Bob", "", 1000.0);
    when(mockApiService.addUser(user)).thenReturn(user);

    ResponseEntity<User> response = routeController.createUserJson(user);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(user, response.getBody());
  }

  /**
   * (Invalid input) Tests POST /users when the service layer throws a generic exception.
   */
  @Test
  public void createUserJson_serviceThrowsException_throwsRuntimeException() {
    User user = new User("Charlie", "charlie@example.com", 800.0);
    when(mockApiService.addUser(user)).thenThrow(new RuntimeException("Database error"));
  
    RuntimeException thrown = assertThrows(
        RuntimeException.class,
        () -> routeController.createUserJson(user)
    );
  
    // Match the controller behavior: it wraps with "Failed to create user"
    assertEquals("Failed to create user", thrown.getMessage());
  }
  
  /**
   * (Invalid input) Tests POST /users when the 'username' field is null.
   */
  @Test
  public void createUserJson_missingUsername_throwsRuntimeException() {
    User user = new User(null, "alice@example.com", 1000.0);

    RuntimeException ex = assertThrows(
        RuntimeException.class,
        () -> routeController.createUserJson(user)
    );

    // Outer exception message from controller
    assertEquals("Failed to create user", ex.getMessage());

    // Inner cause is the real validation error
    assertTrue(ex.getCause() instanceof IllegalArgumentException);
    assertEquals("Username field is required", ex.getCause().getMessage());
  }

  /**
   * (Invalid input) Tests POST /users when the 'email' field is null.
   */
  @Test
  public void createUserJson_missingEmail_throwsRuntimeException() {
    User user = new User("Alice", null, 1000.0);

    RuntimeException ex = assertThrows(
        RuntimeException.class,
        () -> routeController.createUserJson(user)
    );

    // Outer exception message from controller
    assertEquals("Failed to create user", ex.getMessage());

    // Inner cause is the real validation error
    assertTrue(ex.getCause() instanceof IllegalArgumentException);
    assertEquals("Email field is required", ex.getCause().getMessage());
  }

  /**
   * (Invalid input) Tests POST /users when a duplicate email violation occurs.
   */
  @Test
  public void createUserJson_duplicateEmail_throwsIllegalArgumentException() {
    User user = new User("Alice", "alice@example.com", 1000.0);
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException(
            "constraint violation",
            new RuntimeException("users_email_key")
        );

    when(mockApiService.addUser(user)).thenThrow(ex);

    IllegalArgumentException thrown = assertThrows(
        IllegalArgumentException.class,
        () -> routeController.createUserJson(user)
    );

    assertEquals("Email already exists: alice@example.com", thrown.getMessage());
  }

  /**
   * (Invalid input) Tests POST /users when a duplicate username violation occurs.
   */
  @Test
  public void createUserJson_duplicateUsername_throwsIllegalArgumentException() {
    User user = new User("Alice", "alice@example.com", 1000.0);
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException(
            "constraint violation",
            new RuntimeException("users_username_key")
        );

    when(mockApiService.addUser(user)).thenThrow(ex);

    IllegalArgumentException thrown = assertThrows(
        IllegalArgumentException.class,
        () -> routeController.createUserJson(user)
    );

    assertEquals("Username already exists: Alice", thrown.getMessage());
  }

  /**
   * (Invalid input) Tests POST /users for a generic, non-specific data integrity violation.
   */
  @Test
  public void createUserJson_genericDataIntegrityViolation_throwsIllegalArgumentException() {
    User user = new User("Alice", "alice@example.com", 1000.0);
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException(
            "constraint violation",
            new RuntimeException("some_other_constraint")
        );

    when(mockApiService.addUser(user)).thenThrow(ex);

    IllegalArgumentException thrown = assertThrows(
        IllegalArgumentException.class,
        () -> routeController.createUserJson(user)
    );

    assertEquals("Data integrity violation", thrown.getMessage());
  }
  // ---------------------------------------------------------------------------
  // Tests for createUserFromFormHtml
  // ---------------------------------------------------------------------------
  /**
   * (Typical valid input) Tests POST /users/form with valid, unique data.
   */

  @Test
  public void createUserFromFormHtml_validForm_returnsHtmlSuccessWith201() {
    User saved = new User("Alice", "alice@example.com", 1500.0);
    when(mockApiService.addUser(any(User.class))).thenReturn(saved);

    ResponseEntity<String> response =
        routeController.createUserFromFormHtml("Alice", "alice@example.com", 1500.0);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertTrue(response.getBody().contains("User Created Successfully"));
    assertTrue(response.getBody().contains("Alice"));
  }

  /**
   * (Atypical valid input) Tests POST /users/form with a 0.0 budget.
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
   * (Invalid input) Tests POST /users/form when the service throws an exception.
   */
  @Test
  public void createUserFromFormHtml_serviceThrowsException_throwsRuntimeException() {
    when(mockApiService.addUser(any(User.class)))
        .thenThrow(new RuntimeException("Save failed"));

    RuntimeException thrown = assertThrows(
        RuntimeException.class,
        () -> routeController.createUserFromFormHtml("Eve", "eve@example.com", 100.0)
    );

    assertEquals("Save failed", thrown.getMessage());
  }

  /**
   * (Invalid input) Tests POST /users/form with a duplicate username.
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
   * (Invalid input) Tests POST /users/form with a duplicate email.
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


  // ---------------------------------------------------------------------------
  // Tests for updateUserJson
  // ---------------------------------------------------------------------------
  
  /**
   * Tests PUT /users/{userId} fallback logic: empty username string should use existing username.
   */
  @Test
  public void updateUserJson_emptyUsername_fallsBackToExisting() {
    UUID userId = UUID.randomUUID();
    User existing = new User("originalName", "orig@x.com", 100);

    User updates = new User("", "new@x.com", 150);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(existing));
    when(mockApiService.isUsernameExists("", userId)).thenReturn(false);
    when(mockApiService.isEmailExists("new@x.com", userId)).thenReturn(false);
    when(mockApiService.addUser(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    User result = routeController.updateUserJson(userId, updates).getBody();

    assertEquals("originalName", result.getUsername());
  }
  
  /**
   * Tests PUT /users/{userId} fallback logic: empty email string should use existing email.
   */
  @Test
  public void updateUserJson_emptyEmail_fallsBackToExisting() {
    UUID userId = UUID.randomUUID();
    User existing = new User("oldName", "old@x.com", 100);

    User updates = new User("newName", "", 200);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(existing));
    when(mockApiService.isUsernameExists("newName", userId)).thenReturn(false);
    when(mockApiService.isEmailExists("", userId)).thenReturn(false);

    when(mockApiService.addUser(any(User.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    User updated = routeController.updateUserJson(userId, updates).getBody();

    assertEquals("old@x.com", updated.getEmail());
  }
  
  /**
   * Tests PUT /users/{userId} fallback logic: 
   * empty strings and 0.0 budget should use all existing data.
   */
  @Test
  public void updateUserJson_allFieldsEmpty_fallbacksToExisting() {
    UUID userId = UUID.randomUUID();
    User existing = new User("oldName", "old@x.com", 100);

    User updates = new User("", "", 0.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(existing));
    when(mockApiService.isUsernameExists("", userId)).thenReturn(false);
    when(mockApiService.isEmailExists("", userId)).thenReturn(false);
    when(mockApiService.addUser(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    User out = routeController.updateUserJson(userId, updates).getBody();

    assertEquals("oldName", out.getUsername());
    assertEquals("old@x.com", out.getEmail());
    assertEquals(100.0, out.getBudget());
  }


  /**
   * (Typical valid input) Tests PUT /users/{userId} with a valid update payload.
   */
  @Test
  public void updateUserJson_existingUser_returnsUpdatedUserWith200() {
    UUID userId = UUID.randomUUID();
    User existingUser = new User("Alice", "old@example.com", 1000.0);
    User updatedUser = new User("Alice", "new@example.com", 1200.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(existingUser));
    when(mockApiService.addUser(updatedUser)).thenReturn(updatedUser);

    ResponseEntity<User> response = routeController.updateUserJson(userId, updatedUser);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(updatedUser, response.getBody());
    verify(mockApiService, times(1)).deleteUser(userId);
    verify(mockApiService, times(1)).addUser(updatedUser);
  }

  /**
   * (Atypical valid input) Tests PUT /users/{userId} with a 0.0 budget.
   */
  @Test
  public void updateUserJson_zeroBudget_returnsUpdatedUserWith200() {
    UUID userId = UUID.randomUUID();
    User existingUser = new User("Bob", "bob@example.com", 800.0);
    User updatedUser = new User("Bob", "bob@example.com", 0.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(existingUser));
    when(mockApiService.addUser(updatedUser)).thenReturn(updatedUser);

    ResponseEntity<User> response = routeController.updateUserJson(userId, updatedUser);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(updatedUser, response.getBody());
    assertEquals(userId, response.getBody().getUserId());
  }

  /**
   * (Invalid input) Tests PUT /users/{userId} for a non-existent user ID.
   */
  @Test
  public void updateUserJson_userNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    User updatedUser = new User("Eve", "eve@example.com", 900.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.updateUserJson(userId, updatedUser)
    );

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  /**
   * (Invalid input) Tests PUT /users/{userId} with a duplicate username.
   */
  @Test
  public void updateUserJson_duplicateUsername_throwsIllegalArgumentException() {
    UUID userId = UUID.randomUUID();
    User existingUser = new User("Alice", "alice@example.com", 1000.0);
    User updates = new User("NewName", "alice_new@example.com", 1500.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(existingUser));
    when(mockApiService.isUsernameExists("NewName", userId)).thenReturn(true);

    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> routeController.updateUserJson(userId, updates)
    );

    assertEquals("Username already exists: NewName", ex.getMessage());
  }

  /**
   * (Invalid input) Tests PUT /users/{userId} with a duplicate email.
   */
  @Test
  public void updateUserJson_duplicateEmail_throwsIllegalArgumentException() {
    UUID userId = UUID.randomUUID();
    User existingUser = new User("Alice", "alice@example.com", 1000.0);
    User updates = new User("Alice", "new@example.com", 1500.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(existingUser));
    when(mockApiService.isUsernameExists("Alice", userId)).thenReturn(false);
    when(mockApiService.isEmailExists("new@example.com", userId)).thenReturn(true);

    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> routeController.updateUserJson(userId, updates)
    );

    assertEquals("Email already exists: new@example.com", ex.getMessage());
  }


  // ---------------------------------------------------------------------------
  // Tests for updateUserFromFormHtml
  // ---------------------------------------------------------------------------
  
  /**
  * (Invalid input) Tests POST /users/{userId}/update-form with a duplicate email.
  */
  @Test
  public void updateUserFromFormHtml_duplicateEmail_returns400() {
    UUID userId = UUID.randomUUID();

    when(mockApiService.isUsernameExists("Alice", userId)).thenReturn(false);
    when(mockApiService.isEmailExists("alice@x.com", userId)).thenReturn(true);

    ResponseEntity<String> response =
        routeController.updateUserFromFormHtml(userId, "Alice", "alice@x.com", 500);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().contains("User email already in use"));
  }
  
  /**
   * (Invalid input) Tests POST /users/{userId}/update-form with a duplicate username.
   */
  @Test
  public void updateUserFromFormHtml_duplicateUsername_returns400() {
    UUID userId = UUID.randomUUID();

    when(mockApiService.isUsernameExists("Alice", userId)).thenReturn(true);

    ResponseEntity<String> response =
        routeController.updateUserFromFormHtml(userId, "Alice", "alice@x.com", 500);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().contains("Username already in use"));
  }

  /**
   * (Typical valid input) Tests POST /users/{userId}/update-form for an existing user.
   */
  @Test
  public void updateUserFromFormHtml_existingUser_returnsHtmlSuccessWith200() {
    UUID userId = UUID.randomUUID();
    User existingUser = new User("Alice", "alice@example.com", 1000.0);
    User savedUser = new User("Alice", "alice_new@example.com", 1200.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(existingUser));
    when(mockApiService.addUser(any(User.class))).thenReturn(savedUser);

    ResponseEntity<String> response =
        routeController.updateUserFromFormHtml(userId, "Alice", "alice_new@example.com", 1200.0);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().contains("User Updated Successfully!"));
    assertTrue(response.getBody().contains("alice_new@example.com"));
    verify(mockApiService, times(1)).deleteUser(userId);
    verify(mockApiService, times(1)).addUser(any(User.class));
  }

  /**
   * (Atypical valid input) Tests POST /users/{userId}/update-form with a 0.0 budget.
   */
  @Test
  public void updateUserFromFormHtml_zeroBudget_returnsHtmlWithZeroBudget() {
    UUID userId = UUID.randomUUID();
    User existingUser = new User("Bob", "bob@example.com", 500.0);
    User savedUser = new User("Bob", "bob@example.com", 0.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(existingUser));
    when(mockApiService.addUser(any(User.class))).thenReturn(savedUser);

    ResponseEntity<String> response =
        routeController.updateUserFromFormHtml(userId, "Bob", "bob@example.com", 0.0);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().contains("$0.00"));
  }

  /**
   * (Invalid input) Tests POST /users/{userId}/update-form for a non-existent user.
   */
  @Test
  public void updateUserFromFormHtml_userNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.updateUserFromFormHtml(userId, "Eve", "eve@example.com", 100.0)
    );

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  // ---------------------------------------------------------------------------
  // Tests for showCreateUserForm (trivial)
  // ---------------------------------------------------------------------------
  /**
   * Tests GET /users/create-form returns the correct HTML form.
   */
  @Test
  public void showCreateUserForm_returnsExpectedHtmlForm() {
    String html = routeController.showCreateUserForm();

    assertTrue(html.contains("<h2>Create New User</h2>"));
    assertTrue(html.contains("action='/users/form'"));
    assertTrue(html.contains("Username:"));
    assertTrue(html.contains("Email:"));
    assertTrue(html.contains("Budget:"));
  }

  // ---------------------------------------------------------------------------
  // Tests for showEditUserForm
  // ---------------------------------------------------------------------------
  /**
   * (Typical valid input) Tests GET /users/{userId}/edit-form populates the form correctly.
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
    assertTrue(html.contains(String.valueOf(user.getBudget())));
    assertTrue(html.contains("/users/" + userId + "/update-form"));
  }

  /**
   * (Atypical valid input) Tests GET /users/{userId}/edit-form with empty user fields.
   */
  @Test
  public void showEditUserForm_userWithEmptyFields_returnsValidFormHtml() {
    UUID userId = UUID.randomUUID();
    User user = new User("", "", 0.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));

    String html = routeController.showEditUserForm(userId);

    assertTrue(html.contains("<h2>Edit User</h2>"));
    assertTrue(html.contains("value=''")); // empty username/email fields
    assertTrue(html.contains("value='0.0'")); // zero budget still rendered
  }

  /**
   * (Invalid input) Tests GET /users/{userId}/edit-form for a non-existent user.
   */
  @Test
  public void showEditUserForm_userNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.showEditUserForm(userId)
    );

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  // ---------------------------------------------------------------------------
  // Tests for deleteUser
  // ---------------------------------------------------------------------------
  /**
   * (Typical valid input) Tests DELETE /users/{userId} for an existing user.
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
   * (Atypical valid input) Tests DELETE /users/{userId} when the user is already deleted.
   */
  @Test
  public void deleteUser_alreadyDeletedUser_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.deleteUser(userId)).thenReturn(false);

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.deleteUser(userId)
    );

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  /**
   * (Invalid input) Tests DELETE /users/{userId} when the service throws an exception.
   */
  @Test
  public void deleteUser_serviceThrowsRuntimeException_propagatesException() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.deleteUser(userId)).thenThrow(new RuntimeException("Database failure"));

    RuntimeException thrown = assertThrows(
        RuntimeException.class,
        () -> routeController.deleteUser(userId)
    );

    assertEquals("Database failure", thrown.getMessage());
  }

  // ---------------------------------------------------------------------------
  // Tests for deleteUserViaGet
  // ---------------------------------------------------------------------------
  /**
   * (Typical valid input) Tests GET /deleteuser/{userId} for an existing user.
   */
  @Test
  public void deleteUserViaGet_existingUser_returnsSuccessMessage() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.deleteUser(userId)).thenReturn(true);

    String result = routeController.deleteUserViaGet(userId);

    assertEquals("User deleted successfully", result);
  }

  /**
   * (Atypical valid input) Tests GET /deleteuser/{userId} when the user is already deleted.
   */
  @Test
  public void deleteUserViaGet_alreadyDeletedUser_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.deleteUser(userId)).thenReturn(false);

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.deleteUserViaGet(userId)
    );

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  /**
   * (Invalid input) Tests GET /deleteuser/{userId} when the service throws an exception.
   */
  @Test
  public void deleteUserViaGet_serviceThrowsRuntimeException_propagatesException() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.deleteUser(userId)).thenThrow(new RuntimeException("Internal DB error"));

    RuntimeException thrown = assertThrows(
        RuntimeException.class,
        () -> routeController.deleteUserViaGet(userId)
    );

    assertEquals("Internal DB error", thrown.getMessage());
  }

  // ---------------------------------------------------------------------------
  // Tests for getUserTransactions
  // ---------------------------------------------------------------------------

  /**
   * (Invalid input) Tests GET /users/{userId}/transactions when the service throws an exception.
   */
  @Test
  public void getUserTransactions_serviceThrows_returns500() {
    UUID userId = UUID.randomUUID();
    User user = new User("Test", "t@e.com", 10);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransactionsByUser(userId))
        .thenThrow(new RuntimeException("DB exploded"));

    ResponseEntity<?> response = routeController.getUserTransactions(userId);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("DB exploded"));
  }
  
  /**
   * (Typical valid input) Tests GET /users/{userId}/transactions for a user with transactions.
   */
  @Test
  public void getUserTransactions_existingUser_returnsTransactionsWith200() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    Transaction t1 = new Transaction(userId, 50.0, "Food", "Lunch");
    Transaction t2 = new Transaction(userId, 120.0, "Shopping", "Clothes");
    List<Transaction> transactions = List.of(t1, t2);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransactionsByUser(userId)).thenReturn(transactions);

    ResponseEntity<?> response = routeController.getUserTransactions(userId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(transactions, response.getBody());
  }

  /**
   * (Atypical valid input) Tests GET /users/{userId}/transactions for a user with no transactions.
   */
  @Test
  public void getUserTransactions_existingUserNoTransactions_returnsEmptyListWith200() {
    UUID userId = UUID.randomUUID();
    User user = new User("Bob", "bob@example.com", 500.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransactionsByUser(userId)).thenReturn(new ArrayList<>());

    ResponseEntity<?> response = routeController.getUserTransactions(userId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(((List<?>) response.getBody()).isEmpty());
  }

  /**
   * (Invalid input) Tests GET /users/{userId}/transactions for a non-existent user.
   */
  @Test
  public void getUserTransactions_userDoesNotExist_returns404Error() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    ResponseEntity<?> response = routeController.getUserTransactions(userId);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("User " + userId + " not found"));
  }

  /**
   * (Invalid input) Tests GET /users/{userId}/transactions when the service throws an exception.
   */
  @Test
  public void getUserTransactions_serviceThrowsException_returns500Error() {
    UUID userId = UUID.randomUUID();
    User user = new User("Carol", "carol@example.com", 700.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransactionsByUser(userId))
        .thenThrow(new RuntimeException("Database error"));

    ResponseEntity<?> response = routeController.getUserTransactions(userId);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Database error"));
  }

  // ---------------------------------------------------------------------------
  // Tests for getTransaction
  // ---------------------------------------------------------------------------
  /**
   * (Typical valid input) 
   * Tests GET /users/{userId}/transactions/{txId} for a valid, matching transaction.
   */
  @Test
  public void getTransaction_existingUserAndTransaction_returnsTransactionWith200() {
    UUID userId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    Transaction tx = new Transaction(userId, 50.0, "Food", "Lunch");
    tx.setTransactionId(transactionId);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(transactionId)).thenReturn(Optional.of(tx));

    ResponseEntity<Transaction> response = routeController.getTransaction(userId, transactionId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(tx, response.getBody());
    assertEquals(transactionId, response.getBody().getTransactionId());
    assertEquals(userId, response.getBody().getUserId());
  }

  /**
   * (Atypical valid input)
   * Tests GET .../{txId} where the transaction exists but belongs to another user.
   */
  @Test
  public void getTransaction_transactionBelongsToDifferentUser_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();

    User user = new User("Bob", "bob@example.com", 800.0);
    Transaction tx = new Transaction(otherUserId, 30.0, "Misc", "Other user’s purchase");
    tx.setTransactionId(transactionId);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(transactionId)).thenReturn(Optional.of(tx));

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.getTransaction(userId, transactionId)
    );

    assertTrue(thrown.getMessage().contains("Transaction "
        + transactionId + " not found for user " + userId));
  }

  /**
   * (Invalid input) Tests GET .../{txId} for a non-existent user.
   */
  @Test
  public void getTransaction_userDoesNotExist_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();

    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.getTransaction(userId, transactionId)
    );

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  /**
   * (Invalid input) Tests GET .../{txId} for a non-existent transaction.
   */
  @Test
  public void getTransaction_transactionMissing_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();
    User user = new User("Carol", "carol@example.com", 1200.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(transactionId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.getTransaction(userId, transactionId)
    );

    assertTrue(thrown.getMessage().contains("Transaction " + transactionId + " not found"));
  }

  // ---------------------------------------------------------------------------
  // Tests for createTransactionJson
  // ---------------------------------------------------------------------------
  
  /**
   * (Invalid input) Tests POST .../transactions when the service layer throws an exception.
   */
  @Test
  public void createTransactionJson_serviceThrows_propagatesException() {
    UUID userId = UUID.randomUUID();
    Transaction tx = new Transaction();

    when(mockApiService.getUser(userId))
        .thenReturn(Optional.of(new User("u", "u@x.com", 100)));

    when(mockApiService.addTransaction(any(Transaction.class)))
        .thenThrow(new RuntimeException("DB insert broke"));

    RuntimeException ex = assertThrows(RuntimeException.class, () ->
        routeController.createTransactionJson(userId, tx)
    );

    assertTrue(ex.getMessage().contains("DB insert broke"));
  }

  

  /**
   * (Invalid input) Tests POST .../transactions/form when the service layer throws an exception.
   */
  @Test
  public void createTransactionFromFormHtml_serviceThrows_returns500() {
    UUID userId = UUID.randomUUID();

    when(mockApiService.getUser(userId))
        .thenReturn(Optional.of(new User("A", "a@a.com", 100)));

    when(mockApiService.addTransaction(any(Transaction.class)))
        .thenThrow(new RuntimeException("exploded"));

    ResponseEntity<String> response = routeController.createTransactionFromFormHtml(
        userId, "movie", 12.50, "ENTERTAINMENT"
    );

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertTrue(response.getBody().contains("exploded"));
  }  
  
  /**
   * (Typical valid input) Tests POST .../transactions with a valid JSON payload.
   */
  @Test
  public void createTransactionJson_validUserAndTransaction_returnsCreatedTransactionWith201() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    Transaction transaction = new Transaction(userId, 75.0, "Food", "Lunch");
    Transaction saved = new Transaction(userId, 75.0, "Food", "Lunch");

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.addTransaction(transaction)).thenReturn(saved);

    ResponseEntity<Transaction> response =
        routeController.createTransactionJson(userId, transaction);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(saved, response.getBody());
    assertEquals(userId, response.getBody().getUserId());
  }

  /**
   * (Atypical valid input) Tests POST .../transactions with a 0.0 amount.
   */
  @Test
  public void createTransactionJson_zeroAmount_returnsCreatedTransactionWith201() {
    UUID userId = UUID.randomUUID();
    User user = new User("Bob", "bob@example.com", 500.0);
    Transaction transaction = new Transaction(userId, 0.0, "Misc", "Zero transaction");
    Transaction saved = new Transaction(userId, 0.0, "Misc", "Zero transaction");

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.addTransaction(transaction)).thenReturn(saved);

    ResponseEntity<Transaction> response =
        routeController.createTransactionJson(userId, transaction);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(saved, response.getBody());
    assertEquals(0.0, response.getBody().getAmount());
  }

  /**
   * (Invalid input) Tests POST .../transactions for a non-existent user.
   */
  @Test
  public void createTransactionJson_userDoesNotExist_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    Transaction transaction = new Transaction(userId, 100.0, "Travel", "Trip");

    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.createTransactionJson(userId, transaction)
    );

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  // ---------------------------------------------------------------------------
  // Tests for createTransactionFromFormHtml
  // ---------------------------------------------------------------------------
  /**
   * (Typical valid input) Tests POST .../transactions/form with valid data.
   */
  @Test
  public void createTransactionFromFormHtml_validUser_returnsHtmlSuccessWith201() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);
    Transaction saved = new Transaction(userId, 120.0, "Shopping", "Clothes");

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.addTransaction(any(Transaction.class))).thenReturn(saved);

    ResponseEntity<String> response =
        routeController.createTransactionFromFormHtml(userId, "Clothes", 120.0, "Shopping");

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertTrue(response.getBody().contains("Transaction Created Successfully"));
    assertTrue(response.getBody().contains("Clothes"));
    assertTrue(response.getBody().contains("Shopping"));
  }

  /**
   * (Atypical valid input) 
   * Tests POST .../transactions/form with a negative amount (e.g., a refund).
   */
  @Test
  public void createTransactionFromFormHtml_negativeAmount_returnsHtmlSuccessWith201() {
    UUID userId = UUID.randomUUID();
    User user = new User("Bob", "bob@example.com", 800.0);
    Transaction saved = new Transaction(userId, -50.0, "Refund", "Reversal");

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.addTransaction(any(Transaction.class))).thenReturn(saved);

    ResponseEntity<String> response =
        routeController.createTransactionFromFormHtml(userId, "Reversal", -50.0, "Refund");

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertTrue(response.getBody().contains("Reversal"));
    assertTrue(response.getBody().contains("Refund"));
  }

  /**
   * (Invalid input) Tests POST .../transactions/form for a non-existent user.
   */
  @Test
  public void createTransactionFromFormHtml_userDoesNotExist_returns404Error() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    ResponseEntity<String> response =
        routeController.createTransactionFromFormHtml(userId, "Test", 40.0, "Misc");

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertTrue(response.getBody().contains("User " + userId + " not found"));
  }

  /**
   * (Invalid input) Tests POST .../transactions/form when the service throws an exception.
   */
  @Test
  public void createTransactionFromFormHtml_serviceThrowsException_returns500Error() {
    UUID userId = UUID.randomUUID();
    User user = new User("Carol", "carol@example.com", 700.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.addTransaction(any(Transaction.class)))
        .thenThrow(new RuntimeException("Database error"));

    ResponseEntity<String> response =
        routeController.createTransactionFromFormHtml(userId, "Groceries", 45.0, "Food");

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertTrue(response.getBody().contains("Error creating transaction"));
  }

  // ---------------------------------------------------------------------------
  // Tests for updateTransactionJson
  // ---------------------------------------------------------------------------
  
  /**
   * (Invalid input) Tests PUT .../{txId} when the service update call fails (returns empty).
   */
  @Test
  public void updateTransactionJson_updateFails_throwsException() {
    UUID userId = UUID.randomUUID();
    UUID txId = UUID.randomUUID();

    User user = new User("A", "a@b.com", 100);
    Transaction tx = new Transaction(userId, 5, "FOOD", "xx");
    tx.setTransactionId(txId);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(txId)).thenReturn(Optional.of(tx));

    when(mockApiService.updateTransaction(eq(txId), anyMap()))
            .thenReturn(Optional.empty());

    assertThrows(NoSuchElementException.class,
        () -> routeController.updateTransactionJson(userId, txId, Map.of("amount", 9)));
  }

  /**
   * (Typical valid input) Tests PUT .../{txId} with a valid update payload.
   */
  @Test
  public void updateTransactionJson_validUserAndTransaction_returnsUpdatedTransactionWith200() {
    UUID userId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();

    Transaction existing = new Transaction(userId, 100.0, "Food", "Lunch");
    existing.setTransactionId(transactionId);

    Transaction updatedTx = new Transaction(userId, 120.0, "Food", "Dinner");
    updatedTx.setTransactionId(transactionId);

    Map<String, Object> updates = new HashMap<>();
    updates.put("amount", 120.0);
    updates.put("description", "Dinner");
    User user = new User("Alice", "alice@example.com", 1000.0);
    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(transactionId)).thenReturn(Optional.of(existing));
    when(mockApiService.updateTransaction(transactionId, updates))
        .thenReturn(Optional.of(updatedTx));

    ResponseEntity<Transaction> response =
        routeController.updateTransactionJson(userId, transactionId, updates);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(updatedTx, response.getBody());
    assertEquals(120.0, response.getBody().getAmount());
    assertEquals("Dinner", response.getBody().getDescription());
  }

  /**
   * (Atypical valid input) Tests PUT .../{txId} when the service reports the update failed.
   */
  @Test
  public void updateTransactionJson_updateFails_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();
    User user = new User("Bob", "bob@example.com", 800.0);
    Transaction existing = new Transaction(userId, 200.0, "Travel", "Bus");
    existing.setTransactionId(transactionId);

    Map<String, Object> updates = new HashMap<>();
    updates.put("amount", 220.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(transactionId)).thenReturn(Optional.of(existing));
    when(mockApiService.updateTransaction(transactionId, updates)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.updateTransactionJson(userId, transactionId, updates)
    );

    // Just check the type; don't over-constrain the message
    assertNotNull(thrown.getMessage());
  }
  
  /**
   * (Invalid input) Tests PUT .../{txId} for a non-existent user.
   */
  @Test
  public void updateTransactionJson_userDoesNotExist_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();
    Map<String, Object> updates = new HashMap<>();
    updates.put("amount", 90.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.updateTransactionJson(userId, transactionId, updates)
    );

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  /**
   * (Invalid input) Tests PUT .../{txId} for a transaction belonging to another user.
   */
  @Test
  public void updateTransactionJson_transactionToDifferentUser_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();

    User user = new User("Carol", "carol@example.com", 700.0);
    Transaction tx = new Transaction(otherUserId, 50.0, "Misc", "Not yours");
    tx.setTransactionId(transactionId);

    Map<String, Object> updates = new HashMap<>();
    updates.put("description", "Changed");

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(transactionId)).thenReturn(Optional.of(tx));

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.updateTransactionJson(userId, transactionId, updates)
    );

    assertTrue(thrown.getMessage().contains("Transaction "
        + transactionId + " not found for user " + userId));
  }

  /**
   * (Invalid input) Tests PUT .../{txId} for a non-existent transaction.
   */
  @Test
  public void updateTransactionJson_transactionMissing_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();
    User user = new User("Dave", "dave@example.com", 400.0);

    Map<String, Object> updates = new HashMap<>();
    updates.put("category", "Updated");

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(transactionId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.updateTransactionJson(userId, transactionId, updates)
    );

    assertTrue(thrown.getMessage().contains("Transaction "
        + transactionId + " not found for user " + userId));
  }

  // ---------------------------------------------------------------------------
  // Tests for showCreateTransactionForm
  // ---------------------------------------------------------------------------
  /**
   * (Typical valid input) Tests GET .../transactions/create-form returns the correct HTML.
   */
  @Test
  public void showCreateTransactionForm_validUser_returnsHtmlForm() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));

    String html = routeController.showCreateTransactionForm(userId);

    assertNotNull(html);
    assertTrue(html.contains("<form"));
    assertTrue(html.contains("Create New Transaction"));
    assertTrue(html.contains("/users/" + userId + "/transactions/form"));
    assertTrue(html.contains("Description:"));
    assertTrue(html.contains("Category:"));
    assertTrue(html.contains("value='FOOD'"));
  }

  /**
   * (Atypical valid input) Tests GET .../transactions/create-form for a minimal user.
   */
  @Test
  public void showCreateTransactionForm_atypicalUser_returnsHtmlForm() {
    UUID userId = UUID.randomUUID();
    User user = new User("X", "", 0.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));

    String html = routeController.showCreateTransactionForm(userId);

    assertNotNull(html);
    assertTrue(html.contains("Create New Transaction"));
    assertTrue(html.contains("<select name='category'"));
    assertTrue(html.contains("<input type='submit' value='Create Transaction'"));
  }

  /**
   * (Invalid input) Tests GET .../transactions/create-form for a non-existent user.
   */
  @Test
  public void showCreateTransactionForm_userDoesNotExist_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();

    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.showCreateTransactionForm(userId)
    );

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  // ---------------------------------------------------------------------------
  // Tests for deleteTransaction
  // ---------------------------------------------------------------------------

  /**
   * (Invalid input) Tests DELETE .../{txId} where the transaction belongs to another user.
   */
  @Test
  public void deleteTransaction_transactionBelongsToOtherUser_throws() {
    UUID userId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID txId = UUID.randomUUID();

    Transaction tx = new Transaction(otherId, 50, "FOOD", "desc");
    tx.setTransactionId(txId);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(new User("u", "e", 10)));
    when(mockApiService.getTransaction(txId)).thenReturn(Optional.of(tx));

    assertThrows(NoSuchElementException.class,
        () -> routeController.deleteTransaction(userId, txId));
  }  

  /**
   * (Invalid input) Tests DELETE .../{txId} when the service reports the delete failed.
   */
  @Test
  public void deleteTransaction_deleteFails_throwsNoSuchElementException_alt() {
    UUID userId = UUID.randomUUID();
    UUID txId = UUID.randomUUID();

    User user = new User("X", "x@x.com", 100);

    Transaction tx = new Transaction(userId, 10, "FOOD", "Test");
    tx.setTransactionId(txId);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(txId)).thenReturn(Optional.of(tx));
    when(mockApiService.deleteTransaction(txId)).thenReturn(false);

    NoSuchElementException ex = assertThrows(
        NoSuchElementException.class,
        () -> routeController.deleteTransaction(userId, txId)
    );

    assertTrue(ex.getMessage().contains("Transaction " + txId + " not found"));
  }  
  
  /**
   * (Typical valid input) Tests DELETE .../{txId} for a valid, matching transaction.
   */
  @Test
  public void deleteTransaction_validUserAndTransaction_returns200AndConfirmationMap() {
    UUID userId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();

    User user = new User("Alice", "alice@example.com", 1000.0);
    Transaction tx = new Transaction(userId, 50.0, "Food", "Lunch");
    tx.setTransactionId(transactionId);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(transactionId)).thenReturn(Optional.of(tx));
    when(mockApiService.deleteTransaction(transactionId)).thenReturn(true);

    ResponseEntity<Map<String, Object>> response =
        routeController.deleteTransaction(userId, transactionId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue((Boolean) response.getBody().get("deleted"));
    assertEquals(userId, response.getBody().get("userId"));
    assertEquals(transactionId, response.getBody().get("transactionId"));
  }

  /**
   * (Atypical valid input) Tests DELETE .../{txId} when the service reports the delete failed.
   */
  @Test
  public void deleteTransaction_deleteFails_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();

    User user = new User("Bob", "bob@example.com", 500.0);
    Transaction tx = new Transaction(userId, 25.0, "Misc", "Test");
    tx.setTransactionId(transactionId);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(transactionId)).thenReturn(Optional.of(tx));
    when(mockApiService.deleteTransaction(transactionId)).thenReturn(false);

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.deleteTransaction(userId, transactionId)
    );

    assertTrue(thrown.getMessage().contains("Transaction " + transactionId + " not found"));
  }

  /**
   * (Invalid input) Tests DELETE .../{txId} for a non-existent user.
   */
  @Test
  public void deleteTransaction_userDoesNotExist_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();

    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.deleteTransaction(userId, transactionId)
    );

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  /**
   * (Invalid input) Tests DELETE .../{txId} for a transaction belonging to another user.
   */
  @Test
  public void deleteTransaction_transactionToDifferentUser_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();

    User user = new User("Carol", "carol@example.com", 800.0);
    Transaction tx = new Transaction(otherUserId, 75.0, "Travel", "Bus");
    tx.setTransactionId(transactionId);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(transactionId)).thenReturn(Optional.of(tx));

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.deleteTransaction(userId, transactionId)
    );

    assertTrue(thrown.getMessage().contains("Transaction "
        + transactionId + " not found for user " + userId));
  }

  // ---------------------------------------------------------------------------
  // Tests for deleteTransactionViaGet
  // ---------------------------------------------------------------------------
  /**
   * (Typical valid input) Tests GET .../deletetransaction/{txId} for a valid, matching transaction.
   */
  @Test
  public void deleteTransactionViaGet_validUserAndTransaction_returnsSuccessMessage() {
    UUID userId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();

    User user = new User("Alice", "alice@example.com", 1000.0);
    Transaction tx = new Transaction(userId, 60.0, "Food", "Dinner");
    tx.setTransactionId(transactionId);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(transactionId)).thenReturn(Optional.of(tx));
    when(mockApiService.deleteTransaction(transactionId)).thenReturn(true);

    String result = routeController.deleteTransactionViaGet(userId, transactionId);

    assertTrue(result.contains("Transaction deleted successfully"));
  }

  /**
   * (Atypical valid input) 
   * Tests GET .../deletetransaction/{txId} when the service reports the delete failed.
   */
  @Test
  public void deleteTransactionViaGet_deleteFails_returnsErrorMessage() {
    UUID userId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();

    User user = new User("Bob", "bob@example.com", 500.0);
    Transaction tx = new Transaction(userId, 40.0, "Misc", "Test");
    tx.setTransactionId(transactionId);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(transactionId)).thenReturn(Optional.of(tx));
    when(mockApiService.deleteTransaction(transactionId)).thenReturn(false);

    String result = routeController.deleteTransactionViaGet(userId, transactionId);

    assertTrue(result.contains("Error: Failed to delete transaction"));
  }

  /**
   * (Invalid input) Tests GET .../deletetransaction/{txId} for a non-existent user.
   */
  @Test
  public void deleteTransactionViaGet_userDoesNotExist_returnsUserNotFoundError() {
    UUID userId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();

    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    String result = routeController.deleteTransactionViaGet(userId, transactionId);

    assertTrue(result.contains("Error: User " + userId + " not found"));
  }

  /**
   * (Invalid input) 
   * Tests GET .../deletetransaction/{txId} for a transaction belonging to another user.
   */
  @Test
  public void deleteTransactionViaGet_transactionToDifferentUser_returnsTransactionNotFoundError() {
    UUID userId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();

    User user = new User("Carol", "carol@example.com", 700.0);
    Transaction tx = new Transaction(otherUserId, 30.0, "Travel", "Train");
    tx.setTransactionId(transactionId);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getTransaction(transactionId)).thenReturn(Optional.of(tx));

    String result = routeController.deleteTransactionViaGet(userId, transactionId);

    assertTrue(result.contains("Error: Transaction "
        + transactionId + " not found for user " + userId));
  }

  // ---------------------------------------------------------------------------
  // Tests for budgetManagement
  // ---------------------------------------------------------------------------

  /**
  * (Invalid input) Tests GET .../budget for a non-existent user.
  */
  @Test
  public void budgetManagement_userNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.budgetManagement(userId)
    );

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }
  /**
  * (Typical valid input) Tests GET .../budget for a user with spending.
  */

  @Test
  public void budgetManagement_userFoundWithWeeklySpending_returnsHtml() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "a@a.com", 1000.00);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getBudgetReport(userId)).thenReturn(
        Map.of("totalSpent", 200.0, "remaining", 800.0)
    );

    when(mockApiService.totalLast7Days(userId)).thenReturn(150.00);

    String html = routeController.budgetManagement(userId);

    assertTrue(html.contains("Budget Management - Alice"));
    assertTrue(html.contains("$150.00"));
    assertTrue(html.contains("$200.0"));
    assertTrue(html.contains("$800.0"));
  }
  
  /**
  * (Atypical valid input) Tests GET .../budget for a user with zero budget and spending.
  */
  @Test
  public void budgetManagement_zeroSpendingAllFields_returnsHtml() {
    UUID userId = UUID.randomUUID();
    User user = new User("Bob", "bob@example.com", 0.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getBudgetReport(userId)).thenReturn(
        Map.of("totalSpent", 0.0, "remaining", 0.0)
    );
    when(mockApiService.totalLast7Days(userId)).thenReturn(0.0);

    String html = routeController.budgetManagement(userId);

    assertTrue(html.contains("$0.00"));
    assertTrue(html.contains("$0.0"));
  }
    
  /**
   * (Typical valid input) Tests GET .../budget for a user with a budget.
   */
  @Test
  public void budgetManagement_validUser_returnsBudgetHtmlDashboard() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1500.00);

    Map<String, Object> budgetReport = new HashMap<>();
    budgetReport.put("totalSpent", 600.00);
    budgetReport.put("remaining", 900.00);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getBudgetReport(userId)).thenReturn(budgetReport);
    when(mockApiService.totalLast7Days(userId)).thenReturn(200.00);

    String html = routeController.budgetManagement(userId);

    assertNotNull(html);
    assertTrue(html.contains("<h1>Budget Management - Alice</h1>"));
    assertTrue(html.contains("<p><strong>Total Budget:</strong> $1500.00</p>"));
    assertTrue(html.contains("<p><strong>Total Spent:</strong> $600.0</p>"));
    assertTrue(html.contains("<p><strong>Remaining:</strong> $900.0</p>"));
    assertTrue(html.contains("<p><strong>Weekly Spending:</strong> $200.00</p>"));
    assertTrue(html.contains("/users/" + userId + "/budget-report"));
  }

  /**
   * (Atypical valid input) Tests GET .../budget for a user with a zero budget.
   */
  @Test
  public void budgetManagement_userWithZeroBudget_returnsHtmlWithZeros() {
    UUID userId = UUID.randomUUID();
    User user = new User("Bob", "bob@example.com", 0.00);

    Map<String, Object> budgetReport = new HashMap<>();
    budgetReport.put("totalSpent", 0.00);
    budgetReport.put("remaining", 0.00);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getBudgetReport(userId)).thenReturn(budgetReport);
    when(mockApiService.totalLast7Days(userId)).thenReturn(0.00);

    String html = routeController.budgetManagement(userId);

    assertNotNull(html);
    assertTrue(html.contains("<h1>Budget Management - Bob</h1>"));
    assertTrue(html.contains("<p><strong>Total Budget:</strong> $0.00</p>"));
    assertTrue(html.contains("<p><strong>Total Spent:</strong> $0.0</p>"));
    assertTrue(html.contains("<p><strong>Remaining:</strong> $0.0</p>"));
    assertTrue(html.contains("<p><strong>Weekly Spending:</strong> $0.00</p>"));
  }

  // ---------------------------------------------------------------------------
  // Tests for updateBudgetJson
  // ---------------------------------------------------------------------------
  /**
   * (Typical valid input) Tests PUT .../budget with a valid JSON update.
   */
  @Test
  public void updateBudgetJson_validUser_returnsUpdatedBudgetReport() {
    Map<String, Object> budgetUpdate = new HashMap<>();
    budgetUpdate.put("budget", 2000.00);

    Map<String, Object> updatedReport = new HashMap<>();
    updatedReport.put("totalSpent", 500.00);
    updatedReport.put("remaining", 1500.00);
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1500.00);
    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    doNothing().when(mockApiService).setBudgets(userId, budgetUpdate);
    when(mockApiService.getBudgetReport(userId)).thenReturn(updatedReport);

    ResponseEntity<Map<String, Object>> response =
        routeController.updateBudgetJson(userId, budgetUpdate);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(updatedReport, response.getBody());
    verify(mockApiService).setBudgets(userId, budgetUpdate);
  }

  /**
   * (Atypical valid input) Tests PUT .../budget setting the budget to zero.
   */
  @Test
  public void updateBudgetJson_zeroBudget_returnsUpdatedBudgetReport() {
    UUID userId = UUID.randomUUID();
    User user = new User("Bob", "bob@example.com", 0.00);

    Map<String, Object> budgetUpdate = Map.of("budget", 0.00);
    Map<String, Object> report = Map.of("totalSpent", 0.00, "remaining", 0.00);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    doNothing().when(mockApiService).setBudgets(userId, budgetUpdate);
    when(mockApiService.getBudgetReport(userId)).thenReturn(report);

    ResponseEntity<Map<String, Object>> response =
        routeController.updateBudgetJson(userId, budgetUpdate);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(report, response.getBody());
    verify(mockApiService).setBudgets(userId, budgetUpdate);
  }

  /**
   * (Invalid input) Tests PUT .../budget for a non-existent user.
   */
  @Test
  public void updateBudgetJson_userNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    Map<String, Object> budgetUpdate = Map.of("budget", 1000.00);

    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.updateBudgetJson(userId, budgetUpdate)
    );

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  // ---------------------------------------------------------------------------
  // Tests for updateBudget
  // ---------------------------------------------------------------------------
  /**
   * (Typical valid input) Tests POST .../update-budget with valid form data.
   */
  @Test
  public void updateBudget_validUser_returnsHtmlConfirmation() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.00);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    doNothing().when(mockApiService).setBudgets(userId, Map.of("budget", 1200.00));

    ResponseEntity<String> response = routeController.updateBudget(userId, 1200.00);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().contains("Budget Updated Successfully!"));
    assertTrue(response.getBody().contains("$1200.00"));
    verify(mockApiService).setBudgets(userId, Map.of("budget", 1200.00));
  }

  /**
   * (Atypical valid input) Tests POST .../update-budget setting the budget to zero.
   */
  @Test
  public void updateBudget_zeroBudget_returnsHtmlWithZero() {
    UUID userId = UUID.randomUUID();
    User user = new User("Bob", "bob@example.com", 500.00);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    doNothing().when(mockApiService).setBudgets(userId, Map.of("budget", 0.00));

    ResponseEntity<String> response = routeController.updateBudget(userId, 0.00);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().contains("$0.00"));
    assertTrue(response.getBody().contains("Budget Updated Successfully!"));
    verify(mockApiService).setBudgets(userId, Map.of("budget", 0.00));
  }

  /**
   * (Invalid input) Tests POST .../update-budget for a non-existent user.
   */
  @Test
  public void updateBudget_userNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();

    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.updateBudget(userId, 1500.00)
    );

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  // ---------------------------------------------------------------------------
  // Tests for weeklySummary
  // ---------------------------------------------------------------------------

  /**
  * (Typical valid input) Tests GET .../weekly-summary for a user with transactions.
  */
  @Test
  public void weeklySummary_withTransactions_rendersTable() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "a@a.com", 100);

    Transaction t = new Transaction(userId, 10, "FOOD", "Lunch");

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.weeklySummary(userId)).thenReturn(List.of(t));
    when(mockApiService.totalLast7Days(userId)).thenReturn(10.0);

    Map<String, Object> summary = routeController.weeklySummary(userId);

    assertEquals("Alice", summary.get("username"));
    assertEquals(10.0, (Double) summary.get("weeklyTotal"));
    assertEquals(1, summary.get("transactionCount"));
    List<?> transactions = (List<?>) summary.get("transactions");
    assertEquals(1, transactions.size());
    assertTrue(transactions.contains(t));
  }
  
  /**
   * (Typical valid input) Tests GET .../weekly-summary with multiple transactions.
   */
  @Test
  public void weeklySummary_validUser_returnsHtmlWithTransactionsAndTotal() {
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1000.0);

    Transaction tx1 = new Transaction(userId, 50.0, "FOOD", "Lunch");
    Transaction tx2 = new Transaction(userId, 30.0, "TRANSPORTATION", "Subway pass");
    List<Transaction> transactions = List.of(tx1, tx2);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.weeklySummary(userId)).thenReturn(transactions);
    when(mockApiService.totalLast7Days(userId)).thenReturn(80.0);

    Map<String, Object> summary = routeController.weeklySummary(userId);

    assertNotNull(summary);
    assertEquals("Alice", summary.get("username"));
    assertEquals(80.0, (Double) summary.get("weeklyTotal"));
    assertEquals(2, summary.get("transactionCount"));
    List<?> responseTx = (List<?>) summary.get("transactions");
    assertEquals(2, responseTx.size());
    assertTrue(responseTx.containsAll(List.of(tx1, tx2)));
  }

  /**
   * (Atypical valid input) Tests GET .../weekly-summary for a user with no transactions.
   */
  @Test
  public void weeklySummary_noTransactions_returnsNoTransactionsMessage() {
    UUID userId = UUID.randomUUID();
    User user = new User("Bob", "bob@example.com", 500.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.weeklySummary(userId)).thenReturn(new ArrayList<>());
    when(mockApiService.totalLast7Days(userId)).thenReturn(0.0);

    Map<String, Object> summary = routeController.weeklySummary(userId);

    assertNotNull(summary);
    assertEquals("Bob", summary.get("username"));
    assertEquals(0.0, (Double) summary.get("weeklyTotal"));
    assertEquals(0, summary.get("transactionCount"));
    assertTrue(((List<?>) summary.get("transactions")).isEmpty());
  }

  /**
   * (Invalid input) Tests GET .../weekly-summary for a non-existent user.
   */
  @Test
  public void weeklySummary_userNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.weeklySummary(userId)
    );

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  // ---------------------------------------------------------------------------
  // Tests for monthlySummary
  // ---------------------------------------------------------------------------

  /**
   * (Atypical valid input) Tests GET .../monthly-summary when the service returns null.
   */
  @Test
  public void monthlySummary_nullSummary_returnsEmptyPreBlock() {
    UUID userId = UUID.randomUUID();
    User user = new User("Dana", "d@d.com", 1000);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getMonthlySummary(userId)).thenReturn(null);

    Map<String, Object> response = routeController.monthlySummary(userId);

    assertNull(response.get("summary"));
  }
  
  /**
   * (Typical valid input) Tests GET .../monthly-summary for a valid user.
   */
  @Test
  public void monthlySummary_validUser_returnsHtmlWithSummaryText() {
    UUID userId = UUID.randomUUID();
    User user = new User("Charlie", "charlie@example.com", 1200.0);
    String mockSummary = "Total spent this month: $400\nRemaining: $800";

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getMonthlySummary(userId)).thenReturn(mockSummary);

    Map<String, Object> response = routeController.monthlySummary(userId);

    assertNotNull(response);
    assertEquals(mockSummary, response.get("summary"));
  }

  /**
   * (Atypical valid input) Tests GET .../monthly-summary when the service returns an empty string.
   */
  @Test
  public void monthlySummary_emptySummary_returnsHtmlWithEmptyPreBlock() {
    UUID userId = UUID.randomUUID();
    User user = new User("Dana", "dana@example.com", 1000.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getMonthlySummary(userId)).thenReturn("");

    Map<String, Object> response = routeController.monthlySummary(userId);

    assertNotNull(response);
    assertEquals("", response.get("summary"));
  }

  /**
   * (Invalid input) Tests GET .../monthly-summary for a non-existent user.
   */
  @Test
  public void monthlySummary_userNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.monthlySummary(userId)
    );

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  // ---------------------------------------------------------------------------
  // Tests for budgetReport
  // ---------------------------------------------------------------------------
  /**
   * (Typical valid input) Tests GET .../budget-report for a valid user.
   */
  @Test
  public void budgetReport_validUser_returnsJsonBudgetReport() {
    Map<String, Object> report = new HashMap<>();
    report.put("totalSpent", 500.00);
    report.put("remaining", 1000.00);
    report.put("budget", 1500.00);
    UUID userId = UUID.randomUUID();
    User user = new User("Alice", "alice@example.com", 1500.00);
    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getBudgetReport(userId)).thenReturn(report);

    ResponseEntity<Map<String, Object>> response = routeController.budgetReport(userId);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(report, response.getBody());
    assertTrue(response.getBody().containsKey("totalSpent"));
    assertEquals(500.00, response.getBody().get("totalSpent"));
  }

  /**
   * (Atypical valid input) Tests GET .../budget-report for a user with an empty report.
   */
  @Test
  public void budgetReport_emptyReport_returnsHttp200WithEmptyJson() {
    UUID userId = UUID.randomUUID();
    User user = new User("Bob", "bob@example.com", 0.0);

    Map<String, Object> emptyReport = new HashMap<>();
    emptyReport.put("totalSpent", 0.0);
    emptyReport.put("remaining", 0.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getBudgetReport(userId)).thenReturn(emptyReport);

    ResponseEntity<Map<String, Object>> response = routeController.budgetReport(userId);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().containsKey("remaining"));
    assertEquals(0.0, response.getBody().get("remaining"));
  }

  /**
   * (Invalid input) Tests GET .../budget-report for a non-existent user.
   */
  @Test
  public void budgetReport_userNotFound_throwsNoSuchElementException() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

    NoSuchElementException thrown = assertThrows(
        NoSuchElementException.class,
        () -> routeController.budgetReport(userId)
    );

    assertTrue(thrown.getMessage().contains("User " + userId + " not found"));
  }

  // ---------------------------------------------------------------------------
  // Tests for handleNotFound
  // ---------------------------------------------------------------------------
  /**
   * Tests the @ExceptionHandler for NoSuchElementException.
   */
  @Test
  public void handleNotFound_returns404WithErrorMessage() {
    NoSuchElementException ex = new NoSuchElementException("User not found");

    ResponseEntity<Map<String, String>> response = routeController.handleNotFound(ex);

    assertNotNull(response);
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertTrue(response.getBody().containsKey("error"));
    assertEquals("User not found", response.getBody().get("error"));
  }

  // ---------------------------------------------------------------------------
  // Tests for handleBadRequest
  // ---------------------------------------------------------------------------
  /**
   * Tests the @ExceptionHandler for IllegalArgumentException.
   */
  @Test
  public void handleBadRequest_returns400WithErrorMessage() {
    IllegalArgumentException ex = new IllegalArgumentException("Invalid budget amount");

    ResponseEntity<Map<String, String>> response = routeController.handleBadRequest(ex);

    assertNotNull(response);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().containsKey("error"));
    assertEquals("Invalid budget amount", response.getBody().get("error"));
  }
}