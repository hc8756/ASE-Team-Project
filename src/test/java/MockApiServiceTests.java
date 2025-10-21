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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import model.Transaction;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import service.MockApiService;

/**
 * This class contains the unit tests for the MockApiTests class.
 */

public class MockApiServiceTests {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private MockApiService service;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);

  }

  @Test
  public void testViewAllUsers() {
    List<User> users = List.of(new User(), new User());
    when(jdbcTemplate.query(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any()))
        .thenReturn(users);
    List<User> test = service.viewAllUsers();
    assertEquals(2, test.size());
  }

  @Test
  public void testViewAllUsersNoUsers() {
    List<User> users = List.of();
    when(jdbcTemplate.query(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any()))
        .thenReturn(users);
    List<User> test = service.viewAllUsers();
    assertEquals(0, test.size());
  }

  @Test
  public void testGetUserFound() {
    User user = new User();
    UUID userId = user.getUserId();
    when(jdbcTemplate.queryForObject(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any(), 
      eq(userId)))
        .thenReturn(user);
  }

  @Test
  public void testGetUserNotFound() {
    User user = new User();
    UUID userId = user.getUserId();
    when(jdbcTemplate.queryForObject(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any(), 
      eq(userId)))
        .thenThrow(new RuntimeException());

    Optional<User> test = service.getUser(userId);
    assertTrue(test.isEmpty());
  }

  @Test
  public void testAddUserSuccess() {
    User user = new User();
    UUID userId = user.getUserId();
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
  public void testDeleteUserSuccess() {
    User user = new User();
    UUID userId = user.getUserId();
    when(jdbcTemplate.update(anyString(), eq(userId)))
        .thenReturn(1);
    boolean result = service.deleteUser(userId);
    assertTrue(result);
  }

  @Test
  public void testDeleteUserFail() {
    User user = new User();
    UUID userId = user.getUserId();
    when(jdbcTemplate.update(anyString(), eq(userId)))
        .thenReturn(0);
    boolean result = service.deleteUser(userId);
    assertFalse(result);

  }

  // @Test
  // public void testViewAllTransactions() {

  // }

  // @Test
  // public void testGetAllTransactions() {

  // }

  @Test
  public void testAddTransactionSuccess() {
    UUID userId = UUID.randomUUID();
    Transaction transaction = new Transaction(userId, 10.0, "category", "description");
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
  public void testAddTransactionFail() {
    UUID userId = UUID.randomUUID();
    Transaction transaction = new Transaction(userId, 10.0, "category", "description");
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
  public void testGetTransactionsByUserFail() {
    UUID userId = UUID.randomUUID();
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<User>>any(), eq(userId)))
        .thenThrow(new RuntimeException("Error"));
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      service.getTransactionsByUser(userId);
    });
    assertTrue(exception.getMessage().contains("Failed to get transactions: "));
    assertTrue(exception.getMessage().contains("Error"));

  }

  // @Test
  // public void testGetTransactionsByUserSuccess() {

  // }

  @Test
  public void testUpdateTransactionNotFound() {
    UUID transactionId = UUID.randomUUID();
    Map<String, Object> updates = Map.of("key1", "key2");
    when(jdbcTemplate.queryForObject(
        anyString(),
        ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId))).thenThrow(new RuntimeException());

    Optional<Transaction> test = service.updateTransaction(transactionId, updates);
    assertTrue(test.isEmpty());
  }

  @Test
  public void testUpdateTransactionSuccess() {
    User user = new User();
    UUID userId = user.getUserId();
    Transaction transaction = new Transaction(userId, 1.0, "category", "description");
    UUID transactionId = transaction.getTransactionId();

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
  public void testUpdateTransactionSuccessAmountisNumber() {
    User user = new User();
    UUID userId = user.getUserId();
    Transaction transaction = new Transaction(userId, 1.0, "category", "description");
    UUID transactionId = transaction.getTransactionId();

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
  public void testUpdateTransactionFail() {
    User user = new User();
    UUID userId = user.getUserId();
    Transaction transaction = new Transaction(userId, 1.0, "category", "description");
    UUID transactionId = transaction.getTransactionId();

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
  public void testDeleteTransactionSuccess() {
    UUID userId = UUID.randomUUID();
    Transaction transaction = new Transaction(userId, 10.0, "category", "description");
    UUID transactionId = transaction.getTransactionId();
    when(jdbcTemplate.update(anyString(), eq(transactionId)))
        .thenReturn(1);
    boolean result = service.deleteTransaction(transactionId);
    assertTrue(result);
  }

  @Test
  public void testDeleteTransactionFail() {
    UUID userId = UUID.randomUUID();
    Transaction transaction = new Transaction(userId, 10.0, "category", "description");
    UUID transactionId = transaction.getTransactionId();
    when(jdbcTemplate.update(anyString(), eq(transactionId)))
        .thenReturn(0);
    boolean result = service.deleteTransaction(transactionId);
    assertFalse(result);
  }

  @Test
  public void testGetBudgetsTextBlockUserNotFound() {
    UUID userId = UUID.randomUUID();
    when(jdbcTemplate.queryForObject(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any(), 
      eq(userId)))
        .thenThrow(new RuntimeException());
    String test = service.getBudgetsTextBlock(userId);
    assertEquals("User not found", test);
  }

  @Test
  public void testGetBudgetsTextBlockUserFound() {
    User user = new User();
    user.setUsername("User");
    UUID userId = user.getUserId();
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
  public void testGetBudgetWarningsTextUserNotFound() {
    UUID userId = UUID.randomUUID();
    when(jdbcTemplate.queryForObject(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any(), 
      eq(userId)))
        .thenThrow(new RuntimeException());
    String test = service.getBudgetWarningsText(userId);
    assertEquals("User not found", test);
  }

  @Test
  public void testGetBudgetWarningsTextNoBudgetLeft() {
    User user = new User();
    UUID userId = user.getUserId();
    user.setBudget(5.0);

    List<Transaction> transactions = List.of(
        new Transaction(userId, 10.0, "category", "description"));

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
    assertTrue(test.contains("⚠️ OVER BUDGET! You have exceeded your budget by $"));
    assertTrue(test.contains("5.0"));
  }

  @Test
  public void testGetBudgetWarningsTextTenthLeft() {
    User user = new User();
    UUID userId = user.getUserId();
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
    assertTrue(test.contains("⚠️ Budget warning: Only"));
    assertTrue(test.contains("0.5"));
  }

  @Test
  public void testGetMonthlySummaryUserNotFound() {
    UUID userId = UUID.randomUUID();
    when(jdbcTemplate.queryForObject(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any(), 
      eq(userId)))
        .thenThrow(new RuntimeException());
    String test = service.getMonthlySummary(userId);
    assertEquals("User not found", test);
  }

  @Test
  public void testGetBudgetReportUserNotFound() {
    UUID userId = UUID.randomUUID();
    when(jdbcTemplate.queryForObject(
      anyString(), 
      ArgumentMatchers.<RowMapper<User>>any(), 
      eq(userId)))
        .thenThrow(new RuntimeException());
    Map<String, Object> test = service.getBudgetReport(userId);
    assertEquals(Map.of("error", "User not found"), test);
  }

  @Test
  public void getBudgetReportSuccess() {
    User user = new User("User", "email", 100.0);
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
  public void testSetBudgetsUserNotFound() {
    UUID userId = UUID.randomUUID();
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
  public void testSetBudgetsInvalidFormat() {
    User user = new User();
    UUID userId = user.getUserId();
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
  public void testSetBudgetsNegativeBudget() {
    User user = new User();
    UUID userId = user.getUserId();
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
