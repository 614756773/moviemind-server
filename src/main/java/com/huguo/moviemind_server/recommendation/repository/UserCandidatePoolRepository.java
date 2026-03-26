package com.huguo.moviemind_server.recommendation.repository;

import com.huguo.moviemind_server.recommendation.model.UserCandidatePool;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCandidatePoolRepository extends JpaRepository<UserCandidatePool, Long> {
    List<UserCandidatePool> findByUserIdOrderByScoreDesc(String userId);

    long countByUserId(String userId);

    void deleteByUserId(String userId);
}
