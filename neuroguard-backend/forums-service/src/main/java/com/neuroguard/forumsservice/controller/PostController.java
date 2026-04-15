package com.neuroguard.forumsservice.controller;

import com.neuroguard.forumsservice.dto.PagedResponse;
import com.neuroguard.forumsservice.dto.PostRequest;
import com.neuroguard.forumsservice.dto.PostResponse;
import com.neuroguard.forumsservice.service.PostImageService;
import com.neuroguard.forumsservice.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostImageService postImageService;

    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    private String getCurrentUserRole(HttpServletRequest request) {
        return (String) request.getAttribute("userRole");
    }

    @GetMapping
    public ResponseEntity<List<PostResponse>> getAllPosts(HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        return ResponseEntity.ok(postService.getAllPosts(currentUserId));
    }

    @GetMapping("/paged")
    public ResponseEntity<PagedResponse<PostResponse>> getPostsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(required = false) Long categoryId,
            HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        return ResponseEntity.ok(postService.getPostsPaged(page, size, sort, currentUserId, categoryId));
    }

    @GetMapping("/search")
    public ResponseEntity<PagedResponse<PostResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        return ResponseEntity.ok(postService.search(q, page, size, currentUserId));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable Long id, HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        return ResponseEntity.ok(postService.getPostById(id, currentUserId));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody PostRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        PostResponse created = postService.createPost(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id:\\d+}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        String role = getCurrentUserRole(httpRequest);
        return ResponseEntity.ok(postService.updatePost(id, request, userId, role));
    }

    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        String role = getCurrentUserRole(httpRequest);
        postService.deletePost(id, userId, role);
        return ResponseEntity.noContent().build();
    }

    // New endpoints for likes and shares
    @PostMapping("/{id:\\d+}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> likePost(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        postService.likePost(id, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id:\\d+}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unlikePost(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        postService.unlikePost(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id:\\d+}/share")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> sharePost(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        postService.sharePost(id, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id:\\d+}/pin")
    @PreAuthorize("isAuthenticated() and hasRole('ADMIN')")
    public ResponseEntity<PostResponse> setPinned(
            @PathVariable Long id,
            @RequestParam boolean pinned,
            HttpServletRequest request) {
        String role = getCurrentUserRole(request);
        return ResponseEntity.ok(postService.setPinned(id, pinned, role));
    }

    // Post images: upload, list, serve file, delete
    @PostMapping("/{id:\\d+}/images")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> uploadImages(
            @PathVariable Long id,
            @RequestParam("files") MultipartFile[] files,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<String> urls = postImageService.addImages(id, files, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(urls);
    }

    @GetMapping("/{id:\\d+}/images")
    public ResponseEntity<List<String>> getImageUrls(@PathVariable Long id) {
        return ResponseEntity.ok(postImageService.getImageUrls(id));
    }

    @GetMapping("/{id:\\d+}/images/{imageId:\\d+}/file")
    public ResponseEntity<Resource> getImageFile(
            @PathVariable Long id,
            @PathVariable Long imageId) {
        try {
            Resource resource = postImageService.getImageFile(id, imageId);
            String contentType = "image/jpeg";
            String filename = resource.getFilename();
            if (filename != null) {
                if (filename.endsWith(".png")) contentType = "image/png";
                else if (filename.endsWith(".gif")) contentType = "image/gif";
                else if (filename.endsWith(".webp")) contentType = "image/webp";
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (filename != null ? filename : "image") + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id:\\d+}/images/{imageId:\\d+}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long id,
            @PathVariable Long imageId,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        postImageService.deleteImage(id, imageId, userId);
        return ResponseEntity.noContent().build();
    }
}