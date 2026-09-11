package com.englishlearn.repository;

import com.englishlearn.entity.UserPartner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPartnerRepository extends JpaRepository<UserPartner, Integer> {

    List<UserPartner> findByUserIdOrderByIdAsc(Integer userId);

    boolean existsByUserIdAndPartnerUserId(Integer userId, Integer partnerUserId);

    void deleteByUserIdAndPartnerUserId(Integer userId, Integer partnerUserId);
}