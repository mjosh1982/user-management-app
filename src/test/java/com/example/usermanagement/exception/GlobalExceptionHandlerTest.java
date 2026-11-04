package com.example.usermanagement.exception;

import com.example.usermanagement.controller.UserController;
import com.example.usermanagement.dto.UserRequest;
import com.example.usermanagement.model.Gender;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest("POST", "/api/users");
    }

    private static void assertBody(ResponseEntity<ErrorResponse> response, HttpStatus status, ErrorCode code) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(status.value());
        assertThat(body.error()).isEqualTo(status.getReasonPhrase());
        assertThat(body.code()).isEqualTo(code.name());
        assertThat(body.path()).isEqualTo("/api/users");
        assertThat(body.timestamp()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void apiExceptionUsesItsErrorCode() {
        ResponseEntity<ErrorResponse> response =
                handler.handleApiException(new UserAlreadyExistsException("A", "B"), request);

        assertBody(response, HttpStatus.CONFLICT, ErrorCode.USER_ALREADY_EXISTS);
        assertThat(response.getBody().message()).isEqualTo("User 'A B' already exists");
        assertThat(response.getBody().fieldErrors()).isNull();
    }

    @Test
    void userNotFoundMapsTo404() {
        ResponseEntity<ErrorResponse> response = handler.handleApiException(new UserNotFoundException(5L), request);

        assertBody(response, HttpStatus.NOT_FOUND, ErrorCode.USER_NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("User with id 5 not found");
    }

    @Test
    void bodyValidationCollectsFirstErrorPerFieldAndGlobalErrors() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new UserRequest(), "userRequest");
        bindingResult.addError(new FieldError("userRequest", "firstName", "firstName is required"));
        bindingResult.addError(new FieldError("userRequest", "firstName", "second message ignored"));
        bindingResult.addError(new FieldError("userRequest", "age", "age must be >= 0"));
        bindingResult.addError(new ObjectError("userRequest", "object-level problem"));
        MethodParameter parameter =
                new MethodParameter(UserController.class.getMethod("createUser", UserRequest.class), 0);

        ResponseEntity<ErrorResponse> response =
                handler.handleBodyValidation(new MethodArgumentNotValidException(parameter, bindingResult), request);

        assertBody(response, HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED);
        assertThat(response.getBody().message()).isEqualTo("Validation failed");
        assertThat(response.getBody().fieldErrors()).containsExactly(
                org.assertj.core.api.Assertions.entry("firstName", "firstName is required"),
                org.assertj.core.api.Assertions.entry("age", "age must be >= 0"),
                org.assertj.core.api.Assertions.entry("userRequest", "object-level problem"));
    }

    @Test
    void constraintViolationsMapToFieldErrors() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(new UserRequest("", "Smith", 200, Gender.MALE));

            ResponseEntity<ErrorResponse> response =
                    handler.handleConstraintViolation(new ConstraintViolationException(violations), request);

            assertBody(response, HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED);
            assertThat(response.getBody().fieldErrors())
                    .containsEntry("firstName", "firstName is required")
                    .containsEntry("age", "age must be <= 150");
        }
    }

    @Test
    void invalidEnumValueListsAllowedValues() {
        InvalidFormatException cause = InvalidFormatException.from(null, "bad enum", "ROBOT", Gender.class);
        cause.prependPath(new UserRequest(), "gender");
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("bad", cause, new MockHttpInputMessage(new byte[0]));

        ResponseEntity<ErrorResponse> response = handler.handleUnreadable(ex, request);

        assertBody(response, HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST);
        assertThat(response.getBody().message())
                .isEqualTo("Invalid value 'ROBOT' for field 'gender'; allowed values: [MALE, FEMALE, OTHER]");
    }

    @Test
    void mismatchedFieldTypeNamesTheField() {
        MismatchedInputException cause = MismatchedInputException.from(null, Integer.class, "bad int");
        cause.prependPath(new UserRequest(), "age");
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("bad", cause, new MockHttpInputMessage(new byte[0]));

        ResponseEntity<ErrorResponse> response = handler.handleUnreadable(ex, request);

        assertThat(response.getBody().message()).isEqualTo("Invalid value for field 'age'");
    }

    @Test
    void unreadableBodyWithoutDetailsGetsGenericMessage() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("bad",
                new RuntimeException("parser internals"), new MockHttpInputMessage(new byte[0]));

        ResponseEntity<ErrorResponse> response = handler.handleUnreadable(ex, request);

        assertBody(response, HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Malformed or unreadable request body");
    }

    @Test
    void typeMismatchDescribesParameterAndExpectedType() throws Exception {
        MethodParameter parameter = new MethodParameter(UserController.class.getMethod("getUser", Long.class), 0);
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("abc", Long.class, "id", parameter, null);

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex, request);

        assertBody(response, HttpStatus.BAD_REQUEST, ErrorCode.INVALID_PARAMETER);
        assertThat(response.getBody().message()).isEqualTo("Invalid value 'abc' for parameter 'id': expected Long");
    }

    @Test
    void methodNotSupportedSetsAllowHeader() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("PATCH", List.of("GET", "POST"));

        ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupported(ex, request);

        assertBody(response, HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED);
        assertThat(response.getHeaders().getAllow()).containsExactlyInAnyOrder(HttpMethod.GET, HttpMethod.POST);
        assertThat(response.getBody().message()).isEqualTo("HTTP method PATCH is not supported for this endpoint");
    }

    @Test
    void methodNotSupportedWithoutKnownMethodsOmitsAllowHeader() {
        ResponseEntity<ErrorResponse> response =
                handler.handleMethodNotSupported(new HttpRequestMethodNotSupportedException("PATCH"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getHeaders().getAllow()).isEmpty();
    }

    @Test
    void unsupportedMediaTypeMapsTo415() {
        HttpMediaTypeNotSupportedException ex =
                new HttpMediaTypeNotSupportedException(MediaType.TEXT_XML, List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<ErrorResponse> response = handler.handleMediaTypeNotSupported(ex, request);

        assertBody(response, HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody().message())
                .isEqualTo("Content type 'text/xml' is not supported; use application/json");
    }

    @Test
    @SuppressWarnings("deprecation")
    void noResourceFoundMapsTo404() {
        MockHttpServletRequest getRequest = new MockHttpServletRequest("GET", "/api/users");

        ResponseEntity<ErrorResponse> response =
                handler.handleNoResource(new NoResourceFoundException(HttpMethod.GET, "api/users"), getRequest);

        assertBody(response, HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("No endpoint GET /api/users");
    }

    @Test
    void unexpectedExceptionMapsTo500WithGenericMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpected(new IllegalStateException("secret internals"), request);

        assertBody(response, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR);
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred").doesNotContain("secret");
    }
}
