package com.englishlearn.repository;

import com.englishlearn.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByPhone(String phone);

    @Query("select u from User u where " +
            "(:keyword is null or u.phone like %:keyword% or u.nickname like %:keyword%) " +
            "and (:role is null or u.userRole = :role) " +
            "and (:punished is null " +
            "     or (:punished = 1 and u.banUntil is not null and u.banUntil > :now) " +
            "     or (:punished = 0 and (u.banUntil is null or u.banUntil <= :now))) " +
            "order by u.userId desc")
    Page<User> search(@Param("keyword") String keyword, @Param("role") String role,
                      @Param("punished") Integer punished, @Param("now") LocalDateTime now,
                      Pageable pageable);

    /** 注册时间晚于某时刻的用户（管理后台：新增注册统计） */
    List<User> findByRegisterTimeGreaterThanEqual(LocalDateTime start);
}