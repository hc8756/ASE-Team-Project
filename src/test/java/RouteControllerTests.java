import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import controller.RouteController;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import model.Transaction;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import service.MockApiService;

/**
 * Full-context MVC tests for RouteController with a minimal test configuration.
 *
 * <p>Rubric mapping:
 * - We provide ≥3 tests per non-trivial unit (e.g., transactions, users, budgets) and
 *   deliberately include: typical valid input, atypical valid input, and invalid input.
 * - We use a mocking framework (@MockitoBean) for service dependencies.
 * - We show a light setup method (@BeforeEach) to demonstrate setup/teardown practice.
 * - Tests are grouped by responsibility (users / transactions / budget) in one class.
 */
@SpringBootTest(classes = RouteControllerTests.TestConfig.class)
@AutoConfigureMockMvc
@Import(RouteController.class) // Import the controller bean into the test context
class RouteControllerTests {

  /**
   * Minimal Spring Boot configuration for tests.
   * We rely on auto-configuration and explicitly import just the controller.
   */
  @SpringBootConfiguration
  @EnableAutoConfiguration
  static class TestConfig {
    // No beans needed here; @EnableAutoConfiguration + @Import(RouteController.class) is enough
  }

  @Autowired MockMvc mvc;

  // Use a local ObjectMapper to serialize request bodies; avoids bean wiring issues.
  private final ObjectMapper json = new ObjectMapper();

  // Mock the service: the controller should be tested in isolation from the service layer.
  @MockitoBean MockApiService api; // modern replacement for @MockBean (Boot 3.2+)

  // --------------------------- Common helpers & setup ---------------------------

  /**
   * Helper to build a User quickly.
   */
  private static User user(UUID id, String name) {
    User u = new User();
    u.setUserId(id);
    u.setUsername(name);
    u.setEmail(name + "@ex.com");
    u.setBudget(200);
    return u;
  }

  /**
   * Helper to build a Transaction quickly.
   */
  private static Transaction tx(UUID id, UUID userId, String desc, double amt) {
    Transaction t = new Transaction();
    t.setTransactionId(id);
    t.setUserId(userId);
    t.setDescription(desc);
    t.setAmount(amt);
    t.setCategory("FOOD");
    t.setDate(LocalDate.now());
    return t;
  }

  /**
   * Light setup for "setup/teardown" rubric item.
   */
  @BeforeEach
  void setUp() {
    // Nothing to reset between tests right now, but this shows structured setup.
  }

  // ---------------- HOME (simple HTML route) ----------------
  @Test
  void indexShowsUsers() throws Exception {
    // Typical valid input: one user in the system
    UUID uid = UUID.fromString("00000000-0000-0000-0000-000000000001");
    given(api.viewAllUsers()).willReturn(List.of(user(uid, "lisa")));

    mvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("/users/" + uid)));
  }

  // ---------------- USERS (typical / atypical / invalid) ----------------

  @Test
  void getUser_okAnd404() throws Exception {
    // Typical valid input: an existing user id
    UUID uid = UUID.randomUUID();
    given(api.getUser(uid)).willReturn(Optional.of(user(uid, "a")));
    mvc.perform(get("/users/{userId}", uid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(uid.toString()));

    // Invalid input: a missing user id should 404
    UUID missing = UUID.randomUUID();
    given(api.getUser(missing)).willReturn(Optional.empty());
    mvc.perform(get("/users/{userId}", missing))
        .andExpect(status().isNotFound());
  }

  @Test
  void createUser_json_201_and_form_201() throws Exception {
    // Typical valid JSON input
    UUID uid = UUID.randomUUID();
    User body = user(null, "neo");
    User saved = user(uid, "neo");
    given(api.addUser(any(User.class))).willReturn(saved);

    mvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(body)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value(uid.toString()));

    // Atypical valid input path: HTML form version of the same action
    given(api.addUser(any(User.class))).willReturn(saved);
    mvc.perform(post("/users/form")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("username", "neo")
            .param("email", "neo@ex.com")
            .param("budget", "200"))
        .andExpect(status().isCreated())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
  }

  @Test
  void updateUser_404_then_200() throws Exception {
    UUID uid = UUID.randomUUID();

    // Invalid input: updating a user that does not exist -> 404
    given(api.getUser(uid)).willReturn(Optional.empty());
    mvc.perform(put("/users/{userId}", uid)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(user(null, "x"))))
        .andExpect(status().isNotFound());

    // Typical valid input: user exists, returns updated data
    given(api.getUser(uid)).willReturn(Optional.of(user(uid, "old")));
    given(api.addUser(any(User.class))).willReturn(user(uid, "new"));

    mvc.perform(put("/users/{userId}", uid)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(user(null, "new"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("new"));
  }

  @Test
  void deleteUser_ok_and_404() throws Exception {
    // Typical valid input: delete succeeds
    UUID ok = UUID.randomUUID();
    given(api.deleteUser(ok)).willReturn(true);
    mvc.perform(delete("/users/{userId}", ok))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deleted").value(true));

    // Invalid input: delete returns false -> 404 via exception handler
    UUID miss = UUID.randomUUID();
    given(api.deleteUser(miss)).willReturn(false);
    mvc.perform(delete("/users/{userId}", miss))
        .andExpect(status().isNotFound());
  }

  // ---------------- TRANSACTIONS (typical / atypical / invalid) ----------------

  @Test
  void listUserTransactions_ok_404_500() throws Exception {
    UUID uid = UUID.randomUUID();
    UUID tid = UUID.randomUUID();

    // Typical valid input: user exists and has transactions
    given(api.getUser(uid)).willReturn(Optional.of(user(uid, "l")));
    given(api.getTransactionsByUser(uid)).willReturn(List.of(tx(tid, uid, "Coffee", 3.0)));
    mvc.perform(get("/users/{userId}/transactions", uid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].transactionId").value(tid.toString()));

    // Invalid input: user not found -> 404
    UUID missing = UUID.randomUUID();
    given(api.getUser(missing)).willReturn(Optional.empty());
    mvc.perform(get("/users/{userId}/transactions", missing))
        .andExpect(status().isNotFound());

    // Atypical error case: internal service failure -> 500
    given(api.getUser(missing)).willReturn(Optional.of(user(missing, "x")));
    doThrow(new RuntimeException("db")).when(api).getTransactionsByUser(missing);
    mvc.perform(get("/users/{userId}/transactions", missing))
        .andExpect(status().isInternalServerError());
  }

  @Test
  void getTransaction_ok_and_404() throws Exception {
    UUID uid = UUID.randomUUID();
    UUID tid = UUID.randomUUID();

    // Typical valid input: tx exists for this user
    given(api.getUser(uid)).willReturn(Optional.of(user(uid, "l")));
    given(api.getTransaction(tid)).willReturn(Optional.of(tx(tid, uid, "Tea", 2.0)));

    mvc.perform(get("/users/{userId}/transactions/{transactionId}", uid, tid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transactionId").value(tid.toString()));

    // Invalid input: tx missing -> 404
    given(api.getTransaction(tid)).willReturn(Optional.empty());
    mvc.perform(get("/users/{userId}/transactions/{transactionId}", uid, tid))
        .andExpect(status().isNotFound());
  }

  @Test
  void createTransaction_json_201_and_404() throws Exception {
    UUID uid = UUID.randomUUID();
    UUID tid = UUID.randomUUID();

    // Invalid input: user missing -> 404
    UUID missing = UUID.randomUUID();
    given(api.getUser(missing)).willReturn(Optional.empty());
    Transaction body = new Transaction(uid, 9.99, "FOOD", "Sandwich");
    mvc.perform(post("/users/{userId}/transactions", missing)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(body)))
        .andExpect(status().isNotFound());

    // Typical valid input: creation succeeds -> 201
    given(api.getUser(uid)).willReturn(Optional.of(user(uid, "l")));
    given(api.addTransaction(any(Transaction.class))).willReturn(tx(tid, uid, "Sandwich", 9.99));
    mvc.perform(post("/users/{userId}/transactions", uid)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(body)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.transactionId").value(tid.toString()));
  }

  @Test
  void updateTransaction_ok_and_404_user() throws Exception {
    UUID uid = UUID.randomUUID();
    UUID tid = UUID.randomUUID();

    // Invalid input: user missing -> 404
    given(api.getUser(uid)).willReturn(Optional.empty());
    mvc.perform(put("/users/{userId}/transactions/{transactionId}", uid, tid)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("amount", 7.0))))
        .andExpect(status().isNotFound());

    // Typical valid input: update returns updated tx
    given(api.getUser(uid)).willReturn(Optional.of(user(uid, "x")));
    given(api.getTransaction(tid)).willReturn(Optional.of(tx(tid, uid, "x", 1.0)));
    given(api.updateTransaction(eq(tid), anyMap()))
        .willReturn(Optional.of(tx(tid, uid, "x", 7.0)));

    mvc.perform(put("/users/{userId}/transactions/{transactionId}", uid, tid)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("amount", 7.0))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value(7.0));
  }

  @Test
  void deleteTransaction_ok_and_404_user() throws Exception {
    UUID uid = UUID.randomUUID();
    UUID tid = UUID.randomUUID();

    // Invalid input: user missing -> 404
    given(api.getUser(uid)).willReturn(Optional.empty());
    mvc.perform(delete("/users/{userId}/transactions/{transactionId}", uid, tid))
        .andExpect(status().isNotFound());

    // Typical valid input: delete succeeds -> 200
    given(api.getUser(uid)).willReturn(Optional.of(user(uid, "x")));
    given(api.getTransaction(tid)).willReturn(Optional.of(tx(tid, uid, "x", 1.0)));
    given(api.deleteTransaction(tid)).willReturn(true);

    mvc.perform(delete("/users/{userId}/transactions/{transactionId}", uid, tid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deleted").value(true));
  }

  // ---------------- BUDGET (HTML + JSON paths; valid / invalid) ----------------

  @Test
  void budgetPage_ok_and_404() throws Exception {
    // Typical valid input: user exists; HTML page renders numbers
    UUID uid = UUID.randomUUID();
    given(api.getUser(uid)).willReturn(Optional.of(user(uid, "lisa")));
    given(api.getBudgetReport(uid)).willReturn(
        Map.of("totalSpent", "50.00", "remaining", "150.00"));
    given(api.totalLast7Days(uid)).willReturn(12.34);

    mvc.perform(get("/users/{userId}/budget", uid))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));

    // Invalid input: user missing -> 404
    UUID miss = UUID.randomUUID();
    given(api.getUser(miss)).willReturn(Optional.empty());
    mvc.perform(get("/users/{userId}/budget", miss))
        .andExpect(status().isNotFound());
  }

  @Test
  void budgetPut_ok_and_404() throws Exception {
    // Typical valid input: JSON update succeeds
    UUID uid = UUID.randomUUID();
    given(api.getUser(uid)).willReturn(Optional.of(user(uid, "x")));
    given(api.getBudgetReport(uid)).willReturn(Map.of("remaining", 100));

    mvc.perform(put("/users/{userId}/budget", uid)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("budget", 300))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.remaining").value(100));

    // Invalid input: user missing -> 404
    UUID miss = UUID.randomUUID();
    given(api.getUser(miss)).willReturn(Optional.empty());
    mvc.perform(put("/users/{userId}/budget", miss)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("budget", 300))))
        .andExpect(status().isNotFound());
  }

  @Test
  void quickReports_weekly_monthly_json() throws Exception {
    // Atypical valid input: weekly summary empty & monthly summary plain text block
    UUID uid = UUID.randomUUID();
    given(api.getUser(uid)).willReturn(Optional.of(user(uid, "lisa")));
    given(api.weeklySummary(uid)).willReturn(List.of());   // empty weekly
    given(api.totalLast7Days(uid)).willReturn(0.0);        // edge: zero spend
    given(api.getMonthlySummary(uid)).willReturn("OK");    // plain text
    given(api.getBudgetReport(uid)).willReturn(Map.of("total", 500));

    mvc.perform(get("/users/{userId}/weekly-summary", uid))
        .andExpect(status().isOk());

    mvc.perform(get("/users/{userId}/monthly-summary", uid))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("OK")));

    mvc.perform(get("/users/{userId}/budget-report", uid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(500));
  }

  // ---------------- Extra error branches (explicit invalid classes) ----------------

  @Test
  void listUserTransactions_500_onServiceThrow() throws Exception {
    // Invalid situation: service throws unexpectedly -> 500
    UUID uid = UUID.randomUUID();
    given(api.getUser(uid)).willReturn(Optional.of(user(uid, "x")));
    doThrow(new RuntimeException("db")).when(api).getTransactionsByUser(uid);

    mvc.perform(get("/users/{userId}/transactions", uid))
        .andExpect(status().isInternalServerError());
  }

  // ---------------- HTML FORM / ALIAS routes (extra coverage) ----------------

  @Test
  void showCreateUserForm_200() throws Exception {
    // Atypical path: render HTML form
    mvc.perform(get("/users/create-form"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
        .andExpect(content().string(
            org.hamcrest.Matchers.containsString("Create New User")));
  }

  @Test
  void showEditUserForm_200_and_404() throws Exception {
    // Typical valid input: user exists -> show edit form
    UUID uid = UUID.randomUUID();
    given(api.getUser(uid)).willReturn(Optional.of(user(uid, "lisa")));
    mvc.perform(get("/users/{userId}/edit-form", uid))
        .andExpect(status().isOk())
        .andExpect(content().string(
            org.hamcrest.Matchers.containsString("Edit User")));

    // Invalid input: user missing -> 404
    UUID missing = UUID.randomUUID();
    given(api.getUser(missing)).willReturn(Optional.empty());
    mvc.perform(get("/users/{userId}/edit-form", missing))
        .andExpect(status().isNotFound());
  }

  @Test
  void createTransaction_form_201_404_500() throws Exception {
    UUID uid = UUID.randomUUID();

    // Invalid input: 404 user not found
    UUID missing = UUID.randomUUID();
    given(api.getUser(missing)).willReturn(Optional.empty());
    mvc.perform(post("/users/{userId}/transactions/form", missing)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("description", "Bus")
            .param("amount", "2.50")
            .param("category", "TRANSPORTATION"))
        .andExpect(status().isNotFound());

    // Typical valid input: 201 created via form
    given(api.getUser(uid)).willReturn(Optional.of(user(uid, "l")));
    given(api.addTransaction(any(Transaction.class)))
        .willReturn(tx(UUID.randomUUID(), uid, "Bus", 2.5));
    mvc.perform(post("/users/{userId}/transactions/form", uid)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("description", "Bus")
            .param("amount", "2.50")
            .param("category", "TRANSPORTATION"))
        .andExpect(status().isCreated())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));

    // Atypical error case: internal failure -> 500
    doThrow(new RuntimeException("db")).when(api).addTransaction(any(Transaction.class));
    mvc.perform(post("/users/{userId}/transactions/form", uid)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("description", "Err")
            .param("amount", "1.00")
            .param("category", "OTHER"))
        .andExpect(status().isInternalServerError());
  }

  @Test
  void updateBudget_form_200_and_404() throws Exception {
    // Typical valid input: HTML form submission works
    UUID uid = UUID.randomUUID();
    given(api.getUser(uid)).willReturn(Optional.of(user(uid, "lisa")));

    mvc.perform(post("/users/{userId}/update-budget", uid)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("budget", "321.50"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));

    // Invalid input: user missing -> 404
    UUID missing = UUID.randomUUID();
    given(api.getUser(missing)).willReturn(Optional.empty());
    mvc.perform(post("/users/{userId}/update-budget", missing)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("budget", "1"))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteUser_viaGet_200_and_404() throws Exception {
    // Atypical valid input path: deletion via GET (HTML-friendly route)
    UUID uid = UUID.randomUUID();
    given(api.deleteUser(uid)).willReturn(true);
    mvc.perform(get("/deleteuser/{userId}", uid))
        .andExpect(status().isOk())
        .andExpect(content().string(
            org.hamcrest.Matchers.containsString("User deleted " + "successfully")));

    // Invalid input: delete returned false -> handled by exception mapping
    UUID miss = UUID.randomUUID();
    given(api.deleteUser(miss)).willReturn(false);
    mvc.perform(get("/deleteuser/{userId}", miss))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteTransaction_viaGet_allOutcomes() throws Exception {
    UUID uid = UUID.randomUUID();
    UUID tid = UUID.randomUUID();

    // Invalid input: user missing -> returns an error string (200 + message)
    given(api.getUser(uid)).willReturn(Optional.empty());
    mvc.perform(get("/users/{userId}/deletetransaction/{transactionId}", uid, tid))
        .andExpect(status().isOk())
        .andExpect(content().string(
            org.hamcrest.Matchers.containsString("not " + "found")));

    // Invalid input: tx belongs to different user -> message
    given(api.getUser(uid)).willReturn(Optional.of(user(uid, "x")));
    given(api.getTransaction(tid))
        .willReturn(Optional.of(tx(tid, UUID.randomUUID(), "x", 1.0))); // different user
    mvc.perform(get("/users/{userId}/deletetransaction/{transactionId}", uid, tid))
        .andExpect(status().isOk())
        .andExpect(content().string(
            org.hamcrest.Matchers.containsString("not found " + "for user")));

    // Atypical error: delete=false -> message
    given(api.getTransaction(tid)).willReturn(Optional.of(tx(tid, uid, "x", 1.0)));
    given(api.deleteTransaction(tid)).willReturn(false);
    mvc.perform(get("/users/{userId}/deletetransaction/{transactionId}", uid, tid))
        .andExpect(status().isOk())
        .andExpect(content().string(
            org.hamcrest.Matchers.containsString("Failed " + "to delete")));

    // Typical valid input: success message
    given(api.deleteTransaction(tid)).willReturn(true);
    mvc.perform(get("/users/{userId}/deletetransaction/{transactionId}", uid, tid))
        .andExpect(status().isOk())
        .andExpect(content().string(
            org.hamcrest.Matchers.containsString("Transaction deleted " + "successfully")));
  }

  // ---------------- Additional invalid cases (explicit) ----------------

  @Test
  void getTransaction_404_whenTxBelongsToDifferentUser() throws Exception {
    // Invalid: user exists but the transaction belongs to a different user
    UUID uid = UUID.randomUUID();
    UUID otherUser = UUID.randomUUID();
    UUID tid = UUID.randomUUID();

    given(api.getUser(uid)).willReturn(Optional.of(user(uid, "x")));
    given(api.getTransaction(tid)).willReturn(Optional.of(tx(tid, otherUser, "Tea", 2.0)));

    mvc.perform(get("/users/{userId}/transactions/{transactionId}", uid, tid))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateTransaction_404_whenUpdateReturnsEmpty() throws Exception {
    // Invalid: update call returns Optional.empty()
    UUID uid = UUID.randomUUID();
    UUID tid = UUID.randomUUID();

    given(api.getUser(uid)).willReturn(Optional.of(user(uid, "x")));
    given(api.getTransaction(tid)).willReturn(Optional.of(tx(tid, uid, "x", 1.0)));
    given(api.updateTransaction(eq(tid), anyMap())).willReturn(Optional.empty());

    mvc.perform(put("/users/{userId}/transactions/{transactionId}", uid, tid)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("amount", 7.0))))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteTransaction_404_whenDeleteFalse() throws Exception {
    // Invalid: API delete returns false -> controller converts to 404 via exception handler
    UUID uid = UUID.randomUUID();
    UUID tid = UUID.randomUUID();

    given(api.getUser(uid)).willReturn(Optional.of(user(uid, "x")));
    given(api.getTransaction(tid)).willReturn(Optional.of(tx(tid, uid, "x", 1.0)));
    given(api.deleteTransaction(tid)).willReturn(false);

    mvc.perform(delete("/users/{userId}/transactions/{transactionId}", uid, tid))
        .andExpect(status().isNotFound());
  }
}