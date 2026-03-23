package com.huguo.moviemind_server.recommendation.service;

import com.huguo.moviemind_server.common.exception.ResourceNotFoundException;
import com.huguo.moviemind_server.movie.model.Movie;
import com.huguo.moviemind_server.movie.repository.MovieRepository;
import com.huguo.moviemind_server.preference.model.Rating;
import com.huguo.moviemind_server.preference.repository.RatingRepository;
import com.huguo.moviemind_server.recommendation.dto.RecommendationDto;
import com.huguo.moviemind_server.recommendation.model.RecommendationEvent;
import com.huguo.moviemind_server.recommendation.model.RecommendationItem;
import com.huguo.moviemind_server.recommendation.repository.RecommendationEventRepository;
import com.huguo.moviemind_server.watchlist.model.WatchlistItem;
import com.huguo.moviemind_server.watchlist.repository.WatchlistItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class RecommendationService {

    private static final String ALGORITHM_VERSION = "local-genre-tag-v1";

    private final MovieRepository movieRepository;
    private final RatingRepository ratingRepository;
    private final WatchlistItemRepository watchlistItemRepository;
    private final RecommendationEventRepository recommendationEventRepository;

    public RecommendationService(MovieRepository movieRepository,
                                 RatingRepository ratingRepository,
                                 WatchlistItemRepository watchlistItemRepository,
                                 RecommendationEventRepository recommendationEventRepository) {
        this.movieRepository = movieRepository;
        this.ratingRepository = ratingRepository;
        this.watchlistItemRepository = watchlistItemRepository;
        this.recommendationEventRepository = recommendationEventRepository;
    }

    public RecommendationDto generateRecommendations(String userId, int limit) {
        List<Rating> ratings = ratingRepository.findByUserId(userId);
        Set<Long> ratedMovieIds = ratings.stream().map(Rating::getMovieId).collect(Collectors.toSet());
        Set<Long> watchlistMovieIds = watchlistItemRepository.findByUserId(userId).stream()
                .map(WatchlistItem::getMovieId)
                .collect(Collectors.toSet());

        Map<Long, Movie> movieById = movieRepository.findAllById(
                ratings.stream().map(Rating::getMovieId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Movie::getId, Function.identity()));

        Map<String, Double> genreWeights = buildGenreWeights(ratings, movieById);
        Map<String, Double> tagWeights = buildPreferenceWeights(ratings, Rating::getTagStr);

        List<Movie> candidates = movieRepository.findAll().stream()
                .filter(movie -> !ratedMovieIds.contains(movie.getId()))
                .filter(movie -> !watchlistMovieIds.contains(movie.getId()))
                .toList();

        List<ScoredMovie> scoredMovies = candidates.stream()
                .map(movie -> scoreMovie(movie, genreWeights, tagWeights))
                .sorted(Comparator.comparing(ScoredMovie::score).reversed())
                .limit(limit)
                .toList();

        RecommendationEvent event = new RecommendationEvent();
        event.setUserId(userId);
        event.setAlgorithmVersion(ALGORITHM_VERSION);

        List<RecommendationDto.RecommendationMovie> dtoItems = new ArrayList<>();
        int rank = 1;
        for (ScoredMovie scored : scoredMovies) {
            RecommendationItem item = new RecommendationItem();
            item.setMovieId(scored.movie().getId());
            item.setRankLocal(rank);
            item.setRankFinal(rank);
            item.setScoreLocal(scored.score());
            item.setAiReason(buildAiReason(scored.movie(), scored.score()));
            event.addItem(item);

            dtoItems.add(RecommendationDto.RecommendationMovie.builder()
                    .movieId(scored.movie().getId())
                    .title(scored.movie().getTitle())
                    .year(scored.movie().getYear())
                    .genres(scored.movie().getGenresStr())
                    .posterUrl(scored.movie().getPosterUrl())
                    .scoreLocal(scored.score())
                    .aiReason(item.getAiReason())
                    .build());
            rank++;
        }

        RecommendationEvent saved = recommendationEventRepository.save(event);

        return RecommendationDto.builder()
                .eventId(saved.getId())
                .algorithmVersion(ALGORITHM_VERSION)
                .items(dtoItems)
                .build();
    }

    public void submitFeedback(String userId, Long eventId, Long movieId, RecommendationItem.FeedbackType feedbackType) {
        RecommendationEvent event = recommendationEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation event not found: " + eventId));

        if (!event.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Event does not belong to current user");
        }

        RecommendationItem item = event.getItems().stream()
                .filter(i -> i.getMovieId().equals(movieId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found in recommendation event"));

        item.setFeedback(feedbackType);
        item.setFeedbackAt(LocalDateTime.now());

        if (feedbackType == RecommendationItem.FeedbackType.ADOPTED
                && !watchlistItemRepository.existsByUserIdAndMovieId(userId, movieId)) {
            WatchlistItem watchlistItem = new WatchlistItem();
            watchlistItem.setUserId(userId);
            watchlistItem.setMovieId(movieId);
            watchlistItem.setStatus(WatchlistItem.WatchlistStatus.PENDING);
            watchlistItem.setAddedAt(LocalDateTime.now());
            watchlistItem.setAiReasonSnapshot(item.getAiReason());
            watchlistItemRepository.save(watchlistItem);
        }

        recommendationEventRepository.save(event);
    }

    private Map<String, Double> buildPreferenceWeights(List<Rating> ratings, Function<Rating, String> valueExtractor) {
        Map<String, Double> weights = new HashMap<>();
        for (Rating rating : ratings) {
            String values = valueExtractor.apply(rating);
            if (values == null || values.isBlank()) {
                continue;
            }
            double weight = Math.max(0.0, rating.getScore() - 5.0);
            for (String value : values.split(",")) {
                String key = value.trim();
                if (!key.isEmpty()) {
                    weights.merge(key, weight, Double::sum);
                }
            }
        }
        return weights;
    }

    private Map<String, Double> buildGenreWeights(List<Rating> ratings, Map<Long, Movie> movieById) {
        Map<String, Double> weights = new HashMap<>();
        for (Rating rating : ratings) {
            Movie movie = movieById.get(rating.getMovieId());
            if (movie == null || movie.getGenresStr() == null || movie.getGenresStr().isBlank()) {
                continue;
            }
            double weight = Math.max(0.0, rating.getScore() - 5.0);
            for (String genre : movie.getGenresStr().split(",")) {
                String key = genre.trim();
                if (!key.isEmpty()) {
                    weights.merge(key, weight, Double::sum);
                }
            }
        }
        return weights;
    }

    private ScoredMovie scoreMovie(Movie movie, Map<String, Double> genreWeights, Map<String, Double> tagWeights) {
        double genreScore = sumWeights(movie.getGenresStr(), genreWeights);
        double tagScore = sumWeights(movie.getTagsStr(), tagWeights);
        double ratingScore = movie.getRatingExternal() == null ? 0.0 : movie.getRatingExternal() / 2.0;
        double total = genreScore * 0.45 + tagScore * 0.35 + ratingScore * 0.20;
        return new ScoredMovie(movie, total);
    }

    private double sumWeights(String csvValues, Map<String, Double> weights) {
        if (csvValues == null || csvValues.isBlank()) {
            return 0.0;
        }
        double score = 0.0;
        for (String part : csvValues.split(",")) {
            score += weights.getOrDefault(part.trim(), 0.0);
        }
        return score;
    }

    private String buildAiReason(Movie movie, double score) {
        String genres = movie.getGenresStr() == null ? "多元" : movie.getGenresStr();
        return String.format("基于你近期偏好，本片在%s维度匹配较高（本地分 %.2f）", genres, score);
    }

    private record ScoredMovie(Movie movie, double score) {
    }
}
