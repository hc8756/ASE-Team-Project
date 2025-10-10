package service;

import dev.coms4156.project.individualproject.model.Transaction;
import dev.coms4156.project.individualproject.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MockApiService {
    private final JdbcTemplate jdbcTemplate;

    public MockApiService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ========== USER CRUD OPERATIONS ==========

    public List<User> getAllUsers() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    public Optional<User> getUserById(UUID userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, userId);
            return Optional.ofNullable(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public User createUser(User user) {
        String sql = "INSERT INTO users (username, email, budget) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, user.getUsername(), user.getEmail(), user.getBudget());
        return user;
    }

    public boolean deleteUser(UUID userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, userId);
        return rowsAffected > 0;
    }

    // ========== TRANSACTION CRUD OPERATIONS ==========

    public List<Transaction> getAllTransactions() {
        String sql = "SELECT * FROM transactions ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, transactionRowMapper);
    }

    public Optional<Transaction> getTransactionById(UUID transId) {
        String sql = "SELECT * FROM transactions WHERE trans_id = ?";
        try {
            Transaction transaction = jdbcTemplate.queryForObject(sql, transactionRowMapper, transId);
            return Optional.ofNullable(transaction);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<Transaction> getTransactionsByUser(UUID userId) {
        String sql = "SELECT * FROM transactions WHERE user_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, transactionRowMapper, userId);
    }

    public Transaction createTransaction(Transaction transaction) {
        String sql = "INSERT INTO transactions (user_id, description, amount, category) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, 
            transaction.getUserId(),
            transaction.getDescription(),
            transaction.getAmount(),
            transaction.getCategory());
        return transaction;
    }

    public Optional<Transaction> updateTransaction(UUID transId, Transaction transaction) {
        String sql = "UPDATE transactions SET description = ?, amount = ?, category = ? WHERE trans_id = ?";
        int rowsAffected = jdbcTemplate.update(sql,
            transaction.getDescription(),
            transaction.getAmount(),
            transaction.getCategory(),
            transId);
        
        if (rowsAffected > 0) {
            return getTransactionById(transId);
        }
        return Optional.empty();
    }

    public boolean deleteTransaction(UUID transId) {
        String sql = "DELETE FROM transactions WHERE trans_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, transId);
        return rowsAffected > 0;
    }

    // ========== DEBUG/ALL DATA ENDPOINT ==========

    public String getAllDataForDebug() {
        List<User> users = getAllUsers();
        List<Transaction> transactions = getAllTransactions();
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== DATABASE DEBUG VIEW ===\n\n");
        
        sb.append("USERS (").append(users.size()).append("):\n");
        for (User user : users) {
            sb.append(String.format("- %s | %s | %s | Budget: $%.2f\n", 
                user.getUserId(), user.getUsername(), user.getEmail(), user.getBudget()));
        }
        
        sb.append("\nTRANSACTIONS (").append(transactions.size()).append("):\n");
        for (Transaction trans : transactions) {
            sb.append(String.format("- %s | User: %s | %s | $%.2f | %s | %s\n", 
                trans.getTransId(), trans.getUserId(), trans.getDescription(), 
                trans.getAmount(), trans.getCategory(), trans.getCreatedAt()));
        }
        
        return sb.toString();
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
        transaction.setTransId((UUID) rs.getObject("trans_id"));
        transaction.setUserId((UUID) rs.getObject("user_id"));
        transaction.setDescription(rs.getString("description"));
        transaction.setAmount(rs.getDouble("amount"));
        transaction.setCategory(rs.getString("category"));
        transaction.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return transaction;
    };
}