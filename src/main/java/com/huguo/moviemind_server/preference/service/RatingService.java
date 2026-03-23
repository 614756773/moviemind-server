package com.huguo.moviemind_server.preference.service;

import com.huguo.moviemind_server.common.dto.PageResponse;
import com.huguo.moviemind_server.common.exception.ResourceNotFoundException;
import com.huguo.moviemind_server.movie.model.Movie;
import com.huguo.moviemind_server.preference.model.Rating;
import com.huguo.moviemind_server.preference.repository.RatingRepository;
import com.huguo.moviemind_server.movie.repository.MovieRepository;
import com.huguo.moviemind_server.auth.repository.UserRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class RatingService {

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;

    @Autowired
    public RatingService(RatingRepository ratingRepository,
                        UserRepository userRepository,
                        MovieRepository movieRepository) {
        this.ratingRepository = ratingRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
    }

    public Page<RatingResponse> getUserRatings(String userId, String search, Pageable pageable) {
        userRepository.findByUsername(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Page<Rating> ratings;
        if (search != null && !search.trim().isEmpty()) {
            ratings = ratingRepository.searchByUserId(userId, search, pageable);
        } else {
            ratings = ratingRepository.findByUserId(userId, pageable);
        }

        return ratings.map(this::convertToResponse);
    }

    public RatingResponse createRating(String userId, RatingRequest request) {
        userRepository.findByUsername(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        // Check if rating already exists
        Optional<Rating> existingRating = ratingRepository.findByUserIdAndMovieId(userId, request.getMovieId());
        if (existingRating.isPresent()) {
            throw new RuntimeException("Rating for this movie already exists");
        }

        Rating rating = new Rating();
        rating.setUserId(userId);
        rating.setMovieId(request.getMovieId());
        rating.setScore(request.getScore());
        List<String> tags = request.getTags();
        if(tags != null && !tags.isEmpty()) {
            rating.setTagStr(String.join(",", tags));
        }
        rating.setNotes(request.getNotes());
        rating.setSource(Rating.RatingSource.MANUAL);
        rating.setRatedAt(LocalDateTime.now());

        rating = ratingRepository.save(rating);
        return convertToResponse(rating);
    }

    public RatingResponse updateRating(Long ratingId, String userId, RatingRequest request) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating not found"));

        // Verify ownership
        if (!rating.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to update this rating");
        }

        if (request.getScore() != null) {
            rating.setScore(request.getScore());
        }
        List<String> tags = request.getTags();
        if(tags != null && !tags.isEmpty()) {
            rating.setTagStr(String.join(",", tags));
        }
        if (request.getNotes() != null) {
            rating.setNotes(request.getNotes());
        }

        rating = ratingRepository.save(rating);
        return convertToResponse(rating);
    }

    public void deleteRating(Long ratingId, String userId) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating not found"));

        // Verify ownership
        if (!rating.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this rating");
        }

        ratingRepository.delete(rating);
    }

    public RatingResponse getRatingById(Long ratingId, String userId) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating not found"));

        // Verify ownership
        if (!rating.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to access this rating");
        }

        return convertToResponse(rating);
    }

    public List<RatingResponse> getUserTopRatings(String userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "score"));
        Page<Rating> ratings = ratingRepository.findByUserId(userId, pageable);
        return ratings.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public long getUserRatingsCount(String userId) {
        return ratingRepository.countByUserId(userId);
    }

    public RatingResponse convertToResponse(Rating rating) {
        RatingResponse response = new RatingResponse();
        response.setId(rating.getId());
        response.setUserId(rating.getUserId());
        response.setMovieId(rating.getMovieId());
        response.setScore(rating.getScore() != null ? rating.getScore().intValue() : null);
        String tagStr = rating.getTagStr();
        if (tagStr != null && !tagStr.isEmpty()) {
            response.setTags(Set.of(tagStr.split(",")));
        }
        response.setNotes(rating.getNotes());
        response.setRatedAt(rating.getRatedAt());
        response.setSource(RatingResponse.RatingSource.valueOf(rating.getSource().name()));
        return response;
    }

    public static class RatingRequest {
        private Long movieId;
        private Integer score;
        private List<String> tags;
        private String notes;

        // Getters and setters
        public Long getMovieId() { return movieId; }
        public void setMovieId(Long movieId) { this.movieId = movieId; }

        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    @Data
    public static class RatingResponse {
        private Long id;
        private String userId;
        private Long movieId;
        private Integer score;
        private Set<String> tags;
        private String notes;
        private LocalDateTime ratedAt;
        private RatingSource source;
        private String movieTitle;
        private Integer movieYear;
        private String moviePoster;
        private List<String> movieGenres;

        public enum RatingSource {
            MANUAL, FROM_WATCHLIST
        }
    }
}
