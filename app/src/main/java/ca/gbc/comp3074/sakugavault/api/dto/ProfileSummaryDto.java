package ca.gbc.comp3074.sakugavault.api.dto;

import java.util.List;

/** Answer from GET /api/profile/me (Contracts/Profile). The download queue fields are not used by the app. */
public class ProfileSummaryDto {
    public CurrentUserDto user;
    public int continueWatchingCount;
    public int completedEntriesCount;
    public int commentsCount;
    public List<WatchHistoryEntryDto> recentHistory;
}
