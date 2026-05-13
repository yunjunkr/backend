package com.zoopick.server.dto.cctv;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zoopick.server.entity.VideoAnalysisStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GetCctvVideoResponse {
    private Long id;
    @JsonProperty("room_id")
    private Long roomId;
    @JsonProperty("recorded_at")
    private LocalDateTime recordedAt;
    @JsonProperty("video_url")
    private String videoUrl;
    private VideoAnalysisStatus status;
    @JsonProperty("duration_seconds")
    private Integer durationSeconds;
    @JsonProperty("analyzed_seconds")
    private Integer analyzedSeconds;
    @JsonProperty("estimated_completion_at")
    private LocalDateTime estimatedCompletionAt;
    @JsonProperty("started_at")
    private LocalDateTime startedAt;
    @JsonProperty("last_updated_at")
    private LocalDateTime lastUpdatedAt;

}
