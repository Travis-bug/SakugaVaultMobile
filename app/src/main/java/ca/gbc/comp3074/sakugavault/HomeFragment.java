package ca.gbc.comp3074.sakugavault;

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

import java.util.List;

import ca.gbc.comp3074.sakugavault.data.Anime;
import ca.gbc.comp3074.sakugavault.data.SampleData;
import ca.gbc.comp3074.sakugavault.ui.ContinueAdapter;
import ca.gbc.comp3074.sakugavault.ui.HeroAdapter;
import ca.gbc.comp3074.sakugavault.ui.PosterAdapter;
import ca.gbc.comp3074.sakugavault.ui.Ui;

/**
 * Home screen (FR-02, FR-05): hero carousel, Continue Watching and genre rows.
 * Next milestone: data from GET /api/catalog/home and GET /api/watch/history/me.
 */
public class HomeFragment extends Fragment {

    private static final long AUTO_ADVANCE_MS = 5000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private RecyclerView heroList;
    private LinearLayout heroDots;
    private int heroCount;
    private int heroPosition = 0;

    private final Runnable autoAdvance = new Runnable() {
        @Override
        public void run() {
            if (heroList != null && heroCount > 0) {
                heroPosition = (heroPosition + 1) % heroCount;
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

        view.findViewById(R.id.btnSearch).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openSearchTab();
            }
        });

        // Hero carousel: one featured title per page, snaps like a pager.
        List<Anime> featured = SampleData.hero();
        heroCount = featured.size();
        heroList = view.findViewById(R.id.heroList);
        heroList.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        heroList.setAdapter(new HeroAdapter(featured, anime -> openTitle(anime, 1, 0)));
        new PagerSnapHelper().attachToRecyclerView(heroList);
        heroDots = view.findViewById(R.id.heroDots);
        buildDots();
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

        // Continue Watching (titles in progress).
        RecyclerView continueList = view.findViewById(R.id.continueList);
        continueList.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        continueList.setAdapter(new ContinueAdapter(SampleData.continueWatching(), false,
                entry -> openTitle(entry.anime, entry.episode, entry.percent)));

        // Genre rows share one view pool so fast scrolling stays smooth.
        LinearLayout railsContainer = view.findViewById(R.id.railsContainer);
        RecyclerView.RecycledViewPool sharedPool = new RecyclerView.RecycledViewPool();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (String genre : SampleData.RAIL_GENRES) {
            View rail = inflater.inflate(R.layout.item_rail, railsContainer, false);
            ((TextView) rail.findViewById(R.id.railTitle)).setText(genre);
            RecyclerView list = rail.findViewById(R.id.railList);
            list.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
            list.setRecycledViewPool(sharedPool);
            list.setHasFixedSize(true);
            list.setAdapter(new PosterAdapter(SampleData.byGenre(genre), false, anime -> openTitle(anime, 1, 0)));
            railsContainer.addView(rail);
        }

        // Pull to refresh (the real app re-fetches /api/catalog/home).
        SwipeRefreshLayout swipe = view.findViewById(R.id.swipe);
        swipe.setColorSchemeResources(R.color.sv_accent);
        swipe.setProgressBackgroundColorSchemeResource(R.color.sv_surface);
        swipe.setOnRefreshListener(() -> handler.postDelayed(() -> {
            swipe.setRefreshing(false);
            if (isAdded()) {
                Toast.makeText(requireContext(), "Catalog refreshed", Toast.LENGTH_SHORT).show();
            }
        }, 1000));
    }

    private void buildDots() {
        heroDots.removeAllViews();
        for (int i = 0; i < heroCount; i++) {
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

    private void openTitle(Anime anime, int episode, int percent) {
        WatchActivity.start(requireContext(), anime.id, episode, percent);
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.postDelayed(autoAdvance, AUTO_ADVANCE_MS);
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
