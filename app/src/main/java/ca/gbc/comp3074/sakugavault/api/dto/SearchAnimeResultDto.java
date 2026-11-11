package ca.gbc.comp3074.sakugavault.api.dto;

import java.util.List;

/** One search result. */
public class SearchAnimeResultDto {
    public String id;
    public String title;
    public String synopsis;
    public String posterImageUrl;
    public int episodeCount;
    public boolean subAvailable;
    public boolean dubAvailable;
    public String watchRoute;
    public List<String> genres;
}
