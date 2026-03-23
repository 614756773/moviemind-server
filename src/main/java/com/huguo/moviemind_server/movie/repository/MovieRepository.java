package com.huguo.moviemind_server.movie.repository;

import com.huguo.moviemind_server.movie.model.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    Optional<Movie> findByExternalId(String externalId);

    List<Movie> findByTitleContainingIgnoreCase(String title);

    List<Movie> findByYear(Integer year);

    @Query("SELECT m FROM Movie m where m.tagsStr like :tags")
    List<Movie> findByTagsIn(@Param("tags") List<String> tags);

    @Query("SELECT m FROM Movie m")
    Page<Movie> searchMovies(@Param("search") String search, Pageable pageable);

    boolean existsByExternalId(String externalId);
}