package dev.ase.teamproject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import java.util.HashMap;
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
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Unit tests for the {@link MockApiService} class.
 *
 * <p>This test suite validates all public methods of the MockApiService,
 * covering valid and invalid equivalence partitions including boundary values.
 * Tests use Mockito to mock the JdbcTemplate dependency and share common setup
 * through {@code @BeforeEach}.
 *
 * <h2>Equivalence Partitions by Method</h2>
 *
 * <h3>1. viewAllUsers()</h3>
 * <ul>
 *   <li>P1: (Valid) Multiple users exist - returns list of users</li>
 *   <li>P2: (Valid/Boundary) No users exist - returns empty list</li>
 *   <li>P3: (Invalid) Database error - throws exception</li>
 * </ul>
 *
 * <h3>2. getUser(UUID)</h3>
 * <ul>
 *   <li>P1: (Valid) User exists - returns Optional with user</li>
 *   <li>P2: (Valid) User does not exist - returns empty Optional</li>
 *   <li>P3: (Invalid) Database error - returns empty Optional</li>
 * </ul>
 *
 * <h3>3. addUser(User)</h3>
 * <ul>
 *   <li>P1: (Valid) User without userId - generates new UUID</li>
 *   <li>P2: (Valid) User with existing userId - uses provided UUID</li>
 *   <li>P3: (Invalid) Database returns null - user has null ID</li>
 *   <li>P4: (Invalid) Database error - throws exception</li>
 * </ul>
 *
 * <h3>4. deleteUser(UUID)</h3>
 * <ul>
 *   <li>P1: (Valid) User exists - returns true</li>
 *   <li>P2: (Valid) User does not exist - returns false</li>
 *   <li>P3: (Invalid) Database error - throws exception</li>
 * </ul>
 *
 * <h3>5. viewAllTransactions()</h3>
 * <ul>
 *   <li>P1: (Valid) Transactions exist - returns list</li>
 *   <li>P2: (Valid/Boundary) No transactions - returns empty list</li>
 *   <li>P3: (Invalid) Database error - throws exception</li>
 * </ul>
 *
 * <h3>6. getTransaction(UUID)</h3>
 * <ul>
 *   <li>P1: (Valid) Transaction exists - returns Optional with transaction</li>
 *   <li>P2: (Valid) Transaction not found - returns empty Optional</li>
 *   <li>P3: (Invalid) Database error - returns empty Optional</li>
 * </ul>
 *
 * <h3>7. addTransaction(Transaction)</h3>
 * <ul>
 *   <li>P1: (Valid) All fields valid - returns saved transaction</li>
 *   <li>P2: (Valid/Edge) DB returns null - returns original transaction</li>
 *   <li>P3: (Invalid) Null userId - throws IllegalArgumentException</li>
 *   <li>P4: (Invalid) Null description - throws IllegalArgumentException</li>
 *   <li>P5: (Invalid) Blank description - throws IllegalArgumentException</li>
 *   <li>P6: (Invalid) Amount zero - throws IllegalArgumentException</li>
 *   <li>P7: (Invalid) Amount negative - throws IllegalArgumentException</li>
 *   <li>P8: (Invalid) Null category - throws IllegalArgumentException</li>
 *   <li>P9: (Invalid) Blank category - throws IllegalArgumentException</li>
 *   <li>P10: (Invalid) Foreign key violation - throws IllegalArgumentException</li>
 *   <li>P11: (Invalid) Invalid category enum - throws IllegalArgumentException</li>
 *   <li>P12: (Invalid) General database error - throws IllegalStateException</li>
 * </ul>
 *
 * <h3>8. getTransactionsByUser(UUID)</h3>
 * <ul>
 *   <li>P1: (Valid) User has transactions - returns list</li>
 *   <li>P2: (Valid/Boundary) User has no transactions - returns empty list</li>
 *   <li>P3: (Invalid) Database error - throws IllegalStateException</li>
 * </ul>
 *
 * <h3>9. updateTransaction(UUID, Map)</h3>
 * <ul>
 *   <li>P1: (Valid) Valid updates - returns updated transaction</li>
 *   <li>P2: (Valid) Amount as Number - updates correctly</li>
 *   <li>P3: (Invalid) Transaction not found - throws IllegalArgumentException</li>
 *   <li>P4: (Invalid) Empty updates map - throws IllegalArgumentException</li>
 *   <li>P5: (Invalid) Description not string - throws IllegalArgumentException</li>
 *   <li>P6: (Invalid) Blank description - throws IllegalArgumentException</li>
 *   <li>P7: (Invalid) Amount not number/string - throws IllegalArgumentException</li>
 *   <li>P8: (Invalid) Amount invalid string - throws IllegalArgumentException</li>
 *   <li>P9: (Invalid) Amount zero or negative - throws IllegalArgumentException</li>
 *   <li>P10: (Invalid) Category not string - throws IllegalArgumentException</li>
 *   <li>P11: (Invalid) Blank category - throws IllegalArgumentException</li>
 *   <li>P12: (Invalid) Invalid category value - throws IllegalArgumentException</li>
 *   <li>P13: (Invalid) DB category enum error - throws IllegalArgumentException</li>
 *   <li>P14: (Invalid) Update fails (0 rows) - throws IllegalStateException</li>
 * </ul>
 *
 * <h3>10. deleteTransaction(UUID)</h3>
 * <ul>
 *   <li>P1: (Valid) Transaction exists - returns true</li>
 *   <li>P2: (Valid) Transaction not found - returns false</li>
 *   <li>P3: (Invalid) Database error - throws exception</li>
 * </ul>
 *
 * <h3>11. getBudgetsTextBlock(UUID)</h3>
 * <ul>
 *   <li>P1: (Valid) User found with transactions - returns summary</li>
 *   <li>P2: (Valid) User found, no transactions - shows zero spent</li>
 *   <li>P3: (Invalid) User not found - returns "User not found"</li>
 * </ul>
 *
 * <h3>12. getBudgetWarningsText(UUID)</h3>
 * <ul>
 *   <li>P1: (Valid) User near budget limit (&lt;10% remaining) - returns warning</li>
 *   <li>P2: (Valid) User over budget - returns over budget warning</li>
 *   <li>P3: (Valid) User has healthy budget (&gt;10% remaining) - returns empty</li>
 *   <li>P4: (Invalid) User not found - returns "User not found"</li>
 * </ul>
 *
 * <h3>13. getMonthlySummary(UUID)</h3>
 * <ul>
 *   <li>P1: (Valid) User with current month transactions - returns summary</li>
 *   <li>P2: (Valid/Edge) Transactions with null dates - ignored in summary</li>
 *   <li>P3: (Invalid) User not found - returns "User not found"</li>
 * </ul>
 *
 * <h3>14. getBudgetReport(UUID)</h3>
 * <ul>
 *   <li>P1: (Valid) User found - returns complete report map</li>
 *   <li>P2: (Valid) User over budget - report shows warnings</li>
 *   <li>P3: (Invalid) User not found - returns error map</li>
 * </ul>
 *
 * <h3>15. setBudgets(UUID, Map)</h3>
 * <ul>
 *   <li>P1: (Valid) Budget as Number - updates successfully</li>
 *   <li>P2: (Valid) Budget as String - parses and updates</li>
 *   <li>P3: (Valid/Boundary) Budget is zero - updates successfully</li>
 *   <li>P4: (Invalid) User not found - throws IllegalArgumentException</li>
 *   <li>P5: (Invalid) Invalid budget format - throws IllegalArgumentException</li>
 *   <li>P6: (Invalid) Negative budget - throws IllegalArgumentException</li>
 *   <li>P7: (Valid/Edge) No budget key in map - no update performed</li>
 * </ul>
 *
 * <h3>16. weeklySummary(UUID)</h3>
 * <ul>
 *   <li>P1: (Valid) User has recent transactions - returns list</li>
 *   <li>P2: (Valid/Boundary) No transactions in week - returns empty list</li>
 *   <li>P3: (Invalid) Database error - throws exception</li>
 * </ul>
 *
 * <h3>17. totalLast7Days(UUID)</h3>
 * <ul>
 *   <li>P1: (Valid) Transactions exist - returns sum</li>
 *   <li>P2: (Valid/Boundary) No transactions - returns 0.0</li>
 *   <li>P3: (Valid/Edge) Database returns null - returns 0.0</li>
 *   <li>P4: (Invalid) Database error - returns 0.0</li>
 * </ul>
 *
 * <h3>18. isUsernameExists(String, UUID)</h3>
 * <ul>
 *   <li>P1: (Valid) Username exists (no exclusion) - returns true</li>
 *   <li>P2: (Valid) Username does not exist - returns false</li>
 *   <li>P3: (Valid) Username exists but excluded - returns false</li>
 *   <li>P4: (Valid) Username exists for other user - returns true</li>
 *   <li>P5: (Valid/Edge) Count is null - returns false</li>
 * </ul>
 *
 * <h3>19. isEmailExists(String, UUID)</h3>
 * <ul>
 *   <li>P1: (Valid) Email exists (no exclusion) - returns true</li>
 *   <li>P2: (Valid) Email does not exist - returns false</li>
 *   <li>P3: (Valid) Email exists but excluded - returns false</li>
 *   <li>P4: (Valid) Email exists for other user - returns true</li>
 *   <li>P5: (Valid/Edge) Count is null - returns false</li>
 * </ul>
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
   * Creates a default user and transaction for use in tests.
   */
  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    user = new User();
    userId = UUID.randomUUID();
    user.setUserId(userId);
    transaction = new Transaction(userId, 10.0, "FOOD", "description");
    transactionId = UUID.randomUUID();
    transaction.setTransactionId(transactionId);
  }

  // ===========================================================================
  // viewAllUsers
  // ===========================================================================

  /**
   * Tests viewAllUsers when multiple users exist.
   *
   * <p>Partition: P1 (Valid) - Multiple users exist.
   */
  @Test
  public void viewAllUsers_twoUsersExist_returnsListOfTwo() {
    List<User> users = List.of(new User(), new User());
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<User>>any()))
        .thenReturn(users);

    List<User> result = service.viewAllUsers();

    assertEquals(2, result.size());
  }

  /**
   * Tests viewAllUsers when no users exist.
   *
   * <p>Partition: P2 (Valid/Boundary) - No users exist.
   */
  @Test
  public void viewAllUsers_noUsersExist_returnsEmptyList() {
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<User>>any()))
        .thenReturn(List.of());

    List<User> result = service.viewAllUsers();

    assertEquals(0, result.size());
  }

  // ===========================================================================
  // getUser
  // ===========================================================================

  /**
   * Tests getUser when user exists.
   *
   * <p>Partition: P1 (Valid) - User exists.
   */
  @Test
  public void getUser_userExists_returnsUser() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)))
        .thenReturn(user);

    Optional<User> result = service.getUser(userId);

    assertTrue(result.isPresent());
    assertEquals(userId, result.get().getUserId());
  }

  /**
   * Tests getUser when user does not exist.
   *
   * <p>Partition: P2 (Valid) - User does not exist.
   */
  @Test
  public void getUser_userDoesNotExist_returnsEmptyOptional() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)))
        .thenReturn(null);

    Optional<User> result = service.getUser(userId);

    assertTrue(result.isEmpty());
  }

  /**
   * Tests getUser when database throws exception.
   *
   * <p>Partition: P3 (Invalid) - Database error.
   */
  @Test
  public void getUser_databaseError_returnsEmptyOptional() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)))
        .thenThrow(new RuntimeException("DB error"));

    Optional<User> result = service.getUser(userId);

    assertFalse(result.isPresent());
  }

  // ===========================================================================
  // addUser
  // ===========================================================================

  /**
   * Tests addUser with user that has no userId (generates new UUID).
   *
   * <p>Partition: P1 (Valid) - User without userId.
   */
  @Test
  public void addUser_noUserId_returnsUserWithGeneratedId() {
    User newUser = new User();
    newUser.setUsername("testuser");
    newUser.setEmail("test@example.com");
    newUser.setBudget(100.0);
    UUID generatedId = UUID.randomUUID();

    when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class), anyString(), anyString(),
        anyDouble()))
        .thenReturn(generatedId);

    User result = service.addUser(newUser);

    assertEquals(generatedId, result.getUserId());
  }

  /**
   * Tests addUser with user that already has a userId.
   *
   * <p>Partition: P2 (Valid) - User with existing userId.
   */
  @Test
  public void addUser_withExistingUserId_usesProvidedId() {
    UUID existingId = UUID.randomUUID();
    User newUser = new User();
    newUser.setUserId(existingId);
    newUser.setUsername("testuser");
    newUser.setEmail("test@example.com");
    newUser.setBudget(100.0);

    when(jdbcTemplate.update(anyString(), eq(existingId), anyString(), anyString(), anyDouble()))
        .thenReturn(1);

    User result = service.addUser(newUser);

    assertEquals(existingId, result.getUserId());
    verify(jdbcTemplate).update(anyString(), eq(existingId), anyString(), anyString(), anyDouble());
  }

  /**
   * Tests addUser when database returns null UUID.
   *
   * <p>Partition: P3 (Invalid) - Database returns null.
   */
  @Test
  public void addUser_databaseReturnsNull_userHasNullId() {
    User newUser = new User();
    when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class), nullable(String.class),
        nullable(String.class), anyDouble()))
        .thenReturn(null);

    User result = service.addUser(newUser);

    assertNull(result.getUserId());
  }

  /**
   * Tests addUser when database throws exception.
   *
   * <p>Partition: P4 (Invalid) - Database error.
   */
  @Test
  public void addUser_databaseError_throwsException() {
    User newUser = new User();
    when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class), nullable(String.class),
        nullable(String.class), anyDouble()))
        .thenThrow(new RuntimeException("DB error"));

    assertThrows(RuntimeException.class, () -> service.addUser(newUser));
  }

  // ===========================================================================
  // deleteUser
  // ===========================================================================

  /**
   * Tests deleteUser when user exists.
   *
   * <p>Partition: P1 (Valid) - User exists.
   */
  @Test
  public void deleteUser_userExists_returnsTrue() {
    when(jdbcTemplate.update(anyString(), eq(userId))).thenReturn(1);

    boolean result = service.deleteUser(userId);

    assertTrue(result);
  }

  /**
   * Tests deleteUser when user does not exist.
   *
   * <p>Partition: P2 (Valid) - User does not exist.
   */
  @Test
  public void deleteUser_userDoesNotExist_returnsFalse() {
    when(jdbcTemplate.update(anyString(), eq(userId))).thenReturn(0);

    boolean result = service.deleteUser(userId);

    assertFalse(result);
  }

  /**
   * Tests deleteUser when database throws exception.
   *
   * <p>Partition: P3 (Invalid) - Database error.
   */
  @Test
  public void deleteUser_databaseError_throwsException() {
    when(jdbcTemplate.update(anyString(), eq(userId)))
        .thenThrow(new RuntimeException("DB error"));

    assertThrows(RuntimeException.class, () -> service.deleteUser(userId));
  }

  // ===========================================================================
  // viewAllTransactions
  // ===========================================================================

  /**
   * Tests viewAllTransactions when transactions exist.
   *
   * <p>Partition: P1 (Valid) - Transactions exist.
   */
  @Test
  public void viewAllTransactions_transactionsExist_returnsList() {
    Transaction t1 = new Transaction(userId, 50.0, "FOOD", "Lunch");
    Transaction t2 = new Transaction(userId, 120.0, "SHOPPING", "Shoes");
    List<Transaction> mockTransactions = List.of(t1, t2);

    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any()))
        .thenReturn(mockTransactions);

    List<Transaction> result = service.viewAllTransactions();

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("FOOD", result.get(0).getCategory());
  }

  /**
   * Tests viewAllTransactions when no transactions exist.
   *
   * <p>Partition: P2 (Valid/Boundary) - No transactions.
   */
  @Test
  public void viewAllTransactions_noTransactions_returnsEmptyList() {
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any()))
        .thenReturn(List.of());

    List<Transaction> result = service.viewAllTransactions();

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  /**
   * Tests viewAllTransactions when database throws exception.
   *
   * <p>Partition: P3 (Invalid) - Database error.
   */
  @Test
  public void viewAllTransactions_databaseError_throwsException() {
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any()))
        .thenThrow(new RuntimeException("Error"));

    assertThrows(RuntimeException.class, () -> service.viewAllTransactions());
  }

  // ===========================================================================
  // getTransaction
  // ===========================================================================

  /**
   * Tests getTransaction when transaction exists.
   *
   * <p>Partition: P1 (Valid) - Transaction exists.
   */
  @Test
  public void getTransaction_transactionExists_returnsOptionalWithTransaction() {
    Transaction tx = new Transaction(userId, 45.0, "FOOD", "Dinner");
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId)))
        .thenReturn(tx);

    Optional<Transaction> result = service.getTransaction(transactionId);

    assertTrue(result.isPresent());
    assertEquals("FOOD", result.get().getCategory());
    assertEquals(45.0, result.get().getAmount());
  }

  /**
   * Tests getTransaction when transaction not found.
   *
   * <p>Partition: P2 (Valid) - Transaction not found.
   */
  @Test
  public void getTransaction_transactionNotFound_returnsEmptyOptional() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId)))
        .thenReturn(null);

    Optional<Transaction> result = service.getTransaction(transactionId);

    assertFalse(result.isPresent());
  }

  /**
   * Tests getTransaction when database throws exception.
   *
   * <p>Partition: P3 (Invalid) - Database error.
   */
  @Test
  public void getTransaction_databaseError_returnsEmptyOptional() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId)))
        .thenThrow(new RuntimeException("Error"));

    Optional<Transaction> result = service.getTransaction(transactionId);

    assertFalse(result.isPresent());
  }

  // ===========================================================================
  // addTransaction
  // ===========================================================================

  /**
   * Tests addTransaction with all valid fields.
   *
   * <p>Partition: P1 (Valid) - All fields valid.
   */
  @Test
  public void addTransaction_validTransaction_returnsSavedTransaction() {
    UUID validUserId = UUID.randomUUID();
    Transaction tx = new Transaction(validUserId, 10.0, "FOOD", "description");
    UUID txId = UUID.randomUUID();
    LocalDateTime createdTime = LocalDateTime.of(2025, 10, 23, 12, 0);
    LocalDate createdDate = LocalDate.of(2025, 10, 23);

    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(tx.getUserId()), eq(tx.getDescription()), eq(tx.getAmount()), eq(tx.getCategory())))
        .thenAnswer(invocation -> {
          var rs = mock(java.sql.ResultSet.class);
          when(rs.getObject("transaction_id", UUID.class)).thenReturn(txId);
          when(rs.getTimestamp("created_time")).thenReturn(Timestamp.valueOf(createdTime));
          when(rs.getDate("created_date")).thenReturn(Date.valueOf(createdDate));
          var rowMapper = invocation.getArgument(1);
          return ((RowMapper<Transaction>) rowMapper).mapRow(rs, 0);
        });

    Transaction result = service.addTransaction(tx);

    assertNotNull(result);
    assertEquals(txId, result.getTransactionId());
    assertEquals(createdTime, result.getTimestamp());
    assertEquals(createdDate, result.getDate());
  }

  /**
   * Tests addTransaction when database returns null.
   *
   * <p>Partition: P2 (Valid/Edge) - DB returns null.
   */
  @Test
  public void addTransaction_databaseReturnsNull_returnsOriginalTransaction() {
    UUID validUserId = UUID.randomUUID();
    Transaction tx = new Transaction(validUserId, 75.0, "TRANSPORTATION", "subway");

    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        any(), any(), any(), any()))
        .thenReturn(null);

    Transaction result = service.addTransaction(tx);

    assertNotNull(result);
    assertEquals(75.0, result.getAmount());
    assertEquals("TRANSPORTATION", result.getCategory());
    assertNull(result.getTransactionId());
  }

  /**
   * Tests addTransaction with null userId.
   *
   * <p>Partition: P3 (Invalid) - Null userId.
   */
  @Test
  public void addTransaction_nullUserId_throwsIllegalArgumentException() {
    Transaction tx = new Transaction(null, 10.0, "FOOD", "description");

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.addTransaction(tx));

    assertEquals("User ID is required", exception.getMessage());
  }

  /**
   * Tests addTransaction with null description.
   *
   * <p>Partition: P4 (Invalid) - Null description.
   */
  @Test
  public void addTransaction_nullDescription_throwsIllegalArgumentException() {
    Transaction tx = new Transaction();
    tx.setUserId(UUID.randomUUID());
    tx.setAmount(10.0);
    tx.setCategory("FOOD");
    tx.setDescription(null);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.addTransaction(tx));

    assertEquals("Description is required", exception.getMessage());
  }

  /**
   * Tests addTransaction with blank description.
   *
   * <p>Partition: P5 (Invalid) - Blank description.
   */
  @Test
  public void addTransaction_blankDescription_throwsIllegalArgumentException() {
    Transaction tx = new Transaction(UUID.randomUUID(), 10.0, "FOOD", "   ");

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.addTransaction(tx));

    assertEquals("Description is required", exception.getMessage());
  }

  /**
   * Tests addTransaction with zero amount.
   *
   * <p>Partition: P6 (Invalid) - Amount zero.
   */
  @Test
  public void addTransaction_zeroAmount_throwsIllegalArgumentException() {
    Transaction tx = new Transaction(UUID.randomUUID(), 0.0, "FOOD", "description");

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.addTransaction(tx));

    assertEquals("Amount must be greater than 0", exception.getMessage());
  }

  /**
   * Tests addTransaction with negative amount.
   *
   * <p>Partition: P7 (Invalid) - Amount negative.
   */
  @Test
  public void addTransaction_negativeAmount_throwsIllegalArgumentException() {
    Transaction tx = new Transaction(UUID.randomUUID(), -10.0, "FOOD", "description");

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.addTransaction(tx));

    assertEquals("Amount must be greater than 0", exception.getMessage());
  }

  /**
   * Tests addTransaction with null category.
   *
   * <p>Partition: P8 (Invalid) - Null category.
   */
  @Test
  public void addTransaction_nullCategory_throwsIllegalArgumentException() {
    Transaction tx = new Transaction();
    tx.setUserId(UUID.randomUUID());
    tx.setAmount(10.0);
    tx.setDescription("description");
    tx.setCategory(null);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.addTransaction(tx));

    assertEquals("Category is required", exception.getMessage());
  }

  /**
   * Tests addTransaction with blank category.
   *
   * <p>Partition: P9 (Invalid) - Blank category.
   */
  @Test
  public void addTransaction_blankCategory_throwsIllegalArgumentException() {
    Transaction tx = new Transaction(UUID.randomUUID(), 10.0, "   ", "description");

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.addTransaction(tx));

    assertEquals("Category is required", exception.getMessage());
  }

  /**
   * Tests addTransaction when foreign key constraint is violated.
   *
   * <p>Partition: P10 (Invalid) - Foreign key violation.
   */
  @Test
  public void addTransaction_foreignKeyViolation_throwsIllegalArgumentException() {
    UUID validUserId = UUID.randomUUID();
    Transaction tx = new Transaction(validUserId, 10.0, "FOOD", "description");

    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        any(), any(), any(), any()))
        .thenThrow(new RuntimeException("foreign key constraint violation"));

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.addTransaction(tx));

    assertTrue(exception.getMessage().contains("Invalid user ID"));
  }

  /**
   * Tests addTransaction when invalid category enum is used.
   *
   * <p>Partition: P11 (Invalid) - Invalid category enum.
   */
  @Test
  public void addTransaction_invalidCategoryEnum_throwsIllegalArgumentException() {
    UUID validUserId = UUID.randomUUID();
    Transaction tx = new Transaction(validUserId, 10.0, "INVALID_CATEGORY", "description");

    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        any(), any(), any(), any()))
        .thenThrow(new RuntimeException("transaction_category enum error"));

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.addTransaction(tx));

    assertTrue(exception.getMessage().contains("Invalid category"));
  }

  /**
   * Tests addTransaction when general database error occurs.
   *
   * <p>Partition: P12 (Invalid) - General database error.
   */
  @Test
  public void addTransaction_generalDatabaseError_throwsIllegalStateException() {
    UUID validUserId = UUID.randomUUID();
    Transaction tx = new Transaction(validUserId, 10.0, "FOOD", "description");

    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        any(), any(), any(), any()))
        .thenThrow(new RuntimeException("Connection timeout"));

    IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> service.addTransaction(tx));

    assertTrue(exception.getMessage().contains("Failed to create transaction"));
  }

  // ===========================================================================
  // getTransactionsByUser
  // ===========================================================================

  /**
   * Tests getTransactionsByUser when user has transactions.
   *
   * <p>Partition: P1 (Valid) - User has transactions.
   */
  @Test
  public void getTransactionsByUser_hasTransactions_returnsList() {
    Transaction t1 = new Transaction(userId, 25.0, "FOOD", "desc1");
    Transaction t2 = new Transaction(userId, 10.0, "SHOPPING", "desc2");
    List<Transaction> transactions = List.of(t1, t2);

    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(userId)))
        .thenReturn(transactions);

    List<Transaction> result = service.getTransactionsByUser(userId);

    assertEquals(2, result.size());
    assertEquals("FOOD", result.get(0).getCategory());
  }

  /**
   * Tests getTransactionsByUser when user has no transactions.
   *
   * <p>Partition: P2 (Valid/Boundary) - No transactions.
   */
  @Test
  public void getTransactionsByUser_noTransactions_returnsEmptyList() {
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(userId)))
        .thenReturn(List.of());

    List<Transaction> result = service.getTransactionsByUser(userId);

    assertTrue(result.isEmpty());
  }

  /**
   * Tests getTransactionsByUser when database throws exception.
   *
   * <p>Partition: P3 (Invalid) - Database error.
   */
  @Test
  public void getTransactionsByUser_databaseError_throwsIllegalStateException() {
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(userId)))
        .thenThrow(new RuntimeException("Error"));

    IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> service.getTransactionsByUser(userId));

    assertTrue(exception.getMessage().contains("Failed to get transactions"));
  }

  // ===========================================================================
  // updateTransaction
  // ===========================================================================

  /**
   * Tests updateTransaction with valid string updates.
   *
   * <p>Partition: P1 (Valid) - Valid updates.
   */
  @Test
  public void updateTransaction_validUpdates_returnsUpdatedTransaction() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId))).thenReturn(transaction);
    when(jdbcTemplate.update(anyString(), any(), any(), any(), eq(transactionId))).thenReturn(1);

    Transaction updatedTransaction = new Transaction();
    updatedTransaction.setDescription("new description");
    updatedTransaction.setAmount(7.0);
    updatedTransaction.setCategory("OTHER");

    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId))).thenReturn(updatedTransaction);

    Map<String, Object> updates = Map.of(
        "description", "new description",
        "amount", "7.0",
        "category", "OTHER");

    Optional<Transaction> result = service.updateTransaction(transactionId, updates);

    assertTrue(result.isPresent());
    assertEquals("new description", result.get().getDescription());
  }

  /**
   * Tests updateTransaction with amount as Number type.
   *
   * <p>Partition: P2 (Valid) - Amount as Number.
   */
  @Test
  public void updateTransaction_amountAsNumber_updatesCorrectly() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId))).thenReturn(transaction);
    when(jdbcTemplate.update(anyString(), any(), any(), any(), eq(transactionId))).thenReturn(1);

    Transaction updatedTransaction = new Transaction();
    updatedTransaction.setAmount(7.0);

    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId))).thenReturn(updatedTransaction);

    Map<String, Object> updates = Map.of("amount", 7);

    Optional<Transaction> result = service.updateTransaction(transactionId, updates);

    assertTrue(result.isPresent());
    assertEquals(7.0, result.get().getAmount());
  }

  /**
   * Tests updateTransaction when transaction not found.
   *
   * <p>Partition: P3 (Invalid) - Transaction not found.
   */
  @Test
  public void updateTransaction_transactionNotFound_throwsIllegalArgumentException() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId))).thenThrow(new RuntimeException("Not found"));

    Map<String, Object> updates = Map.of("description", "test");

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.updateTransaction(transactionId, updates));

    assertTrue(exception.getMessage().contains("Transaction not found"));
  }

  /**
   * Tests updateTransaction with empty updates map.
   *
   * <p>Partition: P4 (Invalid) - Empty updates map.
   */
  @Test
  public void updateTransaction_emptyUpdates_throwsIllegalArgumentException() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId))).thenReturn(transaction);

    Map<String, Object> updates = Map.of();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.updateTransaction(transactionId, updates));

    assertTrue(exception.getMessage().contains("No valid fields provided"));
  }

  /**
   * Tests updateTransaction when description is not a string.
   *
   * <p>Partition: P5 (Invalid) - Description not string.
   */
  @Test
  public void updateTransaction_descriptionNotString_throwsIllegalArgumentException() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId))).thenReturn(transaction);

    Map<String, Object> updates = Map.of("description", 123);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.updateTransaction(transactionId, updates));

    assertEquals("Description must be a string", exception.getMessage());
  }

  /**
   * Tests updateTransaction when description is blank.
   *
   * <p>Partition: P6 (Invalid) - Blank description.
   */
  @Test
  public void updateTransaction_blankDescription_throwsIllegalArgumentException() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId))).thenReturn(transaction);

    Map<String, Object> updates = Map.of("description", "   ");

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.updateTransaction(transactionId, updates));

    assertEquals("Description cannot be empty", exception.getMessage());
  }

  /**
   * Tests updateTransaction when amount is invalid type.
   *
   * <p>Partition: P7 (Invalid) - Amount not number/string.
   */
  @Test
  public void updateTransaction_amountInvalidType_throwsIllegalArgumentException() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId))).thenReturn(transaction);

    Map<String, Object> updates = Map.of("amount", true);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.updateTransaction(transactionId, updates));

    assertEquals("Amount must be a number", exception.getMessage());
  }

  /**
   * Tests updateTransaction when amount string is invalid.
   *
   * <p>Partition: P8 (Invalid) - Amount invalid string.
   */
  @Test
  public void updateTransaction_amountInvalidString_throwsIllegalArgumentException() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId))).thenReturn(transaction);

    Map<String, Object> updates = Map.of("amount", "not-a-number");

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.updateTransaction(transactionId, updates));

    assertEquals("Amount must be a valid number", exception.getMessage());
  }

  /**
   * Tests updateTransaction when amount is zero.
   *
   * <p>Partition: P9 (Invalid) - Amount zero or negative.
   */
  @Test
  public void updateTransaction_amountZero_throwsIllegalArgumentException() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId))).thenReturn(transaction);

    Map<String, Object> updates = Map.of("amount", 0);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.updateTransaction(transactionId, updates));

    assertEquals("Amount must be greater than 0", exception.getMessage());
  }

  /**
   * Tests updateTransaction when category is not a string.
   *
   * <p>Partition: P10 (Invalid) - Category not string.
   */
  @Test
  public void updateTransaction_categoryNotString_throwsIllegalArgumentException() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId))).thenReturn(transaction);

    Map<String, Object> updates = Map.of("category", 123);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.updateTransaction(transactionId, updates));

    assertEquals("Category must be a string", exception.getMessage());
  }

  /**
   * Tests updateTransaction when category is blank.
   *
   * <p>Partition: P11 (Invalid) - Blank category.
   */
  @Test
  public void updateTransaction_blankCategory_throwsIllegalArgumentException() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId))).thenReturn(transaction);

    Map<String, Object> updates = Map.of("category", "   ");

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.updateTransaction(transactionId, updates));

    assertEquals("Category cannot be empty", exception.getMessage());
  }

  /**
   * Tests updateTransaction when category value is invalid.
   *
   * <p>Partition: P12 (Invalid) - Invalid category value.
   */
  @Test
  public void updateTransaction_invalidCategoryValue_throwsIllegalArgumentException() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId))).thenReturn(transaction);

    Map<String, Object> updates = Map.of("category", "INVALID_CATEGORY");

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.updateTransaction(transactionId, updates));

    assertTrue(exception.getMessage().contains("Invalid category"));
  }

  /**
   * Tests updateTransaction when database throws category enum error.
   *
   * <p>Partition: P13 (Invalid) - DB category enum error.
   */
  @Test
  public void updateTransaction_dbCategoryEnumError_throwsIllegalArgumentException() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId))).thenReturn(transaction);

    DataAccessException dae = mock(DataAccessException.class);
    when(dae.getMessage()).thenReturn("transaction_category enum error");
    when(jdbcTemplate.update(anyString(), any(), any(), any(), eq(transactionId))).thenThrow(dae);

    Map<String, Object> updates = Map.of("category", "FOOD");

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.updateTransaction(transactionId, updates));

    assertTrue(exception.getMessage().contains("Invalid category"));
  }

  /**
   * Tests updateTransaction when update affects zero rows.
   *
   * <p>Partition: P14 (Invalid) - Update fails (0 rows).
   */
  @Test
  public void updateTransaction_zeroRowsAffected_throwsIllegalStateException() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(transactionId))).thenReturn(transaction);
    when(jdbcTemplate.update(anyString(), any(), any(), any(), eq(transactionId))).thenReturn(0);

    Map<String, Object> updates = Map.of("description", "new desc", "category", "OTHER");

    IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> service.updateTransaction(transactionId, updates));

    assertTrue(exception.getMessage().contains("Failed to update transaction"));
  }

  // ===========================================================================
  // deleteTransaction
  // ===========================================================================

  /**
   * Tests deleteTransaction when transaction exists.
   *
   * <p>Partition: P1 (Valid) - Transaction exists.
   */
  @Test
  public void deleteTransaction_transactionExists_returnsTrue() {
    when(jdbcTemplate.update(anyString(), eq(transactionId))).thenReturn(1);

    boolean result = service.deleteTransaction(transactionId);

    assertTrue(result);
  }

  /**
   * Tests deleteTransaction when transaction does not exist.
   *
   * <p>Partition: P2 (Valid) - Transaction not found.
   */
  @Test
  public void deleteTransaction_transactionNotFound_returnsFalse() {
    when(jdbcTemplate.update(anyString(), eq(transactionId))).thenReturn(0);

    boolean result = service.deleteTransaction(transactionId);

    assertFalse(result);
  }

  /**
   * Tests deleteTransaction when database throws exception.
   *
   * <p>Partition: P3 (Invalid) - Database error.
   */
  @Test
  public void deleteTransaction_databaseError_throwsException() {
    when(jdbcTemplate.update(anyString(), eq(transactionId)))
        .thenThrow(new RuntimeException("DB error"));

    assertThrows(RuntimeException.class, () -> service.deleteTransaction(transactionId));
  }

  // ===========================================================================
  // getBudgetsTextBlock
  // ===========================================================================

  /**
   * Tests getBudgetsTextBlock when user found with transactions.
   *
   * <p>Partition: P1 (Valid) - User found with transactions.
   */
  @Test
  public void getBudgetsTextBlock_userWithTransactions_returnsSummary() {
    user.setUsername("TestUser");
    user.setBudget(100.0);
    List<Transaction> transactions = List.of(
        new Transaction(userId, 30.0, "FOOD", "desc1"),
        new Transaction(userId, 20.0, "SHOPPING", "desc2"));

    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)))
        .thenReturn(user);
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(userId)))
        .thenReturn(transactions);

    String result = service.getBudgetsTextBlock(userId);

    assertTrue(result.contains("TestUser"));
    assertTrue(result.contains("Total Spent: $50.00"));
    assertTrue(result.contains("Remaining: $50.00"));
  }

  /**
   * Tests getBudgetsTextBlock when user has no transactions.
   *
   * <p>Partition: P2 (Valid) - User found, no transactions.
   */
  @Test
  public void getBudgetsTextBlock_noTransactions_showsZeroSpent() {
    user.setUsername("TestUser");
    user.setBudget(100.0);

    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)))
        .thenReturn(user);
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(userId)))
        .thenReturn(Collections.emptyList());

    String result = service.getBudgetsTextBlock(userId);

    assertTrue(result.contains("Total Spent: $0.00"));
  }

  /**
   * Tests getBudgetsTextBlock when user not found.
   *
   * <p>Partition: P3 (Invalid) - User not found.
   */
  @Test
  public void getBudgetsTextBlock_userNotFound_returnsUserNotFound() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)))
        .thenThrow(new RuntimeException());

    String result = service.getBudgetsTextBlock(userId);

    assertEquals("User not found", result);
  }

  // ===========================================================================
  // getBudgetWarningsText
  // ===========================================================================

  /**
   * Tests getBudgetWarningsText when user is near budget limit.
   *
   * <p>Partition: P1 (Valid) - Less than 10% remaining.
   */
  @Test
  public void getBudgetWarningsText_nearLimit_returnsWarning() {
    user.setBudget(100.0);
    List<Transaction> transactions = List.of(
        new Transaction(userId, 95.0, "FOOD", "desc"));

    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)))
        .thenReturn(user);
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(userId)))
        .thenReturn(transactions);

    String result = service.getBudgetWarningsText(userId);

    assertTrue(result.contains("Budget warning"));
    assertTrue(result.contains("less than 10%"));
  }

  /**
   * Tests getBudgetWarningsText when user is over budget.
   *
   * <p>Partition: P2 (Valid) - Over budget.
   */
  @Test
  public void getBudgetWarningsText_overBudget_returnsOverBudgetWarning() {
    user.setBudget(50.0);
    List<Transaction> transactions = List.of(
        new Transaction(userId, 75.0, "FOOD", "desc"));

    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)))
        .thenReturn(user);
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(userId)))
        .thenReturn(transactions);

    String result = service.getBudgetWarningsText(userId);

    assertTrue(result.contains("OVER BUDGET"));
  }

  /**
   * Tests getBudgetWarningsText when user has healthy budget.
   *
   * <p>Partition: P3 (Valid) - More than 10% remaining.
   */
  @Test
  public void getBudgetWarningsText_healthyBudget_returnsEmpty() {
    user.setBudget(100.0);
    List<Transaction> transactions = List.of(
        new Transaction(userId, 50.0, "FOOD", "desc"));

    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)))
        .thenReturn(user);
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(userId)))
        .thenReturn(transactions);

    String result = service.getBudgetWarningsText(userId);

    assertEquals("", result);
  }

  /**
   * Tests getBudgetWarningsText when user not found.
   *
   * <p>Partition: P4 (Invalid) - User not found.
   */
  @Test
  public void getBudgetWarningsText_userNotFound_returnsUserNotFound() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)))
        .thenThrow(new RuntimeException());

    String result = service.getBudgetWarningsText(userId);

    assertEquals("User not found", result);
  }

  // ===========================================================================
  // getMonthlySummary
  // ===========================================================================

  /**
   * Tests getMonthlySummary with current month transactions.
   *
   * <p>Partition: P1 (Valid) - User with current month transactions.
   */
  @Test
  public void getMonthlySummary_currentMonthTransactions_returnsSummary() {
    user.setBudget(500.0);
    LocalDate now = LocalDate.now();

    Transaction t1 = new Transaction(userId, 100.0, "FOOD", "d1");
    t1.setDate(now);
    Transaction t2 = new Transaction(userId, 50.0, "SHOPPING", "d2");
    t2.setDate(now);
    Transaction t3 = new Transaction(userId, 200.0, "TRAVEL", "d3");
    t3.setDate(now.minusMonths(1));

    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)))
        .thenReturn(user);
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(userId)))
        .thenReturn(List.of(t1, t2, t3));

    String result = service.getMonthlySummary(userId);

    assertTrue(result.contains("Total Spent: $150.00"));
    assertTrue(result.contains("Remaining: $350.00"));
    assertFalse(result.contains("TRAVEL"));
  }

  /**
   * Tests getMonthlySummary when transactions have null dates.
   *
   * <p>Partition: P2 (Valid/Edge) - Transactions with null dates.
   */
  @Test
  public void getMonthlySummary_nullDates_ignoredInSummary() {
    user.setBudget(500.0);

    Transaction t1 = new Transaction(userId, 100.0, "FOOD", "d1");
    Transaction t2 = new Transaction(userId, 50.0, "SHOPPING", "d2");

    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)))
        .thenReturn(user);
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(userId)))
        .thenReturn(List.of(t1, t2));

    String result = service.getMonthlySummary(userId);

    assertTrue(result.contains("Total Spent: $0.00"));
  }

  /**
   * Tests getMonthlySummary when user not found.
   *
   * <p>Partition: P3 (Invalid) - User not found.
   */
  @Test
  public void getMonthlySummary_userNotFound_returnsUserNotFound() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)))
        .thenThrow(new RuntimeException());

    String result = service.getMonthlySummary(userId);

    assertEquals("User not found", result);
  }

  // ===========================================================================
  // getBudgetReport
  // ===========================================================================

  /**
   * Tests getBudgetReport when user found.
   *
   * <p>Partition: P1 (Valid) - User found.
   */
  @Test
  public void getBudgetReport_userFound_returnsCompleteReport() {
    user.setUsername("TestUser");
    user.setEmail("test@example.com");
    user.setBudget(100.0);
    List<Transaction> transactions = List.of(
        new Transaction(userId, 30.0, "FOOD", "desc1"),
        new Transaction(userId, 20.0, "SHOPPING", "desc2"));

    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)))
        .thenReturn(user);
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(userId)))
        .thenReturn(transactions);

    Map<String, Object> result = service.getBudgetReport(userId);

    assertEquals(userId, result.get("userId"));
    assertEquals("TestUser", result.get("username"));
    assertEquals(100.0, result.get("totalBudget"));
    assertEquals(50.0, result.get("totalSpent"));
    assertEquals(50.0, result.get("remaining"));
    assertEquals(false, result.get("isOverBudget"));
  }

  /**
   * Tests getBudgetReport when user is over budget.
   *
   * <p>Partition: P2 (Valid) - User over budget.
   */
  @Test
  public void getBudgetReport_overBudget_showsWarnings() {
    user.setUsername("TestUser");
    user.setBudget(50.0);
    List<Transaction> transactions = List.of(
        new Transaction(userId, 75.0, "FOOD", "desc"));

    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)))
        .thenReturn(user);
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(userId)))
        .thenReturn(transactions);

    Map<String, Object> result = service.getBudgetReport(userId);

    assertEquals(true, result.get("isOverBudget"));
    assertEquals(true, result.get("hasWarnings"));
    assertTrue(((String) result.get("warnings")).contains("OVER BUDGET"));
  }

  /**
   * Tests getBudgetReport when user not found.
   *
   * <p>Partition: P3 (Invalid) - User not found.
   */
  @Test
  public void getBudgetReport_userNotFound_returnsErrorMap() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)))
        .thenThrow(new RuntimeException());

    Map<String, Object> result = service.getBudgetReport(userId);

    assertEquals(Map.of("error", "User not found"), result);
  }

  // ===========================================================================
  // setBudgets
  // ===========================================================================

  /**
   * Tests setBudgets with budget as Number.
   *
   * <p>Partition: P1 (Valid) - Budget as Number.
   */
  @Test
  public void setBudgets_budgetAsNumber_updatesSuccessfully() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)))
        .thenReturn(user);

    Map<String, Object> updates = Map.of("budget", 200.0);
    service.setBudgets(userId, updates);

    verify(jdbcTemplate).update("UPDATE users SET budget = ? WHERE user_id = ?", 200.0, userId);
  }

  /**
   * Tests setBudgets with budget as String.
   *
   * <p>Partition: P2 (Valid) - Budget as String.
   */
  @Test
  public void setBudgets_budgetAsString_parsesAndUpdates() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)))
        .thenReturn(user);

    Map<String, Object> updates = Map.of("budget", "150.50");
    service.setBudgets(userId, updates);

    verify(jdbcTemplate).update("UPDATE users SET budget = ? WHERE user_id = ?", 150.50, userId);
  }

  /**
   * Tests setBudgets with zero budget.
   *
   * <p>Partition: P3 (Valid/Boundary) - Budget is zero.
   */
  @Test
  public void setBudgets_zeroBudget_updatesSuccessfully() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)))
        .thenReturn(user);

    Map<String, Object> updates = Map.of("budget", 0.0);
    service.setBudgets(userId, updates);

    verify(jdbcTemplate).update("UPDATE users SET budget = ? WHERE user_id = ?", 0.0, userId);
  }

  /**
   * Tests setBudgets when user not found.
   *
   * <p>Partition: P4 (Invalid) - User not found.
   */
  @Test
  public void setBudgets_userNotFound_throwsIllegalArgumentException() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)))
        .thenThrow(new RuntimeException());

    Map<String, Object> updates = Map.of("budget", 100.0);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.setBudgets(userId, updates));

    assertEquals("User not found", exception.getMessage());
  }

  /**
   * Tests setBudgets with invalid budget format.
   *
   * <p>Partition: P5 (Invalid) - Invalid budget format.
   */
  @Test
  public void setBudgets_invalidFormat_throwsIllegalArgumentException() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)))
        .thenReturn(user);

    Map<String, Object> updates = Map.of("budget", true);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.setBudgets(userId, updates));

    assertEquals("Invalid budget format", exception.getMessage());
  }

  /**
   * Tests setBudgets with negative budget.
   *
   * <p>Partition: P6 (Invalid) - Negative budget.
   */
  @Test
  public void setBudgets_negativeBudget_throwsIllegalArgumentException() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)))
        .thenReturn(user);

    Map<String, Object> updates = Map.of("budget", -100.0);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.setBudgets(userId, updates));

    assertEquals("Budget cannot be negative", exception.getMessage());
  }

  /**
   * Tests setBudgets when no budget key in map.
   *
   * <p>Partition: P7 (Valid/Edge) - No budget key.
   */
  @Test
  public void setBudgets_noBudgetKey_noUpdatePerformed() {
    when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(),
        eq(userId)))
        .thenReturn(user);

    Map<String, Object> updates = Map.of("other", "value");
    service.setBudgets(userId, updates);

    verify(jdbcTemplate, never()).update(eq("UPDATE users SET budget = ? WHERE user_id = ?"),
        any(), any());
  }

  // ===========================================================================
  // weeklySummary
  // ===========================================================================

  /**
   * Tests weeklySummary with recent transactions.
   *
   * <p>Partition: P1 (Valid) - User has recent transactions.
   */
  @Test
  public void weeklySummary_hasRecentTransactions_returnsList() {
    LocalDate oneWeekAgo = LocalDate.now().minusDays(7);
    List<Transaction> expected = List.of(
        new Transaction(userId, 100.0, "FOOD", "d1"),
        new Transaction(userId, 50.0, "SHOPPING", "d2"));

    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(userId), eq(oneWeekAgo)))
        .thenReturn(expected);

    List<Transaction> result = service.weeklySummary(userId);

    assertEquals(expected, result);
  }

  /**
   * Tests weeklySummary with no transactions in week.
   *
   * <p>Partition: P2 (Valid/Boundary) - No transactions in week.
   */
  @Test
  public void weeklySummary_noTransactionsInWeek_returnsEmptyList() {
    LocalDate oneWeekAgo = LocalDate.now().minusDays(7);

    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        eq(userId), eq(oneWeekAgo)))
        .thenReturn(Collections.emptyList());

    List<Transaction> result = service.weeklySummary(userId);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  /**
   * Tests weeklySummary when database throws exception.
   *
   * <p>Partition: P3 (Invalid) - Database error.
   */
  @Test
  public void weeklySummary_databaseError_throwsException() {
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Transaction>>any(),
        any(), any()))
        .thenThrow(new IllegalArgumentException("DB Error"));

    assertThrows(IllegalArgumentException.class, () -> service.weeklySummary(userId));
  }

  // ===========================================================================
  // totalLast7Days
  // ===========================================================================

  /**
   * Tests totalLast7Days when transactions exist.
   *
   * <p>Partition: P1 (Valid) - Transactions exist.
   */
  @Test
  public void totalLast7Days_hasTransactions_returnsSum() {
    LocalDate oneWeekAgo = LocalDate.now().minusDays(7);

    when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), eq(userId), eq(oneWeekAgo)))
        .thenReturn(150.0);

    double result = service.totalLast7Days(userId);

    assertEquals(150.0, result);
  }

  /**
   * Tests totalLast7Days when no transactions exist.
   *
   * <p>Partition: P2 (Valid/Boundary) - No transactions.
   */
  @Test
  public void totalLast7Days_noTransactions_returnsZero() {
    LocalDate oneWeekAgo = LocalDate.now().minusDays(7);

    when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), eq(userId), eq(oneWeekAgo)))
        .thenReturn(0.0);

    double result = service.totalLast7Days(userId);

    assertEquals(0.0, result);
  }

  /**
   * Tests totalLast7Days when database returns null.
   *
   * <p>Partition: P3 (Valid/Edge) - Database returns null.
   */
  @Test
  public void totalLast7Days_databaseReturnsNull_returnsZero() {
    LocalDate oneWeekAgo = LocalDate.now().minusDays(7);

    when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), eq(userId), eq(oneWeekAgo)))
        .thenReturn(null);

    double result = service.totalLast7Days(userId);

    assertEquals(0.0, result);
  }

  /**
   * Tests totalLast7Days when database throws exception.
   *
   * <p>Partition: P4 (Invalid) - Database error.
   */
  @Test
  public void totalLast7Days_databaseError_returnsZero() {
    LocalDate oneWeekAgo = LocalDate.now().minusDays(7);

    when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), eq(userId), eq(oneWeekAgo)))
        .thenThrow(new RuntimeException("DB Error"));

    double result = service.totalLast7Days(userId);

    assertEquals(0.0, result);
  }

  // ===========================================================================
  // isUsernameExists
  // ===========================================================================

  /**
   * Tests isUsernameExists when username exists (no exclusion).
   *
   * <p>Partition: P1 (Valid) - Username exists.
   */
  @Test
  public void isUsernameExists_exists_returnsTrue() {
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("testuser")))
        .thenReturn(1);

    boolean result = service.isUsernameExists("testuser", null);

    assertTrue(result);
  }

  /**
   * Tests isUsernameExists when username does not exist.
   *
   * <p>Partition: P2 (Valid) - Username does not exist.
   */
  @Test
  public void isUsernameExists_notExists_returnsFalse() {
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("testuser")))
        .thenReturn(0);

    boolean result = service.isUsernameExists("testuser", null);

    assertFalse(result);
  }

  /**
   * Tests isUsernameExists when username exists but excluded.
   *
   * <p>Partition: P3 (Valid) - Username exists but excluded.
   */
  @Test
  public void isUsernameExists_existsButExcluded_returnsFalse() {
    UUID excludeId = UUID.randomUUID();
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("testuser"),
        eq(excludeId)))
        .thenReturn(0);

    boolean result = service.isUsernameExists("testuser", excludeId);

    assertFalse(result);
  }

  /**
   * Tests isUsernameExists when username exists for other user.
   *
   * <p>Partition: P4 (Valid) - Username exists for other user.
   */
  @Test
  public void isUsernameExists_existsForOtherUser_returnsTrue() {
    UUID excludeId = UUID.randomUUID();
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("testuser"),
        eq(excludeId)))
        .thenReturn(1);

    boolean result = service.isUsernameExists("testuser", excludeId);

    assertTrue(result);
  }

  /**
   * Tests isUsernameExists when count is null.
   *
   * <p>Partition: P5 (Valid/Edge) - Count is null.
   */
  @Test
  public void isUsernameExists_countNull_returnsFalse() {
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("testuser")))
        .thenReturn(null);

    boolean result = service.isUsernameExists("testuser", null);

    assertFalse(result);
  }

  // ===========================================================================
  // isEmailExists
  // ===========================================================================

  /**
   * Tests isEmailExists when email exists (no exclusion).
   *
   * <p>Partition: P1 (Valid) - Email exists.
   */
  @Test
  public void isEmailExists_exists_returnsTrue() {
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("test@example.com")))
        .thenReturn(1);

    boolean result = service.isEmailExists("test@example.com", null);

    assertTrue(result);
  }

  /**
   * Tests isEmailExists when email does not exist.
   *
   * <p>Partition: P2 (Valid) - Email does not exist.
   */
  @Test
  public void isEmailExists_notExists_returnsFalse() {
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("test@example.com")))
        .thenReturn(0);

    boolean result = service.isEmailExists("test@example.com", null);

    assertFalse(result);
  }

  /**
   * Tests isEmailExists when email exists but excluded.
   *
   * <p>Partition: P3 (Valid) - Email exists but excluded.
   */
  @Test
  public void isEmailExists_existsButExcluded_returnsFalse() {
    UUID excludeId = UUID.randomUUID();
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("test@example.com"),
        eq(excludeId)))
        .thenReturn(0);

    boolean result = service.isEmailExists("test@example.com", excludeId);

    assertFalse(result);
  }

  /**
   * Tests isEmailExists when email exists for other user.
   *
   * <p>Partition: P4 (Valid) - Email exists for other user.
   */
  @Test
  public void isEmailExists_existsForOtherUser_returnsTrue() {
    UUID excludeId = UUID.randomUUID();
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("test@example.com"),
        eq(excludeId)))
        .thenReturn(1);

    boolean result = service.isEmailExists("test@example.com", excludeId);

    assertTrue(result);
  }

  /**
   * Tests isEmailExists when count is null.
   *
   * <p>Partition: P5 (Valid/Edge) - Count is null.
   */
  @Test
  public void isEmailExists_countNull_returnsFalse() {
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("test@example.com")))
        .thenReturn(null);

    boolean result = service.isEmailExists("test@example.com", null);

    assertFalse(result);
  }
}