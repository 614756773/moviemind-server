package com.huguo.moviemind_server.content.service;

import com.huguo.moviemind_server.content.client.ChineseMovieDataProvider;
import com.huguo.moviemind_server.content.dto.ExternalMovieData;
import com.huguo.moviemind_server.content.model.MovieRawData;
import com.huguo.moviemind_server.content.repository.MovieRawDataRepository;
import com.huguo.moviemind_server.movie.model.Movie;
import com.huguo.moviemind_server.movie.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MovieDataIngestionService {

    private final List<ChineseMovieDataProvider> providers;
    private final MovieRepository movieRepository;
    private final MovieRawDataRepository movieRawDataRepository;

    public MovieDataIngestionService(List<ChineseMovieDataProvider> providers,
                                     MovieRepository movieRepository,
                                     MovieRawDataRepository movieRawDataRepository) {
        this.providers = providers;
        this.movieRepository = movieRepository;
        this.movieRawDataRepository = movieRawDataRepository;
    }

    public int ingestByKeyword(String source, String keyword, int limit) {
        ChineseMovieDataProvider provider = providers.stream()
                .filter(p -> p.source().equalsIgnoreCase(source))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported source: " + source));

        List<ExternalMovieData> externalMovies = provider.searchMovies(keyword, limit);
        for (ExternalMovieData externalMovie : externalMovies) {
            Movie movie = upsertMovie(externalMovie);

            MovieRawData rawData = new MovieRawData();
            rawData.setMovieId(movie.getId());
            rawData.setSource(source.toLowerCase());
            rawData.setExternalId(externalMovie.getExternalId());
            rawData.setTitle(externalMovie.getTitle());
            rawData.setYear(externalMovie.getYear());
            rawData.setRawJson(externalMovie.getRawPayload() == null ? "{}" : externalMovie.getRawPayload());
            movieRawDataRepository.save(rawData);
        }

        return externalMovies.size();
    }

    private Movie upsertMovie(ExternalMovieData externalMovie) {
        if (externalMovie.getExternalId() != null) {
            return movieRepository.findByExternalId(externalMovie.getExternalId())
                    .map(movie -> updateMovieFields(movie, externalMovie))
                    .orElseGet(() -> movieRepository.save(createMovie(externalMovie)));
        }

        return movieRepository.save(createMovie(externalMovie));
    }

    private Movie updateMovieFields(Movie movie, ExternalMovieData externalMovie) {
        if (externalMovie.getTitle() != null && !externalMovie.getTitle().isBlank()) {
            movie.setTitle(externalMovie.getTitle());
        }
        if (externalMovie.getYear() != null) {
            movie.setYear(externalMovie.getYear());
        }
        if (externalMovie.getSummary() != null && !externalMovie.getSummary().isBlank()) {
            movie.setMetadataJson(externalMovie.getSummary());
        }
        return movieRepository.save(movie);
    }

    private Movie createMovie(ExternalMovieData externalMovie) {
        Movie movie = new Movie();
        movie.setExternalId(externalMovie.getExternalId());
        movie.setTitle(externalMovie.getTitle() == null || externalMovie.getTitle().isBlank()
                ? "未命名电影"
                : externalMovie.getTitle());
        movie.setYear(externalMovie.getYear());
        movie.setMetadataJson(externalMovie.getSummary());
        if (externalMovie.getGenres() != null && !externalMovie.getGenres().isEmpty()) {
            movie.setGenresStr(String.join(",", externalMovie.getGenres()));
        }
        return movie;
    }
}
