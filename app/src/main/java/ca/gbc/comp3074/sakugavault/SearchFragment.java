package ca.gbc.comp3074.sakugavault;

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

import ca.gbc.comp3074.sakugavault.data.Anime;
import ca.gbc.comp3074.sakugavault.data.SampleData;
import ca.gbc.comp3074.sakugavault.ui.PosterAdapter;

/**
 * Search screen (FR-06, FR-07): debounced keyword search plus genre chips.
 * Next milestone: GET /api/catalog/search?q=...&limit=...
 */
public class SearchFragment extends Fragment {

    private static final long DEBOUNCE_MS = 300;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable runSearch = this::applyFilters;

    private PosterAdapter adapter;
    private TextView header;
    private TextView empty;
    private String query = "";
    private String genre = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
        for (String name : SampleData.SEARCH_GENRES) {
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
            applyFilters();
        });

        // Debounced search: wait until the user stops typing for 300 ms.
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

        applyFilters();
    }

    private void applyFilters() {
        if (!isAdded() || adapter == null) {
            return;
        }
        List<Anime> found = SampleData.search(query, genre);
        adapter.submit(found);
        int n = found.size();
        if (query.isEmpty()) {
            header.setText("All".equals(genre)
                    ? String.format(Locale.US, "Trending now · %d titles", n)
                    : String.format(Locale.US, "%s · %d titles", genre, n));
        } else {
            header.setText(String.format(Locale.US, "%d %s for “%s”", n, n == 1 ? "result" : "results", query));
        }
        if (n == 0) {
            String where = "All".equals(genre) ? "" : " in " + genre;
            empty.setText(String.format(Locale.US, "No titles match “%s”%s.\nTry another title or genre.", query, where));
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
