package com.huguo.moviemind_server.watchlist.service;

import com.huguo.moviemind_server.auth.model.User;
import com.huguo.moviemind_server.auth.repository.UserRepository;
import com.huguo.moviemind_server.common.exception.ResourceNotFoundException;
import com.huguo.moviemind_server.movie.model.Movie;
import com.huguo.moviemind_server.movie.repository.MovieRepository;
import com.huguo.moviemind_server.preference.model.Rating;
import com.huguo.moviemind_server.preference.repository.RatingRepository;
import com.huguo.moviemind_server.watchlist.model.WatchlistItem;
import com.huguo.moviemind_server.watchlist.repository.WatchlistItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class WatchlistService {

    private final WatchlistItemRepository watchlistItemRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final RatingRepository ratingRepository;

    @Autowired
    public WatchlistService(WatchlistItemRepository watchlistItemRepository,
                            UserRepository userRepository,
                            MovieRepository movieRepository,
                            RatingRepository ratingRepository) {
        this.watchlistItemRepository = watchlistItemRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.ratingRepository = ratingRepository;
    }

    public Page<WatchlistResponse> getUserWatchlist(String username, String status, String search, Pageable pageable) {
        Page<WatchlistItem> items;
        WatchlistItem.WatchlistStatus statusFilter = status != null ?
                WatchlistItem.WatchlistStatus.valueOf(status.toUpperCase()) : null;

        if (statusFilter != null) {
            if (search != null && !search.trim().isEmpty()) {
                items = watchlistItemRepository.searchByUserId(username, search, pageable);
            } else {
                items = watchlistItemRepository.findByUserIdAndStatus(username, statusFilter, pageable);
            }
        } else {
            if (search != null && !search.trim().isEmpty()) {
                items = watchlistItemRepository.searchByUserId(username, search, pageable);
            } else {
                items = watchlistItemRepository.findByUserId(username, pageable);
            }
        }

        return items.map(this::convertToResponse);
    }

    public WatchlistResponse addToWatchlist(String username, Long movieId, String aiReason) {
        // Verify user exists
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify movie exists
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        // Check if already in watchlist
        if (watchlistItemRepository.existsByUserIdAndMovieId(username, movieId)) {
            throw new RuntimeException("Movie already in watchlist");
        }

        WatchlistItem item = new WatchlistItem();
        item.setUserId(user.getUsername());
        item.setMovieId(movieId);
        item.setStatus(WatchlistItem.WatchlistStatus.PENDING);
        item.setAddedAt(LocalDateTime.now());
        item.setAiReasonSnapshot(aiReason);

        item = watchlistItemRepository.save(item);
        return convertToResponse(item);
    }

    public WatchlistResponse markAsWatched(Long itemId, String username, WatchlistItemMarkWatchedRequest request) {
        WatchlistItem item = watchlistItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist item not found"));

        // Verify ownership
        if (!item.getUserId().equals(username)) {
            throw new RuntimeException("Unauthorized to update this item");
        }

        // Mark as watched
        item.markAsWatched();

        // Create rating if provided
        if (request != null && request.getRating() != null) {
            Rating rating = new Rating();
            rating.setUserId(username);
            rating.setMovieId(item.getMovieId());
            rating.setScore(request.getRating().getScore());
            rating.setNotes(request.getRating().getNotes());
            rating.setSource(Rating.RatingSource.FROM_WATCHLIST);
            rating.setRatedAt(LocalDateTime.now());

            rating = ratingRepository.save(rating);
            item.setRatingId(rating.getId());
        }

        item = watchlistItemRepository.save(item);
        return convertToResponse(item);
    }

    public void removeFromWatchlist(Long itemId, String username) {
        WatchlistItem item = watchlistItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist item not found"));

        // Verify ownership
        if (!item.getUserId().equals(username)) {
            throw new RuntimeException("Unauthorized to delete this item");
        }

        watchlistItemRepository.delete(item);
    }

    public WatchlistResponse getWatchlistItem(Long itemId, String username) {
        WatchlistItem item = watchlistItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist item not found"));

        // Verify ownership
        if (!item.getUserId().equals(username)) {
            throw new RuntimeException("Unauthorized to access this item");
        }

        return convertToResponse(item);
    }

    public WatchlistStats getWatchlistStats(String username) {
        long pendingCount = watchlistItemRepository.countByUserIdAndStatus(username, WatchlistItem.WatchlistStatus.PENDING);
        long watchedCount = watchlistItemRepository.countByUserIdAndStatus(username, WatchlistItem.WatchlistStatus.WATCHED);

        return new WatchlistStats(pendingCount, watchedCount);
    }

    public WatchlistResponse convertToResponse(WatchlistItem item) {
        WatchlistResponse response = new WatchlistResponse();
        response.setId(item.getId());
        response.setUserId(item.getUserId());
        response.setMovieId(item.getMovieId());
        response.setStatus(item.getStatus());
        response.setAddedAt(item.getAddedAt());
        response.setWatchedAt(item.getWatchedAt());
        response.setAiReasonSnapshot(item.getAiReasonSnapshot());

        return response;
    }

    public static class WatchlistItemAddRequest {
        private Long movieId;
        private String aiReason;

        public Long getMovieId() {
            return movieId;
        }

        public void setMovieId(Long movieId) {
            this.movieId = movieId;
        }

        public String getAiReason() {
            return aiReason;
        }

        public void setAiReason(String aiReason) {
            this.aiReason = aiReason;
        }
    }

    public static class WatchlistItemMarkWatchedRequest {
        private RatingRequest rating;

        public RatingRequest getRating() {
            return rating;
        }

        public void setRating(RatingRequest rating) {
            this.rating = rating;
        }
    }

    public static class RatingRequest {
        private Integer score;
        private List<String> tags;
        private String notes;

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    public static class RatingResponse {
        private Long id;
        private Integer score;
        private Set<String> tags;
        private String notes;
        private LocalDateTime ratedAt;

        public RatingResponse(Long id, Integer score, Set<String> tags, String notes, LocalDateTime ratedAt) {
            this.id = id;
            this.score = score;
            this.tags = tags;
            this.notes = notes;
            this.ratedAt = ratedAt;
        }

        // Getters
        public Long getId() {
            return id;
        }

        public Integer getScore() {
            return score;
        }

        public Set<String> getTags() {
            return tags;
        }

        public String getNotes() {
            return notes;
        }

        public LocalDateTime getRatedAt() {
            return ratedAt;
        }
    }

    public static class WatchlistResponse {
        private Long id;
        private String userId;
        private Long movieId;
        private WatchlistItem.WatchlistStatus status;
        private LocalDateTime addedAt;
        private LocalDateTime watchedAt;
        private String aiReasonSnapshot;
        private String movieTitle;
        private Integer movieYear;
        private String moviePoster;
        private List<String> movieGenres;
        private RatingResponse rating;

        // Getters and setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public Long getMovieId() {
            return movieId;
        }

        public void setMovieId(Long movieId) {
            this.movieId = movieId;
        }

        public WatchlistItem.WatchlistStatus getStatus() {
            return status;
        }

        public void setStatus(WatchlistItem.WatchlistStatus status) {
            this.status = status;
        }

        public LocalDateTime getAddedAt() {
            return addedAt;
        }

        public void setAddedAt(LocalDateTime addedAt) {
            this.addedAt = addedAt;
        }

        public LocalDateTime getWatchedAt() {
            return watchedAt;
        }

        public void setWatchedAt(LocalDateTime watchedAt) {
            this.watchedAt = watchedAt;
        }

        public String getAiReasonSnapshot() {
            return aiReasonSnapshot;
        }

        public void setAiReasonSnapshot(String aiReasonSnapshot) {
            this.aiReasonSnapshot = aiReasonSnapshot;
        }

        public String getMovieTitle() {
            return movieTitle;
        }

        public void setMovieTitle(String movieTitle) {
            this.movieTitle = movieTitle;
        }

        public Integer getMovieYear() {
            return movieYear;
        }

        public void setMovieYear(Integer movieYear) {
            this.movieYear = movieYear;
        }

        public String getMoviePoster() {
            return moviePoster;
        }

        public void setMoviePoster(String moviePoster) {
            this.moviePoster = moviePoster;
        }

        public List<String> getMovieGenres() {
            return movieGenres;
        }

        public void setMovieGenres(List<String> movieGenres) {
            this.movieGenres = movieGenres;
        }

        public RatingResponse getRating() {
            return rating;
        }

        public void setRating(RatingResponse rating) {
            this.rating = rating;
        }
    }

    public static class WatchlistStats {
        private long pendingCount;
        private long watchedCount;

        public WatchlistStats(long pendingCount, long watchedCount) {
            this.pendingCount = pendingCount;
            this.watchedCount = watchedCount;
        }

        public long getPendingCount() {
            return pendingCount;
        }

        public long getWatchedCount() {
            return watchedCount;
        }

        public long getTotalCount() {
            return pendingCount + watchedCount;
        }
    }
}
