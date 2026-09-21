package com.englishlearn.repository;

import com.englishlearn.entity.ShareRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 内容分享记录仓储。
 */
public interface ShareRecordRepository extends JpaRepository<ShareRecord, Integer> {

    List<ShareRecord> findTop20ByUserIdOrderByCreateTimeDesc(Integer userId);

    long countByChannel(String channel);

    long countByUserId(Integer userId);
}
