package com.neuroguard.forumsservice.service;

import com.neuroguard.forumsservice.dto.CommentRequest;
import com.neuroguard.forumsservice.dto.CommentResponse;
import com.neuroguard.forumsservice.dto.ReplyRequest;
import com.neuroguard.forumsservice.dto.UserDto;
import com.neuroguard.forumsservice.entity.Comment;
import com.neuroguard.forumsservice.entity.CommentLike;
import com.neuroguard.forumsservice.entity.Post;
import com.neuroguard.forumsservice.feign.UserServiceClient;
import com.neuroguard.forumsservice.repository.CommentLikeRepository;
import com.neuroguard.forumsservice.repository.CommentRepository;
import com.neuroguard.forumsservice.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserServiceClient userServiceClient;
    private final CommentLikeRepository commentLikeRepository;

    @Transactional
    public CommentResponse addComment(Long postId, CommentRequest request, Long authorId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setPost(post);
        comment.setAuthorId(authorId);
        Comment saved = commentRepository.save(comment);

        return mapToResponse(saved, authorId);
    }

    @Transactional
    public CommentResponse addReply(Long postId, Long parentCommentId, ReplyRequest request, Long authorId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new RuntimeException("Parent comment not found"));
        if (!parent.getPost().getId().equals(postId)) {
            throw new RuntimeException("Parent comment does not belong to this post");
        }
        Comment reply = new Comment();
        reply.setContent(request.getContent());
        reply.setPost(post);
        reply.setAuthorId(authorId);
        reply.setParentComment(parent);
        Comment saved = commentRepository.save(reply);
        return mapToResponse(saved, authorId);
    }

    @Transactional
    public void deleteComment(Long commentId, Long currentUserId, String currentUserRole) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getAuthorId().equals(currentUserId) && !"ADMIN".equals(currentUserRole)) {
            throw new RuntimeException("You are not authorized to delete this comment");
        }
        commentRepository.delete(comment);
    }

    public List<CommentResponse> getCommentsByPost(Long postId, Long currentUserId) {
        return commentRepository.findByPostId(postId).stream()
                .map(comment -> mapToResponse(comment, currentUserId))
                .collect(Collectors.toList());
    }

    // Legacy method (if needed) – but we'll keep it private or remove.
    // public List<CommentResponse> getCommentsByPost(Long postId) {
    //     return getCommentsByPost(postId, null);
    // }

    public int countCommentsByPost(Long postId) {
        return commentRepository.findByPostId(postId).size();
    }

    @Transactional
    public void likeComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        if (commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            throw new RuntimeException("You already liked this comment");
        }
        CommentLike like = new CommentLike();
        like.setComment(comment);
        like.setUserId(userId);
        commentLikeRepository.save(like);
    }

    @Transactional
    public void unlikeComment(Long commentId, Long userId) {
        if (!commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            throw new RuntimeException("You have not liked this comment");
        }
        commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
    }

    private CommentResponse mapToResponse(Comment comment, Long currentUserId) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setContent(comment.getContent());
        response.setPostId(comment.getPost().getId());
        response.setAuthorId(comment.getAuthorId());
        response.setCreatedAt(comment.getCreatedAt());
        response.setParentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null);
        response.setLikeCount(comment.getLikes().size());
        response.setReplyCount(comment.getReplies().size());
        if (currentUserId != null) {
            response.setLikedByCurrentUser(commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), currentUserId));
        }
        // Fetch author username via Feign
        try {
            UserDto author = userServiceClient.getUserById(comment.getAuthorId());
            response.setAuthorUsername(author.getUsername());
        } catch (Exception e) {
            response.setAuthorUsername("Unknown");
        }
        return response;
    }
}