package service;

import model.Transaction;
import model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MockApiService {
    private final JdbcTemplate jdbcTemplate;

    public MockApiService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ========== USER CRUD OPERATIONS ==========

    public List<User> viewAllUsers() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    public Optional<User> getUser(UUID userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, userId);
            return Optional.ofNullable(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public User addUser(User user) {
         String sql = "INSERT INTO users (username, email, budget) VALUES (?, ?, ?) RETURNING user_id";
    
        // Use queryForObject to get the generated user_id
        UUID generatedUserId = jdbcTemplate.queryForObject(sql, UUID.class, 
            user.getUsername(), user.getEmail(), user.getBudget());
        
        // Set the generated ID on the user object
        user.setUserId(generatedUserId);
        return user;
    }

    public boolean deleteUser(UUID userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, userId);
        return rowsAffected > 0;
    }

    // ========== TRANSACTION CRUD OPERATIONS ==========

    public List<Transaction> viewAllTransactions() {
        String sql = "SELECT * FROM transactions ORDER BY created_time DESC";
        return jdbcTemplate.query(sql, transactionRowMapper);
    }

    public Optional<Transaction> getTransaction(UUID transactionId) {
        String sql = "SELECT * FROM transactions WHERE transaction_id = ?";
        try {
            Transaction transaction = jdbcTemplate.queryForObject(sql, transactionRowMapper, transactionId);
            return Optional.ofNullable(transaction);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    
    public Transaction addTransaction(Transaction transaction) {
        try {
            // Option A: Use CAST in SQL
            String sql = "INSERT INTO transactions (user_id, description, amount, category) VALUES (?, ?, ?, ?::transaction_category)";
            
            jdbcTemplate.update(sql, 
                transaction.getUserId(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getCategory());
            
            return transaction;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create transaction: " + e.getMessage(), e);
        }
    }

    public List<Transaction> getTransactionsByUser(UUID userId) {
        try {
            String sql = "SELECT * FROM transactions WHERE user_id = ? ORDER BY created_time DESC";
            return jdbcTemplate.query(sql, transactionRowMapper, userId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get transactions: " + e.getMessage(), e);
        }
    }

    public Optional<Transaction> updateTransaction(UUID transactionId, Map<String, Object> updates) {
        // First get the existing transaction
        Optional<Transaction> existing = getTransaction(transactionId);
        if (!existing.isPresent()) {
            return Optional.empty();
        }

        Transaction transaction = existing.get();
        
        // Apply updates
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

        // Update in database
        String sql = "UPDATE transactions SET description = ?, amount = ?, category = ? WHERE transaction_id = ?";
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

    public boolean deleteTransaction(UUID transactionId) {
        String sql = "DELETE FROM transactions WHERE transaction_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, transactionId);
        return rowsAffected > 0;
    }

    // ========== BUDGET & ANALYTICS OPERATIONS (User-specific) ==========

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
            "Budget Summary for %s:\n" +
            "Total Budget: $%.2f\n" +
            "Total Spent: $%.2f\n" +
            "Remaining: $%.2f",
            user.getUsername(), user.getBudget(), totalSpent, remaining
        );
    }

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
            warnings.append("⚠️ OVER BUDGET! You have exceeded your budget by $").append(String.format("%.2f", -remaining)).append("\n");
        } else if (remaining < user.getBudget() * 0.1) {
            warnings.append("⚠️ Budget warning: Only $").append(String.format("%.2f", remaining)).append(" remaining (less than 10%)\n");
        }
        
        return warnings.toString();
    }

    public String getMonthlySummary(UUID userId) {
        Optional<User> userOpt = getUser(userId);
        if (!userOpt.isPresent()) {
            return "User not found";
        }
        
        User user = userOpt.get();
        List<Transaction> transactions = getTransactionsByUser(userId);
        
        // Filter current month transactions
        LocalDate now = LocalDate.now();
        List<Transaction> monthlyTransactions = transactions.stream()
            .filter(t -> t.getDate() != null && t.getDate().getMonth() == now.getMonth() && t.getDate().getYear() == now.getYear())
            .collect(Collectors.toList());
            
        double totalSpent = monthlyTransactions.stream()
            .filter(t -> t.getAmount() > 0)
            .mapToDouble(Transaction::getAmount)
            .sum();
            
        double remaining = user.getBudget() - totalSpent;
        
        // Group by category
        Map<String, Double> byCategory = monthlyTransactions.stream()
            .filter(t -> t.getAmount() > 0)
            .collect(Collectors.groupingBy(
                Transaction::getCategory,
                Collectors.summingDouble(Transaction::getAmount)
            ));
            
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Monthly Summary for %s (%s %d)\n\n", 
            user.getUsername(), now.getMonth(), now.getYear()));
        summary.append(String.format("Total Budget: $%.2f\n", user.getBudget()));
        summary.append(String.format("Total Spent: $%.2f\n", totalSpent));
        summary.append(String.format("Remaining: $%.2f\n\n", remaining));
        
        summary.append("Spending by Category:\n");
        byCategory.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .forEach(entry -> {
                summary.append(String.format("- %s: $%.2f\n", entry.getKey(), entry.getValue()));
            });
            
        return summary.toString();
    }

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
        
        // Category breakdown
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
            "isOverBudget", remaining < 0
        );
    }

    public void setBudgets(UUID userId, Map<String, Object> updates) {
        Optional<User> userOpt = getUser(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("User not found");
        }
        
        // For now, just update the user's total budget
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

    public List<Transaction> weeklySummary(UUID userId) {
        LocalDate oneWeekAgo = LocalDate.now().minusDays(7);
        String sql = "SELECT * FROM transactions WHERE user_id = ? AND created_date >= ? ORDER BY created_time DESC";
        return jdbcTemplate.query(sql, transactionRowMapper, userId, oneWeekAgo);
    }

    public double totalLast7Days(UUID userId) {
        LocalDate oneWeekAgo = LocalDate.now().minusDays(7);
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE user_id = ? AND created_date >= ? AND amount > 0";
        try {
            Double result = jdbcTemplate.queryForObject(sql, Double.class, userId, oneWeekAgo);
            return result != null ? result : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    // ========== ROW MAPPERS ==========

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setUserId((UUID) rs.getObject("user_id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setBudget(rs.getDouble("budget"));
        return user;
    };

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