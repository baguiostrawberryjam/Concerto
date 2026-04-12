package com.example.concerto.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.concerto.R;
import com.example.concerto.models.ConcertoTrack;

import java.util.ArrayList;
import java.util.List;

public class ConcertoQueueAdapter extends RecyclerView.Adapter<ConcertoQueueAdapter.QueueViewHolder> {

    private List<ConcertoTrack> queueList = new ArrayList<>();
    private final String currentUserUid;
    private String currentPlayingUri = "";

    private final ConcertoTrackListener listener;

    public interface ConcertoTrackListener {
        void onVoteClick(ConcertoTrack track);
        void onTrackClick(ConcertoTrack track);
    }

    // Called by ItemTouchHelper swipe — only wired up for host
    public ConcertoTrack getTrackAt(int position) {
        if (position >= 0 && position < queueList.size()) {
            return queueList.get(position);
        }
        return null;
    }

    public ConcertoQueueAdapter(String currentUserUid, ConcertoTrackListener listener) {
        this.currentUserUid = currentUserUid != null ? currentUserUid : "";
        this.listener = listener;
    }

    public void setQueue(List<ConcertoTrack> newQueue) {
        this.queueList = newQueue != null ? newQueue : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setCurrentPlayingUri(String uri) {
        this.currentPlayingUri = uri != null ? uri : "";
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_track, parent, false);
        return new QueueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        ConcertoTrack track = queueList.get(position);
        boolean isPlaying = track.uri != null && !track.uri.isEmpty()
                && currentPlayingUri != null && track.uri.equals(currentPlayingUri);

        // Track name & artist
        holder.tvTrackName.setText(track.name);
        holder.tvTrackName.setTextColor(isPlaying
                ? android.graphics.Color.parseColor("#1DB954")
                : android.graphics.Color.parseColor("#FFFFFF"));
        holder.tvTrackName.setTypeface(null, isPlaying
                ? android.graphics.Typeface.BOLD
                : android.graphics.Typeface.NORMAL);
        holder.tvArtistName.setText(track.artist);

        // Only show vote area for tracks with a valid URI
        if (track.uri == null || track.uri.trim().isEmpty()) {
            holder.layoutVoteArea.setVisibility(View.GONE);
            holder.btnPlay.setOnClickListener(null);
            holder.itemView.setOnClickListener(null);
            return; // Skip binding the rest for corrupt entries
        }

        holder.layoutVoteArea.setVisibility(View.VISIBLE);

        if (isPlaying) {
            // Currently playing — hide vote button, still show vote count
            holder.btnPlay.setVisibility(View.INVISIBLE);
        } else {
            holder.btnPlay.setVisibility(View.VISIBLE);
            holder.btnPlay.setImageResource(R.drawable.ic_button_upvote);

            boolean hasVoted = track.voters != null && track.voters.containsKey(currentUserUid);
            if (hasVoted) {
                holder.btnPlay.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_green_dark));
                holder.btnPlay.setAlpha(1.0f);
            } else {
                holder.btnPlay.clearColorFilter();
                holder.btnPlay.setAlpha(0.5f);
            }
        }

        // FEATURE: Show vote count
        int voteCount = track.voters != null ? track.voters.size() : 0;
        holder.tvVoteCount.setText(String.valueOf(voteCount));
        // Highlight count green if this is a popular track (3+ votes) or user voted
        boolean hasVoted = track.voters != null && track.voters.containsKey(currentUserUid);
        holder.tvVoteCount.setTextColor(hasVoted
                ? android.graphics.Color.parseColor("#1DB954")
                : android.graphics.Color.parseColor("#AAAAAA"));

        holder.btnPlay.setOnClickListener(v -> {
            if (listener != null) listener.onVoteClick(track);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTrackClick(track);
        });

        if (track.imageUrl != null && !track.imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(track.imageUrl)
                    .placeholder(R.drawable.img_album_placeholder)
                    .into(holder.ivAlbumCover);
        } else {
            holder.ivAlbumCover.setImageResource(R.drawable.img_album_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return queueList.size();
    }

    static class QueueViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAlbumCover;
        TextView tvTrackName;
        TextView tvArtistName;
        ImageButton btnPlay;
        TextView tvVoteCount;
        LinearLayout layoutVoteArea;

        public QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAlbumCover = itemView.findViewById(R.id.ivAlbumCover);
            tvTrackName = itemView.findViewById(R.id.tvTrackName);
            tvArtistName = itemView.findViewById(R.id.tvArtistName);
            btnPlay = itemView.findViewById(R.id.btnPlay);
            tvVoteCount = itemView.findViewById(R.id.tvVoteCount);
            layoutVoteArea = itemView.findViewById(R.id.layoutVoteArea);
        }
    }
}