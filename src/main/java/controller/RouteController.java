package controller;

import service.MockApiService;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<String> index() {
        LOGGER.info("Accessed index route.");
        String message = "Welcome to the ledger home page!\nTo see this month's summary, GET /monthly-summary.";
        message = message.replace("\n", "<br>");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body("<html><body><pre>" + message + "</pre></body></html>");
    }

    @GetMapping("/monthly-summary")
    public ResponseEntity<String> monthlySummary() {
        ResponseEntity<String> response;
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
}
