package com.zoopick.server.service.notification.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zoopick.server.entity.Item;
import com.zoopick.server.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

@NullMarked
@Schema(
        name = "TheftSuspectedPayload",
        description = "THEFT_SUSPECTED : 도난 의심 알림 payload"
)
public record TheftSuspectedPayload(
        @JsonProperty("item_id")
        @Schema(description = "물품 ID", example = "10")
        long itemId
) implements NotificationPayload {
    public static TheftSuspectedPayload of(Item item) {
        return new TheftSuspectedPayload(item.getId());
    }

    @Override
    public NotificationType type() {
        return NotificationType.THEFT_SUSPECTED;
    }

    @Override
    public Map<String, String> toMap() {
        return Map.of(
                "item_id", String.valueOf(itemId)
        );
    }
}
