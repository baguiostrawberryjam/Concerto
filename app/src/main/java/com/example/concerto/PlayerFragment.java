package com.example.concerto;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.concerto.auth.AuthViewModel;
import com.example.concerto.databinding.FragmentPlayerBinding;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.android.appremote.api.error.CouldNotFindSpotifyApp;
import com.spotify.android.appremote.api.error.UserNotAuthorizedException;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

public class PlayerFragment extends Fragment {

    private static final String CLIENT_ID = "a75d7664e93f41d4863ea21af859cb34";
    private static final String REDIRECT_URI = "concerto-app://callback";

    private SpotifyAppRemote mSpotifyAppRemote;
    private String trackUriToPlay = "";

    // Track playback state for our play/pause toggle
    private boolean isCurrentlyPaused = false;

    private FragmentPlayerBinding bind;
    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        bind = FragmentPlayerBinding.inflate(inflater, container, false);
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        if (getArguments() != null) {
            trackUriToPlay = getArguments().getString("TRACK_URI");
            Log.d("Player", "Received URI: " + trackUriToPlay);
        }

        // 1. Back Button functionality
        bind.btnPlayerBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        // 2. TRUE Play/Pause functionality
        bind.btnPlayerPlay.setOnClickListener(v -> {
            if (mSpotifyAppRemote != null && mSpotifyAppRemote.isConnected()) {
                if (isCurrentlyPaused) {
                    mSpotifyAppRemote.getPlayerApi().resume();
                } else {
                    mSpotifyAppRemote.getPlayerApi().pause();
                }
            }
        });

        // 3. AUTO-START the connection and playback immediately!
        bind.tvPlayerStatus.setText("Connecting...");
        connectToSpotifyAndPlay();
    }

    private void connectToSpotifyAndPlay() {
        String myAccessToken = authViewModel.getSpotifyToken().getValue();

        if (myAccessToken == null) {
            bind.tvPlayerStatus.setText("Error: Token missing. Please reconnect Spotify.");
            return;
        }

        ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(false)
                        .build();

        SpotifyAppRemote.connect(requireContext(), connectionParams,
                new Connector.ConnectionListener() {

                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        Log.d("Player", "Connected to Spotify App Remote!");

                        // Play the requested song the moment we connect
                        if (trackUriToPlay != null && !trackUriToPlay.isEmpty()) {
                            mSpotifyAppRemote.getPlayerApi().play(trackUriToPlay)
                                    .setErrorCallback(error -> {
                                        Log.e("Player", "Playback Error: " + error.getMessage());
                                        bind.tvPlayerStatus.setText("Playback Error. (Premium required?)");
                                    });
                        }

                        // Subscribe to live updates
                        subscribeToPlayerState();
                    }

                    public void onFailure(Throwable throwable) {
                        Log.e("Player", "Failed to connect", throwable);
                        if (throwable instanceof CouldNotFindSpotifyApp) {
                            bind.tvPlayerStatus.setText("Error: Spotify App missing.");
                        } else if (throwable instanceof UserNotAuthorizedException) {
                            bind.tvPlayerStatus.setText("Error: Fingerprint missing in Dashboard.");
                        } else {
                            bind.tvPlayerStatus.setText("Connection Failed!");
                        }
                    }
                });
    }

    private void subscribeToPlayerState() {
        mSpotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(playerState -> {
                    final Track track = playerState.track;
                    if (track != null) {
                        // Keep our local variable up to date with actual Spotify state
                        isCurrentlyPaused = playerState.isPaused;

                        // Update Text
                        bind.tvPlayerStatus.setText(track.name + "\n" + track.artist.name);

                        // Update Play/Pause Icon based on actual state
                        if (isCurrentlyPaused) {
                            bind.btnPlayerPlay.setImageResource(android.R.drawable.ic_media_play);
                        } else {
                            bind.btnPlayerPlay.setImageResource(android.R.drawable.ic_media_pause);
                        }

                        // Fetch the High-Res Album Art
                        mSpotifyAppRemote.getImagesApi()
                                .getImage(track.imageUri)
                                .setResultCallback(bitmap -> {
                                    bind.ivPlayerAlbumArt.setImageBitmap(bitmap);
                                });
                    }
                });
    }

    @Override
    public void onStop() {
        super.onStop();
        // Disconnecting here saves battery. The music will keep playing in the background!
        if (mSpotifyAppRemote != null && mSpotifyAppRemote.isConnected()) {
            SpotifyAppRemote.disconnect(mSpotifyAppRemote);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }
}