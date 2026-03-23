package com.huguo.moviemind_server.content.repository;

import com.huguo.moviemind_server.content.model.MovieFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MovieFeatureRepository extends JpaRepository<MovieFeature, Long> {
    Optional<MovieFeature> findByMovieId(Long movieId);
}
