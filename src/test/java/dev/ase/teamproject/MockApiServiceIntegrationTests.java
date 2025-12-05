package dev.ase.teamproject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.ase.teamproject.model.Transaction;
import dev.ase.teamproject.model.User;
import dev.ase.teamproject.service.MockApiService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration Tests for MockApiService.
 *
 * <p>These tests verify the integration between the MockApiService class and the
 * external PostgreSQL database hosted on GCP. They exercise:
 * <ul>
 *   <li>Database CRUD operations for User and Transaction entities</li>
 *   <li>Foreign key constraints between users and transactions tables</li>
 *   <li>Database triggers for auto-generated fields (timestamps, dates, UUIDs)</li>
 *   <li>PostgreSQL ENUM type handling for transaction categories</li>
 *   <li>Database aggregate functions and queries (SUM, COUNT, date filtering)</li>
 *   <li>Cascade delete operations</li>
 * </ul>
 *
 * <h2>External Integration Points</h2>
 * <ul>
 *   <li>MockApiService ↔ PostgreSQL Database (via JdbcTemplate)</li>
 *   <li>User table ↔ Transaction table (foreign key relationship)</li>
 *   <li>Application code ↔ Database constraints and validations</li>
 * </ul>
 *
 * <h2>Internal Integration Points</h2>
 * <ul>
 *   <li>MockApiService ↔ User model (data mapping via RowMapper)</li>
 *   <li>MockApiService ↔ Transaction model (data mapping via RowMapper)</li>
 *   <li>Service methods calling other service methods internally</li>
 * </ul>
 */
@ActiveProfiles("test")
@SpringBootTest
public class MockApiServiceIntegrationTests {

  @Autowired
  private MockApiService service;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private UUID userId;

  /**
   * Sets up a clean database state before each test.
   * Creates a base user for tests to use, ensuring isolation between test cases.
   *
   * <p><strong>Integration:</strong> Uses TRUNCATE CASCADE to test database
   * cascade delete functionality.
   */
  @BeforeEach
  public void setup() {
    // Clean tables
    jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");

    User user = new User();
    user.setUsername("user");
    user.setEmail("user@email.com");
    user.setBudget(100.0);

    User saved = service.addUser(user);
    userId = saved.getUserId();
  }

  // ===========================================================================
  // USER CRUD OPERATIONS - Testing User table integration
  // ===========================================================================

  /**
   * Tests retrieval of all users from the database.
   *
   * <p><strong>External Integration:</strong> Service → Database SELECT query
   */
  @Test
  public void testViewAllUsers() {
    List<User> users = service.viewAllUsers();
    assertThat(users).hasSize(1);
    assertThat(users.get(0).getUsername()).isEqualTo("user");
  }

  /**
   * Tests retrieval of a specific user by UUID.
   *
   * <p><strong>External Integration:</strong> Service → Database SELECT with WHERE clause
   */
  @Test
  public void testGetUser() {
    Optional<User> user = service.getUser(userId);
    assertThat(user).isPresent();
    assertThat(user.get().getUsername()).isEqualTo("user");
  }

  /**
   * Tests retrieval of non-existent user.
   *
   * <p><strong>External Integration:</strong> Service → Database returns empty result
   */
  @Test
  public void testGetUser_notFound() {
    UUID fakeId = UUID.randomUUID();

    Optional<User> user = service.getUser(fakeId);

    assertThat(user).isEmpty();
  }

  /**
   * Tests successful user creation with database-generated UUID.
   *
   * <p><strong>External Integration:</strong> Service → Database INSERT with RETURNING clause
   * to retrieve auto-generated user_id
   */
  @Test
  public void testAddUser_success() {
    User user = new User();
    user.setUsername("newUser");
    user.setEmail("test@email.com");
    user.setBudget(50.0);

    User saved = service.addUser(user);

    assertThat(saved.getUserId()).isNotNull();
    assertThat(service.getUser(saved.getUserId())).isPresent();
  }

  /**
   * Tests database unique constraint enforcement on username.
   *
   * <p><strong>External Integration:</strong> Service → Database unique constraint violation
   * handling (users_username_key constraint)
   */
  @Test
  public void testAddUser_duplicateUsername_fail() {
    User duplicate = new User();
    duplicate.setUsername("user");
    duplicate.setEmail("test@email.com");
    duplicate.setBudget(10.0);

    assertThatThrownBy(() -> service.addUser(duplicate))
        .isInstanceOf(Exception.class);
  }

  /**
   * Tests database unique constraint enforcement on email.
   *
   * <p><strong>External Integration:</strong> Service → Database unique constraint violation
   * handling (users_email_key constraint)
   */
  @Test
  public void testAddUser_duplicateEmail_fail() {
    User duplicate = new User();
    duplicate.setUsername("differentUser");
    duplicate.setEmail("user@email.com");  // Same email as setup user
    duplicate.setBudget(10.0);

    assertThatThrownBy(() -> service.addUser(duplicate))
        .isInstanceOf(Exception.class);
  }

  /**
   * Tests user deletion from database.
   *
   * <p><strong>External Integration:</strong> Service → Database DELETE operation
   * and verification that record no longer exists
   */
  @Test
  public void testDeleteUser() {
    boolean deleted = service.deleteUser(userId);
    assertThat(deleted).isTrue();
    assertThat(service.getUser(userId)).isEmpty();
  }

  /**
   * Tests deletion of non-existent user.
   *
   * <p><strong>External Integration:</strong> Service → Database DELETE returns 0 rows affected
   */
  @Test
  public void testDeleteUser_notFound() {
    UUID fakeId = UUID.randomUUID();

    boolean deleted = service.deleteUser(fakeId);

    assertThat(deleted).isFalse();
  }

  // ===========================================================================
  // TRANSACTION CRUD OPERATIONS - Testing Transaction table integration
  // ===========================================================================

  /**
   * Tests retrieval of all transactions from the database.
   *
   * <p><strong>External Integration:</strong> Service → Database SELECT query
   * with ORDER BY clause
   */
  @Test
  public void testViewAllTransactions() {
    // Add transactions for test user
    Transaction t1 = new Transaction(userId, 10.0, "FOOD", "first");
    Transaction t2 = new Transaction(userId, 20.0, "OTHER", "second");
    service.addTransaction(t1);
    service.addTransaction(t2);

    List<Transaction> all = service.viewAllTransactions();

    assertThat(all).hasSizeGreaterThanOrEqualTo(2);
    // Most recent should be first (ORDER BY created_time DESC)
    assertThat(all.get(0).getDescription()).isEqualTo("second");
  }

  /**
   * Tests retrieval of a specific transaction by ID.
   *
   * <p><strong>External Integration:</strong> Service → Database SELECT with WHERE clause
   */
  @Test
  public void testGetTransaction_found() {
    Transaction t = new Transaction(userId, 50.0, "FOOD", "specific transaction");
    Transaction saved = service.addTransaction(t);

    Optional<Transaction> found = service.getTransaction(saved.getTransactionId());

    assertThat(found).isPresent();
    assertThat(found.get().getDescription()).isEqualTo("specific transaction");
  }

  /**
   * Tests retrieval of non-existent transaction.
   *
   * <p><strong>External Integration:</strong> Service → Database returns empty result
   */
  @Test
  public void testGetTransaction_notFound() {
    UUID fakeId = UUID.randomUUID();

    Optional<Transaction> found = service.getTransaction(fakeId);

    assertThat(found).isEmpty();
  }

  /**
   * Tests successful transaction creation with database-generated fields.
   *
   * <p><strong>External Integration:</strong>
   * <ul>
   *   <li>Service → Database INSERT with RETURNING clause</li>
   *   <li>Database auto-generates: transaction_id, created_time, created_date</li>
   *   <li>Tests PostgreSQL ENUM casting (::transaction_category)</li>
   * </ul>
   */
  @Test
  public void testAddTransaction_success() {
    Transaction t = new Transaction();
    t.setUserId(userId);
    t.setDescription("desc");
    t.setAmount(10.0);
    t.setCategory("OTHER");

    Transaction saved = service.addTransaction(t);

    assertThat(saved.getTransactionId()).isNotNull();
    assertThat(saved.getDate()).isNotNull();
    assertThat(saved.getTimestamp()).isNotNull();
  }

  /**
   * Tests application-level validation before database interaction.
   *
   * <p><strong>Internal Integration:</strong> Application validation layer preventing
   * invalid data from reaching the database
   */
  @Test
  public void testAddTransaction_negativeAmountInvalid() {
    Transaction t = new Transaction();
    t.setUserId(userId);
    t.setDescription("desc");
    t.setAmount(-5.0);
    t.setCategory("OTHER");

    assertThatThrownBy(() -> service.addTransaction(t))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Amount must be greater than 0");
  }

  /**
   * Tests application-level validation for zero amount.
   *
   * <p><strong>Internal Integration:</strong> Application validation layer
   */
  @Test
  public void testAddTransaction_zeroAmountInvalid() {
    Transaction t = new Transaction();
    t.setUserId(userId);
    t.setDescription("desc");
    t.setAmount(0.0);
    t.setCategory("OTHER");

    assertThatThrownBy(() -> service.addTransaction(t))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Amount must be greater than 0");
  }

  /**
   * Tests application-level validation for null description.
   *
   * <p><strong>Internal Integration:</strong> Application validation layer
   */
  @Test
  public void testAddTransaction_nullDescriptionInvalid() {
    Transaction t = new Transaction();
    t.setUserId(userId);
    t.setDescription(null);
    t.setAmount(10.0);
    t.setCategory("OTHER");

    assertThatThrownBy(() -> service.addTransaction(t))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Description is required");
  }

  /**
   * Tests application-level validation for null category.
   *
   * <p><strong>Internal Integration:</strong> Application validation layer
   */
  @Test
  public void testAddTransaction_nullCategoryInvalid() {
    Transaction t = new Transaction();
    t.setUserId(userId);
    t.setDescription("desc");
    t.setAmount(10.0);
    t.setCategory(null);

    assertThatThrownBy(() -> service.addTransaction(t))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Category is required");
  }

  /**
   * Tests database ENUM constraint enforcement.
   *
   * <p><strong>External Integration:</strong> Application attempts invalid category,
   * database ENUM type rejects it, application handles the error
   */
  @Test
  public void testAddTransaction_categoryInvalid() {
    Transaction t = new Transaction();
    t.setUserId(userId);
    t.setDescription("desc");
    t.setAmount(5.0);
    t.setCategory("INVALID");

    assertThatThrownBy(() -> service.addTransaction(t))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid category");
  }

  /**
   * Tests partial update of transaction record.
   *
   * <p><strong>External Integration:</strong> Service → Database UPDATE operation
   * with selective field modification
   */
  @Test
  public void testUpdateTransaction_success() {
    Transaction t = new Transaction(userId, 20.0, "OTHER", "desc");
    Transaction saved = service.addTransaction(t);

    Map<String, Object> updates = Map.of(
        "amount", 25.0,
        "description", "updated"
    );

    Optional<Transaction> updated = service.updateTransaction(saved.getTransactionId(), updates);

    assertThat(updated).isPresent();
    assertThat(updated.get().getAmount()).isEqualTo(25.0);
    assertThat(updated.get().getDescription()).isEqualTo("updated");
  }

  /**
   * Tests update with category change.
   *
   * <p><strong>External Integration:</strong> Service → Database UPDATE with ENUM casting
   */
  @Test
  public void testUpdateTransaction_categoryChange() {
    Transaction t = new Transaction(userId, 20.0, "OTHER", "desc");
    Transaction saved = service.addTransaction(t);

    Map<String, Object> updates = Map.of("category", "FOOD");

    Optional<Transaction> updated = service.updateTransaction(saved.getTransactionId(), updates);

    assertThat(updated).isPresent();
    assertThat(updated.get().getCategory()).isEqualTo("FOOD");
  }

  /**
   * Tests update with invalid category.
   *
   * <p><strong>Internal Integration:</strong> Application validation rejects invalid category
   */
  @Test
  public void testUpdateTransaction_invalidCategory() {
    Transaction t = new Transaction(userId, 20.0, "OTHER", "desc");
    Transaction saved = service.addTransaction(t);

    Map<String, Object> updates = Map.of("category", "INVALID_CATEGORY");

    assertThatThrownBy(() -> service.updateTransaction(saved.getTransactionId(), updates))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid category");
  }

  /**
   * Tests transaction deletion from database.
   *
   * <p><strong>External Integration:</strong> Service → Database DELETE operation
   */
  @Test
  public void testDeleteTransaction_success() {
    Transaction t = new Transaction(userId, 10.0, "OTHER", "desc");
    Transaction saved = service.addTransaction(t);

    boolean deleted = service.deleteTransaction(saved.getTransactionId());
    assertThat(deleted).isTrue();

    // Verify actually deleted
    assertThat(service.getTransaction(saved.getTransactionId())).isEmpty();
  }

  /**
   * Tests deletion of non-existent transaction.
   *
   * <p><strong>External Integration:</strong> Service → Database DELETE returns 0 rows
   */
  @Test
  public void testDeleteTransaction_notFound() {
    UUID fakeId = UUID.randomUUID();

    boolean deleted = service.deleteTransaction(fakeId);

    assertThat(deleted).isFalse();
  }

  // ===========================================================================
  // FOREIGN KEY RELATIONSHIP TESTS - Testing User-Transaction data sharing
  // ===========================================================================

  /**
   * Tests that transactions cannot be created for non-existent users.
   *
   * <p><strong>External Integration:</strong> Database foreign key constraint enforcement
   * between transactions.user_id and users.user_id
   */
  @Test
  public void testAddTransaction_invalidUserId() {
    UUID fakeUserId = UUID.randomUUID();
    Transaction t = new Transaction(fakeUserId, 10.0, "OTHER", "desc");

    assertThatThrownBy(() -> service.addTransaction(t))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid user ID");
  }

  /**
   * Tests cascade delete: deleting a user deletes their transactions.
   *
   * <p><strong>External Integration:</strong>
   * <ul>
   *   <li>User-Transaction foreign key with ON DELETE CASCADE</li>
   *   <li>Database automatically removes child records</li>
   *   <li>Service verifies cascaded deletion</li>
   * </ul>
   *
   * <p><strong>Shared Data:</strong> Transaction.userId references User.userId
   */
  @Test
  public void testDeleteUser_cascadesTransactions() {
    // Create transactions for user
    Transaction t1 = new Transaction(userId, 10.0, "FOOD", "lunch");
    Transaction t2 = new Transaction(userId, 20.0, "OTHER", "misc");
    service.addTransaction(t1);
    service.addTransaction(t2);

    // Verify transactions exist
    List<Transaction> before = service.getTransactionsByUser(userId);
    assertThat(before).hasSize(2);

    // Delete user
    boolean deleted = service.deleteUser(userId);
    assertThat(deleted).isTrue();

    // Verify transactions were cascaded
    List<Transaction> after = service.getTransactionsByUser(userId);
    assertThat(after).isEmpty();
  }

  /**
   * Tests retrieval of transactions filtered by user ID.
   *
   * <p><strong>External Integration:</strong> Service → Database query filtering
   * by foreign key relationship
   *
   * <p><strong>Shared Data:</strong> Tests that transactions are correctly
   * associated with their owning user via userId
   */
  @Test
  public void testGetTransactionsByUser() {
    // Create second user
    User user2 = new User("user2", "user2@email.com", 200.0);
    User savedUser2 = service.addUser(user2);

    // Create transactions for both users
    Transaction t1 = new Transaction(userId, 10.0, "FOOD", "user1-tx1");
    Transaction t2 = new Transaction(userId, 15.0, "OTHER", "user1-tx2");
    Transaction t3 = new Transaction(savedUser2.getUserId(), 30.0, "FOOD", "user2-tx1");

    service.addTransaction(t1);
    service.addTransaction(t2);
    service.addTransaction(t3);

    // Test user 1's transactions
    List<Transaction> user1Txs = service.getTransactionsByUser(userId);
    assertThat(user1Txs).hasSize(2);
    assertThat(user1Txs).allMatch(tx -> tx.getUserId().equals(userId));

    // Test user 2's transactions
    List<Transaction> user2Txs = service.getTransactionsByUser(savedUser2.getUserId());
    assertThat(user2Txs).hasSize(1);
    assertThat(user2Txs.get(0).getDescription()).isEqualTo("user2-tx1");
  }

  // ===========================================================================
  // ANALYTICS OPERATIONS - Testing complex database queries with aggregations
  // ===========================================================================

  /**
   * Tests budget summary generation with SUM aggregation.
   *
   * <p><strong>Internal Integration:</strong>
   * <ul>
   *   <li>Service retrieves user from database</li>
   *   <li>Service queries transactions with WHERE clause</li>
   *   <li>Application calculates SUM</li>
   *   <li>Combines data from both User and Transaction tables into summary</li>
   * </ul>
   *
   * <p><strong>Shared Data:</strong> Combines User.budget with Transaction.amount
   */
  @Test
  public void testGetBudgetsTextBlock_userFound() {
    addSampleTransactions();
    String summary = service.getBudgetsTextBlock(userId);
    assertThat(summary).contains("Total Budget", "Total Spent", "Remaining");
  }

  /**
   * Tests error handling when querying non-existent user.
   *
   * <p><strong>External Integration:</strong> Service queries database for non-existent
   * record and handles absence gracefully
   */
  @Test
  public void testGetBudgetsTextBlock_userNotFound() {
    UUID fakeId = UUID.randomUUID();
    String summary = service.getBudgetsTextBlock(fakeId);
    assertThat(summary).contains("User not found");
  }

  /**
   * Tests budget warning generation for over-budget user.
   *
   * <p><strong>Internal Integration:</strong>
   * <ul>
   *   <li>Service retrieves user from database</li>
   *   <li>Service retrieves transactions and calculates totals</li>
   *   <li>Business logic generates warning messages</li>
   * </ul>
   *
   * <p><strong>Shared Data:</strong> Compares User.budget with sum of Transaction.amount
   */
  @Test
  public void testGetBudgetWarningsText_overBudget() {
    // Create transactions exceeding the 100.0 budget
    Transaction t1 = new Transaction(userId, 80.0, "FOOD", "big expense");
    Transaction t2 = new Transaction(userId, 50.0, "OTHER", "another expense");
    service.addTransaction(t1);
    service.addTransaction(t2);

    String warnings = service.getBudgetWarningsText(userId);

    assertThat(warnings).contains("OVER BUDGET");
  }

  /**
   * Tests budget warning generation for near-budget user.
   *
   * <p><strong>Internal Integration:</strong> Business logic detects less than 10% remaining
   *
   * <p><strong>Shared Data:</strong> User.budget compared with Transaction totals
   */
  @Test
  public void testGetBudgetWarningsText_nearBudget() {
    // Spend 95 of 100 budget (5% remaining, which is < 10%)
    Transaction t = new Transaction(userId, 95.0, "FOOD", "big expense");
    service.addTransaction(t);

    String warnings = service.getBudgetWarningsText(userId);

    assertThat(warnings).contains("Budget warning");
  }

  /**
   * Tests budget warning generation for healthy budget.
   *
   * <p><strong>Internal Integration:</strong> No warnings when budget is healthy
   */
  @Test
  public void testGetBudgetWarningsText_healthy() {
    // Spend only 10 of 100 budget (90% remaining)
    Transaction t = new Transaction(userId, 10.0, "FOOD", "small expense");
    service.addTransaction(t);

    String warnings = service.getBudgetWarningsText(userId);

    assertThat(warnings).isEmpty();
  }

  /**
   * Tests budget warning for non-existent user.
   *
   * <p><strong>External Integration:</strong> Service handles missing user gracefully
   */
  @Test
  public void testGetBudgetWarningsText_userNotFound() {
    UUID fakeId = UUID.randomUUID();

    String warnings = service.getBudgetWarningsText(fakeId);

    assertThat(warnings).contains("User not found");
  }

  /**
   * Tests budget update operation.
   *
   * <p><strong>External Integration:</strong> Service → Database UPDATE operation
   * with validation
   */
  @Test
  public void testSetBudgets_success() {
    service.setBudgets(userId, Map.of("budget", 200.0));
    Optional<User> updated = service.getUser(userId);
    assertThat(updated).isPresent();
    assertThat(updated.get().getBudget()).isEqualTo(200.0);
  }

  /**
   * Tests budget validation before database update.
   *
   * <p><strong>Internal Integration:</strong> Application validates data before
   * sending to database
   */
  @Test
  public void testSetBudgets_invalidBudget() {
    assertThatThrownBy(() -> service.setBudgets(userId, Map.of("budget", -10)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Budget cannot be negative");
  }

  /**
   * Tests budget update for non-existent user.
   *
   * <p><strong>External Integration:</strong> Service validates user exists before update
   */
  @Test
  public void testSetBudgets_userNotFound() {
    UUID fakeId = UUID.randomUUID();

    assertThatThrownBy(() -> service.setBudgets(fakeId, Map.of("budget", 100.0)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("User not found");
  }

  /**
   * Tests weekly summary with date-based filtering.
   *
   * <p><strong>External Integration:</strong>
   * <ul>
   *   <li>Service → Database query with date comparison (created_date >= ?)</li>
   *   <li>Tests PostgreSQL date arithmetic</li>
   *   <li>Verifies ORDER BY clause works correctly</li>
   * </ul>
   */
  @Test
  public void testWeeklySummary() {
    addSampleTransactions();
    List<Transaction> weekly = service.weeklySummary(userId);
    assertThat(weekly).hasSize(3);
  }

  /**
   * Tests weekly summary for user with no transactions.
   *
   * <p><strong>External Integration:</strong> Database returns empty result set
   */
  @Test
  public void testWeeklySummary_noTransactions() {
    List<Transaction> weekly = service.weeklySummary(userId);
    assertThat(weekly).isEmpty();
  }

  /**
   * Tests aggregate SUM calculation with date filtering.
   *
   * <p><strong>External Integration:</strong> Service → Database aggregate query
   * (SUM with WHERE clause and date comparison)
   */
  @Test
  public void testTotalLast7Days() {
    addSampleTransactions();
    double total = service.totalLast7Days(userId);
    assertThat(total).isEqualTo(26.0); // 10 + 1 + 15
  }

  /**
   * Tests total calculation when no transactions exist.
   *
   * <p><strong>External Integration:</strong> Database COALESCE returns 0 for null SUM
   */
  @Test
  public void testTotalLast7Days_noTransactions() {
    double total = service.totalLast7Days(userId);
    assertThat(total).isEqualTo(0.0);
  }

  /**
   * Tests monthly summary with date filtering and grouping.
   *
   * <p><strong>Internal Integration:</strong>
   * <ul>
   *   <li>Service queries transactions from database</li>
   *   <li>Filters by current month/year</li>
   *   <li>Groups by category (using Java streams)</li>
   *   <li>Calculates totals per category</li>
   * </ul>
   *
   * <p><strong>Shared Data:</strong> Combines User.budget with grouped Transaction data
   */
  @Test
  public void testGetMonthlySummary() {
    addSampleTransactions();
    String summary = service.getMonthlySummary(userId);
    assertThat(summary)
        .contains("Total Budget")
        .contains("Total Spent")
        .contains("Spending by Category");
  }

  /**
   * Tests monthly summary for non-existent user.
   *
   * <p><strong>External Integration:</strong> Service handles missing user gracefully
   */
  @Test
  public void testGetMonthlySummary_userNotFound() {
    UUID fakeId = UUID.randomUUID();

    String summary = service.getMonthlySummary(fakeId);

    assertThat(summary).contains("User not found");
  }

  /**
   * Tests comprehensive budget report generation.
   *
   * <p><strong>Internal Integration:</strong>
   * <ul>
   *   <li>Retrieves user data from database</li>
   *   <li>Retrieves all user transactions</li>
   *   <li>Performs aggregations (SUM, GROUP BY category)</li>
   *   <li>Combines multiple queries into single report</li>
   *   <li>Applies business logic (warnings, thresholds)</li>
   *   <li>Calls getBudgetWarningsText internally</li>
   * </ul>
   *
   * <p><strong>Shared Data:</strong> Comprehensive integration of User and Transaction data
   */
  @Test
  public void testGetBudgetReport() {
    addSampleTransactions();
    Map<String, Object> report = service.getBudgetReport(userId);

    assertThat(report).containsKeys(
        "userId", "username", "totalBudget", "totalSpent",
        "remaining", "categories", "isOverBudget", "warnings"
    );
    assertThat(report.get("totalSpent")).isEqualTo(26.0);
  }

  /**
   * Tests budget report for non-existent user.
   *
   * <p><strong>External Integration:</strong> Service returns error map for missing user
   */
  @Test
  public void testGetBudgetReport_userNotFound() {
    UUID fakeId = UUID.randomUUID();

    Map<String, Object> report = service.getBudgetReport(fakeId);

    assertThat(report).containsKey("error");
    assertThat(report.get("error")).isEqualTo("User not found");
  }

  /**
   * Tests budget report with over-budget status.
   *
   * <p><strong>Internal Integration:</strong> Business logic correctly sets isOverBudget flag
   *
   * <p><strong>Shared Data:</strong> Compares User.budget (100) with Transaction totals (130)
   */
  @Test
  public void testGetBudgetReport_overBudget() {
    Transaction t1 = new Transaction(userId, 80.0, "FOOD", "big expense");
    Transaction t2 = new Transaction(userId, 50.0, "OTHER", "another expense");
    service.addTransaction(t1);
    service.addTransaction(t2);

    Map<String, Object> report = service.getBudgetReport(userId);

    assertThat(report.get("totalSpent")).isEqualTo(130.0);
    assertThat(report.get("remaining")).isEqualTo(-30.0);
    assertThat(report.get("isOverBudget")).isEqualTo(true);
    assertThat(report.get("hasWarnings")).isEqualTo(true);
  }

  // ===========================================================================
  // UNIQUE CONSTRAINT VALIDATION - Testing database constraint checks
  // ===========================================================================

  /**
   * Tests username uniqueness check against database.
   *
   * <p><strong>External Integration:</strong> Service → Database COUNT query
   * to check for existing username
   */
  @Test
  public void testIsUsernameExists_existing() {
    boolean exists = service.isUsernameExists("user", null);
    assertThat(exists).isTrue();
  }

  /**
   * Tests username check for non-existent username.
   *
   * <p><strong>External Integration:</strong> Service → Database COUNT returns 0
   */
  @Test
  public void testIsUsernameExists_notExisting() {
    boolean exists = service.isUsernameExists("nonexistent", null);
    assertThat(exists).isFalse();
  }

  /**
   * Tests username uniqueness check with exclusion.
   *
   * <p><strong>External Integration:</strong> Service → Database COUNT query
   * with compound WHERE clause (username = ? AND user_id != ?)
   */
  @Test
  public void testIsUsernameExists_excludingSelf() {
    boolean exists = service.isUsernameExists("user", userId);
    assertThat(exists).isFalse(); // Excludes self, so not considered duplicate
  }

  /**
   * Tests email uniqueness check against database.
   *
   * <p><strong>External Integration:</strong> Service → Database COUNT query
   * to check for existing email
   */
  @Test
  public void testIsEmailExists_existing() {
    boolean exists = service.isEmailExists("user@email.com", null);
    assertThat(exists).isTrue();
  }

  /**
   * Tests email check for non-existent email.
   *
   * <p><strong>External Integration:</strong> Service → Database COUNT returns 0
   */
  @Test
  public void testIsEmailExists_notExisting() {
    boolean exists = service.isEmailExists("nonexistent@email.com", null);
    assertThat(exists).isFalse();
  }

  /**
   * Tests email uniqueness check with exclusion.
   *
   * <p><strong>External Integration:</strong> Service → Database COUNT query
   * with compound WHERE clause
   */
  @Test
  public void testIsEmailExists_excludingSelf() {
    boolean exists = service.isEmailExists("user@email.com", userId);
    assertThat(exists).isFalse();
  }

  // ===========================================================================
  // TIMESTAMP AND DATE HANDLING - Testing database triggers and defaults
  // ===========================================================================

  /**
   * Tests that database automatically generates timestamps and dates.
   *
   * <p><strong>External Integration:</strong>
   * <ul>
   *   <li>Database trigger sets created_time to CURRENT_TIMESTAMP</li>
   *   <li>Database trigger sets created_date to CURRENT_DATE</li>
   *   <li>Service retrieves and maps these values correctly</li>
   * </ul>
   */
  @Test
  public void testTransactionTimestampGeneration() {
    Transaction t = new Transaction(userId, 50.0, "FOOD", "test");

    // Don't set timestamp or date manually
    Transaction saved = service.addTransaction(t);

    // Verify database populated these fields
    assertThat(saved.getTimestamp()).isNotNull();
    assertThat(saved.getDate()).isNotNull();
    assertThat(saved.getDate()).isEqualTo(LocalDate.now());
  }

  /**
   * Tests that transaction ID is generated by database.
   *
   * <p><strong>External Integration:</strong> Database generates UUID via gen_random_uuid()
   */
  @Test
  public void testTransactionIdGeneration() {
    Transaction t = new Transaction(userId, 25.0, "OTHER", "test id generation");

    // Transaction ID should be null before save
    assertThat(t.getTransactionId()).isNull();

    Transaction saved = service.addTransaction(t);

    // Database should have generated the ID
    assertThat(saved.getTransactionId()).isNotNull();
  }

  // ===========================================================================
  // Helper Methods
  // ===========================================================================

  /**
   * Helper method to create sample transactions for testing.
   * Reduces code duplication across tests.
   */
  private void addSampleTransactions() {
    Transaction t1 = new Transaction(userId, 10.0, "OTHER", "desc1");
    Transaction t2 = new Transaction(userId, 1.0, "FOOD", "desc2");
    Transaction t3 = new Transaction(userId, 15.0, "OTHER", "desc3");
    service.addTransaction(t1);
    service.addTransaction(t2);
    service.addTransaction(t3);
  }
}