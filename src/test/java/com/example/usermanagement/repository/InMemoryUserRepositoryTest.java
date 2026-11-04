package com.example.usermanagement.repository;

import com.example.usermanagement.model.Gender;
import com.example.usermanagement.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryUserRepositoryTest {

    private InMemoryUserRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepository();
    }

    @Test
    void seedsThreeSampleUsers() {
        assertThat(repository.findAll())
                .extracting(User::getId, User::getFirstName, User::getLastName, User::getAge, User::getGender)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, "Alice", "Smith", 30, Gender.FEMALE),
                        org.assertj.core.groups.Tuple.tuple(2L, "Bob", "Johnson", 42, Gender.MALE),
                        org.assertj.core.groups.Tuple.tuple(3L, "Charlie", "Brown", 25, Gender.OTHER));
    }

    @Test
    void findAllIsSortedById() {
        repository.save(new User(10L, "Zed", "Last", 50, Gender.MALE));
        repository.save(new User(null, "New", "User", 20, Gender.OTHER));

        assertThat(repository.findAll()).extracting(User::getId).isSorted();
    }

    @Test
    void findAllReturnsUnmodifiableList() {
        List<User> users = repository.findAll();

        assertThatThrownBy(() -> users.add(new User())).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void findByIdReturnsUserWhenPresent() {
        assertThat(repository.findById(2L)).hasValueSatisfying(u -> assertThat(u.getFirstName()).isEqualTo("Bob"));
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        assertThat(repository.findById(999L)).isEmpty();
    }

    @Test
    void findByNameIsCaseInsensitive() {
        assertThat(repository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase("aLiCe", "SMITH"))
                .hasValueSatisfying(u -> assertThat(u.getId()).isEqualTo(1L));
    }

    @Test
    void findByNameRequiresBothNamesToMatch() {
        assertThat(repository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase("Alice", "Johnson")).isEmpty();
        assertThat(repository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase("Nobody", "Smith")).isEmpty();
    }

    @Test
    void saveWithoutIdAssignsNextId() {
        User saved = repository.save(new User(null, "Eve", "Adams", 33, Gender.FEMALE));

        assertThat(saved.getId()).isEqualTo(4L);
        assertThat(repository.findById(4L)).contains(saved);
        assertThat(repository.findAll()).hasSize(4);
    }

    @Test
    void saveWithExistingIdReplacesUser() {
        User updated = repository.save(new User(1L, "Alice", "Walker", 31, Gender.FEMALE));

        assertThat(updated.getId()).isEqualTo(1L);
        assertThat(repository.findById(1L)).contains(updated);
        assertThat(repository.findAll()).hasSize(3);
    }

    @Test
    void saveDoesNotMutateTheArgument() {
        User input = new User(null, "Eve", "Adams", 33, Gender.FEMALE);

        repository.save(input);

        assertThat(input.getId()).isNull();
    }

    @Test
    void idsAreNotReusedAfterDelete() {
        repository.deleteById(3L);

        User saved = repository.save(new User(null, "Eve", "Adams", 33, Gender.FEMALE));

        assertThat(saved.getId()).isEqualTo(4L);
    }

    @Test
    void returnedUsersAreDefensiveCopies() {
        User fromFind = repository.findById(1L).orElseThrow();
        fromFind.setFirstName("Mutated");
        repository.findAll().get(0).setLastName("Mutated");

        User stored = repository.findById(1L).orElseThrow();
        assertThat(stored.getFirstName()).isEqualTo("Alice");
        assertThat(stored.getLastName()).isEqualTo("Smith");
    }

    @Test
    void mutatingSavedArgumentAfterSaveDoesNotAffectStore() {
        User input = new User(null, "Eve", "Adams", 33, Gender.FEMALE);
        User saved = repository.save(input);
        input.setFirstName("Mutated");
        saved.setFirstName("Mutated");

        assertThat(repository.findById(saved.getId()).orElseThrow().getFirstName()).isEqualTo("Eve");
    }

    @Test
    void existsByIdReflectsStoreContents() {
        assertThat(repository.existsById(1L)).isTrue();
        assertThat(repository.existsById(999L)).isFalse();
    }

    @Test
    void deleteByIdRemovesUser() {
        repository.deleteById(1L);

        assertThat(repository.existsById(1L)).isFalse();
        assertThat(repository.findAll()).extracting(User::getId).containsExactly(2L, 3L);
    }

    @Test
    void deleteByIdOfMissingUserIsNoOp() {
        repository.deleteById(999L);

        assertThat(repository.findAll()).hasSize(3);
    }

    @Test
    void concurrentSavesGetUniqueIds() throws InterruptedException {
        int threads = 8;
        int perThread = 250;
        Set<Long> ids = ConcurrentHashMap.newKeySet();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                start.await();
                for (int i = 0; i < perThread; i++) {
                    ids.add(repository.save(new User(null, "F", "L", 20, Gender.OTHER)).getId());
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(ids).hasSize(threads * perThread);
        assertThat(repository.findAll()).hasSize(3 + threads * perThread);
    }
}
