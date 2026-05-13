package com.zoopick.server.service.notification.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zoopick.server.entity.ChatRoom;
import com.zoopick.server.entity.NotificationType;
import com.zoopick.server.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

@NullMarked
@Schema(
        name = "ChatMessagePayload",
        description = "CHAT_MESSAGE : 채팅 메시지 알림 payload"
)
public record ChatMessagePayload(
        @JsonProperty("room_id")
        @Schema(description = "채팅방 ID", example = "123")
        long roomId,

        @JsonProperty("sender_nickname")
        @Schema(description = "보낸 사람 닉네임", example = "zoopickUser")
        String senderNickname,

        @JsonProperty("message")
        @Schema(description = "메시지 내용", example = "물건 찾으셨나요?")
        String message
) implements NotificationPayload {
    public static ChatMessagePayload of(ChatRoom chatRoom, User sender, String message) {
        return new ChatMessagePayload(chatRoom.getId(), sender.getNickname(), message);
    }

    @Override
    public NotificationType type() {
        return NotificationType.CHAT_MESSAGE;
    }

    @Override
    public Map<String, String> toMap() {
        return Map.of(
                "room_id", String.valueOf(roomId),
                "sender_nickname", senderNickname,
                "message", message
        );
    }
}
