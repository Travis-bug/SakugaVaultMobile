package ca.gbc.comp3074.sakugavault.services.watch;

import ca.gbc.comp3074.sakugavault.services.DeveloperLogger;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import ca.gbc.comp3074.sakugavault.api.ApiCallback;
import ca.gbc.comp3074.sakugavault.api.ApiClient;
import ca.gbc.comp3074.sakugavault.api.ApiError;
import ca.gbc.comp3074.sakugavault.api.SakugaVaultApi;
import ca.gbc.comp3074.sakugavault.api.dto.CursorPagedResult;
import ca.gbc.comp3074.sakugavault.api.dto.UpsertWatchHistoryRequestDto;
import ca.gbc.comp3074.sakugavault.api.dto.WatchHistoryEntryDto;

public final class WatchHistoryService implements IWatchHistoryService {

    private final SakugaVaultApi api;
    /** Save-then-flush sequences still running. History reads wait for them (all on the main thread). */
    private int pendingFlushes = 0;
    private final List<Runnable> waitingForFlush = new ArrayList<>();

    public WatchHistoryService(Context context) {
        this.api = ApiClient.api(context);
    }

    @Override
    public void getMyHistory(int pageSize, ApiCallback<List<WatchHistoryEntryDto>> callback) {
        Runnable load = () -> loadHistory(pageSize, callback);
        if (pendingFlushes > 0) {
            waitingForFlush.add(load); // wait, so the list includes the progress that was just saved
        } else {
            load.run();
        }
    }

    private void loadHistory(int pageSize, ApiCallback<List<WatchHistoryEntryDto>> callback) {
        ApiClient.enqueue(api.myHistory(pageSize, null), new ApiCallback<CursorPagedResult<WatchHistoryEntryDto>>() {
            @Override
            public void onSuccess(CursorPagedResult<WatchHistoryEntryDto> page) {
                callback.onSuccess(page == null || page.items == null ? new ArrayList<>() : page.items);
            }

            @Override
            public void onError(ApiError error) {
                callback.onError(error);
            }
        });
    }

    @Override
    public void saveProgress(String animeId, int episodeNumber, int positionSeconds, int durationSeconds,
                             boolean completed) {
        UpsertWatchHistoryRequestDto request = new UpsertWatchHistoryRequestDto(
                animeId, episodeNumber, Math.max(0, positionSeconds), Math.max(0, durationSeconds), completed);
        ApiClient.enqueue(api.saveProgress(request), ignoreResult());
    }

    @Override
    public void saveProgressAndFlush(String animeId, int episodeNumber, int positionSeconds, int durationSeconds,
                                     boolean completed) {
        pendingFlushes++;
        UpsertWatchHistoryRequestDto request = new UpsertWatchHistoryRequestDto(
                animeId, episodeNumber, Math.max(0, positionSeconds), Math.max(0, durationSeconds), completed);
        ApiClient.enqueue(api.saveProgress(request), new ApiCallback<WatchHistoryEntryDto>() {
            @Override
            public void onSuccess(WatchHistoryEntryDto result) {
                flushThenRelease();
            }

            @Override
            public void onError(ApiError error) {
                flushThenRelease();
            }
        });
    }

    private void flushThenRelease() {
        ApiClient.enqueue(api.flushHistory(), new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                release();
            }

            @Override
            public void onError(ApiError error) {
                release();
            }
        });
    }

    private void release() {
        pendingFlushes = Math.max(0, pendingFlushes - 1);
        if (pendingFlushes == 0 && !waitingForFlush.isEmpty()) {
            List<Runnable> ready = new ArrayList<>(waitingForFlush);
            waitingForFlush.clear();
            for (Runnable load : ready) {
                load.run();
            }
        }
    }

    @Override
    public void flush() {
        ApiClient.enqueue(api.flushHistory(), ignoreResult());
    }

    private static <T> ApiCallback<T> ignoreResult() {
        return new ApiCallback<T>() {
            @Override
            public void onSuccess(T result) {
            }

            @Override
            public void onError(ApiError error) {
            }
        };
    }
}
