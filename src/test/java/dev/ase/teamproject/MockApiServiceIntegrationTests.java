package dev.ase.teamproject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.ase.teamproject.model.Transaction;
import dev.ase.teamproject.model.User;
import dev.ase.teamproject.service.MockApiService;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

@ActiveProfiles("test")
@SpringBootTest
// @TestPropertySource(locations = "classpath:application-test.properties")

public class MockApiServiceIntegrationTests {

  @Autowired
  private MockApiService service;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private UUID userId;

  @BeforeEach
  void setup() {
    // Clean tables
    jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");

    // Add a test user
    User user = new User();
    user.setUsername("user");
    user.setEmail("user@email.com");
    user.setBudget(100.0);

    // Use service to add user
    User saved = service.addUser(user);
    userId = saved.getUserId();
  }

  // ---------------------
  // USER TESTS
  // ---------------------

  @Test
  void testViewAllUsers() {
    List<User> users = service.viewAllUsers();
    assertThat(users).hasSize(1);
    assertThat(users.get(0).getUsername()).isEqualTo("user");
  }

  @Test
  void testGetUser() {
    Optional<User> user = service.getUser(userId);
    assertThat(user).isPresent();
    assertThat(user.get().getUsername()).isEqualTo("user");
  }

  @Test
  void testAddUser_success() {
    User user = new User();
    user.setUsername("newUser");
    user.setEmail("test@email.com");
    user.setBudget(50.0);

    User saved = service.addUser(user);

    assertThat(saved.getUserId()).isNotNull();
    assertThat(service.getUser(saved.getUserId())).isPresent();
  }

  @Test
  void testAddUser_fail() {
    User duplicate = new User();
    duplicate.setUsername("user");
    duplicate.setEmail("test@email.com");
    duplicate.setBudget(10.0);

    assertThatThrownBy(() -> service.addUser(duplicate))
        .isInstanceOf(Exception.class);
  }

  @Test
  void testDeleteUser() {
    boolean deleted = service.deleteUser(userId);
    assertThat(deleted).isTrue();
    assertThat(service.getUser(userId)).isEmpty();
  }

  // ---------------------
  // TRANSACTION TESTS
  // ---------------------

  @Test
  void testAddTransaction_success() {
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

  @Test
  void testAddTransaction_negativeAmountInvalid() {
    Transaction t = new Transaction();
    t.setUserId(userId);
    t.setDescription("desc");
    t.setAmount(-5.0);
    t.setCategory("OTHER");

    assertThatThrownBy(() -> service.addTransaction(t))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Amount must be greater than 0");
  }

  @Test
  void testAddTransaction_categoryInvalid() {
    Transaction t = new Transaction();
    t.setUserId(userId);
    t.setDescription("desc");
    t.setAmount(5.0);
    t.setCategory("INVALID");

    assertThatThrownBy(() -> service.addTransaction(t))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid category");
  }

  @Test
  void testUpdateTransaction_success() {
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

  @Test
  void testDeleteTransaction_success() {
    Transaction t = new Transaction(userId, 10.0, "OTHER", "desc");
    Transaction saved = service.addTransaction(t);

    boolean deleted = service.deleteTransaction(saved.getTransactionId());
    assertThat(deleted).isTrue();
  }

  // ---------------------
  // ANALYTICS / REPORTS
  // ---------------------

  @Test
  void testGetBudgetsTextBlock_userFound() {
    addSampleTransactions();
    String summary = service.getBudgetsTextBlock(userId);
    assertThat(summary).contains("Total Budget", "Total Spent", "Remaining");
  }

  @Test
  void testGetBudgetsTextBlock_userNotFound() {
    UUID fakeId = UUID.randomUUID();
    String summary = service.getBudgetsTextBlock(fakeId);
    assertThat(summary).contains("User not found");
  }

  @Test
  void testSetBudgets_success() {
    service.setBudgets(userId, Map.of("budget", 200.0));
    Optional<User> updated = service.getUser(userId);
    assertThat(updated).isPresent();
    assertThat(updated.get().getBudget()).isEqualTo(200.0);
  }

  @Test
  void testSetBudgets_invalidBudget() {
    assertThatThrownBy(() -> service.setBudgets(userId, Map.of("budget", -10)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Budget cannot be negative");
  }

  private void addSampleTransactions() {
    Transaction t1 = new Transaction(userId, 10.0, "OTHER", "desc1");
    Transaction t2 = new Transaction(userId, 1.0, "FOOD", "desc2");
    Transaction t3 = new Transaction(userId, 15.0, "OTHER", "desc3");
    service.addTransaction(t1);
    service.addTransaction(t2);
    service.addTransaction(t3);
  }
}
