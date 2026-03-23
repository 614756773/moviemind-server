package com.huguo.moviemind_server.preference.controller;

import com.huguo.moviemind_server.common.dto.ApiResponse;
import com.huguo.moviemind_server.common.dto.PageResponse;
import com.huguo.moviemind_server.preference.service.RatingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/preferences")
@CrossOrigin(origins = "http://localhost:3000")
public class RatingController {

    private final RatingService ratingService;

    @Autowired
    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RatingService.RatingResponse>>> getPreferences(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ratedAt,desc") String[] sort,
            @RequestParam(required = false) String search) {

        // Get current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("Not authenticated", null));
        }

        // Parse sort parameters
        String sortField = sort[0];
        Sort.Direction sortDirection = sort.length > 1 ?
                Sort.Direction.fromString(sort[1]) : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, sortDirection, sortField);
        Page<RatingService.RatingResponse> ratings = ratingService.getUserRatings(
                authentication.getName(), search, pageable);

        PageResponse<RatingService.RatingResponse> response = new PageResponse<>(ratings);
        return ResponseEntity.ok(new ApiResponse<>("Preferences retrieved successfully", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RatingService.RatingResponse>> createPreference(
            @RequestBody RatingService.RatingRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("Not authenticated", null));
        }

        RatingService.RatingResponse rating = ratingService.createRating(
                authentication.getName(), request);

        return ResponseEntity.status(201)
                .body(new ApiResponse<>("Preference created successfully", rating));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RatingService.RatingResponse>> updatePreference(
            @PathVariable Long id,
            @RequestBody RatingService.RatingRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("Not authenticated", null));
        }

        RatingService.RatingResponse rating = ratingService.updateRating(
                id, authentication.getName(), request);

        return ResponseEntity.ok(new ApiResponse<>("Preference updated successfully", rating));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RatingService.RatingResponse>> getPreference(
            @PathVariable Long id) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("Not authenticated", null));
        }

        RatingService.RatingResponse rating = ratingService.getRatingById(
                id, authentication.getName());

        return ResponseEntity.ok(new ApiResponse<>("Preference retrieved successfully", rating));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePreference(
            @PathVariable Long id) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("Not authenticated", null));
        }

        ratingService.deleteRating(id, authentication.getName());
        return ResponseEntity.ok(new ApiResponse<>("Preference deleted successfully", null));
    }

    @GetMapping("/top")
    public ResponseEntity<ApiResponse<List<RatingService.RatingResponse>>> getTopRatings(
            @RequestParam(defaultValue = "5") int limit) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("Not authenticated", null));
        }

        List<RatingService.RatingResponse> topRatings = ratingService.getUserTopRatings(
                authentication.getName(), limit);

        return ResponseEntity.ok(new ApiResponse<>("Top ratings retrieved successfully", topRatings));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getRatingCount() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("Not authenticated", null));
        }

        long count = ratingService.getUserRatingsCount(authentication.getName());
        return ResponseEntity.ok(new ApiResponse<>("Rating count retrieved", count));
    }
}