package com.huguo.moviemind_server.recommendation.repository;

import com.huguo.moviemind_server.recommendation.model.RecommendationFeatureToggle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecommendationFeatureToggleRepository extends JpaRepository<RecommendationFeatureToggle, Long> {
    Optional<RecommendationFeatureToggle> findByUserId(String userId);
}
