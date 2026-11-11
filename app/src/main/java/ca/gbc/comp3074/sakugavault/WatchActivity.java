package ca.gbc.comp3074.sakugavault;

import ca.gbc.comp3074.sakugavault.services.DeveloperLogger;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ca.gbc.comp3074.sakugavault.api.ApiCallback;
import ca.gbc.comp3074.sakugavault.api.ApiClient;
import ca.gbc.comp3074.sakugavault.api.ApiError;
import ca.gbc.comp3074.sakugavault.api.dto.AnimeCardDto;
import ca.gbc.comp3074.sakugavault.api.dto.CommentDto;
import ca.gbc.comp3074.sakugavault.api.dto.CommentPostedDto;
import ca.gbc.comp3074.sakugavault.api.dto.EpisodeListResponseDto;
import ca.gbc.comp3074.sakugavault.api.dto.PlaybackSubtitleDto;
import ca.gbc.comp3074.sakugavault.api.dto.ResolvedPlaybackDto;
import ca.gbc.comp3074.sakugavault.api.dto.WatchEpisodeDto;
import ca.gbc.comp3074.sakugavault.api.dto.WatchPageDto;
import ca.gbc.comp3074.sakugavault.api.dto.WatchSeasonDto;
import ca.gbc.comp3074.sakugavault.data.Anime;
import ca.gbc.comp3074.sakugavault.data.PlaybackPrefs;
import ca.gbc.comp3074.sakugavault.data.SessionStore;
import ca.gbc.comp3074.sakugavault.data.local.WatchListEntry;
import ca.gbc.comp3074.sakugavault.player.DemoVideos;
import ca.gbc.comp3074.sakugavault.player.PlayerFactory;
import ca.gbc.comp3074.sakugavault.services.AppServices;
import ca.gbc.comp3074.sakugavault.services.catalog.ICatalogService;
import ca.gbc.comp3074.sakugavault.services.watch.IWatchHistoryService;
import ca.gbc.comp3074.sakugavault.services.watch.IWatchListService;
import ca.gbc.comp3074.sakugavault.services.watch.IWatchPageService;
import ca.gbc.comp3074.sakugavault.ui.CommentAdapter;
import ca.gbc.comp3074.sakugavault.ui.EpisodeAdapter;
import ca.gbc.comp3074.sakugavault.ui.PosterAdapter;
import ca.gbc.comp3074.sakugavault.ui.Ui;

/**
 * Title page and player (FR-03, FR-04, FR-05, FR-09).
 * Real data: details, seasons, comments and similar titles from GET /api/watch/{id};
 * the provider's episode list from GET /api/watch/{id}/episodes; progress saved to
 * POST /api/watch/history; comments posted to POST /api/catalog/comments; and the
 * watchlist button saves the show in Room through IWatchListService.
 * Video: a Media3 ExoPlayer plays the stream from POST /api/watch/{id}/resolve-playback, or an
 * openly licensed demo video (player/DemoVideos) when "Demo videos" is on or no stream is found.
 */
@OptIn(markerClass = UnstableApi.class) // Media3's full-screen button API
public class WatchActivity extends AppCompatActivity {

    private static final String EXTRA_ID = "anime_id";
    private static final String EXTRA_EPISODE = "episode";
    private static final String EXTRA_POSITION = "position_seconds";
    private static final int SAVE_EVERY_SECONDS = 15;
    private static final int DEFAULT_RUNTIME_MINUTES = 24;
    /** How long to wait for the scrapers before playing a demo video instead. */
    private static final long RESOLVE_TIMEOUT_MS = 20_000;

    public static void start(Context context, String animeId, int episode, int positionSeconds) {
        Intent intent = new Intent(context, WatchActivity.class);
        intent.putExtra(EXTRA_ID, animeId);
        intent.putExtra(EXTRA_EPISODE, episode);
        intent.putExtra(EXTRA_POSITION, positionSeconds);
        context.startActivity(intent);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());

    private IWatchPageService watchPages;
    private IWatchHistoryService history;
    private ICatalogService catalog;
    private IWatchListService watchList;

    private String animeId;
    private Anime anime;
    private List<WatchSeasonDto> seasons = new ArrayList<>();
    private int seasonIndex;
    private int episode;
    private int positionSeconds;
    private int durationSeconds;
    private boolean loaded;
    private boolean fullscreen;
    private boolean inWatchList;
    /** Saved ids from Room; observed for the whole life of the page. */
    private LiveData<List<String>> savedIds;
    private boolean watchlistBusy;

    private View topBar;
    private View detailsScroll;
    private View playerContainer;
    private ImageView playerImage;
    private TextView playerArt;
    private PlayerView playerView;
    private TextView tvDemoBadge;
    private TextView tvResolving;
    private View playerError;
    private TextView tvPlayerError;
    private TextView tvStreamInfo;
    private MaterialSwitch switchDemo;
    private MaterialButtonToggleGroup langToggle;
    private TextView tvTopTitle;
    private TextView tvSyncStatus;
    private TextView tvLangInfo;
    private TextView tvCommentsHeader;
    private TextView tvEpisodesNote;
    private TextView tvLoadMessage;
    private View loadStatus;
    private View loadProgress;
    private View btnRetry;
    private View detailsContent;
    private MaterialButton btnWatchlist;
    private MaterialButton btnPost;
    private Spinner seasonSpinner;
    private TextInputEditText etComment;
    private EpisodeAdapter episodeAdapter;
    private CommentAdapter commentAdapter;
    private PosterAdapter similarAdapter;

    // ------------------------------------------------------------------ player state

    private ExoPlayer player;
    /** What is playing, kept so the player can be rebuilt when the page comes back from the background. */
    private MediaItem currentItem;
    private boolean currentIsDemo;
    /** Where to continue after the player was released in onStop(). */
    private long resumePositionMs;
    /** True until a new video is ready, so the saved position can be checked against its real length. */
    private boolean checkResumePosition;
    /** Goes up with every lookup, so an answer that arrives after the viewer moved on is ignored. */
    private int resolveGeneration;

    /** Saves progress every 15 seconds while the video is playing. */
    private final Runnable progressTicker = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                saveProgress();
            }
            handler.postDelayed(this, SAVE_EVERY_SECONDS * 1000L);
        }
    };

    /** The scrapers can be slow; after RESOLVE_TIMEOUT_MS a demo video plays instead. */
    private final Runnable resolveTimeout = () -> {
        resolveGeneration++; // ignore the answer if it still arrives
        playDemo("looking for a stream took longer than " + RESOLVE_TIMEOUT_MS / 1000 + " seconds.");
    };

    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            handler.removeCallbacks(progressTicker);
            if (isPlaying) {
                handler.postDelayed(progressTicker, SAVE_EVERY_SECONDS * 1000L);
            } else if (player != null && player.getPlaybackState() == Player.STATE_READY
                    && !player.getPlayWhenReady()
                    && getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                saveProgress(); // the viewer pressed pause (leaving the page saves in onPause)
            }
        }

        @Override
        public void onPlaybackStateChanged(int state) {
            if (player == null) {
                return;
            }
            if (state == Player.STATE_READY && checkResumePosition) {
                checkResumePosition = false;
                long durationMs = player.getDuration();
                if (durationMs != C.TIME_UNSET && player.getCurrentPosition() >= durationMs - 5_000) {
                    player.seekTo(0); // the saved spot is at the very end of this video, so start over
                }
                syncPositionFromPlayer();
            } else if (state == Player.STATE_ENDED) {
                saveProgress(); // reached the end, so it counts as watched
            }
        }

        @Override
        public void onPlayerError(PlaybackException error) {
            syncPositionFromPlayer();
            showPlayerError((currentIsDemo ? "The demo video" : "This stream")
                    + " couldn't be played (" + error.getErrorCodeName() + ").");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch);
        Ui.padForSystemBars(findViewById(R.id.root));

        watchPages = AppServices.watchPage(this);
        history = AppServices.watchHistory(this);
        catalog = AppServices.catalog(this);
        watchList = AppServices.watchList(this);

        animeId = getIntent().getStringExtra(EXTRA_ID);
        episode = Math.max(1, getIntent().getIntExtra(EXTRA_EPISODE, 1));
        positionSeconds = Math.max(0, getIntent().getIntExtra(EXTRA_POSITION, 0));

        findViews();
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (fullscreen) {
                    setFullscreen(false);
                } else {
                    finish();
                }
            }
        });

        setUpPlayer();
        setUpLists();
        btnRetry.setOnClickListener(v -> loadPage());
        btnWatchlist.setOnClickListener(v -> toggleWatchlist());

        // The button follows the Room tables, so it is right even if the show was saved from Home.
        savedIds = watchList.getSavedIds();
        savedIds.observe(this, ids -> {
            inWatchList = ids != null && ids.contains(animeId);
            updateWatchlistButton();
        });

        applyLayout();
        tvTopTitle.setText("Loading…");
        loadPage();
    }

    private void findViews() {
        topBar = findViewById(R.id.topBar);
        detailsScroll = findViewById(R.id.detailsScroll);
        playerContainer = findViewById(R.id.playerContainer);
        playerImage = findViewById(R.id.playerImage);
        playerArt = findViewById(R.id.playerArt);
        playerView = findViewById(R.id.playerView);
        tvDemoBadge = findViewById(R.id.tvDemoBadge);
        tvResolving = findViewById(R.id.tvResolving);
        playerError = findViewById(R.id.playerError);
        tvPlayerError = findViewById(R.id.tvPlayerError);
        tvStreamInfo = findViewById(R.id.tvStreamInfo);
        switchDemo = findViewById(R.id.switchDemo);
        langToggle = findViewById(R.id.langToggle);
        tvTopTitle = findViewById(R.id.tvTopTitle);
        tvSyncStatus = findViewById(R.id.tvSyncStatus);
        tvLangInfo = findViewById(R.id.tvLangInfo);
        tvCommentsHeader = findViewById(R.id.tvCommentsHeader);
        tvEpisodesNote = findViewById(R.id.tvEpisodesNote);
        tvLoadMessage = findViewById(R.id.tvLoadMessage);
        loadStatus = findViewById(R.id.loadStatus);
        loadProgress = findViewById(R.id.loadProgress);
        btnRetry = findViewById(R.id.btnRetry);
        detailsContent = findViewById(R.id.detailsContent);
        btnWatchlist = findViewById(R.id.btnWatchlist);
        btnPost = findViewById(R.id.btnPost);
        seasonSpinner = findViewById(R.id.seasonSpinner);
        etComment = findViewById(R.id.etComment);
    }

    private boolean gone() {
        return isFinishing() || isDestroyed();
    }

    // ------------------------------------------------------------------ loading the title

    private void loadPage() {
        loadStatus.setVisibility(View.VISIBLE);
        loadProgress.setVisibility(View.VISIBLE);
        btnRetry.setVisibility(View.GONE);
        tvLoadMessage.setText("Loading title…");
        detailsContent.setVisibility(View.GONE);

        watchPages.getWatchPage(animeId, new ApiCallback<WatchPageDto>() {
            @Override
            public void onSuccess(WatchPageDto page) {
                if (gone()) {
                    return;
                }
                if (page == null || page.id == null) {
                    showLoadError("The requested item could not be found.");
                    return;
                }
                bindPage(page);
                loadProviderEpisodes();
            }

            @Override
            public void onError(ApiError error) {
                if (gone() || Ui.handleSessionExpired(WatchActivity.this, error)) {
                    return;
                }
                showLoadError(error.message);
            }
        });
    }

    private void showLoadError(String message) {
        loadProgress.setVisibility(View.GONE);
        btnRetry.setVisibility(View.VISIBLE);
        tvLoadMessage.setText("Couldn't load this title.\n" + message);
        tvTopTitle.setText("Title unavailable");
    }

    private void bindPage(WatchPageDto page) {
        animeId = page.id;
        anime = Anime.fromWatchPage(page);
        loaded = true;
        loadStatus.setVisibility(View.GONE);
        detailsContent.setVisibility(View.VISIBLE);

        int runtime = anime.runtimeMinutes > 0 ? anime.runtimeMinutes : DEFAULT_RUNTIME_MINUTES;
        durationSeconds = runtime * 60;
        if (positionSeconds >= durationSeconds) {
            positionSeconds = 0;
        }

        // Player background: the backdrop image over the title's gradient.
        playerContainer.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{anime.colorStart, 0xFF000000}));
        String image = anime.backdropUrl != null ? anime.backdropUrl : anime.posterUrl;
        Ui.loadImage(playerImage, image);
        playerArt.setText(anime.title.toUpperCase(Locale.ROOT));
        playerArt.setVisibility(image == null ? View.VISIBLE : View.GONE);

        setUpDetails(page);
        seasons = page.seasons == null ? new ArrayList<>() : page.seasons;
        setUpSeasons();

        commentAdapter.submit(page.comments);
        updateCommentsHeader();

        List<Anime> similar = new ArrayList<>();
        if (page.similarAnime != null) {
            for (AnimeCardDto card : page.similarAnime) {
                similar.add(Anime.fromCard(card));
            }
        }
        similarAdapter.submit(similar);
        int similarVisibility = similar.isEmpty() ? View.GONE : View.VISIBLE;
        findViewById(R.id.tvSimilarHeader).setVisibility(similarVisibility);
        findViewById(R.id.similarList).setVisibility(similarVisibility);

        // Use the list Room has already delivered (asking for a new LiveData here would start empty).
        List<String> ids = savedIds.getValue();
        inWatchList = ids != null && ids.contains(animeId);
        updateWatchlistButton();
        updateTitle();
        tvSyncStatus.setText(positionSeconds > 0
                ? "Resuming at " + format(positionSeconds) + " (saved to your account)"
                : "Progress saves every 15 seconds and syncs with the website");

        resolveStream();
    }

    /** Asks for the provider's real episode list; until it arrives the page's placeholder list is shown. */
    private void loadProviderEpisodes() {
        tvEpisodesNote.setText("Checking the full episode list…");
        tvEpisodesNote.setVisibility(View.VISIBLE);
        watchPages.getEpisodes(animeId, new ApiCallback<EpisodeListResponseDto>() {
            @Override
            public void onSuccess(EpisodeListResponseDto response) {
                if (gone()) {
                    return;
                }
                if (response != null && response.isResolved && response.seasons != null && !response.seasons.isEmpty()) {
                    seasons = response.seasons;
                    setUpSeasons();
                    tvEpisodesNote.setVisibility(View.GONE);
                } else if (response != null && response.statusMessage != null && !response.statusMessage.isEmpty()) {
                    tvEpisodesNote.setText(response.statusMessage);
                } else {
                    tvEpisodesNote.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(ApiError error) {
                if (gone() || Ui.handleSessionExpired(WatchActivity.this, error)) {
                    return;
                }
                tvEpisodesNote.setVisibility(View.GONE);
            }
        });
    }

    // ------------------------------------------------------------------ player (Media3 ExoPlayer)

    private void setUpPlayer() {
        // The player's own full-screen button rotates the screen; applyLayout() does the rest.
        playerView.setFullscreenButtonClickListener(this::setFullscreen);
        findViewById(R.id.btnPlayerRetry).setOnClickListener(v -> resolveStream());

        switchDemo.setChecked(PlaybackPrefs.useDemoVideos(this));
        switchDemo.setOnCheckedChangeListener((button, checked) -> {
            PlaybackPrefs.setUseDemoVideos(this, checked);
            resolveStream();
        });
    }

    /** Builds the player. Video requests go through the app's OkHttp client, so the playback cookie is sent. */
    private void createPlayer() {
        player = PlayerFactory.create(this, ApiClient.mediaClient(this));
        player.addListener(playerListener);
        playerView.setPlayer(player);
    }

    /** Remembers the position and frees the decoder (the page is no longer visible). */
    private void releasePlayer() {
        if (player == null) {
            return;
        }
        syncPositionFromPlayer();
        resumePositionMs = positionSeconds * 1000L;
        handler.removeCallbacks(progressTicker);
        player.removeListener(playerListener);
        player.release();
        player = null;
        playerView.setPlayer(null);
    }

    /**
     * Picks what to play for the current episode and language.
     * Demo videos on: this episode's demo video straight away.
     * Off: POST /api/watch/{id}/resolve-playback, then the live stream, or a demo video when the
     * back end finds nothing or takes longer than RESOLVE_TIMEOUT_MS.
     */
    private void resolveStream() {
        if (!loaded) {
            return;
        }
        int generation = ++resolveGeneration;
        handler.removeCallbacks(resolveTimeout);
        syncPositionFromPlayer(); // switching SUB/DUB keeps your place
        stopPlayback();
        if (PlaybackPrefs.useDemoVideos(this)) {
            playDemo(null);
            return;
        }
        showPlayerStatus("Finding a stream for episode " + episode + "…");
        tvStreamInfo.setText("Looking for a live stream through the SakugaVault back end…");
        handler.postDelayed(resolveTimeout, RESOLVE_TIMEOUT_MS);
        watchPages.resolvePlayback(animeId, episode, isDubSelected(), new ApiCallback<ResolvedPlaybackDto>() {
            @Override
            public void onSuccess(ResolvedPlaybackDto result) {
                if (gone() || generation != resolveGeneration) {
                    return;
                }
                handler.removeCallbacks(resolveTimeout);
                if (result != null && result.isResolved
                        && result.streamUrl != null && !result.streamUrl.trim().isEmpty()) {
                    playLive(result);
                } else if (result != null && result.statusMessage != null && !result.statusMessage.isEmpty()) {
                    playDemo(result.statusMessage);
                } else {
                    playDemo("none of the source sites had it.");
                }
            }

            @Override
            public void onError(ApiError error) {
                if (gone() || generation != resolveGeneration) {
                    return;
                }
                handler.removeCallbacks(resolveTimeout);
                if (Ui.handleSessionExpired(WatchActivity.this, error)) {
                    return;
                }
                playDemo(error.message);
            }
        });
    }

    private boolean isDubSelected() {
        return langToggle.getCheckedButtonId() == R.id.btnDub;
    }

    /** Plays the stream the back end found, with any subtitle files it returned. */
    private void playLive(ResolvedPlaybackDto result) {
        String url = ApiClient.absoluteUrl(result.streamUrl);
        MediaItem.Builder item = new MediaItem.Builder()
                .setUri(url)
                .setSubtitleConfigurations(subtitleConfigs(result.subtitles, result.subtitleLanguage));
        // Proxied streams (/api/watch/streams/{id}) have no ".m3u8" in the link, so say it's HLS.
        if ("HLS".equalsIgnoreCase(result.preferredProtocol) || url.toLowerCase(Locale.ROOT).contains(".m3u8")) {
            item.setMimeType(MimeTypes.APPLICATION_M3U8);
        }
        String source = result.sourceHost == null || result.sourceHost.isEmpty() ? "the back end" : result.sourceHost;
        tvStreamInfo.setText("Live stream from " + source + (result.usedFallback ? " (backup source)." : "."));
        tvDemoBadge.setVisibility(View.GONE);
        currentIsDemo = false;
        playItem(item.build());
    }

    /** Plays this episode's demo video. reason says why there is no live stream (null when Demo videos is on). */
    private void playDemo(String reason) {
        DemoVideos.Video video = DemoVideos.forEpisode(episode);
        String credit = "Demo video: " + video.title + " (" + video.credit + ").";
        tvStreamInfo.setText(reason == null
                ? credit + " Turn off Demo videos to look up the live stream instead."
                : "No live stream for episode " + episode + ": " + reason + "\n" + credit);
        tvDemoBadge.setVisibility(View.VISIBLE);
        currentIsDemo = true;
        playItem(new MediaItem.Builder()
                .setUri(video.url)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build());
    }

    /** Starts a new video at this episode's saved position (or waits for onStart() if the page is hidden). */
    private void playItem(MediaItem item) {
        currentItem = item;
        resumePositionMs = positionSeconds * 1000L;
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            startItem(item, resumePositionMs, true);
        }
    }

    private void startItem(MediaItem item, long startPositionMs, boolean autoplay) {
        if (player == null) {
            createPlayer();
        }
        hidePlayerStatus();
        playerError.setVisibility(View.GONE);
        playerView.setVisibility(View.VISIBLE);
        // Subtitles: English ones are switched on automatically for SUB, none for DUB.
        boolean dub = isDubSelected();
        player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon()
                .setPreferredTextLanguage(dub ? null : "en")
                .setSelectUndeterminedTextLanguage(!dub)
                .build());
        checkResumePosition = true;
        player.setMediaItem(item, Math.max(0, startPositionMs));
        player.prepare();
        player.setPlayWhenReady(autoplay);
    }

    /** Stops the current video, before another one is looked up. */
    private void stopPlayback() {
        handler.removeCallbacks(progressTicker);
        currentItem = null;
        if (player != null) {
            player.stop();
            player.clearMediaItems();
        }
        playerView.setVisibility(View.INVISIBLE);
        playerError.setVisibility(View.GONE);
        tvDemoBadge.setVisibility(View.GONE);
        hidePlayerStatus();
    }

    private void showPlayerStatus(String message) {
        tvResolving.setText(message);
        tvResolving.setVisibility(View.VISIBLE);
    }

    private void hidePlayerStatus() {
        tvResolving.setVisibility(View.GONE);
    }

    /** The clear error message with a Retry button that FR-04 asks for. */
    private void showPlayerError(String message) {
        hidePlayerStatus();
        playerView.setVisibility(View.INVISIBLE);
        tvPlayerError.setText(message);
        playerError.setVisibility(View.VISIBLE);
    }

    /** Subtitle files that come with a live stream; the player's subtitles button lists them. */
    private List<MediaItem.SubtitleConfiguration> subtitleConfigs(List<PlaybackSubtitleDto> subtitles,
                                                                  String language) {
        List<MediaItem.SubtitleConfiguration> configs = new ArrayList<>();
        if (subtitles == null) {
            return configs;
        }
        String wanted = (language == null || language.isEmpty() ? "en" : language).toLowerCase(Locale.ROOT);
        boolean defaultChosen = isDubSelected(); // no subtitles switched on for DUB
        for (PlaybackSubtitleDto subtitle : subtitles) {
            String url = ApiClient.absoluteUrl(subtitle.url);
            if (url == null) {
                continue;
            }
            String path = url.toLowerCase(Locale.ROOT);
            String mimeType = path.contains(".srt") ? MimeTypes.APPLICATION_SUBRIP
                    : path.contains(".ass") || path.contains(".ssa") ? MimeTypes.TEXT_SSA
                    : MimeTypes.TEXT_VTT;
            boolean isDefault = !defaultChosen && subtitle.language != null
                    && subtitle.language.toLowerCase(Locale.ROOT).startsWith(wanted);
            defaultChosen |= isDefault;
            configs.add(new MediaItem.SubtitleConfiguration.Builder(Uri.parse(url))
                    .setMimeType(mimeType)
                    .setLanguage(subtitle.language)
                    .setLabel(subtitle.label)
                    .setSelectionFlags(isDefault ? C.SELECTION_FLAG_DEFAULT : 0)
                    .build());
        }
        return configs;
    }

    /** Copies the player's position, and the video's real length once known, into the progress fields. */
    private void syncPositionFromPlayer() {
        if (player == null || player.getCurrentMediaItem() == null) {
            return;
        }
        positionSeconds = (int) (Math.max(0, player.getCurrentPosition()) / 1000);
        long durationMs = player.getDuration();
        if (durationMs != C.TIME_UNSET && durationMs > 0) {
            durationSeconds = (int) (durationMs / 1000);
        }
    }

    private static String format(int seconds) {
        return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60);
    }

    private void saveProgress() {
        saveProgress(false);
    }

    /**
     * POST /api/watch/history — the same tracked history the website shows on the Profile page.
     * When leaving the page, the save is followed by a flush so Continue Watching is up to date.
     */
    private void saveProgress(boolean flushAfter) {
        syncPositionFromPlayer();
        if (!loaded || durationSeconds <= 0) {
            return;
        }
        boolean completed = positionSeconds >= durationSeconds * 9 / 10; // 90% watched counts as finished
        if (flushAfter) {
            history.saveProgressAndFlush(animeId, episode, positionSeconds, durationSeconds, completed);
        } else {
            history.saveProgress(animeId, episode, positionSeconds, durationSeconds, completed);
        }
        tvSyncStatus.setText("✓ Progress saved at " + format(positionSeconds) + " (episode " + episode + ")");
    }

    // ------------------------------------------------------------------ full screen

    private void setFullscreen(boolean on) {
        setRequestedOrientation(on
                ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if (!on) {
            // Give rotation back to the sensor after returning to portrait.
            handler.postDelayed(() -> setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED), 1500);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyLayout();
    }

    /** Portrait: 16:9 player with details below. Landscape: player fills the screen. */
    private void applyLayout() {
        fullscreen = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        ViewGroup.LayoutParams params = playerContainer.getLayoutParams();
        if (fullscreen) {
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        } else {
            int width = getResources().getDisplayMetrics().widthPixels;
            params.height = width * 9 / 16;
        }
        playerContainer.setLayoutParams(params);
        topBar.setVisibility(fullscreen ? View.GONE : View.VISIBLE);
        detailsScroll.setVisibility(fullscreen ? View.GONE : View.VISIBLE);
        playerView.setFullscreenButtonState(fullscreen);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (fullscreen) {
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            controller.hide(WindowInsetsCompat.Type.systemBars());
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars());
        }
    }

    // ------------------------------------------------------------------ details

    private void setUpDetails(WatchPageDto page) {
        ((TextView) findViewById(R.id.tvTitle)).setText(anime.title);
        StringBuilder meta = new StringBuilder(anime.badgeLine());
        if (anime.runtimeMinutes > 0) {
            meta.append(meta.length() > 0 ? " · " : "").append(anime.runtimeMinutes).append(" min");
        }
        ((TextView) findViewById(R.id.tvMeta)).setText(meta);

        TextView synopsis = findViewById(R.id.tvSynopsis);
        TextView more = findViewById(R.id.tvMore);
        synopsis.setText(anime.synopsis.isEmpty() ? "No synopsis yet." : anime.synopsis);
        synopsis.setMaxLines(2);
        more.setText("More");
        more.setOnClickListener(v -> {
            boolean expanded = synopsis.getMaxLines() > 2;
            synopsis.setMaxLines(expanded ? 2 : 20);
            more.setText(expanded ? "More" : "Less");
        });

        MaterialButton btnSub = findViewById(R.id.btnSub);
        MaterialButton btnDub = findViewById(R.id.btnDub);
        btnSub.setEnabled(anime.subAvailable || !anime.dubAvailable);
        btnDub.setEnabled(anime.dubAvailable);
        if (!anime.subAvailable && anime.dubAvailable) {
            langToggle.check(R.id.btnDub);
        }
        showLanguage(page, langToggle.getCheckedButtonId() == R.id.btnDub);
        langToggle.clearOnButtonCheckedListeners();
        langToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                showLanguage(page, checkedId == R.id.btnDub);
                resolveStream();
            }
        });
    }

    private void showLanguage(WatchPageDto page, boolean dub) {
        String text = dub ? "Audio: English dub" : "Audio: Japanese · Subtitles: English";
        if (!anime.dubAvailable) {
            text += " · No dub yet";
        }
        tvLangInfo.setText(text);
    }

    private void updateTitle() {
        tvTopTitle.setText(anime == null ? "Loading…" : anime.title + " · Episode " + episode);
    }

    // ------------------------------------------------------------------ watchlist

    /** Adds or removes this show. Adding grabs the title + metadata from the back end and saves them in Room. */
    private void toggleWatchlist() {
        if (!loaded || watchlistBusy) {
            return;
        }
        watchlistBusy = true;
        btnWatchlist.setEnabled(false);
        if (inWatchList) {
            watchList.remove(animeId, () -> {
                watchlistBusy = false;
                updateWatchlistButton();
                Toast.makeText(this, "Removed from your watchlist", Toast.LENGTH_SHORT).show();
            });
            return;
        }
        btnWatchlist.setText("Saving…");
        watchList.add(animeId, new ApiCallback<WatchListEntry>() {
            @Override
            public void onSuccess(WatchListEntry entry) {
                if (gone()) {
                    return;
                }
                watchlistBusy = false;
                inWatchList = true;
                updateWatchlistButton();
                Toast.makeText(WatchActivity.this, "Saved to your watchlist", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(ApiError error) {
                if (gone()) {
                    return;
                }
                watchlistBusy = false;
                updateWatchlistButton();
                if (!Ui.handleSessionExpired(WatchActivity.this, error)) {
                    Toast.makeText(WatchActivity.this, error.message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void updateWatchlistButton() {
        if (btnWatchlist == null || watchlistBusy) {
            return;
        }
        btnWatchlist.setEnabled(loaded);
        btnWatchlist.setText(inWatchList ? "In Watchlist" : "Add to Watchlist");
        btnWatchlist.setIconResource(inWatchList ? R.drawable.ic_check : R.drawable.ic_add);
    }

    // ------------------------------------------------------------------ episodes

    private void setUpLists() {
        RecyclerView episodes = findViewById(R.id.episodeList);
        episodes.setLayoutManager(new LinearLayoutManager(this));
        episodeAdapter = new EpisodeAdapter(this::playEpisode);
        episodes.setAdapter(episodeAdapter);

        RecyclerView comments = findViewById(R.id.commentList);
        comments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter();
        comments.setAdapter(commentAdapter);
        btnPost.setOnClickListener(v -> postComment());

        RecyclerView similar = findViewById(R.id.similarList);
        similar.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        similarAdapter = new PosterAdapter(new ArrayList<>(), false, other -> start(this, other.id, 1, 0));
        similar.setAdapter(similarAdapter);
    }

    private void setUpSeasons() {
        if (seasons.isEmpty()) {
            seasonSpinner.setVisibility(View.GONE);
            episodeAdapter.submit(new ArrayList<>(), episode);
            tvEpisodesNote.setText("No episodes are listed for this title yet.");
            tvEpisodesNote.setVisibility(View.VISIBLE);
            return;
        }
        seasonIndex = 0;
        String[] labels = new String[seasons.size()];
        for (int i = 0; i < seasons.size(); i++) {
            WatchSeasonDto season = seasons.get(i);
            labels[i] = season.label == null || season.label.isEmpty() ? "Season " + (i + 1) : season.label;
            if (containsEpisode(season, episode)) {
                seasonIndex = i;
            }
        }
        seasonSpinner.setVisibility(seasons.size() > 1 ? View.VISIBLE : View.GONE);
        seasonSpinner.setOnItemSelectedListener(null);
        ArrayAdapter<String> seasonAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        seasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        seasonSpinner.setAdapter(seasonAdapter);
        seasonSpinner.setSelection(seasonIndex, false);
        seasonSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != seasonIndex) {
                    seasonIndex = position;
                    showSeason();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        showSeason();
    }

    private static boolean containsEpisode(WatchSeasonDto season, int number) {
        if (season.episodes == null) {
            return false;
        }
        for (WatchEpisodeDto episodeDto : season.episodes) {
            if (episodeDto.episodeNumber == number) {
                return true;
            }
        }
        return false;
    }

    private void showSeason() {
        episodeAdapter.submit(seasons.get(seasonIndex).episodes, episode);
    }

    private void playEpisode(int number) {
        syncPositionFromPlayer();
        if (positionSeconds > 0) {
            saveProgress(); // the episode you were watching
        }
        stopPlayback();
        episode = number;
        positionSeconds = 0;
        episodeAdapter.setCurrent(number);
        updateTitle();
        resolveStream();
    }

    // ------------------------------------------------------------------ comments

    private void postComment() {
        String text = etComment.getText() == null ? "" : etComment.getText().toString().trim();
        if (!loaded) {
            return;
        }
        if (text.isEmpty()) {
            Toast.makeText(this, "Write a comment first", Toast.LENGTH_SHORT).show();
            return;
        }
        btnPost.setEnabled(false);
        catalog.postComment(animeId, text, new ApiCallback<CommentPostedDto>() {
            @Override
            public void onSuccess(CommentPostedDto posted) {
                if (gone()) {
                    return;
                }
                btnPost.setEnabled(true);
                CommentDto comment = new CommentDto();
                comment.userName = posted != null && posted.authorDisplayName != null
                        ? posted.authorDisplayName : SessionStore.displayName(WatchActivity.this);
                comment.body = posted != null && posted.body != null ? posted.body : text;
                comment.postedAtUtc = posted != null ? posted.createdAtUtc : null;
                commentAdapter.addFirst(comment);
                etComment.setText("");
                updateCommentsHeader();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(etComment.getWindowToken(), 0);
                }
                Toast.makeText(WatchActivity.this, "Comment posted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(ApiError error) {
                if (gone()) {
                    return;
                }
                btnPost.setEnabled(true);
                if (!Ui.handleSessionExpired(WatchActivity.this, error)) {
                    Toast.makeText(WatchActivity.this, error.message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void updateCommentsHeader() {
        tvCommentsHeader.setText("Comments (" + commentAdapter.getItemCount() + ")");
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void onStart() {
        super.onStart();
        // Back from the background: rebuild the player where the viewer left off, paused.
        if (player == null && currentItem != null) {
            startItem(currentItem, resumePositionMs, false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
        if (loaded) {
            syncPositionFromPlayer();
            if (positionSeconds > 0) {
                saveProgress(true); // save, then flush, in that order
            } else {
                history.flush();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        releasePlayer();
        super.onDestroy();
    }
}
