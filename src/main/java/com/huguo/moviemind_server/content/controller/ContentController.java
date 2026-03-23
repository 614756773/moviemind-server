package com.huguo.moviemind_server.content.controller;

import com.huguo.moviemind_server.common.dto.ApiResponse;
import com.huguo.moviemind_server.content.model.MovieFeature;
import com.huguo.moviemind_server.content.service.MovieDataIngestionService;
import com.huguo.moviemind_server.content.service.MovieFeatureExtractionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/content")
@CrossOrigin(origins = "http://localhost:3000")
public class ContentController {

    private final MovieDataIngestionService movieDataIngestionService;
    private final MovieFeatureExtractionService movieFeatureExtractionService;

    public ContentController(MovieDataIngestionService movieDataIngestionService,
                             MovieFeatureExtractionService movieFeatureExtractionService) {
        this.movieDataIngestionService = movieDataIngestionService;
        this.movieFeatureExtractionService = movieFeatureExtractionService;
    }

    @PostMapping("/ingestion")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ingestMovies(
            @RequestParam(defaultValue = "douban") String source,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit
    ) {
        int count = movieDataIngestionService.ingestByKeyword(source, keyword, Math.min(limit, 50));
        return ResponseEntity.ok(new ApiResponse<>("Ingestion completed", Map.of(
                "source", source,
                "keyword", keyword,
                "count", count
        )));
    }

    @PostMapping("/features/analyze/{movieId}")
    public ResponseEntity<ApiResponse<MovieFeature>> analyzeSingleMovie(@PathVariable Long movieId) {
        MovieFeature feature = movieFeatureExtractionService.analyzeAndStore(movieId);
        return ResponseEntity.ok(new ApiResponse<>("Feature analysis completed", feature));
    }

    @PostMapping("/features/analyze")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeBatch(
            @RequestParam(defaultValue = "20") int limit
    ) {
        int count = movieFeatureExtractionService.analyzeTopMovies(Math.min(limit, 100));
        return ResponseEntity.ok(new ApiResponse<>("Batch feature analysis completed", Map.of("count", count)));
    }

    @GetMapping("/features/{movieId}")
    public ResponseEntity<ApiResponse<MovieFeature>> getFeature(@PathVariable Long movieId) {
        MovieFeature feature = movieFeatureExtractionService.getByMovieId(movieId);
        return ResponseEntity.ok(new ApiResponse<>("Feature retrieved successfully", feature));
    }
}
