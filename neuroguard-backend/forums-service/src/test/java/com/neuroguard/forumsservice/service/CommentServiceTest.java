package com.neuroguard.forumsservice.service;

import com.neuroguard.forumsservice.dto.CommentRequest;
import com.neuroguard.forumsservice.entity.Comment;
import com.neuroguard.forumsservice.entity.Post;
import com.neuroguard.forumsservice.feign.UserServiceClient;
import com.neuroguard.forumsservice.repository.CommentLikeRepository;
import com.neuroguard.forumsservice.repository.CommentRepository;
import com.neuroguard.forumsservice.repository.PostRepository;
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
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private CommentLikeRepository commentLikeRepository;
    @Mock
    private ProfanityFilterService profanityFilterService;

    @InjectMocks
    private CommentService commentService;

    @Test
    void addComment_postNotFound() {
        CommentRequest request = new CommentRequest();
        request.setContent("My comment");
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> commentService.addComment(1L, request, 9L)
        );

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void updateComment_forbidden() {
        Comment comment = new Comment();
        comment.setId(4L);
        comment.setAuthorId(1L);
        Post post = new Post();
        post.setId(2L);
        comment.setPost(post);
        when(commentRepository.findById(4L)).thenReturn(Optional.of(comment));

        CommentRequest req = new CommentRequest();
        req.setContent("updated");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> commentService.updateComment(4L, req, 5L, "USER")
        );

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void deleteComment_successForAdmin() {
        Comment comment = new Comment();
        comment.setId(10L);
        comment.setAuthorId(88L);
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));

        commentService.deleteComment(10L, 1L, "ADMIN");
    }

    @Test
    void likeComment_alreadyLiked_conflict() {
        Comment comment = new Comment();
        comment.setId(5L);
        when(commentRepository.findById(5L)).thenReturn(Optional.of(comment));
        when(commentLikeRepository.existsByCommentIdAndUserId(5L, 2L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> commentService.likeComment(5L, 2L)
        );

        assertEquals(409, ex.getStatusCode().value());
    }
}
