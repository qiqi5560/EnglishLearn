package com.englishlearn.repository;

import com.englishlearn.entity.DailyTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DailyTaskRepository extends JpaRepository<DailyTask, Integer> {

    List<DailyTask> findByUserIdAndTaskDateOrderByTaskId(Integer userId, LocalDate taskDate);

    boolean existsByUserIdAndTaskDateAndTitle(Integer userId, LocalDate taskDate, String title);
}