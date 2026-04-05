package com.example.concerto.player;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.concerto.R;
import com.example.concerto.concerto.ConcertoViewModel;
import com.example.concerto.databinding.FragmentPlayerBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class PlayerFragment extends Fragment {

    private FragmentPlayerBinding bind;
    private SpotifyPlayerController spotifyManager;
    private PlayerViewModel playerViewModel;
    private ConcertoViewModel concertoViewModel;

    private BottomSheetBehavior<View> bottomSheetBehavior;
    private BottomSheetBehavior.BottomSheetCallback bottomSheetCallback;

    private boolean isSubscribed = false;
    private boolean isUserSeeking = false;
    private boolean isTransitioning = false;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());

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

    // ==========================================
    // INITIALIZATION METHODS
    // ==========================================

    private void initViewModels() {
        if (getActivity() == null) return;
        playerViewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);
        concertoViewModel = new ViewModelProvider(requireActivity()).get(ConcertoViewModel.class);
        spotifyManager = new SpotifyPlayerController(requireContext());
    }

    private void setupUI() {
        if (getActivity() == null) return;

        View bottomSheet = getActivity().findViewById(R.id.layoutPlayerSheetContainer);
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

                    if (getActivity() != null) {
                        View bottomNav = getActivity().findViewById(R.id.bottomNav);
                        if (bottomNav != null) {
                            float safeOffset = Math.max(0f, slideOffset);
                            bottomNav.setTranslationY(safeOffset * bottomNav.getHeight());
                            bottomNav.setAlpha(1.0f - safeOffset);
                        }
                    }
                }
            };
            bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            bind.viewMiniPlayerBg.setRenderEffect(
                    android.graphics.RenderEffect.createBlurEffect(30f, 30f, android.graphics.Shader.TileMode.CLAMP)
            );
        }
        startSmoothMiniPlayerAnimation();
    }

    private void setupObservers() {
        playerViewModel.getCommandPlayUri().observe(getViewLifecycleOwner(), uri -> {
            if (uri == null || !isAdded()) return;

            if (uri.equals(playerViewModel.getCurrentPlayingUri().getValue())) {
                expandPlayer();
                return;
            }

            spotifyManager.playOrConnect(uri, new SpotifyPlayerController.ConnectionListener() {
                @Override
                public void onConnected() {
                    if (!isSubscribed && isAdded()) {
                        subscribeToPlayerState();
                        isSubscribed = true;
                    }
                }
                @Override public void onError(Throwable error) { Log.e("Spotify", "Connection failed", error); }
            });
        });

        playerViewModel.getCommandExpand().observe(getViewLifecycleOwner(), expand -> {
            if (expand != null && expand && isAdded()) expandPlayer();
        });

        playerViewModel.getCommandPause().observe(getViewLifecycleOwner(), pause -> {
            if (pause != null && pause && spotifyManager.isReady() && isAdded()) {
                spotifyManager.pause();
            }
        });

        playerViewModel.getCommandSeekTo().observe(getViewLifecycleOwner(), positionMs -> {
            if (positionMs != null && spotifyManager.isReady() && isAdded()) {
                spotifyManager.seekTo(positionMs);
            }
        });

        playerViewModel.getDisplayTrackName().observe(getViewLifecycleOwner(), name -> {
            if (bind != null && name != null) {
                bind.tvMiniTrackName.setText(name);
                bind.tvTrackName.setText(name);
            }
        });

        playerViewModel.getDisplayArtistName().observe(getViewLifecycleOwner(), artist -> {
            if (bind != null && artist != null) {
                bind.tvMiniArtistName.setText(artist);
                bind.tvArtistName.setText(artist);
            }
        });

        playerViewModel.getDisplayImageUrl().observe(getViewLifecycleOwner(), url -> {
            if (bind != null && isAdded()) {
                if (url != null && !url.isEmpty()) {
                    com.bumptech.glide.Glide.with(this).load(url).into(bind.ivPlayerAlbumArt);
                    com.bumptech.glide.Glide.with(this).load(url).into(bind.ivMiniAlbumArt);
                } else {
                    bind.ivPlayerAlbumArt.setImageResource(R.drawable.img_album_placeholder);
                    bind.ivMiniAlbumArt.setImageResource(R.drawable.img_album_placeholder);
                }
            }
        });

        playerViewModel.getControlsEnabled().observe(getViewLifecycleOwner(), enabled -> {
            if (bind != null && enabled != null) {
                bind.seekBarProgress.setEnabled(enabled);
                bind.btnPlayerPlay.setEnabled(enabled);
                bind.btnMiniPlay.setEnabled(enabled);
                bind.btnNext.setEnabled(enabled);
                bind.btnPrevious.setEnabled(enabled);

                float alpha = enabled ? 1.0f : 0.5f;
                bind.btnPlayerPlay.setAlpha(alpha);
                bind.btnMiniPlay.setAlpha(alpha);
                bind.btnNext.setAlpha(alpha);
                bind.btnPrevious.setAlpha(alpha);
            }
        });

        playerViewModel.getIsLoadingTrack().observe(getViewLifecycleOwner(), isLoading -> {
            if (bind != null && isAdded()) {
                boolean loading = (isLoading != null && isLoading);
                bind.pbPlayerLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
                bind.ivPlayerAlbumArt.setAlpha(loading ? 0.3f : 1.0f);
                bind.btnPlayerPlay.setEnabled(!loading);

                Boolean hasControls = playerViewModel.getControlsEnabled().getValue();
                if (hasControls != null && hasControls) {
                    bind.btnPlayerPlay.setEnabled(!loading);
                    bind.btnMiniPlay.setEnabled(!loading);
                }

                if (loading) {
                    com.bumptech.glide.Glide.with(this).clear(bind.ivPlayerAlbumArt);
                    com.bumptech.glide.Glide.with(this).clear(bind.ivMiniAlbumArt);
                    bind.ivPlayerAlbumArt.setImageResource(R.drawable.img_album_placeholder);
                    bind.ivMiniAlbumArt.setImageResource(R.drawable.img_album_placeholder);
                }
            }
        });
    }

    private void setupButtons() {
        bind.layoutMiniPlayer.setOnClickListener(v -> expandPlayer());
        bind.btnMinimize.setOnClickListener(v -> collapsePlayer());

        View.OnClickListener togglePlayPause = v -> {
            if (spotifyManager.isReady() && isAdded()) {
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

        bind.seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && bind != null) {
                    bind.tvCurrentTime.setText(formatDuration(progress));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { isUserSeeking = true; }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
                playerViewModel.seekTo(seekBar.getProgress());
            }
        });

        bind.btnNext.setOnClickListener(v -> {
            if (spotifyManager.isReady() && isAdded()) {
                playerViewModel.setIsLoadingTrack(true);
                Boolean isHost = concertoViewModel.getIsHost().getValue();
                if (isHost != null && isHost) {
                    concertoViewModel.playNextTrack();
                } else {
                    spotifyManager.skipNext();
                }
            }
        });

        bind.btnPrevious.setOnClickListener(v -> {
            if (spotifyManager.isReady() && isAdded()) {
                Boolean isHost = concertoViewModel.getIsHost().getValue();
                if (isHost != null && isHost) {
                    // CONCERTO RULE: Always seek to 0 because there is no "previous" track in the shared queue
                    spotifyManager.seekTo(0);
                    playerViewModel.setIsLoadingTrack(false);
                } else {
                    // SOLO RULE: Let Spotify handle the skip (it naturally seeks to 0 if it's the first track)
                    playerViewModel.setIsLoadingTrack(true);
                    spotifyManager.skipPrevious();
                }
            }
        });
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private final Runnable progressUpdater = new Runnable() {
        @Override
        public void run() {
            if (bind != null && isAdded() && !isUserSeeking && spotifyManager != null && spotifyManager.isReady()) {
                spotifyManager.getPlayerState(playerState -> {
                    if (playerState != null && playerState.track != null && !playerState.isPaused && bind != null && isAdded()) {
                        bind.seekBarProgress.setMax((int) playerState.track.duration);
                        bind.seekBarProgress.setProgress((int) playerState.playbackPosition);
                        bind.tvCurrentTime.setText(formatDuration(playerState.playbackPosition));
                        bind.tvTotalTime.setText(formatDuration(playerState.track.duration));

                        long remainingMs = playerState.track.duration - playerState.playbackPosition;
                        if (remainingMs < 1500 && !isTransitioning) {
                            isTransitioning = true;
                            Boolean isHost = concertoViewModel.getIsHost().getValue();
                            if (isHost != null && isHost) {
                                concertoViewModel.playNextTrack();
                            }
                            progressHandler.postDelayed(() -> isTransitioning = false, 4000);
                        }
                    }
                });
            }
            progressHandler.postDelayed(this, 1000);
        }
    };

    private void subscribeToPlayerState() {
        spotifyManager.subscribeToPlayerState((track, isPaused) -> {
            if (bind == null || !isAdded()) return;

            if (concertoViewModel.getActiveSessionPin().getValue() == null) {
                playerViewModel.setControlsEnabled(true);
            }

            String currentPlaying = playerViewModel.getCurrentPlayingUri().getValue();
            boolean isNewTrack = (currentPlaying == null || !currentPlaying.equals(track.uri));

            playerViewModel.setCurrentPlayingUri(track.uri);
            playerViewModel.setIsCurrentlyPaused(isPaused);

            if (isNewTrack) {
                if (concertoViewModel.getActiveSessionPin().getValue() == null) {
                    String artistName = (track.artist != null && track.artist.name != null) ? track.artist.name : "Unknown Artist";
                    bind.tvMiniTrackName.setText(track.name);
                    bind.tvMiniArtistName.setText(artistName);
                    bind.tvTrackName.setText(track.name);
                    bind.tvArtistName.setText(artistName);

                    String currentUrl = playerViewModel.getDisplayImageUrl().getValue();
                    playerViewModel.setDisplayInfo(track.name, artistName, currentUrl);

                    if (track.imageUri != null) {
                        spotifyManager.loadImage(track.imageUri, bitmap -> {
                            if (bind != null && bitmap != null && isAdded()) {
                                bind.ivPlayerAlbumArt.setImageBitmap(bitmap);
                                bind.ivMiniAlbumArt.setImageBitmap(bitmap);
                            }
                            playerViewModel.setIsLoadingTrack(false);
                        });
                    } else {
                        playerViewModel.setIsLoadingTrack(false);
                    }
                } else {
                    playerViewModel.setIsLoadingTrack(false);
                }

                spotifyManager.getPlayerState(playerState -> {
                    if (playerState != null && playerState.track != null && bind != null && isAdded()) {
                        bind.seekBarProgress.setMax((int) playerState.track.duration);
                        bind.tvTotalTime.setText(formatDuration(playerState.track.duration));
                    }
                });
            } else {
                Boolean wasLoading = playerViewModel.getIsLoadingTrack().getValue();
                if (wasLoading != null && wasLoading) {
                    if (concertoViewModel.getActiveSessionPin().getValue() == null) {
                        if (track.imageUri != null) {
                            spotifyManager.loadImage(track.imageUri, bitmap -> {
                                if (bind != null && bitmap != null && isAdded()) {
                                    bind.ivPlayerAlbumArt.setImageBitmap(bitmap);
                                    bind.ivMiniAlbumArt.setImageBitmap(bitmap);
                                }
                                playerViewModel.setIsLoadingTrack(false);
                            });
                            return;
                        }
                    } else {
                        String url = playerViewModel.getDisplayImageUrl().getValue();
                        if (url != null && !url.isEmpty() && bind != null && isAdded()) {
                            com.bumptech.glide.Glide.with(this).load(url).into(bind.ivPlayerAlbumArt);
                            com.bumptech.glide.Glide.with(this).load(url).into(bind.ivMiniAlbumArt);
                        }
                    }
                }
                playerViewModel.setIsLoadingTrack(false);
            }

            int iconRes = isPaused ? R.drawable.ic_button_play : R.drawable.ic_button_pause;
            int iconResMini = isPaused ? R.drawable.ic_button_play_solid : R.drawable.ic_button_pause_solid;
            bind.btnPlayerPlay.setImageResource(iconRes);
            bind.btnMiniPlay.setImageResource(iconResMini);

            if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
    }

    private void expandPlayer() {
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private void collapsePlayer() {
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    private String formatDuration(long durationMs) {
        long seconds = (durationMs / 1000) % 60;
        long minutes = (durationMs / (1000 * 60)) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void startSmoothMiniPlayerAnimation() {
        if (getActivity() == null) return;

        int colorPrimary = ContextCompat.getColor(requireContext(), R.color.primary_translucent);
        int colorSecondary = ContextCompat.getColor(requireContext(), R.color.secondary_translucent);
        int colorDark = Color.parseColor("#A0121212");

        GradientDrawable smoothGradient = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{colorPrimary, colorDark, colorDark}
        );

        float radius = 8 * getResources().getDisplayMetrics().density;
        smoothGradient.setCornerRadius(radius);
        bind.viewMiniPlayerBg.setBackground(smoothGradient);
        bind.viewMiniPlayerBg.setClipToOutline(true);

        ValueAnimator colorAnimator = ValueAnimator.ofArgb(colorPrimary, colorSecondary);
        colorAnimator.setDuration(10000);
        colorAnimator.setRepeatCount(ValueAnimator.INFINITE);
        colorAnimator.setRepeatMode(ValueAnimator.REVERSE);

        colorAnimator.addUpdateListener(animator -> {
            if (bind == null || !isAdded()) {
                animator.cancel();
                return;
            }
            int animatedColor = (int) animator.getAnimatedValue();
            smoothGradient.setColors(new int[]{animatedColor, colorDark, colorDark});
        });

        colorAnimator.start();
    }
}