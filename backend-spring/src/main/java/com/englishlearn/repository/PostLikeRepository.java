package com.englishlearn.repository;

import com.englishlearn.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Integer> {

    Optional<PostLike> findByPostIdAndUserId(Integer postId, Integer userId);

    List<PostLike> findByUserIdAndPostIdIn(Integer userId, List<Integer> postIds);

    boolean existsByPostIdAndUserId(Integer postId, Integer userId);

    long deleteByPostId(Integer postId);
}