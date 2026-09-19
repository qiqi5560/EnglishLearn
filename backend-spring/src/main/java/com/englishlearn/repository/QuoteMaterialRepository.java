package com.englishlearn.repository;

import com.englishlearn.entity.QuoteMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuoteMaterialRepository extends JpaRepository<QuoteMaterial, Integer> {

    /** 内置素材 + 当前用户自建素材，可按分类/等级/关键词过滤 */
    @Query("select q from QuoteMaterial q where (q.builtin = 1 or q.ownerId = :userId) " +
            "and (:category is null or q.category = :category) " +
            "and (:level is null or q.level = :level) " +
            "and (:keyword is null or q.title like %:keyword% or q.textEn like %:keyword% or q.source like %:keyword%) " +
            "order by q.builtin desc, q.quoteId")
    List<QuoteMaterial> search(@Param("userId") Integer userId,
                               @Param("category") String category,
                               @Param("level") String level,
                               @Param("keyword") String keyword);

    long countByBuiltin(Integer builtin);
}
