package ca.gbc.comp3074.sakugavault.services.watch;

import ca.gbc.comp3074.sakugavault.api.ApiCallback;
import ca.gbc.comp3074.sakugavault.api.dto.EpisodeListResponseDto;
import ca.gbc.comp3074.sakugavault.api.dto.WatchPageDto;

/** The title page. Mirrors the back end's Services/Watch/IWatchPageService and IEpisodeListService. */
public interface IWatchPageService {

    /** GET /api/watch/{id}: details, seasons, comments and similar titles. */
    void getWatchPage(String animeId, ApiCallback<WatchPageDto> callback);

    /** GET /api/watch/{id}/episodes: the provider's real episode list (can take a few seconds). */
    void getEpisodes(String animeId, ApiCallback<EpisodeListResponseDto> callback);
}
