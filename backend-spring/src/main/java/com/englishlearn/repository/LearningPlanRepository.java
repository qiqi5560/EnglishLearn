package com.englishlearn.repository;

import com.englishlearn.entity.LearningPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearningPlanRepository extends JpaRepository<LearningPlan, Integer> {

    Optional<LearningPlan> findByUserId(Integer userId);

    Optional<LearningPlan> findByUserIdAndPlanStatus(Integer userId, String status);

    List<LearningPlan> findByPlanStatusAndUserIdIn(String status, List<Integer> userIds);
}