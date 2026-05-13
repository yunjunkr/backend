package com.zoopick.server.websocket.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record ChatReadPayload(@JsonProperty("reader_nickname") String readerNickname) implements ChatEventPayload {
    @Override
    public ChatEventMessage.Type type() {
        return ChatEventMessage.Type.READ;
    }
}
