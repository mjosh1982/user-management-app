package com.example.usermanagement.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionTypesTest {

    @Test
    void userNotFoundExceptionCarriesIdCodeAndMessage() {
        UserNotFoundException ex = new UserNotFoundException(7L);

        assertThat(ex).isInstanceOf(ApiException.class).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("User with id 7 not found");
        assertThat(ex.getUserId()).isEqualTo(7L);
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void userAlreadyExistsExceptionCarriesCodeAndMessage() {
        UserAlreadyExistsException ex = new UserAlreadyExistsException("Alice", "Smith");

        assertThat(ex).isInstanceOf(ApiException.class);
        assertThat(ex.getMessage()).isEqualTo("User 'Alice Smith' already exists");
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_ALREADY_EXISTS);
        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void errorCodesMapToExpectedStatuses() {
        assertThat(ErrorCode.VALIDATION_FAILED.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ErrorCode.MALFORMED_REQUEST.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ErrorCode.INVALID_PARAMETER.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ErrorCode.RESOURCE_NOT_FOUND.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ErrorCode.METHOD_NOT_ALLOWED.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(ErrorCode.INTERNAL_ERROR.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void everyErrorCodeIsAnErrorStatus(ErrorCode code) {
        assertThat(code.getStatus().isError()).isTrue();
    }
}
