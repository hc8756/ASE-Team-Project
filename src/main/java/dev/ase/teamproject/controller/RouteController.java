package dev.ase.teamproject.controller;

import dev.ase.teamproject.model.Transaction;
import dev.ase.teamproject.model.User;
import dev.ase.teamproject.service.MockApiService;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller providing API endpoints for user and transaction management.
 * Supports both JSON and HTML form-based interactions for creating, reading,
 * updating, and deleting users and transactions, as well as budget analytics.
 */
@RestController
public class RouteController {
  private static final Logger LOGGER = Logger.getLogger(RouteController.class.getName());
  private static final String HTML_OPEN = "<html><body>";
  private static final String HTML_CLOSE = "</body></html>";
  private static final String P_OPEN = "<p>";
  private static final String P_CLOSE = "</p>";
  private static final String H2_OPEN = "<h2>";
  private static final String H2_CLOSE = "</h2>";
  private static final String FMT_2F = "%.2f";
  private static final String USER_NF_PREFIX = "User ";
  private static final String TX_NF_PREFIX = "Transaction ";
  private static final String NF_SUFFIX = " not found";
  private static final String NF_FOR_USER = " not found for user ";
  private static final String FORM_CLOSE = "</form>";
  private static final String REQUIRED = " required><br><br>";
  private static final String GET_USERS = "GET /users/";
  private static final String POST_USERS = "POST /users/";
  private static final String USER_CR_FAIL = "User Creation Failed";

  private final MockApiService mockApiService;

  /**
   * Constructs a new {@code RouteController} with the specified service dependency.
   *
   * @param mockApiService The service layer handling business logic and data access.
   */
  public RouteController(final MockApiService mockApiService) {
    this.mockApiService = mockApiService;
  }

  /**
   * Displays the home page with a list of all users in the system.
   * Provides links to individual user detail pages.
   *
   * @return An HTML response containing the user list or a message if no users exist.
   */
  @GetMapping({"/", "/index"})
  public ResponseEntity<String> index() {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("GET /index called - Fetching home page with user list.");
    }
    final List<User> users = mockApiService.viewAllUsers();
    final StringBuilder userList = new StringBuilder(Math.max(64, users.size() * 48));

    if (users.isEmpty()) {
      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.info("No users found in database.");
      }
      userList.append("No users found.");
    } else {
      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.info("Fetched " + users.size() + " user(s) for index page.");
      }
      userList.append("Existing Users:<br>");
      for (final User user : users) {
        userList.append(String.format(
            "- <a href='/users/%s'>%s</a> | %s | Budget: $%.2f<br>",
            user.getUserId(), user.getUsername(), user.getEmail(), user.getBudget()));
      }
    }

    final String html = HTML_OPEN
        + "<h1>Welcome to the Personal Finance Tracker</h1>"
        + P_OPEN + userList + P_CLOSE
        + HTML_CLOSE;

    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
  }

  // ---------------------------------------------------------------------------
  // User Management
  // ---------------------------------------------------------------------------

  /**
   * Retrieves all users in the system.
   *
   * @return A JSON list of all {@code User} objects.
   */
  @GetMapping("/users")
  public List<User> getAllUsers() {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("GET /users called - Retrieving all users.");
    }
    return mockApiService.viewAllUsers();
  }

  /**
   * Retrieves detailed information about a specific user.
   *
   * @param userId The unique identifier of the user.
   * @return A JSON response containing the {@code User} object.
   * @throws NoSuchElementException if the user is not found.
   */
  @GetMapping("/users/{userId}")
  public ResponseEntity<User> getUser(@PathVariable final UUID userId) {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info(GET_USERS + userId + " called - Fetching user details.");
    }
    return mockApiService.getUser(userId)
        .map(u -> {
          if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("User found: " + u.getUsername());
          }
          return ResponseEntity.ok(u);
        })
        .orElseThrow(() -> {
          if (LOGGER.isLoggable(Level.WARNING)) {
            LOGGER.warning("User not found with ID: " + userId);
          }
          return new NoSuchElementException(USER_NF_PREFIX + userId + NF_SUFFIX);
        });
  }

  /**
   * Creates a new user from a JSON request body. Validates that username and email
   * are provided and checks for uniqueness constraints.
   *
   * @param user The {@code User} object containing username, email, and budget.
   * @return A JSON response with the created {@code User} object and HTTP 201 status.
   * @throws RuntimeException if username or email is missing.
   * @throws IllegalArgumentException if username or email already exists.
   * @throws IllegalStateException if user creation fails.
   */
  @PostMapping(
      value = "/users",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<User> createUserJson(@RequestBody final User user) {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("POST /users called - Creating new user via JSON: " + user.getUsername());
    }
    if (user.getUsername() == null) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Username field is required.");
      }
      throw new RuntimeException(
          "Failed to create user", new IllegalArgumentException("Username field is required"));
    }
    if (user.getEmail() == null) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Email field is required.");
      }
      throw new RuntimeException(
          "Failed to create user", new IllegalArgumentException("Email field is required"));
    }
    try {
      final User saved = mockApiService.addUser(user);
      return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    } catch (DataIntegrityViolationException e) {
      final String errorMessage = e.getMostSpecificCause().getMessage();
      if (errorMessage.contains("users_email_key")) {
        if (LOGGER.isLoggable(Level.WARNING)) {
          LOGGER.warning("Duplicate email violation: " + user.getEmail());
        }
        throw new IllegalArgumentException("Email already exists: " + user.getEmail(), e);
      } else if (errorMessage.contains("users_username_key")) {
        if (LOGGER.isLoggable(Level.WARNING)) {
          LOGGER.warning("Duplicate username violation: " + user.getUsername());
        }
        throw new IllegalArgumentException("Username already exists: " + user.getUsername(), e);
      } else {
        if (LOGGER.isLoggable(Level.WARNING)) {
          LOGGER.warning("Data integrity violation: " + errorMessage);
        }
        throw new IllegalArgumentException("Data integrity violation", e);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to create user", e);
    }
  }

  /**
   * Creates a new user from HTML form data. Validates uniqueness of username
   * and email before creation.
   *
   * @param username The username for the new user.
   * @param email The email address for the new user.
   * @param budget The initial budget for the new user.
   * @return An HTML response confirming user creation or displaying an error message.
   */
  @PostMapping(
      value = "/users/form",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
      produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> createUserFromFormHtml(
      @RequestParam final String username,
      @RequestParam final String email,
      @RequestParam final double budget) {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("POST /users/form called - Creating user via form: " + username);
    }
    // Check if username and email already exist
    if (mockApiService.isUsernameExists(username, null)) {
      final String html = HTML_OPEN
          + H2_OPEN + USER_CR_FAIL + H2_CLOSE
          + "<p>Username already in use</p>"
          + HTML_CLOSE;
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(html);
    } else if (mockApiService.isEmailExists(email, null)) {
      final String html = HTML_OPEN
          + H2_OPEN + USER_CR_FAIL + H2_CLOSE
          + "<p>User email already in use</p>"
          + HTML_CLOSE;
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(html);
    } else {
      final User user = new User();
      user.setUsername(username);
      user.setEmail(email);
      user.setBudget(budget);

      final User saved = mockApiService.addUser(user);

      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.info("User created successfully via form. ID: " + saved.getUserId());
      }

      final String html = HTML_OPEN
          + H2_OPEN + "User Created Successfully!" + H2_CLOSE
          + "<p><strong>User ID:</strong> " + saved.getUserId() + P_CLOSE
          + "<p><strong>Username:</strong> " + saved.getUsername() + P_CLOSE
          + "<p><strong>Email:</strong> " + saved.getEmail() + P_CLOSE
          + "<p><strong>Budget:</strong> $" + String.format(FMT_2F, saved.getBudget()) + P_CLOSE
          + HTML_CLOSE;
      return ResponseEntity.status(HttpStatus.CREATED).body(html);
    }
  }

  /**
   * Updates an existing user with new information from a JSON request body.
   * Validates uniqueness of username and email if they are being changed.
   * Fields that are not provided or are empty retain their existing values.
   *
   * @param userId The unique identifier of the user to update.
   * @param userUpdates A {@code User} object containing the fields to update.
   * @return A JSON response with the updated {@code User} object.
   * @throws NoSuchElementException if the user is not found.
   * @throws IllegalArgumentException if the new username or email already exists.
   */
  @PutMapping(
      value = "/users/{userId}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<User> updateUserJson(
      @PathVariable final UUID userId,
      @RequestBody final User userUpdates) {

    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("PUT /users/" + userId + " called - Updating user via JSON.");
    }

    final Optional<User> existingUser = mockApiService.getUser(userId);
    if (!existingUser.isPresent()) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Cannot update user - not found: " + userId);
      }
      throw new NoSuchElementException(USER_NF_PREFIX + userId + NF_SUFFIX);
    }

    // Check if username and email already exist
    if (mockApiService.isUsernameExists(userUpdates.getUsername(), userId)) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Duplicate username violation: " + userUpdates.getUsername());
      }
      throw new IllegalArgumentException("Username already exists: " + userUpdates.getUsername());
    }
    if (mockApiService.isEmailExists(userUpdates.getEmail(), userId)) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Duplicate email violation: " + userUpdates.getEmail());
      }
      throw new IllegalArgumentException("Email already exists: " + userUpdates.getEmail());
    }

    final User existing = existingUser.get();

    // Mutate the provided instance instead of creating a new one
    userUpdates.setUserId(userId);

    if (userUpdates.getUsername() == null || userUpdates.getUsername().isEmpty()) {
      userUpdates.setUsername(existing.getUsername());
    }
    if (userUpdates.getEmail() == null || userUpdates.getEmail().isEmpty()) {
      userUpdates.setEmail(existing.getEmail());
    }

    mockApiService.deleteUser(userId);
    final User saved = mockApiService.addUser(userUpdates);

    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("User updated successfully. ID: " + userId);
    }
    return ResponseEntity.ok(saved);
  }

  /**
   * Updates an existing user from HTML form data. Validates uniqueness of username
   * and email if they are being changed.
   *
   * @param userId The unique identifier of the user to update.
   * @param username The new username.
   * @param email The new email address.
   * @param budget The new budget amount.
   * @return An HTML response confirming the update or displaying an error message.
   * @throws NoSuchElementException if the user is not found.
   */
  @PostMapping(
      value = "/users/{userId}/update-form",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
      produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> updateUserFromFormHtml(
      @PathVariable final UUID userId,
      @RequestParam final String username,
      @RequestParam final String email,
      @RequestParam final double budget) {

    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info(POST_USERS + userId + "/update-form called - Updating user via form.");
    }
    if (mockApiService.isUsernameExists(username, userId)) {
      final String html = HTML_OPEN
          + H2_OPEN + USER_CR_FAIL + H2_CLOSE
          + "<p>Username already in use</p>"
          + HTML_CLOSE;
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(html);
    } else if (mockApiService.isEmailExists(email, userId)) {
      final String html = HTML_OPEN
          + H2_OPEN + USER_CR_FAIL + H2_CLOSE
          + "<p>User email already in use</p>"
          + HTML_CLOSE;
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(html);
    } else {
      final Optional<User> existingUser = mockApiService.getUser(userId);
      if (!existingUser.isPresent()) {
        if (LOGGER.isLoggable(Level.WARNING)) {
          LOGGER.warning("Cannot update form user - not found: " + userId);
        }
        throw new NoSuchElementException(USER_NF_PREFIX + userId + NF_SUFFIX);
      }

      final User user = new User();
      user.setUserId(userId);
      user.setUsername(username);
      user.setEmail(email);
      user.setBudget(budget);

      mockApiService.deleteUser(userId);
      final User saved = mockApiService.addUser(user);

      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.info("User updated successfully via form. ID: " + userId);
      }

      final String html = HTML_OPEN
          + H2_OPEN + "User Updated Successfully!" + H2_CLOSE
          + "<p><strong>User ID:</strong> " + saved.getUserId() + P_CLOSE
          + "<p><strong>Username:</strong> " + saved.getUsername() + P_CLOSE
          + "<p><strong>Email:</strong> " + saved.getEmail() + P_CLOSE
          + "<p><strong>Budget:</strong> $" + String.format(FMT_2F, saved.getBudget()) + P_CLOSE
          + HTML_CLOSE;
      return ResponseEntity.ok(html);
    }
  }
  /**
   * Deletes a transaction via a GET request for testing convenience.
   * This endpoint should not be used in production as DELETE operations should use
   * the DELETE HTTP method. Validates transaction ownership before deletion.
   *
   * @param userId The unique identifier of the user who owns the transaction.
   * @param transactionId The unique identifier of the transaction to delete.
   * @return ResponseEntity with plain text confirmation message (200 OK) or
   *         error description (404 Not Found, 500 Internal Server Error).
   */

  @GetMapping("/users/{userId}/deletetransaction/{transactionId}")
  public ResponseEntity<String> deleteTransactionViaGet(
      @PathVariable final UUID userId,
      @PathVariable final UUID transactionId) {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("GET /users/" + userId + "/deletetransaction/"
          + transactionId + " called - Deleting via GET.");
    }
    if (!mockApiService.getUser(userId).isPresent()) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Cannot delete via GET - user not found: " + userId);
      }
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body("Error: User " + userId + NF_SUFFIX);
    }
    final Optional<Transaction> txOpt = mockApiService.getTransaction(transactionId);
    final boolean belongsToUser = txOpt.map(t -> t.getUserId().equals(userId)).orElse(false);
    if (!belongsToUser) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Cannot delete via GET - transaction not found: " + transactionId);
      }
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body("Error: Transaction " + transactionId + " not found for user " + userId);
    }
    final boolean deleted = mockApiService.deleteTransaction(transactionId);
    if (!deleted) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Transaction deletion via GET failed: " + transactionId);
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error: Failed to delete transaction " + transactionId);
    }
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("Transaction deleted successfully via GET for user "
          + userId + ": " + transactionId);
    }
    return ResponseEntity.ok("Transaction deleted successfully!");
  }

  /**
   * Displays an HTML form for creating a new user.
   *
   * @return An HTML page with a form for entering user details.
   */
  @GetMapping(value = "/users/create-form", produces = MediaType.TEXT_HTML_VALUE)
  public String showCreateUserForm() {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("GET /users/create-form called - Displaying user creation form.");
    }
    return HTML_OPEN
        + "<h2>Create New User</h2>"
        + "<form action='/users/form' method='post'>"
        + "Username: <input type='text' name='username'" + REQUIRED
        + "Email: <input type='email' name='email'" + REQUIRED
        + "Budget: <input type='number' name='budget' step='0.01'" + REQUIRED
        + "<input type='submit' value='Create User'>"
        + FORM_CLOSE
        + HTML_CLOSE;
  }

  /**
   * Displays an HTML form for editing an existing user. The form is pre-populated
   * with the user's current information.
   *
   * @param userId The unique identifier of the user to edit.
   * @return An HTML page with a form for updating user details.
   * @throws NoSuchElementException if the user is not found.
   */
  @GetMapping(value = "/users/{userId}/edit-form", produces = MediaType.TEXT_HTML_VALUE)
  public String showEditUserForm(@PathVariable final UUID userId) {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info(GET_USERS + userId + "/edit-form called - Displaying edit form.");
    }
    final Optional<User> userOpt = mockApiService.getUser(userId);
    if (!userOpt.isPresent()) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Cannot display edit form - user not found: " + userId);
      }
      throw new NoSuchElementException(USER_NF_PREFIX + userId + NF_SUFFIX);
    }

    final User user = userOpt.get();
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("Edit form displayed for user: " + user.getUsername());
    }
    return HTML_OPEN
        + "<h2>Edit User</h2>"
        + "<form action='/users/" + userId + "/update-form' method='post'>"
        + "Username: <input type='text' name='username' value='" + user.getUsername()
        + "'" + REQUIRED
        + "Email: <input type='email' name='email' value='" + user.getEmail()
        + "'" + REQUIRED
        + "Budget: <input type='number' name='budget' step='0.01' value='"
        + user.getBudget() + "'" + REQUIRED
        + "<input type='submit' value='Update User'>"
        + FORM_CLOSE
        + HTML_CLOSE;
  }

  /**
   * Deletes a user by their unique identifier via a DELETE request.
   *
   * @param userId The unique identifier of the user to delete.
   * @return A JSON response confirming the deletion.
   * @throws NoSuchElementException if the user is not found or already deleted.
   */
  @DeleteMapping("/users/{userId}")
  public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable final UUID userId) {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("DELETE /users/" + userId + " called - Deleting user.");
    }
    final boolean deleted = mockApiService.deleteUser(userId);
    if (!deleted) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("User not found or already deleted: " + userId);
      }
      throw new NoSuchElementException(USER_NF_PREFIX + userId + NF_SUFFIX);
    }
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("User deleted successfully: " + userId);
    }
    return ResponseEntity.ok(Map.of("deleted", true, "userId", userId));
  }

  /**
   * Deletes a user by their unique identifier via a GET request.
   * This endpoint is provided for testing convenience but should not be used
   * in production as DELETE operations should use the DELETE HTTP method.
   *
   * @param userId The unique identifier of the user to delete.
   * @return A plain text confirmation message.
   * @throws NoSuchElementException if the user is not found.
   */
  @GetMapping("/deleteuser/{userId}")
  public String deleteUserViaGet(@PathVariable final UUID userId) {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("GET /deleteuser/" + userId + " called - Deleting user via GET.");
    }
    final boolean deleted = mockApiService.deleteUser(userId);
    if (!deleted) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Cannot delete via GET - user not found: " + userId);
      }
      throw new NoSuchElementException(USER_NF_PREFIX + userId + NF_SUFFIX);
    }
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("User deleted successfully via GET: " + userId);
    }
    return "User deleted successfully";
  }

  // ---------------------------------------------------------------------------
  // Transactions
  // ---------------------------------------------------------------------------

  /**
   * Retrieves all transactions for a specific user.
   * Returns an empty list if the user has no transactions.
   *
   * @param userId The unique identifier of the user whose transactions to retrieve.
   * @return ResponseEntity containing a list of Transaction objects, or an error message
   *         with NOT_FOUND status if the user does not exist.
   */
  @GetMapping("/users/{userId}/transactions")
  public ResponseEntity<?> getUserTransactions(@PathVariable final UUID userId) {
    try {
      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.info("GET /users/" + userId + "/transactions called - Fetching all transactions.");
      }
      if (!mockApiService.getUser(userId).isPresent()) {
        if (LOGGER.isLoggable(Level.WARNING)) {
          LOGGER.warning("User not found for transaction listing: " + userId);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("Error: User " + userId + NF_SUFFIX);
      }
      final List<Transaction> transactions = mockApiService.getTransactionsByUser(userId);
      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.info("Retrieved " + transactions.size() + " transactions for user " + userId);
      }
      return ResponseEntity.ok(transactions);
    } catch (RuntimeException e) {
      if (LOGGER.isLoggable(Level.SEVERE)) {
        LOGGER.severe("Error retrieving transactions for user " + userId + ": " + e.getMessage());
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error retrieving transactions: " + e.getMessage());
    }
  }

  /**
   * Retrieves a specific transaction for a user by transaction ID.
   * Validates that the transaction belongs to the specified user.
   *
   * @param userId The unique identifier of the user.
   * @param transactionId The unique identifier of the transaction to retrieve.
   * @return The requested Transaction object.
   * @throws NoSuchElementException if the user is not found, or if the transaction
   *         does not exist or does not belong to the specified user.
   */
  @GetMapping("/users/{userId}/transactions/{transactionId}")
  public ResponseEntity<Transaction> getTransaction(
      @PathVariable final UUID userId,
      @PathVariable final UUID transactionId) {

    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info(GET_USERS + userId + "/transactions/"
          + transactionId + " called - Fetching transaction details.");
    }
    if (!mockApiService.getUser(userId).isPresent()) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Cannot fetch transaction - user not found: " + userId);
      }
      throw new NoSuchElementException(USER_NF_PREFIX + userId + NF_SUFFIX);
    }
    return mockApiService.getTransaction(transactionId)
        .filter(transaction -> transaction.getUserId().equals(userId))
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new NoSuchElementException(
            TX_NF_PREFIX + transactionId + NF_FOR_USER + userId));
  }

  /**
   * Creates a new transaction for a user using JSON input.
   * The transaction is automatically associated with the specified user,
   * overriding any userId present in the request body.
   *
   * @param userId The unique identifier of the user for whom to create the transaction.
   * @param transaction The transaction object containing details (description, amount, category).
   *        The userId field will be overwritten with the path parameter value.
   * @return The created Transaction object with HTTP status 201 (CREATED).
   * @throws NoSuchElementException if the user is not found.
   */
  @PostMapping(
      value = "/users/{userId}/transactions",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Transaction> createTransactionJson(
      @PathVariable final UUID userId,
      @RequestBody final Transaction transaction) {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info(POST_USERS + userId
          + "/transactions called - Creating new transaction via JSON.");
    }
    if (!mockApiService.getUser(userId).isPresent()) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Cannot create transaction - user not found: " + userId);
      }
      throw new NoSuchElementException(USER_NF_PREFIX + userId + NF_SUFFIX);
    }
    try {
      transaction.setUserId(userId);
      final Transaction saved = mockApiService.addTransaction(transaction);

      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.info("Transaction created successfully for user "
            + userId + ": " + saved.getTransactionId());
      }
      return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    } catch (Exception e) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Transaction creation failed: " + e.getMessage());
      }
      throw e;
    }
  }

  /**
   * Creates a new transaction for a user using HTML form data.
   * Returns an HTML page confirming the transaction creation with formatted details.
   *
   * @param userId The unique identifier of the user for whom to create the transaction.
   * @param description A textual description of the transaction.
   * @param amount The monetary amount of the transaction.
   * @param category The category of the transaction (e.g., FOOD, TRANSPORTATION).
   * @return ResponseEntity containing an HTML success page with HTTP status 201 (CREATED),
   *         or an error message with appropriate status code if creation fails.
   */
  @PostMapping(value = "/users/{userId}/transactions/form",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
      produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> createTransactionFromFormHtml(
      @PathVariable final UUID userId,
      @RequestParam final String description,
      @RequestParam final double amount,
      @RequestParam final String category) {
    try {
      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.info(POST_USERS + userId
            + "/transactions/form called - Creating transaction via form.");
      }
      if (!mockApiService.getUser(userId).isPresent()) {
        if (LOGGER.isLoggable(Level.WARNING)) {
          LOGGER.warning("Cannot create transaction form - user not found: " + userId);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("Error: User " + userId + NF_SUFFIX);
      }

      final Transaction transaction = new Transaction(userId, amount, category, description);
      final Transaction saved = mockApiService.addTransaction(transaction);

      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.info("Transaction created successfully via form for user " + userId
            + ": " + saved.getTransactionId());
      }

      final String html = "<html><body>"
          + "<h2>Transaction Created Successfully!</h2>"
          + "<p><strong>Description:</strong> " + saved.getDescription() + P_CLOSE
          + "<p><strong>Amount:</strong> $" + String.format("%.2f", saved.getAmount()) + P_CLOSE
          + "<p><strong>Category:</strong> " + saved.getCategory() + P_CLOSE
          + "<br>"
          + "</body></html>";

      return ResponseEntity.status(HttpStatus.CREATED).body(html);
    } catch (RuntimeException e) {
      if (LOGGER.isLoggable(Level.SEVERE)) {
        LOGGER.severe("Error creating transaction via form for user "
            + userId + ": " + e.getMessage());
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error creating transaction: " + e.getMessage());
    }
  }

  /**
   * Updates an existing transaction for a user with partial data.
   * Only the fields present in the updates map will be modified; other fields remain unchanged.
   * Validates that the transaction belongs to the specified user before updating.
   *
   * @param userId The unique identifier of the user who owns the transaction.
   * @param transactionId The unique identifier of the transaction to update.
   * @param updates A map containing the fields to update (e.g., "description", "amount",
   *                "category").
   * @return The updated Transaction object.
   * @throws NoSuchElementException if the user is not found, or if the transaction
   *         does not exist or does not belong to the specified user.
   */
  @PutMapping(
      value = "/users/{userId}/transactions/{transactionId}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Transaction> updateTransactionJson(
      @PathVariable final UUID userId,
      @PathVariable final UUID transactionId,
      @RequestBody final Map<String, Object> updates) {

    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("PUT /users/" + userId + "/transactions/"
          + transactionId + " called - Updating transaction.");
    }
    if (!mockApiService.getUser(userId).isPresent()) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Cannot update transaction - user not found: " + userId);
      }
      throw new NoSuchElementException(USER_NF_PREFIX + userId + NF_SUFFIX);
    }

    final Optional<Transaction> existing = mockApiService.getTransaction(transactionId);
    if (!existing.isPresent() || !existing.get().getUserId().equals(userId)) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Cannot update - transaction not found: "
            + transactionId + " for user " + userId);
      }
      throw new NoSuchElementException(
          TX_NF_PREFIX + transactionId + NF_FOR_USER + userId);
    }
    try {
      final Optional<Transaction> updated =
          mockApiService.updateTransaction(transactionId, updates);
      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.info("Transaction updated successfully: " + transactionId + " for user " + userId);
      }
      return ResponseEntity.ok(updated.get());
    } catch (Exception e) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Transaction creation failed: " + e.getMessage());
      }
      throw e;
    }
  }

  /**
   * Displays an HTML form for creating a new transaction.
   * The form includes fields for description, amount, and a dropdown for category selection.
   *
   * @param userId The unique identifier of the user for whom to display the form.
   * @return An HTML page containing the transaction creation form.
   * @throws NoSuchElementException if the user is not found.
   */
  @GetMapping(
      value = "/users/{userId}/transactions/create-form",
      produces = MediaType.TEXT_HTML_VALUE)
  public String showCreateTransactionForm(@PathVariable final UUID userId) {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info(GET_USERS + userId
          + "/transactions/create-form called - Displaying transaction form.");
    }
    if (!mockApiService.getUser(userId).isPresent()) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Cannot display transaction form - user not found: " + userId);
      }
      throw new NoSuchElementException(USER_NF_PREFIX + userId + NF_SUFFIX);
    }

    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("Transaction form displayed successfully for user " + userId);
    }
    return HTML_OPEN
        + "<h2>Create New Transaction</h2>"
        + "<form action='/users/" + userId + "/transactions/form' method='post'>"
        + "Description: <input type='text' name='description'" + REQUIRED
        + "Amount: <input type='number' name='amount' step='0.01'" + REQUIRED
        + "Category: <select name='category' required>"
        + "<option value='FOOD'>Food</option>"
        + "<option value='TRANSPORTATION'>Transportation</option>"
        + "<option value='ENTERTAINMENT'>Entertainment</option>"
        + "<option value='UTILITIES'>Utilities</option>"
        + "<option value='SHOPPING'>Shopping</option>"
        + "<option value='HEALTHCARE'>Healthcare</option>"
        + "<option value='TRAVEL'>Travel</option>"
        + "<option value='EDUCATION'>Education</option>"
        + "<option value='OTHER'>Other</option>"
        + "</select><br><br>"
        + "<input type='submit' value='Create Transaction'>"
        + FORM_CLOSE
        + HTML_CLOSE;
  }

  /**
   * Deletes a specific transaction for a user.
   * Validates that the transaction belongs to the specified user before deletion.
   *
   * @param userId The unique identifier of the user who owns the transaction.
   * @param transactionId The unique identifier of the transaction to delete.
   * @return A map containing deletion confirmation with keys: "deleted" (boolean),
   *         "userId" (UUID), and "transactionId" (UUID).
   * @throws NoSuchElementException if the user is not found, or if the transaction
   *         does not exist or does not belong to the specified user.
   */
  @DeleteMapping("/users/{userId}/transactions/{transactionId}")
  public ResponseEntity<Map<String, Object>> deleteTransaction(
      @PathVariable final UUID userId,
      @PathVariable final UUID transactionId) {

    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("DELETE /users/" + userId + "/transactions/"
          + transactionId + " called - Deleting transaction.");
    }
    if (!mockApiService.getUser(userId).isPresent()) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Cannot delete transaction - user not found: " + userId);
      }
      throw new NoSuchElementException(USER_NF_PREFIX + userId + NF_SUFFIX);
    }

    final Optional<Transaction> existing = mockApiService.getTransaction(transactionId);
    if (!existing.isPresent() || !existing.get().getUserId().equals(userId)) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Cannot delete transaction - not found or mismatched user. ID: "
            + transactionId);
      }
      throw new NoSuchElementException(
          TX_NF_PREFIX + transactionId + NF_FOR_USER + userId);
    }

    final boolean deleted = mockApiService.deleteTransaction(transactionId);
    if (!deleted) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Transaction deletion failed for ID: " + transactionId);
      }
      throw new NoSuchElementException(TX_NF_PREFIX + transactionId + NF_SUFFIX);
    }
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("Transaction deleted successfully for user " + userId + ": " + transactionId);
    }
    return ResponseEntity.ok(Map.of(
        "deleted", true, "userId", userId, "transactionId", transactionId));
  }


  // ---------------------------------------------------------------------------
  // Budget & Analytics
  // ---------------------------------------------------------------------------

  /**
   * Displays an HTML page for budget management showing current budget status,
   * spending totals, and links to various financial reports.
   * Includes a form for updating the user's budget.
   *
   * @param userId The unique identifier of the user whose budget page to display.
   * @return An HTML page containing budget information and management tools.
   * @throws NoSuchElementException if the user is not found.
   */
  @GetMapping(value = "/users/{userId}/budget", produces = MediaType.TEXT_HTML_VALUE)
  public String budgetManagement(@PathVariable final UUID userId) {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info(GET_USERS + userId + "/budget called - Loading budget management page.");
    }
    if (!mockApiService.getUser(userId).isPresent()) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Cannot load budget page - user not found: " + userId);
      }
      throw new NoSuchElementException(USER_NF_PREFIX + userId + NF_SUFFIX);
    }

    final User user = mockApiService.getUser(userId).get();
    final Map<String, Object> budgetReport = mockApiService.getBudgetReport(userId);
    final String weeklyTotal = String.format(FMT_2F, mockApiService.totalLast7Days(userId));

    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("Budget page loaded successfully for user: " + user.getUsername());
    }
    return HTML_OPEN
        + "<h1>Budget Management - " + user.getUsername() + "</h1>"
        + "<h2>Current Budget</h2>"
        + "<p><strong>Total Budget:</strong> $" + String.format(FMT_2F, user.getBudget()) + P_CLOSE
        + "<p><strong>Total Spent:</strong> $" + budgetReport.get("totalSpent") + P_CLOSE
        + "<p><strong>Remaining:</strong> $" + budgetReport.get("remaining") + P_CLOSE
        + "<p><strong>Weekly Spending:</strong> $" + weeklyTotal + P_CLOSE
        + "<h2>Update Budget</h2>"
        + "<form action='/users/" + userId + "/update-budget' method='post'>"
        + "New Budget: <input type='number' name='budget' step='0.01' value='"
        + user.getBudget() + "'" + REQUIRED
        + "<input type='submit' value='Update Budget'>"
        + FORM_CLOSE
        + "<h2>Quick Reports</h2>"
        + "<ul>"
        + "<li><a href='/users/" + userId + "/weekly-summary'>Weekly Summary</a></li>"
        + "<li><a href='/users/" + userId + "/monthly-summary'>Monthly Summary</a></li>"
        + "<li><a href='/users/" + userId + "/budget-report'>Budget Report (JSON)</a></li>"
        + "</ul>"
        + HTML_CLOSE;
  }

  /**
   * Updates a user's budget using JSON input.
   * Accepts a map that may contain budget-related fields such as "budget" (total budget amount).
   *
   * @param userId The unique identifier of the user whose budget to update.
   * @param budgetUpdate A map containing budget fields to update.
   * @return A map containing the updated budget report with keys such as "totalSpent",
   *         "remaining", and other budget statistics.
   * @throws NoSuchElementException if the user is not found.
   */
  @PutMapping(
      value = "/users/{userId}/budget",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> updateBudgetJson(
      @PathVariable final UUID userId,
      @RequestBody final Map<String, Object> budgetUpdate) {

    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("PUT /users/" + userId + "/budget called - Updating budget via JSON.");
    }
    if (!mockApiService.getUser(userId).isPresent()) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Cannot update budget - user not found: " + userId);
      }
      throw new NoSuchElementException(USER_NF_PREFIX + userId + NF_SUFFIX);
    }

    mockApiService.setBudgets(userId, budgetUpdate);
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("Budget updated successfully for user " + userId + " with data: " + budgetUpdate);
    }
    return ResponseEntity.ok(mockApiService.getBudgetReport(userId));
  }

  /**
   * Updates a user's budget using HTML form data.
   * Returns an HTML confirmation page showing the new budget amount.
   *
   * @param userId The unique identifier of the user whose budget to update.
   * @param budget The new budget amount to set.
   * @return ResponseEntity containing an HTML success page confirming the budget update.
   * @throws NoSuchElementException if the user is not found.
   */
  @PostMapping(
      value = "/users/{userId}/update-budget",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
      produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> updateBudget(
      @PathVariable final UUID userId,
      @RequestParam final double budget) {

    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info(POST_USERS + userId + "/update-budget called - Updating budget via HTML form.");
    }
    if (!mockApiService.getUser(userId).isPresent()) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Cannot update budget via form - user not found: " + userId);
      }
      throw new NoSuchElementException(USER_NF_PREFIX + userId + NF_SUFFIX);
    }

    mockApiService.setBudgets(userId, Map.of("budget", budget));
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("Budget successfully updated via form for user " + userId + " to $" + budget);
    }

    final String html = HTML_OPEN
        + H2_OPEN + "Budget Updated Successfully!" + H2_CLOSE
        + "<p><strong>New Budget:</strong> $" + String.format(FMT_2F, budget) + P_CLOSE
        + HTML_CLOSE;

    return ResponseEntity.ok(html);
  }

  /**
   * Generates a weekly summary of transactions for a user.
   * Includes transactions from the last 7 days and calculates the weekly spending total.
   *
   * @param userId The unique identifier of the user whose weekly summary to generate.
   * @return A map containing keys: "username", "weeklyTotal" (double), "transactionCount" (int),
   *         and "transactions" (list of Transaction objects).
   * @throws NoSuchElementException if the user is not found.
   */
  @GetMapping(value = "/users/{userId}/weekly-summary", produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> weeklySummary(@PathVariable final UUID userId) {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info(GET_USERS + userId + "/weekly-summary called - Generating weekly summary.");
    }
    if (!mockApiService.getUser(userId).isPresent()) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Cannot generate weekly summary - user not found: " + userId);
      }
      throw new NoSuchElementException(USER_NF_PREFIX + userId + NF_SUFFIX);
    }

    final User user = mockApiService.getUser(userId).get();
    final List<Transaction> wkTransactions = mockApiService.weeklySummary(userId);
    final double weeklyTotal = mockApiService.totalLast7Days(userId);

    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("Weekly summary generated for user " + user.getUsername()
          + " with " + wkTransactions.size() + " transactions.");
    }

    final Map<String, Object> response = new ConcurrentHashMap<>();
    response.put("username", user.getUsername());
    response.put("weeklyTotal", weeklyTotal);
    response.put("transactionCount", wkTransactions.size());
    response.put("transactions", wkTransactions);

    return response;
  }

  /**
   * Generates a monthly summary of transactions for a user.
   * Provides aggregated spending data for the current month.
   *
   * @param userId The unique identifier of the user whose monthly summary to generate.
   * @return A map containing a "summary" key with monthly transaction data,
   *         or an empty map if no summary is available.
   * @throws NoSuchElementException if the user is not found.
   */
  @GetMapping(
      value = "/users/{userId}/monthly-summary",
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public Map<String, Object> monthlySummary(@PathVariable final UUID userId) {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info(GET_USERS + userId + "/monthly-summary called - Generating monthly summary.");
    }
    if (!mockApiService.getUser(userId).isPresent()) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Cannot generate monthly summary - user not found: " + userId);
      }
      throw new NoSuchElementException(USER_NF_PREFIX + userId + NF_SUFFIX);
    }

    final String summary = mockApiService.getMonthlySummary(userId);
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("Monthly summary generated successfully for user " + userId);
    }

    final Map<String, Object> response = new ConcurrentHashMap<>();
    if (summary != null) {
      response.put("summary", summary);
    }

    return response;
  }

  /**
   * Retrieves a detailed budget report for a user in JSON format.
   * Includes total budget, total spent, remaining budget, and other financial metrics.
   *
   * @param userId The unique identifier of the user whose budget report to retrieve.
   * @return A map containing budget statistics with keys such as "totalSpent", "remaining",
   *         and other relevant financial data.
   * @throws NoSuchElementException if the user is not found.
   */
  @GetMapping(
      value = "/users/{userId}/budget-report",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> budgetReport(@PathVariable final UUID userId) {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info(GET_USERS + userId + "/budget-report called - Retrieving budget report (JSON).");
    }
    if (!mockApiService.getUser(userId).isPresent()) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Cannot retrieve budget report - user not found: " + userId);
      }
      throw new NoSuchElementException(USER_NF_PREFIX + userId + NF_SUFFIX);
    }

    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("Budget report retrieved successfully for user " + userId);
    }
    return ResponseEntity.ok(mockApiService.getBudgetReport(userId));
  }

  // ---------------------------------------------------------------------------
  // Exception handlers & helper functions
  // ---------------------------------------------------------------------------

  /**
   * Exception handler for resource not found errors.
   * Returns a JSON error response with HTTP status 404 (NOT_FOUND).
   *
   * @param exception The NoSuchElementException containing the error message.
   * @return ResponseEntity containing a map with an "error" key and the exception message.
   */
  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<Map<String, String>> handleNotFound(
      final NoSuchElementException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("error", exception.getMessage()));
  }

  /**
   * Exception handler for invalid request data or parameters.
   * Returns a JSON error response with HTTP status 400 (BAD_REQUEST).
   *
   * @param exception The IllegalArgumentException containing the error message.
   * @return ResponseEntity containing a map with an "error" key and the exception message.
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleBadRequest(
      final IllegalArgumentException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("error", exception.getMessage()));
  }
}
