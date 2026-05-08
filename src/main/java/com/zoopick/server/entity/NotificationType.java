package com.zoopick.server.entity;

import com.zoopick.server.exception.BadRequestException;

import java.util.Map;
import java.util.Set;

public enum NotificationType {
    MATCH_FOUND("item_id", "match_id", "score"),
    CHAT_MESSAGE("room_id", "sender_nickname", "message"),
    ITEM_RETURNED("item_id", "item_post_id"),
    THEFT_SUSPECTED("item_id"),
    LOCKER_READY("item_id");

    public final Set<String> requiredPayloadKeys;

    NotificationType(String... requiredPayloadKeys) {
        this.requiredPayloadKeys = Set.of(requiredPayloadKeys);
    }

    @Deprecated(forRemoval = true)
    public void validatePayload(Map<String, String> payload) {
        if (payload.keySet().containsAll(this.requiredPayloadKeys))
            throw new BadRequestException("잘못된 알림 요청입니다.", "type: " + this + " required: " + requiredPayloadKeys);
    }
}
