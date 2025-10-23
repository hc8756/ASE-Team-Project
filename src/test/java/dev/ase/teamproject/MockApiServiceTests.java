package dev.ase.teamproject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ase.teamproject.model.Transaction;
import dev.ase.teamproject.model.User;
import dev.ase.teamproject.service.MockApiService;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * This class contains the unit tests for the MockApiService class.
 */
public class MockApiServiceTests {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private MockApiService service;

  private User user;
  private UUID userId;
  private Transaction transaction;
  private UUID transactionId;

  /**
   * Initializes mock objects and sets up test data before each test run.
   */
  @BeforeEach
  public void setUp() {
    user = new User();
    userId = user.getUserId();
    transaction = new Transaction(
      userId, 
      10.0, 
      "category", 
      "description");
    transactionId = transaction.getTransactionId();
    MockitoAnnotations.openMocks(this);
  }

  // ---------------------------------------------------------------------------
  // viewAllUsers
  // ---------------------------------------------------------------------------

  @Test
  public void viewAllUsers_twoUsersExist_returnsListOfTwo() {
    List<User> users = List.of(new User(), new User());
    when(jdbcTemplate.query(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any()))
        .thenReturn(users);
    List<User> test = service.viewAllUsers();
    assertEquals(2, test.size());
  }

  @Test
  public void viewAllUsers_noUsersExist_returnsEmptyList() {
    List<User> users = List.of();
    when(jdbcTemplate.query(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any()))
        .thenReturn(users);
    List<User> test = service.viewAllUsers();
    assertEquals(0, test.size());
  }

  // ---------------------------------------------------------------------------
  // getUser
  // ---------------------------------------------------------------------------

  /** Typical valid input. */
  @Test
  public void getUser_userExists_returnsUser() {
    when(jdbcTemplate.queryForObject(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any(), 
      eq(userId)))
        .thenReturn(user);
  }

  /** Atypical valid input. */
  @Test
  public void getUser_userDoesNotExist_returnsEmptyOptional() {
    when(jdbcTemplate.queryForObject(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any(), 
      eq(userId)))
        .thenReturn(null);

    Optional<User> test = service.getUser(userId);
    assertTrue(test.isEmpty());
  }

  /** Invalid input. */
  @Test
  public void getUser_invalid_throwsExceptionReturnsEmpty() {
    when(jdbcTemplate.queryForObject(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any(), 
      eq(userId)))
        .thenThrow(new RuntimeException("DB error"));
    Optional<User> result = service.getUser(userId);
    assertFalse(result.isPresent());
  }

  // ---------------------------------------------------------------------------
  // addUser
  // ---------------------------------------------------------------------------

  /** Typical valid input. */
  @Test
  public void addUser_validInput_returnsUserWithGeneratedId() {
    when(jdbcTemplate.queryForObject(
      anyString(), 
      eq(UUID.class), 
      anyString(), 
      anyString(), 
      anyDouble()))
        .thenReturn(userId);

    User test = service.addUser(user);
    assertEquals(userId, test.getUserId());
  }

  /** Atypical valid input. */
  @Test
  public void addUser_atypical_returnsUserWithNullId() {
    when(jdbcTemplate.queryForObject(
      anyString(), 
      eq(UUID.class),
      anyString(), 
      anyString(), 
      anyDouble()))
        .thenReturn(null);
    User test = service.addUser(user);
    assertNull(test.getUserId());
  }

  /** Invalid input. */
  @Test
  public void addUser_invalid_throwsException() {
    when(jdbcTemplate.queryForObject(
        anyString(),
        eq(UUID.class),
        nullable(String.class),
        nullable(String.class),
        anyDouble()
    )).thenThrow(new RuntimeException("DB error"));

    assertThrows(RuntimeException.class, () -> service.addUser(user));
  }

  // ---------------------------------------------------------------------------
  // deleteUser
  // ---------------------------------------------------------------------------

  /** Typical valid input. */
  @Test
  public void deleteUser_userExists_returnsTrue() {
    when(jdbcTemplate.update(anyString(), eq(userId)))
        .thenReturn(1);
    boolean result = service.deleteUser(userId);
    assertTrue(result);
  }

  /** Atypical valid input. */
  @Test
  public void deleteUser_userDoesNotExist_returnsFalse() {
    when(jdbcTemplate.update(anyString(), eq(userId)))
        .thenReturn(0);
    boolean result = service.deleteUser(userId);
    assertFalse(result);
  }

  /** Invalid input. */
  @Test
  public void deleteUser_invalid_throwsRuntimeException() {
    when(jdbcTemplate.update(anyString(), eq(userId)))
        .thenThrow(new RuntimeException("DB error"));
    assertThrows(RuntimeException.class, () -> {
      service.deleteUser(userId);
    });
  }

  // ---------------------------------------------------------------------------
  // viewAllTransactions
  // ---------------------------------------------------------------------------

  @Test
  public void viewAllTransactions_returnsTransactions() {
    Transaction t1 = new Transaction(userId, 25.0, "category1", "desc1");
    Transaction t2 = new Transaction(userId, 10.0, "category2", "desc2");
    List<Transaction> transactions = List.of(t1, t2);
    when(jdbcTemplate.query(
      anyString(),
      ArgumentMatchers.<RowMapper<Transaction>>any()))
        .thenReturn(transactions);
      List<Transaction> test = service.viewAllTransactions();
      assertEquals(2, test.size());
  }

  @Test
  public void viewAllTransactions_noTransactions_returnsEmptyList() {
    List<Transaction> transactions = List.of();
    when(jdbcTemplate.query(
      anyString(),
      ArgumentMatchers.<RowMapper<Transaction>>any()))
        .thenReturn(transactions);
      List<Transaction> test = service.viewAllTransactions();
      assertEquals(0, test.size());
  }


  // ---------------------------------------------------------------------------
  // getTransactionsByUser
  // ---------------------------------------------------------------------------

  /** Typical valid input. */
  @Test
  public void getTransactionsByUser_typical_returnsTransactionList() {
    Transaction t1 = new Transaction(userId, 25.0, "category1", "desc1");
    Transaction t2 = new Transaction(userId, 10.0, "category2", "desc2");
    List<Transaction> transactions = List.of(t1, t2);

    when(jdbcTemplate.query(
      anyString(), 
      ArgumentMatchers.<RowMapper<Transaction>>any(), 
      eq(userId)))
        .thenReturn(transactions);

    List<Transaction> test = service.getTransactionsByUser(userId);
    assertEquals(2, test.size());
    assertEquals("category1", test.get(0).getCategory());
  }

  /** Atypical valid input. */
  @Test
  public void getTransactionsByUser_atypical_returnsEmptyList() {
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<User>>any(), eq(userId)))
        .thenReturn(List.of());

    List<Transaction> result = service.getTransactionsByUser(userId);
    assertTrue(result.isEmpty());
  }

  /** Invalid input. */
  @Test
  public void getTransactionsByUser_queryFails_throwsRuntimeException() {
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<User>>any(), eq(userId)))
        .thenThrow(new RuntimeException("Error"));
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      service.getTransactionsByUser(userId);
    });
    
    assertTrue(exception.getMessage().contains("Failed to get transactions: "));
    assertTrue(exception.getMessage().contains("Error"));

  }

  // ---------------------------------------------------------------------------
  // addTransaction
  // ---------------------------------------------------------------------------

  /** Typical valid input. */
  @Test
  public void addTransaction_validTransaction_returnsSavedTransaction() {
    LocalDateTime createdTime = LocalDateTime.of(2025, 10, 23, 12, 0);
    LocalDate createdDate = LocalDate.of(2025, 10, 23);

    when(jdbcTemplate.queryForObject(
        anyString(),
        ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transaction.getUserId()),
        eq(transaction.getDescription()),
        eq(transaction.getAmount()),
        eq(transaction.getCategory())))
          .thenAnswer(invocation -> {
            var rs = mock(java.sql.ResultSet.class);
            when(rs.getObject("transaction_id", UUID.class)).thenReturn(transactionId);
            when(rs.getTimestamp("created_time")).thenReturn(Timestamp.valueOf(createdTime));
            when(rs.getDate("created_date")).thenReturn(Date.valueOf(createdDate));
            var rowMapper = invocation.getArgument(1);
            return ((org.springframework.jdbc.core.RowMapper<Transaction>) rowMapper).mapRow(rs, 0);
          });

    Transaction result = service.addTransaction(transaction);

    assertNotNull(result);
    assertEquals(transaction.getUserId(), result.getUserId());
    assertEquals(transaction.getDescription(), result.getDescription());
    assertEquals(transaction.getAmount(), result.getAmount());
    assertEquals(transaction.getCategory(), result.getCategory());
    assertEquals(transaction.getTransactionId(), result.getTransactionId());
    assertEquals(createdTime, result.getTimestamp());
    assertEquals(createdDate, result.getDate());
  }

  /** Atypical valid input. */
  @Test
  public void addTransaction_atypical_returnsTransactionEvenIfNoRowsAffected() {
    when(jdbcTemplate.queryForObject(
      anyString(),
      ArgumentMatchers.<RowMapper<Transaction>>any(),
      any(),
      any(),
      any(),
      any()))
        .thenReturn(null);

    Transaction t = new Transaction(userId, 75.0, "transport", "subway");
    Transaction result = service.addTransaction(t);
    assertNotNull(result);
    assertEquals(75.0, result.getAmount());
    assertNull(result.getTransactionId());
    assertNull(result.getTimestamp());
    assertNull(result.getDate());
  }

  /** Invalid input. */
  @Test
  public void addTransaction_failedTransaction_throwsRuntimeException() {
    when(jdbcTemplate.queryForObject(
        anyString(),
        ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transaction.getUserId()),
        eq(transaction.getDescription()),
        eq(transaction.getAmount()),
        eq(transaction.getCategory())
    )).thenThrow(new RuntimeException("DB insert error"));
  
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      service.addTransaction(transaction);
    });
  
    assertTrue(exception.getMessage().contains("Failed to create transaction"));
    assertTrue(exception.getCause().getMessage().contains("DB insert error"));
  }

  // ---------------------------------------------------------------------------
  // updateTransaction
  // ---------------------------------------------------------------------------

  /** Typical valid input. */
  @Test
  public void updateTransaction_validUpdates_returnsUpdatedTransaction() {
    when(jdbcTemplate.queryForObject(
        anyString(),
        ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId))).thenReturn(transaction);

    when(jdbcTemplate.update(anyString(), any(), any(), any(), eq(transactionId)))
        .thenReturn(1);

    Transaction updatedTransaction = new Transaction();
    updatedTransaction.setDescription("new description");
    updatedTransaction.setAmount(7.0);
    updatedTransaction.setCategory(("new category"));

    when(jdbcTemplate.queryForObject(
        anyString(),
        ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId))).thenReturn(updatedTransaction);
    
    Map<String, Object> updates = Map.of(
        "description", "new description", 
        "amount", "7.0", 
        "category", "new category");

    Optional<Transaction> test = service.updateTransaction(transactionId, updates);

    assertTrue(test.isPresent());
    assertEquals("new description", test.get().getDescription());
    assertEquals(7.0, test.get().getAmount());
    assertEquals("new category", test.get().getCategory());
  }

  @Test
  public void updateTransaction_amountIsNumeric_returnsUpdatedTransaction() {
    when(jdbcTemplate.queryForObject(
        anyString(),
        ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId))).thenReturn(transaction);

    when(jdbcTemplate.update(anyString(), any(), any(), any(), eq(transactionId)))
        .thenReturn(1);

    Transaction updatedTransaction = new Transaction();
    updatedTransaction.setAmount(7.0);

    when(jdbcTemplate.queryForObject(
        anyString(),
        ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId))).thenReturn(updatedTransaction);
    
    Map<String, Object> updates = Map.of("amount", 7);
    Optional<Transaction> test = service.updateTransaction(transactionId, updates);

    assertTrue(test.isPresent());
    assertEquals(7.0, test.get().getAmount());
  }

  /** Atypical valid input. */
  @Test
  public void updateTransaction_transactionNotFound_returnsEmptyOptional() {
    Map<String, Object> updates = Map.of("key1", "key2");
    when(jdbcTemplate.queryForObject(
        anyString(),
        ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId))).thenThrow(new RuntimeException());

    Optional<Transaction> test = service.updateTransaction(transactionId, updates);
    assertTrue(test.isEmpty());
  }

  /** Invalid input. */
  @Test
  public void updateTransaction_updateFails_returnsEmptyOptional() {

    Map<String, Object> updates = Map.of(
        "description", "new description", 
        "amount", "7.0", 
        "category", "new category");

    when(jdbcTemplate.queryForObject(
        anyString(),
        ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId))).thenReturn(transaction);

    when(jdbcTemplate.update(anyString(), any(), any(), any(), eq(transactionId)))
        .thenReturn(0);

    Optional<Transaction> test = service.updateTransaction(transactionId, updates);

    assertTrue(test.isEmpty());
  }

  // ---------------------------------------------------------------------------
  // deleteTransaction
  // ---------------------------------------------------------------------------

  /** Typical valid input. */
  @Test
  public void deleteTransaction_transactionExists_returnsTrue() {
    when(jdbcTemplate.update(anyString(), eq(transactionId)))
        .thenReturn(1);
    boolean test = service.deleteTransaction(transactionId);
    assertTrue(test);
  }

  /** Atypical valid input. */
  @Test
  public void deleteTransaction_transactionDoesNotExist_returnsFalse() {
    when(jdbcTemplate.update(anyString(), eq(transactionId)))
        .thenReturn(0);
    boolean test = service.deleteTransaction(transactionId);
    assertFalse(test);
  }

  /** Invalid input. */
  @Test
  public void deleteTransaction_invalid_throwsRuntimeException() {
    when(jdbcTemplate.update(anyString(), eq(transactionId)))
        .thenThrow(new RuntimeException("DB error"));
    assertThrows(RuntimeException.class, () -> service.deleteTransaction(transactionId));
  }

  // ---------------------------------------------------------------------------
  // getBudgetsTextBlock
  // ---------------------------------------------------------------------------

  /** Typical valid input. */
  @Test
  public void getBudgetsTextBlock_userFound_returnsBudgetSummaryText() {
    user.setUsername("User");
    List<Transaction> transactions = List.of(
        new Transaction(userId, 1.0, "category", "description"),
        new Transaction(userId, 2.0, "category2", "description2"));
    when(jdbcTemplate.queryForObject(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any(), 
      eq(userId)))
        .thenReturn(user);

    when(jdbcTemplate.query(
      anyString(), 
      ArgumentMatchers.<RowMapper<Transaction>>any(), 
      eq(userId)))
        .thenReturn(transactions);

    String test = service.getBudgetsTextBlock(userId);
    assertTrue(test.contains("User"));
    assertTrue(test.contains("3.0"));
  }

  /** Atypical valid input. */
  @Test
  public void getBudgetsTextBlock_atypical_noTransactionsShowsZeroSpent() {
    UUID testId = UUID.randomUUID();
    User testUser = new User();
    testUser.setUserId(testId);
    testUser.setUsername("username");
    testUser.setBudget(10.00);
    when(jdbcTemplate.queryForObject(
      anyString(),
      ArgumentMatchers.<RowMapper<User>>any(),
      eq(userId)
    )).thenReturn(user);
    when(jdbcTemplate.query(
      anyString(),
      ArgumentMatchers.<RowMapper<Transaction>>any(),
      eq(userId)
    )).thenReturn(Collections.emptyList());

    String test = service.getBudgetsTextBlock(userId);
    System.out.println("DEBUG: " + test);
    assertTrue(test.contains("Total Spent: $0.00"));
  }

  /** Invalid input. */
  @Test
  public void getBudgetsTextBlock_userNotFound_returnsUserNotFoundMessage() {
    when(jdbcTemplate.queryForObject(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any(), 
      eq(userId)))
        .thenThrow(new RuntimeException());
    String test = service.getBudgetsTextBlock(userId);
    assertEquals("User not found", test);
  }

  // ---------------------------------------------------------------------------
  // getBudgetWarningsText
  // ---------------------------------------------------------------------------

  /** Typical valid input. */
  @Test
  public void getBudgetWarningsText_userNearBudgetLimit_returnsWarningMessage() {
    user.setBudget(10.0);

    List<Transaction> transactions = List.of(
        new Transaction(userId, 9.5, "category", "description"));

    when(jdbcTemplate.queryForObject(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any(), 
      eq(userId)))
        .thenReturn(user);
    when(jdbcTemplate.query(
      anyString(), 
      ArgumentMatchers.<RowMapper<Transaction>>any(),
      eq(userId)))
        .thenReturn(transactions);

    String test = service.getBudgetWarningsText(userId);
    assertTrue(test.contains("Budget warning: Only"));
    assertTrue(test.contains("0.5"));
  }

  /** Atypical valid input. */
  @Test
  public void getBudgetWarningsText_userOverBudget_returnsOverBudgetWarning() {
    user.setBudget(5.0);

    List<Transaction> transactions = List.of(
        transaction);

    when(jdbcTemplate.queryForObject(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any(), 
      eq(userId)))
        .thenReturn(user);
    when(jdbcTemplate.query(
      anyString(), 
      ArgumentMatchers.<RowMapper<Transaction>>any(), 
      eq(userId)))
        .thenReturn(transactions);

    String test = service.getBudgetWarningsText(userId);
    assertTrue(test.contains("OVER BUDGET! You have exceeded your budget by $"));
    assertTrue(test.contains("5.0"));
  }

  /** Invalid input. */
  @Test
  public void getBudgetWarningsText_userNotFound_returnsUserNotFoundMessage() {
    when(jdbcTemplate.queryForObject(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any(), 
      eq(userId)))
        .thenThrow(new RuntimeException());
    String test = service.getBudgetWarningsText(userId);
    assertEquals("User not found", test);
  }

  // ---------------------------------------------------------------------------
  // getMonthlySummary
  // ---------------------------------------------------------------------------

  /** Typical valid input. */
  @Test
  public void getMonthlySummary_typical_returnsFormattedSummary() {
    user.setBudget(500);
    LocalDate now = LocalDate.now();

    Transaction t1 = new Transaction(userId, 100.0, "c1", "d1");
    t1.setDate(now);
    Transaction t2 = new Transaction(userId, 50.0, "c2", "d2");
    t2.setDate(now);
    Transaction t3 = new Transaction(userId, 200.0, "c3", "d3");
    t3.setDate(now.minusMonths(1));
    List<Transaction> transactions = List.of(t1, t2, t3);

    when(jdbcTemplate.queryForObject(
        anyString(),
        ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)
    )).thenReturn(user);

    when(jdbcTemplate.query(
        anyString(),
        ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(userId)
    )).thenReturn(transactions);

    String summary = service.getMonthlySummary(userId);

    assertTrue(summary.contains("Total Spent: $150.00"));
    assertTrue(summary.contains("Remaining: $350.00"));
    assertTrue(summary.contains("- c1: $100.00"));
    assertTrue(summary.contains("- c2: $50.00"));
    assertFalse(summary.contains("c3"));

  }

  /** Atypical valid input. */
  @Test
  public void getMonthlySummary_invalid_nullDatesIgnoredInSummary() {
    user.setBudget(500);
    LocalDate now = LocalDate.now();

    Transaction t1 = new Transaction(userId, 100.0, "c1", "d1");
    Transaction t2 = new Transaction(userId, 50.0, "c2", "d2");
    Transaction t3 = new Transaction(userId, 200.0, "c3", "d3");
    List<Transaction> transactions = List.of(t1, t2, t3);

    when(jdbcTemplate.queryForObject(
      anyString(),
      ArgumentMatchers.<RowMapper<User>>any(),
      eq(userId)
    )).thenReturn(user);

    when(jdbcTemplate.query(
      anyString(),
      ArgumentMatchers.<RowMapper<Transaction>>any(),
      eq(userId)
    )).thenReturn(transactions);

    String summary = service.getMonthlySummary(userId);
    assertTrue(summary.contains("Total Spent: $0.00"));
    assertTrue(summary.contains("Remaining: $500.00"));
  }

  /** Invalid input. */
  @Test
  public void getMonthlySummary_userNotFound_returnsUserNotFoundMessage() {
    when(jdbcTemplate.queryForObject(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any(), 
      eq(userId)))
        .thenThrow(new RuntimeException());
    String test = service.getMonthlySummary(userId);
    assertEquals("User not found", test);
  }

  // ---------------------------------------------------------------------------
  // getBudgetReport
  // ---------------------------------------------------------------------------

  /** Typical valid input. */
  @Test
  public void getBudgetReport_userFound_returnsValidReport() {
    user.setUsername("User");
    user.setEmail("email");
    user.setBudget(100.0);
    UUID userId = UUID.randomUUID();
    user.setUserId(userId);
    List<Transaction> transactions = List.of(
        new Transaction(userId, 1.0, "category", "description"),
        new Transaction(userId, 2.0, "category2", "description2"));
    when(jdbcTemplate.queryForObject(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any(), 
      eq(userId)))
        .thenReturn((user));
    when(jdbcTemplate.query(
      anyString(), 
      ArgumentMatchers.<RowMapper<Transaction>>any(), 
      eq(userId)))
        .thenReturn((transactions));

    Map<String, Object> test = service.getBudgetReport(userId);

    assertEquals(userId, test.get("userId"));
    assertEquals("User", test.get("username"));
    assertEquals(100.0, test.get("totalBudget"));
    assertEquals(3.0, test.get("totalSpent"));
    assertEquals(97.0, test.get("remaining"));
    assertEquals(false, test.get("isOverBudget"));
    assertEquals("", test.get("warnings"));
    assertEquals(false, test.get("hasWarnings"));

    Map<String, Double> categories = Map.of(
        "category", 1.0,
        "category2", 2.0);

    assertEquals(categories, test.get("categories"));
  }

  /** Atypical valid input. */
  @Test
  public void getBudgetReport_overBudget() {
    user.setUsername("User");
    user.setEmail("email");
    user.setBudget(1.0);
    UUID userId = UUID.randomUUID();
    user.setUserId(userId);
    List<Transaction> transactions = List.of(
        new Transaction(userId, 1.0, "category", "description"),
        new Transaction(userId, 2.0, "category2", "description2"));
    when(jdbcTemplate.queryForObject(
      anyString(),
      ArgumentMatchers.<RowMapper<User>>any(),
      eq(userId)))
        .thenReturn(user);
  
    when(jdbcTemplate.query(
      anyString(),
      ArgumentMatchers.<RowMapper<Transaction>>any(),
      eq(userId)))
        .thenReturn(transactions);
    
    Map<String, Object> test = service.getBudgetReport(userId);
    assertEquals(true, test.get("isOverBudget"));
    assertEquals("OVER BUDGET! You have exceeded your budget by $2.00\n", test.get("warnings"));
    assertEquals(true, test.get("hasWarnings"));
  }

  /** Invalid input. */
  @Test
  public void getBudgetReport_userNotFound_returnsErrorMap() {
    when(jdbcTemplate.queryForObject(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any(), 
      eq(userId)))
        .thenThrow(new RuntimeException());
    Map<String, Object> test = service.getBudgetReport(userId);
    assertEquals(Map.of("error", "User not found"), test);
  }

  // ---------------------------------------------------------------------------
  // setBudgets
  // ---------------------------------------------------------------------------

  /** Typical valid input. */
  @Test
  public void setBudgets_validUpdate() {
    Map<String, Object> updates = Map.of("budget", 20.0);
    when(jdbcTemplate.queryForObject(
      anyString(),
      ArgumentMatchers.<RowMapper<User>>any(),
      eq(userId)))
        .thenReturn(user);
    
    service.setBudgets(userId, updates);

    verify(jdbcTemplate).update("UPDATE users SET budget = ? WHERE user_id = ?", 20.0, userId);
  }


  /** Atypical valid input. */
  @Test
  public void setBudgets_acceptsString() {
    Map<String, Object> updates = Map.of("budget", "20.0");
    when(jdbcTemplate.queryForObject(
      anyString(),
      ArgumentMatchers.<RowMapper<User>>any(),
      eq(userId)))
        .thenReturn(user);
    
    service.setBudgets(userId, updates);

    verify(jdbcTemplate).update("UPDATE users SET budget = ? WHERE user_id = ?", 20.0, userId);
  }

  /** Invalid inputs. */

  @Test
  public void setBudgets_userNotFound_throwsIllegalArgumentException() {
    Map<String, Object> updates = Map.of("test1", "test2");
    when(jdbcTemplate.queryForObject(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any(),
      eq(userId)))
        .thenThrow(new IllegalArgumentException("User not found"));
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      service.setBudgets(userId, updates);
    });

    assertTrue(exception.getMessage().contains("User not found"));
  }

  @Test
  public void setBudgets_invalidFormat_throwsIllegalArgumentException() {
    Map<String, Object> updates = Map.of("budget", true);
    user.setBudget(100.0);
    when(jdbcTemplate.queryForObject(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any(), 
      eq(userId)))
        .thenReturn(user);
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      service.setBudgets(userId, updates);
    });

    assertTrue(exception.getMessage().contains("Invalid budget format"));
  }

  @Test
  public void setBudgets_negativeBudget_throwsIllegalArgumentException() {
    Map<String, Object> updates = Map.of("budget", -100.0);
    user.setBudget(100.0);
    when(jdbcTemplate.queryForObject(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any(), 
      eq(userId)))
        .thenReturn(user);
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      service.setBudgets(userId, updates);
    });

    assertTrue(exception.getMessage().contains("Budget cannot be negative"));
  }

  // ---------------------------------------------------------------------------
  // weeklySummary
  // ---------------------------------------------------------------------------

  /** Typical valid input. */
  @Test
  public void weeklySummary_returnsTransactions() {
    LocalDate oneWeekAgo = LocalDate.now().minusDays(7);

    List<Transaction> expected = List.of(
      new Transaction(userId, 100.0, "c1", "d1"),
      new Transaction(userId, 50.0, "c2", "d2")
    );

    when(jdbcTemplate.query(
      anyString(), 
      ArgumentMatchers.<RowMapper<Transaction>>any(), 
      eq(userId), 
      eq(oneWeekAgo)))
        .thenReturn(expected);

    List<Transaction> result = service.weeklySummary(userId);

    assertEquals(expected, result);
  }

  /** Atypical valid input. */
  @Test
  public void weeklySummary_atypical_noTransactions_returnsEmptyList() {
    LocalDate oneWeekAgo = LocalDate.now().minusDays(7);

    when(jdbcTemplate.query(
      anyString(), 
      ArgumentMatchers.<RowMapper<Transaction>>any(), 
      eq(userId), 
      eq(oneWeekAgo)))
        .thenReturn(Collections.emptyList());

    List<Transaction> result = service.weeklySummary(userId);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  /** Invalid input. */
  @Test
  public void weeklySummary_invalidInput_throwsException() {
    UUID userId = null;
    
    when(jdbcTemplate.query(
      anyString(), 
      ArgumentMatchers.<RowMapper<Transaction>>any(), 
      eq(userId), 
      any()))
        .thenThrow(new IllegalArgumentException("DB Error"));

    assertThrows(IllegalArgumentException.class, () -> service.weeklySummary(userId));
  }

  // ---------------------------------------------------------------------------
  // totalLast7Days
  // ---------------------------------------------------------------------------

  /** Typical valid input. */
  @Test
  public void totalLast7Days_returnsSum() {
    LocalDate oneWeekAgo = LocalDate.now().minusDays(7);

    when(jdbcTemplate.queryForObject(
      anyString(),
      eq(Double.class),
      eq(userId),
      eq(oneWeekAgo)
    )).thenReturn(150.0);

    double result = service.totalLast7Days(userId);

    assertEquals(150.0, result);
  }

  /** Atypical valid input. */
  @Test
  public void totalLast7Days_noTransactions_returnsZero() {
    LocalDate oneWeekAgo = LocalDate.now().minusDays(7);

    when(jdbcTemplate.queryForObject(
      anyString(),
      eq(Double.class),
      eq(userId),
      eq(oneWeekAgo)
    )).thenReturn(0.0);

    double result = service.totalLast7Days(userId);

    assertEquals(0.0, result);
  }

  /** Invalid input. */
  @Test
  public void totalLast7Days_invalidInput_returnsZero() {
    LocalDate oneWeekAgo = LocalDate.now().minusDays(7);

    when(jdbcTemplate.queryForObject(
      anyString(),
      eq(Double.class),
      eq(userId),
      eq(oneWeekAgo)
    )).thenThrow(new RuntimeException("DB Error"));

    double result = service.totalLast7Days(userId);

    assertEquals(0.0, result);
  }

}
