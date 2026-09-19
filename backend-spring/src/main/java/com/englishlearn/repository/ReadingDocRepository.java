package com.englishlearn.repository;

import com.englishlearn.entity.ReadingDoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReadingDocRepository extends JpaRepository<ReadingDoc, Integer> {

    List<ReadingDoc> findByOwnerIdOrderByDocIdDesc(Integer ownerId);

    Optional<ReadingDoc> findByDocIdAndOwnerId(Integer docId, Integer ownerId);
}
