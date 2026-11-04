package com.example.usermanagement.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenderTest {

    @Test
    void definesExactlyThreeValuesInOrder() {
        assertThat(Gender.values()).containsExactly(Gender.MALE, Gender.FEMALE, Gender.OTHER);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MALE", "FEMALE", "OTHER"})
    void valueOfResolvesKnownNames(String name) {
        assertThat(Gender.valueOf(name).name()).isEqualTo(name);
    }

    @ParameterizedTest
    @ValueSource(strings = {"male", "UNKNOWN", ""})
    void valueOfRejectsUnknownNames(String name) {
        assertThatThrownBy(() -> Gender.valueOf(name)).isInstanceOf(IllegalArgumentException.class);
    }
}
