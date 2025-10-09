package dev.coms4156.project.individualproject.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import dev.coms4156.project.individualproject.model.User;
import dev.coms4156.project.individualproject.model.Transaction;

import java.util.List;
import java.util.UUID;

@Service
public class DatabaseService {
    private final JdbcTemplate jdbcTemplate;
    
    public DatabaseService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    // USER OPERATIONS
    public void createUser(User user) {
        String sql = "INSERT INTO users (user_id, username, email, budget) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, 
            user.getUserId() != null ? user.getUserId() : UUID.randomUUID(),
            user.getUsername(),
            user.getEmail(), 
            user.getBudget());
    }
    
    public List<User> getAllUsers() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, userRowMapper);
    }
    
    public User getUserById(UUID userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        return jdbcTemplate.queryForObject(sql, userRowMapper, userId);
    }
    
    // TRANSACTION OPERATIONS  
    public void addTransaction(Transaction transaction) {
        String sql = "INSERT INTO transactions (trans_id, user_id, description, amount, category) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, 
            transaction.getTransId() != null ? transaction.getTransId() : UUID.randomUUID(),
            transaction.getUserId(),
            transaction.getDescription(), 
            transaction.getAmount(), 
            transaction.getCategory());
    }
    
    public List<Transaction> getAllTransactions() {
        String sql = "SELECT * FROM transactions";
        return jdbcTemplate.query(sql, transactionRowMapper);
    }
    
    public List<Transaction> getUserTransactions(UUID userId) {
        String sql = "SELECT * FROM transactions WHERE user_id = ?";
        return jdbcTemplate.query(sql, transactionRowMapper, userId);
    }
    
    // Row mappers (convert database rows to Java objects)
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