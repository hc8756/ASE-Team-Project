package controller;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import service.MockApiService;

/**
 * Routes for the Ledger API.
 */
@RestController
public class RouteController {

  private final MockApiService mockApiService;
  private static final Logger LOGGER = Logger.getLogger(RouteController.class.getName());

  /**
   * Constructs the route controller.
   *
   * @param mockApiService service providing ledger and budget operations
   */
  public RouteController(final MockApiService mockApiService) {
    this.mockApiService = mockApiService;
  }

  /**
   * Home page. Shows instructions plus current budgets and any warnings.
   *
   * @return HTML response for the index page
   */
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

  /**
   * Monthly summary page. Shows spend summary, budgets, and warnings.
   *
   * @return HTML response for the monthly summary
   */
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

  /**
   * Updates a transaction's mutable fields (amount, category, description).
   * The id and date fields cannot be changed.
   *
   * @param id transaction id
   * @param body JSON map with fields to update
   * @return updated transaction or an error status
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

  /**
   * Returns the current budget report including total and per-category values,
   * current month spend, remaining, and over-budget flags.
   *
   * @return JSON budget report
   */
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
   * Sets budgets. You may add, update, or remove categories (set to 0 to remove).
   * Categories not provided remain unchanged. The sum of categories must equal
   * the total (if provided) or the existing total (if not provided).
   *
   * @param updates JSON map with "Total" and/or category budgets
   * @return updated budget report or an error status
   */
  @PatchMapping(
      value = "/set-budget",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> setBudget(
      @RequestBody Map<String, Object> updates) {
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
}