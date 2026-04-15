package com.neuroguard.forumsservice.repository;


import com.neuroguard.forumsservice.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByAuthorId(Long authorId);

    List<Post> findAllByOrderByCreatedAtDesc();

    List<Post> findAllByOrderByPinnedDescCreatedAtDesc();

    Page<Post> findAllByOrderByPinnedDescCreatedAtDesc(Pageable pageable);

    Page<Post> findAllByOrderByPinnedDescCreatedAtAsc(Pageable pageable);

    @Query("SELECT p FROM Post p ORDER BY p.pinned DESC, size(p.likes) DESC, p.createdAt DESC")
    Page<Post> findPostsOrderByLikesDesc(Pageable pageable);

    @Query("SELECT p FROM Post p ORDER BY p.pinned DESC, (SELECT COUNT(c) FROM Comment c WHERE c.post = p) DESC, p.createdAt DESC")
    Page<Post> findPostsOrderByCommentCountDesc(Pageable pageable);

    // Category-filtered (categoryId not null)
    Page<Post> findByCategoryIdOrderByPinnedDescCreatedAtDesc(Long categoryId, Pageable pageable);
    Page<Post> findByCategoryIdOrderByPinnedDescCreatedAtAsc(Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.category.id = :categoryId ORDER BY p.pinned DESC, size(p.likes) DESC, p.createdAt DESC")
    Page<Post> findByCategoryIdOrderByLikesDesc(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.category.id = :categoryId ORDER BY p.pinned DESC, (SELECT COUNT(c) FROM Comment c WHERE c.post = p) DESC, p.createdAt DESC")
    Page<Post> findByCategoryIdOrderByCommentCountDesc(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE (LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :q, '%'))) ORDER BY p.pinned DESC, p.createdAt DESC")
    Page<Post> search(@Param("q") String q, Pageable pageable);
}