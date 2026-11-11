package ca.gbc.comp3074.sakugavault.api.dto;

/** Body of POST /api/watch/{id}/resolve-playback (the back end's PlaybackResolutionRequestDto). */
public class PlaybackResolutionRequestDto {
    public int episodeNumber;
    /** "sub" or "dub". */
    public String preferredLanguage;
    public String subtitleLanguage = "en";

    public PlaybackResolutionRequestDto(int episodeNumber, String preferredLanguage) {
        this.episodeNumber = episodeNumber;
        this.preferredLanguage = preferredLanguage;
    }
}
