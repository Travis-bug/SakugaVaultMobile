package ca.gbc.comp3074.sakugavault.player;

/**
 * Openly licensed videos the player shows instead of a live stream, so playback works without
 * the back end's scrapers (which depend on third-party sites that change and go down).
 * Both are Blender Foundation open movies (CC BY 3.0), served as HLS by public test servers.
 * To use other videos, change this list: any HLS (.m3u8) or MP4 link works.
 */
public final class DemoVideos {

    /** One demo video and the credit its licence asks for. */
    public static final class Video {
        public final String title;
        public final String credit;
        public final String url;

        Video(String title, String credit, String url) {
            this.title = title;
            this.credit = credit;
            this.url = url;
        }
    }

    private static final Video[] VIDEOS = {
            new Video("Big Buck Bunny", "© 2008 Blender Foundation, CC BY 3.0",
                    "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
            // This one has subtitle tracks, so it also shows the subtitles button working.
            new Video("Tears of Steel", "© 2012 Blender Foundation, CC BY 3.0",
                    "https://test-streams.mux.dev/tos_ismc/main.m3u8"),
    };

    private DemoVideos() {
    }

    /** Odd episodes get the first video and even episodes the second, so changing episode changes the video. */
    public static Video forEpisode(int episode) {
        return VIDEOS[Math.floorMod(episode - 1, VIDEOS.length)];
    }
}
