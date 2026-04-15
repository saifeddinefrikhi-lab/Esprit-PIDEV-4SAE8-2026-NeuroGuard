package com.neuroguard.forumsservice.service;

import com.neuroguard.forumsservice.dto.PostRequest;
import com.neuroguard.forumsservice.entity.Post;
import com.neuroguard.forumsservice.feign.UserServiceClient;
import com.neuroguard.forumsservice.repository.CategoryRepository;
import com.neuroguard.forumsservice.repository.PostLikeRepository;
import com.neuroguard.forumsservice.repository.PostRepository;
import com.neuroguard.forumsservice.repository.PostShareRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private CommentService commentService;
    @Mock
    private PostLikeRepository postLikeRepository;
    @Mock
    private PostShareRepository postShareRepository;
    @Mock
    private ProfanityFilterService profanityFilterService;
    @Mock
    private ReadabilityService readabilityService;
    @Mock
    private PostImageService postImageService;

    @InjectMocks
    private PostService postService;

    @Test
    void createPost_success() {
        PostRequest request = new PostRequest();
        request.setTitle("Hello");
        request.setContent("Content");

        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post p = invocation.getArgument(0);
            if (p.getId() == null) {
                p.setId(1L);
            }
            return p;
        });
        when(readabilityService.computeFleschReadingEase(any())).thenReturn(75.0);
        when(commentService.countCommentsByPost(1L)).thenReturn(0);
        when(postImageService.getImageUrls(1L)).thenReturn(java.util.List.of());
        when(postImageService.getImageIds(1L)).thenReturn(java.util.List.of());

        var response = postService.createPost(request, 5L);

        assertEquals("Hello", response.getTitle());
        assertEquals(5L, response.getAuthorId());
    }

    @Test
    void deletePost_forbiddenForNonOwnerAndNonAdmin() {
        Post post = new Post();
        post.setId(11L);
        post.setAuthorId(1L);
        when(postRepository.findById(11L)).thenReturn(Optional.of(post));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> postService.deletePost(11L, 2L, "USER")
        );

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void deletePost_notFound() {
        when(postRepository.findById(77L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> postService.deletePost(77L, 1L, "ADMIN")
        );

        assertEquals(404, ex.getStatusCode().value());
    }
}
