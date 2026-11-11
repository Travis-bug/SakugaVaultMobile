package ca.gbc.comp3074.sakugavault;

import ca.gbc.comp3074.sakugavault.services.DeveloperLogger;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import ca.gbc.comp3074.sakugavault.api.ApiCallback;
import ca.gbc.comp3074.sakugavault.api.ApiError;
import ca.gbc.comp3074.sakugavault.api.dto.AnimeCardDto;
import ca.gbc.comp3074.sakugavault.api.dto.CatalogHeroDto;
import ca.gbc.comp3074.sakugavault.api.dto.GenreRailDto;
import ca.gbc.comp3074.sakugavault.api.dto.HomeCatalogDto;
import ca.gbc.comp3074.sakugavault.api.dto.WatchHistoryEntryDto;
import ca.gbc.comp3074.sakugavault.data.Anime;
import ca.gbc.comp3074.sakugavault.data.local.WatchListEntry;
import ca.gbc.comp3074.sakugavault.services.AppServices;
import ca.gbc.comp3074.sakugavault.services.catalog.ICatalogService;
import ca.gbc.comp3074.sakugavault.services.watch.IWatchHistoryService;
import ca.gbc.comp3074.sakugavault.services.watch.IWatchListService;
import ca.gbc.comp3074.sakugavault.ui.ContinueAdapter;
import ca.gbc.comp3074.sakugavault.ui.HeroAdapter;
import ca.gbc.comp3074.sakugavault.ui.PosterAdapter;
import ca.gbc.comp3074.sakugavault.ui.Ui;

/**
 * Home screen (FR-02, FR-05).
 * Hero carousel and genre rows from GET /api/catalog/home, Continue Watching from
 * GET /api/watch/history/me, and the hero Watchlist buttons from the Room watchlist.
 */
public class HomeFragment extends Fragment {

    private static final long AUTO_ADVANCE_MS = 5000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Anime> heroItems = new ArrayList<>();

    private ICatalogService catalog;
    private IWatchHistoryService history;
    private IWatchListService watchList;

    private SwipeRefreshLayout swipe;
    private TextView status;
    private RecyclerView heroList;
    private LinearLayout heroDots;
    private HeroAdapter heroAdapter;
    private View continueHeader;
    private RecyclerView continueList;
    private ContinueAdapter continueAdapter;
    private LinearLayout railsContainer;
    private int heroPosition = 0;
    private boolean firstResume = true;

    private final Runnable autoAdvance = new Runnable() {
        @Override
        public void run() {
            if (heroList != null && !heroItems.isEmpty()) {
                heroPosition = (heroPosition + 1) % heroItems.size();
                heroList.smoothScrollToPosition(heroPosition);
                updateDots();
            }
            handler.postDelayed(this, AUTO_ADVANCE_MS);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        catalog = AppServices.catalog(requireContext());
        history = AppServices.watchHistory(requireContext());
        watchList = AppServices.watchList(requireContext());

        status = view.findViewById(R.id.tvHomeStatus);
        continueHeader = view.findViewById(R.id.tvContinueHeader);
        railsContainer = view.findViewById(R.id.railsContainer);

        view.findViewById(R.id.btnSearch).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openSearchTab();
            }
        });

        // Hero carousel: one featured title per page, snaps like a pager.
        heroList = view.findViewById(R.id.heroList);
        heroList.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        heroAdapter = new HeroAdapter(heroItems, anime -> openTitle(anime.id, 1, 0), this::toggleWatchlist);
        heroList.setAdapter(heroAdapter);
        new PagerSnapHelper().attachToRecyclerView(heroList);
        heroDots = view.findViewById(R.id.heroDots);
        heroList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (manager != null) {
                        int position = manager.findFirstCompletelyVisibleItemPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            heroPosition = position;
                            updateDots();
                        }
                    }
                }
            }
        });

        // Continue Watching: shows still in progress, from the tracked watch history.
        continueList = view.findViewById(R.id.continueList);
        continueList.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        continueAdapter = new ContinueAdapter(false,
                entry -> openTitle(entry.animeId, entry.episodeNumber, entry.positionSeconds));
        continueList.setAdapter(continueAdapter);

        // Watchlist buttons follow the Room tables: they update by themselves when a show is added or removed.
        watchList.getSavedIds().observe(getViewLifecycleOwner(), ids -> heroAdapter.setSavedIds(ids));

        swipe = view.findViewById(R.id.swipe);
        swipe.setColorSchemeResources(R.color.sv_accent);
        swipe.setProgressBackgroundColorSchemeResource(R.color.sv_surface);
        swipe.setOnRefreshListener(this::loadAll);

        swipe.setRefreshing(true);
        loadAll();
    }

    private boolean alive() {
        return isAdded() && getView() != null;
    }

    private void loadAll() {
        loadCatalog();
        loadContinueWatching();
    }

    private void loadCatalog() {
        catalog.getHome(new ApiCallback<HomeCatalogDto>() {
            @Override
            public void onSuccess(HomeCatalogDto home) {
                if (!alive()) {
                    return;
                }
                swipe.setRefreshing(false);
                showCatalog(home);
            }

            @Override
            public void onError(ApiError error) {
                if (!alive()) {
                    return;
                }
                swipe.setRefreshing(false);
                if (Ui.handleSessionExpired(getActivity(), error)) {
                    return;
                }
                showStatus(error.message + "\nPull down to try again.");
            }
        });
    }

    private void showCatalog(HomeCatalogDto home) {
        heroItems.clear();
        if (home != null && home.heroItems != null) {
            for (CatalogHeroDto dto : home.heroItems) {
                heroItems.add(Anime.fromHero(dto));
            }
        }
        heroAdapter.notifyDataSetChanged();
        heroPosition = 0;
        heroList.scrollToPosition(0);
        heroList.setVisibility(heroItems.isEmpty() ? View.GONE : View.VISIBLE);
        buildDots();

        // Genre rows share one view pool so fast scrolling stays smooth.
        railsContainer.removeAllViews();
        RecyclerView.RecycledViewPool sharedPool = new RecyclerView.RecycledViewPool();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int rails = 0;
        if (home != null && home.genreRows != null) {
            for (GenreRailDto row : home.genreRows) {
                if (row.titles == null || row.titles.isEmpty()) {
                    continue;
                }
                List<Anime> titles = new ArrayList<>();
                for (AnimeCardDto card : row.titles) {
                    titles.add(Anime.fromCard(card));
                }
                View rail = inflater.inflate(R.layout.item_rail, railsContainer, false);
                ((TextView) rail.findViewById(R.id.railTitle)).setText(row.genre);
                RecyclerView list = rail.findViewById(R.id.railList);
                list.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
                list.setRecycledViewPool(sharedPool);
                list.setHasFixedSize(true);
                list.setAdapter(new PosterAdapter(titles, false, anime -> openTitle(anime.id, 1, 0)));
                railsContainer.addView(rail);
                rails++;
            }
        }

        if (heroItems.isEmpty() && rails == 0) {
            showStatus("The catalog is empty right now. The anime source sites may be down.\nPull down to try again.");
        } else {
            status.setVisibility(View.GONE);
        }
    }

    private void loadContinueWatching() {
        history.getMyHistory(20, new ApiCallback<List<WatchHistoryEntryDto>>() {
            @Override
            public void onSuccess(List<WatchHistoryEntryDto> entries) {
                if (!alive()) {
                    return;
                }
                List<WatchHistoryEntryDto> inProgress = new ArrayList<>();
                for (WatchHistoryEntryDto entry : entries) {
                    if (!entry.completed) {
                        inProgress.add(entry);
                    }
                }
                continueAdapter.submit(inProgress);
                int visibility = inProgress.isEmpty() ? View.GONE : View.VISIBLE;
                continueHeader.setVisibility(visibility);
                continueList.setVisibility(visibility);
            }

            @Override
            public void onError(ApiError error) {
                if (alive()) {
                    Ui.handleSessionExpired(getActivity(), error);
                }
            }
        });
    }

    /** Hero "Watchlist" button: add grabs the title + metadata from the back end and saves it in Room. */
    private void toggleWatchlist(Anime anime, boolean saved, MaterialButton button) {
        button.setEnabled(false);
        if (saved) {
            watchList.remove(anime.id, () -> {
                if (alive()) {
                    Toast.makeText(requireContext(), "Removed from your watchlist", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }
        button.setText("Saving…");
        watchList.add(anime.id, new ApiCallback<WatchListEntry>() {
            @Override
            public void onSuccess(WatchListEntry entry) {
                if (alive()) {
                    Toast.makeText(requireContext(), "Saved “" + anime.title + "” to your watchlist",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(ApiError error) {
                if (!alive() || Ui.handleSessionExpired(getActivity(), error)) {
                    return;
                }
                heroAdapter.notifyDataSetChanged(); // puts the button back to "Watchlist"
                Toast.makeText(requireContext(), error.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showStatus(String message) {
        status.setText(message);
        status.setVisibility(View.VISIBLE);
    }

    private void buildDots() {
        heroDots.removeAllViews();
        for (int i = 0; i < heroItems.size(); i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(Ui.dp(requireContext(), 6), Ui.dp(requireContext(), 6));
            params.setMargins(Ui.dp(requireContext(), 3), 0, Ui.dp(requireContext(), 3), 0);
            dot.setLayoutParams(params);
            heroDots.addView(dot);
        }
        updateDots();
    }

    private void updateDots() {
        if (heroDots == null || !isAdded()) {
            return;
        }
        for (int i = 0; i < heroDots.getChildCount(); i++) {
            View dot = heroDots.getChildAt(i);
            boolean active = i == heroPosition;
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) dot.getLayoutParams();
            params.width = Ui.dp(requireContext(), active ? 18 : 6);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(active ? R.drawable.bg_dot_active : R.drawable.bg_dot);
        }
    }

    private void openTitle(String animeId, int episode, int positionSeconds) {
        WatchActivity.start(requireContext(), animeId, episode, positionSeconds);
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.postDelayed(autoAdvance, AUTO_ADVANCE_MS);
        // Coming back from the player: refresh Continue Watching so the new progress shows.
        if (!firstResume) {
            loadContinueWatching();
        }
        firstResume = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(autoAdvance);
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        heroList = null;
        heroDots = null;
        super.onDestroyView();
    }
}
