package com.neuroguard.forumsservice.dto;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentResponse {
    private Long id;
    private String content;
    private Long postId;
    private Long authorId;
    private String authorUsername;
    private LocalDateTime createdAt;
    private Long parentCommentId;           // null if top-level
    private int likeCount;
    private int replyCount;                 // number of direct replies
    private boolean likedByCurrentUser;
}