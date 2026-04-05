package com.example.concerto.dashboard;

import androidx.lifecycle.ViewModelProvider;
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
        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        playerViewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);
    }

    private void setupRecyclerView() {
        trackAdapter = new TrackAdapter((track, canPlayMusic) -> {
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
        });

        bind.rvDashboardTracks.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        bind.rvDashboardTracks.setAdapter(trackAdapter);
        bind.progressBar.setVisibility(View.VISIBLE);
    }

    private void setupObservers() {
        dashboardViewModel.getSongs().observe(getViewLifecycleOwner(), songs -> {
            if (!isAdded() || bind == null) return;

            bind.progressBar.setVisibility(View.GONE);
            if (songs != null && !songs.isEmpty()) {
                trackAdapter.setTracks(songs);
            } else {
                Toast.makeText(requireContext(), "No songs found.", Toast.LENGTH_SHORT).show();
            }
        });

        authViewModel.getSpotifyToken().observe(getViewLifecycleOwner(), token -> {
            if (!isAdded() || bind == null) return;

            boolean isConnected = (token != null && !token.trim().isEmpty());
            trackAdapter.setCanPlayMusic(isConnected);
            bind.btnConnectSpotify.setVisibility(isConnected ? View.GONE : View.VISIBLE);

            if (trackAdapter.getItemCount() == 0) {
                dashboardViewModel.loadRandomSongs();
            }
        });

        playerViewModel.getCurrentPlayingUri().observe(getViewLifecycleOwner(), uri -> {
            if (trackAdapter != null) {
                trackAdapter.setCurrentPlayingUri(uri);
            }
        });

        if (authViewModel.getSpotifyToken().getValue() == null) {
            authViewModel.restoreSpotifyTokenFromFirebase(null);
        }
    }

    private void setupButtons() {
        bind.btnConnectSpotify.setOnClickListener(v -> {
            if (isAdded() && getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutFragmentContainer, new ConnectSpotifyFragment())
                        .addToBackStack(null)
                        .commitAllowingStateLoss();
            }
        });
    }
}