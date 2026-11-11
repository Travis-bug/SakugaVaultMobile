package ca.gbc.comp3074.sakugavault.api.dto;

/** One tracked show in the watch history (latest episode, position, finished or not). */
public class WatchHistoryEntryDto {
    public String animeId;
    public String animeTitle;
    public String posterImageUrl;
    public int episodeNumber;
    public int positionSeconds;
    public int durationSeconds;
    public boolean completed;
    public String lastWatchedAtUtc;
}
