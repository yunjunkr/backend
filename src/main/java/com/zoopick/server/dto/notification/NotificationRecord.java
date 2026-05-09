package com.zoopick.server.dto.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zoopick.server.entity.NotificationType;
import com.zoopick.server.service.notification.payload.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 알림 하나의 데이터
 */
@Getter
@Setter
@Schema(description = "알림 단건 응답")
public class NotificationRecord {
    @Schema(description = "알림 ID", example = "101")
    private long id;

    @Schema(description = "알림 타입", example = "CHAT_MESSAGE")
    private NotificationType type;

    @Schema(
            description = "알림 타입별 payload입니다.",
            oneOf = {
                    MatchFoundPayload.class,
                    ChatMessagePayload.class,
                    ItemReturnedPayload.class,
                    TheftSuspectedPayload.class,
                    LockerReadyPayload.class
            }
    )
    private Map<String, Object> payload;

    @JsonProperty("read_at")
    @Schema(description = "읽음 시각", example = "2026-05-09T14:30:00")
    private LocalDateTime readAt;

    @JsonProperty("created_at")
    @Schema(description = "생성 시각", example = "2026-05-09T14:20:00")
    private LocalDateTime createdAt;
}
