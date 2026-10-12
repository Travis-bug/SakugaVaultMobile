package ca.gbc.comp3074.sakugavault;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ca.gbc.comp3074.sakugavault.data.Anime;
import ca.gbc.comp3074.sakugavault.data.SampleData;
import ca.gbc.comp3074.sakugavault.data.SessionStore;
import ca.gbc.comp3074.sakugavault.ui.ContinueAdapter;

/**
 * Profile screen (FR-08): account, viewing stats, recent history, watchlist and sign-out.
 * Next milestone: GET /api/profile/me and POST /api/auth/logout.
 */
public class ProfileFragment extends Fragment {

    private TextView tvWatchlist;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String name = SessionStore.displayName(requireContext());
        String userName = SessionStore.userName(requireContext());
        String email = SessionStore.email(requireContext());

        ((TextView) view.findViewById(R.id.tvAvatar)).setText(initials(name));
        ((TextView) view.findViewById(R.id.tvDisplayName)).setText(name);
        ((TextView) view.findViewById(R.id.tvHandle)).setText("@" + userName + " · " + email);

        ((TextView) view.findViewById(R.id.statInProgress)).setText(String.valueOf(SampleData.continueWatching().size()));
        ((TextView) view.findViewById(R.id.statCompleted)).setText(String.valueOf(SampleData.completedCount()));
        ((TextView) view.findViewById(R.id.statComments)).setText("5");

        RecyclerView history = view.findViewById(R.id.historyList);
        history.setLayoutManager(new LinearLayoutManager(requireContext()));
        history.setAdapter(new ContinueAdapter(SampleData.recentHistory(), true,
                entry -> WatchActivity.start(requireContext(), entry.anime.id, entry.episode, entry.percent)));

        tvWatchlist = view.findViewById(R.id.tvWatchlist);
        view.findViewById(R.id.rowWatchlist).setOnClickListener(v -> showWatchlist());
        view.findViewById(R.id.btnSignOut).setOnClickListener(v -> confirmSignOut());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (tvWatchlist != null) {
            tvWatchlist.setText(String.format(Locale.US, "My Watchlist (%d)", SampleData.WATCHLIST.size()));
        }
    }

    private void showWatchlist() {
        final List<Anime> saved = new ArrayList<>();
        for (String id : SampleData.WATCHLIST) {
            Anime anime = SampleData.find(id);
            if (anime != null) {
                saved.add(anime);
            }
        }
        if (saved.isEmpty()) {
            Toast.makeText(requireContext(), "Your watchlist is empty. Tap Watchlist on a featured title to add it.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        String[] titles = new String[saved.size()];
        for (int i = 0; i < saved.size(); i++) {
            titles[i] = saved.get(i).title;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("My Watchlist")
                .setItems(titles, (dialog, which) ->
                        WatchActivity.start(requireContext(), saved.get(which).id, 1, 0))
                .setNegativeButton("Close", null)
                .show();
    }

    private void confirmSignOut() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sign out?")
                .setMessage("This ends your session on this device. Your watch history stays saved to your account.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    SessionStore.clear(requireContext());
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .show();
    }

    private static String initials(String name) {
        StringBuilder sb = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
            }
            if (sb.length() == 2) {
                break;
            }
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }
}
