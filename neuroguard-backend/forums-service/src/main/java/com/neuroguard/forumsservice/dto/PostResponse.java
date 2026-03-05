package com.neuroguard.forumsservice.dto;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private String authorUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int commentCount;
    private int likeCount;
    private int shareCount;
    private boolean likedByCurrentUser;    // optional
    private boolean sharedByCurrentUser;   // optional
}