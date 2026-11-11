package ca.gbc.comp3074.sakugavault.data;

import java.util.List;
import java.util.Locale;

import ca.gbc.comp3074.sakugavault.api.dto.AnimeCardDto;
import ca.gbc.comp3074.sakugavault.api.dto.CatalogHeroDto;
import ca.gbc.comp3074.sakugavault.api.dto.SearchAnimeResultDto;
import ca.gbc.comp3074.sakugavault.api.dto.WatchHistoryEntryDto;
import ca.gbc.comp3074.sakugavault.api.dto.WatchPageDto;
import ca.gbc.comp3074.sakugavault.data.local.WatchListEntry;

/**
 * One anime title as the screens show it. Built from whichever API answer the
 * screen has (a Home card, a search result, the title page, a history entry or a
 * saved watchlist row), so the adapters only need to understand one class.
 */
public class Anime {

    /** Placeholder gradients shown until (or if) the poster image loads. */
    private static final int[][] PALETTE = {
            {0xFF3B2F6B, 0xFFB06AB3}, {0xFF1D4E5F, 0xFF7FB7A8}, {0xFF3A0CA3, 0xFFF72585},
            {0xFF1B1F3B, 0xFF5B3FA0}, {0xFF5A1E2B, 0xFF2E7D6B}, {0xFF6B1F1F, 0xFFF0A040},
            {0xFF0F3D7A, 0xFFE0402F}, {0xFF1F3A5F, 0xFFFF8C42}, {0xFF2F4F2F, 0xFFE8A0BF}
    };

    public final String id;
    public final String title;
    public final String synopsis;
    public final String[] genres;
    public final int episodeCount;
    public final int runtimeMinutes;
    public final boolean subAvailable;
    public final boolean dubAvailable;
    /** Poster (portrait) and backdrop (landscape) image URLs. Either can be null. */
    public final String posterUrl;
    public final String backdropUrl;
    public final int colorStart;
    public final int colorEnd;

    public Anime(String id, String title, String synopsis, String[] genres, int episodeCount,
                 int runtimeMinutes, boolean subAvailable, boolean dubAvailable,
                 String posterUrl, String backdropUrl) {
        this.id = id == null ? "" : id;
        this.title = title == null || title.trim().isEmpty() ? "Untitled" : title;
        this.synopsis = synopsis == null ? "" : synopsis;
        this.genres = genres == null ? new String[0] : genres;
        this.episodeCount = episodeCount;
        this.runtimeMinutes = runtimeMinutes;
        this.subAvailable = subAvailable;
        this.dubAvailable = dubAvailable;
        this.posterUrl = posterUrl;
        this.backdropUrl = backdropUrl;
        int[] colors = PALETTE[Math.floorMod(this.id.hashCode(), PALETTE.length)];
        this.colorStart = colors[0];
        this.colorEnd = colors[1];
    }

    /** Old constructor kept so the sample data from the UI prototype still compiles. */
    public Anime(String id, String title, String synopsis, String[] genres, int episodeCount,
                 int runtimeMinutes, boolean subAvailable, boolean dubAvailable,
                 int colorStart, int colorEnd) {
        this.id = id;
        this.title = title;
        this.synopsis = synopsis;
        this.genres = genres;
        this.episodeCount = episodeCount;
        this.runtimeMinutes = runtimeMinutes;
        this.subAvailable = subAvailable;
        this.dubAvailable = dubAvailable;
        this.posterUrl = null;
        this.backdropUrl = null;
        this.colorStart = colorStart;
        this.colorEnd = colorEnd;
    }

    // ------------------------------------------------------------ built from API answers

    public static Anime fromHero(CatalogHeroDto dto) {
        return new Anime(dto.id, dto.title, dto.synopsis, null, 0, 0, false, false,
                dto.posterImageUrl, dto.backdropImageUrl);
    }

    public static Anime fromCard(AnimeCardDto dto) {
        return new Anime(dto.id, dto.title, null, null, dto.episodeCount, 0,
                dto.subAvailable, dto.dubAvailable, dto.coverImageUrl, null);
    }

    public static Anime fromSearch(SearchAnimeResultDto dto) {
        return new Anime(dto.id, dto.title, dto.synopsis, toArray(dto.genres), dto.episodeCount, 0,
                dto.subAvailable, dto.dubAvailable, dto.posterImageUrl, null);
    }

    public static Anime fromWatchPage(WatchPageDto dto) {
        return new Anime(dto.id, dto.title, dto.synopsis, null, dto.episodeCount, dto.runtimeMinutes,
                dto.subAvailable, dto.dubAvailable, dto.posterImageUrl, dto.backdropImageUrl);
    }

    public static Anime fromHistory(WatchHistoryEntryDto dto) {
        return new Anime(dto.animeId, dto.animeTitle, null, null, 0, 0, false, false,
                dto.posterImageUrl, null);
    }

    public static Anime fromWatchList(WatchListEntry entry) {
        return new Anime(entry.title.animeId, entry.title.title, entry.metadata.synopsis, null,
                entry.metadata.episodeCount, entry.metadata.runtimeMinutes,
                entry.metadata.subAvailable, entry.metadata.dubAvailable,
                entry.title.posterImageUrl, entry.metadata.backdropImageUrl);
    }

    private static String[] toArray(List<String> list) {
        return list == null ? new String[0] : list.toArray(new String[0]);
    }

    // ------------------------------------------------------------ text helpers

    /** Up to two initials, shown on the placeholder artwork. */
    public String initials() {
        StringBuilder sb = new StringBuilder();
        for (String word : title.split("[^A-Za-z0-9]+")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
            }
            if (sb.length() == 2) {
                break;
            }
        }
        return sb.toString();
    }

    public String genreLine() {
        return String.join(" · ", genres);
    }

    /** For example "SUB · DUB · 24 eps". */
    public String badgeLine() {
        StringBuilder sb = new StringBuilder();
        if (subAvailable) {
            sb.append("SUB");
        }
        if (dubAvailable) {
            sb.append(sb.length() > 0 ? " · " : "").append("DUB");
        }
        if (episodeCount > 0) {
            sb.append(sb.length() > 0 ? " · " : "").append(String.format(Locale.US, "%,d eps", episodeCount));
        }
        return sb.toString();
    }

    public boolean hasGenre(String genre) {
        for (String g : genres) {
            if (g.equalsIgnoreCase(genre)) {
                return true;
            }
        }
        return false;
    }

    public boolean matches(String query) {
        String q = query.toLowerCase(Locale.ROOT);
        if (title.toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        for (String g : genres) {
            if (g.toLowerCase(Locale.ROOT).contains(q)) {
                return true;
            }
        }
        return false;
    }
}
