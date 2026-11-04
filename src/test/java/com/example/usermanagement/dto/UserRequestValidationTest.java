package com.example.usermanagement.dto;

import com.example.usermanagement.model.Gender;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class UserRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static UserRequest valid() {
        return new UserRequest("Alice", "Smith", 30, Gender.FEMALE);
    }

    private static Map<String, String> violations(UserRequest request) {
        Set<ConstraintViolation<UserRequest>> result = validator.validate(request);
        return result.stream().collect(Collectors.toMap(v -> v.getPropertyPath().toString(),
                ConstraintViolation::getMessage, (a, b) -> a + "; " + b));
    }

    @Test
    void validRequestHasNoViolations() {
        assertThat(violations(valid())).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void blankFirstNameIsRejected(String firstName) {
        UserRequest request = valid();
        request.setFirstName(firstName);

        assertThat(violations(request)).containsEntry("firstName", "firstName is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void blankLastNameIsRejected(String lastName) {
        UserRequest request = valid();
        request.setLastName(lastName);

        assertThat(violations(request)).containsEntry("lastName", "lastName is required");
    }

    @Test
    void namesUpTo50CharactersAreAccepted() {
        UserRequest request = valid();
        request.setFirstName("a".repeat(50));
        request.setLastName("b".repeat(50));

        assertThat(violations(request)).isEmpty();
    }

    @Test
    void namesLongerThan50CharactersAreRejected() {
        UserRequest request = valid();
        request.setFirstName("a".repeat(51));
        request.setLastName("b".repeat(51));

        assertThat(violations(request))
                .containsEntry("firstName", "firstName must be at most 50 characters")
                .containsEntry("lastName", "lastName must be at most 50 characters");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 75, 150})
    void agesWithinRangeAreAccepted(int age) {
        UserRequest request = valid();
        request.setAge(age);

        assertThat(violations(request)).isEmpty();
    }

    @Test
    void negativeAgeIsRejected() {
        UserRequest request = valid();
        request.setAge(-1);

        assertThat(violations(request)).containsEntry("age", "age must be >= 0");
    }

    @Test
    void ageAbove150IsRejected() {
        UserRequest request = valid();
        request.setAge(151);

        assertThat(violations(request)).containsEntry("age", "age must be <= 150");
    }

    @Test
    void missingGenderIsRejected() {
        UserRequest request = valid();
        request.setGender(null);

        assertThat(violations(request)).containsEntry("gender", "gender is required");
    }

    @Test
    void emptyRequestReportsEveryRequiredField() {
        assertThat(violations(new UserRequest()))
                .containsOnlyKeys("firstName", "lastName", "gender");
    }
}
