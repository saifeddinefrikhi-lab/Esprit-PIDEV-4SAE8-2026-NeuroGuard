package com.neuroguard.userservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuroguard.userservice.entities.Role;
import com.neuroguard.userservice.entities.User;
import com.neuroguard.userservice.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        User user = new User();
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setUsername("admin1");
        user.setEmail("admin@test.com");
        user.setPassword("encoded");
        user.setRole(Role.ADMIN);
        userRepository.save(user);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListUsers() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void nonAdminCannotListUsers() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateUser() throws Exception {
        Map<String, Object> payload = Map.of(
                "firstName", "Jane",
                "lastName", "Doe",
                "username", "jane01",
                "email", "jane@test.com",
                "phoneNumber", "12345678",
                "gender", "F",
                "age", 28,
                "role", "PATIENT",
                "password", "Password123"
        );

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }
}
