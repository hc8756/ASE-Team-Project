package dev.ase.teamproject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * This class contains the unit tests for the RouteController class.
 */
@ExtendWith(MockitoExtension.class)
public class RouteControllerTests {

  @Mock
  private MockApiService mockApiService;

  @InjectMocks
  private RouteController routeController;

  @BeforeEach
  public void setUp() {
  }

  @AfterEach
  public void tearDown() {
  }

  // ---------------------------------------------------------------------------
  // Tests for index
  // ---------------------------------------------------------------------------
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

  @Test
  public void index_noUsers_returnsNoUsersMessage() {
    when(mockApiService.viewAllUsers()).thenReturn(new ArrayList<>());

    ResponseEntity<String> response = routeController.index();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().contains("No users found."));
  }

  @Test
  public void index_serviceThrowsException_throwsRuntimeException() {
    when(mockApiService.viewAllUsers()).thenThrow(new RuntimeException("Database unavailable"));

    RuntimeException thrown = assertThrows(RuntimeException.class, () -> routeController.index());

    assertEquals("Database unavailable", thrown.getMessage());
  }

  // ---------------------------------------------------------------------------
  // Tests for getAllUsers (trivial)
  // ---------------------------------------------------------------------------
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
   * Typical valid input.
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
   * Atypical valid input.
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
   * Invalid input.
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
   * Typical valid input.
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
   * Atypical valid input.
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
   * Invalid input.
   */
  @Test
  public void createUserJson_serviceThrowsException_throwsRuntimeException() {
    User user = new User("Charlie", "charlie@example.com", 800.0);
    when(mockApiService.addUser(user)).thenThrow(new RuntimeException("Database error"));

    RuntimeException thrown = assertThrows(
        RuntimeException.class,
        () -> routeController.createUserJson(user)
    );

    assertEquals("Database error", thrown.getMessage());
  }

  // ---------------------------------------------------------------------------
  // Tests for createUserFromFormHtml
  // ---------------------------------------------------------------------------
  /**
   * Typical valid input.
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
   * Atypical valid input.
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
   * Invalid input.
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

  // ---------------------------------------------------------------------------
  // Tests for updateUserJson
  // ---------------------------------------------------------------------------
  /**
   * Typical valid input.
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
   * Atypical valid input.
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
   * Invalid input.
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


  // ---------------------------------------------------------------------------
  // Tests for updateUserFromFormHtml
  // ---------------------------------------------------------------------------
  /**
   * Typical valid input.
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
   * Atypical valid input.
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
   * Invalid input.
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
   * Typical valid input.
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
   * Atypical valid input.
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
   * Invalid input.
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
   * Typical valid input.
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
   * Atypical valid input.
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
   * Invalid input.
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
   * Typical valid input.
   */
  @Test
  public void deleteUserViaGet_existingUser_returnsSuccessMessage() {
    UUID userId = UUID.randomUUID();
    when(mockApiService.deleteUser(userId)).thenReturn(true);

    String result = routeController.deleteUserViaGet(userId);

    assertEquals("User deleted successfully", result);
  }

  /**
   * Atypical valid input:.
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
   * Invalid input.
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
   * Typical valid input.
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
   * Atypical valid input.
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
   * Invalid input.
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
   * Invalid input.
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
   * Typical valid input.
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
   * Atypical valid input.
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
   * Invalid input.
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
   * Invalid input.
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
   * Typical valid input.
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
   * Atypical valid input.
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
   * Invalid input.
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
   * Typical valid input.
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
   * Atypical valid input.
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
   * Invalid input.
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
   * Invalid input.
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
   * Typical valid input.
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
   * Atypical valid input.
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

    assertTrue(thrown.getMessage().contains("Transaction " + transactionId + " not found"));
  }

  /**
   * Invalid input.
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
   * Invalid input.
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
   * Invalid input.
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
   * Typical valid input.
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
   * Atypical valid input.
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
   * Invalid input.
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
   * Typical valid input.
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
   * Atypical valid input.
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
   * Invalid input.
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
   * Invalid input.
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
   * Typical valid input.
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
   * Atypical valid input.
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
   * Invalid input.
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
   * Invalid input.
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
   * Typical valid input.
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
   * Atypical valid input.
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
   * Typical valid input.
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
   * Atypical valid input.
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
   * Invalid input.
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
   * Typical valid input.
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
   * Atypical valid input.
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
   * Invalid input.
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
   * Typical valid input.
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

    String html = routeController.weeklySummary(userId);

    assertNotNull(html);
    assertTrue(html.contains("<h1>Weekly Summary - Alice</h1>"));
    assertTrue(html.contains("Total Spent Last 7 Days"));
    assertTrue(html.contains("<table"));
    assertTrue(html.contains("Lunch"));
    assertTrue(html.contains("Subway pass"));
    assertTrue(html.contains("$50.00"));
    assertTrue(html.contains("$30.00"));
  }

  /**
   * Atypical valid input.
   */
  @Test
  public void weeklySummary_noTransactions_returnsNoTransactionsMessage() {
    UUID userId = UUID.randomUUID();
    User user = new User("Bob", "bob@example.com", 500.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.weeklySummary(userId)).thenReturn(new ArrayList<>());
    when(mockApiService.totalLast7Days(userId)).thenReturn(0.0);

    String html = routeController.weeklySummary(userId);

    assertNotNull(html);
    assertTrue(html.contains("<h1>Weekly Summary - Bob</h1>"));
    assertTrue(html.contains("No transactions in the last 7 days."));
    assertTrue(html.contains("$0.00"));
  }

  /**
   * Invalid input.
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
   * Typical valid input.
   */
  @Test
  public void monthlySummary_validUser_returnsHtmlWithSummaryText() {
    UUID userId = UUID.randomUUID();
    User user = new User("Charlie", "charlie@example.com", 1200.0);
    String mockSummary = "Total spent this month: $400\nRemaining: $800";

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getMonthlySummary(userId)).thenReturn(mockSummary);

    String html = routeController.monthlySummary(userId);

    assertNotNull(html);
    assertTrue(html.contains("<h1>Monthly Summary</h1>"));
    assertTrue(html.contains("Total spent this month"));
    assertTrue(html.contains("<pre>"));
    assertTrue(html.contains("$400"));
  }

  /**
   * Atypical valid input.
   */
  @Test
  public void monthlySummary_emptySummary_returnsHtmlWithEmptyPreBlock() {
    UUID userId = UUID.randomUUID();
    User user = new User("Dana", "dana@example.com", 1000.0);

    when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));
    when(mockApiService.getMonthlySummary(userId)).thenReturn("");

    String html = routeController.monthlySummary(userId);

    assertNotNull(html);
    assertTrue(html.contains("<h1>Monthly Summary</h1>"));
    assertTrue(html.contains("<pre></pre>"));
  }

  /**
   * Invalid input.
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
   * Typical valid input.
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
   * Atypical valid input.
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
   * Invalid input.
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