package com.huguo.moviemind_server.movie.controller;

import com.huguo.moviemind_server.common.dto.ApiResponse;
import com.huguo.moviemind_server.common.dto.PageResponse;
import com.huguo.moviemind_server.movie.model.Movie;
import com.huguo.moviemind_server.movie.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@CrossOrigin(origins = "http://localhost:3000")
public class MovieController {

    private final MovieService movieService;

    @Autowired
    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Movie>>> getMovies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "title,asc") String[] sort,
            @RequestParam(required = false) String search) {

        // Parse sort parameters
        String sortField = sort[0];
        Sort.Direction sortDirection = sort.length > 1 ?
                Sort.Direction.fromString(sort[1]) : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, sortDirection, sortField);
        Page<Movie> movies = movieService.searchMovies(search, pageable);

        PageResponse<Movie> response = new PageResponse<>(movies);
        return ResponseEntity.ok(new ApiResponse<>("Movies retrieved successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Movie>> getMovieById(@PathVariable Long id) {
        Movie movie = movieService.getMovieById(id);
        return ResponseEntity.ok(new ApiResponse<>("Movie retrieved successfully", movie));
    }

    @GetMapping("/external/{externalId}")
    public ResponseEntity<ApiResponse<Movie>> getMovieByExternalId(@PathVariable String externalId) {
        Movie movie = movieService.getMovieByExternalId(externalId);
        return ResponseEntity.ok(new ApiResponse<>("Movie retrieved successfully", movie));
    }

    @GetMapping("/tags/{tagName}")
    public ResponseEntity<ApiResponse<List<Movie>>> getMoviesByTag(@PathVariable String tagName) {
        List<Movie> movies = movieService.getMoviesByTag(tagName);
        return ResponseEntity.ok(new ApiResponse<>("Movies by tag retrieved successfully", movies));
    }

    @GetMapping("/genres/{genreName}")
    public ResponseEntity<ApiResponse<List<Movie>>> getMoviesByGenre(@PathVariable String genreName) {
        List<Movie> movies = movieService.getMoviesByGenre(genreName);
        return ResponseEntity.ok(new ApiResponse<>("Movies by genre retrieved successfully", movies));
    }

    @GetMapping("/year/{year}")
    public ResponseEntity<ApiResponse<List<Movie>>> getMoviesByYear(@PathVariable Integer year) {
        List<Movie> movies = movieService.getMoviesByYear(year);
        return ResponseEntity.ok(new ApiResponse<>("Movies by year retrieved successfully", movies));
    }

    @GetMapping("/tags")
    public ResponseEntity<ApiResponse<List<String>>> getAllTags() {
        List<String> tags = movieService.getAllTagNames();
        return ResponseEntity.ok(new ApiResponse<>("Tags retrieved successfully", tags));
    }

    @GetMapping("/genres")
    public ResponseEntity<ApiResponse<List<String>>> getAllGenres() {
        List<String> genres = movieService.getAllGenreNames();
        return ResponseEntity.ok(new ApiResponse<>("Genres retrieved successfully", genres));
    }
}
