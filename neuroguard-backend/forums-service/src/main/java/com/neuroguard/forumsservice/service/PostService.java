package com.neuroguard.forumsservice.service;

import com.neuroguard.forumsservice.dto.PostRequest;
import com.neuroguard.forumsservice.dto.PostResponse;
import com.neuroguard.forumsservice.dto.UserDto;
import com.neuroguard.forumsservice.entity.Post;
import com.neuroguard.forumsservice.entity.PostLike;
import com.neuroguard.forumsservice.entity.PostShare;
import com.neuroguard.forumsservice.feign.UserServiceClient;
import com.neuroguard.forumsservice.repository.PostLikeRepository;
import com.neuroguard.forumsservice.repository.PostRepository;
import com.neuroguard.forumsservice.repository.PostShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserServiceClient userServiceClient;
    private final CommentService commentService;
    private final PostLikeRepository postLikeRepository;
    private final PostShareRepository postShareRepository;

    @Transactional
    public PostResponse createPost(PostRequest request, Long authorId) {
        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setAuthorId(authorId);
        Post saved = postRepository.save(post);
        return mapToResponse(saved, authorId);
    }

    public List<PostResponse> getAllPosts(Long currentUserId) {
        return postRepository.findAll().stream()
                .map(post -> mapToResponse(post, currentUserId))
                .collect(Collectors.toList());
    }

    public PostResponse getPostById(Long id, Long currentUserId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        return mapToResponse(post, currentUserId);
    }

    @Transactional
    public PostResponse updatePost(Long id, PostRequest request, Long currentUserId, String currentUserRole) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getAuthorId().equals(currentUserId) && !"ADMIN".equals(currentUserRole)) {
            throw new RuntimeException("You are not authorized to update this post");
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        return mapToResponse(postRepository.save(post), currentUserId);
    }

    @Transactional
    public void deletePost(Long id, Long currentUserId, String currentUserRole) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getAuthorId().equals(currentUserId) && !"ADMIN".equals(currentUserRole)) {
            throw new RuntimeException("You are not authorized to delete this post");
        }
        postRepository.delete(post);
    }

    @Transactional
    public void likePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            throw new RuntimeException("You already liked this post");
        }
        PostLike like = new PostLike();
        like.setPost(post);
        like.setUserId(userId);
        postLikeRepository.save(like);
    }

    @Transactional
    public void unlikePost(Long postId, Long userId) {
        if (!postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            throw new RuntimeException("You have not liked this post");
        }
        postLikeRepository.deleteByPostIdAndUserId(postId, userId);
    }

    @Transactional
    public void sharePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        if (postShareRepository.existsByPostIdAndUserId(postId, userId)) {
            throw new RuntimeException("You already shared this post");
        }
        PostShare share = new PostShare();
        share.setPost(post);
        share.setUserId(userId);
        postShareRepository.save(share);
    }

    private PostResponse mapToResponse(Post post, Long currentUserId) {
        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setTitle(post.getTitle());
        response.setContent(post.getContent());
        response.setAuthorId(post.getAuthorId());
        response.setCreatedAt(post.getCreatedAt());
        response.setUpdatedAt(post.getUpdatedAt());
        response.setCommentCount(commentService.countCommentsByPost(post.getId()));
        response.setLikeCount(post.getLikes().size());
        response.setShareCount(post.getShares().size());

        if (currentUserId != null) {
            response.setLikedByCurrentUser(postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUserId));
            response.setSharedByCurrentUser(postShareRepository.existsByPostIdAndUserId(post.getId(), currentUserId));
        }

        try {
            UserDto author = userServiceClient.getUserById(post.getAuthorId());
            response.setAuthorUsername(author.getUsername());
        } catch (Exception e) {
            response.setAuthorUsername("Unknown");
        }

        return response;
    }
}