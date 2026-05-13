package com.zoopick.server.dto.cctv;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zoopick.server.entity.DetectionReviewStatus;
import com.zoopick.server.entity.ItemCategory;
import com.zoopick.server.entity.ItemColor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GetDetectionByIdResponse {
    private Long id;
    @JsonProperty("video_id")
    private Long videoId;;
    @JsonProperty("detected_at")
    private LocalDateTime detectedAt;
    @JsonProperty("detected_category")
    private ItemCategory detectedCategory;
    @JsonProperty("detected_color")
    private ItemColor detectedColor;
    @JsonIgnore // embedding은 일단 안볼거같아서 뺌
    private float[] embedding;
    @JsonProperty("item_snapshot_url")
    private String itemSnapshotUrl;
    @JsonProperty("moment_snapshot_url")
    private String momentSnapshotUrl;
    @JsonProperty("review_status")
    private DetectionReviewStatus reviewStatus = DetectionReviewStatus.PENDING;
    @JsonProperty("reviewed_at")
    private LocalDateTime reviewedAt;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

}
