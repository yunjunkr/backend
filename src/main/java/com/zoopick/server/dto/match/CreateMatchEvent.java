package com.zoopick.server.dto.match;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CreateMatchEvent {
    private final List<Entry> entries;

    @Getter
    @AllArgsConstructor
    public static class Entry {
        private final long matchId;
        private final float score;
        private final long lostItemId;
        private final long reporterUserId;
        private final String itemPostTitle;
        private final String foundLocationName;
    }
}
