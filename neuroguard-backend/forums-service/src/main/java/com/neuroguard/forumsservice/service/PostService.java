package com.neuroguard.forumsservice.service;

import com.neuroguard.forumsservice.dto.PagedResponse;
import com.neuroguard.forumsservice.dto.PostRequest;
import com.neuroguard.forumsservice.dto.PostResponse;
import com.neuroguard.forumsservice.dto.UserDto;
import com.neuroguard.forumsservice.entity.Category;
import com.neuroguard.forumsservice.entity.Post;
import com.neuroguard.forumsservice.entity.PostLike;
import com.neuroguard.forumsservice.entity.PostShare;
import com.neuroguard.forumsservice.feign.UserServiceClient;
import com.neuroguard.forumsservice.repository.CategoryRepository;
import com.neuroguard.forumsservice.repository.PostLikeRepository;
import com.neuroguard.forumsservice.repository.PostRepository;
import com.neuroguard.forumsservice.repository.PostShareRepository;
import com.neuroguard.forumsservice.service.PostImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final UserServiceClient userServiceClient;
    private final CommentService commentService;
    private final PostLikeRepository postLikeRepository;
    private final PostShareRepository postShareRepository;
    private final ProfanityFilterService profanityFilterService;
    private final ReadabilityService readabilityService;
    private final PostImageService postImageService;

    @Transactional
    public PostResponse createPost(PostRequest request, Long authorId) {
        profanityFilterService.validate(request.getTitle());
        profanityFilterService.validate(request.getContent());
        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setAuthorId(authorId);
        if (request.getCategoryId() != null) {
            categoryRepository.findById(request.getCategoryId()).ifPresent(post::setCategory);
        }
        Post saved = postRepository.save(post);
        double readability = readabilityService.computeFleschReadingEase(request.getTitle() + " " + request.getContent());
        saved.setReadabilityScore(readability);
        saved = postRepository.save(saved);
        return mapToResponse(saved, authorId);
    }

    public List<PostResponse> getAllPosts(Long currentUserId) {
        return postRepository.findAllByOrderByPinnedDescCreatedAtDesc().stream()
                .map(post -> mapToResponse(post, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * Get posts with pagination and sorting.
     * @param page 0-based page index
     * @param size page size (default 10)
     * @param sort one of: newest, oldest, mostLiked, mostComments
     * @param categoryId optional; filter by category
     */
    public PagedResponse<PostResponse> getPostsPaged(int page, int size, String sort, Long currentUserId, Long categoryId) {
        if (size < 1) size = 10;
        if (size > 50) size = 50;
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage;
        String sortKey = sort != null ? sort.toLowerCase() : "newest";
        if (categoryId != null) {
            switch (sortKey) {
                case "oldest":
                    postPage = postRepository.findByCategoryIdOrderByPinnedDescCreatedAtAsc(categoryId, pageable);
                    break;
                case "mostliked":
                    postPage = postRepository.findByCategoryIdOrderByLikesDesc(categoryId, pageable);
                    break;
                case "mostcomments":
                    postPage = postRepository.findByCategoryIdOrderByCommentCountDesc(categoryId, pageable);
                    break;
                default:
                    postPage = postRepository.findByCategoryIdOrderByPinnedDescCreatedAtDesc(categoryId, pageable);
                    break;
            }
        } else {
            switch (sortKey) {
                case "oldest":
                    postPage = postRepository.findAllByOrderByPinnedDescCreatedAtAsc(PageRequest.of(page, size));
                    break;
                case "mostliked":
                    postPage = postRepository.findPostsOrderByLikesDesc(pageable);
                    break;
                case "mostcomments":
                    postPage = postRepository.findPostsOrderByCommentCountDesc(pageable);
                    break;
                default:
                    postPage = postRepository.findAllByOrderByPinnedDescCreatedAtDesc(pageable);
                    break;
            }
        }
        List<PostResponse> content = postPage.getContent().stream()
                .map(post -> mapToResponse(post, currentUserId))
                .collect(Collectors.toList());
        return new PagedResponse<>(
                content,
                postPage.getTotalElements(),
                postPage.getTotalPages(),
                postPage.getSize(),
                postPage.getNumber(),
                postPage.isFirst(),
                postPage.isLast()
        );
    }

    public PostResponse getPostById(Long id, Long currentUserId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        return mapToResponse(post, currentUserId);
    }

    @Transactional
    public PostResponse updatePost(Long id, PostRequest request, Long currentUserId, String currentUserRole) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

        if (!post.getAuthorId().equals(currentUserId) && !"ADMIN".equals(currentUserRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to update this post");
        }

        profanityFilterService.validate(request.getTitle());
        profanityFilterService.validate(request.getContent());
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        if (request.getCategoryId() != null) {
            categoryRepository.findById(request.getCategoryId()).ifPresent(post::setCategory);
        } else {
            post.setCategory(null);
        }
        Post saved = postRepository.save(post);
        double readability = readabilityService.computeFleschReadingEase(saved.getTitle() + " " + saved.getContent());
        saved.setReadabilityScore(readability);
        saved = postRepository.save(saved);
        return mapToResponse(saved, currentUserId);
    }

    @Transactional
    public void deletePost(Long id, Long currentUserId, String currentUserRole) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

        if (!post.getAuthorId().equals(currentUserId) && !"ADMIN".equals(currentUserRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to delete this post");
        }
        postRepository.delete(post);
    }

    @Transactional
    public void likePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already liked this post");
        }
        PostLike like = new PostLike();
        like.setPost(post);
        like.setUserId(userId);
        postLikeRepository.save(like);
    }

    @Transactional
    public void unlikePost(Long postId, Long userId) {
        if (!postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You have not liked this post");
        }
        postLikeRepository.deleteByPostIdAndUserId(postId, userId);
    }

    @Transactional
    public void sharePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        if (postShareRepository.existsByPostIdAndUserId(postId, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already shared this post");
        }
        PostShare share = new PostShare();
        share.setPost(post);
        share.setUserId(userId);
        postShareRepository.save(share);
    }

    @Transactional
    public PostResponse setPinned(Long postId, boolean pinned, String currentUserRole) {
        if (!"ADMIN".equals(currentUserRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can pin or unpin posts");
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        post.setPinned(pinned);
        return mapToResponse(postRepository.save(post), null);
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
        response.setPinned(post.isPinned());

        if (currentUserId != null) {
            response.setLikedByCurrentUser(postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUserId));
            response.setSharedByCurrentUser(postShareRepository.existsByPostIdAndUserId(post.getId(), currentUserId));
        }

        try {
            UserDto author = userServiceClient.getUserById(post.getAuthorId());
            response.setAuthorUsername(author.getUsername());
            response.setAuthorRole(author.getRole());
        } catch (Exception e) {
            response.setAuthorUsername("Unknown");
        }
        if (post.getCategory() != null) {
            response.setCategoryId(post.getCategory().getId());
            response.setCategoryName(post.getCategory().getName());
        }
        if (post.getReadabilityScore() != null) {
            response.setReadabilityScore(post.getReadabilityScore());
            response.setReadabilityLabel(readabilityService.getReadabilityLabel(post.getReadabilityScore()));
        }
        response.setImageUrls(postImageService.getImageUrls(post.getId()));
        response.setImageIds(postImageService.getImageIds(post.getId()));
        return response;
    }

    public PagedResponse<PostResponse> search(String q, int page, int size, Long currentUserId) {
        if (size < 1) size = 10;
        if (size > 50) size = 50;
        if (q == null || q.isBlank()) {
            return getPostsPaged(page, size, "newest", currentUserId, null);
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.search(q.trim(), pageable);
        List<PostResponse> content = postPage.getContent().stream()
                .map(post -> mapToResponse(post, currentUserId))
                .collect(Collectors.toList());
        return new PagedResponse<>(
                content,
                postPage.getTotalElements(),
                postPage.getTotalPages(),
                postPage.getSize(),
                postPage.getNumber(),
                postPage.isFirst(),
                postPage.isLast()
        );
    }
}