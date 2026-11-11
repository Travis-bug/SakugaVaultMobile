package ca.gbc.comp3074.sakugavault.services.catalog;

import ca.gbc.comp3074.sakugavault.api.ApiCallback;
import ca.gbc.comp3074.sakugavault.api.dto.CatalogSearchResponseDto;
import ca.gbc.comp3074.sakugavault.api.dto.CommentPostedDto;
import ca.gbc.comp3074.sakugavault.api.dto.HomeCatalogDto;

/** Browsing the catalog. Mirrors the back end's Services/Catalog/ICatalogService. */
public interface ICatalogService {

    /** GET /api/catalog/home: featured titles and genre rows. */
    void getHome(ApiCallback<HomeCatalogDto> callback);

    /** GET /api/catalog/search?q=... An empty query returns what's trending. */
    void search(String query, int limit, ApiCallback<CatalogSearchResponseDto> callback);

    /** POST /api/catalog/comments */
    void postComment(String animeId, String body, ApiCallback<CommentPostedDto> callback);
}
