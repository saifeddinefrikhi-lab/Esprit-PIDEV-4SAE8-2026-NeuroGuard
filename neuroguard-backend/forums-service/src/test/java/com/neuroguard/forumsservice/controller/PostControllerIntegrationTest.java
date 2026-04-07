package com.neuroguard.forumsservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuroguard.forumsservice.dto.UserDto;
import com.neuroguard.forumsservice.entity.Post;
import com.neuroguard.forumsservice.feign.UserServiceClient;
import com.neuroguard.forumsservice.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class PostControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PostRepository postRepository;

    @MockBean
    private UserServiceClient userServiceClient;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        UserDto dto = new UserDto();
        dto.setId(1L);
        dto.setUsername("u1");
        dto.setRole("USER");
        when(userServiceClient.getUserById(anyLong())).thenReturn(dto);
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void authenticatedUserCanCreatePost() throws Exception {
        Map<String, Object> payload = Map.of("title", "Post Title", "content", "Post Content");

        mockMvc.perform(post("/api/posts")
                        .requestAttr("userId", 1L)
                        .requestAttr("userRole", "PATIENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void ownerCanDeletePost() throws Exception {
        Post post = new Post();
        post.setTitle("Owned");
        post.setContent("Owned content");
        post.setAuthorId(1L);
        Post saved = postRepository.save(post);

        mockMvc.perform(delete("/api/posts/{id}", saved.getId())
                        .requestAttr("userId", 1L)
                        .requestAttr("userRole", "PATIENT"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void nonOwnerCannotDeletePost() throws Exception {
        Post post = new Post();
        post.setTitle("Foreign");
        post.setContent("Foreign content");
        post.setAuthorId(99L);
        Post saved = postRepository.save(post);

        mockMvc.perform(delete("/api/posts/{id}", saved.getId())
                        .requestAttr("userId", 1L)
                        .requestAttr("userRole", "PATIENT"))
                .andExpect(status().isForbidden());
    }
}
