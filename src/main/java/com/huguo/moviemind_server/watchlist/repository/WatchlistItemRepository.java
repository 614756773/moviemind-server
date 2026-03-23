package com.huguo.moviemind_server.watchlist.repository;

import com.huguo.moviemind_server.watchlist.model.WatchlistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {

    List<WatchlistItem> findByUserId(String userId);

    Page<WatchlistItem> findByUserId(String userId, Pageable pageable);

    Optional<WatchlistItem> findByUserIdAndMovieId(String userId, Long movieId);

    List<WatchlistItem> findByUserIdAndStatus(String userId, WatchlistItem.WatchlistStatus status);

    Page<WatchlistItem> findByUserIdAndStatus(String userId, WatchlistItem.WatchlistStatus status, Pageable pageable);

    @Query("SELECT w FROM WatchlistItem w WHERE w.userId = :userId AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(w.movie.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<WatchlistItem> searchByUserId(@Param("userId") String userId, @Param("search") String search, Pageable pageable);

    boolean existsByUserIdAndMovieId(String userId, Long movieId);

    long countByUserIdAndStatus(String userId, WatchlistItem.WatchlistStatus status);
}