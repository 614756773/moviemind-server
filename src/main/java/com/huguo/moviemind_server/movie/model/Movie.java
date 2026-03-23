package com.huguo.moviemind_server.movie.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "movies")
@EntityListeners(AuditingEntityListener.class)
@Data
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 100)
    private String externalId;

    @Column(nullable = false, length = 200)
    private String title;

    private Integer year;

    @Column(name = "genres_str")
    private String genresStr;

    @Column(name = "tags_str")
    private String tagsStr;

    private String posterUrl;

    private Double ratingExternal;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}