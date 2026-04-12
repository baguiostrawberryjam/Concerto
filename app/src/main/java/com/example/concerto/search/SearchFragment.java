package com.example.concerto.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.concerto.R;
import com.example.concerto.adapters.TrackAdapter;
import com.example.concerto.auth.AuthViewModel;
import com.example.concerto.concerto.ConcertoViewModel;
import com.example.concerto.databinding.FragmentSearchBinding;
import com.example.concerto.player.PlayerViewModel;
import com.example.concerto.auth.ConnectSpotifyFragment;

public class SearchFragment extends Fragment {

    private FragmentSearchBinding bind;
    private SearchViewModel searchViewModel;
    private PlayerViewModel playerViewModel;
    private ConcertoViewModel concertoViewModel;
    private AuthViewModel authViewModel; // ADDED
    private TrackAdapter trackAdapter;

    public static SearchFragment newInstance() {
        return new SearchFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        bind = FragmentSearchBinding.inflate(inflater, container, false);
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViewModels();
        setupUI();
        setupObservers();
        setupButtons();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }

    private void initViewModels() {
        searchViewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        playerViewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);
        concertoViewModel = new ViewModelProvider(requireActivity()).get(ConcertoViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class); // ADDED
    }

    private void setupUI() {
        trackAdapter = new TrackAdapter((track, canPlay) -> {
            String activePin = concertoViewModel.getActiveSessionPin().getValue();

            if (activePin != null) {
                // In a Concerto Room: ANYONE can add to the queue!
                concertoViewModel.addTrackToQueue(track);

                com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottomNav);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_concerto);
                }
            } else {
                // Solo Mode: Enforce Premium Check
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

                    String artistName = (track.artist != null && track.artist.name != null) ? track.artist.name : "Unknown Artist";
                    String imageUrl = (track.imageUri != null && track.imageUri.raw != null) ? track.imageUri.raw : "";

                    playerViewModel.setDisplayInfo(track.name, artistName, imageUrl);
                    playerViewModel.setControlsEnabled(true);
                    playerViewModel.playTrack(track.uri);
                    playerViewModel.expandPlayer();
                }
            }
        });

        bind.rvSearchTracks.setLayoutManager(new LinearLayoutManager(requireContext()));
        bind.rvSearchTracks.setAdapter(trackAdapter);
    }

    private void setupObservers() {
        searchViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            bind.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (isLoading) {
                bind.tvEmptyState.setVisibility(View.GONE);
                bind.rvSearchTracks.setVisibility(View.GONE);
                bind.tvResultsLabel.setVisibility(View.GONE);
            }
        });

        searchViewModel.getSearchResults().observe(getViewLifecycleOwner(), tracks -> {
            if (tracks != null && !tracks.isEmpty()) {
                trackAdapter.setTracks(tracks);
                bind.rvSearchTracks.setVisibility(View.VISIBLE);
                bind.tvEmptyState.setVisibility(View.GONE);
                bind.tvResultsLabel.setVisibility(View.VISIBLE);
            } else {
                bind.rvSearchTracks.setVisibility(View.GONE);
                bind.tvEmptyState.setVisibility(View.VISIBLE);
                bind.tvEmptyState.setText("No results found. Try another search.");
                bind.tvResultsLabel.setVisibility(View.GONE);
            }
        });

        // --- THE FIX: Observe Premium status from AuthViewModel directly ---
        authViewModel.getIsPremiumUser().observe(getViewLifecycleOwner(), isPremium -> {
            trackAdapter.setCanPlayMusic(isPremium != null && isPremium);
        });

        playerViewModel.getCurrentPlayingUri().observe(getViewLifecycleOwner(), uri -> {
            if (trackAdapter != null) {
                trackAdapter.setCurrentPlayingUri(uri);
            }
        });
    }

    private void setupButtons() {
        bind.btnSearch.setOnClickListener(v -> executeSearch());
        bind.etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                executeSearch();
                return true;
            }
            return false;
        });
    }

    private void executeSearch() {
        String query = bind.etSearchQuery.getText().toString().trim();
        if (!query.isEmpty()) {
            searchViewModel.loadSearchedSongs(query);
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(bind.etSearchQuery.getWindowToken(), 0);
            }
        } else {
            Toast.makeText(requireContext(), "Please enter a search term", Toast.LENGTH_SHORT).show();
        }
    }
}