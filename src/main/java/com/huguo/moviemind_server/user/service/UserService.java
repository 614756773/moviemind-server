package com.huguo.moviemind_server.user.service;

import com.huguo.moviemind_server.common.exception.ResourceNotFoundException;
import com.huguo.moviemind_server.auth.model.User;
import com.huguo.moviemind_server.auth.repository.UserRepository;
import com.huguo.moviemind_server.user.model.NotificationSettings;
import com.huguo.moviemind_server.user.repository.NotificationSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;

    @Autowired
    public UserService(UserRepository userRepository,
                     NotificationSettingsRepository notificationSettingsRepository) {
        this.userRepository = userRepository;
        this.notificationSettingsRepository = notificationSettingsRepository;
    }

    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public UserResponse getUserProfile(String userId) {
        User user = getUserById(userId);

        NotificationSettings settings = notificationSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationSettings newSettings = new NotificationSettings();
                    newSettings.setUserId(userId);
                    return newSettings;
                });

        return new UserResponse(user, settings);
    }

    public UserResponse updateUserProfile(String userId, UserUpdateRequest request) {
        User user = getUserById(userId);

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username already exists");
            }
            user.setUsername(request.getUsername());
        }

        user.updateTimestamp();
        user = userRepository.save(user);

        NotificationSettings settings = notificationSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationSettings newSettings = new NotificationSettings();
                    newSettings.setUserId(userId);
                    return notificationSettingsRepository.save(newSettings);
                });

        if (request.getWatchRemindersEnabled() != null) {
            settings.setWatchRemindersEnabled(request.getWatchRemindersEnabled());
        }
        if (request.getNewRecommendationsEnabled() != null) {
            settings.setNewRecommendationsEnabled(request.getNewRecommendationsEnabled());
        }
        if (request.getWeeklyDigestEnabled() != null) {
            settings.setWeeklyDigestEnabled(request.getWeeklyDigestEnabled());
        }

        settings = notificationSettingsRepository.save(settings);

        return new UserResponse(user, settings);
    }

    public NotificationSettingsResponse getNotificationSettings(String userId) {
        NotificationSettings settings = notificationSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationSettings newSettings = new NotificationSettings();
                    newSettings.setUserId(userId);
                    return notificationSettingsRepository.save(newSettings);
                });

        return convertToResponse(settings);
    }

    public NotificationSettingsResponse updateNotificationSettings(String userId, NotificationSettingsRequest request) {
        NotificationSettings settings = notificationSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationSettings newSettings = new NotificationSettings();
                    newSettings.setUserId(userId);
                    return notificationSettingsRepository.save(newSettings);
                });

        if (request.getWatchRemindersEnabled() != null) {
            settings.setWatchRemindersEnabled(request.getWatchRemindersEnabled());
        }
        if (request.getNewRecommendationsEnabled() != null) {
            settings.setNewRecommendationsEnabled(request.getNewRecommendationsEnabled());
        }
        if (request.getWeeklyDigestEnabled() != null) {
            settings.setWeeklyDigestEnabled(request.getWeeklyDigestEnabled());
        }

        settings = notificationSettingsRepository.save(settings);
        return convertToResponse(settings);
    }

    public UserResponse convertToResponse(User user, NotificationSettings settings) {
        return new UserResponse(user, settings);
    }

    public static NotificationSettingsResponse convertToResponse(NotificationSettings settings) {
        NotificationSettingsResponse response = new NotificationSettingsResponse();
        response.setUserId(settings.getUserId());
        response.setWatchRemindersEnabled(settings.isWatchRemindersEnabled());
        response.setNewRecommendationsEnabled(settings.isNewRecommendationsEnabled());
        response.setWeeklyDigestEnabled(settings.isWeeklyDigestEnabled());
        response.setCreatedAt(settings.getCreatedAt() != null ? settings.getCreatedAt().toString() : null);
        response.setUpdatedAt(settings.getUpdatedAt() != null ? settings.getUpdatedAt().toString() : null);
        return response;
    }

    public static class UserUpdateRequest {
        private String username;
        private Boolean watchRemindersEnabled;
        private Boolean newRecommendationsEnabled;
        private Boolean weeklyDigestEnabled;

        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public Boolean getWatchRemindersEnabled() { return watchRemindersEnabled; }
        public void setWatchRemindersEnabled(Boolean watchRemindersEnabled) {
            this.watchRemindersEnabled = watchRemindersEnabled;
        }

        public Boolean getNewRecommendationsEnabled() { return newRecommendationsEnabled; }
        public void setNewRecommendationsEnabled(Boolean newRecommendationsEnabled) {
            this.newRecommendationsEnabled = newRecommendationsEnabled;
        }

        public Boolean getWeeklyDigestEnabled() { return weeklyDigestEnabled; }
        public void setWeeklyDigestEnabled(Boolean weeklyDigestEnabled) {
            this.weeklyDigestEnabled = weeklyDigestEnabled;
        }
    }

    public static class NotificationSettingsRequest {
        private Boolean watchRemindersEnabled;
        private Boolean newRecommendationsEnabled;
        private Boolean weeklyDigestEnabled;

        // Getters and setters
        public Boolean getWatchRemindersEnabled() { return watchRemindersEnabled; }
        public void setWatchRemindersEnabled(Boolean watchRemindersEnabled) {
            this.watchRemindersEnabled = watchRemindersEnabled;
        }

        public Boolean getNewRecommendationsEnabled() { return newRecommendationsEnabled; }
        public void setNewRecommendationsEnabled(Boolean newRecommendationsEnabled) {
            this.newRecommendationsEnabled = newRecommendationsEnabled;
        }

        public Boolean getWeeklyDigestEnabled() { return weeklyDigestEnabled; }
        public void setWeeklyDigestEnabled(Boolean weeklyDigestEnabled) {
            this.weeklyDigestEnabled = weeklyDigestEnabled;
        }
    }

    public static class UserResponse {
        private String id;
        private String username;
        private String createdAt;
        private String updatedAt;
        private NotificationSettingsResponse notificationSettings;

        public UserResponse(User user, NotificationSettings settings) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.createdAt = user.getCreatedAt().toString();
            this.updatedAt = user.getUpdatedAt().toString();
            this.notificationSettings = UserService.convertToResponse(settings);
        }

        // Getters
        public String getId() { return id; }
        public String getUsername() { return username; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public NotificationSettingsResponse getNotificationSettings() { return notificationSettings; }
    }

    public static class NotificationSettingsResponse {
        private String userId;
        private boolean watchRemindersEnabled;
        private boolean newRecommendationsEnabled;
        private boolean weeklyDigestEnabled;
        private String createdAt;
        private String updatedAt;

        // Getters
        public String getUserId() { return userId; }
        public boolean isWatchRemindersEnabled() { return watchRemindersEnabled; }
        public boolean isNewRecommendationsEnabled() { return newRecommendationsEnabled; }
        public boolean isWeeklyDigestEnabled() { return weeklyDigestEnabled; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }

        // Setters
        public void setUserId(String userId) { this.userId = userId; }
        public void setWatchRemindersEnabled(boolean watchRemindersEnabled) {
            this.watchRemindersEnabled = watchRemindersEnabled;
        }
        public void setNewRecommendationsEnabled(boolean newRecommendationsEnabled) {
            this.newRecommendationsEnabled = newRecommendationsEnabled;
        }
        public void setWeeklyDigestEnabled(boolean weeklyDigestEnabled) {
            this.weeklyDigestEnabled = weeklyDigestEnabled;
        }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }
}