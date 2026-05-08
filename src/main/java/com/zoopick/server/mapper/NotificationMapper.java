package com.zoopick.server.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoopick.server.dto.notification.NotificationRecord;
import com.zoopick.server.entity.User;
import com.zoopick.server.entity.ZoopickNotification;
import com.zoopick.server.service.command.SendNotificationCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationMapper {
    private final ObjectMapper objectMapper;

    public NotificationRecord toNotificationResponse(ZoopickNotification notification) {
        NotificationRecord notificationRecord = new NotificationRecord();
        notificationRecord.setId(notification.getId());
        notificationRecord.setType(notification.getType());
        notificationRecord.setCreatedAt(notification.getCreatedAt());
        Map<String, Object> payload = objectMapper.convertValue(notification.getPayload(), new TypeReference<>() {
        });
        notificationRecord.setPayload(payload);
        notificationRecord.setReadAt(notification.getReadAt());

        return notificationRecord;
    }

    public ZoopickNotification toZoopickNotification(User user, SendNotificationCommand command) {
        return ZoopickNotification.builder()
                .user(user)
                .createdAt(LocalDateTime.now())
                .payload(objectMapper.convertValue(command.payload(), new TypeReference<>() {
                }))
                .type(command.payload().type())
                .build();
    }
}
