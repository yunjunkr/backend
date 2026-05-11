package com.zoopick.server.dto.match;

public record CreateMatchEvent(Long matchId, Long lostItemId, Long foundItemId) {}