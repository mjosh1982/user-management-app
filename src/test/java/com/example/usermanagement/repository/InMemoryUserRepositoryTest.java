package com.example.usermanagement.repository;

import com.example.usermanagement.model.Gender;
import com.example.usermanagement.model.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryUserRepositoryTest {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();

    @Test
    void seedsSampleUsers() {
        assertThat(repository.findAll()).hasSize(3);
    }

    @Test
    void saveAssignsIncrementingIds() {
        User saved = repository.save(new User(null, "Eve", "Adams", 33, Gender.FEMALE));
        assertThat(saved.getId()).isEqualTo(4L);
        assertThat(repository.findById(4L)).contains(saved);
    }

    @Test
    void returnedUsersAreDefensiveCopies() {
        User user = repository.findById(1L).orElseThrow();
        user.setFirstName("Mutated");
        assertThat(repository.findById(1L).orElseThrow().getFirstName()).isEqualTo("Alice");
    }

    @Test
    void deleteRemovesUser() {
        repository.deleteById(1L);
        assertThat(repository.existsById(1L)).isFalse();
        assertThat(repository.findAll()).hasSize(2);
    }
}
