package com.example.concerto.concerto;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.concerto.R;
import com.example.concerto.adapters.ConcertoQueueAdapter;
import com.example.concerto.databinding.FragmentConcertoBinding;
import com.example.concerto.models.ConcertoTrack;
import com.example.concerto.player.PlayerViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class ConcertoFragment extends Fragment {

    private FragmentConcertoBinding bind;
    private ConcertoViewModel concertoViewModel;
    private ConcertoQueueAdapter queueAdapter;
    private PlayerViewModel playerViewModel;
    private String currentUid;

    // Track last displayed URI to avoid repeat "Now Playing" toasts
    private String lastNowPlayingUri = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        bind = FragmentConcertoBinding.inflate(inflater, container, false);
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViewModels();
        currentUid = concertoViewModel.getCurrentUserUid();
        setupRecyclerView();
        setupSwipeToDelete();
        setupButtons();
        setupObservers();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }

    // ==========================================
    // INIT
    // ==========================================

    private void initViewModels() {
        if (getActivity() == null) return;
        concertoViewModel = new ViewModelProvider(requireActivity()).get(ConcertoViewModel.class);
        playerViewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);
    }

    private void setupRecyclerView() {
        queueAdapter = new ConcertoQueueAdapter(currentUid, new ConcertoQueueAdapter.ConcertoTrackListener() {
            @Override
            public void onVoteClick(ConcertoTrack track) {
                concertoViewModel.toggleVoteForTrack(track);
            }

            @Override
            public void onTrackClick(ConcertoTrack track) {
                Boolean isHostVal = concertoViewModel.getIsHost().getValue();
                String currentPlayingUri = playerViewModel.getCurrentPlayingUri().getValue();

                if (isHostVal != null && isHostVal) {
                    // Host: expand if this is the currently playing track (URI known via Remote SDK)
                    if (currentPlayingUri != null && currentPlayingUri.equals(track.uri)) {
                        playerViewModel.expandPlayer();
                    }
                } else {
                    // Guest: compare against Firebase currentlyPlaying (no Remote SDK)
                    ConcertoTrack playing = concertoViewModel.getCurrentlyPlaying().getValue();
                    if (playing != null && track.uri != null && track.uri.equals(playing.uri)) {
                        playerViewModel.expandPlayer();
                    }
                }
            }
        });

        bind.rvConcertoQueue.setLayoutManager(new LinearLayoutManager(requireContext()));
        bind.rvConcertoQueue.setAdapter(queueAdapter);
    }

    // ==========================================
    // SWIPE TO DELETE — HOST ONLY
    // ==========================================

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull androidx.recyclerview.widget.RecyclerView rv,
                                  @NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder vh,
                                  @NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder,
                                 int direction) {
                Boolean isHostVal = concertoViewModel.getIsHost().getValue();
                int position = viewHolder.getAdapterPosition();

                // Guest: snap back immediately, no action
                if (isHostVal == null || !isHostVal) {
                    queueAdapter.notifyItemChanged(position);
                    return;
                }

                ConcertoTrack track = queueAdapter.getTrackAt(position);

                if (track != null && track.uri != null) {
                    concertoViewModel.deleteTrackFromQueue(track);
                    Toast.makeText(requireContext(), "Track removed.", Toast.LENGTH_SHORT).show();
                } else {
                    queueAdapter.notifyItemChanged(position);
                }
            }

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c,
                                    @NonNull androidx.recyclerview.widget.RecyclerView rv,
                                    @NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder vh,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                Boolean isHostVal = concertoViewModel.getIsHost().getValue();
                // Only show red swipe background for host
                if (isHostVal != null && isHostVal && isCurrentlyActive) {
                    android.graphics.Paint paint = new android.graphics.Paint();
                    paint.setColor(android.graphics.Color.parseColor("#CC3333"));
                    android.view.View itemView = vh.itemView;
                    android.graphics.drawable.Drawable icon = androidx.core.content.ContextCompat
                            .getDrawable(requireContext(), android.R.drawable.ic_menu_delete);

                    if (dX < 0) {
                        c.drawRect(itemView.getRight() + dX, itemView.getTop(),
                                itemView.getRight(), itemView.getBottom(), paint);
                        if (icon != null) {
                            int m = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                            icon.setBounds(itemView.getRight() - m - icon.getIntrinsicWidth(),
                                    itemView.getTop() + m,
                                    itemView.getRight() - m,
                                    itemView.getTop() + m + icon.getIntrinsicHeight());
                            icon.setTint(android.graphics.Color.WHITE);
                            icon.draw(c);
                        }
                    } else if (dX > 0) {
                        c.drawRect(itemView.getLeft(), itemView.getTop(),
                                itemView.getLeft() + dX, itemView.getBottom(), paint);
                        if (icon != null) {
                            int m = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                            icon.setBounds(itemView.getLeft() + m, itemView.getTop() + m,
                                    itemView.getLeft() + m + icon.getIntrinsicWidth(),
                                    itemView.getTop() + m + icon.getIntrinsicHeight());
                            icon.setTint(android.graphics.Color.WHITE);
                            icon.draw(c);
                        }
                    }
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive);
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(bind.rvConcertoQueue);
    }

    // ==========================================
    // BUTTONS
    // ==========================================

    private void setupButtons() {
        // End Session (host) / Leave (guest)
        bind.btnLeaveSession.setOnClickListener(v -> {
            concertoViewModel.leaveConcerto();
            if (isAdded() && getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutFragmentContainer, new ConcertoLobbyFragment())
                        .commit();
            }
        });

        // Host: step out temporarily without ending the room
        bind.btnTempLeave.setOnClickListener(v -> {
            concertoViewModel.disconnectWithoutEnding();
            if (isAdded() && getActivity() != null) {
                BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNav);
                if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_home);
            }
        });

        bind.btnAddSong.setOnClickListener(v -> {
            if (getActivity() != null) {
                BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNav);
                if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_search);
            }
        });

        bind.btnHostSkip.setOnClickListener(v -> concertoViewModel.playNextTrack());
    }

    // ==========================================
    // OBSERVERS
    // ==========================================

    private void setupObservers() {
        concertoViewModel.getActiveSessionPin().observe(getViewLifecycleOwner(), pin -> {
            if (pin != null && bind != null) {
                bind.tvSessionPin.setText("Join Code: " + pin);
            }
        });

        concertoViewModel.getIsHost().observe(getViewLifecycleOwner(), isHost -> {
            if (isHost == null || bind == null) return;
            bind.btnHostSkip.setVisibility(isHost ? View.VISIBLE : View.GONE);
            bind.btnTempLeave.setVisibility(isHost ? View.VISIBLE : View.GONE);
            // btnLeaveSession is an ImageButton — tint it red for host to signal End Session
            bind.btnLeaveSession.setColorFilter(isHost
                    ? android.graphics.Color.parseColor("#FF5555")
                    : android.graphics.Color.parseColor("#FFFFFF"));

            // Pulse dots only for guests
            bind.layoutListeningDots.setVisibility(isHost ? View.GONE : View.VISIBLE);
            if (!isHost) startListeningDotsAnimation();
        });

        playerViewModel.getCurrentPlayingUri().observe(getViewLifecycleOwner(), uri -> {
            if (queueAdapter != null) queueAdapter.setCurrentPlayingUri(uri != null ? uri : "");
        });

        // Queue — only tracks NOT currently playing (currently playing shown in card above)
        concertoViewModel.getQueue().observe(getViewLifecycleOwner(), queue -> {
            queueAdapter.setQueue(queue != null ? queue : new ArrayList<>());
        });

        concertoViewModel.getCurrentlyPlaying().observe(getViewLifecycleOwner(), track -> {
            Boolean isHostVal = concertoViewModel.getIsHost().getValue();

            if (track != null && track.uri != null) {
                // Show unified Now Playing card for EVERYONE
                bind.cardNowListening.setVisibility(View.VISIBLE);
                updateNowPlayingCard(track);

                // Toast on track change for guests
                if (isHostVal == null || !isHostVal) {
                    if (!track.uri.equals(lastNowPlayingUri)) {
                        lastNowPlayingUri = track.uri;
                        if (isAdded()) {
                            Toast.makeText(requireContext(),
                                    "Now Playing: " + track.name, Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    lastNowPlayingUri = track.uri;
                }

                // Push display info to PlayerViewModel for mini-player + full player
                playerViewModel.setDisplayInfo(
                        track.name,
                        track.artist != null ? track.artist : "",
                        track.imageUrl != null ? track.imageUrl : "");

                if (isHostVal != null && isHostVal) {
                    // Host drives playback
                    playerViewModel.setControlsEnabled(true);
                    String currentUri = playerViewModel.getCurrentPlayingUri().getValue();
                    if (!track.uri.equals(currentUri)) {
                        playerViewModel.playTrack(track.uri);
                    }
                } else {
                    // Guest: view only
                    playerViewModel.setControlsEnabled(false);
                }

            } else {
                // Nothing playing
                bind.cardNowListening.setVisibility(View.GONE);
                lastNowPlayingUri = null;
                playerViewModel.setDisplayInfo("Waiting for music...", "Join the queue", "");
                playerViewModel.setControlsEnabled(false);
                if (isHostVal != null && isHostVal) {
                    playerViewModel.pausePlayer();
                }
            }
        });

        // Guest: host temporarily left — null concerto, kick guests out safely
        concertoViewModel.getSessionStatus().observe(getViewLifecycleOwner(), status -> {
            Boolean isHostVal = concertoViewModel.getIsHost().getValue();
            if (!isAdded() || getActivity() == null) return;

            if ("ended".equals(status) && (isHostVal == null || !isHostVal)) {
                Toast.makeText(requireContext(),
                        "The host ended the session.", Toast.LENGTH_LONG).show();
                concertoViewModel.leaveConcerto();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutFragmentContainer, new ConcertoLobbyFragment())
                        .commitAllowingStateLoss();
            } else if ("host_away".equals(status) && (isHostVal == null || !isHostVal)) {
                Toast.makeText(requireContext(),
                        "The host has stepped out. You will be returned to the lobby.",
                        Toast.LENGTH_LONG).show();
                concertoViewModel.leaveConcerto();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutFragmentContainer, new ConcertoLobbyFragment())
                        .commitAllowingStateLoss();
            }
        });

        concertoViewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty() && isAdded()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                concertoViewModel.clearToastMessage();
            }
        });
    }

    // ==========================================
    // NOW PLAYING CARD — shared by host and guest
    // ==========================================

    private void updateNowPlayingCard(ConcertoTrack track) {
        if (bind == null || track == null) return;
        bind.tvNowPlayingTrack.setText(track.name);
        bind.tvNowPlayingArtist.setText(track.artist != null ? track.artist : "");
        if (track.imageUrl != null && !track.imageUrl.isEmpty() && isAdded()) {
            Glide.with(this).load(track.imageUrl)
                    .placeholder(R.drawable.img_album_placeholder)
                    .into(bind.ivNowPlayingArt);
        } else {
            bind.ivNowPlayingArt.setImageResource(R.drawable.img_album_placeholder);
        }
    }

    private void startListeningDotsAnimation() {
        if (bind == null) return;
        View[] dots = {bind.dot1, bind.dot2, bind.dot3};
        for (int i = 0; i < dots.length; i++) {
            ObjectAnimator anim = ObjectAnimator.ofFloat(dots[i], "alpha", 0.2f, 1.0f);
            anim.setDuration(600);
            anim.setStartDelay(i * 200L);
            anim.setRepeatCount(ObjectAnimator.INFINITE);
            anim.setRepeatMode(ObjectAnimator.REVERSE);
            anim.setInterpolator(new AccelerateDecelerateInterpolator());
            anim.start();
        }
    }
}