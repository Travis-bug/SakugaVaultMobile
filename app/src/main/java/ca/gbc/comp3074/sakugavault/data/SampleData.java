package ca.gbc.comp3074.sakugavault.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Hard-coded sample data for the UI prototype.
 * The next milestone replaces this with Retrofit calls to the SakugaVault REST API
 * (/api/catalog/home, /api/catalog/search, /api/watch/{id}, /api/watch/history ...).
 */
public final class SampleData {

    public static final int EPISODES_PER_SEASON = 24;

    public static final List<Anime> ALL = Collections.unmodifiableList(Arrays.asList(
            a("witch-hat", "Witch Hat Atelier", "A village girl discovers that magic is drawn, not born, and becomes an apprentice in a witch's atelier.", 12, 24, true, false, 0xFF3B2F6B, 0xFFB06AB3, "Fantasy", "Adventure"),
            a("frieren", "Frieren: Beyond Journey's End", "Long after her hero party's quest, an elf mage travels again to understand the friends she outlived.", 28, 24, true, true, 0xFF1D4E5F, 0xFF7FB7A8, "Fantasy", "Adventure", "Drama"),
            a("dandadan", "Dandadan", "A girl who believes in ghosts and a boy who believes in aliens set out to prove each other wrong.", 24, 24, true, true, 0xFF3A0CA3, 0xFFF72585, "Action", "Comedy", "Supernatural"),
            a("jjk", "Jujutsu Kaisen", "A high-school student swallows a cursed relic and joins a school of sorcerers who fight curses.", 47, 24, true, true, 0xFF1B1F3B, 0xFF5B3FA0, "Action", "Supernatural"),
            a("demon-slayer", "Demon Slayer", "A boy joins the demon-hunting corps to find a cure for his sister, who was turned into a demon.", 63, 24, true, true, 0xFF5A1E2B, 0xFF2E7D6B, "Action", "Supernatural", "Drama"),
            a("chainsaw-man", "Chainsaw Man", "A broke devil hunter fuses with his chainsaw-devil dog and is recruited by a government agency.", 12, 24, true, true, 0xFF6B1F1F, 0xFFF0A040, "Action", "Supernatural"),
            a("mob-psycho", "Mob Psycho 100", "A quiet middle-schooler with enormous psychic power just wants an ordinary life.", 37, 24, true, true, 0xFF2D2A5F, 0xFF8EC5FC, "Action", "Comedy"),
            a("naruto", "Naruto", "A loud, determined young ninja chases his dream of becoming the leader of his village.", 220, 23, true, true, 0xFFB34A12, 0xFFFFB347, "Action", "Adventure"),
            a("one-piece", "One Piece", "A rubber-bodied captain and his crew sail the seas in search of a legendary treasure.", 1100, 24, true, true, 0xFF0F3D7A, 0xFFE0402F, "Action", "Adventure", "Comedy"),
            a("spy-family", "Spy x Family", "A spy, an assassin and a mind-reading child form a pretend family, each hiding a secret.", 37, 24, true, true, 0xFF2F4F2F, 0xFFE8A0BF, "Comedy", "Action"),
            a("haikyu", "Haikyu!!", "A short but fearless player joins his high-school volleyball team and aims for nationals.", 85, 24, true, true, 0xFF1F3A5F, 0xFFFF8C42, "Sports", "Comedy"),
            a("blue-lock", "Blue Lock", "Three hundred young strikers compete in an extreme training program to find one ace.", 38, 24, true, true, 0xFF0B1E4A, 0xFF2EC4F1, "Sports"),
            a("bebop", "Cowboy Bebop", "A crew of bounty hunters drifts through space, chasing jobs and running from their past.", 26, 24, true, true, 0xFF2B1B17, 0xFFC8553D, "Sci-Fi", "Action"),
            a("steins-gate", "Steins;Gate", "A self-proclaimed mad scientist finds a way to send messages into the past.", 24, 24, true, true, 0xFF1E2A22, 0xFF8FB339, "Sci-Fi", "Drama"),
            a("your-lie", "Your Lie in April", "A pianist who stopped playing meets a free-spirited violinist who pulls him back to music.", 22, 23, true, true, 0xFF2A3B6B, 0xFFF7C8D0, "Romance", "Drama"),
            a("kaguya", "Kaguya-sama: Love Is War", "Two student-council geniuses scheme to make the other one confess first.", 37, 24, true, true, 0xFF5B1A3A, 0xFFF28AB2, "Romance", "Comedy"),
            a("wistoria", "Wistoria: Wand and Sword", "A student with no magic at all aims for the top of a magic academy with only his sword.", 12, 24, true, false, 0xFF1C2541, 0xFF5BC0BE, "Fantasy", "Action"),
            a("bleach-tybw", "Bleach: Thousand-Year Blood War", "Ichigo and the Soul Reapers face an invasion from an army of Quincy.", 26, 24, true, true, 0xFF101010, 0xFF8C8C8C, "Action", "Supernatural")
    ));

    /** Genre rows on the Home screen (the API returns these as GenreRailDto). */
    public static final String[] RAIL_GENRES = {"Action", "Fantasy", "Comedy", "Sports", "Sci-Fi", "Romance"};

    /** Filter chips on the Search screen. */
    public static final String[] SEARCH_GENRES = {"All", "Action", "Fantasy", "Comedy", "Supernatural", "Sports", "Sci-Fi", "Romance", "Drama"};

    /** Bookmarked titles (Could Have: stored in Room in the real app). */
    public static final Set<String> WATCHLIST = new LinkedHashSet<>();

    private static final List<HistoryEntry> HISTORY = new ArrayList<>();

    static {
        HISTORY.add(new HistoryEntry(find("naruto"), 5, 65));
        HISTORY.add(new HistoryEntry(find("frieren"), 9, 12));
        HISTORY.add(new HistoryEntry(find("spy-family"), 3, 40));
        HISTORY.add(new HistoryEntry(find("jjk"), 24, 100));
    }

    private SampleData() {
    }

    private static Anime a(String id, String title, String synopsis, int episodes, int runtime,
                           boolean sub, boolean dub, int c1, int c2, String... genres) {
        return new Anime(id, title, synopsis, genres, episodes, runtime, sub, dub, c1, c2);
    }

    public static Anime find(String id) {
        for (Anime anime : ALL) {
            if (anime.id.equals(id)) {
                return anime;
            }
        }
        return null;
    }

    public static List<Anime> hero() {
        return Arrays.asList(find("witch-hat"), find("frieren"), find("dandadan"), find("jjk"), find("blue-lock"));
    }

    public static List<Anime> byGenre(String genre) {
        List<Anime> out = new ArrayList<>();
        for (Anime anime : ALL) {
            if (anime.hasGenre(genre)) {
                out.add(anime);
            }
        }
        return out;
    }

    /** Same matching the API's /api/catalog/search does: title or genre, optionally narrowed by a chip. */
    public static List<Anime> search(String query, String genre) {
        List<Anime> out = new ArrayList<>();
        for (Anime anime : ALL) {
            boolean genreOk = "All".equals(genre) || anime.hasGenre(genre);
            boolean queryOk = query.isEmpty() || anime.matches(query);
            if (genreOk && queryOk) {
                out.add(anime);
            }
        }
        return out;
    }

    public static List<Anime> similarTo(Anime anime) {
        List<Anime> out = new ArrayList<>();
        for (Anime other : ALL) {
            if (!other.id.equals(anime.id) && other.hasGenre(anime.genres[0])) {
                out.add(other);
            }
        }
        return out;
    }

    /** Titles in progress (Continue Watching row). */
    public static List<HistoryEntry> continueWatching() {
        List<HistoryEntry> out = new ArrayList<>();
        for (HistoryEntry entry : HISTORY) {
            if (entry.percent < 100) {
                out.add(entry);
            }
        }
        return out;
    }

    /** Everything recently watched, including finished episodes (Profile screen). */
    public static List<HistoryEntry> recentHistory() {
        return new ArrayList<>(HISTORY);
    }

    /** Called by the player; the real app also POSTs this to /api/watch/history. */
    public static void recordProgress(Anime anime, int episode, int percent) {
        for (int i = 0; i < HISTORY.size(); i++) {
            if (HISTORY.get(i).anime.id.equals(anime.id)) {
                HISTORY.remove(i);
                break;
            }
        }
        HISTORY.add(0, new HistoryEntry(anime, episode, percent));
    }

    public static int completedCount() {
        return 12;
    }

    public static int seasonCount(Anime anime) {
        return Math.max(1, (anime.episodeCount + EPISODES_PER_SEASON - 1) / EPISODES_PER_SEASON);
    }

    public static List<Comment> commentsFor(Anime anime) {
        List<Comment> out = new ArrayList<>();
        out.add(new Comment("kai_92", "2h", "Rewatching this before the next season. Still holds up."));
        out.add(new Comment("sora.dev", "5h", "The animation in the second half of this episode is unreal."));
        out.add(new Comment("mika", "1d", "Tried the dub this time and it's better than I expected."));
        return out;
    }

    public static class HistoryEntry {
        public final Anime anime;
        public final int episode;
        public final int percent;

        public HistoryEntry(Anime anime, int episode, int percent) {
            this.anime = anime;
            this.episode = episode;
            this.percent = percent;
        }

        public int minutesLeft() {
            return Math.max(1, anime.runtimeMinutes * (100 - percent) / 100);
        }
    }

    public static class Comment {
        public final String user;
        public final String when;
        public final String body;

        public Comment(String user, String when, String body) {
            this.user = user;
            this.when = when;
            this.body = body;
        }
    }
}
