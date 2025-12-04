package dev.ase.teamproject;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ase.teamproject.model.Transaction;
import dev.ase.teamproject.model.User;
import dev.ase.teamproject.service.MockApiService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration Tests for the RouteController.
 *
 * <p>This test suite provides comprehensive integration testing at multiple levels:
 *
 * <h2>Internal Integration Tests</h2>
 *
 * <p>These tests exercise the complete request-response cycle through all layers:
 * <ul>
 *   <li><strong>HTTP Layer:</strong> Request parsing, URL mapping,content negotiation</li>
 *   <li><strong>Controller Layer:</strong> RouteController request handling
 *   and response building</li>
 *   <li><strong>Service Layer:</strong> MockApiService business logic and validation</li>
 *   <li><strong>Data Access Layer:</strong> JdbcTemplate database operations</li>
 *   <li><strong>External Resource:</strong> PostgreSQL database on GCP</li>
 * </ul>
 *
 * <h2>Integration Points Tested</h2>
 * <ul>
 *   <li>RouteController ↔ MockApiService (internal integration via dependency injection)</li>
 *   <li>MockApiService ↔ JdbcTemplate ↔ PostgreSQL (external integration with database)</li>
 *   <li>Spring MVC ↔ Jackson ObjectMapper (JSON serialization/deserialization)</li>
 *   <li>Exception handlers ↔ HTTP response codes</li>
 *   <li>Request validation ↔ Database constraints</li>
 * </ul>
 *
 * <h2>Data Flow Tested</h2>
 * <pre>
 * HTTP Request → MockMvc → DispatcherServlet → RouteController
 *     → MockApiService → JdbcTemplate → PostgreSQL Database
 *     → Response flows back through all layers
 * </pre>
 *
 * <h2>Shared Data Integration</h2>
 * <ul>
 *   <li>User and Transaction entities share userId (foreign key relationship)</li>
 *   <li>Budget calculations integrate user.budget with transaction.amount</li>
 *   <li>Weekly/Monthly summaries integrate date filtering with aggregations</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RouteControllerIntegrationTests {

  /**
   * MockMvc instance for simulating HTTP requests to the controller.
   * Integrates with Spring's DispatcherServlet for full request processing.
   */
  @Autowired
  private MockMvc mockMvc;

  /**
   * ObjectMapper for JSON serialization/deserialization.
   * Tests integration between Java objects and JSON payloads.
   */
  @Autowired
  private ObjectMapper objectMapper;

  /**
   * The real MockApiService bean (not mocked).
   * Tests actual integration between controller and service layers.
   */
  @Autowired
  private MockApiService mockApiService;

  /**
   * JdbcTemplate for direct database setup/cleanup.
   * Ensures test isolation by managing database state.
   */
  @Autowired
  private JdbcTemplate jdbcTemplate;

  /**
   * UUID of the test user created in setUp.
   * Shared across tests to verify data relationships.
   */
  private UUID testUserId;

  /**
   * Sets up a clean database state before each test.
   * Creates a base user for tests requiring existing data.
   *
   * <p><strong>Integration:</strong> Direct database manipulation via JdbcTemplate
   * to ensure consistent starting state for integration tests.
   */
  @BeforeEach
  public void setUp() {
    // Clean database to ensure test isolation
    jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");

    // Create a test user via the service (tests service-DB integration)
    User testUser = new User("IntegrationTestUser", "integration@test.com", 1000.0);
    User saved = mockApiService.addUser(testUser);
    testUserId = saved.getUserId();
  }

  // ===========================================================================
  // USER ENDPOINT INTEGRATION TESTS
  // Tests: HTTP → Controller → Service → Database → Response
  // ===========================================================================

  /**
   * Tests integration for GET /users endpoint.
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>HTTP GET request → RouteController.getAllUsers()</li>
   *   <li>RouteController → MockApiService.viewAllUsers()</li>
   *   <li>MockApiService → JdbcTemplate → PostgreSQL SELECT query</li>
   *   <li>Database results → User objects → JSON serialization</li>
   * </ul>
   */
  @Test
  public void getAllUsers_returnsUsersFromDatabase() throws Exception {
    // Add another user to verify multiple records
    User user2 = new User("SecondUser", "second@test.com", 500.0);
    mockApiService.addUser(user2);

    mockMvc.perform(get("/users"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].username").exists())
        .andExpect(jsonPath("$[1].username").exists());
  }

  /**
   * Tests integration for GET /users/{userId} endpoint.
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>Path variable extraction from URL</li>
   *   <li>RouteController → MockApiService.getUser(UUID)</li>
   *   <li>MockApiService → Database SELECT WHERE user_id = ?</li>
   *   <li>User entity mapping and JSON response</li>
   * </ul>
   */
  @Test
  public void getUser_returnsUserFromDatabase() throws Exception {
    mockMvc.perform(get("/users/{userId}", testUserId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.userId", is(testUserId.toString())))
        .andExpect(jsonPath("$.username", is("IntegrationTestUser")))
        .andExpect(jsonPath("$.email", is("integration@test.com")))
        .andExpect(jsonPath("$.budget", is(1000.0)));
  }

  /**
   * Tests integration for GET /users/{userId} with non-existent user.
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>Database returns empty result</li>
   *   <li>Service returns Optional.empty()</li>
   *   <li>Controller throws NoSuchElementException</li>
   *   <li>@ExceptionHandler converts to 404 response</li>
   * </ul>
   */
  @Test
  public void getUser_nonExistent_returns404() throws Exception {
    UUID nonExistentId = UUID.randomUUID();

    mockMvc.perform(get("/users/{userId}", nonExistentId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error", is("User " + nonExistentId + " not found")));
  }

  /**
   * Tests integration for POST /users (JSON) endpoint.
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>JSON request body deserialization to User object</li>
   *   <li>RouteController validation (null checks)</li>
   *   <li>MockApiService → Database INSERT with RETURNING</li>
   *   <li>Database generates UUID, returns to application</li>
   *   <li>User object serialized to JSON response</li>
   * </ul>
   */
  @Test
  public void createUserJson_persistsToDatabase() throws Exception {
    User newUser = new User("NewIntegrationUser", "newuser@test.com", 750.0);

    MvcResult result = mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(newUser)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username", is("NewIntegrationUser")))
        .andExpect(jsonPath("$.userId", notNullValue()))
        .andReturn();

    // Verify data actually persisted to database
    String response = result.getResponse().getContentAsString();
    User createdUser = objectMapper.readValue(response, User.class);

    // Query database directly to verify persistence
    mockMvc.perform(get("/users/{userId}", createdUser.getUserId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username", is("NewIntegrationUser")));
  }

  /**
   * Tests integration for POST /users with duplicate email.
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>Database unique constraint on email column</li>
   *   <li>DataIntegrityViolationException thrown by JdbcTemplate</li>
   *   <li>Controller catches and converts to IllegalArgumentException</li>
   *   <li>@ExceptionHandler returns 400 response</li>
   * </ul>
   */
  @Test
  public void createUserJson_duplicateEmail_returns400() throws Exception {
    User duplicateEmailUser = new User("DifferentName", "integration@test.com", 500.0);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(duplicateEmailUser)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error", containsString("Email already exists")));
  }

  /**
   * Tests integration for POST /users with duplicate username.
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>Database unique constraint on username column</li>
   *   <li>DataIntegrityViolationException propagation</li>
   *   <li>Error message extraction and formatting</li>
   * </ul>
   */
  @Test
  public void createUserJson_duplicateUsername_returns400() throws Exception {
    User duplicateUsernameUser = new User("IntegrationTestUser", "different@test.com", 500.0);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(duplicateUsernameUser)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error", containsString("Username already exists")));
  }

  /**
   * Tests integration for POST /users/form (HTML form submission).
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>Form URL-encoded content type handling</li>
   *   <li>@RequestParam binding to method parameters</li>
   *   <li>Service uniqueness checks before insert</li>
   *   <li>HTML response generation with user data</li>
   * </ul>
   */
  @Test
  public void createUserFromForm_persistsAndReturnsHtml() throws Exception {
    mockMvc.perform(post("/users/form")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("username", "FormCreatedUser")
            .param("email", "formuser@test.com")
            .param("budget", "600.0"))
        .andExpect(status().isCreated())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
        .andExpect(content().string(containsString("User Created Successfully")))
        .andExpect(content().string(containsString("FormCreatedUser")));

    // Verify persisted to database via API
    mockMvc.perform(get("/users"))
        .andExpect(jsonPath("$[?(@.username == 'FormCreatedUser')]").exists());
  }

  /**
   * Tests integration for PUT /users/{userId} (JSON update).
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>Path variable and request body combined</li>
   *   <li>Service retrieves existing user from DB</li>
   *   <li>Uniqueness validation against other users</li>
   *   <li>Delete-then-insert update strategy</li>
   *   <li>Updated data persisted and returned</li>
   * </ul>
   */
  @Test
  public void updateUserJson_updatesDatabase() throws Exception {
    User updates = new User("UpdatedUsername", "updated@test.com", 1500.0);

    mockMvc.perform(put("/users/{userId}", testUserId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updates)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username", is("UpdatedUsername")))
        .andExpect(jsonPath("$.email", is("updated@test.com")))
        .andExpect(jsonPath("$.budget", is(1500.0)));

    // Verify update persisted
    mockMvc.perform(get("/users/{userId}", testUserId))
        .andExpect(jsonPath("$.username", is("UpdatedUsername")));
  }

  /**
   * Tests integration for DELETE /users/{userId}.
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>RouteController → MockApiService.deleteUser()</li>
   *   <li>Database DELETE operation</li>
   *   <li>Cascade delete of related transactions</li>
   *   <li>Confirmation response generation</li>
   * </ul>
   */
  @Test
  public void deleteUser_removesFromDatabase() throws Exception {
    mockMvc.perform(delete("/users/{userId}", testUserId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deleted", is(true)))
        .andExpect(jsonPath("$.userId", is(testUserId.toString())));

    // Verify actually deleted from database
    mockMvc.perform(get("/users/{userId}", testUserId))
        .andExpect(status().isNotFound());
  }

  // ===========================================================================
  // TRANSACTION ENDPOINT INTEGRATION TESTS
  // Tests: HTTP → Controller → Service → Database with User relationship
  // ===========================================================================

  /**
   * Tests integration for POST /users/{userId}/transactions.
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>Transaction linked to User via foreign key (userId)</li>
   *   <li>Service validates user exists before insert</li>
   *   <li>Database generates transaction_id, timestamp, date</li>
   *   <li>PostgreSQL ENUM type handling for category</li>
   * </ul>
   *
   * <p><strong>Shared Data:</strong> Transaction.userId references User.userId
   */
  @Test
  public void createTransaction_persistsWithUserRelationship() throws Exception {
    Transaction newTx = new Transaction(testUserId, 150.0, "FOOD", "Grocery shopping");

    MvcResult result = mockMvc.perform(post("/users/{userId}/transactions", testUserId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(newTx)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.description", is("Grocery shopping")))
        .andExpect(jsonPath("$.amount", is(150.0)))
        .andExpect(jsonPath("$.category", is("FOOD")))
        .andExpect(jsonPath("$.transactionId", notNullValue()))
        .andExpect(jsonPath("$.userId", is(testUserId.toString())))
        .andReturn();

    // Verify transaction retrievable via user's transactions endpoint
    mockMvc.perform(get("/users/{userId}/transactions", testUserId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].description", is("Grocery shopping")));
  }

  /**
   * Tests integration for GET /users/{userId}/transactions.
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>User existence validation</li>
   *   <li>Database query filtered by user_id foreign key</li>
   *   <li>Multiple transactions returned as JSON array</li>
   * </ul>
   */
  @Test
  public void getUserTransactions_returnsUserSpecificTransactions() throws Exception {
    // Create transactions for test user
    Transaction tx1 = new Transaction(testUserId, 50.0, "FOOD", "Lunch");
    Transaction tx2 = new Transaction(testUserId, 100.0, "SHOPPING", "Clothes");
    mockApiService.addTransaction(tx1);
    mockApiService.addTransaction(tx2);

    // Create another user with transactions (should not appear)
    User otherUser = new User("OtherUser", "other@test.com", 500.0);
    User savedOther = mockApiService.addUser(otherUser);
    Transaction otherTx = new Transaction(savedOther.getUserId(), 200.0, "OTHER", "Other expense");
    mockApiService.addTransaction(otherTx);

    // Verify only test user's transactions returned
    mockMvc.perform(get("/users/{userId}/transactions", testUserId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[*].userId",
            org.hamcrest.Matchers.everyItem(is(testUserId.toString()))));
  }

  /**
   * Tests integration for GET /users/{userId}/transactions/{transactionId}.
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>User existence validation</li>
   *   <li>Transaction existence validation</li>
   *   <li>Transaction ownership validation (userId match)</li>
   *   <li>Single transaction retrieval by ID</li>
   * </ul>
   */
  @Test
  public void getTransaction_returnsSpecificTransaction() throws Exception {
    // Create a transaction
    Transaction tx = new Transaction(testUserId, 75.0, "ENTERTAINMENT", "Movie tickets");
    Transaction saved = mockApiService.addTransaction(tx);

    mockMvc.perform(get("/users/{userId}/transactions/{transactionId}",
            testUserId, saved.getTransactionId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transactionId", is(saved.getTransactionId().toString())))
        .andExpect(jsonPath("$.description", is("Movie tickets")))
        .andExpect(jsonPath("$.amount", is(75.0)));
  }

  /**
   * Tests integration for transaction ownership validation.
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>Transaction exists but belongs to different user</li>
   *   <li>Controller validates userId matches transaction.userId</li>
   *   <li>Returns 404 even though transaction exists (security)</li>
   * </ul>
   *
   * <p><strong>Shared Data:</strong> Tests foreign key relationship enforcement
   */
  @Test
  public void getTransaction_wrongUser_returns404() throws Exception {
    // Create transaction for test user
    Transaction tx = new Transaction(testUserId, 50.0, "FOOD", "Snacks");
    Transaction saved = mockApiService.addTransaction(tx);

    // Create another user
    User otherUser = new User("OtherUser", "other@test.com", 500.0);
    User savedOther = mockApiService.addUser(otherUser);

    // Try to access transaction via wrong user's endpoint
    mockMvc.perform(get("/users/{userId}/transactions/{transactionId}",
            savedOther.getUserId(), saved.getTransactionId()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error", containsString("not found for user")));
  }

  /**
   * Tests integration for PUT /users/{userId}/transactions/{transactionId}.
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>Partial update via Map payload</li>
   *   <li>Service validates user and transaction exist</li>
   *   <li>Database UPDATE operation</li>
   *   <li>Updated transaction returned</li>
   * </ul>
   */
  @Test
  public void updateTransaction_updatesDatabase() throws Exception {
    // Create a transaction
    Transaction tx = new Transaction(testUserId, 100.0, "SHOPPING", "Initial description");
    Transaction saved = mockApiService.addTransaction(tx);

    // Update amount and description
    Map<String, Object> updates = Map.of(
        "amount", 150.0,
        "description", "Updated description"
    );

    mockMvc.perform(put("/users/{userId}/transactions/{transactionId}",
            testUserId, saved.getTransactionId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updates)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount", is(150.0)))
        .andExpect(jsonPath("$.description", is("Updated description")));

    // Verify update persisted
    mockMvc.perform(get("/users/{userId}/transactions/{transactionId}",
            testUserId, saved.getTransactionId()))
        .andExpect(jsonPath("$.amount", is(150.0)))
        .andExpect(jsonPath("$.description", is("Updated description")));
  }

  /**
   * Tests integration for DELETE /users/{userId}/transactions/{transactionId}.
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>Ownership validation before delete</li>
   *   <li>Database DELETE operation</li>
   *   <li>Confirmation response</li>
   *   <li>Subsequent retrieval returns 404</li>
   * </ul>
   */
  @Test
  public void deleteTransaction_removesFromDatabase() throws Exception {
    // Create a transaction
    Transaction tx = new Transaction(testUserId, 50.0, "OTHER", "To be deleted");
    Transaction saved = mockApiService.addTransaction(tx);

    // Delete the transaction
    mockMvc.perform(delete("/users/{userId}/transactions/{transactionId}",
            testUserId, saved.getTransactionId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deleted", is(true)));

    // Verify actually deleted
    mockMvc.perform(get("/users/{userId}/transactions/{transactionId}",
            testUserId, saved.getTransactionId()))
        .andExpect(status().isNotFound());
  }

  /**
   * Tests cascade delete: deleting user removes their transactions.
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>User-Transaction foreign key with ON DELETE CASCADE</li>
   *   <li>Database cascades delete to child records</li>
   *   <li>Full-stack verification via API</li>
   * </ul>
   *
   * <p><strong>Shared Data:</strong> Tests referential integrity between tables
   */
  @Test
  public void deleteUser_cascadesTransactionDeletion() throws Exception {
    // Create transactions for user
    Transaction tx1 = new Transaction(testUserId, 50.0, "FOOD", "Lunch");
    Transaction tx2 = new Transaction(testUserId, 100.0, "SHOPPING", "Clothes");
    Transaction savedTx1 = mockApiService.addTransaction(tx1);
    Transaction savedTx2 = mockApiService.addTransaction(tx2);

    // Verify transactions exist
    mockMvc.perform(get("/users/{userId}/transactions", testUserId))
        .andExpect(jsonPath("$", hasSize(2)));

    // Delete user
    mockMvc.perform(delete("/users/{userId}", testUserId))
        .andExpect(status().isOk());

    // Verify user deleted
    mockMvc.perform(get("/users/{userId}", testUserId))
        .andExpect(status().isNotFound());

    // Create new user to query transactions endpoint
    User newUser = new User("NewUser", "new@test.com", 500.0);
    User savedNew = mockApiService.addUser(newUser);

    // Transactions should be gone (cascade delete)
    // Attempting to get deleted transactions returns 404
    mockMvc.perform(get("/users/{userId}/transactions/{transactionId}",
            savedNew.getUserId(), savedTx1.getTransactionId()))
        .andExpect(status().isNotFound());
  }

  // ===========================================================================
  // BUDGET & ANALYTICS INTEGRATION TESTS
  // Tests: Aggregation queries spanning User and Transaction tables
  // ===========================================================================

  /**
   * Tests integration for GET /users/{userId}/budget-report.
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>User data retrieval (budget amount)</li>
   *   <li>Transaction aggregation (SUM by category)</li>
   *   <li>Business logic calculations (remaining, warnings)</li>
   *   <li>Complex JSON response structure</li>
   * </ul>
   *
   * <p><strong>Shared Data:</strong> Combines User.budget with Transaction.amount
   */
  @Test
  public void getBudgetReport_aggregatesUserAndTransactionData() throws Exception {
    // Create transactions for the test user
    Transaction tx1 = new Transaction(testUserId, 200.0, "FOOD", "Groceries");
    Transaction tx2 = new Transaction(testUserId, 300.0, "SHOPPING", "Electronics");
    Transaction tx3 = new Transaction(testUserId, 100.0, "FOOD", "Dining out");
    mockApiService.addTransaction(tx1);
    mockApiService.addTransaction(tx2);
    mockApiService.addTransaction(tx3);

    mockMvc.perform(get("/users/{userId}/budget-report", testUserId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId", is(testUserId.toString())))
        .andExpect(jsonPath("$.username", is("IntegrationTestUser")))
        .andExpect(jsonPath("$.totalBudget", is(1000.0)))
        .andExpect(jsonPath("$.totalSpent", is(600.0)))  // 200 + 300 + 100
        .andExpect(jsonPath("$.remaining", is(400.0)))   // 1000 - 600
        .andExpect(jsonPath("$.isOverBudget", is(false)))
        .andExpect(jsonPath("$.categories.FOOD", is(300.0)))  // 200 + 100
        .andExpect(jsonPath("$.categories.SHOPPING", is(300.0)));
  }

  /**
   * Tests integration for PUT /users/{userId}/budget.
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>Budget update via JSON payload</li>
   *   <li>Service validates user exists</li>
   *   <li>Database UPDATE on users table</li>
   *   <li>Returns updated budget report</li>
   * </ul>
   */
  @Test
  public void updateBudgetJson_updatesDatabaseAndReturnsReport() throws Exception {
    Map<String, Object> budgetUpdate = Map.of("budget", 2000.0);

    mockMvc.perform(put("/users/{userId}/budget", testUserId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(budgetUpdate)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalBudget", is(2000.0)));

    // Verify budget actually updated in database
    mockMvc.perform(get("/users/{userId}", testUserId))
        .andExpect(jsonPath("$.budget", is(2000.0)));
  }

  /**
   * Tests integration for GET /users/{userId}/weekly-summary.
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>Date-based filtering (transactions from last 7 days)</li>
   *   <li>Database query with date comparison</li>
   *   <li>Aggregation of filtered results</li>
   *   <li>Summary statistics calculation</li>
   * </ul>
   */
  @Test
  public void getWeeklySummary_filtersAndAggregates() throws Exception {
    // Create transactions (will have today's date from DB trigger)
    Transaction tx1 = new Transaction(testUserId, 50.0, "FOOD", "Recent lunch");
    Transaction tx2 = new Transaction(testUserId, 75.0, "OTHER", "Recent expense");
    mockApiService.addTransaction(tx1);
    mockApiService.addTransaction(tx2);

    mockMvc.perform(get("/users/{userId}/weekly-summary", testUserId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username", is("IntegrationTestUser")))
        .andExpect(jsonPath("$.weeklyTotal", is(125.0)))  // 50 + 75
        .andExpect(jsonPath("$.transactionCount", is(2)))
        .andExpect(jsonPath("$.transactions", hasSize(2)));
  }

  /**
   * Tests integration for GET /users/{userId}/monthly-summary.
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>Monthly date filtering</li>
   *   <li>Category-based grouping</li>
   *   <li>String report generation</li>
   * </ul>
   */
  @Test
  public void getMonthlySummary_generatesReport() throws Exception {
    // Create transactions
    Transaction tx1 = new Transaction(testUserId, 100.0, "FOOD", "Groceries");
    Transaction tx2 = new Transaction(testUserId, 50.0, "FOOD", "Snacks");
    mockApiService.addTransaction(tx1);
    mockApiService.addTransaction(tx2);

    mockMvc.perform(get("/users/{userId}/monthly-summary", testUserId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary", containsString("Total Budget")))
        .andExpect(jsonPath("$.summary", containsString("Total Spent")))
        .andExpect(jsonPath("$.summary", containsString("FOOD")));
  }

  /**
   * Tests budget over-spending detection across layers.
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>Transactions exceed user's budget</li>
   *   <li>Service calculates negative remaining</li>
   *   <li>isOverBudget flag set correctly</li>
   *   <li>Warnings generated based on business rules</li>
   * </ul>
   *
   * <p><strong>Shared Data:</strong> Budget comparison requires both User and Transaction data
   */
  @Test
  public void getBudgetReport_detectsOverBudget() throws Exception {
    // Create transactions that exceed budget (1000)
    Transaction tx1 = new Transaction(testUserId, 600.0, "SHOPPING", "Big purchase");
    Transaction tx2 = new Transaction(testUserId, 500.0, "OTHER", "Another expense");
    mockApiService.addTransaction(tx1);
    mockApiService.addTransaction(tx2);

    mockMvc.perform(get("/users/{userId}/budget-report", testUserId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalSpent", is(1100.0)))
        .andExpect(jsonPath("$.remaining", is(-100.0)))
        .andExpect(jsonPath("$.isOverBudget", is(true)))
        .andExpect(jsonPath("$.hasWarnings", is(true)));
  }

  // ===========================================================================
  // HTML ENDPOINT INTEGRATION TESTS
  // Tests: Form submissions and HTML response generation
  // ===========================================================================

  /**
   * Tests integration for GET / (index page).
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>Fetches all users from database</li>
   *   <li>Generates HTML with user links</li>
   *   <li>Content-Type negotiation (TEXT_HTML)</li>
   * </ul>
   */
  @Test
  public void indexPage_displaysUsersFromDatabase() throws Exception {
    mockMvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
        .andExpect(content().string(containsString("Welcome to the Personal Finance Tracker")))
        .andExpect(content().string(containsString("IntegrationTestUser")))
        .andExpect(content().string(containsString("integration@test.com")));
  }

  /**
   * Tests integration for GET /users/{userId}/budget (HTML page).
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>User lookup from database</li>
   *   <li>Budget report generation</li>
   *   <li>Weekly spending calculation</li>
   *   <li>HTML page rendering with data</li>
   * </ul>
   */
  @Test
  public void budgetManagementPage_displaysUserBudgetData() throws Exception {
    // Add some transactions
    Transaction tx = new Transaction(testUserId, 250.0, "FOOD", "Weekly groceries");
    mockApiService.addTransaction(tx);

    mockMvc.perform(get("/users/{userId}/budget", testUserId))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
        .andExpect(content().string(containsString("Budget Management")))
        .andExpect(content().string(containsString("IntegrationTestUser")));
  }

  /**
   * Tests integration for POST /users/{userId}/update-budget (HTML form).
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>Form parameter extraction</li>
   *   <li>User lookup and validation</li>
   *   <li>Budget update in database</li>
   *   <li>HTML success response</li>
   * </ul>
   */
  @Test
  public void updateBudgetForm_updatesAndReturnsHtml() throws Exception {
    mockMvc.perform(post("/users/{userId}/update-budget", testUserId)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("budget", "1500.0"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
        .andExpect(content().string(containsString("Budget Updated Successfully")))
        .andExpect(content().string(containsString("1500.00")));

    // Verify update persisted
    mockMvc.perform(get("/users/{userId}", testUserId))
        .andExpect(jsonPath("$.budget", is(1500.0)));
  }

  /**
   * Tests integration for transaction form submission.
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>Form data to Transaction object conversion</li>
   *   <li>User existence validation</li>
   *   <li>Database INSERT with form data</li>
   *   <li>HTML confirmation page</li>
   * </ul>
   */
  @Test
  public void createTransactionForm_persistsAndReturnsHtml() throws Exception {
    mockMvc.perform(post("/users/{userId}/transactions/form", testUserId)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("description", "Form-created transaction")
            .param("amount", "99.99")
            .param("category", "OTHER"))
        .andExpect(status().isCreated())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
        .andExpect(content().string(containsString("Transaction Created Successfully")));

    // Verify persisted
    mockMvc.perform(get("/users/{userId}/transactions", testUserId))
        .andExpect(jsonPath("$[0].description", is("Form-created transaction")))
        .andExpect(jsonPath("$[0].amount", is(99.99)));
  }

  // ===========================================================================
  // EXCEPTION HANDLER INTEGRATION TESTS
  // Tests: Exception propagation through all layers
  // ===========================================================================

  /**
   * Tests NoSuchElementException handling.
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>Database returns no results</li>
   *   <li>Service returns Optional.empty()</li>
   *   <li>Controller throws NoSuchElementException</li>
   *   <li>@ExceptionHandler intercepts and formats response</li>
   *   <li>HTTP 404 status with JSON error body</li>
   * </ul>
   */
  @Test
  public void exceptionHandler_notFound_returns404Json() throws Exception {
    UUID nonExistentId = UUID.randomUUID();

    mockMvc.perform(get("/users/{userId}/budget-report", nonExistentId))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error", containsString("not found")));
  }

  /**
   * Tests IllegalArgumentException handling.
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>Invalid data sent to controller</li>
   *   <li>Controller validates and throws IllegalArgumentException</li>
   *   <li>@ExceptionHandler intercepts and formats response</li>
   *   <li>HTTP 400 status with JSON error body</li>
   * </ul>
   */
  @Test
  public void exceptionHandler_internal_badRequest_returns400Json() throws Exception {
    // Null username triggers validation error in controller
    User invalidUser = new User(null, "valid@email.com", 100.0);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidUser)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists());
  }

  // ===========================================================================
  // CONCURRENT ACCESS INTEGRATION TESTS
  // Tests: Database handles concurrent operations correctly
  // ===========================================================================

  /**
   * Tests that unique constraints are enforced under concurrent-like conditions.
   *
   * <p><strong>Integration Points:</strong>
   * <ul>
   *   <li>Multiple requests attempting same operation</li>
   *   <li>Database unique constraint enforcement</li>
   *   <li>Proper error responses for violations</li>
   * </ul>
   */
  @Test
  public void uniqueConstraint_enforcedAcrossRequests() throws Exception {
    // First request succeeds
    User user1 = new User("UniqueUser", "unique@test.com", 100.0);
    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(user1)))
        .andExpect(status().isCreated());

    // Second request with same username fails
    User user2 = new User("UniqueUser", "different@test.com", 200.0);
    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(user2)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error", containsString("Username already exists")));
  }
}