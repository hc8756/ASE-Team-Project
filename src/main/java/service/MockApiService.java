package service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.Ledger;
import org.springframework.stereotype.Service;

/**
 * Service that loads ledger transactions, provides summaries,
 * budget reporting, and update operations.
 */
@Service
public class MockApiService {

  /** Tolerance for floating point comparisons. */
  private static final double EPS = 0.005;

  /** The list of ledger transactions. */
  private List<Ledger> entries;

  /** Logger for logging messages. */
  private static final Logger LOGGER = Logger.getLogger(MockApiService.class.getName());

  /** ObjectMapper for JSON processing. */
  private final ObjectMapper mapper;

  /** Budgets per category (includes the "Total" key). */
  private Map<String, Double> budgets = new LinkedHashMap<>();

  /** Fixed categories plus a catch-all. */
  private static final List<String> CATEGORIES =
      List.of("Health", "Dining", "Groceries", "Transportation", "Education", "Other",
          "Miscellaneous");

  /**
   * Constructs the service. Configures the mapper and loads data, then seeds budgets.
   *
   * @param mapper Spring-managed object mapper
   */
  public MockApiService(final ObjectMapper mapper) {
    this.mapper = mapper.copy();
    this.mapper.registerModule(new JavaTimeModule());
    this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    loadLedger();

    budgets.put("Total", 500.0);
    budgets.put("Health", 50.0);
    budgets.put("Dining", 100.0);
    budgets.put("Groceries", 150.0);
    budgets.put("Transportation", 50.0);
    budgets.put("Education", 50.0);
    budgets.put("Other", 50.0);
    budgets.put("Miscellaneous", 50.0);
  }

  /**
   * Loads ledger entries from {@code resources/mockdata/ledger.json}.
   */
  private void loadLedger() {
    try (InputStream is =
        Thread.currentThread().getContextClassLoader()
              .getResourceAsStream("mockdata/ledger.json")) {

      if (is == null) {
        LOGGER.severe("Failed to find mockdata/ledger.json in resources.");
        entries = new ArrayList<>(0);
        return;
      }

      entries = this.mapper.readValue(is, new TypeReference<List<Ledger>>() {});
      LOGGER.info("Successfully loaded ledger entries.");
    } catch (IOException e) {
      entries = new ArrayList<>(0);
      if (LOGGER.isLoggable(Level.SEVERE)) {
        LOGGER.log(Level.SEVERE, "Failed to load ledger entries.", e);
      }
    }
  }

  /**
   * Builds a plain-text monthly summary for the current month.
   * Includes total spend and category breakdown by percentage, plus budgets and warnings.
   *
   * @return summary text
   */
  public String getMonthlySummary() {
    if (entries == null) {
      entries = new ArrayList<>();
    }

    final YearMonth now = YearMonth.now(ZoneId.systemDefault());

    // Filter to current month, positive spend only
    final List<Ledger> monthEntries = new ArrayList<>();
    for (Ledger e : entries) {
      if (e.getDate() != null && YearMonth.from(e.getDate()).equals(now)) {
        if (e.getAmount() > 0) {
          monthEntries.add(e);
        }
      }
    }

    if (monthEntries.isEmpty()) {
      return String.format(Locale.US,
          "Monthly Summary for %s%nTotal: $0.00%nNo transactions for this month.", now);
    }

    double total = 0.0;
    for (Ledger e : monthEntries) {
      total += e.getAmount();
    }

    // Initialize category totals with fixed keys
    Map<String, Double> catTotals = new LinkedHashMap<>();
    for (String c : CATEGORIES) {
      catTotals.put(c, 0.0);
    }

    for (Ledger e : monthEntries) {
      String cat = normalizeCategory(e.getCategory());
      catTotals.put(cat, catTotals.getOrDefault(cat, 0.0) + e.getAmount());
    }

    StringBuilder sb = new StringBuilder();
    sb.append(String.format(Locale.US, "Monthly Summary for %s%n", now));
    sb.append(String.format(Locale.US, "Total: $%.2f%n", total));
    sb.append("Breakdown by category (percent of total):\n");

    for (Map.Entry<String, Double> en : catTotals.entrySet()) {
      double amount = en.getValue();
      if (amount > 0.0) {
        double pct = (total > 0.0) ? (amount * 100.0 / total) : 0.0;
        sb.append(
            String.format(Locale.US, "- %s: %.2f%% ($%.2f)%n", en.getKey(), pct, amount));
      }
    }

    sb.append(getBudgetsTextBlock());
    String warnings = getBudgetWarningsText();
    if (!warnings.isBlank()) {
      sb.append("\n").append(warnings);
    }
    return sb.toString();
  }

  /**
   * Normalizes various raw category labels into the fixed set used by the service.
   *
   * @param raw input category
   * @return normalized category
   */
  private String normalizeCategory(final String raw) {
    if (raw == null || raw.isBlank()) {
      return "Miscellaneous";
    }

    String r = raw.trim().toLowerCase(Locale.ROOT);

    if (r.startsWith("health") || r.contains("pharmacy") || r.contains("clinic")) {
      return "Health";
    }
    if (r.startsWith("dining") || r.contains("restaurant") || r.contains("cafe")
        || r.contains("coffee")) {
      return "Dining";
    }
    if (r.startsWith("grocer") || r.contains("supermarket") || r.contains("market")) {
      return "Groceries";
    }
    if (r.startsWith("transport") || r.contains("uber") || r.contains("lyft")
        || r.contains("metro") || r.contains("subway") || r.contains("bus")) {
      return "Transportation";
    }
    if (r.startsWith("educat") || r.contains("tuition") || r.contains("books")) {
      return "Education";
    }
    if (r.startsWith("other")) {
      return "Other";
    }
    return "Miscellaneous";
  }

  /**
   * Updates a transaction by id; only amount, category, and description are mutable.
   *
   * @param id transaction id
   * @param patch fields to update
   * @return the updated entry if found
   */
  public synchronized Optional<model.Ledger> updateTransaction(
      final long id, final Map<String, Object> patch) {

    if (entries == null || entries.isEmpty()) {
      return Optional.empty();
    }

    // Do not allow changing id or date
    if (patch.containsKey("id") || patch.containsKey("ledgerId") || patch.containsKey("date")) {
      throw new IllegalArgumentException("Fields id and date are immutable");
    }

    for (int i = 0; i < entries.size(); i++) {
      model.Ledger e = entries.get(i);
      if (e.getLedgerId() == id) {

        // amount (optional)
        if (patch.containsKey("amount") && patch.get("amount") != null) {
          Object v = patch.get("amount");
          double amt;
          if (v instanceof Number) {
            amt = ((Number) v).doubleValue();
          } else {
            try {
              amt = Double.parseDouble(v.toString());
            } catch (NumberFormatException nfe) {
              throw new IllegalArgumentException("amount must be numeric");
            }
          }
          e.setAmount(amt);
        }

        // category (optional)
        if (patch.containsKey("category") && patch.get("category") != null) {
          String cat = patch.get("category").toString();
          e.setCategory(normalizeCategory(cat));
        }

        // description (optional)
        if (patch.containsKey("description")) {
          Object d = patch.get("description");
          e.setDescription(d == null ? null : d.toString());
        }

        entries.set(i, e);
        return Optional.of(e);
      }
    }
    return Optional.empty();
  }

  /**
   * Returns a defensive copy of the current budgets map.
   *
   * @return budgets map
   */
  public Map<String, Double> getBudgets() {
    return new LinkedHashMap<>(budgets);
  }

  /**
   * Returns current month spend per category, including the "Total" key.
   *
   * @return spend per category
   */
  public Map<String, Double> getCurrentMonthSpendByCategory() {
    final YearMonth now = YearMonth.now(ZoneId.systemDefault());
    Map<String, Double> spend = new LinkedHashMap<>();

    // Initialize all budget keys to 0 for stable output
    for (String k : budgets.keySet()) {
      spend.put(k, 0.0);
    }

    double total = 0.0;
    if (entries != null) {
      for (Ledger e : entries) {
        boolean inMonth =
            e.getDate() != null && YearMonth.from(e.getDate()).equals(now);
        if (inMonth && e.getAmount() > 0) {
          String cat = normalizeCategory(e.getCategory());
          spend.put(cat, spend.getOrDefault(cat, 0.0) + e.getAmount());
          total += e.getAmount();
        }
      }
    }
    spend.put("Total", total);
    return spend;
  }

  /**
   * Builds a JSON-friendly budget report including budgets, spend, remaining,
   * and overBudget flags for each key.
   *
   * @return budget report map
   */
  public Map<String, Object> getBudgetReport() {
    Map<String, Double> b = getBudgets();
    Map<String, Double> s = getCurrentMonthSpendByCategory();

    Map<String, Object> perCat = new LinkedHashMap<>();
    for (String key : b.keySet()) {
      double budget = b.getOrDefault(key, 0.0);
      double spent = s.getOrDefault(key, 0.0);

      Map<String, Object> row = new LinkedHashMap<>();
      row.put("budget", budget);
      row.put("spent", spent);
      row.put("remaining", budget - spent);
      row.put("overBudget", spent > budget && budget > 0.0);
      perCat.put(key, row);
    }

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("period", YearMonth.now(ZoneId.systemDefault()).toString());
    out.put("summary", perCat);
    return out;
  }

  /**
   * Builds a text block describing budgets and current spend, for embedding in pages.
   *
   * @return multi-line budgets block
   */
  public String getBudgetsTextBlock() {
    Map<String, Double> b = getBudgets();
    Map<String, Double> s = getCurrentMonthSpendByCategory();

    StringBuilder sb = new StringBuilder();
    sb.append("\nBudgets (this month):\n");

    // Total first
    if (b.containsKey("Total")) {
      double budget = b.get("Total");
      double spent = s.getOrDefault("Total", 0.0);
      sb.append(
          String.format(Locale.US, "- Total: $%.2f (spent $%.2f, remaining $%.2f)%n",
              budget, spent, budget - spent));
    }

    // Then categories
    for (Map.Entry<String, Double> en : b.entrySet()) {
      String key = en.getKey();
      if ("Total".equals(key)) {
        continue;
      }
      double budget = en.getValue();
      double spent = s.getOrDefault(key, 0.0);
      sb.append(
          String.format(Locale.US, "- %s: $%.2f (spent $%.2f, remaining $%.2f)%n",
              key, budget, spent, budget - spent));
    }
    return sb.toString();
  }

  /**
   * Returns warning lines when total or any category exceeds its budget.
   *
   * @return warnings text (possibly empty)
   */
  public String getBudgetWarningsText() {
    Map<String, Double> b = getBudgets();
    Map<String, Double> s = getCurrentMonthSpendByCategory();

    StringBuilder warn = new StringBuilder();

    // total
    double totalBudget = b.getOrDefault("Total", 0.0);
    double totalSpent = s.getOrDefault("Total", 0.0);
    if (totalBudget > 0.0 && totalSpent > totalBudget) {
      warn.append(
          String.format(Locale.US,
              "WARNING: Total spend ($%.2f) exceeds total budget ($%.2f)%n",
              totalSpent, totalBudget));
    }

    // categories
    for (Map.Entry<String, Double> en : b.entrySet()) {
      String key = en.getKey();
      if ("Total".equals(key)) {
        continue;
      }
      double budget = en.getValue();
      double spent = s.getOrDefault(key, 0.0);
      if (budget > 0.0 && spent > budget) {
        warn.append(
            String.format(Locale.US,
                "WARNING: %s spend ($%.2f) exceeds budget ($%.2f)%n",
                key, spent, budget));
      }
    }
    return warn.toString();
  }

  /**
   * Updates budgets according to the provided map. You may add, update, or remove categories
   * (set to 0 to remove). Categories not provided remain unchanged. The sum of categories must
   * equal the provided "Total" value if included, or the existing total otherwise.
   *
   * @param updates map containing "Total" and/or category budgets
   */
  public synchronized void setBudgets(final Map<String, Object> updates) {
    if (updates == null || updates.isEmpty()) {
      throw new IllegalArgumentException("Empty budget update");
    }

    // Work on a copy
    Map<String, Double> newBudgets = new LinkedHashMap<>(budgets);

    // Detect if Total provided
    Double providedTotal = null;
    for (String k : updates.keySet()) {
      if (k == null) {
        continue;
      }
      String keyNorm = k.trim();
      if (keyNorm.equalsIgnoreCase("Total")) {
        Object v = updates.get(k);
        if (v == null) {
          throw new IllegalArgumentException("Total cannot be null");
        }
        providedTotal = parseDoubleStrict(v, "Total");
      }
    }

    // Apply category updates (add/update/delete). Skip Total here.
    for (Map.Entry<String, Object> e : updates.entrySet()) {
      String rawKey = e.getKey();
      if (rawKey == null) {
        continue;
      }
      String key = rawKey.trim();

      if (key.equalsIgnoreCase("Total")) {
        continue;
      }

      String cat = normalizeCategory(key);
      Object v = e.getValue();
      if (v == null) {
        throw new IllegalArgumentException("Budget cannot be null for " + cat);
      }

      double amount = parseDoubleStrict(v, cat);
      if (amount < 0) {
        throw new IllegalArgumentException("Budget cannot be negative for " + cat);
      }

      if (Math.abs(amount) < EPS) {
        // delete category when set to 0
        newBudgets.remove(cat);
      } else {
        newBudgets.put(cat, amount);
      }
    }

    // Compute sum of categories (exclude Total)
    double sumCategories = 0.0;
    for (Map.Entry<String, Double> en : newBudgets.entrySet()) {
      if ("Total".equals(en.getKey())) {
        continue;
      }
      sumCategories += en.getValue();
    }

    if (providedTotal != null) {
      // Caller provided Total. It MUST equal sum of categories.
      if (Math.abs(sumCategories - providedTotal) > EPS) {
        String msg = String.format(Locale.US,
            "Category budgets ($%.2f) must equal Total ($%.2f)",
            sumCategories, providedTotal);
        throw new IllegalArgumentException(msg);
      }
      newBudgets.put("Total", providedTotal);
    } else {
      // Caller did not provide Total. It must match existing Total.
      double currentTotal = budgets.getOrDefault("Total", 0.0);
      if (Math.abs(sumCategories - currentTotal) > EPS) {
        String msg = String.format(Locale.US,
            "Category budgets ($%.2f) do not equal existing Total ($%.2f). "
                + "Include \"Total\" in the update.",
            sumCategories, currentTotal);
        throw new IllegalArgumentException(msg);
      }
      newBudgets.put("Total", currentTotal);
    }

    budgets = newBudgets;
  }

  /**
   * Parses a value to double, throwing an {@link IllegalArgumentException} on failure.
   *
   * @param v     value to parse
   * @param field field name for error messages
   * @return parsed double
   */
  private static double parseDoubleStrict(final Object v, final String field) {
    if (v instanceof Number) {
      return ((Number) v).doubleValue();
    }
    try {
      return Double.parseDouble(v.toString());
    } catch (NumberFormatException nfe) {
      throw new IllegalArgumentException(field + " must be numeric");
    }
  }
}