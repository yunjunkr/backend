package com.zoopick.server.mapper.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoopick.server.entity.NotificationType;
import com.zoopick.server.exception.InternalServerException;
import com.zoopick.server.service.notification.payload.*;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@NullMarked
public class NotificationPayloadMapper {
    private final Map<NotificationType, Class<? extends NotificationPayload>> payloadTypes = Map.of(
            NotificationType.CHAT_MESSAGE, ChatMessagePayload.class,
            NotificationType.ITEM_RETURNED, ItemReturnedPayload.class,
            NotificationType.LOCKER_READY, LockerReadyPayload.class,
            NotificationType.THEFT_SUSPECTED, TheftSuspectedPayload.class,
            NotificationType.MATCH_FOUND, MatchFoundPayload.class
    );

    private final ObjectMapper objectMapper;

    public <T extends NotificationPayload> T toNotificationPayload(JsonNode jsonNode, Class<T> payloadType) {
        try {
            return objectMapper.convertValue(jsonNode, payloadType);
        } catch (IllegalArgumentException exception) {
            throw new InternalServerException("Failed to convert payload " + jsonNode, exception);
        }
    }

    public NotificationPayload toNotificationPayload(Object object, NotificationType type) {
        try {
            Class<? extends NotificationPayload> payloadType = payloadTypes.get(type);
            if (!payloadTypes.containsKey(type))
                throw new UnsupportedOperationException("Unsupported notification type : " + type);
            return objectMapper.convertValue(object, payloadType);
        } catch (IllegalArgumentException exception) {
            throw new InternalServerException("Failed to convert payload " + object, exception);
        }
    }
}
