package com.zoopick.server.mapper.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoopick.server.dto.notification.NotificationRecord;
import com.zoopick.server.entity.User;
import com.zoopick.server.entity.ZoopickNotification;
import com.zoopick.server.service.notification.SendNotificationCommand;
import com.zoopick.server.service.notification.payload.NotificationPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class NotificationMapper {
    private final ObjectMapper objectMapper;
    private final NotificationPayloadMapper notificationPayloadMapper;

    public NotificationRecord toNotificationResponse(ZoopickNotification notification) {
        NotificationRecord notificationRecord = new NotificationRecord();
        notificationRecord.setId(notification.getId());
        notificationRecord.setType(notification.getType());
        notificationRecord.setCreatedAt(notification.getCreatedAt());
        NotificationPayload payload = notificationPayloadMapper.toNotificationPayload(notification.getPayload(), notification.getType());
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
