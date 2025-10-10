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
        long id = seq.getAndIncrement();
        Transaction tx = new Transaction(
                id,
                Optional.ofNullable(incoming.getDescription()).orElse(""),
                incoming.getAmount(),
                LocalDateTime.now()
        );
        store.put(id, tx);
        return tx;
    }

    public boolean delete(long id) {
        return store.remove(id) != null;
    }

    public List<Transaction> viewAll() {
        ArrayList<Transaction> list = new ArrayList<>(store.values());
        list.sort(Comparator.comparing(Transaction::getTimestamp)); // oldest->newest
        return list;
    }

    public List<Transaction> weeklySummary() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        ArrayList<Transaction> list = new ArrayList<>();
        for (Transaction t : store.values()) {
            if (t.getTimestamp().isAfter(sevenDaysAgo)) list.add(t);
        }
        list.sort(Comparator.comparing(Transaction::getTimestamp));
        return list;
    }
}
