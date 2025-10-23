package controller;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import model.Transaction;
import model.User;
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
import service.MockApiService;

/**
 * This class contains all the API routes for the application.
 */
@RestController
public class RouteController {

  private static final Logger LOGGER = Logger.getLogger(RouteController.class.getName());
  private final MockApiService mockApiService;

  public RouteController(MockApiService mockApiService) {
    this.mockApiService = mockApiService;
  }

  /**
   * Displays the home page listing all users.
   *
   * @return A {@code ResponseEntity} containing HTML text with an HTTP 200 response.
   */
  @GetMapping({"/", "/index"})
  public ResponseEntity<String> index() {
    LOGGER.info("GET /index called - Fetching home page with user list.");
    List<User> users = mockApiService.viewAllUsers();
    StringBuilder userList = new StringBuilder();
    
    if (users.isEmpty()) {
      LOGGER.info("No users found in database.");
      userList.append("No users found.");
    } else {
      LOGGER.info("Fetched " + users.size() + " user(s) for index page.");
      userList.append("Existing Users:<br>");
      for (User user : users) {
        userList.append(String.format("- <a href='/users/%s'>%s</a> | %s | Budget: $%.2f<br>", 
            user.getUserId(), user.getUsername(), user.getEmail(), user.getBudget()));
      }
    }
    
    String html = "<html><body>"
            + "<h1>Welcome to the Personal Finance Tracker</h1>"
            + "<p>" + userList.toString() + "</p>"
            + "</body></html>";
    
    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
  }

  // ---------------------------------------------------------------------------
  // User Management
  // ---------------------------------------------------------------------------

  /**
   * Returns all users.
   *
   * @return A {@code List} of {@code User} objects.
   */
  @GetMapping("/users")
  public List<User> getAllUsers() {
    LOGGER.info("GET /users called - Retrieving all users.");
    return mockApiService.viewAllUsers();
  }

  /**
   * Returns the details of a specific user.
   *
   * @param userId A {@code UUID} representing the unique identifier of the user.
   *
   * @return A {@code ResponseEntity} containing the {@code User} object with an
   *         HTTP 200 response if found
   * @throws NoSuchElementException if the user does not exist.
   */
  @GetMapping("/users/{userId}")
  public ResponseEntity<User> getUser(@PathVariable UUID userId) {
    LOGGER.info("GET /users/" + userId + " called - Fetching user details.");
    return mockApiService.getUser(userId)
        .map(user -> {
          LOGGER.info("User found: " + user.getUsername());
          return ResponseEntity.ok(user);
        })
        .orElseThrow(() -> {
          LOGGER.warning("User not found with ID: " + userId);
          return new NoSuchElementException("User " + userId + " not found");
        });
  }

  /**
   * Creates a new user using a JSON payload.
   *
   * @param user A {@code User} object to be created.
   *
   * @return A {@code ResponseEntity} containing the saved {@code User} with an
   *         HTTP 201 Created response.
   */
  @PostMapping(value = "/users",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<User> createUserJson(@RequestBody User user) {
    LOGGER.info("POST /users called - Creating new user via JSON: " + user.getUsername());
    User saved = mockApiService.addUser(user);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
  }

  /**
   * Creates a new user through an HTML form submission.
   *
   * @param username A {@code String} containing the user's name.
   * @param email    A {@code String} containing the user's email.
   * @param budget   A {@code double} representing the user's initial budget.
   *
   * @return A {@code ResponseEntity} containing an HTML success message with an
   *         HTTP 201 Created response.
   */
  @PostMapping(value = "/users/form", 
                consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> createUserFromFormHtml(
          @RequestParam String username,
          @RequestParam String email,
          @RequestParam double budget) {

    LOGGER.info("POST /users/form called - Creating user via form: " + username);
    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setBudget(budget);
    
    User saved = mockApiService.addUser(user);
    LOGGER.info("User created successfully via form. ID: " + saved.getUserId());
    
    String html = "<html><body>"
            + "<h2>User Created Successfully!</h2>"
            + "<p><strong>User ID:</strong> " + saved.getUserId() + "</p>"
            + "<p><strong>Username:</strong> " + saved.getUsername() + "</p>"
            + "<p><strong>Email:</strong> " + saved.getEmail() + "</p>"
            + "<p><strong>Budget:</strong> $" + String.format("%.2f", saved.getBudget()) + "</p>"
            + "</body></html>";
    
    return ResponseEntity.status(HttpStatus.CREATED).body(html);
  }

  /**
   * Updates an existing user with JSON data.
   *
   * @param userId A {@code UUID} representing the user's ID.
   * @param user   A {@code User} object containing updated data.
   *
   * @return A {@code ResponseEntity} with the updated {@code User} and an
   *         HTTP 200 response if found
   * @throws NoSuchElementException if the user does not exist.
   */
  @PutMapping(value = "/users/{userId}",
              consumes = MediaType.APPLICATION_JSON_VALUE,
              produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<User> updateUserJson(
          @PathVariable UUID userId,
          @RequestBody User user) {

    LOGGER.info("PUT /users/" + userId + " called - Updating user via JSON.");
    Optional<User> existingUser = mockApiService.getUser(userId);
    if (!existingUser.isPresent()) {
      LOGGER.warning("Cannot update user - not found: " + userId);
      throw new NoSuchElementException("User " + userId + " not found");
    }
    
    // For now, delete and recreate since we don't have update in service
    mockApiService.deleteUser(userId);
    User saved = mockApiService.addUser(user);
    saved.setUserId(userId); // Keep the same user ID

    LOGGER.info("User updated successfully. ID: " + userId);
    return ResponseEntity.ok(saved);
  }

  /**
   * Updates an existing user using an HTML form submission.
   *
   * @param userId   A {@code UUID} representing the user’s ID.
   * @param username A {@code String} containing the updated username.
   * @param email    A {@code String} containing the updated email address.
   * @param budget   A {@code double} containing the new budget.
   *
   * @return A {@code ResponseEntity} containing an HTML confirmation page with an
   *         HTTP 200 response.
   * @throws NoSuchElementException if the user does not exist.
   */

  @PostMapping(value = "/users/{userId}/update-form", 
                consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> updateUserFromFormHtml(
          @PathVariable UUID userId,
          @RequestParam String username,
          @RequestParam String email,
          @RequestParam double budget) {

    LOGGER.info("POST /users/" + userId + "/update-form called - Updating user via form.");
    Optional<User> existingUser = mockApiService.getUser(userId);
    if (!existingUser.isPresent()) {
      LOGGER.warning("Cannot update form user - not found: " + userId);
      throw new NoSuchElementException("User " + userId + " not found");
    }
    
    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setBudget(budget);
    
    mockApiService.deleteUser(userId);
    User saved = mockApiService.addUser(user);
    saved.setUserId(userId);

    LOGGER.info("User updated successfully via form. ID: " + userId);
    
    String html = "<html><body>" 
            + "<h2>User Updated Successfully!</h2>" 
            + "<p><strong>User ID:</strong> " + saved.getUserId() + "</p>" 
            + "<p><strong>Username:</strong> " + saved.getUsername() + "</p>" 
            + "<p><strong>Email:</strong> " + saved.getEmail() + "</p>" 
            + "<p><strong>Budget:</strong> $" + String.format("%.2f", saved.getBudget()) + "</p>" 
            + "</body></html>";
    return ResponseEntity.ok().body(html);
  }

  /**
   * Displays a simple HTML form for creating a new user.
   *
   * @return An HTML form string.
   */
  @GetMapping(value = "/users/create-form", produces = MediaType.TEXT_HTML_VALUE)
  public String showCreateUserForm() {
    LOGGER.info("GET /users/create-form called - Displaying user creation form.");
    return "<html><body>"
            + "<h2>Create New User</h2>"
            + "<form action='/users/form' method='post'>"
            + "Username: <input type='text' name='username' required><br><br>"
            + "Email: <input type='email' name='email' required><br><br>"
            + "Budget: <input type='number' name='budget' step='0.01' required><br><br>"
            + "<input type='submit' value='Create User'>"
            + "</body></html>";
  }

  /**
   * Displays an editable HTML form populated with an existing user’s information.
   *
   * @param userId A {@code UUID} representing the user to edit.
   *
   * @return An HTML form string if the user exists.
   * @throws NoSuchElementException if the user does not exist.
   */
  @GetMapping(value = "/users/{userId}/edit-form", produces = MediaType.TEXT_HTML_VALUE)
  public String showEditUserForm(@PathVariable UUID userId) {
    LOGGER.info("GET /users/" + userId + "/edit-form called - Displaying edit form.");
    Optional<User> userOpt = mockApiService.getUser(userId);
    if (!userOpt.isPresent()) {
      LOGGER.warning("Cannot display edit form - user not found: " + userId);
      throw new NoSuchElementException("User " + userId + " not found");
    }
    
    User user = userOpt.get();
    LOGGER.info("Edit form displayed for user: " + user.getUsername());
    return "<html><body>"
            + "<h2>Edit User</h2>"
            + "<form action='/users/" + userId + "/update-form' method='post'>"
            + "Username: <input type='text' name='username' value='" + user.getUsername() 
            + "' required><br><br>"
            + "Email: <input type='email' name='email' value='" + user.getEmail() 
            + "' required><br><br>"
            + "Budget: <input type='number' name='budget' step='0.01' value='" 
            + user.getBudget() + "' required><br><br>"
            + "<input type='submit' value='Update User'>"
            + "</form>"
            + "</body></html>";
  }

  /**
   * Deletes a user by ID using a JSON API request.
   *
   * @param userId A {@code UUID} representing the user to delete.
   *
   * @return A {@code ResponseEntity} containing a JSON confirmation object with an
   *         HTTP 200 response if successful.
   * @throws NoSuchElementException if the user does not exist.
   */
  @DeleteMapping("/users/{userId}")
  public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable UUID userId) {
    LOGGER.info("DELETE /users/" + userId + " called - Deleting user.");
    boolean deleted = mockApiService.deleteUser(userId);
    if (!deleted) {
      LOGGER.warning("User not found or already deleted: " + userId);
      throw new NoSuchElementException("User " + userId + " not found");
    }
    LOGGER.info("User deleted successfully: " + userId);
    return ResponseEntity.ok(Map.of("deleted", true, "userId", userId));
  }

  /**
   * Deletes a user through a GET request (intended for quick browser testing).
   *
   * @param userId A {@code UUID} representing the user to delete.
   *
   * @return A plain-text success message with HTTP 200 response if successful.
   * @throws NoSuchElementException if the user does not exist.
   */
  @GetMapping("/deleteuser/{userId}")
  public String deleteUserViaGet(@PathVariable UUID userId) {
    LOGGER.info("GET /deleteuser/" + userId + " called - Deleting user via GET.");
    boolean deleted = mockApiService.deleteUser(userId);
    if (!deleted) {
      LOGGER.warning("Cannot delete via GET - user not found: " + userId);
      throw new NoSuchElementException("User " + userId + " not found");
    }
    LOGGER.info("User deleted successfully via GET: " + userId);
    return "User deleted successfully";
  }

  // ---------------------------------------------------------------------------
  // Transaction Management
  // ---------------------------------------------------------------------------

  /**
   * Returns all transactions belonging to a specific user.
   *
   * @param userId A {@code UUID} representing the user whose transactions are requested.
   *
   * @return A {@code ResponseEntity} containing a {@code List<Transaction>} with an
   *         HTTP 200 OK response if successful, an HTTP 404 Not Found if the user does not exist,
   *         or an HTTP 500 Internal Server Error if an exception occurs.
   */
  @GetMapping("/users/{userId}/transactions")
  public ResponseEntity<?> getUserTransactions(@PathVariable UUID userId) {
    LOGGER.info("GET /users/" + userId + "/transactions called - Fetching all transactions.");
    try {
      if (!mockApiService.getUser(userId).isPresent()) {
        LOGGER.warning("User not found for transaction listing: " + userId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("Error: User " + userId + " not found");
      }
      List<Transaction> transactions = mockApiService.getTransactionsByUser(userId);
      LOGGER.info("Retrieved " + transactions.size() + " transactions for user " + userId);
      return ResponseEntity.ok(transactions);
    } catch (Exception e) {
      LOGGER.severe("Error retrieving transactions for user " + userId + ": " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Error retrieving transactions: " + e.getMessage());
    }
  }

  /**
   * Returns the details of a specific transaction belonging to a user.
   *
   * @param userId        A {@code UUID} representing the user who owns the transaction.
   * @param transactionId A {@code UUID} representing the unique identifier of the transaction.
   *
   * @return A {@code ResponseEntity} containing the {@code Transaction} object with
   *         HTTP 200 OK if found.
   * @throws NoSuchElementException if the user or transaction does not exist.
   */
  @GetMapping("/users/{userId}/transactions/{transactionId}")
  public ResponseEntity<Transaction> getTransaction(
          @PathVariable UUID userId, 
          @PathVariable UUID transactionId) {

    LOGGER.info("GET /users/" + userId + "/transactions/"
        + transactionId + " called - Fetching transaction details.");
    if (!mockApiService.getUser(userId).isPresent()) {
      LOGGER.warning("Cannot fetch transaction - user not found: " + userId);
      throw new NoSuchElementException("User " + userId + " not found");
    }
    return mockApiService.getTransaction(transactionId)
            .filter(tx -> tx.getUserId().equals(userId))
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new NoSuchElementException("Transaction "
            + transactionId + " not found for user " + userId));
  }

  /**
   * Creates a new transaction for a user using a JSON payload.
   *
   * @param userId      A {@code UUID} representing the user who owns the transaction.
   * @param transaction A {@code Transaction} object containing the transaction details.
   *
   * @return A {@code ResponseEntity} containing the saved {@code Transaction} with
   *         HTTP 201 Created.
   * @throws NoSuchElementException if the user does not exist.
   */
  @PostMapping(value = "/users/{userId}/transactions",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Transaction> createTransactionJson(
          @PathVariable UUID userId,
          @RequestBody Transaction transaction) {

    LOGGER.info("POST /users/" + userId
        + "/transactions called - Creating new transaction via JSON.");

    if (!mockApiService.getUser(userId).isPresent()) {
      LOGGER.warning("Cannot create transaction - user not found: " + userId);
      throw new NoSuchElementException("User " + userId + " not found");
    }
    
    // Ensure transaction belongs to correct user
    transaction.setUserId(userId);
    Transaction saved = mockApiService.addTransaction(transaction);
    LOGGER.info("Transaction created successfully for user "
        + userId + ": " + saved.getTransactionId());
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
  }

  /**
   * Creates a new transaction for a user via HTML form submission.
   *
   * @param userId      A {@code UUID} representing the user who owns the transaction.
   * @param description A {@code String} describing the transaction.
   * @param amount      A {@code double} representing the transaction amount.
   * @param category    A {@code String} specifying the transaction category.
   *
   * @return A {@code ResponseEntity} containing an HTML confirmation page with an
   *         HTTP 201 Created response if successful, an HTTP 404 Not Found if the user does not
   *         exist, or an HTTP 500 Internal Server Error if an exception occurs.
   */
  @PostMapping(value = "/users/{userId}/transactions/form", 
              consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
              produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> createTransactionFromFormHtml(
          @PathVariable UUID userId,
          @RequestParam String description,
          @RequestParam double amount,
          @RequestParam String category) {

    LOGGER.info("POST /users/" + userId
        + "/transactions/form called - Creating transaction via form.");

    try {
      if (!mockApiService.getUser(userId).isPresent()) {
        LOGGER.warning("Cannot create transaction form - user not found: " + userId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("Error: User " + userId + " not found");
      }
      
      Transaction transaction = new Transaction(userId, amount, category, description);
      Transaction saved = mockApiService.addTransaction(transaction);

      LOGGER.info("Transaction created successfully via form for user " + userId
          + ": " + saved.getTransactionId());
      
      String html = "<html><body>"
              + "<h2>Transaction Created Successfully!</h2>"
              + "<p><strong>Description:</strong> " + saved.getDescription() + "</p>"
              + "<p><strong>Amount:</strong> $" + String.format("%.2f", saved.getAmount()) + "</p>"
              + "<p><strong>Category:</strong> " + saved.getCategory() + "</p>"
              + "<br>"
              + "</body></html>";
      
      return ResponseEntity.status(HttpStatus.CREATED).body(html);
    } catch (Exception e) {
      LOGGER.severe("Error creating transaction via form for user "
          + userId + ": " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Error creating transaction: " + e.getMessage());
    }
  }

  /**
   * Updates an existing transaction using a JSON payload.
   *
   * @param userId        A {@code UUID} representing the user who owns the transaction.
   * @param transactionId A {@code UUID} representing the transaction to update.
   * @param updates       A {@code Map<String, Object>} containing the fields to update.
   *
   * @return A {@code ResponseEntity} containing the updated {@code Transaction} with an
   *         HTTP 200 OK response if successful.
   * @throws NoSuchElementException if the user does not exist, or the transaction does not exist
   *         or does not belong to the user.
   */
  @PutMapping(value = "/users/{userId}/transactions/{transactionId}",
              consumes = MediaType.APPLICATION_JSON_VALUE,
              produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Transaction> updateTransactionJson(
          @PathVariable UUID userId,
          @PathVariable UUID transactionId,
          @RequestBody Map<String, Object> updates) {

    LOGGER.info("PUT /users/" + userId + "/transactions/"
        + transactionId + " called - Updating transaction.");

    if (!mockApiService.getUser(userId).isPresent()) {
      LOGGER.warning("Cannot update transaction - user not found: " + userId);
      throw new NoSuchElementException("User " + userId + " not found");
    }
    
    Optional<Transaction> existing = mockApiService.getTransaction(transactionId);
    if (!existing.isPresent() 
        || !existing.get().getUserId().equals(userId)) {
      LOGGER.warning("Cannot update - transaction not found: "
          + transactionId + " for user " + userId);
      throw new NoSuchElementException("Transaction "
      + transactionId + " not found for user " + userId);
    }
    
    Optional<Transaction> updated = mockApiService.updateTransaction(transactionId, updates);
    if (!updated.isPresent()) {
      LOGGER.warning("Transaction update failed: " + transactionId);
      throw new NoSuchElementException("Transaction " + transactionId + " not found");
    }

    LOGGER.info("Transaction updated successfully: " + transactionId + " for user " + userId);
    return ResponseEntity.ok(updated.get());
  }

  /**
   * Displays a simple HTML form for creating a new transaction for a specified user.
   *
   * @param userId A {@code UUID} representing the user for whom the transaction will be created.
   *
   * @return An HTML form string for transaction creation.
   * @throws NoSuchElementException if the user does not exist.
   */
  @GetMapping(value = "/users/{userId}/transactions/create-form", 
        produces = MediaType.TEXT_HTML_VALUE)
  public String showCreateTransactionForm(@PathVariable UUID userId) {

    LOGGER.info("GET /users/" + userId
        + "/transactions/create-form called - Displaying transaction form.");
    if (!mockApiService.getUser(userId).isPresent()) {
      LOGGER.warning("Cannot display transaction form - user not found: " + userId);
      throw new NoSuchElementException("User " + userId + " not found");
    }

    LOGGER.info("Transaction form displayed successfully for user " + userId);
    return "<html><body>"
            + "<h2>Create New Transaction</h2>"
            + "<form action='/users/" + userId + "/transactions/form' method='post'>"
            + "Description: <input type='text' name='description' required><br><br>"
            + "Amount: <input type='number' name='amount' step='0.01' required><br><br>"
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
            + "</form>"
            + "</body></html>";
  }

  /**
   * Deletes a transaction for a specified user via a JSON API request.
   *
   * @param userId        A {@code UUID} representing the user who owns the transaction.
   * @param transactionId A {@code UUID} representing the transaction to delete.
   *
   * @return A {@code ResponseEntity} containing a JSON confirmation object with
   *         HTTP 200 OK if successful.
   * @throws NoSuchElementException if the user or transaction does not exist.
   */
  @DeleteMapping("/users/{userId}/transactions/{transactionId}")
  public ResponseEntity<Map<String, Object>> deleteTransaction(
          @PathVariable UUID userId, 
          @PathVariable UUID transactionId) {

    LOGGER.info("DELETE /users/" + userId + "/transactions/"
        + transactionId + " called - Deleting transaction.");

    if (!mockApiService.getUser(userId).isPresent()) {
      LOGGER.warning("Cannot delete transaction - user not found: " + userId);
      throw new NoSuchElementException("User " + userId + " not found");
    }
    
    Optional<Transaction> existing = mockApiService.getTransaction(transactionId);
    if (!existing.isPresent() 
        || !existing.get().getUserId().equals(userId)) {
      LOGGER.warning("Cannot delete transaction - not found or mismatched user. ID: "
          + transactionId);
      throw new NoSuchElementException("Transaction "
          + transactionId + " not found for user " + userId);
    }
    
    boolean deleted = mockApiService.deleteTransaction(transactionId);
    if (!deleted) {
      LOGGER.warning("Transaction deletion failed for ID: " + transactionId);
      throw new NoSuchElementException("Transaction " + transactionId + " not found");
    }
    LOGGER.info("Transaction deleted successfully for user " + userId + ": " + transactionId);
    return ResponseEntity.ok(Map.of("deleted", true,
        "userId", userId, "transactionId", transactionId));
  }

  /**
   * Deletes a transaction using a GET request (for quick browser testing).
   *
   * @param userId        A {@code UUID} representing the user who owns the transaction.
   * @param transactionId A {@code UUID} representing the transaction to delete.
   *
   * @return A plain-text confirmation message with HTTP 200 OK if successful,
   *         or an error message string if the user or transaction does not exist or deletion fails.
   */

  @GetMapping("/users/{userId}/deletetransaction/{transactionId}")
  public String deleteTransactionViaGet(
          @PathVariable UUID userId, 
          @PathVariable UUID transactionId) {

    LOGGER.info("GET /users/" + userId + "/deletetransaction/"
        + transactionId + " called - Deleting via GET.");
    if (!mockApiService.getUser(userId).isPresent()) {
      LOGGER.warning("Cannot delete via GET - user not found: " + userId);
      return "Error: User " + userId + " not found";
    }
    
    Optional<Transaction> existing = mockApiService.getTransaction(transactionId);
    if (!existing.isPresent() || !existing.get().getUserId().equals(userId)) {
      LOGGER.warning("Cannot delete via GET - transaction not found: " + transactionId);
      return "Error: Transaction " + transactionId + " not found for user " + userId;
    }
    
    boolean deleted = mockApiService.deleteTransaction(transactionId);
    if (!deleted) {
      LOGGER.warning("Transaction deletion via GET failed: " + transactionId);
      return "Error: Failed to delete transaction " + transactionId;
    }

    LOGGER.info("Transaction deleted successfully via GET for user "
        + userId + ": " + transactionId);
    return "Transaction deleted successfully!";
  }

  // ---------------------------------------------------------------------------
  // Budget & Analytics Endpoints
  // ---------------------------------------------------------------------------

  /**
   * Displays the budget management page for a specific user, including current budget,
   * total spending, weekly spending, and quick access to related reports.
   *
   * @param userId A {@code UUID} representing the user whose budget page is requested.
   *
   * @return A raw HTML string containing the budget management dashboard if the user exists.
   * @throws NoSuchElementException if the user is not found.
   */
  @GetMapping(value = "/users/{userId}/budget", produces = MediaType.TEXT_HTML_VALUE)
  public String budgetManagement(@PathVariable UUID userId) {

    LOGGER.info("GET /users/" + userId + "/budget called - Loading budget management page.");

    if (!mockApiService.getUser(userId).isPresent()) {
      LOGGER.warning("Cannot load budget page - user not found: " + userId);
      throw new NoSuchElementException("User " + userId + " not found");
    }
    
    User user = mockApiService.getUser(userId).get();
    Map<String, Object> budgetReport = mockApiService.getBudgetReport(userId);
    String weeklyTotal = String.format("%.2f", mockApiService.totalLast7Days(userId));

    LOGGER.info("Budget page loaded successfully for user: " + user.getUsername());
    return "<html><body>"
            + "<h1>Budget Management - " + user.getUsername() + "</h1>"
            
            + "<h2>Current Budget</h2>"
            + "<p><strong>Total Budget:</strong> $" 
            + String.format("%.2f", user.getBudget()) + "</p>"
            + "<p><strong>Total Spent:</strong> $" + budgetReport.get("totalSpent") + "</p>"
            + "<p><strong>Remaining:</strong> $" + budgetReport.get("remaining") + "</p>"
            + "<p><strong>Weekly Spending:</strong> $" + weeklyTotal + "</p>"
            
            + "<h2>Update Budget</h2>"
            + "<form action='/users/" + userId + "/update-budget' method='post'>"
            + "New Budget: <input type='number' name='budget' step='0.01' value='" 
            + user.getBudget() + "' required><br><br>"
            + "<input type='submit' value='Update Budget'>"
            + "</form>"
            
            + "<h2>Quick Reports</h2>"
            + "<ul>"
            + "<li><a href='/users/" + userId + "/weekly-summary'>Weekly Summary</a></li>"
            + "<li><a href='/users/" + userId + "/monthly-summary'>Monthly Summary</a></li>"
            + "<li><a href='/users/" + userId + "/budget-report'>Budget Report (JSON)</a></li>"
            + "</ul>"
            + "</body></html>";
  }

  /**
   * Updates a user's budget using a JSON payload.
   *
   * @param userId       A {@code UUID} representing the user whose budget is being updated.
   * @param budgetUpdate A {@code Map<String, Object>} containing the new budget values.
   *
   * @return A {@code ResponseEntity} containing the updated budget report if successful.
   * @throws NoSuchElementException if the user does not exist.
   */
  @PutMapping(value = "/users/{userId}/budget",
              consumes = MediaType.APPLICATION_JSON_VALUE,
              produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> updateBudgetJson(
          @PathVariable UUID userId,
          @RequestBody Map<String, Object> budgetUpdate) {

    LOGGER.info("PUT /users/" + userId + "/budget called - Updating budget via JSON.");

    if (!mockApiService.getUser(userId).isPresent()) {
      LOGGER.warning("Cannot update budget - user not found: " + userId);
      throw new NoSuchElementException("User " + userId + " not found");
    }
    mockApiService.setBudgets(userId, budgetUpdate);
    LOGGER.info("Budget updated successfully for user " + userId + " with data: " + budgetUpdate);
    return ResponseEntity.ok(mockApiService.getBudgetReport(userId));
  }

  /**
   * Updates a user's budget via HTML form submission.
   *
   * @param userId A {@code UUID} representing the user whose budget is being updated.
   * @param budget A {@code double} representing the new budget amount.
   *
   * @return A {@code ResponseEntity} containing an HTML confirmation message with HTTP 200 OK
   *         if successful
   * @throws NoSuchElementException if the user does not exist.
   */
  @PostMapping(value = "/users/{userId}/update-budget", 
                consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> updateBudget(
          @PathVariable UUID userId,
          @RequestParam double budget) {

    LOGGER.info("POST /users/" + userId + "/update-budget called - Updating budget via HTML form.");

    if (!mockApiService.getUser(userId).isPresent()) {
      LOGGER.warning("Cannot update budget via form - user not found: " + userId);
      throw new NoSuchElementException("User " + userId + " not found");
    }
    
    mockApiService.setBudgets(userId, Map.of("budget", budget));
    LOGGER.info("Budget successfully updated via form for user " + userId + " to $" + budget);
    
    String html = "<html><body>"
            + "<h2>Budget Updated Successfully!</h2>"
            + "<p><strong>New Budget:</strong> $" 
            + String.format("%.2f", budget) + "</p>"
            + "</body></html>";
    
    return ResponseEntity.ok().body(html);
  }

  /**
   * Displays a weekly spending summary for a specific user.
   *
   * @param userId A {@code UUID} representing the user whose weekly summary is requested.
   *
   * @return A raw HTML string containing a table of transactions and spending totals
   *         with an HTTP 200 OK response if the user exists.
   * @throws NoSuchElementException if the user with the given {@code userId} does not exist.
   */
  @GetMapping(value = "/users/{userId}/weekly-summary", produces = MediaType.TEXT_HTML_VALUE)
  public String weeklySummary(@PathVariable UUID userId) {

    LOGGER.info("GET /users/" + userId + "/weekly-summary called - Generating weekly summary.");

    if (!mockApiService.getUser(userId).isPresent()) {
      LOGGER.warning("Cannot generate weekly summary - user not found: " + userId);
      throw new NoSuchElementException("User " + userId + " not found");
    }
    
    User user = mockApiService.getUser(userId).get();
    List<Transaction> weeklyTransactions = mockApiService.weeklySummary(userId);
    double weeklyTotal = mockApiService.totalLast7Days(userId);

    LOGGER.info("Weekly summary generated for user " + user.getUsername()
        + " with " + weeklyTransactions.size() + " transactions.");

    StringBuilder transactionsHtml = new StringBuilder();
    if (weeklyTransactions.isEmpty()) {
      transactionsHtml.append("<p>No transactions in the last 7 days.</p>");
    } else {
      transactionsHtml.append("<table border='1' style='"
          + "border-collapse: collapse; width: 100%;'>");
      transactionsHtml.append("<tr><th>Description</th>" 
          + "<th>Amount</th><th>Category</th><th>Date</th></tr>");
      for (Transaction tx : weeklyTransactions) {
        transactionsHtml.append("<tr>")
            .append("<td>").append(tx.getDescription()).append("</td>")
            .append("<td>$").append(String.format("%.2f", 
              tx.getAmount())).append("</td>")
            .append("<td>").append(tx.getCategory()).append("</td>")
            .append("<td>").append(tx.getDate()).append("</td>")
            .append("</tr>");
      }
      transactionsHtml.append("</table>");
    }
    return "<html><body>"
            + "<h1>Weekly Summary - " + user.getUsername() + "</h1>"
            + "<p><strong>Total Spent Last 7 Days:</strong> $" 
            + String.format("%.2f", weeklyTotal) + "</p>"
            + "<h2>Recent Transactions</h2>"
            + transactionsHtml.toString()
            + "</body></html>";
  }

  /**
   * Displays a monthly spending summary for a specific user.
   *
   * @param userId A {@code UUID} representing the user whose monthly summary is requested.
   *
   * @return A raw HTML string containing the monthly summary report
   *         with an HTTP 200 OK response if the user exists.
   * @throws NoSuchElementException if the user with the given {@code userId} does not exist,
   */
  @GetMapping(value = "/users/{userId}/monthly-summary", produces = MediaType.TEXT_HTML_VALUE)
  public String monthlySummary(@PathVariable UUID userId) {

    LOGGER.info("GET /users/" + userId + "/monthly-summary called - Generating monthly summary.");

    if (!mockApiService.getUser(userId).isPresent()) {
      LOGGER.warning("Cannot generate monthly summary - user not found: " + userId);
      throw new NoSuchElementException("User " + userId + " not found");
    }
    
    String summary = mockApiService.getMonthlySummary(userId);
    LOGGER.info("Monthly summary generated successfully for user " + userId);
    return "<html><body>"
            + "<h1>Monthly Summary</h1>"
            + "<pre>" + summary + "</pre>"
            + "</body></html>";
  }

  /**
   * Returns a JSON-formatted budget report for a specific user.
   *
   * @param userId A {@code UUID} representing the user whose budget report is requested.
   *
   * @return A {@code ResponseEntity} containing a JSON budget report with an HTTP 200 OK response
   *         if the user exists.
   * @throws NoSuchElementException if the user with the given {@code userId} does not exist.
   */
  @GetMapping(value = "/users/{userId}/budget-report", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> budgetReport(@PathVariable UUID userId) {

    LOGGER.info("GET /users/"
        + userId + "/budget-report called - Retrieving budget report (JSON).");

    if (!mockApiService.getUser(userId).isPresent()) {
      LOGGER.warning("Cannot retrieve budget report - user not found: " + userId);
      throw new NoSuchElementException("User " + userId + " not found");
    }

    LOGGER.info("Budget report retrieved successfully for user " + userId);
    return ResponseEntity.ok(mockApiService.getBudgetReport(userId));
  }

  // ---------------------------------------------------------------------------
  // Exception handlers
  // ---------------------------------------------------------------------------

  /**
   * Handles NoSuchElementException thrown when a requested resource
   * (such as a user or transaction) does not exist.
   *
   * @param ex The {@code NoSuchElementException} that was thrown.
   *
   * @return A {@code ResponseEntity} containing a JSON object with an error message
   *         and an HTTP 404 Not Found status.
   */
  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
  }

  /**
   * Handles IllegalArgumentException thrown when a request contains invalid data
   * or parameters.
   *
   * @param ex The {@code IllegalArgumentException} that was thrown.
   *
   * @return A {@code ResponseEntity} containing a JSON object with an error message
   *         and an HTTP 400 Bad Request status.
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", ex.getMessage()));
  }
}