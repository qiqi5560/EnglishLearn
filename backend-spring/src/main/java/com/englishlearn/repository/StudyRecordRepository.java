package com.englishlearn.repository;

import com.englishlearn.entity.StudyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StudyRecordRepository extends JpaRepository<StudyRecord, Integer> {

    /** 统计区间内的学习记录（指标量化：任务点击判定、活跃用户） */
    List<StudyRecord> findByLearnDateBetween(LocalDate start, LocalDate end);


    List<StudyRecord> findByUserIdOrderByLearnDate(Integer userId);

    List<StudyRecord> findByLearnDateGreaterThanEqualOrderByLearnDate(LocalDate start);
}