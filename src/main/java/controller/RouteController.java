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

/** Endpoints for API service. */
@RestController
public class RouteController {

  private static final Logger LOGGER = Logger.getLogger(RouteController.class.getName());
  private final MockApiService mockApiService;

  public RouteController(MockApiService mockApiService) {
    this.mockApiService = mockApiService;
  }

  /** Home page - shows instructions with user creation options. */
  @GetMapping({"/", "/index"})
  public ResponseEntity<String> index() {
    LOGGER.info("Accessed index route.");
    
    List<User> users = mockApiService.viewAllUsers();
    StringBuilder userList = new StringBuilder();
    
    if (users.isEmpty()) {
      userList.append("No users found.");
    } else {
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

  /** Get all users. */
  @GetMapping("/users")
  public List<User> getAllUsers() {
    return mockApiService.viewAllUsers();
  }

  /** Get specific user. */
  @GetMapping("/users/{userId}")
  public ResponseEntity<User> getUser(@PathVariable UUID userId) {
    return mockApiService.getUser(userId)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new NoSuchElementException("User " + userId + " not found"));
  }

  /** Create new user - JSON version for curl. */
  @PostMapping(value = "/users", 
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<User> createUserJson(@RequestBody User user) {
    User saved = mockApiService.addUser(user);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
  }

  /** Create new user - HTML FORM that returns HTML response. */
  @PostMapping(value = "/users/form", 
                consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> createUserFromFormHtml(
          @RequestParam String username,
          @RequestParam String email,
          @RequestParam double budget) {
      
    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setBudget(budget);
    
    User saved = mockApiService.addUser(user);
    
    String html = "<html><body>"
            + "<h2>User Created Successfully!</h2>"
            + "<p><strong>User ID:</strong> " + saved.getUserId() + "</p>"
            + "<p><strong>Username:</strong> " + saved.getUsername() + "</p>"
            + "<p><strong>Email:</strong> " + saved.getEmail() + "</p>"
            + "<p><strong>Budget:</strong> $" + String.format("%.2f", saved.getBudget()) + "</p>"
            + "</body></html>";
    
    return ResponseEntity.status(HttpStatus.CREATED).body(html);
  }

  /** Update user - JSON version for curl. */
  @PutMapping(value = "/users/{userId}",
              consumes = MediaType.APPLICATION_JSON_VALUE,
              produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<User> updateUserJson(
          @PathVariable UUID userId,
          @RequestBody User user) {
      
    Optional<User> existingUser = mockApiService.getUser(userId);
    if (!existingUser.isPresent()) {
      throw new NoSuchElementException("User " + userId + " not found");
    }
    
    // For now, delete and recreate since we don't have update in service
    mockApiService.deleteUser(userId);
    User saved = mockApiService.addUser(user);
    saved.setUserId(userId); // Keep the same user ID
    
    return ResponseEntity.ok(saved);
  }

  /** Update user - HTML FORM that returns HTML response. */
  @PostMapping(value = "/users/{userId}/update-form", 
                consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> updateUserFromFormHtml(
          @PathVariable UUID userId,
          @RequestParam String username,
          @RequestParam String email,
          @RequestParam double budget) {
      
    Optional<User> existingUser = mockApiService.getUser(userId);
    if (!existingUser.isPresent()) {
      throw new NoSuchElementException("User " + userId + " not found");
    }
    
    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setBudget(budget);
    
    mockApiService.deleteUser(userId);
    User saved = mockApiService.addUser(user);
    saved.setUserId(userId);
    
    String html = "<html><body>" 
            + "<h2>User Updated Successfully!</h2>" 
            + "<p><strong>User ID:</strong> " + saved.getUserId() + "</p>" 
            + "<p><strong>Username:</strong> " + saved.getUsername() + "</p>" 
            + "<p><strong>Email:</strong> " + saved.getEmail() + "</p>" 
            + "<p><strong>Budget:</strong> $" + String.format("%.2f", saved.getBudget()) + "</p>" 
            + "</body></html>";
    return ResponseEntity.ok().body(html);
  }

  /** Simple HTML form for creating users. */
  @GetMapping(value = "/users/create-form", produces = MediaType.TEXT_HTML_VALUE)
  public String showCreateUserForm() {
    return "<html><body>"
            + "<h2>Create New User</h2>"
            + "<form action='/users/form' method='post'>"
            + "Username: <input type='text' name='username' required><br><br>"
            + "Email: <input type='email' name='email' required><br><br>"
            + "Budget: <input type='number' name='budget' step='0.01' required><br><br>"
            + "<input type='submit' value='Create User'>"
            + "</body></html>";
  }

  /** Edit user form. */
  @GetMapping(value = "/users/{userId}/edit-form", produces = MediaType.TEXT_HTML_VALUE)
  public String showEditUserForm(@PathVariable UUID userId) {
    Optional<User> userOpt = mockApiService.getUser(userId);
    if (!userOpt.isPresent()) {
      throw new NoSuchElementException("User " + userId + " not found");
    }
    
    User user = userOpt.get();
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

  /** Delete user - JSON version. */
  @DeleteMapping("/users/{userId}")
  public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable UUID userId) {
    boolean deleted = mockApiService.deleteUser(userId);
    if (!deleted) {
      throw new NoSuchElementException("User " + userId + " not found");
    }
    return ResponseEntity.ok(Map.of("deleted", true, "userId", userId));
  }

  /** Delete user - GET version for browser. */
  @GetMapping("/deleteuser/{userId}")
  public String deleteUserViaGet(@PathVariable UUID userId) {
    boolean deleted = mockApiService.deleteUser(userId);
    if (!deleted) {
      throw new NoSuchElementException("User " + userId + " not found");
    }
    return "User deleted successfully";
  }

  // ---------------------------------------------------------------------------
  // Transaction Management
  // ---------------------------------------------------------------------------

  /** Get all transactions for a user. */
  @GetMapping("/users/{userId}/transactions")
  public ResponseEntity<?> getUserTransactions(@PathVariable UUID userId) {
    try {
      if (!mockApiService.getUser(userId).isPresent()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("Error: User " + userId + " not found");
      }
      List<Transaction> transactions = mockApiService.getTransactionsByUser(userId);
      return ResponseEntity.ok(transactions);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Error retrieving transactions: " + e.getMessage());
    }
  }

  /** Get specific transaction. */
  @GetMapping("/users/{userId}/transactions/{transactionId}")
  public ResponseEntity<Transaction> getTransaction(
          @PathVariable UUID userId, 
          @PathVariable UUID transactionId) {
    if (!mockApiService.getUser(userId).isPresent()) {
      throw new NoSuchElementException("User " + userId + " not found");
    }
    return mockApiService.getTransaction(transactionId)
            .filter(tx -> tx.getUserId().equals(userId))
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new NoSuchElementException("Transaction " 
            + transactionId + " not found for user " + userId));
  }

  /** Create transaction - JSON version for curl. */
  @PostMapping(value = "/users/{userId}/transactions",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Transaction> createTransactionJson(
          @PathVariable UUID userId,
          @RequestBody Transaction transaction) {
      
    if (!mockApiService.getUser(userId).isPresent()) {
      throw new NoSuchElementException("User " + userId + " not found");
    }
    
    // Ensure transaction belongs to correct user
    transaction.setUserId(userId);
    Transaction saved = mockApiService.addTransaction(transaction);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
  }

  /** Create transaction - HTML FORM version. */
  @PostMapping(value = "/users/{userId}/transactions/form", 
              consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
              produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> createTransactionFromFormHtml(
          @PathVariable UUID userId,
          @RequestParam String description,
          @RequestParam double amount,
          @RequestParam String category) {
      
    try {
      if (!mockApiService.getUser(userId).isPresent()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("Error: User " + userId + " not found");
      }
      
      Transaction transaction = new Transaction(userId, amount, category, description);
      Transaction saved = mockApiService.addTransaction(transaction);
      
      String html = "<html><body>"
              + "<h2>Transaction Created Successfully!</h2>"
              + "<p><strong>Description:</strong> " + saved.getDescription() + "</p>"
              + "<p><strong>Amount:</strong> $" + String.format("%.2f", saved.getAmount()) + "</p>"
              + "<p><strong>Category:</strong> " + saved.getCategory() + "</p>"
              + "<br>"
              + "</body></html>";
      
      return ResponseEntity.status(HttpStatus.CREATED).body(html);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Error creating transaction: " + e.getMessage());
    }
  }

  /** Update transaction - JSON version for curl. */
  @PutMapping(value = "/users/{userId}/transactions/{transactionId}",
              consumes = MediaType.APPLICATION_JSON_VALUE,
              produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Transaction> updateTransactionJson(
          @PathVariable UUID userId,
          @PathVariable UUID transactionId,
          @RequestBody Map<String, Object> updates) {
      
    if (!mockApiService.getUser(userId).isPresent()) {
      throw new NoSuchElementException("User " + userId + " not found");
    }
    
    Optional<Transaction> existing = mockApiService.getTransaction(transactionId);
    if (!existing.isPresent() 
        || !existing.get().getUserId().equals(userId)) {
      throw new NoSuchElementException("Transaction " 
      + transactionId + " not found for user " + userId);
    }
    
    Optional<Transaction> updated = mockApiService.updateTransaction(transactionId, updates);
    if (!updated.isPresent()) {
      throw new NoSuchElementException("Transaction " + transactionId + " not found");
    }
    
    return ResponseEntity.ok(updated.get());
  }

  /** Simple HTML form for creating transactions. */
  @GetMapping(value = "/users/{userId}/transactions/create-form", 
        produces = MediaType.TEXT_HTML_VALUE)
  public String showCreateTransactionForm(@PathVariable UUID userId) {
    if (!mockApiService.getUser(userId).isPresent()) {
      throw new NoSuchElementException("User " + userId + " not found");
    }
    
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

  /** Delete transaction - JSON version. */
  @DeleteMapping("/users/{userId}/transactions/{transactionId}")
  public ResponseEntity<Map<String, Object>> deleteTransaction(
          @PathVariable UUID userId, 
          @PathVariable UUID transactionId) {
      
    if (!mockApiService.getUser(userId).isPresent()) {
      throw new NoSuchElementException("User " + userId + " not found");
    }
    
    Optional<Transaction> existing = mockApiService.getTransaction(transactionId);
    if (!existing.isPresent() 
        || !existing.get().getUserId().equals(userId)) {
      throw new NoSuchElementException("Transaction " 
          + transactionId + " not found for user " + userId);
    }
    
    boolean deleted = mockApiService.deleteTransaction(transactionId);
    if (!deleted) {
      throw new NoSuchElementException("Transaction " + transactionId + " not found");
    }
    return ResponseEntity.ok(Map.of("deleted", true,
        "userId", userId, "transactionId", transactionId));
  }

  /** Delete transaction - GET version for browser. */
  @GetMapping("/users/{userId}/deletetransaction/{transactionId}")
  public String deleteTransactionViaGet(
          @PathVariable UUID userId, 
          @PathVariable UUID transactionId) {
    
    if (!mockApiService.getUser(userId).isPresent()) {
      return "Error: User " + userId + " not found";
    }
    
    Optional<Transaction> existing = mockApiService.getTransaction(transactionId);
    if (!existing.isPresent() || !existing.get().getUserId().equals(userId)) {
      return "Error: Transaction " + transactionId + " not found for user " + userId;
    }
    
    boolean deleted = mockApiService.deleteTransaction(transactionId);
    if (!deleted) {
      return "Error: Failed to delete transaction " + transactionId;
    }
    
    return "Transaction deleted successfully!";
  }

  // ---------------------------------------------------------------------------
  // Budget & Analytics Endpoints
  // ---------------------------------------------------------------------------

  /** Budget management page. */
  @GetMapping(value = "/users/{userId}/budget", produces = MediaType.TEXT_HTML_VALUE)
  public String budgetManagement(@PathVariable UUID userId) {
    if (!mockApiService.getUser(userId).isPresent()) {
      throw new NoSuchElementException("User " + userId + " not found");
    }
    
    User user = mockApiService.getUser(userId).get();
    Map<String, Object> budgetReport = mockApiService.getBudgetReport(userId);
    String weeklyTotal = String.format("%.2f", mockApiService.totalLast7Days(userId));
    
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

  /** Update budget - JSON version for curl. */
  @PutMapping(value = "/users/{userId}/budget",
              consumes = MediaType.APPLICATION_JSON_VALUE,
              produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> updateBudgetJson(
          @PathVariable UUID userId,
          @RequestBody Map<String, Object> budgetUpdate) {
      
    if (!mockApiService.getUser(userId).isPresent()) {
      throw new NoSuchElementException("User " + userId + " not found");
    }
    mockApiService.setBudgets(userId, budgetUpdate);
    return ResponseEntity.ok(mockApiService.getBudgetReport(userId));
  }

  /** Update budget - HTML FORM version. */
  @PostMapping(value = "/users/{userId}/update-budget", 
                consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> updateBudget(
          @PathVariable UUID userId,
          @RequestParam double budget) {
      
    if (!mockApiService.getUser(userId).isPresent()) {
      throw new NoSuchElementException("User " + userId + " not found");
    }
    
    mockApiService.setBudgets(userId, Map.of("budget", budget));
    
    String html = "<html><body>"
            + "<h2>Budget Updated Successfully!</h2>"
            + "<p><strong>New Budget:</strong> $" 
            + String.format("%.2f", budget) + "</p>"
            + "</body></html>";
    
    return ResponseEntity.ok().body(html);
  }

  /** Weekly summary. */
  @GetMapping(value = "/users/{userId}/weekly-summary", produces = MediaType.TEXT_HTML_VALUE)
  public String weeklySummary(@PathVariable UUID userId) {
    if (!mockApiService.getUser(userId).isPresent()) {
      throw new NoSuchElementException("User " + userId + " not found");
    }
    
    User user = mockApiService.getUser(userId).get();
    List<Transaction> weeklyTransactions = mockApiService.weeklySummary(userId);
    double weeklyTotal = mockApiService.totalLast7Days(userId);
    
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

  /** Monthly summary. */
  @GetMapping(value = "/users/{userId}/monthly-summary", produces = MediaType.TEXT_HTML_VALUE)
  public String monthlySummary(@PathVariable UUID userId) {
    if (!mockApiService.getUser(userId).isPresent()) {
      throw new NoSuchElementException("User " + userId + " not found");
    }
    
    String summary = mockApiService.getMonthlySummary(userId);
    return "<html><body>"
            + "<h1>Monthly Summary</h1>"
            + "<pre>" + summary + "</pre>"
            + "</body></html>";
  }

  /** Budget report (JSON). */
  @GetMapping(value = "/users/{userId}/budget-report", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> budgetReport(@PathVariable UUID userId) {
    if (!mockApiService.getUser(userId).isPresent()) {
      throw new NoSuchElementException("User " + userId + " not found");
    }
    return ResponseEntity.ok(mockApiService.getBudgetReport(userId));
  }

  // ---------------------------------------------------------------------------
  // Exception handlers
  // ---------------------------------------------------------------------------

  /** Exception handler for handle not found. */
  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
  }

  /** Exception handler for invalid handle. */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", ex.getMessage()));
  }
}