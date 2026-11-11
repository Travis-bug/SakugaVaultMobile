package ca.gbc.comp3074.sakugavault;

import ca.gbc.comp3074.sakugavault.services.DeveloperLogger;

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

import java.util.Locale;

import ca.gbc.comp3074.sakugavault.api.ApiCallback;
import ca.gbc.comp3074.sakugavault.api.ApiError;
import ca.gbc.comp3074.sakugavault.api.dto.ProfileSummaryDto;
import ca.gbc.comp3074.sakugavault.data.SessionStore;
import ca.gbc.comp3074.sakugavault.services.AppServices;
import ca.gbc.comp3074.sakugavault.services.profile.IProfileService;
import ca.gbc.comp3074.sakugavault.services.watch.IWatchListService;
import ca.gbc.comp3074.sakugavault.ui.ContinueAdapter;
import ca.gbc.comp3074.sakugavault.ui.Ui;

/**
 * Profile screen (FR-08): account, counts and recent history from GET /api/profile/me,
 * the watchlist count from Room, and sign-out (POST /api/auth/logout).
 */
public class ProfileFragment extends Fragment {

    private IProfileService profile;
    private IWatchListService watchList;
    private ContinueAdapter historyAdapter;
    private TextView statInProgress;
    private TextView statCompleted;
    private TextView statComments;
    private boolean firstResume = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        profile = AppServices.profile(requireContext());
        watchList = AppServices.watchList(requireContext());

        showUser(view, SessionStore.displayName(requireContext()),
                SessionStore.userName(requireContext()), SessionStore.email(requireContext()));

        statInProgress = view.findViewById(R.id.statInProgress);
        statCompleted = view.findViewById(R.id.statCompleted);
        statComments = view.findViewById(R.id.statComments);
        statInProgress.setText("–");
        statCompleted.setText("–");
        statComments.setText("–");

        RecyclerView history = view.findViewById(R.id.historyList);
        history.setLayoutManager(new LinearLayoutManager(requireContext()));
        historyAdapter = new ContinueAdapter(true, entry -> WatchActivity.start(requireContext(),
                entry.animeId, entry.episodeNumber, entry.completed ? 0 : entry.positionSeconds));
        history.setAdapter(historyAdapter);

        // Watchlist count comes straight from the Room tables and updates by itself.
        TextView tvWatchlist = view.findViewById(R.id.tvWatchlist);
        tvWatchlist.setText("My Watchlist");
        watchList.getAll().observe(getViewLifecycleOwner(), entries ->
                tvWatchlist.setText(String.format(Locale.US, "My Watchlist (%d)", entries == null ? 0 : entries.size())));
        view.findViewById(R.id.rowWatchlist).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), WatchListActivity.class)));

        view.findViewById(R.id.btnSignOut).setOnClickListener(v -> confirmSignOut());
        loadProfile();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!firstResume) {
            loadProfile(); // picks up progress saved while watching
        }
        firstResume = false;
    }

    private void loadProfile() {
        profile.getMyProfile(new ApiCallback<ProfileSummaryDto>() {
            @Override
            public void onSuccess(ProfileSummaryDto summary) {
                if (!isAdded() || getView() == null || summary == null) {
                    return;
                }
                statInProgress.setText(String.valueOf(summary.continueWatchingCount));
                statCompleted.setText(String.valueOf(summary.completedEntriesCount));
                statComments.setText(String.valueOf(summary.commentsCount));
                historyAdapter.submit(summary.recentHistory);
                if (summary.user != null) {
                    showUser(getView(), summary.user.displayName, summary.user.userName, summary.user.email);
                }
            }

            @Override
            public void onError(ApiError error) {
                if (!isAdded() || getView() == null || Ui.handleSessionExpired(getActivity(), error)) {
                    return;
                }
                Toast.makeText(requireContext(), "Couldn't load your profile. " + error.message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showUser(View view, String name, String userName, String email) {
        ((TextView) view.findViewById(R.id.tvAvatar)).setText(initials(name));
        ((TextView) view.findViewById(R.id.tvDisplayName)).setText(name);
        ((TextView) view.findViewById(R.id.tvHandle)).setText("@" + userName + " · " + email);
    }

    private void confirmSignOut() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sign out?")
                .setMessage("This ends your session on this device. Your watch history stays saved to your account, "
                        + "and your watchlist stays on this phone for next time.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Sign Out", (dialog, which) ->
                        AppServices.auth(requireContext()).logout(() -> {
                            Intent intent = new Intent(requireContext(), LoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }))
                .show();
    }

    private static String initials(String name) {
        StringBuilder sb = new StringBuilder();
        for (String part : (name == null ? "" : name).trim().split("\\s+")) {
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
