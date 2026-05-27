package com.healthcare.notification.repository;

import com.healthcare.notification.domain.NotificationJob;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryNotificationRepository implements NotificationRepository {
    private final Map<String, NotificationJob> store = new ConcurrentHashMap<>();

    @Override
    public NotificationJob save(NotificationJob aggregate) {
        store.put(aggregate.id(), aggregate);
        return aggregate;
    }

    @Override
    public Optional<NotificationJob> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<NotificationJob> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }
}
