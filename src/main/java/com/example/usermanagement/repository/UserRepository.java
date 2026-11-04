package com.example.usermanagement.repository;

import com.example.usermanagement.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Storage abstraction for users. The default implementation is in-memory;
 * swap it for a JPA-backed implementation to persist to MySQL.
 * Method names follow Spring Data conventions so a {@code JpaRepository} can provide them.
 */
public interface UserRepository {

    List<User> findAll();

    Optional<User> findById(Long id);

    Optional<User> findByFirstNameIgnoreCaseAndLastNameIgnoreCase(String firstName, String lastName);

    User save(User user);

    boolean existsById(Long id);

    void deleteById(Long id);
}
