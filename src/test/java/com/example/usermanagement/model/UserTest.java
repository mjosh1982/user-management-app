package com.example.usermanagement.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void allArgsConstructorPopulatesEveryField() {
        User user = new User(1L, "Alice", "Smith", 30, Gender.FEMALE);

        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getFirstName()).isEqualTo("Alice");
        assertThat(user.getLastName()).isEqualTo("Smith");
        assertThat(user.getAge()).isEqualTo(30);
        assertThat(user.getGender()).isEqualTo(Gender.FEMALE);
    }

    @Test
    void noArgsConstructorLeavesDefaults() {
        User user = new User();

        assertThat(user.getId()).isNull();
        assertThat(user.getFirstName()).isNull();
        assertThat(user.getLastName()).isNull();
        assertThat(user.getAge()).isZero();
        assertThat(user.getGender()).isNull();
    }

    @Test
    void settersUpdateFields() {
        User user = new User();
        user.setId(5L);
        user.setFirstName("Bob");
        user.setLastName("Jones");
        user.setAge(41);
        user.setGender(Gender.MALE);

        assertThat(user).isEqualTo(new User(5L, "Bob", "Jones", 41, Gender.MALE));
    }

    @Test
    void equalsAndHashCodeUseAllFields() {
        User a = new User(1L, "Alice", "Smith", 30, Gender.FEMALE);
        User b = new User(1L, "Alice", "Smith", 30, Gender.FEMALE);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(new User(2L, "Alice", "Smith", 30, Gender.FEMALE));
        assertThat(a).isNotEqualTo(new User(1L, "Alicia", "Smith", 30, Gender.FEMALE));
        assertThat(a).isNotEqualTo(new User(1L, "Alice", "Smyth", 30, Gender.FEMALE));
        assertThat(a).isNotEqualTo(new User(1L, "Alice", "Smith", 31, Gender.FEMALE));
        assertThat(a).isNotEqualTo(new User(1L, "Alice", "Smith", 30, Gender.OTHER));
        assertThat(a).isNotEqualTo(null);
    }

    @Test
    void toStringContainsFieldValues() {
        String text = new User(1L, "Alice", "Smith", 30, Gender.FEMALE).toString();

        assertThat(text).contains("id=1", "firstName=Alice", "lastName=Smith", "age=30", "gender=FEMALE");
    }
}
