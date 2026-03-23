package com.huguo.moviemind_server.preference.service;

import com.huguo.moviemind_server.movie.repository.MovieRepository;
import com.huguo.moviemind_server.preference.model.Rating;
import com.huguo.moviemind_server.preference.repository.RatingRepository;
import com.huguo.moviemind_server.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MovieRepository movieRepository;

    private RatingService ratingService;

    @BeforeEach
    void setUp() {
        ratingService = new RatingService(ratingRepository, userRepository, movieRepository);
    }

    @Test
    void convertToResponse_shouldDeduplicateTagsWithoutException() {
        Rating rating = buildRatingWithTags("drama, drama,comedy");

        RatingService.RatingResponse response = ratingService.convertToResponse(rating);

        assertEquals(new LinkedHashSet<>(List.of("drama", "comedy")), response.getTags());
    }

    @Test
    void convertToResponse_shouldIgnoreTrailingCommasAndBlankTags() {
        Rating rating = buildRatingWithTags("action, thriller,, ,");

        RatingService.RatingResponse response = ratingService.convertToResponse(rating);

        assertEquals(new LinkedHashSet<>(List.of("action", "thriller")), response.getTags());
    }

    private static Rating buildRatingWithTags(String tagStr) {
        Rating rating = new Rating();
        rating.setId(1L);
        rating.setUserId("user-1");
        rating.setMovieId(42L);
        rating.setScore(5);
        rating.setTagStr(tagStr);
        rating.setSource(Rating.RatingSource.MANUAL);
        return rating;
    }
}
