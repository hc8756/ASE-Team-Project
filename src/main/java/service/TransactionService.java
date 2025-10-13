package service;

import model.Transaction;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TransactionService {

    private final Map<Long, Transaction> store = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(1);

    public Transaction add(Transaction incoming) {
        Objects.requireNonNull(incoming, "Transaction cannot be null");

        String desc = Optional.ofNullable(incoming.getDescription()).orElse("").trim();
        double amount = incoming.getAmount();

        long id = seq.getAndIncrement();
        Transaction tx = new Transaction(
                id,
                desc,
                amount,
                LocalDateTime.now()
        );
        store.put(id, tx);
        return tx;
    }

    public Optional<Transaction> get(long id) {
        return Optional.ofNullable(store.get(id));
    }

    public boolean delete(long id) {
        return store.remove(id) != null;
    }

    public List<Transaction> viewAll() {
        List<Transaction> list = new ArrayList<>(store.values());
        list.sort(Comparator.comparing(Transaction::getTimestamp)); // oldest -> newest
        return List.copyOf(list);
    }

    public List<Transaction> weeklySummary() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Transaction> list = new ArrayList<>();
        for (Transaction t : store.values()) {
            if (!t.getTimestamp().isBefore(sevenDaysAgo)) {
                list.add(t);
            }
        }
        list.sort(Comparator.comparing(Transaction::getTimestamp));
        return List.copyOf(list);
    }

    public double totalLast7Days() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        return store.values().stream()
                .filter(t -> !t.getTimestamp().isBefore(cutoff))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public void clearAll() {
        store.clear();
        seq.set(1);
    }
}
