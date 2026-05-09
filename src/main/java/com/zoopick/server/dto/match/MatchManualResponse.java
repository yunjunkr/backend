package com.zoopick.server.dto.match;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zoopick.server.entity.MatchManualType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MatchManualResponse {
    @NotNull
    @JsonProperty("match_id")
    private Long matchId;
    @NotNull
    @JsonProperty("match_manual_type")
    private MatchManualType matchManualType;
    @JsonProperty("locker_id")
    private Long lockerId;
}
