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
import com.example.concerto.auth.LoginFragment;
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null; // Prevent memory leaks
    }

    private void initViewModels() {
        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        playerViewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);
    }

    private void setupUI() {
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

        bind.rvTracks.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        bind.rvTracks.setAdapter(trackAdapter);

        String username = authViewModel.getCurrentUsername();
        if (username != null) {
            bind.tvWelcomeMessage.setText("Welcome, " + username + "!");
        } else {
            bind.tvWelcomeMessage.setText("Welcome to your Dashboard!");
        }

        bind.progressBar.setVisibility(View.VISIBLE);
    }

    private void setupObservers() {
        dashboardViewModel.getSongs().observe(getViewLifecycleOwner(), songs -> {
            bind.progressBar.setVisibility(View.GONE);
            if (songs != null && !songs.isEmpty()) {
                trackAdapter.setTracks(songs);
            } else {
                Toast.makeText(requireContext(), "No songs found.", Toast.LENGTH_SHORT).show();
            }
        });

        authViewModel.getSpotifyToken().observe(getViewLifecycleOwner(), token -> {
            boolean isConnected = (token != null && !token.trim().isEmpty());

            trackAdapter.setCanPlayMusic(isConnected);
            bind.btnConnectSpotify.setVisibility(isConnected ? View.GONE : View.VISIBLE);

            if (isConnected && trackAdapter.getItemCount() == 0) {
                dashboardViewModel.loadRandomSongs();
            }
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

        bind.btnLogout.setOnClickListener(v -> {
            playerViewModel.pausePlayer();

            View bottomNav = requireActivity().findViewById(R.id.bottomNav);
            if (bottomNav != null) {
                bottomNav.setVisibility(View.GONE);
            }

            Fragment playerFragment = requireActivity().getSupportFragmentManager().findFragmentByTag("PLAYER");
            if (playerFragment != null) {
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .remove(playerFragment)
                        .commit();
            }

            authViewModel.logoutFull();

            requireActivity().getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.layoutFragmentContainer, new LoginFragment())
                    .commit();
        });
    }
}