package com.example.usermanagement.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserControllerIntegrationTest {

    private static final String VALID_USER = """
            {"firstName":"Diana","lastName":"Prince","age":28,"gender":"FEMALE"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listUsersReturnsSeededUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Alice"));
    }

    @Test
    void getUserById() throws Exception {
        mockMvc.perform(get("/api/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Bob"));
    }

    @Test
    void getMissingUserReturns404() throws Exception {
        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User with id 999 not found"));
    }

    @Test
    void addUserReturns201() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID_USER))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/users/4")))
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.firstName").value("Diana"))
                .andExpect(jsonPath("$.gender").value("FEMALE"));

        mockMvc.perform(get("/api/users")).andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    void addInvalidUserReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"","lastName":"X","age":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.firstName").exists())
                .andExpect(jsonPath("$.fieldErrors.age").exists())
                .andExpect(jsonPath("$.fieldErrors.gender").exists());
    }

    @Test
    void addUserWithMalformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser() throws Exception {
        mockMvc.perform(put("/api/users/1").contentType(MediaType.APPLICATION_JSON).content(VALID_USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Diana"));

        mockMvc.perform(get("/api/users/1")).andExpect(jsonPath("$.lastName").value("Prince"));
    }

    @Test
    void updateMissingUserReturns404() throws Exception {
        mockMvc.perform(put("/api/users/999").contentType(MediaType.APPLICATION_JSON).content(VALID_USER))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser() throws Exception {
        mockMvc.perform(delete("/api/users/1")).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/users/1")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/users")).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void deleteMissingUserReturns404() throws Exception {
        mockMvc.perform(delete("/api/users/999")).andExpect(status().isNotFound());
    }

    @Test
    void openApiDocsAreExposed() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/users']").exists())
                .andExpect(jsonPath("$.paths['/api/users/{id}']").exists());
    }
}
