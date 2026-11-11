package ca.gbc.comp3074.sakugavault;

import ca.gbc.comp3074.sakugavault.services.DeveloperLogger;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ca.gbc.comp3074.sakugavault.api.ApiCallback;
import ca.gbc.comp3074.sakugavault.api.ApiError;
import ca.gbc.comp3074.sakugavault.api.dto.CatalogSearchResponseDto;
import ca.gbc.comp3074.sakugavault.api.dto.SearchAnimeResultDto;
import ca.gbc.comp3074.sakugavault.data.Anime;
import ca.gbc.comp3074.sakugavault.services.AppServices;
import ca.gbc.comp3074.sakugavault.services.catalog.ICatalogService;
import ca.gbc.comp3074.sakugavault.ui.PosterAdapter;
import ca.gbc.comp3074.sakugavault.ui.Ui;

/**
 * Search screen (FR-06, FR-07): GET /api/catalog/search?q=... after the user stops
 * typing, plus genre chips that filter the results on the phone.
 */
public class SearchFragment extends Fragment {

    private static final long DEBOUNCE_MS = 400;
    private static final int LIMIT = 30;
    /** The back end's genres (Data/SakugaVaultSeeder). */
    private static final String[] GENRES = {"All", "Action", "Adventure", "Comedy", "Drama", "Fantasy",
            "Mystery", "Romance", "Sci-Fi", "Sports", "Supernatural"};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable runSearch = this::search;
    private final List<Anime> lastResults = new ArrayList<>();

    private ICatalogService catalog;
    private PosterAdapter adapter;
    private TextView header;
    private TextView empty;
    private String query = "";
    private String genre = "All";
    /** Each search gets a number so an older, slower answer can't overwrite a newer one. */
    private int searchNumber = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        catalog = AppServices.catalog(requireContext());
        header = view.findViewById(R.id.tvResultsHeader);
        empty = view.findViewById(R.id.tvEmpty);

        RecyclerView results = view.findViewById(R.id.resultsList);
        int columns = getResources().getInteger(R.integer.grid_columns);
        results.setLayoutManager(new GridLayoutManager(requireContext(), columns));
        adapter = new PosterAdapter(new ArrayList<>(), true,
                anime -> WatchActivity.start(requireContext(), anime.id, 1, 0));
        results.setAdapter(adapter);

        // Genre chips
        ChipGroup chipGroup = view.findViewById(R.id.chipGroup);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int allChipId = View.NO_ID;
        for (String name : GENRES) {
            Chip chip = (Chip) inflater.inflate(R.layout.item_chip, chipGroup, false);
            chip.setId(View.generateViewId());
            chip.setText(name);
            chip.setCheckable(true);
            chipGroup.addView(chip);
            if ("All".equals(name)) {
                allChipId = chip.getId();
            }
        }
        chipGroup.check(allChipId);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            Chip chip = group.findViewById(checkedIds.get(0));
            genre = chip.getText().toString();
            showResults();
        });

        // Wait until the user stops typing before calling the API.
        EditText searchField = view.findViewById(R.id.etSearch);
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                query = s.toString().trim();
                handler.removeCallbacks(runSearch);
                handler.postDelayed(runSearch, DEBOUNCE_MS);
            }
        });

        search();
    }

    private void search() {
        if (!isAdded() || adapter == null) {
            return;
        }
        final int number = ++searchNumber;
        header.setText(query.isEmpty() ? "Loading trending titles…" : "Searching for “" + query + "”…");
        empty.setVisibility(View.GONE);
        catalog.search(query, LIMIT, new ApiCallback<CatalogSearchResponseDto>() {
            @Override
            public void onSuccess(CatalogSearchResponseDto response) {
                if (!isAdded() || adapter == null || number != searchNumber) {
                    return;
                }
                lastResults.clear();
                if (response != null && response.results != null) {
                    for (SearchAnimeResultDto result : response.results) {
                        lastResults.add(Anime.fromSearch(result));
                    }
                }
                showResults();
            }

            @Override
            public void onError(ApiError error) {
                if (!isAdded() || adapter == null || number != searchNumber
                        || Ui.handleSessionExpired(getActivity(), error)) {
                    return;
                }
                lastResults.clear();
                adapter.submit(lastResults);
                header.setText("Search didn't work");
                empty.setText(error.message);
                empty.setVisibility(View.VISIBLE);
            }
        });
    }

    /** Shows the last results, narrowed to the selected genre chip. */
    private void showResults() {
        if (!isAdded() || adapter == null) {
            return;
        }
        List<Anime> shown = new ArrayList<>();
        for (Anime anime : lastResults) {
            if ("All".equals(genre) || anime.hasGenre(genre)) {
                shown.add(anime);
            }
        }
        adapter.submit(shown);
        int n = shown.size();
        if (query.isEmpty()) {
            header.setText("All".equals(genre)
                    ? String.format(Locale.US, "Trending now · %d titles", n)
                    : String.format(Locale.US, "Trending in %s · %d titles", genre, n));
        } else {
            header.setText(String.format(Locale.US, "%d %s for “%s”", n, n == 1 ? "result" : "results", query));
        }
        if (n == 0) {
            String where = "All".equals(genre) ? "" : " in " + genre;
            empty.setText(query.isEmpty()
                    ? "Nothing trending" + where + " right now."
                    : String.format(Locale.US, "No titles match “%s”%s.\nTry another title or genre.", query, where));
            empty.setVisibility(View.VISIBLE);
        } else {
            empty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(runSearch);
        adapter = null;
        super.onDestroyView();
    }
}
