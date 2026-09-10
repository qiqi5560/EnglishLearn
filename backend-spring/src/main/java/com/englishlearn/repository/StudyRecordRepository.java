package com.englishlearn.repository;

import com.englishlearn.entity.StudyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StudyRecordRepository extends JpaRepository<StudyRecord, Integer> {

    List<StudyRecord> findByUserIdOrderByLearnDate(Integer userId);

    List<StudyRecord> findByLearnDateGreaterThanEqualOrderByLearnDate(LocalDate start);
}