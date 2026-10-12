package ca.gbc.comp3074.sakugavault.data;

import java.util.Locale;

/**
 * One anime title. In the finished app this is filled from the SakugaVault API
 * (AnimeCardDto / WatchPageDto) and cached in Room; in this prototype it comes from SampleData.
 */
public class Anime {
    public final String id;
    public final String title;
    public final String synopsis;
    public final String[] genres;
    public final int episodeCount;
    public final int runtimeMinutes;
    public final boolean subAvailable;
    public final boolean dubAvailable;
    /** Two colours for the placeholder artwork gradient (real cover art comes later, loaded with Glide). */
    public final int colorStart;
    public final int colorEnd;

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
        this.colorStart = colorStart;
        this.colorEnd = colorEnd;
    }

    /** Up to two initials, used on the placeholder artwork. */
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
            sb.append("SUB · ");
        }
        if (dubAvailable) {
            sb.append("DUB · ");
        }
        sb.append(String.format(Locale.US, "%,d eps", episodeCount));
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
