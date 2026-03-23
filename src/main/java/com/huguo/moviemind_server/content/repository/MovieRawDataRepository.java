package com.huguo.moviemind_server.content.repository;

import com.huguo.moviemind_server.content.model.MovieRawData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MovieRawDataRepository extends JpaRepository<MovieRawData, Long> {
    Optional<MovieRawData> findTopByMovieIdOrderByCrawledAtDesc(Long movieId);
}
