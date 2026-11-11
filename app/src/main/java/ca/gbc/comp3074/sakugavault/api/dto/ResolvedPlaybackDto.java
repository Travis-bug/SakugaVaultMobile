package ca.gbc.comp3074.sakugavault.api.dto;

import java.util.List;

/** Answer from POST /api/watch/{id}/resolve-playback: where the episode can be played from. */
public class ResolvedPlaybackDto {
    public String animeId;
    public int episodeNumber;
    public boolean isResolved;
    /** "HLS" for an .m3u8 playlist, "HTTP" for a plain video file. */
    public String preferredProtocol;
    /** A direct link, or a relative proxy path such as /api/watch/streams/{id} that needs the playback cookie. */
    public String streamUrl;
    public String sourceHost;
    public String resolver;
    public String audioLanguage;
    public String subtitleLanguage;
    public boolean usedFallback;
    public List<PlaybackSubtitleDto> subtitles;
    public String statusMessage;
}
