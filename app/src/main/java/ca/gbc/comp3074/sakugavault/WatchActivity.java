package ca.gbc.comp3074.sakugavault;

import ca.gbc.comp3074.sakugavault.services.DeveloperLogger;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ca.gbc.comp3074.sakugavault.api.ApiCallback;
import ca.gbc.comp3074.sakugavault.api.ApiError;
import ca.gbc.comp3074.sakugavault.api.dto.AnimeCardDto;
import ca.gbc.comp3074.sakugavault.api.dto.CommentDto;
import ca.gbc.comp3074.sakugavault.api.dto.CommentPostedDto;
import ca.gbc.comp3074.sakugavault.api.dto.EpisodeListResponseDto;
import ca.gbc.comp3074.sakugavault.api.dto.WatchEpisodeDto;
import ca.gbc.comp3074.sakugavault.api.dto.WatchPageDto;
import ca.gbc.comp3074.sakugavault.api.dto.WatchSeasonDto;
import ca.gbc.comp3074.sakugavault.data.Anime;
import ca.gbc.comp3074.sakugavault.data.SessionStore;
import ca.gbc.comp3074.sakugavault.data.local.WatchListEntry;
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
 * Still simulated: the video itself (next step: Media3 ExoPlayer + resolve-playback).
 */
public class WatchActivity extends AppCompatActivity {

    private static final String EXTRA_ID = "anime_id";
    private static final String EXTRA_EPISODE = "episode";
    private static final String EXTRA_POSITION = "position_seconds";
    private static final int SAVE_EVERY_SECONDS = 15;
    private static final int DEFAULT_RUNTIME_MINUTES = 24;

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
    private boolean playing;
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
    private ImageButton btnPlayPause;
    private ImageButton btnFullscreen;
    private SeekBar seekBar;
    private TextView tvTime;
    private TextView tvResolving;
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

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (playing) {
                positionSeconds = Math.min(durationSeconds, positionSeconds + 1);
                updateTime();
                if (positionSeconds % SAVE_EVERY_SECONDS == 0) {
                    saveProgress();
                }
                if (positionSeconds >= durationSeconds) {
                    setPlaying(false);
                    saveProgress();
                }
            }
            handler.postDelayed(this, 1000);
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

        setUpPlayerControls();
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
        updateTime();
        loadPage();
    }

    private void findViews() {
        topBar = findViewById(R.id.topBar);
        detailsScroll = findViewById(R.id.detailsScroll);
        playerContainer = findViewById(R.id.playerContainer);
        playerImage = findViewById(R.id.playerImage);
        playerArt = findViewById(R.id.playerArt);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnFullscreen = findViewById(R.id.btnFullscreen);
        seekBar = findViewById(R.id.seekBar);
        tvTime = findViewById(R.id.tvTime);
        tvResolving = findViewById(R.id.tvResolving);
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
        seekBar.setMax(durationSeconds);
        seekBar.setProgress(positionSeconds);

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
        updateTime();
        tvSyncStatus.setText(positionSeconds > 0
                ? "Resuming at " + format(positionSeconds) + " (saved to your account)"
                : "Progress saves every 15 seconds and syncs with the website");

        handler.removeCallbacks(tick);
        handler.postDelayed(tick, 1000);
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

    // ------------------------------------------------------------------ player (simulated for now)

    private void setUpPlayerControls() {
        btnPlayPause.setOnClickListener(v -> setPlaying(!playing));
        btnFullscreen.setOnClickListener(v -> setFullscreen(!fullscreen));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) {
                    positionSeconds = progress;
                    updateTime();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                saveProgress();
            }
        });
    }

    /** Stands in for POST /api/watch/{id}/resolve-playback until the real player is added. */
    private void resolveStream() {
        setPlaying(false);
        btnPlayPause.setVisibility(View.INVISIBLE);
        tvResolving.setVisibility(View.VISIBLE);
        handler.postDelayed(() -> {
            tvResolving.setVisibility(View.GONE);
            btnPlayPause.setVisibility(View.VISIBLE);
            setPlaying(true);
        }, 900);
    }

    private void setPlaying(boolean play) {
        playing = loaded && play && positionSeconds < durationSeconds;
        btnPlayPause.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        btnPlayPause.setAlpha(playing ? 0.35f : 1f);
    }

    private void updateTime() {
        seekBar.setProgress(positionSeconds);
        tvTime.setText(format(positionSeconds) + " / " + format(durationSeconds));
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
        btnFullscreen.setImageResource(fullscreen ? R.drawable.ic_fullscreen_exit : R.drawable.ic_fullscreen);

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

        MaterialButtonToggleGroup langToggle = findViewById(R.id.langToggle);
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
        if (positionSeconds > 0) {
            saveProgress();
        }
        episode = number;
        positionSeconds = 0;
        episodeAdapter.setCurrent(number);
        updateTitle();
        updateTime();
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
    protected void onPause() {
        super.onPause();
        if (playing) {
            setPlaying(false);
        }
        if (loaded) {
            if (positionSeconds > 0) {
                saveProgress(true); // save, then flush, in that order
            } else {
                history.flush();
            }
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
