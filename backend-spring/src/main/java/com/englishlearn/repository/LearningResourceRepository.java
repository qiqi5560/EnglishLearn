package com.englishlearn.repository;

import com.englishlearn.entity.LearningResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LearningResourceRepository extends JpaRepository<LearningResource, Integer> {

    @Query("select r from LearningResource r where r.status = 1 " +
            "and (:type is null or r.resType = :type) " +
            "and (:category is null or r.category = :category) " +
            "and (:level is null or r.level = :level) " +
            "and (:keyword is null or r.title like %:keyword%) " +
            "order by r.resourceId")
    Page<LearningResource> search(@Param("type") String type,
                                  @Param("category") String category,
                                  @Param("level") String level,
                                  @Param("keyword") String keyword,
                                  Pageable pageable);

    Optional<LearningResource> findFirstByTitleContainingAndStatus(String title, Integer status);

    @Query("select r from LearningResource r where (:keyword is null or r.title like %:keyword%) " +
            "order by r.resourceId desc")
    Page<LearningResource> adminSearch(@Param("keyword") String keyword, Pageable pageable);

    long countByStatus(Integer status);
}