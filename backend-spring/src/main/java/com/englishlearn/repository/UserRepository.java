package com.englishlearn.repository;

import com.englishlearn.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByPhone(String phone);

    @Query("select u from User u where " +
            "(:keyword is null or u.phone like %:keyword% or u.nickname like %:keyword%) " +
            "and (:role is null or u.userRole = :role) " +
            "order by u.userId desc")
    Page<User> search(@Param("keyword") String keyword, @Param("role") String role, Pageable pageable);
}