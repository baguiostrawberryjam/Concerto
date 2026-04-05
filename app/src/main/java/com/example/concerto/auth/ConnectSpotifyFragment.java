package com.example.concerto.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.concerto.R;
import com.example.concerto.databinding.FragmentConnectSpotifyBinding;
import com.example.concerto.spotify.SpotifyConfig;
import com.example.concerto.utils.PKCEUtil;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

public class ConnectSpotifyFragment extends Fragment {

    private AuthViewModel authViewModel;
    private FragmentConnectSpotifyBinding bind;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        bind = FragmentConnectSpotifyBinding.inflate(inflater, container, false);
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        hideNavBar();
        initViewModels();
        setupObservers();
        setupButtons();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        showNavBar();
        bind = null;
    }

    private void hideNavBar() {
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.bottomNav);
            if (bottomNav != null) {
                bottomNav.setVisibility(View.GONE);
            }
        }
    }

    private void showNavBar() {
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.bottomNav);
            if (bottomNav != null) {
                bottomNav.setVisibility(View.VISIBLE);
            }
        }
    }

    private void initViewModels() {
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
    }

    private void setupObservers() {
        authViewModel.getSpotifyToken().observe(getViewLifecycleOwner(), token -> {
            // FIXED: Ensure fragment is attached before interacting with Context or FragmentManager
            if (token != null && !token.equals("") && isAdded() && getActivity() != null) {
                Toast.makeText(requireContext(), "Spotify Connected successfully!", Toast.LENGTH_SHORT).show();
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void setupButtons() {
        bind.btnSpotifyLogin.setOnClickListener(v -> startSpotifyLogin());

        bind.btnCancel.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void startSpotifyLogin() {
        String verifier = PKCEUtil.generateCodeVerifier();
        String challenge = PKCEUtil.generateCodeChallenge(verifier);

        authViewModel.setCodeVerifier(verifier);

        AuthorizationRequest request = new AuthorizationRequest.Builder(
                SpotifyConfig.CLIENT_ID,
                AuthorizationResponse.Type.CODE,
                SpotifyConfig.REDIRECT_URI)
                .setScopes(new String[]{"app-remote-control", "user-read-playback-state"})
                .setShowDialog(true)
                .setCustomParam("code_challenge_method", "S256")
                .setCustomParam("code_challenge", challenge)
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW, request.toUri());
        startActivity(intent);
    }
}