package ca.gbc.comp3074.sakugavault.api.dto;

import java.util.List;

/** Answer from GET /api/watch/{id}: everything the title page shows (Contracts/Watch). */
public class WatchPageDto {
    public String id;
    public String title;
    public String synopsis;
    public String posterImageUrl;
    public String backdropImageUrl;
    public int runtimeMinutes;
    public int episodeCount;
    public boolean subAvailable;
    public boolean dubAvailable;
    public List<String> availableAudioLanguages;
    public List<String> availableSubtitleLanguages;
    public String metadataLastSyncedAtUtc;
    public List<WatchSeasonDto> seasons;
    public List<CommentDto> comments;
    public List<AnimeCardDto> similarAnime;
}
