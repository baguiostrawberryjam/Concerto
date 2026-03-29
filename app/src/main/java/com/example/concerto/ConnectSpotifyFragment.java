package com.example.concerto;

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
import com.example.concerto.auth.AuthViewModel;
import com.example.concerto.databinding.FragmentConnectSpotifyBinding;
import com.example.concerto.utils.PKCEUtil;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

public class ConnectSpotifyFragment extends Fragment {

    private static final String CLIENT_ID = "a75d7664e93f41d4863ea21af859cb34";
    private static final String REDIRECT_URI = "concerto-app://callback";

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

        // Share the ViewModel with MainActivity
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        // OBSERVER: Watch for the token to arrive from MainActivity
        authViewModel.getSpotifyToken().observe(getViewLifecycleOwner(), token -> {
            if (token != null) {
                Toast.makeText(requireContext(), "Spotify Connected successfully!", Toast.LENGTH_SHORT).show();

                // Success! Remove this Fragment and go back to the Dashboard
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        bind.btnSpotifyLogin.setOnClickListener(v -> startSpotifyLogin());

        // Change your old Logout button into a "Cancel" button for this screen
        bind.btnLogout.setText("Cancel");
        bind.btnLogout.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });
    }

    private void startSpotifyLogin() {
        String verifier = PKCEUtil.generateCodeVerifier();
        String challenge = PKCEUtil.generateCodeChallenge(verifier);

        authViewModel.setCodeVerifier(verifier);

        AuthorizationRequest request = new AuthorizationRequest.Builder(
                CLIENT_ID,
                AuthorizationResponse.Type.CODE,
                REDIRECT_URI)
                .setScopes(new String[]{"app-remote-control", "user-read-playback-state"})
                .setShowDialog(true)
                .setCustomParam("code_challenge_method", "S256")
                .setCustomParam("code_challenge", challenge)
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW, request.toUri());
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }
}