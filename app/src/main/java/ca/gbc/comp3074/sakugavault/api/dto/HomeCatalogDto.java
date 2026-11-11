package ca.gbc.comp3074.sakugavault.api.dto;

import java.util.List;

/** Answer from GET /api/catalog/home: featured titles and genre rows (Contracts/Catalog). */
public class HomeCatalogDto {
    public List<CatalogHeroDto> heroItems;
    public List<GenreRailDto> genreRows;
}
