package com.neuroguard.forumsservice.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PostRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private Long categoryId;  // optional
}