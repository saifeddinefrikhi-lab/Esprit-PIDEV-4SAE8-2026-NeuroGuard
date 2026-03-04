package com.neuroguard.forumsservice.repository;

import com.neuroguard.forumsservice.entity.PostShare;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PostShareRepository extends JpaRepository<PostShare, Long> {
    Optional<PostShare> findByPostIdAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    int countByPostId(Long postId);
}