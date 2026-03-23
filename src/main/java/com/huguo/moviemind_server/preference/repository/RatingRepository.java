package com.huguo.moviemind_server.preference.repository;

import com.huguo.moviemind_server.preference.model.Rating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByUserId(String userId);

    Page<Rating> findByUserId(String userId, Pageable pageable);

    Optional<Rating> findByUserIdAndMovieId(String userId, Long movieId);

    Optional<Rating> findByUserIdAndMovieIdAndSource(String userId, Long movieId, Rating.RatingSource source);

    List<Rating> findByUserIdAndScoreGreaterThanEqual(String userId, Integer minScore);

    List<Rating> findByUserIdAndScoreLessThanEqual(String userId, Integer maxScore);

    @Query("SELECT r FROM Rating r WHERE r.userId = :userId AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(r.notes) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "EXISTS (SELECT 1 FROM r.tags t WHERE LOWER(t) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<Rating> searchByUserId(@Param("userId") String userId, @Param("search") String search, Pageable pageable);

    List<Rating> findByMovieId(Long movieId);

    long countByUserId(String userId);
}