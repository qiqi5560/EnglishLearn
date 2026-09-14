package com.englishlearn.repository;

import com.englishlearn.entity.DailyTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyTaskRepository extends JpaRepository<DailyTask, Integer> {

    List<DailyTask> findByUserIdAndTaskDateOrderByTaskId(Integer userId, LocalDate taskDate);

    boolean existsByUserIdAndTaskDateAndTitle(Integer userId, LocalDate taskDate, String title);

    Optional<DailyTask> findByUserIdAndTaskDateAndSceneId(Integer userId, LocalDate taskDate, Integer sceneId);

    long countByUserIdAndTaskDateAndTaskType(Integer userId, LocalDate taskDate, String taskType);
}
