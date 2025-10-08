package service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import model.Ledger;

import java.io.IOException;
import java.io.InputStream;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

/**
 * Service that loads ledger transactions and provides summaries.
 */
@Service
public class MockApiService {
    /** The list of ledger transactions. */
    private List<Ledger> entries;
    /** Logger for logging messages. */
    private static final Logger LOGGER = Logger.getLogger(MockApiService.class.getName());
    /** ObjectMapper for JSON processing. */    
    private final ObjectMapper mapper;

    // Fixed categories + catch-all
    private static final List<String> CATEGORIES = List.of(
        "Health", "Dining", "Groceries", "Transportation", "Education",
        "Other", "Miscellaneous"
    );

    public MockApiService(ObjectMapper mapper) {
        // copy so our tweaks don’t affect global mapper, but either way works
        this.mapper = mapper.copy();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        loadLedger();
    }

    private void loadLedger() {
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
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
     * Includes total spend and category breakdown by percentage.
     */
    public String getMonthlySummary() {
        if (entries == null) entries = new ArrayList<>();

        final YearMonth now = YearMonth.now(ZoneId.systemDefault());

        // Filter to current month
        final List<Ledger> monthEntries = new ArrayList<>();
        for (Ledger e : entries) {
            if (e.getDate() != null && YearMonth.from(e.getDate()).equals(now)) {
                // treat negative amounts as refunds; only sum positive spend
                if (e.getAmount() > 0) {
                    monthEntries.add(e);
                }
            }
        }

        if (monthEntries.isEmpty()) {
            return String.format(Locale.US,
                "Monthly Summary for %s\nTotal: $0.00\nNo transactions for this month.",
                now);
        }

        double total = 0.0;
        for (Ledger e : monthEntries) total += e.getAmount();

        // Initialize category totals with fixed keys
        Map<String, Double> catTotals = new LinkedHashMap<>();
        for (String c : CATEGORIES) catTotals.put(c, 0.0);

        for (Ledger e : monthEntries) {
            String cat = normalizeCategory(e.getCategory());
            catTotals.put(cat, catTotals.getOrDefault(cat, 0.0) + e.getAmount());
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US,
            "Monthly Summary for %s\n", now));
        sb.append(String.format(Locale.US,
            "Total: $%.2f\n", total));
        sb.append("Breakdown by category (percent of total):\n");

        for (Map.Entry<String, Double> en : catTotals.entrySet()) {
            double pct = total > 0.0 ? (en.getValue() * 100.0 / total) : 0.0;
            if (en.getValue() > 0.0) {
                sb.append(String.format(Locale.US,
                    "- %s: %.2f%% ($%.2f)\n", en.getKey(), pct, en.getValue()));
            }
        }
        return sb.toString();
    }

    private String normalizeCategory(String raw) {
        if (raw == null || raw.isBlank()) return "Miscellaneous";
        String r = raw.trim().toLowerCase(Locale.ROOT);
        if (r.startsWith("health") || r.contains("pharmacy") || r.contains("clinic")) return "Health";
        if (r.startsWith("dining") || r.contains("restaurant") || r.contains("cafe") || r.contains("coffee")) return "Dining";
        if (r.startsWith("grocer") || r.contains("supermarket") || r.contains("market")) return "Groceries";
        if (r.startsWith("transport") || r.contains("uber") || r.contains("lyft") || r.contains("metro") || r.contains("subway") || r.contains("bus")) return "Transportation";
        if (r.startsWith("educat") || r.contains("tuition") || r.contains("books")) return "Education";
        if (r.startsWith("other")) return "Other";
        return "Miscellaneous";
    }
}