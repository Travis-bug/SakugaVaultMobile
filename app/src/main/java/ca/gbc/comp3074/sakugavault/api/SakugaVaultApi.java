package ca.gbc.comp3074.sakugavault.api;

import ca.gbc.comp3074.sakugavault.api.dto.AuthResponseDto;
import ca.gbc.comp3074.sakugavault.api.dto.CatalogSearchResponseDto;
import ca.gbc.comp3074.sakugavault.api.dto.CommentPostedDto;
import ca.gbc.comp3074.sakugavault.api.dto.CursorPagedResult;
import ca.gbc.comp3074.sakugavault.api.dto.EpisodeListResponseDto;
import ca.gbc.comp3074.sakugavault.api.dto.HomeCatalogDto;
import ca.gbc.comp3074.sakugavault.api.dto.LoginRequestDto;
import ca.gbc.comp3074.sakugavault.api.dto.PostCommentRequestDto;
import ca.gbc.comp3074.sakugavault.api.dto.ProfileSummaryDto;
import ca.gbc.comp3074.sakugavault.api.dto.RegisterRequestDto;
import ca.gbc.comp3074.sakugavault.api.dto.UpsertWatchHistoryRequestDto;
import ca.gbc.comp3074.sakugavault.api.dto.WatchHistoryEntryDto;
import ca.gbc.comp3074.sakugavault.api.dto.WatchPageDto;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Every SakugaVault API endpoint the app calls, grouped by back-end controller.
 * Retrofit turns this interface into real HTTP calls.
 */
public interface SakugaVaultApi {

    // ---------------------------------------------------------------- AuthController

    @POST("api/auth/login")
    Call<AuthResponseDto> login(@Body LoginRequestDto request);

    @POST("api/auth/register")
    Call<AuthResponseDto> register(@Body RegisterRequestDto request);

    @POST("api/auth/logout")
    Call<Void> logout();

    // ---------------------------------------------------------------- CatalogController

    @GET("api/catalog/home")
    Call<HomeCatalogDto> home();

    @GET("api/catalog/search")
    Call<CatalogSearchResponseDto> search(@Query("q") String query, @Query("limit") int limit);

    @POST("api/catalog/comments")
    Call<CommentPostedDto> postComment(@Body PostCommentRequestDto request);

    // ---------------------------------------------------------------- WatchController

    @GET("api/watch/{animeId}")
    Call<WatchPageDto> watchPage(@Path("animeId") String animeId);

    @GET("api/watch/{animeId}/episodes")
    Call<EpisodeListResponseDto> episodes(@Path("animeId") String animeId);

    @GET("api/watch/history/me")
    Call<CursorPagedResult<WatchHistoryEntryDto>> myHistory(@Query("pageSize") int pageSize,
                                                            @Query("cursor") String cursor);

    @POST("api/watch/history")
    Call<WatchHistoryEntryDto> saveProgress(@Body UpsertWatchHistoryRequestDto request);

    @POST("api/watch/history/flush")
    Call<Void> flushHistory();

    // ---------------------------------------------------------------- ProfileController

    @GET("api/profile/me")
    Call<ProfileSummaryDto> myProfile();
}
