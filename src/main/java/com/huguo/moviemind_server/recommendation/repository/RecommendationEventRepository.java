package com.huguo.moviemind_server.recommendation.repository;

import com.huguo.moviemind_server.recommendation.model.RecommendationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationEventRepository extends JpaRepository<RecommendationEvent, Long> {
    List<RecommendationEvent> findByUserIdOrderByGeneratedAtDesc(String userId);
}
