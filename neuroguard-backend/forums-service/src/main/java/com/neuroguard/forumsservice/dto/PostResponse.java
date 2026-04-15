package com.neuroguard.forumsservice.dto;


import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
    private boolean pinned;
    private Long categoryId;
    private String categoryName;
    private String authorRole;   // e.g. PATIENT, PROVIDER, ADMIN
    private Double readabilityScore;  // Flesch Reading Ease 0–100
    private String readabilityLabel;   // Easy, Medium, Hard
    private List<String> imageUrls;    // URLs to view post images (e.g. /api/posts/1/images/1/file)
    private List<Long> imageIds;       // same order as imageUrls, for delete
}