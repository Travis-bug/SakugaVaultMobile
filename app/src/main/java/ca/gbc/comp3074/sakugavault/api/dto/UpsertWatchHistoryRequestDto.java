package ca.gbc.comp3074.sakugavault.api.dto;

/** Body of POST /api/watch/history (saves where the user is in an episode). */
public class UpsertWatchHistoryRequestDto {
    public String animeId;
    public int episodeNumber;
    public int positionSeconds;
    public int durationSeconds;
    public boolean completed;

    public UpsertWatchHistoryRequestDto() {
    }

    public UpsertWatchHistoryRequestDto(String animeId, int episodeNumber, int positionSeconds, int durationSeconds, boolean completed) {
        this.animeId = animeId;
        this.episodeNumber = episodeNumber;
        this.positionSeconds = positionSeconds;
        this.durationSeconds = durationSeconds;
        this.completed = completed;
    }
}
