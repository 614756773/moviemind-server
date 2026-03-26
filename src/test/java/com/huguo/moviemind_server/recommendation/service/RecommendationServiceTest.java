package com.huguo.moviemind_server.recommendation.service;

import com.huguo.moviemind_server.movie.model.Movie;
import com.huguo.moviemind_server.movie.repository.MovieRepository;
import com.huguo.moviemind_server.preference.model.Rating;
import com.huguo.moviemind_server.preference.repository.RatingRepository;
import com.huguo.moviemind_server.recommendation.dto.RecommendationDto;
import com.huguo.moviemind_server.recommendation.model.RecommendationEvent;
import com.huguo.moviemind_server.recommendation.model.RecommendationFeatureToggle;
import com.huguo.moviemind_server.recommendation.model.RecommendationItem;
import com.huguo.moviemind_server.recommendation.model.UserCandidatePool;
import com.huguo.moviemind_server.recommendation.repository.RecommendationEventRepository;
import com.huguo.moviemind_server.recommendation.repository.RecommendationFeatureToggleRepository;
import com.huguo.moviemind_server.recommendation.repository.UserCandidatePoolRepository;
import com.huguo.moviemind_server.watchlist.model.WatchlistItem;
import com.huguo.moviemind_server.watchlist.repository.WatchlistItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock private MovieRepository movieRepository;
    @Mock private RatingRepository ratingRepository;
    @Mock private WatchlistItemRepository watchlistItemRepository;
    @Mock private RecommendationEventRepository recommendationEventRepository;
    @Mock private RecommendationFeatureToggleRepository recommendationFeatureToggleRepository;
    @Mock private UserCandidatePoolRepository userCandidatePoolRepository;

    private RecommendationService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationService(
                movieRepository,
                ratingRepository,
                watchlistItemRepository,
                recommendationEventRepository,
                recommendationFeatureToggleRepository,
                userCandidatePoolRepository
        );
    }

    @Test
    void generateRecommendations_shouldFilterRatedAndWatchlistMovies() {
        Rating rating = new Rating();
        rating.setMovieId(1L);
        rating.setScore(9);
        rating.setTagStr("悬疑,烧脑");

        Movie ratedMovie = movie(1L, "盗梦空间", "科幻", "烧脑", 9.3);
        Movie candidate1 = movie(2L, "星际穿越", "科幻", "烧脑", 9.4);
        Movie candidate2 = movie(3L, "当幸福来敲门", "剧情", "励志", 8.9);

        WatchlistItem inWatchlist = new WatchlistItem();
        inWatchlist.setMovieId(3L);

        when(recommendationFeatureToggleRepository.findByUserId("u1")).thenReturn(Optional.empty());
        when(ratingRepository.findByUserId("u1")).thenReturn(List.of(rating));
        when(movieRepository.findAllById(Set.of(1L))).thenReturn(List.of(ratedMovie));
        when(movieRepository.findAll()).thenReturn(List.of(ratedMovie, candidate1, candidate2));
        when(watchlistItemRepository.findByUserId("u1")).thenReturn(List.of(inWatchlist));
        when(recommendationEventRepository.save(any(RecommendationEvent.class))).thenAnswer(invocation -> {
            RecommendationEvent event = invocation.getArgument(0);
            event.setId(100L);
            return event;
        });

        RecommendationDto dto = service.generateRecommendations("u1", 10);

        assertEquals(100L, dto.getEventId());
        assertEquals(1, dto.getItems().size());
        assertEquals(2L, dto.getItems().getFirst().getMovieId());
        assertFalse(dto.getItems().getFirst().getAiReason().isBlank());
    }

    @Test
    void generateRecommendations_shouldUseCandidatePoolWhenEnabled() {
        RecommendationFeatureToggle toggle = new RecommendationFeatureToggle();
        toggle.setUserId("u1");
        toggle.setCandidatePoolEnabled(true);

        Movie candidate = movie(2L, "星际穿越", "科幻", "烧脑", 9.4);

        UserCandidatePool pool = new UserCandidatePool();
        pool.setUserId("u1");
        pool.setMovieId(2L);
        pool.setScore(8.6);

        when(recommendationFeatureToggleRepository.findByUserId("u1")).thenReturn(Optional.of(toggle));
        when(ratingRepository.findByUserId("u1")).thenReturn(List.of());
        when(watchlistItemRepository.findByUserId("u1")).thenReturn(List.of());
        when(userCandidatePoolRepository.findByUserIdOrderByScoreDesc("u1")).thenReturn(List.of(pool));
        when(movieRepository.findAllById(Set.of(2L))).thenReturn(List.of(candidate));
        when(recommendationEventRepository.save(any(RecommendationEvent.class))).thenAnswer(invocation -> {
            RecommendationEvent event = invocation.getArgument(0);
            event.setId(200L);
            return event;
        });

        RecommendationDto dto = service.generateRecommendations("u1", 10);

        assertEquals(1, dto.getItems().size());
        assertEquals(2L, dto.getItems().getFirst().getMovieId());
        verify(movieRepository, never()).findAll();
    }

    @Test
    void submitFeedback_adopted_shouldCreateWatchlistItem() {
        RecommendationEvent event = new RecommendationEvent();
        event.setId(1L);
        event.setUserId("u1");

        RecommendationItem item = new RecommendationItem();
        item.setMovieId(99L);
        item.setAiReason("推荐理由");
        event.addItem(item);

        when(recommendationEventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(watchlistItemRepository.existsByUserIdAndMovieId("u1", 99L)).thenReturn(false);

        service.submitFeedback("u1", 1L, 99L, RecommendationItem.FeedbackType.ADOPTED);

        ArgumentCaptor<WatchlistItem> captor = ArgumentCaptor.forClass(WatchlistItem.class);
        verify(watchlistItemRepository).save(captor.capture());
        assertEquals(99L, captor.getValue().getMovieId());
        assertEquals(WatchlistItem.WatchlistStatus.PENDING, captor.getValue().getStatus());
    }

    private Movie movie(Long id, String title, String genres, String tags, Double ratingExternal) {
        Movie movie = new Movie();
        movie.setId(id);
        movie.setTitle(title);
        movie.setGenresStr(genres);
        movie.setTagsStr(tags);
        movie.setRatingExternal(ratingExternal);
        return movie;
    }
}
