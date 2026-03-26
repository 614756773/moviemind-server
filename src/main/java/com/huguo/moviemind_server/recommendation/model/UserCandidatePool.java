package com.huguo.moviemind_server.recommendation.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_candidate_pool", indexes = {
        @Index(name = "idx_user_candidate_pool_user_score", columnList = "user_id,score DESC"),
        @Index(name = "idx_user_candidate_pool_user_movie", columnList = "user_id,movie_id", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Data
public class UserCandidatePool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "algorithm_version", nullable = false, length = 50)
    private String algorithmVersion;

    @CreatedDate
    @Column(name = "precomputed_at", nullable = false, updatable = false)
    private LocalDateTime precomputedAt;
}
