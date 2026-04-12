package com.example.concerto.dashboard;

import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.concerto.R;
import com.example.concerto.adapters.TrackAdapter;
import com.example.concerto.auth.AuthViewModel;
import com.example.concerto.databinding.FragmentDashboardBinding;
import com.example.concerto.player.PlayerViewModel;
import com.example.concerto.auth.ConnectSpotifyFragment;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding bind;
    private DashboardViewModel dashboardViewModel;
    private AuthViewModel authViewModel;
    private PlayerViewModel playerViewModel;
    private TrackAdapter trackAdapter;

    public static DashboardFragment newInstance() {
        return new DashboardFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        bind = FragmentDashboardBinding.inflate(inflater, container, false);
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViewModels();
        setupUI();
        setupObservers();
        setupButtons();
        setupGenreSelectors();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }

    private void initViewModels() {
        // Activity-scoped intentionally: songs persist across Dashboard re-visits
        dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        playerViewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);
    }

    private void setupUI() {
        trackAdapter = new TrackAdapter((track, canPlay) -> {
            String token = authViewModel.getSpotifyToken().getValue();
            Boolean isPremium = authViewModel.getIsPremiumUser().getValue();

            if (token == null || token.trim().isEmpty()) {
                Toast.makeText(requireContext(), "Connect Spotify to play full tracks!", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutFragmentContainer, new ConnectSpotifyFragment())
                        .addToBackStack(null)
                        .commit();
            } else if (isPremium == null || !isPremium) {
                Toast.makeText(requireContext(), "Spotify Premium is required to play tracks.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(requireContext(), "Loading: " + track.name, Toast.LENGTH_SHORT).show();
                playerViewModel.setControlsEnabled(true);
                String imageUrl = (track.imageUri != null && track.imageUri.raw != null) ? track.imageUri.raw : "";
                String artistName = (track.artist != null && track.artist.name != null) ? track.artist.name : "Unknown Artist";
                playerViewModel.setDisplayInfo(track.name, artistName, imageUrl);
                playerViewModel.playTrack(track.uri);
                playerViewModel.expandPlayer();
            }
        });

        bind.rvDashboardTracks.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        bind.rvDashboardTracks.setAdapter(trackAdapter);

        String username = authViewModel.getCurrentUsername();

        bind.progressBar.setVisibility(View.VISIBLE);

        // FEATURE: Pull-to-refresh — Spotify green spinner, forces a fresh song reload
        bind.swipeRefreshLayout.setColorSchemeColors(android.graphics.Color.parseColor("#7C72E0"));
        bind.swipeRefreshLayout.setOnRefreshListener(() -> {
            // Re-fetch the currently active genre from ViewModel (survives re-navigation)
            dashboardViewModel.loadSongsByGenre(dashboardViewModel.getLastGenre());
        });
    }

    private void setupObservers() {
        dashboardViewModel.getSongs().observe(getViewLifecycleOwner(), songs -> {
            // Stop both the initial progress bar and the swipe refresh spinner
            bind.progressBar.setVisibility(View.GONE);
            bind.swipeRefreshLayout.setRefreshing(false);

            // Make the RecyclerView visible again now that loading is complete!
            bind.rvDashboardTracks.setVisibility(View.VISIBLE);

            if (songs != null && !songs.isEmpty()) {
                trackAdapter.setTracks(songs);
            } else {
                Toast.makeText(requireContext(), "No songs found.", Toast.LENGTH_SHORT).show();
            }
        });

        authViewModel.getSpotifyToken().observe(getViewLifecycleOwner(), token -> {
            // Guard: treat null and empty string as not connected
            boolean isConnected = (token != null && !token.trim().isEmpty());
            bind.btnConnectSpotify.setVisibility(isConnected ? View.GONE : View.VISIBLE);
        });

        authViewModel.getIsPremiumUser().observe(getViewLifecycleOwner(), isPremium -> {
            trackAdapter.setCanPlayMusic(isPremium != null && isPremium);
        });

        if (authViewModel.getSpotifyToken().getValue() == null) {
            authViewModel.restoreSpotifyTokenFromFirebase(null);
        }
    }

    private void setupButtons() {
        bind.btnConnectSpotify.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.layoutFragmentContainer, new ConnectSpotifyFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void setupGenreSelectors() {
        View.OnClickListener genreClickListener = view -> {
            // 1. Reset all buttons to unselected
            bind.btnClassical.setSelected(false);
            bind.btnHiphop.setSelected(false);
            bind.btnPop.setSelected(false);

            // 2. Set the clicked button to selected
            view.setSelected(true);

            // 3. Show the loading spinner
            bind.progressBar.setVisibility(View.VISIBLE);
            bind.rvDashboardTracks.setVisibility(View.GONE);

            // 4. Update the TextView and fetch songs based on the button clicked
            if (view.getId() == R.id.btnClassical) {
                bind.tvGenre.setText("Classical");
                dashboardViewModel.loadSongsByGenre("classical");
            } else if (view.getId() == R.id.btnHiphop) {
                bind.tvGenre.setText("Hip-hop");
                dashboardViewModel.loadSongsByGenre("hip-hop");
            } else if (view.getId() == R.id.btnPop) {
                bind.tvGenre.setText("Pop");
                dashboardViewModel.loadSongsByGenre("pop");
            }
        };

        // Attach the listener to all three buttons
        bind.btnClassical.setOnClickListener(genreClickListener);
        bind.btnHiphop.setOnClickListener(genreClickListener);
        bind.btnPop.setOnClickListener(genreClickListener);

        // Restore which genre button was last selected (survives re-navigation).
        // If songs are already loaded, just restore the UI state without re-fetching.
        // If songs are empty (first load), trigger a real click to fetch.
        restoreGenreSelection();
    }

    /**
     * Restores the selected genre button and label based on what the ViewModel last loaded.
     * On first launch (no songs yet) it triggers a real click to kick off the network call.
     * On re-navigation (songs already cached) it just restores the visual state silently.
     */
    private void restoreGenreSelection() {
        String lastGenre = dashboardViewModel.getLastGenre();
        boolean hasSongs = trackAdapter.getItemCount() > 0
                || (dashboardViewModel.getSongs().getValue() != null
                && !dashboardViewModel.getSongs().getValue().isEmpty());

        if (hasSongs) {
            // Songs already loaded — just restore the visual selected state + label
            applyGenreSelection(lastGenre, false);
        } else {
            // First load — trigger a real click so the network call fires
            applyGenreSelection(lastGenre, true);
        }
    }

    /**
     * Sets the correct button as selected and updates the genre label.
     * @param genre  The genre string ("classical", "hip-hop", "pop").
     * @param fetch  If true, also triggers the network fetch via performClick logic.
     */
    private void applyGenreSelection(String genre, boolean fetch) {
        if (bind == null) return;

        // Reset all first
        bind.btnClassical.setSelected(false);
        bind.btnHiphop.setSelected(false);
        bind.btnPop.setSelected(false);

        switch (genre) {
            case "hip-hop":
                bind.btnHiphop.setSelected(true);
                bind.tvGenre.setText("Hip-hop");
                if (fetch) dashboardViewModel.loadSongsByGenre("hip-hop");
                break;
            case "pop":
                bind.btnPop.setSelected(true);
                bind.tvGenre.setText("Pop");
                if (fetch) dashboardViewModel.loadSongsByGenre("pop");
                break;
            default: // "classical" is the default
                bind.btnClassical.setSelected(true);
                bind.tvGenre.setText("Classical");
                if (fetch) dashboardViewModel.loadSongsByGenre("classical");
                break;
        }
    }
}