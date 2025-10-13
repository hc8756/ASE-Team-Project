package controller;

import model.Transaction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.TransactionService;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    // ---- Create ----
    // POST /transactions
    // (id/timestamp in body are ignored; service sets them)
    @PostMapping
    public ResponseEntity<Transaction> create(@RequestBody Transaction tx) {
        Transaction saved = service.add(tx);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ---- Read ----
    // GET /transactions
    @GetMapping
    public List<Transaction> list() {
        return service.viewAll();
    }

    // GET /transactions/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getById(@PathVariable long id) {
        return service.get(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NoSuchElementException("Transaction " + id + " not found"));
    }

    // GET /transactions/weekly
    @GetMapping("/weekly")
    public List<Transaction> weekly() {
        return service.weeklySummary();
    }

    // GET /transactions/weekly/total
    @GetMapping("/weekly/total")
    public Map<String, Object> weeklyTotal() {
        double total = service.totalLast7Days();
        return Map.of("totalLast7Days", total);
    }

    // ---- Delete ----
    // DELETE /transactions/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable long id) {
        boolean deleted = service.delete(id);
        if (!deleted) {
            throw new NoSuchElementException("Transaction " + id + " not found");
        }
        return ResponseEntity.ok(Map.of("deleted", true, "id", id));
    }

    // ---- Simple exception mapping ----
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
