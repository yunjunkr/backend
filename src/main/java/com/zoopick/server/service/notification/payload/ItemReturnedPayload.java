package com.zoopick.server.service.notification.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zoopick.server.entity.ItemPost;
import com.zoopick.server.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

@AllArgsConstructor
@NullMarked
@Schema(
        name = "ItemReturnedPayload",
        description = "ITEM_RETURNED : 분실물 반환 알림 payload"
)
public class ItemReturnedPayload implements NotificationPayload {
    @JsonProperty("item_id")
    @Schema(description = "물품 ID", example = "10")
    private final long itemId;
    @JsonProperty("item_post_id")
    @Schema(description = "게시글 ID", example = "45")
    private final long itemPostId;

    public static ItemReturnedPayload of(ItemPost itemPost) {
        return new ItemReturnedPayload(itemPost.getItem().getId(), itemPost.getId());
    }

    @Override
    public NotificationType type() {
        return NotificationType.ITEM_RETURNED;
    }

    @Override
    public Map<String, String> toMap() {
        return Map.of(
                "item_id", String.valueOf(itemId),
                "item_post_id", String.valueOf(itemPostId)
        );
    }
}
