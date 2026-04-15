package com.neuroguard.forumsservice.service;

import com.neuroguard.forumsservice.entity.Post;
import com.neuroguard.forumsservice.entity.PostImage;
import com.neuroguard.forumsservice.repository.PostImageRepository;
import com.neuroguard.forumsservice.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PostImageService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;

    @Value("${app.upload.dir:uploads/post-images}")
    private String uploadDir;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    @Transactional
    public List<String> addImages(Long postId, MultipartFile[] files, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        if (!post.getAuthorId().equals(currentUserId)) {
            throw new RuntimeException("Only the post author can add images");
        }
        Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(basePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
        List<String> urls = new ArrayList<>();
        int order = postImageRepository.findByPostIdOrderBySortOrderAsc(postId).size();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new RuntimeException("File too large: " + file.getOriginalFilename() + " (max 5MB)");
            }
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
                throw new RuntimeException("Invalid image type: " + file.getOriginalFilename());
            }
            String ext = contentType.split("/")[1];
            if ("jpeg".equalsIgnoreCase(ext)) ext = "jpg";
            String storedName = "post-" + postId + "_" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path target = basePath.resolve(storedName);
            try {
                file.transferTo(target.toFile());
            } catch (IOException e) {
                throw new RuntimeException("Failed to save file: " + file.getOriginalFilename(), e);
            }
            PostImage img = new PostImage();
            img.setPost(post);
            img.setStoredFileName(storedName);
            img.setSortOrder(order++);
            postImageRepository.save(img);
            urls.add("/api/posts/" + postId + "/images/" + img.getId() + "/file");
        }
        return urls;
    }

    public List<String> getImageUrls(Long postId) {
        return postImageRepository.findByPostIdOrderBySortOrderAsc(postId).stream()
                .map(img -> "/api/posts/" + postId + "/images/" + img.getId() + "/file")
                .toList();
    }

    public List<Long> getImageIds(Long postId) {
        return postImageRepository.findByPostIdOrderBySortOrderAsc(postId).stream()
                .map(PostImage::getId)
                .toList();
    }

    public Resource getImageFile(Long postId, Long imageId) throws IOException {
        PostImage img = postImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));
        if (!img.getPost().getId().equals(postId)) {
            throw new RuntimeException("Image not found for this post");
        }
        Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path filePath = basePath.resolve(img.getStoredFileName());
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("Image file not readable");
        }
        return resource;
    }

    @Transactional
    public void deleteImage(Long postId, Long imageId, Long currentUserId) {
        PostImage img = postImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));
        if (!img.getPost().getId().equals(postId)) {
            throw new RuntimeException("Image not found for this post");
        }
        Post post = img.getPost();
        if (!post.getAuthorId().equals(currentUserId)) {
            throw new RuntimeException("Only the post author can delete images");
        }
        postImageRepository.delete(img);
        try {
            Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = basePath.resolve(img.getStoredFileName());
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {}
    }
}
