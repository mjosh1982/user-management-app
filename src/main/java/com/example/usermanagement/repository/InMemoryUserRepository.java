package com.example.usermanagement.repository;

import com.example.usermanagement.model.Gender;
import com.example.usermanagement.model.User;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, User> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong();

    public InMemoryUserRepository() {
        save(new User(null, "Alice", "Smith", 30, Gender.FEMALE));
        save(new User(null, "Bob", "Johnson", 42, Gender.MALE));
        save(new User(null, "Charlie", "Brown", 25, Gender.OTHER));
    }

    @Override
    public List<User> findAll() {
        return store.values().stream()
                .map(InMemoryUserRepository::copy)
                .sorted(Comparator.comparing(User::getId))
                .toList();
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(store.get(id)).map(InMemoryUserRepository::copy);
    }

    @Override
    public User save(User user) {
        User toStore = copy(user);
        if (toStore.getId() == null) {
            toStore.setId(idGenerator.incrementAndGet());
        }
        store.put(toStore.getId(), toStore);
        return copy(toStore);
    }

    @Override
    public boolean existsById(Long id) {
        return store.containsKey(id);
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }

    private static User copy(User u) {
        return new User(u.getId(), u.getFirstName(), u.getLastName(), u.getAge(), u.getGender());
    }
}
