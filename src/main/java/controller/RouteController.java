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

    public RouteController(MockApiService mockApiService) {
        this.mockApiService = mockApiService;
    }

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

    @GetMapping("/monthly-summary")
    public ResponseEntity<?> monthlySummary() {
        try {
            String summary = mockApiService.getMonthlySummary();
            String html =
                "<html><body><pre>"
                    + summary.replace("\n", "<br>")
                    + "</pre></body></html>";
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting monthly summary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error occurred when getting monthly summary");
        }
    }
}