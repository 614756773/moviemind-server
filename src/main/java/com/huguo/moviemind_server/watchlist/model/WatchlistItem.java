package com.huguo.moviemind_server.watchlist.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "watchlist_items")
@EntityListeners(AuditingEntityListener.class)
@Data
public class WatchlistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WatchlistStatus status;

    @Column(name = "added_at")
    private LocalDateTime addedAt;

    @Column(name = "watched_at")
    private LocalDateTime watchedAt;

    @Column(name = "rating_id")
    private Long ratingId;

    @Column(name = "ai_reason_snapshot", columnDefinition = "TEXT")
    private String aiReasonSnapshot;


    public void markAsWatched() {
        this.status = WatchlistStatus.WATCHED;
        this.watchedAt = LocalDateTime.now();
    }

    public enum WatchlistStatus {
        PENDING, WATCHED
    }
}