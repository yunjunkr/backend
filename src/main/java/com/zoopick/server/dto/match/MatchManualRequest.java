package com.zoopick.server.dto.match;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MatchManualRequest {
    @NotNull
    @JsonProperty("lost_item_id")
    Long lostItemId;
    @NotNull
    @JsonProperty("found_item_id")
    Long foundItemId;

}
