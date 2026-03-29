package com.example.concerto;

import androidx.lifecycle.ViewModelProvider;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.concerto.adapters.TrackAdapter;
import com.example.concerto.auth.AuthViewModel;
import com.example.concerto.databinding.FragmentDashboardBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class DashboardFragment extends Fragment {

    private DashboardViewModel dashboardViewModel;
    private AuthViewModel authViewModel;
    private FragmentDashboardBinding bind;
    private FirebaseAuth mAuth;

    public static DashboardFragment newInstance() {
        return new DashboardFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        bind = FragmentDashboardBinding.inflate(inflater, container, false);
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        TrackAdapter trackAdapter = new TrackAdapter();
        bind.rvTracks.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        bind.rvTracks.setAdapter(trackAdapter);

        if (currentUser != null && currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
            bind.tvWelcomeMessage.setText("Welcome, " + currentUser.getDisplayName() + "!");
        } else {
            bind.tvWelcomeMessage.setText("Welcome to your Dashboard!");
        }

        bind.progressBar.setVisibility(View.VISIBLE);

        dashboardViewModel.getSongs().observe(getViewLifecycleOwner(), songs -> {
            bind.progressBar.setVisibility(View.GONE);
            if (songs != null && !songs.isEmpty()) {
                trackAdapter.setTracks(songs);
            } else {
                Toast.makeText(requireContext(), "No songs found.", Toast.LENGTH_SHORT).show();
            }
        });

        // Toggle UI based on connection status
        dashboardViewModel.getCanPlayMusic().observe(getViewLifecycleOwner(), canPlay -> {
            trackAdapter.setCanPlayMusic(canPlay);
            if (canPlay) {
                bind.btnConnectSpotify.setVisibility(View.GONE);
            } else {
                bind.btnConnectSpotify.setVisibility(View.VISIBLE);
            }
        });

        authViewModel.restoreSpotifyTokenFromFirebase(() -> {
            dashboardViewModel.loadRandomSongs();
        });

        // NEW: Navigate to the ConnectSpotifyFragment!
        bind.btnConnectSpotify.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ConnectSpotifyFragment())
                    .addToBackStack(null) // Allows the user to hit the physical back button to cancel
                    .commit();
        });

        bind.btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            authViewModel.logoutSpotify();

            requireActivity().getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new LoginFragment())
                    .commit();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }
}