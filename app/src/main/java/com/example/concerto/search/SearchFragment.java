package com.example.concerto.search;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.concerto.R;
import com.example.concerto.adapters.TrackAdapter;
import com.example.concerto.concerto.ConcertoViewModel;
import com.example.concerto.databinding.FragmentSearchBinding;
import com.example.concerto.player.PlayerViewModel;
import com.example.concerto.auth.ConnectSpotifyFragment;

public class SearchFragment extends Fragment {

    private FragmentSearchBinding bind;
    private SearchViewModel searchViewModel;
    private PlayerViewModel playerViewModel;
    private ConcertoViewModel concertoViewModel;
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
        setupRecyclerView();
        setupObservers();
        setupButtons();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }

    // ==========================================
    // INITIALIZATION METHODS
    // ==========================================

    private void initViewModels() {
        if (getActivity() == null) return;
        searchViewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        playerViewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);
        concertoViewModel = new ViewModelProvider(requireActivity()).get(ConcertoViewModel.class);
    }

    private void setupRecyclerView() {
        trackAdapter = new TrackAdapter((track, canPlayMusic) -> {
            String activePin = concertoViewModel.getActiveSessionPin().getValue();
            if (activePin != null) {
                concertoViewModel.addTrackToQueue(track);
                if (isAdded() && getActivity() != null) {
                    Toast.makeText(requireContext(), "Adding to Concerto queue...", Toast.LENGTH_SHORT).show();
                    getActivity().findViewById(R.id.bottomNav).performClick(); // Re-select Concerto to go back
                }
            } else {
                if (canPlayMusic) {
                    playerViewModel.playTrack(track.uri);
                    playerViewModel.setDisplayInfo(track.name, track.artist != null ? track.artist.name : "Unknown Artist", track.imageUri != null ? track.imageUri.raw : "");
                } else {
                    if (isAdded() && getActivity() != null) {
                        Toast.makeText(requireContext(), "Connect Spotify to play music", Toast.LENGTH_SHORT).show();
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.layoutFragmentContainer, new ConnectSpotifyFragment())
                                .addToBackStack(null)
                                .commitAllowingStateLoss();
                    }
                }
            }
        });

        bind.rvSearchTracks.setLayoutManager(new LinearLayoutManager(requireContext()));
        bind.rvSearchTracks.setAdapter(trackAdapter);
    }

    private void setupObservers() {
        searchViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (!isAdded() || bind == null) return;
            bind.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        searchViewModel.getSearchResults().observe(getViewLifecycleOwner(), songs -> {
            if (!isAdded() || bind == null) return;
            if (songs != null && !songs.isEmpty()) {
                trackAdapter.setTracks(songs);
            } else {
                Toast.makeText(requireContext(), "No results found. Try another search.", Toast.LENGTH_SHORT).show();
            }
        });

        searchViewModel.getCanPlayMusic().observe(getViewLifecycleOwner(), canPlay -> {
            if (trackAdapter != null) trackAdapter.setCanPlayMusic(canPlay);
        });

        playerViewModel.getCurrentPlayingUri().observe(getViewLifecycleOwner(), uri -> {
            if (trackAdapter != null) trackAdapter.setCurrentPlayingUri(uri);
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

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private void executeSearch() {
        if (bind == null || !isAdded()) return;

        String query = bind.etSearchQuery.getText().toString().trim();
        if (!query.isEmpty()) {
            searchViewModel.loadSearchedSongs(query);

            // FIXED: Safe Context Check for Keyboard Manager
            Context context = getContext();
            if (context != null) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(bind.etSearchQuery.getWindowToken(), 0);
                }
            }
        } else {
            Toast.makeText(requireContext(), "Please enter a search term", Toast.LENGTH_SHORT).show();
        }
    }
}