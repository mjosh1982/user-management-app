package com.example.usermanagement.controller;

import com.example.usermanagement.dto.UserRequest;
import com.example.usermanagement.exception.UserAlreadyExistsException;
import com.example.usermanagement.exception.UserNotFoundException;
import com.example.usermanagement.model.Gender;
import com.example.usermanagement.model.User;
import com.example.usermanagement.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    private static final String VALID_BODY = """
            {"firstName":"Diana","lastName":"Prince","age":28,"gender":"FEMALE"}
            """;
    private static final UserRequest VALID_REQUEST = new UserRequest("Diana", "Prince", 28, Gender.FEMALE);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void listReturnsAllUsers() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(
                new User(1L, "Alice", "Smith", 30, Gender.FEMALE),
                new User(2L, "Bob", "Johnson", 42, Gender.MALE)));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Alice"))
                .andExpect(jsonPath("$[0].lastName").value("Smith"))
                .andExpect(jsonPath("$[0].age").value(30))
                .andExpect(jsonPath("$[0].gender").value("FEMALE"))
                .andExpect(jsonPath("$[1].firstName").value("Bob"));
    }

    @Test
    void listReturnsEmptyArray() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getReturnsUser() throws Exception {
        when(userService.getUser(2L)).thenReturn(new User(2L, "Bob", "Johnson", 42, Gender.MALE));

        mockMvc.perform(get("/api/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.firstName").value("Bob"));
    }

    @Test
    void getMissingUserReturns404() throws Exception {
        when(userService.getUser(99L)).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("User with id 99 not found"))
                .andExpect(jsonPath("$.path").value("/api/users/99"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void getWithNonNumericIdReturns400() throws Exception {
        mockMvc.perform(get("/api/users/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'id': expected Long"));
        verifyNoInteractions(userService);
    }

    @Test
    void getWithNonPositiveIdReturns400() throws Exception {
        mockMvc.perform(get("/api/users/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.id").value("id must be a positive number"));
        verifyNoInteractions(userService);
    }

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        when(userService.createUser(VALID_REQUEST)).thenReturn(new User(4L, "Diana", "Prince", 28, Gender.FEMALE));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/users/4"))
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.firstName").value("Diana"));
        verify(userService).createUser(VALID_REQUEST);
    }

    @Test
    void createIgnoresClientSuppliedId() throws Exception {
        when(userService.createUser(VALID_REQUEST)).thenReturn(new User(4L, "Diana", "Prince", 28, Gender.FEMALE));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":100,"firstName":"Diana","lastName":"Prince","age":28,"gender":"FEMALE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4));
    }

    @Test
    void createWithInvalidBodyReturns400WithAllFieldErrors() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":" ","lastName":"","age":200}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.firstName").value("firstName is required"))
                .andExpect(jsonPath("$.fieldErrors.lastName").value("lastName is required"))
                .andExpect(jsonPath("$.fieldErrors.age").value("age must be <= 150"))
                .andExpect(jsonPath("$.fieldErrors.gender").value("gender is required"));
        verifyNoInteractions(userService);
    }

    @Test
    void createWithInvalidGenderListsAllowedValues() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"A","lastName":"B","age":5,"gender":"UNKNOWN"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid value 'UNKNOWN' for field 'gender'; allowed values: [MALE, FEMALE, OTHER]"));
    }

    @Test
    void createWithWrongFieldTypeNamesTheField() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"A","lastName":"B","age":"old","gender":"MALE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid value for field 'age'"));
    }

    @Test
    void createWithMalformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));
    }

    @Test
    void createWithEmptyBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void createWithUnsupportedContentTypeReturns415() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.message").value(containsString("text/plain")));
    }

    @Test
    void createDuplicateReturns409() throws Exception {
        when(userService.createUser(any())).thenThrow(new UserAlreadyExistsException("Diana", "Prince"));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("User 'Diana Prince' already exists"));
    }

    @Test
    void updateReturnsUpdatedUser() throws Exception {
        when(userService.updateUser(1L, VALID_REQUEST)).thenReturn(new User(1L, "Diana", "Prince", 28, Gender.FEMALE));

        mockMvc.perform(put("/api/users/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.lastName").value("Prince"));
    }

    @Test
    void updateMissingUserReturns404() throws Exception {
        when(userService.updateUser(eq(99L), any())).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(put("/api/users/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void updateWithInvalidBodyReturns400() throws Exception {
        mockMvc.perform(put("/api/users/1").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"","lastName":"Prince","age":-3,"gender":"FEMALE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.firstName").value("firstName is required"))
                .andExpect(jsonPath("$.fieldErrors.age").value("age must be >= 0"));
        verifyNoInteractions(userService);
    }

    @Test
    void updateWithNegativeIdReturns400() throws Exception {
        mockMvc.perform(put("/api/users/-5").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.id").value("id must be a positive number"));
        verifyNoInteractions(userService);
    }

    @Test
    void updateDuplicateNameReturns409() throws Exception {
        when(userService.updateUser(eq(1L), any())).thenThrow(new UserAlreadyExistsException("Diana", "Prince"));

        mockMvc.perform(put("/api/users/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"));
    }

    @Test
    void deleteReturns204WithEmptyBody() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(userService).deleteUser(1L);
    }

    @Test
    void deleteMissingUserReturns404() throws Exception {
        doThrow(new UserNotFoundException(99L)).when(userService).deleteUser(99L);

        mockMvc.perform(delete("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void deleteWithNonNumericIdReturns400() throws Exception {
        mockMvc.perform(delete("/api/users/x1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        verify(userService, org.mockito.Mockito.never()).deleteUser(anyLong());
    }

    @Test
    void unsupportedMethodReturns405WithAllowHeader() throws Exception {
        mockMvc.perform(delete("/api/users"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(header().string("Allow", containsString("POST")))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("HTTP method DELETE is not supported for this endpoint"));
    }

    @Test
    void patchIsNotSupported() throws Exception {
        mockMvc.perform(patch("/api/users/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void unknownPathReturns404() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No endpoint GET /api/does-not-exist"));
    }

    @Test
    void unexpectedErrorReturns500WithoutLeakingDetails() throws Exception {
        when(userService.getAllUsers()).thenThrow(new IllegalStateException("database password=secret"));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("secret"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }
}
