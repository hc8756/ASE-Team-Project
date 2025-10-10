package controller;

import dev.coms4156.project.individualproject.model.Transaction;
import dev.coms4156.project.individualproject.model.User;
import service.MockApiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class RouteController {
    private final MockApiService mockApiService;

    public RouteController(MockApiService mockApiService) {
        this.mockApiService = mockApiService;
    }

    @GetMapping({"/", "/index"})
    public ResponseEntity<?> index() {
      String message =
          "Welcome\n"
              + "To see all users, go to /users\n"
              + "To see all transactions, go to /transactions";
      String html =
          "<html><body><pre>"
              + message.replace("\n", "<br>")
              + "</pre></body></html>";
      return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }
    
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return mockApiService.getAllUsers();
    }

    @GetMapping("/transactions")
    public List<Transaction> getAllTransactions() {
        return mockApiService.getAllTransactions();
    }

}