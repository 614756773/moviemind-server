package com.huguo.moviemind_server.recommendation.controller;

import com.huguo.moviemind_server.common.dto.ApiResponse;
import com.huguo.moviemind_server.recommendation.dto.FeedbackRequest;
import com.huguo.moviemind_server.recommendation.dto.RecommendationDto;
import com.huguo.moviemind_server.recommendation.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "http://localhost:3000")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<RecommendationDto>> getRecommendations(
            Authentication authentication,
            @RequestParam(defaultValue = "10") int limit
    ) {
        RecommendationDto recommendation = recommendationService.generateRecommendations(
                authentication.getName(),
                Math.min(limit, 30)
        );
        return ResponseEntity.ok(new ApiResponse<>("Recommendations generated", recommendation));
    }

    @PostMapping("/feedback")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitFeedback(
            Authentication authentication,
            @Valid @RequestBody FeedbackRequest request
    ) {
        recommendationService.submitFeedback(
                authentication.getName(),
                request.getEventId(),
                request.getMovieId(),
                request.getFeedbackType()
        );

        return ResponseEntity.ok(new ApiResponse<>("Feedback submitted", Map.of(
                "eventId", request.getEventId(),
                "movieId", request.getMovieId(),
                "feedbackType", request.getFeedbackType().name()
        )));
    }
}
