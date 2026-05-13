package com.zoopick.server.mapper.notification;

import com.zoopick.server.dto.notification.SendNotificationRequest;
import com.zoopick.server.entity.NotificationType;
import com.zoopick.server.exception.BadRequestException;
import com.zoopick.server.exception.InternalServerException;
import com.zoopick.server.service.notification.SendNotificationCommand;
import com.zoopick.server.service.notification.payload.NotificationPayload;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@NullMarked
public class SendNotificationRequestMapper {
    private final NotificationPayloadMapper notificationPayloadMapper;

    /**
     * {@linkplain SendNotificationRequest}를 {@linkplain SendNotificationCommand}로 변환합니다.
     *
     * @param request {@link SendNotificationRequest}
     * @return {@link SendNotificationCommand}
     * @throws BadRequestException     payload의 형식이 잘못되었을 때
     * @throws InternalServerException 지원하지 않는 알림 형식이거나 서버 장애가 발생했을 때
     */
    public SendNotificationCommand toCommand(SendNotificationRequest request) {
        try {
            NotificationType type = request.getType();
            NotificationPayload payload = notificationPayloadMapper.toNotificationPayload(request.getPayload(), type);
            return new SendNotificationCommand(request.getTitle(), request.getBody(), payload);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("잘못된 요청입니다.", request.getPayload() + " is not readable");
        } catch (RuntimeException exception) {
            throw new InternalServerException("Failed to convert payload " + request.getPayload(), exception);
        }
    }
}
