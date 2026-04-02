package com.example.concerto.player;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.concerto.R;
import com.example.concerto.databinding.FragmentPlayerBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class PlayerFragment extends Fragment {

    private SpotifyPlayerController spotifyManager;
    private FragmentPlayerBinding bind;
    private PlayerViewModel playerViewModel;

    private BottomSheetBehavior<View> bottomSheetBehavior;
    private BottomSheetBehavior.BottomSheetCallback bottomSheetCallback;
    private boolean isSubscribed = false;

    // --- Progress Bar State ---
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private boolean isUserSeeking = false;

    // This loop runs every 1000ms (1 second) to check the real Spotify position
    private final Runnable progressUpdater = new Runnable() {
        @Override
        public void run() {
            if (bind != null && !isUserSeeking && spotifyManager != null && spotifyManager.isReady()) {
                spotifyManager.getPlayerState(playerState -> {
                    if (playerState != null && playerState.track != null && !playerState.isPaused) {
                        bind.seekBarProgress.setMax((int) playerState.track.duration);
                        bind.seekBarProgress.setProgress((int) playerState.playbackPosition);
                        bind.tvCurrentTime.setText(formatDuration(playerState.playbackPosition));
                        bind.tvTotalTime.setText(formatDuration(playerState.track.duration));
                    }
                });
            }
            // Re-trigger itself in 1 second
            progressHandler.postDelayed(this, 1000);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        bind = FragmentPlayerBinding.inflate(inflater, container, false);
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViewModels();
        setupUI();
        setupObservers();
        setupButtons();

        // Start the UI Ticker
        progressHandler.post(progressUpdater);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        progressHandler.removeCallbacks(progressUpdater);

        if (bottomSheetBehavior != null && bottomSheetCallback != null) {
            bottomSheetBehavior.removeBottomSheetCallback(bottomSheetCallback);
        }
        bind = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (spotifyManager != null) {
            spotifyManager.disconnect();
        }
    }

    private void initViewModels() {
        playerViewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);
        spotifyManager = new SpotifyPlayerController(requireContext());
    }

    private void setupUI() {
        View bottomSheet = requireActivity().findViewById(R.id.layoutPlayerSheetContainer);
        if (bottomSheet != null) {
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
            bottomSheetBehavior.setHideable(true);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            bottomSheet.setVisibility(View.VISIBLE);

            bottomSheetCallback = new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {}

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    if (bind == null) return;
                    bind.layoutMiniPlayer.setAlpha(1.0f - slideOffset);
                    bind.layoutMiniPlayer.setVisibility(slideOffset > 0.95f ? View.INVISIBLE : View.VISIBLE);
                    bind.layoutFullPlayer.setAlpha(slideOffset);
                    bind.layoutFullPlayer.setVisibility(slideOffset < 0.05f ? View.INVISIBLE : View.VISIBLE);

                    View bottomNav = requireActivity().findViewById(R.id.bottomNav);
                    if (bottomNav != null) {
                        float safeOffset = Math.max(0f, slideOffset);
                        bottomNav.setTranslationY(safeOffset * bottomNav.getHeight());
                        bottomNav.setAlpha(1.0f - safeOffset);
                    }
                }
            };
            bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback);
        }
    }

    private void setupObservers() {
        playerViewModel.getCommandPlayUri().observe(getViewLifecycleOwner(), uri -> {
            if (uri != null && uri.equals(playerViewModel.getCurrentPlayingUri().getValue())) {
                expandPlayer();
                return;
            }

            spotifyManager.playOrConnect(uri, new SpotifyPlayerController.ConnectionListener() {
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
            if (expand != null && expand) expandPlayer();
        });

        playerViewModel.getCommandPause().observe(getViewLifecycleOwner(), pause -> {
            if (pause != null && pause && spotifyManager.isReady()) {
                spotifyManager.pause();
            }
        });

        // ADDED: Observe Seek Commands
        playerViewModel.getCommandSeekTo().observe(getViewLifecycleOwner(), positionMs -> {
            if (positionMs != null && spotifyManager.isReady()) {
                spotifyManager.seekTo(positionMs);
            }
        });
    }

    private void setupButtons() {
        bind.layoutMiniPlayer.setOnClickListener(v -> expandPlayer());

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

        // ADDED: Handle User Dragging the SeekBar
        bind.seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && bind != null) {
                    // Update text instantly while dragging so the user knows where they are
                    bind.tvCurrentTime.setText(formatDuration(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Pause our background updater so it doesn't fight the user's finger
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
                // Fire the Walkie-Talkie command to the ViewModel
                playerViewModel.seekTo(seekBar.getProgress());
            }
        });
    }

    private void subscribeToPlayerState() {
        spotifyManager.subscribeToPlayerState((track, isPaused) -> {
            if (bind == null) return;

            playerViewModel.setCurrentPlayingUri(track.uri);
            playerViewModel.setIsCurrentlyPaused(isPaused);

            bind.tvMiniTrackName.setText(track.name);
            bind.tvMiniArtistName.setText(track.artist.name);
            bind.tvPlayerStatus.setText(track.name + "\n" + track.artist.name);

            // Fetch state once immediately on track change to set the max duration
            spotifyManager.getPlayerState(playerState -> {
                if (playerState != null && playerState.track != null) {
                    bind.seekBarProgress.setMax((int) playerState.track.duration);
                    bind.tvTotalTime.setText(formatDuration(playerState.track.duration));
                }
            });

            int iconRes = isPaused ? android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause;
            bind.btnPlayerPlay.setImageResource(iconRes);
            bind.btnMiniPlay.setImageResource(iconRes);

            if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }

            spotifyManager.loadImage(track.imageUri, bitmap -> {
                if (bind == null) return;
                bind.ivPlayerAlbumArt.setImageBitmap(bitmap);
                bind.ivMiniAlbumArt.setImageBitmap(bitmap);
            });
        });
    }

    private void expandPlayer() {
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private String formatDuration(long durationMs) {
        long seconds = (durationMs / 1000) % 60;
        long minutes = (durationMs / (1000 * 60)) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}