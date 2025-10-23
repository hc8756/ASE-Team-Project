package dev.ase.teamproject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import dev.ase.teamproject.model.Transaction;
import dev.ase.teamproject.model.User;
import dev.ase.teamproject.service.MockApiService;
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

  @Test
  public void getUser_userExists_returnsUser() {
    when(jdbcTemplate.queryForObject(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any(), 
      eq(userId)))
        .thenReturn(user);
  }

  @Test
  public void getUser_userDoesNotExist_returnsEmptyOptional() {
    when(jdbcTemplate.queryForObject(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any(), 
      eq(userId)))
        .thenThrow(new RuntimeException());

    Optional<User> test = service.getUser(userId);
    assertTrue(test.isEmpty());
  }

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

  @Test
  public void deleteUser_userExists_returnsTrue() {
    when(jdbcTemplate.update(anyString(), eq(userId)))
        .thenReturn(1);
    boolean result = service.deleteUser(userId);
    assertTrue(result);
  }

  @Test
  public void deleteUser_userDoesNotExist_returnsFalse() {
    when(jdbcTemplate.update(anyString(), eq(userId)))
        .thenReturn(0);
    boolean result = service.deleteUser(userId);
    assertFalse(result);

  }

  @Test
  public void addTransaction_validTransaction_returnsSavedTransaction() {
    when(jdbcTemplate.update(
        anyString(),
        eq(transaction.getUserId()),
        eq(transaction.getDescription()),
        eq(transaction.getAmount()),
        eq(transaction.getCategory())))
        .thenReturn(1);
    Transaction test = service.addTransaction(transaction);
    assertNotNull(test);
    assertEquals(transaction.getUserId(), test.getUserId());
    assertEquals(transaction.getDescription(), test.getDescription());
    assertEquals(transaction.getAmount(), test.getAmount());
    assertEquals(transaction.getCategory(), test.getCategory());
  }

  @Test
  public void addTransaction_failedTransaction_throwsRuntimeException() {
    when(jdbcTemplate.update(
        anyString(),
        eq(transaction.getUserId()),
        eq(transaction.getDescription()),
        eq(transaction.getAmount()),
        eq(transaction.getCategory())))
        .thenThrow(new RuntimeException());

    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      service.addTransaction(transaction);
    });

    assertTrue(exception.getMessage().contains("Failed to create transaction"));
  }

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

  @Test
  public void deleteTransaction_transactionExists_returnsTrue() {
    when(jdbcTemplate.update(anyString(), eq(transactionId)))
        .thenReturn(1);
    boolean result = service.deleteTransaction(transactionId);
    assertTrue(result);
  }

  @Test
  public void deleteTransaction_transactionDoesNotExist_returnsFalse() {
    when(jdbcTemplate.update(anyString(), eq(transactionId)))
        .thenReturn(0);
    boolean result = service.deleteTransaction(transactionId);
    assertFalse(result);
  }

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

}
