package com.huguo.moviemind_server.user.controller;

import com.huguo.moviemind_server.common.dto.ApiResponse;
import com.huguo.moviemind_server.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserService.UserResponse>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("Not authenticated", null));
        }

        UserService.UserResponse user = userService.getUserProfile(authentication.getName());
        return ResponseEntity.ok(new ApiResponse<>("User profile retrieved successfully", user));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserService.UserResponse>> updateCurrentUser(
            @RequestBody UserService.UserUpdateRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("Not authenticated", null));
        }

        UserService.UserResponse user = userService.updateUserProfile(
                authentication.getName(), request);

        return ResponseEntity.ok(new ApiResponse<>("User profile updated successfully", user));
    }

    @GetMapping("/me/stats")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getUserStats() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("Not authenticated", null));
        }

        // These methods would be implemented in the appropriate service
        // For now, returning mock data
        UserStatsResponse stats = new UserStatsResponse();
        stats.setMovieCount(10);
        stats.setRatingCount(5);
        stats.setWatchlistCount(3);
        stats.setJoinedSince("2024-01-01");

        return ResponseEntity.ok(new ApiResponse<>("User stats retrieved successfully", stats));
    }

    public static class UserStatsResponse {
        private int movieCount;
        private int ratingCount;
        private int watchlistCount;
        private String joinedSince;

        // Getters and setters
        public int getMovieCount() { return movieCount; }
        public void setMovieCount(int movieCount) { this.movieCount = movieCount; }

        public int getRatingCount() { return ratingCount; }
        public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }

        public int getWatchlistCount() { return watchlistCount; }
        public void setWatchlistCount(int watchlistCount) { this.watchlistCount = watchlistCount; }

        public String getJoinedSince() { return joinedSince; }
        public void setJoinedSince(String joinedSince) { this.joinedSince = joinedSince; }
    }
}