package com.huguo.moviemind_server.content.service;

import com.huguo.moviemind_server.content.model.MovieFeature;
import com.huguo.moviemind_server.content.model.MovieRawData;
import com.huguo.moviemind_server.content.repository.MovieFeatureRepository;
import com.huguo.moviemind_server.content.repository.MovieRawDataRepository;
import com.huguo.moviemind_server.movie.model.Movie;
import com.huguo.moviemind_server.movie.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieFeatureExtractionServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private MovieRawDataRepository movieRawDataRepository;

    @Mock
    private MovieFeatureRepository movieFeatureRepository;

    private MovieFeatureExtractionService service;

    @BeforeEach
    void setUp() {
        service = new MovieFeatureExtractionService(
                movieRepository,
                movieRawDataRepository,
                movieFeatureRepository,
                new RuleBasedFeatureExtractor()
        );
    }

    @Test
    void analyzeAndStore_shouldExtractThemeMoodAndSentiment() {
        Movie movie = new Movie();
        movie.setId(1L);
        movie.setTitle("温暖家庭成长故事");
        movie.setGenresStr("剧情");
        movie.setTagsStr("治愈,励志");

        MovieRawData rawData = new MovieRawData();
        rawData.setSource("douban");
        rawData.setRawJson("这是一部现实主义电影，关于家庭和成长，整体温暖治愈。");

        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(movieRawDataRepository.findTopByMovieIdOrderByCrawledAtDesc(1L)).thenReturn(Optional.of(rawData));
        when(movieFeatureRepository.findByMovieId(1L)).thenReturn(Optional.empty());
        when(movieFeatureRepository.save(any(MovieFeature.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MovieFeature feature = service.analyzeAndStore(1L);

        assertEquals(1L, feature.getMovieId());
        assertTrue(feature.getSentimentScore() > 0);
        assertTrue(feature.getThemes().contains("家庭"));
        assertTrue(feature.getThemes().contains("成长"));
        assertTrue(feature.getMoodTags().contains("治愈"));
        assertTrue(feature.getStyleTags().contains("现实主义"));
        assertEquals("douban", feature.getDataSource());
        assertEquals("rule-based-v1.1", feature.getAlgorithmVersion());
    }

    @Test
    void analyzeAndStore_shouldFallbackToMovieBasicWhenNoRawData() {
        Movie movie = new Movie();
        movie.setId(2L);
        movie.setTitle("黑暗悲剧");
        movie.setMetadataJson("压抑悲伤的战争故事");

        when(movieRepository.findById(2L)).thenReturn(Optional.of(movie));
        when(movieRawDataRepository.findTopByMovieIdOrderByCrawledAtDesc(2L)).thenReturn(Optional.empty());
        when(movieFeatureRepository.findByMovieId(2L)).thenReturn(Optional.empty());
        when(movieFeatureRepository.save(any(MovieFeature.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MovieFeature feature = service.analyzeAndStore(2L);

        assertEquals("movie-basic", feature.getDataSource());
        assertTrue(feature.getSentimentScore() < 0);
    }
}
