package dev.ase.teamproject;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ase.teamproject.model.Transaction;
import dev.ase.teamproject.model.User;
import dev.ase.teamproject.service.MockApiService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Tests (API Tests) for the RouteController.
 *
 * This test suite uses @SpringBootTest and MockMvc to test the full
 * HTTP request-response cycle, including request mapping, JSON serialization,
 * and exception handling at the web layer.
 *
 * It fulfills the "API Testing" and "Integration Testing" criteria by testing
 * the integration between the Web Layer (RouteController) and the
 * Service Layer (MockApiService).
 */
@SpringBootTest
@AutoConfigureMockMvc
public class RouteControllerIntegrationTests {

    /**
     * Used to send simulated HTTP requests to the controller.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Used to serialize Java objects into JSON strings for POST/PUT bodies.
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Replaces the real MockApiService bean in the application context
     * with a Mockito mock, allowing us to control its behavior.
     */
    @MockBean
    private MockApiService mockApiService;

    // ---------------------------------------------------------------------------
    // User Endpoint Tests
    // ---------------------------------------------------------------------------

    /**
     * Tests GET /users
     * Partition: P1 (Valid) - Users exist.
     */
    @Test
    public void getAllUsers_usersExist_returns200OkAndUserList() throws Exception {
        // Arrange
        User user1 = new User("Alice", "alice@example.com", 100);
        User user2 = new User("Bob", "bob@example.com", 200);
        when(mockApiService.viewAllUsers()).thenReturn(List.of(user1, user2));

        // Act & Assert
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2))) // $ is the root (the list)
                .andExpect(jsonPath("$[0].username", is("Alice")))
                .andExpect(jsonPath("$[1].username", is("Bob")));
    }

    /**
     * Tests GET /users/{userId}
     * Partition: P1 (Valid) - User exists.
     */
    @Test
    public void getUser_existingUser_returns200OkAndUser() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        User user = new User("Alice", "alice@example.com", 100);
        user.setUserId(userId);
        when(mockApiService.getUser(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        mockMvc.perform(get("/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username", is("Alice")))
                .andExpect(jsonPath("$.email", is("alice@example.com")))
                .andExpect(jsonPath("$.userId", is(userId.toString())));
    }

    /**
     * Tests GET /users/{userId}
     * Partition: P3 (Invalid) - User does not exist.
     * This also tests the @ExceptionHandler(NoSuchElementException.class)
     */
    @Test
    public void getUser_nonexistentUser_returns404NotFound() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(mockApiService.getUser(userId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/users/{userId}", userId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("User " + userId + " not found")));
    }

    /**
     * Tests POST /users (JSON)
     * Partition: P1 (Valid) - Valid user payload.
     */
    @Test
    public void createUserJson_validUser_returns201Created() throws Exception {
        // Arrange
        User userToCreate = new User("NewUser", "new@example.com", 500);
        
        // This is what the service will "save" and return
        User savedUser = new User("NewUser", "new@example.com", 500);
        savedUser.setUserId(UUID.randomUUID());

        // We use any() because the user object sent to addUser won't have the ID yet
        when(mockApiService.addUser(any(User.class))).thenReturn(savedUser);

        // Act & Assert
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userToCreate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("NewUser")))
                .andExpect(jsonPath("$.userId", is(savedUser.getUserId().toString())));
    }

    /**
     * Tests POST /users (JSON)
     * Partition: P5 (Invalid) - Duplicate email.
     * This also tests the @ExceptionHandler(IllegalArgumentException.class)
     */
    @Test
    public void createUserJson_duplicateEmail_returns400BadRequest() throws Exception {
        // Arrange
        User userToCreate = new User("NewUser", "taken@example.com", 500);
        
        // Mock the service to throw the exception the controller expects
        when(mockApiService.addUser(any(User.class)))
                .thenThrow(new IllegalArgumentException("Email already exists: taken@example.com"));

        // Act & Assert
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userToCreate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Email already exists: taken@example.com")));
    }

    /**
     * Tests POST /users/form (HTML)
     * Partition: P1 (Valid) - Valid form data.
     */
    @Test
    public void createUserFromFormHtml_validForm_returns201CreatedHtml() throws Exception {
        // Arrange
        User savedUser = new User("FormUser", "form@example.com", 300);
        savedUser.setUserId(UUID.randomUUID());

        when(mockApiService.isUsernameExists("FormUser", null)).thenReturn(false);
        when(mockApiService.isEmailExists("form@example.com", null)).thenReturn(false);
        when(mockApiService.addUser(any(User.class))).thenReturn(savedUser);

        // Act & Assert
        mockMvc.perform(post("/users/form")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "FormUser")
                .param("email", "form@example.com")
                .param("budget", "300"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("User Created Successfully!")))
                .andExpect(content().string(containsString("FormUser")));
    }

    /**
     * Tests DELETE /users/{userId}
     * Partition: P1 (Valid) - User exists.
     */
    @Test
    public void deleteUser_existingUser_returns200Ok() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(mockApiService.deleteUser(userId)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted", is(true)))
                .andExpect(jsonPath("$.userId", is(userId.toString())));
    }

    /**
     * Tests DELETE /users/{userId}
     * Partition: P2 (Invalid) - User does not exist.
     */
    @Test
    public void deleteUser_nonExistentUser_returns404NotFound() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(mockApiService.deleteUser(userId)).thenReturn(false); // Service reports delete failed

        // Act & Assert
        mockMvc.perform(delete("/users/{userId}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("User " + userId + " not found")));
    }

    // ---------------------------------------------------------------------------
    // Transaction Endpoint Tests
    // ---------------------------------------------------------------------------

    /**
     * Tests GET /users/{userId}/transactions
     * Partition: P1 (Valid) - User exists, transactions returned.
     */
    @Test
    public void getUserTransactions_existingUser_returns200OkAndTransactions() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        Transaction t1 = new Transaction(userId, 50.0, "Food", "Lunch");
        Transaction t2 = new Transaction(userId, 120.0, "Shopping", "Clothes");
        t1.setTransactionId(UUID.randomUUID());
        t2.setTransactionId(UUID.randomUUID());
        
        when(mockApiService.getUser(userId)).thenReturn(Optional.of(new User())); // User exists
        when(mockApiService.getTransactionsByUser(userId)).thenReturn(List.of(t1, t2));

        // Act & Assert
        mockMvc.perform(get("/users/{userId}/transactions", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].description", is("Lunch")))
                .andExpect(jsonPath("$[1].description", is("Clothes")));
    }

    /**
     * Tests POST /users/{userId}/transactions
     * Partition: P1 (Valid) - Valid user and transaction.
     */
    @Test
    public void createTransactionJson_valid_returns201Created() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        Transaction txToCreate = new Transaction(userId, 75.0, "Food", "Dinner");
        Transaction savedTx = new Transaction(userId, 75.0, "Food", "Dinner");
        savedTx.setTransactionId(UUID.randomUUID());

        when(mockApiService.getUser(userId)).thenReturn(Optional.of(new User())); // User exists
        when(mockApiService.addTransaction(any(Transaction.class))).thenReturn(savedTx);

        // Act & Assert
        mockMvc.perform(post("/users/{userId}/transactions", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(txToCreate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description", is("Dinner")))
                .andExpect(jsonPath("$.transactionId", is(savedTx.getTransactionId().toString())));
    }

    /**
     * Tests POST /users/{userId}/transactions
     * Partition: (Invalid) - User does not exist.
     */
    @Test
    public void createTransactionJson_userNotFound_returns404NotFound() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        Transaction txToCreate = new Transaction(userId, 75.0, "Food", "Dinner");

        when(mockApiService.getUser(userId)).thenReturn(Optional.empty()); // User does NOT exist

        // Act & Assert
        mockMvc.perform(post("/users/{userId}/transactions", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(txToCreate)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("User " + userId + " not found")));
    }
}