package com.englishlearn.repository;

import com.englishlearn.entity.CommunityComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Integer> {

    List<CommunityComment> findByPostIdOrderByCommentIdAsc(Integer postId);
}