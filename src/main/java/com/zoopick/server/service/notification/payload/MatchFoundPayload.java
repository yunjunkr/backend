package com.zoopick.server.service.notification.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zoopick.server.entity.Item;
import com.zoopick.server.entity.ItemMatch;
import com.zoopick.server.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

@AllArgsConstructor
@NullMarked
@Schema(
        name = "MatchFoundPayload",
        description = "MATCH_FOUND : 매칭 발견 알림 payload"
)
public class MatchFoundPayload implements NotificationPayload {
    @JsonProperty("item_id")
    @Schema(description = "분실물 ID", example = "10")
    private final long itemId;
    @JsonProperty("match_id")
    @Schema(description = "매칭 ID", example = "55")
    private final long matchId;
    @Schema(description = "매칭 점수", example = "0.87")
    private final float score;

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
