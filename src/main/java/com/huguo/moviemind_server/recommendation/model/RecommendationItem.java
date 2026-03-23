package com.huguo.moviemind_server.recommendation.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_items")
@Data
public class RecommendationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private RecommendationEvent event;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Column(name = "rank_local")
    private Integer rankLocal;

    @Column(name = "score_local")
    private Double scoreLocal;

    @Column(name = "rank_final")
    private Integer rankFinal;

    @Column(name = "ai_reason", columnDefinition = "TEXT")
    private String aiReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback", length = 20)
    private FeedbackType feedback;

    @Column(name = "feedback_at")
    private LocalDateTime feedbackAt;

    public enum FeedbackType {
        ADOPTED,
        REJECTED,
        IGNORED
    }
}
