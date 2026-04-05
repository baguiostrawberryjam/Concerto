package com.example.concerto.concerto;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.concerto.R;
import com.example.concerto.adapters.ConcertoQueueAdapter;
import com.example.concerto.databinding.FragmentConcertoBinding;
import com.example.concerto.player.PlayerViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ConcertoFragment extends Fragment {

    private FragmentConcertoBinding bind;
    private ConcertoViewModel concertoViewModel;
    private ConcertoQueueAdapter queueAdapter;
    private PlayerViewModel playerViewModel;
    private String currentUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        bind = FragmentConcertoBinding.inflate(inflater, container, false);
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViewModels();
        currentUid = concertoViewModel.getCurrentUserUid();

        setupRecyclerView();
        setupButtons();
        setupObservers();
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
        concertoViewModel = new ViewModelProvider(requireActivity()).get(ConcertoViewModel.class);
        playerViewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);
    }

    private void setupRecyclerView() {
        queueAdapter = new ConcertoQueueAdapter(currentUid, new ConcertoQueueAdapter.ConcertoTrackListener() {
            @Override
            public void onVoteClick(com.example.concerto.models.ConcertoTrack track) {
                concertoViewModel.toggleVoteForTrack(track);
            }

            @Override
            public void onTrackClick(com.example.concerto.models.ConcertoTrack track) {
                String currentPlaying = playerViewModel.getCurrentPlayingUri().getValue();
                if (currentPlaying != null && currentPlaying.equals(track.uri)) {
                    playerViewModel.expandPlayer();
                }
            }
        });

        bind.rvConcertoQueue.setLayoutManager(new LinearLayoutManager(requireContext()));
        bind.rvConcertoQueue.setAdapter(queueAdapter);
    }

    private void setupButtons() {
        bind.btnLeaveSession.setOnClickListener(v -> {
            concertoViewModel.leaveConcerto();
            if (isAdded() && getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutFragmentContainer, new ConcertoLobbyFragment())
                        .commit();
            }
        });

        bind.btnAddSong.setOnClickListener(v -> {
            if (getActivity() != null) {
                // Navigate to SearchFragment safely
                BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNav);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_search);
                }
            }
        });

        bind.btnHostSkip.setOnClickListener(v -> {
            concertoViewModel.playNextTrack();
        });
    }

    private void setupObservers() {
        concertoViewModel.getActiveSessionPin().observe(getViewLifecycleOwner(), pin -> {
            if (pin != null && bind != null) {
                bind.tvSessionPin.setText("Join Code: " + pin);
            }
        });

        concertoViewModel.getIsHost().observe(getViewLifecycleOwner(), isHost -> {
            if (isHost != null && bind != null) {
                bind.btnHostSkip.setVisibility(isHost ? View.VISIBLE : View.GONE);
                bind.btnLeaveSession.setText(isHost ? "End Session" : "Leave");
            }
        });

        playerViewModel.getCurrentPlayingUri().observe(getViewLifecycleOwner(), uri -> {
            if (queueAdapter != null) queueAdapter.setCurrentPlayingUri(uri);
        });

        Runnable updateUnifiedQueueUI = () -> {
            java.util.List<com.example.concerto.models.ConcertoTrack> combinedList = new java.util.ArrayList<>();

            com.example.concerto.models.ConcertoTrack playing = concertoViewModel.getCurrentlyPlaying().getValue();
            if (playing != null) combinedList.add(playing);

            java.util.List<com.example.concerto.models.ConcertoTrack> queue = concertoViewModel.getQueue().getValue();
            if (queue != null) combinedList.addAll(queue);

            queueAdapter.setQueue(combinedList);
        };

        concertoViewModel.getQueue().observe(getViewLifecycleOwner(), tracks -> updateUnifiedQueueUI.run());

        concertoViewModel.getCurrentlyPlaying().observe(getViewLifecycleOwner(), track -> {
            updateUnifiedQueueUI.run();
            Boolean isHostVal = concertoViewModel.getIsHost().getValue();

            if (track != null) {
                String currentPlayingUri = playerViewModel.getCurrentPlayingUri().getValue();

                if (isHostVal != null && isHostVal) {
                    playerViewModel.setControlsEnabled(true);
                    if (!track.uri.equals(currentPlayingUri)) {
                        playerViewModel.playTrack(track.uri);
                    }
                } else {
                    playerViewModel.setControlsEnabled(false);
                }

                playerViewModel.setDisplayInfo(track.name, track.artist, track.imageUrl != null ? track.imageUrl : "");

            } else {
                playerViewModel.setDisplayInfo("Waiting for music...", "Join the queue", "");
                playerViewModel.setControlsEnabled(false);

                if (isHostVal != null && isHostVal) {
                    playerViewModel.pausePlayer();
                }
            }
        });

        concertoViewModel.getSessionStatus().observe(getViewLifecycleOwner(), status -> {
            Boolean isHostVal = concertoViewModel.getIsHost().getValue();
            if ("ended".equals(status) && (isHostVal == null || !isHostVal) && isAdded() && getActivity() != null) {
                Toast.makeText(requireContext(), "The Host ended the session.", Toast.LENGTH_LONG).show();
                concertoViewModel.leaveConcerto();

                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutFragmentContainer, new ConcertoLobbyFragment())
                        .commitAllowingStateLoss();
            }
        });

        concertoViewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty() && isAdded()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                concertoViewModel.clearToastMessage();
            }
        });
    }
}