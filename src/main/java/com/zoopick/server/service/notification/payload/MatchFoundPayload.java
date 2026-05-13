package com.zoopick.server.service.notification.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zoopick.server.entity.Item;
import com.zoopick.server.entity.ItemMatch;
import com.zoopick.server.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

@NullMarked
@Schema(
        name = "MatchFoundPayload",
        description = "MATCH_FOUND : 매칭 발견 알림 payload"
)
public record MatchFoundPayload(
        @JsonProperty("item_id")
        @Schema(description = "분실물 ID", example = "10")
        long itemId,

        @JsonProperty("match_id")
        @Schema(description = "매칭 ID", example = "55")
        long matchId,

        @JsonProperty("score")
        @Schema(description = "매칭 점수", example = "0.87")
        float score
) implements NotificationPayload {
    public static MatchFoundPayload of(Item item, ItemMatch itemMatch) {
        return new MatchFoundPayload(item.getId(), itemMatch.getId(), itemMatch.getScore());
    }

    @Override
    public NotificationType type() {
        return NotificationType.MATCH_FOUND;
    }

    @Override
    public Map<String, String> toMap() {
        return Map.of(
                "item_id", String.valueOf(itemId),
                "match_id", String.valueOf(matchId),
                "score", String.valueOf(score)
        );
    }
}
