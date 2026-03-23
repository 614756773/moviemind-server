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

    @Query("SELECT m FROM Movie m WHERE LOWER(COALESCE(m.tagsStr, '')) LIKE LOWER(CONCAT('%', :tagName, '%'))")
    List<Movie> findByTagName(@Param("tagName") String tagName);

    @Query("SELECT m FROM Movie m WHERE LOWER(COALESCE(m.genresStr, '')) LIKE LOWER(CONCAT('%', :genreName, '%'))")
    List<Movie> findByGenreName(@Param("genreName") String genreName);

    @Query("SELECT m FROM Movie m")
    Page<Movie> searchMovies(@Param("search") String search, Pageable pageable);

    boolean existsByExternalId(String externalId);
}
