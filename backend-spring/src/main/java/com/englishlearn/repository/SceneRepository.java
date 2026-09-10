package com.englishlearn.repository;

import com.englishlearn.entity.Scene;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SceneRepository extends JpaRepository<Scene, Integer> {

    @Query("select s from Scene s where s.status = 1 " +
            "and (:category is null or s.sceneCategory = :category) " +
            "and (:level is null or s.levelScope = :level) " +
            "and (:keyword is null or s.sceneName like %:keyword%) " +
            "order by s.sceneId")
    Page<Scene> search(@Param("category") String category,
                       @Param("level") String level,
                       @Param("keyword") String keyword,
                       Pageable pageable);

    List<Scene> findByStatusOrderBySceneId(Integer status);

    Optional<Scene> findFirstBySceneNameAndStatus(String sceneName, Integer status);
}