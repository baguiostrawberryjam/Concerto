package com.example.concerto.player;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.concerto.player.PlayerViewModel;
import com.example.concerto.R;
import com.example.concerto.databinding.FragmentPlayerBinding;
import com.example.concerto.spotify.SpotifyManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.spotify.protocol.types.Track;

public class PlayerFragment extends Fragment {

    private SpotifyManager spotifyManager;

    private boolean isSubscribed = false;

    private FragmentPlayerBinding bind;
    private PlayerViewModel playerViewModel;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private BottomSheetBehavior.BottomSheetCallback bottomSheetCallback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        bind = FragmentPlayerBinding.inflate(inflater, container, false);
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spotifyManager = new SpotifyManager(requireContext());
        initViewModels();

        setupBottomSheet();
        setupButtons();
        observePlayerCommands();
    }

    private void subscribeToPlayerState() {
        spotifyManager.subscribeToPlayerState((track, isPaused) -> {
            if (bind == null) return;

            playerViewModel.setCurrentPlayingUri(track.uri);
            playerViewModel.setIsCurrentlyPaused(isPaused);

            bind.tvMiniTrackName.setText(track.name);
            bind.tvMiniArtistName.setText(track.artist.name);
            bind.tvPlayerStatus.setText(track.name + "\n" + track.artist.name);

            int iconRes = isPaused
                    ? android.R.drawable.ic_media_play
                    : android.R.drawable.ic_media_pause;

            bind.btnPlayerPlay.setImageResource(iconRes);
            bind.btnMiniPlay.setImageResource(iconRes);

            if (bottomSheetBehavior != null &&
                    bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }

            // Image loading now via manager
            spotifyManager.loadImage(track.imageUri, bitmap -> {
                if (bind == null) return;

                bind.ivPlayerAlbumArt.setImageBitmap(bitmap);
                bind.ivMiniAlbumArt.setImageBitmap(bitmap);
            });
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        spotifyManager.disconnect();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (bottomSheetBehavior != null && bottomSheetCallback != null) {
            bottomSheetBehavior.removeBottomSheetCallback(bottomSheetCallback);
        }

        bind = null;
    }

    private void initViewModels() {
        playerViewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);
    }

    private void setupBottomSheet() {

        View bottomSheet = requireActivity().findViewById(R.id.player_sheet_container);
        if (bottomSheet != null) {
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

            bottomSheetCallback = new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    if (bind == null) return;

                    bind.layoutMiniPlayer.setAlpha(1.0f - slideOffset);
                    bind.layoutMiniPlayer.setVisibility(slideOffset > 0.95f ? View.INVISIBLE : View.VISIBLE);

                    bind.layoutFullPlayer.setAlpha(slideOffset);
                    bind.layoutFullPlayer.setVisibility(slideOffset < 0.05f ? View.INVISIBLE : View.VISIBLE);
                }
            };

            bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback);

            bind.layoutMiniPlayer.setOnClickListener(v -> {
                expandPlayer();
            });
        }

    }

    private void setupButtons() {
        View.OnClickListener togglePlayPause = v -> {
            if (spotifyManager.isReady()) {
            Boolean isPaused = playerViewModel.getIsCurrentlyPaused().getValue();

            if (isPaused != null && isPaused) {
                spotifyManager.resume();
            } else {
                spotifyManager.pause();
            }
        }
        };
        bind.btnPlayerPlay.setOnClickListener(togglePlayPause);
        bind.btnMiniPlay.setOnClickListener(togglePlayPause);
    }

    private void observePlayerCommands() {

        playerViewModel.getCommandPlayUri().observe(getViewLifecycleOwner(), uri -> {
            if (uri != null && uri.equals(playerViewModel.getCurrentPlayingUri().getValue())) {
                expandPlayer();
                return;
            }

            spotifyManager.playOrConnect(uri, new SpotifyManager.ConnectionListener() {
                @Override
                public void onConnected() {
                    if (!isSubscribed) {
                    subscribeToPlayerState();
                    isSubscribed = true;
                }
                }

                @Override
                public void onError(Throwable error) {
                    Log.e("Spotify", "Connection failed", error);
                }
            });
        });

        playerViewModel.getCommandExpand().observe(getViewLifecycleOwner(), expand -> {
            if (expand) expandPlayer();
        });

        playerViewModel.getCommandPause().observe(getViewLifecycleOwner(), pause -> {
            if (pause && spotifyManager.isReady()) {
                spotifyManager.pause();
            }
        });

    }

    // HELPER METHODS

    private void expandPlayer() {
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }
}