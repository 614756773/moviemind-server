package com.huguo.moviemind_server.content.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "movie_raw_data")
@EntityListeners(AuditingEntityListener.class)
@Data
public class MovieRawData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "movie_id")
    private Long movieId;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "release_year")
    private Integer year;

    @Column(name = "raw_json", columnDefinition = "TEXT", nullable = false)
    private String rawJson;

    @CreatedDate
    @Column(name = "crawled_at", nullable = false, updatable = false)
    private LocalDateTime crawledAt;
}
