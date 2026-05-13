package com.zoopick.server.websocket.message;

public record ChatEventMessage(Type type, ChatEventPayload payload) {
    public static ChatEventMessage of(ChatEventPayload payload) {
        return new ChatEventMessage(payload.type(), payload);
    }

    public enum Type {
        INFO,
        ERROR,
        MESSAGE,
        READ
    }
}
