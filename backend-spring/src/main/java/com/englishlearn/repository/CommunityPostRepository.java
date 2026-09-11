package com.englishlearn.repository;

import com.englishlearn.entity.CommunityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Integer> {

    @Query("select distinct p.topic from CommunityPost p where p.topic is not null and p.topic <> ''")
    List<String> findDistinctTopics();

    @Query("select p from CommunityPost p where p.status = 1 " +
            "and (:topic is null or p.topic = :topic) " +
            "order by p.isTop desc, p.createTime desc")
    Page<CommunityPost> searchPublished(@Param("topic") String topic, Pageable pageable);

    @Query("select p from CommunityPost p where (:status is null or p.status = :status) " +
            "order by p.createTime desc")
    Page<CommunityPost> adminSearch(@Param("status") Integer status, Pageable pageable);

    long countByStatus(Integer status);
}