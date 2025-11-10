package dev.ase.teamproject.service;

import dev.ase.teamproject.model.Transaction;
import dev.ase.teamproject.model.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

/**
 * This class defines the Mock API Service responsible for managing
 * interactions between the application and the PostgreSQL database.
 * <p>
 *  It provides CRUD operations for users and transactions, as well as
 *  analytics and budgeting functionalities.
 *  All methods in this class use the Spring JdbcTemplate for data access.
 * </p>
 */
@Service
public class MockApiService {
  private final JdbcTemplate jdbcTemplate;

  /**
   * Constructs a new {@code MockApiService} with the specified {@code JdbcTemplate}.
   *
   * @param jdbcTemplate A {@code JdbcTemplate} used to communicate with the database.
   */
  public MockApiService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Retrieves all users from the database.
   *
   * @return A {@code List} of all {@code User} records.
   */
  public List<User> viewAllUsers() {
    String sql = "SELECT * FROM users";
    return jdbcTemplate.query(sql, userRowMapper);
  }

  /**
   * Retrieves a specific user by their unique identifier.
   *
   * @param userId The {@code UUID} of the user to retrieve.
   * @return An {@code Optional} containing the {@code User} if found, or empty if not found.
   */
  public Optional<User> getUser(UUID userId) {
    String sql = "SELECT * FROM users WHERE user_id = ?";
    try {
      User user = jdbcTemplate.queryForObject(sql, userRowMapper, userId);
      return Optional.ofNullable(user);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * Adds a new user to the database.
   *
   * @param user The {@code User} object containing username, email, and budget values.
   * @return The created {@code User} object with a generated {@code UUID}.
   */
  public User addUser(User user) {
    if (user.getUserId() != null) {
      String sql = "INSERT INTO users (user_id, username, email, budget) VALUES (?, ?, ?, ?)";
      jdbcTemplate.update(sql, 
          user.getUserId(),  // Use the provided UUID
          user.getUsername(), 
          user.getEmail(), 
          user.getBudget());
      return user;  // Return as-is
    } else {
      String sql = "INSERT INTO users (username, email, budget) VALUES (?, ?, ?) RETURNING user_id";
      UUID generatedUserId = jdbcTemplate.queryForObject(sql, UUID.class, 
          user.getUsername(), user.getEmail(), user.getBudget());
      user.setUserId(generatedUserId);
      return user;
    }
  }

  /**
   * Deletes a user by their unique identifier.
   *
   * @param userId The {@code UUID} of the user to delete.
   * @return {@code true} if a record was deleted; {@code false} otherwise.
   */
  public boolean deleteUser(UUID userId) {
    String sql = "DELETE FROM users WHERE user_id = ?";
    int rowsAffected = jdbcTemplate.update(sql, userId);
    return rowsAffected > 0;
  }

  /**
   * Retrieves all transactions sorted by most recent creation time.
   *
   * @return A {@code List} of {@code Transaction} objects sorted by {@code created_time}.
   */
  public List<Transaction> viewAllTransactions() {
    String sql = "SELECT * FROM transactions ORDER BY created_time DESC";
    return jdbcTemplate.query(sql, transactionRowMapper);
  }

  /**
   * Retrieves a specific transaction by its unique identifier.
   *
   * @param transactionId The {@code UUID} of the transaction to retrieve.
   * @return An {@code Optional} containing the {@code Transaction} if found, or empty if not found.
   */
  public Optional<Transaction> getTransaction(UUID transactionId) {
    String sql = "SELECT * FROM transactions WHERE transaction_id = ?";
    try {
      Transaction transaction = jdbcTemplate
          .queryForObject(sql, transactionRowMapper, transactionId);
      return Optional.ofNullable(transaction);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * Creates a new transaction record in the database.
   *
   * @param transaction The {@code Transaction} to insert.
   * @return The created {@code Transaction}.
   * @throws RuntimeException if the insert operation fails.
   */
  public Transaction addTransaction(Transaction transaction) {
    try {
      String sql = "INSERT INTO transactions (user_id, description, amount, category) " 
          + "VALUES (?, ?, ?, ?::transaction_category) "
          + "RETURNING transaction_id, created_time, created_date";
      Transaction savedTransaction = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
        transaction.setTransactionId(rs.getObject("transaction_id", UUID.class));
        transaction.setTimestamp(rs.getTimestamp("created_time").toLocalDateTime());
        transaction.setDate(rs.getDate("created_date").toLocalDate());
        return transaction; 
      }, 
          transaction.getUserId(),
          transaction.getDescription(),
          transaction.getAmount(),
          transaction.getCategory());
      return savedTransaction != null ? savedTransaction : transaction;
    } catch (Exception e) {
      throw new RuntimeException("Failed to create transaction: " + e.getMessage(), e);
    }
  }

  /**
   * Retrieves all transactions associated with a specific user.
   *
   * @param userId The {@code UUID} of the user.
   * @return A {@code List} of the user's {@code Transaction} records.
   * @throws RuntimeException if the query fails.
   */
  public List<Transaction> getTransactionsByUser(UUID userId) {
    try {
      String sql = "SELECT * FROM transactions WHERE user_id = ? ORDER BY created_time DESC";
      return jdbcTemplate.query(sql, transactionRowMapper, userId);
    } catch (Exception e) {
      throw new RuntimeException("Failed to get transactions: " + e.getMessage(), e);
    }
  }

  /**
   * Updates an existing transaction record with new values.
   *
   * @param transactionId The {@code UUID} of the transaction to update.
   * @param updates A {@code Map} containing the fields to modify and their new values.
   * @return An {@code Optional} of the updated {@code Transaction}, or empty if the update failed.
   */
  public Optional<Transaction> updateTransaction(UUID transactionId, Map<String, Object> updates) {
    Optional<Transaction> existing = getTransaction(transactionId);
    if (!existing.isPresent()) {
      return Optional.empty();
    }
    Transaction transaction = existing.get();
    if (updates.containsKey("description")) {
      transaction.setDescription((String) updates.get("description"));
    }
    if (updates.containsKey("amount")) {
      Object amount = updates.get("amount");
      if (amount instanceof Number) {
        transaction.setAmount(((Number) amount).doubleValue());
      } else if (amount instanceof String) {
        transaction.setAmount(Double.parseDouble((String) amount));
      }
    }
    if (updates.containsKey("category")) {
      transaction.setCategory((String) updates.get("category"));
    }
    String sql = "UPDATE transactions SET description = ?, amount = ?, " 
        + "category = ?::transaction_category WHERE transaction_id = ?";
    int rowsAffected = jdbcTemplate.update(sql,
        transaction.getDescription(),
        transaction.getAmount(),
        transaction.getCategory(),
        transactionId);
    if (rowsAffected > 0) {
      return getTransaction(transactionId);
    }
    return Optional.empty();
  }

  /**
   * Deletes a transaction record by its unique identifier.
   *
   * @param transactionId The {@code UUID} of the transaction to delete.
   * @return {@code true} if a record was deleted; {@code false} otherwise.
   */
  public boolean deleteTransaction(UUID transactionId) {
    String sql = "DELETE FROM transactions WHERE transaction_id = ?";
    int rowsAffected = jdbcTemplate.update(sql, transactionId);
    return rowsAffected > 0;
  }

  /**
   * Generates a summary of a user's budget information.
   *
   * @param userId The {@code UUID} of the user.
   * @return A formatted summary string with budget details and remaining balance.
   */
  public String getBudgetsTextBlock(UUID userId) {
    Optional<User> userOpt = getUser(userId);
    if (!userOpt.isPresent()) {
      return "User not found";
    }
    User user = userOpt.get();
    List<Transaction> transactions = getTransactionsByUser(userId);
    double totalSpent = transactions.stream()
        .filter(t -> t.getAmount() > 0)
        .mapToDouble(Transaction::getAmount)
        .sum();
    double remaining = user.getBudget() - totalSpent;
    return String.format(
        "Budget Summary for %s:\n"
        + "Total Budget: $%.2f\n"
        + "Total Spent: $%.2f\n"
        + "Remaining: $%.2f",
        user.getUsername(), user.getBudget(), totalSpent, remaining
    );
  }

  /**
   * Generates warning messages for users who are near or over their budget.
   *
   * @param userId The {@code UUID} of the user.
   * @return A formatted string with warning messages, or an empty string if none apply.
   */
  public String getBudgetWarningsText(UUID userId) {
    Optional<User> userOpt = getUser(userId);
    if (!userOpt.isPresent()) {
      return "User not found";
    }
    User user = userOpt.get();
    List<Transaction> transactions = getTransactionsByUser(userId);
    double totalSpent = transactions.stream()
        .filter(t -> t.getAmount() > 0)
        .mapToDouble(Transaction::getAmount)
        .sum();
    double remaining = user.getBudget() - totalSpent;
    StringBuilder warnings = new StringBuilder();
    if (remaining < 0) {
      warnings.append("OVER BUDGET! You have exceeded your budget by $")
          .append(String.format("%.2f", -remaining)).append("\n");
    } else if (remaining < user.getBudget() * 0.1) {
      warnings.append("Budget warning: Only $").append(String.format("%.2f", remaining))
          .append(" remaining (less than 10%)\n");
    }
    return warnings.toString();
  }

  /**
   * Generates a monthly spending summary with category breakdown.
   *
   * @param userId The {@code UUID} of the user.
   * @return A formatted multi-line string summarizing the user's monthly spending.
   */
  public String getMonthlySummary(UUID userId) {
    Optional<User> userOpt = getUser(userId);
    if (!userOpt.isPresent()) {
      return "User not found";
    }
    User user = userOpt.get();
    List<Transaction> transactions = getTransactionsByUser(userId);
    LocalDate now = LocalDate.now();
    List<Transaction> monthlyTransactions = transactions.stream()
        .filter(t -> t.getDate() != null && t.getDate().getMonth() 
            == now.getMonth() && t.getDate().getYear() == now.getYear())
        .collect(Collectors.toList());
    double totalSpent = monthlyTransactions.stream()
        .filter(t -> t.getAmount() > 0)
        .mapToDouble(Transaction::getAmount)
        .sum();
    double remaining = user.getBudget() - totalSpent;
    StringBuilder summary = new StringBuilder();
    summary.append(String.format("Monthly Summary for %s (%s %d)\n\n", 
        user.getUsername(), now.getMonth(), now.getYear()));
    summary.append(String.format("Total Budget: $%.2f\n", user.getBudget()));
    summary.append(String.format("Total Spent: $%.2f\n", totalSpent));
    summary.append(String.format("Remaining: $%.2f\n\n", remaining));
    summary.append("Spending by Category:\n");
    Map<String, Double> byCategory = monthlyTransactions.stream()
        .filter(t -> t.getAmount() > 0)
        .collect(Collectors.groupingBy(
            Transaction::getCategory,
            Collectors.summingDouble(Transaction::getAmount)
        ));
    byCategory.entrySet().stream()
        .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
        .forEach(entry -> {
          summary.append(String.format("- %s: $%.2f\n", entry.getKey(), entry.getValue()));
        });
    return summary.toString();
  }

  /**
   * Generates a budget report containing analytics, totals, and warnings.
   *
   * @param userId The {@code UUID} of the user.
   * @return A {@code Map} with budget metrics and summaries.
   */
  public Map<String, Object> getBudgetReport(UUID userId) {
    Optional<User> userOpt = getUser(userId);
    if (!userOpt.isPresent()) {
      return Map.of("error", "User not found");
    }
    User user = userOpt.get();
    List<Transaction> transactions = getTransactionsByUser(userId);
    double totalSpent = transactions.stream()
        .filter(t -> t.getAmount() > 0)
        .mapToDouble(Transaction::getAmount)
        .sum();
    double remaining = user.getBudget() - totalSpent;
    String warningsText = getBudgetWarningsText(userId);
    Map<String, Double> byCategory = transactions.stream()
        .filter(t -> t.getAmount() > 0)
        .collect(Collectors.groupingBy(
            Transaction::getCategory,
            Collectors.summingDouble(Transaction::getAmount)
        ));
    return Map.of(
        "userId", userId,
        "username", user.getUsername(),
        "totalBudget", user.getBudget(),
        "totalSpent", totalSpent,
        "remaining", remaining,
        "categories", byCategory,
        "isOverBudget", remaining < 0,
        "warnings", warningsText,
        "hasWarnings", !warningsText.isEmpty()
    );
  }

  /**
   * Updates the user's budget with new settings or amounts.
   *
   * @param userId The {@code UUID} of the user.
   * @param updates A {@code Map} containing updated budget values.
   * @throws IllegalArgumentException if the user is not found or the budget is invalid.
   */
  public void setBudgets(UUID userId, Map<String, Object> updates) {
    Optional<User> userOpt = getUser(userId);
    if (!userOpt.isPresent()) {
      throw new IllegalArgumentException("User not found");
    }
    if (updates.containsKey("budget")) {
      Object budget = updates.get("budget");
      double newBudget;
      if (budget instanceof Number) {
        newBudget = ((Number) budget).doubleValue();
      } else if (budget instanceof String) {
        newBudget = Double.parseDouble((String) budget);
      } else {
        throw new IllegalArgumentException("Invalid budget format");
      }
      if (newBudget < 0) {
        throw new IllegalArgumentException("Budget cannot be negative");
      }
      String sql = "UPDATE users SET budget = ? WHERE user_id = ?";
      jdbcTemplate.update(sql, newBudget, userId);
    }
  }

  /**
   * Retrieves transactions created within the last seven days for a given user.
   *
   * @param userId The {@code UUID} of the user.
   * @return Aa {@code List} of {@code Transaction} objects created within the past week.
   */
  public List<Transaction> weeklySummary(UUID userId) {
    LocalDate oneWeekAgo = LocalDate.now().minusDays(7);
    String sql = "SELECT * FROM transactions WHERE user_id = ? AND"
        + " created_date >= ? ORDER BY created_time DESC";
    return jdbcTemplate.query(sql, transactionRowMapper, userId, oneWeekAgo);
  }

  /**
   * Calculates the total spending for a user over the past seven days.
   *
   * @param userId The {@code UUID} of the user.
   * @return The total spending amount for the last 7 days.
   */
  public double totalLast7Days(UUID userId) {
    LocalDate oneWeekAgo = LocalDate.now().minusDays(7);
    String sql = "SELECT COALESCE(SUM(amount), 0) FROM transactions" 
        + "WHERE user_id = ? AND created_date >= ? AND amount > 0";
    try {
      Double result = jdbcTemplate.queryForObject(sql, Double.class, userId, oneWeekAgo);
      return result != null ? result : 0.0;
    } catch (Exception e) {
      return 0.0;
    }
  }

  /** Maps SQL query results to {@code User} objects. */
  private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
    User user = new User();
    user.setUserId((UUID) rs.getObject("user_id"));
    user.setUsername(rs.getString("username"));
    user.setEmail(rs.getString("email"));
    user.setBudget(rs.getDouble("budget"));
    return user;
  };

  /** Maps SQL query results to {@code Transaction} objects. */
  private final RowMapper<Transaction> transactionRowMapper = (rs, rowNum) -> {
    Transaction transaction = new Transaction();
    try {
      transaction.setTransactionId((UUID) rs.getObject("transaction_id"));
      transaction.setUserId((UUID) rs.getObject("user_id"));
      transaction.setDescription(rs.getString("description"));
      transaction.setAmount(rs.getDouble("amount"));
      transaction.setCategory(rs.getString("category"));
      
      // Handle potential null values for timestamps
      if (rs.getTimestamp("created_time") != null) {
        transaction.setTimestamp(rs.getTimestamp("created_time").toLocalDateTime());
      }
      
      if (rs.getDate("created_date") != null) {
        transaction.setDate(rs.getDate("created_date").toLocalDate());
      }
      
      return transaction;
    } catch (Exception e) {
      throw new RuntimeException("Error mapping transaction row: " + e.getMessage(), e);
    } 
  };
}