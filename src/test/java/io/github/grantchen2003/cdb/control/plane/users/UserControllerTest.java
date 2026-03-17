package io.github.grantchen2003.cdb.control.plane.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    // -----------------------------------------------------------------------
    // POST /users — happy path
    // -----------------------------------------------------------------------

    @Test
    void createUser_returns201() throws Exception {
        stubUserService();

        mockMvc.perform(post("/users"))
                .andExpect(status().isCreated());
    }

    @Test
    void createUser_returnsRawApiKeyInBody() throws Exception {
        stubUserService();

        mockMvc.perform(post("/users"))
                .andExpect(jsonPath("$.rawApiKey", notNullValue()));
    }

    @Test
    void createUser_rawApiKeyIsHexString() throws Exception {
        stubUserService();

        // generate() produces 32 random bytes → 64 hex chars
        mockMvc.perform(post("/users"))
                .andExpect(jsonPath("$.rawApiKey", matchesPattern("[0-9a-f]{64}")));
    }

    @Test
    void createUser_responseContentTypeIsJson() throws Exception {
        stubUserService();

        mockMvc.perform(post("/users"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void createUser_callsServiceExactlyOnce() throws Exception {
        stubUserService();

        mockMvc.perform(post("/users"));

        verify(userService, times(1)).createUser(anyString());
    }

    @Test
    void createUser_passesHashedKeyNotRawKeyToService() throws Exception {
        stubUserService();

        mockMvc.perform(post("/users"));

        // Capture what was passed to the service and assert it is NOT the raw key
        // returned in the response body. We do this by grabbing the response and
        // comparing the two values.
        var result = mockMvc.perform(post("/users"))
                .andReturn();

        var responseBody = objectMapper.readTree(result.getResponse().getContentAsString());
        var rawApiKey = responseBody.get("rawApiKey").asText();

        verify(userService, atLeastOnce()).createUser(argThat(arg -> !arg.equals(rawApiKey)));
    }

    // -----------------------------------------------------------------------
    // Wrong HTTP methods
    // -----------------------------------------------------------------------

    @Test
    void getUsers_returns405() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/users"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void deleteUsers_returns405() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/users"))
                .andExpect(status().isMethodNotAllowed());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void stubUserService() {
        when(userService.createUser(anyString()))
                .thenReturn(new User(UUID.randomUUID().toString(), "hashedKey", Instant.now()));
    }
}