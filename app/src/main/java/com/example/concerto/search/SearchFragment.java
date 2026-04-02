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
import com.example.concerto.databinding.FragmentSearchBinding;
import com.example.concerto.player.PlayerViewModel;
import com.example.concerto.auth.ConnectSpotifyFragment;

public class SearchFragment extends Fragment {

    private FragmentSearchBinding bind;
    private SearchViewModel searchViewModel;
    private PlayerViewModel playerViewModel;
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
        bind = null; // Prevent memory leaks
    }

    private void initViewModels() {
        // Scoped to this fragment
        searchViewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        // Scoped to the Activity so it can share commands with the PlayerFragment
        playerViewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);
    }

    private void setupUI() {
        // We reuse the exact same adapter logic from the Dashboard!
        trackAdapter = new TrackAdapter((track, canPlay) -> {
            if (canPlay) {
                Toast.makeText(requireContext(), "Loading: " + track.name, Toast.LENGTH_SHORT).show();
                playerViewModel.playTrack(track.uri);
                playerViewModel.expandPlayer();
            } else {
                Toast.makeText(requireContext(), "Connect Spotify to play full tracks!", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutFragmentContainer, new ConnectSpotifyFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        bind.rvSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        bind.rvSearchResults.setAdapter(trackAdapter);
    }

    private void setupObservers() {
        // 1. Observe Network Loading State
        searchViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            bind.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (isLoading) {
                bind.tvEmptyState.setVisibility(View.GONE);
                bind.rvSearchResults.setVisibility(View.GONE);
            }
        });

        // 2. Observe the Search Results
        searchViewModel.getSearchResults().observe(getViewLifecycleOwner(), tracks -> {
            if (tracks != null && !tracks.isEmpty()) {
                trackAdapter.setTracks(tracks);
                bind.rvSearchResults.setVisibility(View.VISIBLE);
                bind.tvEmptyState.setVisibility(View.GONE);
            } else {
                bind.rvSearchResults.setVisibility(View.GONE);
                bind.tvEmptyState.setVisibility(View.VISIBLE);
                bind.tvEmptyState.setText("No results found. Try another search.");
            }
        });

        // 3. Observe Playback Permissions (Did they connect Spotify?)
        searchViewModel.getCanPlayMusic().observe(getViewLifecycleOwner(), canPlay -> {
            trackAdapter.setCanPlayMusic(canPlay);
        });
    }

    private void setupButtons() {
        // Handle physical button click
        bind.btnSearch.setOnClickListener(v -> executeSearch());

        // Handle the "Enter/Search" key on the software keyboard
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

            // Hide the keyboard after searching
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(bind.etSearchQuery.getWindowToken(), 0);
            }
        } else {
            Toast.makeText(requireContext(), "Please enter a search term", Toast.LENGTH_SHORT).show();
        }
    }
}