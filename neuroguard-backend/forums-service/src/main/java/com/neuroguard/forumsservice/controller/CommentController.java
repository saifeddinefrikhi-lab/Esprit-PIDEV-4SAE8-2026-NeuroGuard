package com.neuroguard.forumsservice.controller;

import com.neuroguard.forumsservice.dto.CommentRequest;
import com.neuroguard.forumsservice.dto.CommentResponse;
import com.neuroguard.forumsservice.dto.ReplyRequest;
import com.neuroguard.forumsservice.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    private String getCurrentUserRole(HttpServletRequest request) {
        return (String) request.getAttribute("userRole");
    }

    // Public: get comments for a post (with optional current user context)
    @GetMapping
    public ResponseEntity<List<CommentResponse>> getCommentsByPost(
            @PathVariable Long postId,
            HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request); // may be null
        return ResponseEntity.ok(commentService.getCommentsByPost(postId, currentUserId));
    }

    // Authenticated: add comment
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        CommentResponse created = commentService.addComment(postId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Authenticated: update comment (author or admin)
    @PutMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        String role = getCurrentUserRole(httpRequest);
        CommentResponse updated = commentService.updateComment(commentId, request, userId, role);
        return ResponseEntity.ok(updated);
    }

    // Authenticated: delete comment (author or admin)
    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        String role = getCurrentUserRole(httpRequest);
        commentService.deleteComment(commentId, userId, role);
        return ResponseEntity.noContent().build();
    }

    // --- New endpoints for likes and replies ---

    @PostMapping("/{commentId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> likeComment(@PathVariable Long commentId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        commentService.likeComment(commentId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{commentId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unlikeComment(@PathVariable Long commentId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        commentService.unlikeComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{commentId}/replies")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponse> addReply(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody ReplyRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        CommentResponse reply = commentService.addReply(postId, commentId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(reply);
    }
}