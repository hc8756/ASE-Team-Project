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
 *  It provides CRUD operations for users and transactions, as well as
 *  analytics and budgeting functionalities.
 *  All methods in this class use the Spring JdbcTemplate for data access.
 */
@SuppressWarnings({
    "PMD.TooManyMethods", // Service class with multiple operations
    "PMD.OnlyOneReturn", // Multiple return statements for clarity
    "PMD.AvoidCatchingGenericException" // Generic exception handling for DB ops
})
@Service
public class MockApiService {

  /** The JdbcTemplate used for database interactions. */
  private final JdbcTemplate jdbcTemplate;

  /** Maps SQL query results to {@code User} objects. */
  private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
    final User user = new User();
    user.setUserId((UUID) rs.getObject("user_id"));
    user.setUsername(rs.getString("username"));
    user.setEmail(rs.getString("email"));
    user.setBudget(rs.getDouble("budget"));
    return user;
  };

  /** Maps SQL query results to {@code Transaction} objects. */
  private final RowMapper<Transaction> txRowMapper = (rs, rowNum) -> {
    final Transaction transaction = new Transaction();
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
      throw new IllegalStateException("Error mapping transaction row: " + e.getMessage(), e);
    } 
  };

  /** Common string literal. */
  private static final String USER_NOT_FOUND = "User not found";

  /**
   * Constructs a new {@code MockApiService} with the specified {@code JdbcTemplate}.
   *
   * @param jdbcTemplate A {@code JdbcTemplate} used to communicate with database.
   */
  public MockApiService(final JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Retrieves all users from the database.
   *
   * @return A {@code List} of all {@code User} records.
   */
  public List<User> viewAllUsers() {
    final String sql = "SELECT * FROM users";
    return jdbcTemplate.query(sql, userRowMapper);
  }

  /**
   * Retrieves a specific user by their unique identifier.
   *
   * @param userId The {@code UUID} of the user to retrieve.
   * @return An {@code Optional} containing the {@code User} if found, 
   *         or empty if not found.
   */
  public Optional<User> getUser(final UUID userId) {
    final String sql = "SELECT * FROM users WHERE user_id = ?";
    try {
      final User user = jdbcTemplate.queryForObject(sql, userRowMapper, userId);
      return Optional.ofNullable(user);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * Adds a new user to the database.
   *
   * @param user The {@code User} object containing username, email, and budget.
   * @return The created {@code User} object with a generated {@code UUID}.
   */
  public User addUser(final User user) {
    if (user.getUserId() != null) {
      final String sql = "INSERT INTO users (user_id, username, email, budget) VALUES (?, ?, ?, ?)";
      jdbcTemplate.update(sql, 
          user.getUserId(),  // Use the provided UUID
          user.getUsername(), 
          user.getEmail(), 
          user.getBudget());
      return user;  // Return as-is
    } else {
      final String sql = 
          "INSERT INTO users (username, email, budget) VALUES (?, ?, ?) RETURNING user_id";
      final UUID generatedUserId = jdbcTemplate.queryForObject(sql, UUID.class, 
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
  public boolean deleteUser(final UUID userId) {
    final String sql = "DELETE FROM users WHERE user_id = ?";
    final int rowsAffected = jdbcTemplate.update(sql, userId);
    return rowsAffected > 0;
  }

  /**
   * Retrieves all transactions sorted by most recent creation time.
   *
   * @return A {@code List} of {@code Transaction} objects 
   *         sorted by {@code created_time}.
   */
  public List<Transaction> viewAllTransactions() {
    final String sql = "SELECT * FROM transactions ORDER BY created_time DESC";
    return jdbcTemplate.query(sql, txRowMapper);
  }

  /**
   * Retrieves a specific transaction by its unique identifier.
   *
   * @param transactionId The {@code UUID} of the transaction to retrieve.
   * @return An {@code Optional} containing the {@code Transaction} if found,
   *         or empty if not.
   */
  public Optional<Transaction> getTransaction(final UUID transactionId) {
    final String sql = "SELECT * FROM transactions WHERE transaction_id = ?";
    try {
      final Transaction transaction = jdbcTemplate
          .queryForObject(sql, txRowMapper, transactionId);
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
   * @throws IllegalStateException if the insert operation fails.
   */
  public Transaction addTransaction(final Transaction transaction) {
    try {
      final String sql = "INSERT INTO transactions (user_id, description, amount, category) " 
          + "VALUES (?, ?, ?, ?::transaction_category) "
          + "RETURNING transaction_id, created_time, created_date";
      final Transaction savedTransaction = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
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
      throw new IllegalStateException("Failed to create transaction: " + e.getMessage(), e);
    }
  }

  /**
   * Retrieves all transactions associated with a specific user.
   *
   * @param userId The {@code UUID} of the user.
   * @return A {@code List} of the user's {@code Transaction} records.
   * @throws IllegalStateException if the query fails.
   */
  public List<Transaction> getTransactionsByUser(final UUID userId) {
    try {
      final String sql = "SELECT * FROM transactions WHERE user_id = ? ORDER BY created_time DESC";
      return jdbcTemplate.query(sql, txRowMapper, userId);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to get transactions: " + e.getMessage(), e);
    }
  }

  /**
   * Updates an existing transaction record with new values.
   *
   * @param transactionId The {@code UUID} of the transaction to update.
   * @param updates A {@code Map} containing the fields to modify and new values.
   * @return An {@code Optional} of the updated {@code Transaction}, 
   *         or empty if update fails.
   */
  public Optional<Transaction> updateTransaction(
        final UUID transactionId, final Map<String, Object> updates) {
    final Optional<Transaction> existing = getTransaction(transactionId);
    if (!existing.isPresent()) {
      return Optional.empty();
    }
    final Transaction transaction = existing.get();
    if (updates.containsKey("description")) {
      transaction.setDescription((String) updates.get("description"));
    }
    if (updates.containsKey("amount")) {
      final Object amount = updates.get("amount");
      if (amount instanceof Number) {
        transaction.setAmount(((Number) amount).doubleValue());
      } else if (amount instanceof String) {
        transaction.setAmount(Double.parseDouble((String) amount));
      }
    }
    if (updates.containsKey("category")) {
      transaction.setCategory((String) updates.get("category"));
    }
    final String sql = "UPDATE transactions SET description = ?, amount = ?, " 
        + "category = ?::transaction_category WHERE transaction_id = ?";
    final int rowsAffected = jdbcTemplate.update(sql,
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
  public boolean deleteTransaction(final UUID transactionId) {
    final String sql = "DELETE FROM transactions WHERE transaction_id = ?";
    final int rowsAffected = jdbcTemplate.update(sql, transactionId);
    return rowsAffected > 0;
  }

  /**
   * Generates a summary of a user's budget information.
   *
   * @param userId The {@code UUID} of the user.
   * @return A formatted summary string with budget details and remaining balance.
   */
  public String getBudgetsTextBlock(final UUID userId) {
    final Optional<User> userOpt = getUser(userId);
    if (!userOpt.isPresent()) {
      return USER_NOT_FOUND;
    }
    final User user = userOpt.get();
    final List<Transaction> transactions = getTransactionsByUser(userId);
    final double totalSpent = transactions.stream()
        .filter(t -> t.getAmount() > 0)
        .mapToDouble(Transaction::getAmount)
        .sum();
    final double remaining = user.getBudget() - totalSpent;
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
   * @return A formatted string with warning messages, 
   *         or an empty string if none apply.
   */
  public String getBudgetWarningsText(final UUID userId) {
    final Optional<User> userOpt = getUser(userId);
    if (!userOpt.isPresent()) {
      return USER_NOT_FOUND;
    }
    final User user = userOpt.get();
    final List<Transaction> transactions = getTransactionsByUser(userId);
    final double totalSpent = transactions.stream()
        .filter(t -> t.getAmount() > 0)
        .mapToDouble(Transaction::getAmount)
        .sum();
    final double remaining = user.getBudget() - totalSpent;
    final StringBuilder warnings = new StringBuilder(128);
    if (remaining < 0) {
      warnings.append("OVER BUDGET! You have exceeded your budget by $")
          .append(String.format("%.2f", -remaining)).append('\n');
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
  public String getMonthlySummary(final UUID userId) {
    final Optional<User> userOpt = getUser(userId);
    if (!userOpt.isPresent()) {
      return USER_NOT_FOUND;
    }
    final User user = userOpt.get();
    final List<Transaction> transactions = getTransactionsByUser(userId);
    final LocalDate now = LocalDate.now();
    final List<Transaction> monthTransactions = transactions.stream()
        .filter(t -> t.getDate() != null && t.getDate().getMonth() 
            == now.getMonth() && t.getDate().getYear() == now.getYear())
        .collect(Collectors.toList());
    final double totalSpent = monthTransactions.stream()
        .filter(t -> t.getAmount() > 0)
        .mapToDouble(Transaction::getAmount)
        .sum();
    final double remaining = user.getBudget() - totalSpent;
    final StringBuilder summary = new StringBuilder(128);
    summary.append(String.format("Monthly Summary for %s (%s %d)%n%n",
          user.getUsername(), now.getMonth(), now.getYear()))
        .append(String.format("Total Budget: $%.2f%n", user.getBudget()))
        .append(String.format("Total Spent: $%.2f%n", totalSpent))
        .append(String.format("Remaining: $%.2f%n%n", remaining))
        .append("Spending by Category:\n");
    final Map<String, Double> byCategory = monthTransactions.stream()
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
  public Map<String, Object> getBudgetReport(final UUID userId) {
    final Optional<User> userOpt = getUser(userId);
    if (!userOpt.isPresent()) {
      return Map.of("error", USER_NOT_FOUND);
    }
    final User user = userOpt.get();
    final List<Transaction> transactions = getTransactionsByUser(userId);
    final double totalSpent = transactions.stream()
        .filter(t -> t.getAmount() > 0)
        .mapToDouble(Transaction::getAmount)
        .sum();
    final double remaining = user.getBudget() - totalSpent;
    final String warningsText = getBudgetWarningsText(userId);
    final Map<String, Double> byCategory = transactions.stream()
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
   * @throws IllegalArgumentException if the user is not found or budget is invalid.
   */
  public void setBudgets(final UUID userId, final Map<String, Object> updates) {
    final Optional<User> userOpt = getUser(userId);
    if (!userOpt.isPresent()) {
      throw new IllegalArgumentException(USER_NOT_FOUND);
    }
    if (updates.containsKey("budget")) {
      final Object budget = updates.get("budget");
      final double newBudget;
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
      final String sql = "UPDATE users SET budget = ? WHERE user_id = ?";
      jdbcTemplate.update(sql, newBudget, userId);
    }
  }

  /**
   * Retrieves transactions created within the last seven days for a given user.
   *
   * @param userId The {@code UUID} of the user.
   * @return A {@code List} of {@code Transaction} objects created in the past week.
   */
  public List<Transaction> weeklySummary(final UUID userId) {
    final LocalDate oneWeekAgo = LocalDate.now().minusDays(7);
    final String sql = "SELECT * FROM transactions WHERE user_id = ? AND"
        + " created_date >= ? ORDER BY created_time DESC";
    return jdbcTemplate.query(sql, txRowMapper, userId, oneWeekAgo);
  }

  /**
   * Calculates the total spending for a user over the past seven days.
   *
   * @param userId The {@code UUID} of the user.
   * @return The total spending amount for the last 7 days.
   */
  public double totalLast7Days(final UUID userId) {
    final LocalDate oneWeekAgo = LocalDate.now().minusDays(7);
    final String sql = "SELECT COALESCE(SUM(amount), 0) FROM transactions" 
        + " WHERE user_id = ? AND created_date >= ? AND amount > 0";
    try {
      final Double result = jdbcTemplate.queryForObject(sql, Double.class, userId, oneWeekAgo);
      return result != null ? result : 0.0;
    } catch (Exception e) {
      return 0.0;
    }
  }
}