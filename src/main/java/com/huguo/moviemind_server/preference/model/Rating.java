package com.huguo.moviemind_server.preference.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "ratings")
@EntityListeners(AuditingEntityListener.class)
@Data
public class Rating {

    public Double getScore() {
        return score != null ? score.doubleValue() : null;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Column(nullable = false)
    private Integer score;

    @Column(name = "tag_str")
    private String tagStr;

    private String notes;

    @Column(name = "rated_at")
    private LocalDateTime ratedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RatingSource source;

    public enum RatingSource {
        MANUAL, FROM_WATCHLIST
    }
}