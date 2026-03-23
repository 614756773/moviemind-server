package com.huguo.moviemind_server.content.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "movie_features", uniqueConstraints = {
        @UniqueConstraint(name = "uk_movie_features_movie_id", columnNames = "movie_id")
})
@EntityListeners(AuditingEntityListener.class)
@Data
public class MovieFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Column(name = "sentiment_score")
    private Double sentimentScore;

    @Column(name = "themes", columnDefinition = "TEXT")
    private String themes;

    @Column(name = "mood_tags", columnDefinition = "TEXT")
    private String moodTags;

    @Column(name = "style_tags", columnDefinition = "TEXT")
    private String styleTags;

    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords;

    @Column(name = "data_source", length = 50)
    private String dataSource;

    @Column(name = "algorithm_version", length = 50)
    private String algorithmVersion;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
