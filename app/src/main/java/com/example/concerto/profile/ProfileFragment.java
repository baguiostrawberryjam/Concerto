package com.example.concerto.profile;

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
import com.example.concerto.adapters.HostedConcertoAdapter;
import com.example.concerto.auth.AuthViewModel;
import com.example.concerto.auth.ConnectSpotifyFragment;
import com.example.concerto.auth.LoginFragment;
import com.example.concerto.concerto.ConcertoFragment;
import com.example.concerto.concerto.ConcertoViewModel;
import com.example.concerto.databinding.FragmentProfileBinding;
import com.example.concerto.player.PlayerViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding bind;
    private AuthViewModel authViewModel;
    private ProfileViewModel profileViewModel;
    private PlayerViewModel playerViewModel;
    private ConcertoViewModel concertoViewModel;
    private HostedConcertoAdapter adapter;

    private boolean isLoggingOut = false;
    private boolean isAttemptingToJoin = false;

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        bind = FragmentProfileBinding.inflate(inflater, container, false);
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Hide Nav Bar on Entry
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.bottomNav);
            if (bottomNav != null) bottomNav.setVisibility(View.GONE);
        }

        initViewModels();
        setupUI();
        setupObservers();
        setupButtons();

        profileViewModel.fetchUserConcertos();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (!isLoggingOut && getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.bottomNav);
            if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
        }
        bind = null;
    }

    private void initViewModels() {
        if (getActivity() == null) return;
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        playerViewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        concertoViewModel = new ViewModelProvider(requireActivity()).get(ConcertoViewModel.class);
    }

    private void setupUI() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            bind.tvUsername.setText(authViewModel.getCurrentUsername());
            bind.tvEmail.setText(user.getEmail());
        }

        adapter = new HostedConcertoAdapter((pin, isActive) -> {
            if (!isActive) {
                if (isAdded()) Toast.makeText(getContext(), "Session already ended", Toast.LENGTH_SHORT).show();
                return;
            }
            isAttemptingToJoin = true;
            concertoViewModel.joinHostedConcerto(pin);
            if (isAdded()) Toast.makeText(getContext(), "Joining Room: " + pin, Toast.LENGTH_SHORT).show();
        });

        bind.rvHostedConcertos.setLayoutManager(new LinearLayoutManager(requireContext()));
        bind.rvHostedConcertos.setAdapter(adapter);
    }

    private void setupObservers() {
        authViewModel.getSpotifyToken().observe(getViewLifecycleOwner(), token -> {
            if (bind == null || !isAdded()) return;
            boolean isConnected = (token != null && !token.trim().isEmpty());

            if (isConnected) {
                bind.tvSpotifyStatus.setText("Spotify: Connected");
                bind.tvSpotifyStatus.setTextColor(android.graphics.Color.parseColor("#1DB954"));
                bind.btnConnectSpotify.setVisibility(View.GONE);
            } else {
                bind.tvSpotifyStatus.setText("Spotify: Disconnected");
                bind.tvSpotifyStatus.setTextColor(android.graphics.Color.parseColor("#FF5555"));
                bind.btnConnectSpotify.setVisibility(View.VISIBLE);
            }
        });

        profileViewModel.getHostedConcertos().observe(getViewLifecycleOwner(), concertos -> {
            if (bind == null || !isAdded()) return;
            if (concertos != null && !concertos.isEmpty()) {
                adapter.setConcertos(concertos);
            }
        });

        concertoViewModel.getActiveSessionPin().observe(getViewLifecycleOwner(), pin -> {
            if (pin != null && isAttemptingToJoin && isAdded() && getActivity() != null) {
                isAttemptingToJoin = false;
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutFragmentContainer, new ConcertoFragment())
                        .commitAllowingStateLoss();
            }
        });

        concertoViewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty() && isAdded()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                concertoViewModel.clearToastMessage();
            }
        });
    }

    private void setupButtons() {
        bind.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNav);
                if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_home);
            }
        });

        bind.btnSettings.setOnClickListener(v -> {
            if (isAdded()) Toast.makeText(requireContext(), "Settings coming soon!", Toast.LENGTH_SHORT).show();
        });

        bind.btnEditProfile.setOnClickListener(v -> {
            if (isAdded()) Toast.makeText(requireContext(), "Edit Profile coming soon!", Toast.LENGTH_SHORT).show();
        });

        bind.btnConnectSpotify.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutFragmentContainer, new ConnectSpotifyFragment())
                        .addToBackStack(null)
                        .commitAllowingStateLoss();
            }
        });

        bind.btnLogout.setOnClickListener(v -> {
            isLoggingOut = true;

            playerViewModel.pausePlayer();
            playerViewModel.reset();
            concertoViewModel.leaveConcerto();

            if (getActivity() != null) {
                View bottomNav = getActivity().findViewById(R.id.bottomNav);
                if (bottomNav != null) bottomNav.setVisibility(View.GONE);

                Fragment playerFragment = getActivity().getSupportFragmentManager().findFragmentByTag("PLAYER");
                if (playerFragment != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .remove(playerFragment)
                            .commitAllowingStateLoss();
                }

                authViewModel.logoutFull();

                getActivity().getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);

                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutFragmentContainer, new LoginFragment())
                        .commitAllowingStateLoss();
            }
        });
    }
}