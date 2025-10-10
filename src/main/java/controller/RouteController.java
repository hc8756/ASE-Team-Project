package controller;

import dev.coms4156.project.individualproject.model.Transaction;
import dev.coms4156.project.individualproject.model.User;
import service.MockApiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class RouteController {
    private final MockApiService mockApiService;

    public RouteController(MockApiService mockApiService) {
        this.mockApiService = mockApiService;
    }

    // ========== DEBUG ENDPOINT ==========
    
    @GetMapping("/debug")
    public String debugAllData() {
        return mockApiService.getAllDataForDebug();
    }

    // ========== USER ENDPOINTS ==========

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return mockApiService.getAllUsers();
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable UUID id) {
        return mockApiService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/users")
    public User createUser(@RequestBody User user) {
        return mockApiService.createUser(user);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        boolean deleted = mockApiService.deleteUser(id);
        if (deleted) {
            return ResponseEntity.ok().body("User deleted successfully");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    // ========== TRANSACTION ENDPOINTS ==========

    @GetMapping("/transactions")
    public List<Transaction> getAllTransactions() {
        return mockApiService.getAllTransactions();
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable UUID id) {
        return mockApiService.getTransactionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{userId}/transactions")
    public List<Transaction> getUserTransactions(@PathVariable UUID userId) {
        return mockApiService.getTransactionsByUser(userId);
    }

    @PostMapping("/transactions")
    public Transaction createTransaction(@RequestBody Transaction transaction) {
        return mockApiService.createTransaction(transaction);
    }

    @PutMapping("/transactions/{id}")
    public ResponseEntity<Transaction> updateTransaction(@PathVariable UUID id, @RequestBody Transaction transaction) {
        return mockApiService.updateTransaction(id, transaction)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable UUID id) {
        boolean deleted = mockApiService.deleteTransaction(id);
        if (deleted) {
            return ResponseEntity.ok().body("Transaction deleted successfully");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Transaction not found");
        }
    }
}