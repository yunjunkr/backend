package com.zoopick.server.repository;

import com.zoopick.server.entity.NotificationType;
import com.zoopick.server.entity.ZoopickNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<ZoopickNotification, Long> {
    List<ZoopickNotification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<ZoopickNotification> findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadAtIsNull(Long userId);

    List<ZoopickNotification> findAllByUserIdAndType(Long userId, NotificationType type);
}
