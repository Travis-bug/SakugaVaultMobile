package ca.gbc.comp3074.sakugavault.services.catalog;

import ca.gbc.comp3074.sakugavault.services.DeveloperLogger;

import android.content.Context;

import ca.gbc.comp3074.sakugavault.api.ApiCallback;
import ca.gbc.comp3074.sakugavault.api.ApiClient;
import ca.gbc.comp3074.sakugavault.api.SakugaVaultApi;
import ca.gbc.comp3074.sakugavault.api.dto.CatalogSearchResponseDto;
import ca.gbc.comp3074.sakugavault.api.dto.CommentPostedDto;
import ca.gbc.comp3074.sakugavault.api.dto.HomeCatalogDto;
import ca.gbc.comp3074.sakugavault.api.dto.PostCommentRequestDto;

public final class CatalogService implements ICatalogService {

    private final SakugaVaultApi api;

    public CatalogService(Context context) {
        this.api = ApiClient.api(context);
    }

    @Override
    public void getHome(ApiCallback<HomeCatalogDto> callback) {
        ApiClient.enqueue(api.home(), callback);
    }

    @Override
    public void search(String query, int limit, ApiCallback<CatalogSearchResponseDto> callback) {
        ApiClient.enqueue(api.search(query == null ? "" : query.trim(), limit), callback);
    }

    @Override
    public void postComment(String animeId, String body, ApiCallback<CommentPostedDto> callback) {
        ApiClient.enqueue(api.postComment(new PostCommentRequestDto(animeId, body)), callback);
    }
}
