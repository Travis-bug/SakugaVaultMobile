package ca.gbc.comp3074.sakugavault;

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
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

import ca.gbc.comp3074.sakugavault.data.Anime;
import ca.gbc.comp3074.sakugavault.data.SampleData;
import ca.gbc.comp3074.sakugavault.data.SessionStore;
import ca.gbc.comp3074.sakugavault.ui.CommentAdapter;
import ca.gbc.comp3074.sakugavault.ui.EpisodeAdapter;
import ca.gbc.comp3074.sakugavault.ui.PosterAdapter;
import ca.gbc.comp3074.sakugavault.ui.Ui;

/**
 * Title Detail and Player screen (FR-03, FR-04, FR-05, FR-09).
 * Prototype: the player is simulated (no video yet) so the controls, episode list,
 * sub/dub switch, full-screen mode and progress saving can be shown.
 * Next milestone: AndroidX Media3 ExoPlayer playing the stream from
 * POST /api/watch/{id}/resolve-playback, and progress sent to /api/watch/history.
 */
public class WatchActivity extends AppCompatActivity {

    private static final String EXTRA_ID = "anime_id";
    private static final String EXTRA_EPISODE = "episode";
    private static final String EXTRA_PERCENT = "percent";
    private static final int SAVE_EVERY_SECONDS = 15;

    public static void start(Context context, String animeId, int episode, int percent) {
        Intent intent = new Intent(context, WatchActivity.class);
        intent.putExtra(EXTRA_ID, animeId);
        intent.putExtra(EXTRA_EPISODE, episode);
        intent.putExtra(EXTRA_PERCENT, percent);
        context.startActivity(intent);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());

    private Anime anime;
    private int episode;
    private int seasonIndex;
    private int positionSeconds;
    private int durationSeconds;
    private boolean playing;
    private boolean fullscreen;

    private View topBar;
    private View detailsScroll;
    private View playerContainer;
    private ImageButton btnPlayPause;
    private ImageButton btnFullscreen;
    private SeekBar seekBar;
    private TextView tvTime;
    private TextView tvResolving;
    private TextView tvTopTitle;
    private TextView tvSyncStatus;
    private TextView tvLangInfo;
    private TextView tvCommentsHeader;
    private TextInputEditText etComment;
    private EpisodeAdapter episodeAdapter;
    private CommentAdapter commentAdapter;

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

        anime = SampleData.find(getIntent().getStringExtra(EXTRA_ID));
        if (anime == null) {
            anime = SampleData.ALL.get(0);
        }
        episode = Math.max(1, getIntent().getIntExtra(EXTRA_EPISODE, 1));
        int percent = getIntent().getIntExtra(EXTRA_PERCENT, 0);
        durationSeconds = anime.runtimeMinutes * 60;
        positionSeconds = percent >= 100 ? 0 : durationSeconds * percent / 100;
        seasonIndex = (episode - 1) / SampleData.EPISODES_PER_SEASON;

        topBar = findViewById(R.id.topBar);
        detailsScroll = findViewById(R.id.detailsScroll);
        playerContainer = findViewById(R.id.playerContainer);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnFullscreen = findViewById(R.id.btnFullscreen);
        seekBar = findViewById(R.id.seekBar);
        tvTime = findViewById(R.id.tvTime);
        tvResolving = findViewById(R.id.tvResolving);
        tvTopTitle = findViewById(R.id.tvTopTitle);
        tvSyncStatus = findViewById(R.id.tvSyncStatus);
        tvLangInfo = findViewById(R.id.tvLangInfo);
        tvCommentsHeader = findViewById(R.id.tvCommentsHeader);
        etComment = findViewById(R.id.etComment);

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
        setUpDetails();
        setUpEpisodes();
        setUpComments();
        setUpSimilar();

        applyLayout();
        updateTitle();
        updateTime();
        tvSyncStatus.setText(positionSeconds > 0
                ? "Resuming at " + format(positionSeconds) + " (saved from your last session)"
                : "Progress saves every 15 seconds and syncs with the website");
        handler.postDelayed(tick, 1000);
        resolveStream();
    }

    // ------------------------------------------------------------------ player

    private void setUpPlayer() {
        GradientDrawable art = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{anime.colorStart, 0xFF000000});
        playerContainer.setBackground(art);
        ((TextView) findViewById(R.id.playerArt)).setText(anime.title.toUpperCase(Locale.ROOT));

        btnPlayPause.setOnClickListener(v -> setPlaying(!playing));
        btnFullscreen.setOnClickListener(v -> setFullscreen(!fullscreen));

        seekBar.setMax(durationSeconds);
        seekBar.setProgress(positionSeconds);
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

    /** Simulates POST /api/watch/{id}/resolve-playback before playback starts. */
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
        playing = play && positionSeconds < durationSeconds;
        btnPlayPause.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        // Fade the big button while playing so the "video" is visible.
        btnPlayPause.setAlpha(playing ? 0.35f : 1f);
    }

    private void updateTime() {
        seekBar.setProgress(positionSeconds);
        tvTime.setText(format(positionSeconds) + " / " + format(durationSeconds));
    }

    private static String format(int seconds) {
        return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60);
    }

    /** Local save every 15 s; the real app also POSTs to /api/watch/history. */
    private void saveProgress() {
        int percent = durationSeconds == 0 ? 0 : positionSeconds * 100 / durationSeconds;
        if (percent >= 90) {
            percent = 100; // counts as completed once 90% is watched
        }
        SampleData.recordProgress(anime, episode, percent);
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

    private void setUpDetails() {
        ((TextView) findViewById(R.id.tvTitle)).setText(anime.title);
        ((TextView) findViewById(R.id.tvMeta)).setText(anime.badgeLine() + " · " + anime.runtimeMinutes + " min · " + anime.genreLine());
        TextView synopsis = findViewById(R.id.tvSynopsis);
        TextView more = findViewById(R.id.tvMore);
        synopsis.setText(anime.synopsis);
        more.setOnClickListener(v -> {
            boolean expanded = synopsis.getMaxLines() > 2;
            synopsis.setMaxLines(expanded ? 2 : 20);
            more.setText(expanded ? "More" : "Less");
        });

        MaterialButtonToggleGroup langToggle = findViewById(R.id.langToggle);
        MaterialButton btnSub = findViewById(R.id.btnSub);
        MaterialButton btnDub = findViewById(R.id.btnDub);
        btnSub.setEnabled(anime.subAvailable);
        btnDub.setEnabled(anime.dubAvailable);
        if (!anime.subAvailable && anime.dubAvailable) {
            langToggle.check(R.id.btnDub);
        }
        showLanguage(langToggle.getCheckedButtonId() == R.id.btnDub);
        langToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                showLanguage(checkedId == R.id.btnDub);
                resolveStream();
            }
        });
    }

    private void showLanguage(boolean dub) {
        String text = dub ? "Audio: English dub" : "Audio: Japanese · Subtitles: English";
        if (!anime.dubAvailable) {
            text += " · No dub yet";
        }
        tvLangInfo.setText(text);
    }

    private void updateTitle() {
        tvTopTitle.setText(anime.title + " · Episode " + episode);
    }

    // ------------------------------------------------------------------ episodes

    private void setUpEpisodes() {
        int seasons = SampleData.seasonCount(anime);
        String[] labels = new String[seasons];
        for (int i = 0; i < seasons; i++) {
            labels[i] = "Season " + (i + 1);
        }
        Spinner spinner = findViewById(R.id.seasonSpinner);
        ArrayAdapter<String> seasonAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        seasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(seasonAdapter);
        spinner.setSelection(Math.min(seasonIndex, seasons - 1), false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != seasonIndex) {
                    seasonIndex = position;
                    loadSeason();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        RecyclerView list = findViewById(R.id.episodeList);
        list.setLayoutManager(new LinearLayoutManager(this));
        episodeAdapter = new EpisodeAdapter(this::playEpisode);
        list.setAdapter(episodeAdapter);
        loadSeason();
    }

    private void loadSeason() {
        int first = seasonIndex * SampleData.EPISODES_PER_SEASON + 1;
        int count = Math.min(SampleData.EPISODES_PER_SEASON, anime.episodeCount - first + 1);
        episodeAdapter.setRange(first, Math.max(0, count), episode);
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

    // ------------------------------------------------------------------ comments & similar

    private void setUpComments() {
        RecyclerView list = findViewById(R.id.commentList);
        list.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(SampleData.commentsFor(anime));
        list.setAdapter(commentAdapter);
        updateCommentsHeader();

        findViewById(R.id.btnPost).setOnClickListener(v -> {
            String text = etComment.getText() == null ? "" : etComment.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Write a comment first", Toast.LENGTH_SHORT).show();
                return;
            }
            commentAdapter.addFirst(new SampleData.Comment(SessionStore.userName(this), "now", text));
            etComment.setText("");
            updateCommentsHeader();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(etComment.getWindowToken(), 0);
            }
            Toast.makeText(this, "Comment posted", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateCommentsHeader() {
        tvCommentsHeader.setText("Comments (" + commentAdapter.getItemCount() + ")");
    }

    private void setUpSimilar() {
        RecyclerView list = findViewById(R.id.similarList);
        list.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        list.setAdapter(new PosterAdapter(SampleData.similarTo(anime), false,
                other -> start(this, other.id, 1, 0)));
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void onPause() {
        super.onPause();
        if (playing) {
            setPlaying(false);
        }
        if (positionSeconds > 0) {
            saveProgress();
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
