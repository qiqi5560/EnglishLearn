package com.englishlearn.repository;

import com.englishlearn.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * 关注关系仓储（follower 关注 followee）。
 */
public interface UserFollowRepository extends JpaRepository<UserFollow, Integer> {

    boolean existsByFollowerIdAndFolloweeId(Integer followerId, Integer followeeId);

    long countByFolloweeId(Integer followeeId);

    long countByFollowerId(Integer followerId);

    List<UserFollow> findTop100ByFollowerIdOrderByFollowIdDesc(Integer followerId);

    List<UserFollow> findTop100ByFolloweeIdOrderByFollowIdDesc(Integer followeeId);

    long deleteByFollowerIdAndFolloweeId(Integer followerId, Integer followeeId);

    /** 批量判断关注状态（列表页一次性拿到已关注的用户） */
    @Query("select f.followeeId from UserFollow f where f.followerId = :followerId and f.followeeId in :ids")
    List<Integer> followeeIdsIn(@Param("followerId") Integer followerId, @Param("ids") Collection<Integer> ids);
}
