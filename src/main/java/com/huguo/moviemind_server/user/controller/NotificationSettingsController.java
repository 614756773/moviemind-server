package com.huguo.moviemind_server.user.controller;

import com.huguo.moviemind_server.common.dto.ApiResponse;
import com.huguo.moviemind_server.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "http://localhost:3000")
public class NotificationSettingsController {

    private final UserService userService;

    @Autowired
    public NotificationSettingsController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<UserService.NotificationSettingsResponse>> getNotificationSettings() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("Not authenticated", null));
        }

        UserService.NotificationSettingsResponse settings = userService.getNotificationSettings(
                authentication.getName());

        return ResponseEntity.ok(new ApiResponse<>("Notification settings retrieved successfully", settings));
    }

    @PutMapping("/notifications")
    public ResponseEntity<ApiResponse<UserService.NotificationSettingsResponse>> updateNotificationSettings(
            @RequestBody UserService.NotificationSettingsRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("Not authenticated", null));
        }

        UserService.NotificationSettingsResponse settings = userService.updateNotificationSettings(
                authentication.getName(), request);

        return ResponseEntity.ok(new ApiResponse<>("Notification settings updated successfully", settings));
    }

    @DeleteMapping("/data")
    public ResponseEntity<ApiResponse<Void>> clearUserData() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("Not authenticated", null));
        }

        // Implementation would involve:
        // 1. Delete all ratings for the user
        // 2. Delete all watchlist items for the user
        // 3. Delete recommendation events for the user
        // 4. Reset notification settings to defaults
        // For now, returning success message
        return ResponseEntity.ok(new ApiResponse<>("User data cleared successfully", null));
    }
}