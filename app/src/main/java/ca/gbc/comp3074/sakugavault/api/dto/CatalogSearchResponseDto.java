package ca.gbc.comp3074.sakugavault.api.dto;

import java.util.List;

/** Answer from GET /api/catalog/search?q=... */
public class CatalogSearchResponseDto {
    public String query;
    public int totalResults;
    public List<SearchAnimeResultDto> results;
}
