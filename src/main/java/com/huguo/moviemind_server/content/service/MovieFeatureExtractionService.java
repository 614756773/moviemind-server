package com.huguo.moviemind_server.content.service;

import com.huguo.moviemind_server.common.exception.ResourceNotFoundException;
import com.huguo.moviemind_server.content.dto.ExtractionResult;
import com.huguo.moviemind_server.content.model.MovieFeature;
import com.huguo.moviemind_server.content.model.MovieRawData;
import com.huguo.moviemind_server.content.repository.MovieFeatureRepository;
import com.huguo.moviemind_server.content.repository.MovieRawDataRepository;
import com.huguo.moviemind_server.movie.model.Movie;
import com.huguo.moviemind_server.movie.repository.MovieRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class MovieFeatureExtractionService {

    private final MovieRepository movieRepository;
    private final MovieRawDataRepository movieRawDataRepository;
    private final MovieFeatureRepository movieFeatureRepository;
    private final RuleBasedFeatureExtractor ruleBasedFeatureExtractor;

    public MovieFeatureExtractionService(MovieRepository movieRepository,
                                         MovieRawDataRepository movieRawDataRepository,
                                         MovieFeatureRepository movieFeatureRepository,
                                         RuleBasedFeatureExtractor ruleBasedFeatureExtractor) {
        this.movieRepository = movieRepository;
        this.movieRawDataRepository = movieRawDataRepository;
        this.movieFeatureRepository = movieFeatureRepository;
        this.ruleBasedFeatureExtractor = ruleBasedFeatureExtractor;
    }

    public MovieFeature analyzeAndStore(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found: " + movieId));

        MovieRawData rawData = movieRawDataRepository.findTopByMovieIdOrderByCrawledAtDesc(movieId).orElse(null);
        String content = buildContent(movie, rawData).toLowerCase(Locale.ROOT);
        ExtractionResult extractionResult = ruleBasedFeatureExtractor.extract(content);

        MovieFeature feature = movieFeatureRepository.findByMovieId(movieId).orElseGet(MovieFeature::new);
        feature.setMovieId(movieId);
        feature.setSentimentScore(extractionResult.getSentimentScore());
        feature.setThemes(String.join(",", extractionResult.getThemes()));
        feature.setMoodTags(String.join(",", extractionResult.getMoodTags()));
        feature.setStyleTags(String.join(",", extractionResult.getStyleTags()));
        feature.setKeywords(String.join(",", extractionResult.getKeywords()));
        feature.setDataSource(rawData == null ? "movie-basic" : rawData.getSource());
        feature.setAlgorithmVersion(extractionResult.getAlgorithmVersion());

        return movieFeatureRepository.save(feature);
    }

    public int analyzeTopMovies(int limit) {
        List<Movie> movies = movieRepository.findAll(PageRequest.of(0, limit)).getContent();
        for (Movie movie : movies) {
            analyzeAndStore(movie.getId());
        }
        return movies.size();
    }

    public MovieFeature getByMovieId(Long movieId) {
        return movieFeatureRepository.findByMovieId(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found for movie: " + movieId));
    }

    private String buildContent(Movie movie, MovieRawData rawData) {
        StringBuilder sb = new StringBuilder();
        appendIfNotBlank(sb, movie.getTitle());
        appendIfNotBlank(sb, movie.getGenresStr());
        appendIfNotBlank(sb, movie.getTagsStr());
        appendIfNotBlank(sb, movie.getMetadataJson());
        if (rawData != null) {
            appendIfNotBlank(sb, rawData.getRawJson());
        }
        return sb.toString();
    }

    private static void appendIfNotBlank(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(value).append(' ');
        }
    }

}
