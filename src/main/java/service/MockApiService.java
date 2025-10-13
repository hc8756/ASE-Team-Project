package service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import model.Ledger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service that loads ledger transactions, provides summaries,
 * budget reporting, update operations, and basic CRUD/weekly APIs.
 */
@Service
public class MockApiService {

  // ===== In-memory store & id sequence =====
  private final Map<Long, Ledger> store = new ConcurrentHashMap<>();
  private final AtomicLong seq = new AtomicLong(1);

  /** Tolerance for floating point comparisons. */
  private static final double EPS = 0.005;

  /** The list of ledger transactions (kept in sync with store). */
  private List<Ledger> entries = new ArrayList<>();

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
   */
  public MockApiService(final ObjectMapper mapper) {
    this.mapper = mapper.copy();
    this.mapper.registerModule(new JavaTimeModule());
    this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    loadLedger(); // seeds entries + store + seq

    budgets.put("Total", 500.0);
    budgets.put("Health", 50.0);
    budgets.put("Dining", 100.0);
    budgets.put("Groceries", 150.0);
    budgets.put("Transportation", 50.0);
    budgets.put("Education", 50.0);
    budgets.put("Other", 50.0);
    budgets.put("Miscellaneous", 50.0);
  }

  // ===========================
  // ==  CRUD & WEEKLY (NEW)  ==
  // ===========================

  /** Create a ledger entry (assigns id, normalizes fields). */
  public Ledger add(final Ledger incoming) {
    Objects.requireNonNull(incoming, "Ledger cannot be null");

    long id = seq.getAndIncrement();
    String desc = Optional.ofNullable(incoming.getDescription()).orElse("").trim();
    String cat = normalizeCategory(incoming.getCategory());
    double amount = incoming.getAmount();
    LocalDate date = Optional.ofNullable(incoming.getDate()).orElse(LocalDate.now());

    // avoid relying on a specific constructor: set fields explicitly
    incoming.setLedgerId(id);
    incoming.setDescription(desc);
    incoming.setCategory(cat);
    incoming.setAmount(amount);
    incoming.setDate(date);

    store.put(id, incoming);

    // keep entries in sync (sorted oldest->newest)
    entries.add(incoming);
    entries.sort(Comparator.comparing(Ledger::getDate).thenComparing(Ledger::getLedgerId));
    return incoming;
  }

  public Optional<Ledger> get(final long id) {
    return Optional.ofNullable(store.get(id));
  }

  public boolean delete(final long id) {
    Ledger removed = store.remove(id);
    if (removed == null) return false;
    // remove from entries too
    entries.removeIf(e -> e.getLedgerId() == id);
    return true;
  }

  /** All entries sorted by date then id (defensive copy). */
  public List<Ledger> viewAll() {
    ArrayList<Ledger> list = new ArrayList<>(store.values());
    list.sort(Comparator.comparing(Ledger::getDate).thenComparing(Ledger::getLedgerId));
    return List.copyOf(list);
  }

  /** Entries with date >= (today - 7 days), inclusive; sorted oldest->newest. */
  public List<Ledger> weeklySummary() {
    LocalDate cutoff = LocalDate.now().minusDays(7);
    ArrayList<Ledger> list = new ArrayList<>();
    for (Ledger e : store.values()) {
      if (e.getDate() != null && !e.getDate().isBefore(cutoff)) list.add(e);
    }
    list.sort(Comparator.comparing(Ledger::getDate).thenComparing(Ledger::getLedgerId));
    return List.copyOf(list);
  }

  /** Sum of amounts for last 7 days. */
  public double totalLast7Days() {
    LocalDate cutoff = LocalDate.now().minusDays(7);
    return store.values().stream()
        .filter(e -> e.getDate() != null && !e.getDate().isBefore(cutoff))
        .mapToDouble(Ledger::getAmount)
        .sum();
  }

  /** Clear everything (useful for tests). */
  public void clearAll() {
    store.clear();
    entries = new ArrayList<>();
    seq.set(1);
  }

  // -------------------------------------------
  // -- Aliases expected by RouteController ----
  // -------------------------------------------

  public Ledger addTransaction(final Ledger tx) {
    return add(tx);
  }

  public Optional<Ledger> getTransaction(final long id) {
    return get(id);
  }

  public List<Ledger> viewAllTransactions() {
    return viewAll();
  }

  public boolean deleteTransaction(final long id) {
    return delete(id);
  }

  // ===========================
  // ==   Existing features   ==
  // ===========================

  /**
   * Loads ledger entries from {@code resources/mockdata/ledger.json}.
   * Also hydrates the in-memory store and id sequence.
   */
  private void loadLedger() {
    try (InputStream is =
        Thread.currentThread().getContextClassLoader()
              .getResourceAsStream("mockdata/ledger.json")) {

      if (is == null) {
        LOGGER.severe("Failed to find mockdata/ledger.json in resources.");
        entries = new ArrayList<>(0);
        store.clear();
        seq.set(1);
        return;
      }

      entries = this.mapper.readValue(is, new TypeReference<List<Ledger>>() {});
      LOGGER.info("Successfully loaded ledger entries.");

      // hydrate store and seq; normalize categories
      store.clear();
      long maxId = 0L;
      for (Ledger e : entries) {
        String cat = normalizeCategory(e.getCategory());
        e.setCategory(cat);
        long id = e.getLedgerId();
        // if input id is 0 or negative, assign a new increasing id
        if (id <= 0) {
          id = ++maxId;
          e.setLedgerId(id);
        } else {
          maxId = Math.max(maxId, id);
        }
        store.put(id, e);
      }
      seq.set(maxId + 1);

      // keep entries sorted
      entries.sort(Comparator.comparing(Ledger::getDate).thenComparing(Ledger::getLedgerId));

    } catch (IOException e) {
      entries = new ArrayList<>(0);
      store.clear();
      seq.set(1);
      if (LOGGER.isLoggable(Level.SEVERE)) {
        LOGGER.log(Level.SEVERE, "Failed to load ledger entries.", e);
      }
    }
  }

  /**
   * Builds a plain-text monthly summary for the current month.
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
   */
  public synchronized Optional<Ledger> updateTransaction(
      final long id, final Map<String, Object> patch) {

    if (entries == null || entries.isEmpty()) {
      return Optional.empty();
    }

    // Do not allow changing id or date
    if (patch.containsKey("id") || patch.containsKey("ledgerId") || patch.containsKey("date")) {
      throw new IllegalArgumentException("Fields id and date are immutable");
    }

    for (int i = 0; i < entries.size(); i++) {
      Ledger e = entries.get(i);
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

        // keep store + entries in sync
        entries.set(i, e);
        store.put(id, e);
        return Optional.of(e);
      }
    }
    return Optional.empty();
  }

  /** Budgets — getters & report helpers */

  public Map<String, Double> getBudgets() {
    return new LinkedHashMap<>(budgets);
  }

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
   * Updates budgets according to the provided map.
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
      if (k == null) continue;
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
      if (rawKey == null) continue;
      String key = rawKey.trim();

      if (key.equalsIgnoreCase("Total")) continue;

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
      if ("Total".equals(en.getKey())) continue;
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
