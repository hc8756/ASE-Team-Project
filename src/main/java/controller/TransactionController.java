package controller;

import model.Transaction;
import org.springframework.web.bind.annotation.*;
import service.TransactionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    // Add a transaction (id/timestamp are ignored if sent; they’re set by the service)
    @PostMapping("/add")
    public Transaction add(@RequestBody Transaction tx) {
        return service.add(tx);
    }

    // Delete by id
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable long id) {
        boolean ok = service.delete(id);
        return Map.of("deleted", ok, "id", id);
    }

    // View all transactions
    @GetMapping("/view")
    public List<Transaction> view() {
        return service.viewAll();
    }

    // Transactions from the last 7 days
    @GetMapping("/weekly-summary")
    public List<Transaction> weeklySummary() {
        return service.weeklySummary();
    }
}
