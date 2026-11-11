package ca.gbc.comp3074.sakugavault.services.watch;

import ca.gbc.comp3074.sakugavault.services.DeveloperLogger;

import android.content.Context;

import ca.gbc.comp3074.sakugavault.api.ApiCallback;
import ca.gbc.comp3074.sakugavault.api.ApiClient;
import ca.gbc.comp3074.sakugavault.api.SakugaVaultApi;
import ca.gbc.comp3074.sakugavault.api.dto.EpisodeListResponseDto;
import ca.gbc.comp3074.sakugavault.api.dto.PlaybackResolutionRequestDto;
import ca.gbc.comp3074.sakugavault.api.dto.ResolvedPlaybackDto;
import ca.gbc.comp3074.sakugavault.api.dto.WatchPageDto;

public final class WatchPageService implements IWatchPageService {

    private final SakugaVaultApi api;

    public WatchPageService(Context context) {
        this.api = ApiClient.api(context);
    }

    @Override
    public void getWatchPage(String animeId, ApiCallback<WatchPageDto> callback) {
        ApiClient.enqueue(api.watchPage(animeId), callback);
    }

    @Override
    public void getEpisodes(String animeId, ApiCallback<EpisodeListResponseDto> callback) {
        ApiClient.enqueue(api.episodes(animeId), callback);
    }

    @Override
    public void resolvePlayback(String animeId, int episode, boolean dub, ApiCallback<ResolvedPlaybackDto> callback) {
        PlaybackResolutionRequestDto request = new PlaybackResolutionRequestDto(episode, dub ? "dub" : "sub");
        ApiClient.enqueue(api.resolvePlayback(animeId, request), callback);
    }
}
