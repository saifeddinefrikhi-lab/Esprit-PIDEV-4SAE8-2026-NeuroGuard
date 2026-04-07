package com.neuroguard.forumsservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuroguard.forumsservice.dto.UserDto;
import com.neuroguard.forumsservice.entity.Comment;
import com.neuroguard.forumsservice.entity.Post;
import com.neuroguard.forumsservice.feign.UserServiceClient;
import com.neuroguard.forumsservice.repository.CommentRepository;
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
class CommentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private CommentRepository commentRepository;

    @MockBean
    private UserServiceClient userServiceClient;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        postRepository.deleteAll();

        UserDto dto = new UserDto();
        dto.setId(1L);
        dto.setUsername("u1");
        dto.setRole("USER");
        when(userServiceClient.getUserById(anyLong())).thenReturn(dto);
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void authenticatedUserCanCreateComment() throws Exception {
        Post post = new Post();
        post.setTitle("t");
        post.setContent("c");
        post.setAuthorId(5L);
        Post savedPost = postRepository.save(post);

        mockMvc.perform(post("/api/posts/{postId}/comments", savedPost.getId())
                        .requestAttr("userId", 1L)
                        .requestAttr("userRole", "PATIENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "nice post"))))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void nonOwnerCannotDeleteComment() throws Exception {
        Post post = new Post();
        post.setTitle("t");
        post.setContent("c");
        post.setAuthorId(5L);
        Post savedPost = postRepository.save(post);

        Comment comment = new Comment();
        comment.setContent("comment");
        comment.setPost(savedPost);
        comment.setAuthorId(99L);
        Comment savedComment = commentRepository.save(comment);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", savedPost.getId(), savedComment.getId())
                        .requestAttr("userId", 1L)
                        .requestAttr("userRole", "PATIENT"))
                .andExpect(status().isForbidden());
    }
}
