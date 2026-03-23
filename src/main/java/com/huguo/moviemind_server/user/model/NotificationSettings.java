package com.huguo.moviemind_server.user.model;

import com.huguo.moviemind_server.auth.model.User;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_settings")
@EntityListeners(AuditingEntityListener.class)
@Data
public class NotificationSettings {
    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @OneToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "watch_reminders_enabled", nullable = false)
    private boolean watchRemindersEnabled = true;

    @Column(name = "new_recommendations_enabled", nullable = false)
    private boolean newRecommendationsEnabled = true;

    @Column(name = "weekly_digest_enabled", nullable = false)
    private boolean weeklyDigestEnabled = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}