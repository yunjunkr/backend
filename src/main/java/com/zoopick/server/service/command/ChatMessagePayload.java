package com.zoopick.server.service.command;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zoopick.server.entity.ChatRoom;
import com.zoopick.server.entity.NotificationType;
import com.zoopick.server.entity.User;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

@AllArgsConstructor
@NullMarked
public final class ChatMessagePayload implements NotificationPayload {
    @JsonProperty("room_id")
    private final long roomId;
    @JsonProperty("sender_nickname")
    private final String senderNickname;
    @JsonProperty("message")
    private final String message;

    public static ChatMessagePayload of(ChatRoom chatRoom, User sender, String message) {
        return new ChatMessagePayload(chatRoom.getId(), sender.getNickname(), message);
    }

    @JsonIgnore
    @Override
    public NotificationType type() {
        return NotificationType.CHAT_MESSAGE;
    }

    @JsonIgnore
    @Override
    public Map<String, String> toMap() {
        return Map.of(
                "room_id", String.valueOf(roomId),
                "sender_nickname", senderNickname,
                "message", message
        );
    }
}
