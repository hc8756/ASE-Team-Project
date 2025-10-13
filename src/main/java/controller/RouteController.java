package controller;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import model.Ledger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import service.MockApiService;

/**
 * Unified controller for ledger/budget routes and transaction routes,
 * all backed by MockApiService (which uses Ledger internally).
 */
@RestController
public class RouteController {

  private static final Logger LOGGER = Logger.getLogger(RouteController.class.getName());

  private final MockApiService mockApiService;

  public RouteController(MockApiService mockApiService) {
    this.mockApiService = mockApiService;
  }

  // ---------------------------------------------------------------------------
  // Home & Monthly Summary
  // ---------------------------------------------------------------------------

  /** Home page. Shows instructions plus current budgets and any warnings. */
  @GetMapping({"/", "/index"})
  public ResponseEntity<?> index() {
    LOGGER.info("Accessed index route.");
    String message =
        "Welcome to the ledger home page!\n"
            + "To see this month's summary, GET /monthly-summary.";
    message += "\n" + mockApiService.getBudgetsTextBlock();
    String warns = mockApiService.getBudgetWarningsText();
    if (!warns.isBlank()) {
      message += "\n" + warns;
    }
    String html =
        "<html><body><pre>"
            + message.replace("\n", "<br>")
            + "</pre></body></html>";
    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
  }

  /** Monthly summary page. Shows spend summary, budgets, and warnings. */
  @GetMapping("/monthly-summary")
  public ResponseEntity<?> monthlySummary() {
    try {
      String summary = mockApiService.getMonthlySummary();
      String html =
          "<html><body><pre>"
              + summary.replace("\n", "<br>")
              + "</pre></body></html>";
      return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    } catch (IllegalStateException | IllegalArgumentException e) {
      if (LOGGER.isLoggable(Level.SEVERE)) {
        LOGGER.log(Level.SEVERE, "Error getting monthly summary", e);
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error occurred when getting monthly summary");
    }
  }

  // ---------------------------------------------------------------------------
  // Budgets
  // ---------------------------------------------------------------------------

  /** Returns the current budget report (JSON). */
  @GetMapping(value = "/view-budget", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> viewBudget() {
    try {
      return ResponseEntity.ok(mockApiService.getBudgetReport());
    } catch (Exception ex) {
      if (LOGGER.isLoggable(Level.SEVERE)) {
        LOGGER.log(Level.SEVERE, "Error getting budget report", ex);
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Sets budgets. Add/update/remove categories (set to 0 to remove).
   * Sum of categories must equal the total (if provided) or the existing total.
   */
  @PatchMapping(
      value = "/set-budget",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> setBudget(@RequestBody Map<String, Object> updates) {
    try {
      mockApiService.setBudgets(updates);
      return ResponseEntity.ok(mockApiService.getBudgetReport());
    } catch (IllegalArgumentException ex) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.log(Level.WARNING, "Bad budget payload", ex);
      }
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    } catch (Exception ex) {
      if (LOGGER.isLoggable(Level.SEVERE)) {
        LOGGER.log(Level.SEVERE, "Error setting budgets", ex);
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "internal error"));
    }
  }

  // ---------------------------------------------------------------------------
  // Transactions (all through MockApiService)
  // ---------------------------------------------------------------------------

  // Core RESTful routes already in your code:

  /** Create: POST /transactions */
  @PostMapping("/transactions")
  public ResponseEntity<Ledger> create(@RequestBody Ledger tx) {
    Ledger saved = mockApiService.addTransaction(tx);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
  }

  /** Read: GET /transactions */
  @GetMapping("/transactions")
  public List<Ledger> list() {
    return mockApiService.viewAllTransactions();
  }

  /** Read: GET /transactions/{id} */
  @GetMapping("/transactions/{id}")
  public ResponseEntity<Ledger> getById(@PathVariable long id) {
    return mockApiService
        .getTransaction(id)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new NoSuchElementException("Transaction " + id + " not found"));
  }

  /** Read: GET /transactions/weekly */
  @GetMapping("/transactions/weekly")
  public List<Ledger> weekly() {
    return mockApiService.weeklySummary();
  }

  /** Read: GET /transactions/weekly/total */
  @GetMapping("/transactions/weekly/total")
  public Map<String, Object> weeklyTotal() {
    double total = mockApiService.totalLast7Days();
    return Map.of("totalLast7Days", total);
  }

  /**
   * Update (partial): PATCH /update-transaction/{id}
   */
  @PatchMapping(
      value = "/update-transaction/{id}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> updateTransaction(
      @PathVariable("id") long id, @RequestBody Map<String, Object> body) {
    try {
      return mockApiService
          .updateTransaction(id, body)
          .<ResponseEntity<?>>map(ResponseEntity::ok)
          .orElseGet(
              () ->
                  ResponseEntity.status(HttpStatus.NOT_FOUND)
                      .body(Map.of("error", "transaction not found")));
    } catch (IllegalArgumentException ex) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.log(Level.WARNING, "Bad update payload", ex);
      }
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    } catch (Exception ex) {
      if (LOGGER.isLoggable(Level.SEVERE)) {
        LOGGER.log(Level.SEVERE, "Error updating transaction", ex);
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "internal error"));
    }
  }

  /** Delete: DELETE /transactions/{id} */
  @DeleteMapping("/transactions/{id}")
  public ResponseEntity<Map<String, Object>> delete(@PathVariable long id) {
    boolean deleted = mockApiService.deleteTransaction(id);
    if (!deleted) {
      throw new NoSuchElementException("Transaction " + id + " not found");
    }
    return ResponseEntity.ok(Map.of("deleted", true, "id", id));
  }

  // ---------------------------------------------------------------------------
  // Alias endpoints to match the issue titles (same behavior as above)
  // ---------------------------------------------------------------------------

  /** Make add-transaction endpoint: POST /transactions/add */
  @PostMapping("/transactions/add")
  public ResponseEntity<Ledger> addAlias(@RequestBody Ledger tx) {
    Ledger saved = mockApiService.addTransaction(tx);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
  }

  /** Make delete-transaction endpoint: DELETE /transactions/delete/{id} */
  @DeleteMapping("/transactions/delete/{id}")
  public ResponseEntity<Map<String, Object>> deleteAlias(@PathVariable long id) {
    boolean deleted = mockApiService.deleteTransaction(id);
    if (!deleted) {
      throw new NoSuchElementException("Transaction " + id + " not found");
    }
    return ResponseEntity.ok(Map.of("deleted", true, "id", id));
  }

  /** Make view-transactions endpoint: GET /transactions/view */
  @GetMapping("/transactions/view")
  public List<Ledger> viewAlias() {
    return mockApiService.viewAllTransactions();
    }

  /** Make weekly-summary endpoint: GET /transactions/weekly-summary */
  @GetMapping("/transactions/weekly-summary")
  public List<Ledger> weeklySummaryAlias() {
    return mockApiService.weeklySummary();
  }

  // ---------------------------------------------------------------------------
  // Exception handlers
  // ---------------------------------------------------------------------------

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
