package ca.gbc.comp3074.sakugavault.api.dto;

import java.util.List;

/** A season and its episodes. */
public class WatchSeasonDto {
    public String id;
    public String label;
    public List<WatchEpisodeDto> episodes;
}
