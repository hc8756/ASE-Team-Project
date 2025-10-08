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

    public RouteController(final MockApiService mockApiService) {
        this.mockApiService = mockApiService;
    }

    @GetMapping({"/", "/index"})
    public ResponseEntity<?> index() {
        LOGGER.info("Accessed index route.");
        String message = "Welcome to the ledger home page!\nTo see this month's summary, GET /monthly-summary.";
        message = message.replace("\n", "<br>");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body("<html><body><pre>" + message + "</pre></body></html>");
    }

    @GetMapping("/monthly-summary")
    public ResponseEntity<?> monthlySummary() {
        ResponseEntity<?> response;
        try {
            String summary = mockApiService.getMonthlySummary()
                    .replace("\n", "<br>");
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body("<html><body><pre>" + summary + "</pre></body></html>");
        } catch (IllegalStateException | IllegalArgumentException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "Error getting monthly summary", e);
            }
            response = new ResponseEntity<>("Error occurred when getting monthly summary",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    @PatchMapping(
        value = "/update-transaction/{id}",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> updateTransaction(
            @PathVariable("id") long id,
            @RequestBody Map<String, Object> body
    ) {
        ResponseEntity<?> response;
        try {
            response = mockApiService.updateTransaction(id, body)
                    .<ResponseEntity<?>>map(updated -> ResponseEntity.ok(updated))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "transaction not found")));
        } catch (IllegalArgumentException ex) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, "Bad update payload", ex);
            }
            response = ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "Error updating transaction", ex);
            }
            response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "internal error"));
        }
        return response;    
    }
}
