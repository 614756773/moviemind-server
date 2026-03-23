package com.huguo.moviemind_server.user.repository;

import com.huguo.moviemind_server.user.model.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, String> {
    Optional<NotificationSettings> findByUserId(String userId);
    boolean existsByUserId(String userId);
}