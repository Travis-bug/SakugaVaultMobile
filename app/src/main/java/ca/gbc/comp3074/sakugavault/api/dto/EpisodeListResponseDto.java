package ca.gbc.comp3074.sakugavault.api.dto;

import java.util.List;

/** Answer from GET /api/watch/{id}/episodes: the provider's real episode list. */
public class EpisodeListResponseDto {
    public boolean isResolved;
    public String providerKey;
    public List<WatchSeasonDto> seasons;
    public String statusMessage;
}
