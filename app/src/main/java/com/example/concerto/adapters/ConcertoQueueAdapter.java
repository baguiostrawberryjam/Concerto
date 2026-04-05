package com.example.concerto.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
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

    public ConcertoQueueAdapter(String currentUserUid, ConcertoTrackListener listener) {
        this.currentUserUid = currentUserUid != null ? currentUserUid : "";
        this.listener = listener;
    }

    // FIXED: Null-safe setter
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

        boolean isPlaying = track.uri.equals(currentPlayingUri);
        holder.tvTrackName.setText(track.name);
        holder.tvTrackName.setTextColor(isPlaying ? android.graphics.Color.parseColor("#1DB954") : android.graphics.Color.parseColor("#FFFFFF"));
        holder.tvTrackName.setTypeface(null, isPlaying ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        holder.tvArtistName.setText(track.artist);

        if (isPlaying) {
            holder.btnPlay.setVisibility(View.INVISIBLE);
        } else {
            holder.btnPlay.setVisibility(View.VISIBLE);
            holder.btnPlay.setImageResource(android.R.drawable.arrow_up_float);

            boolean hasVoted = track.voters != null && track.voters.containsKey(currentUserUid);
            if (hasVoted) {
                holder.btnPlay.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_green_dark));
                holder.btnPlay.setAlpha(1.0f);
            } else {
                holder.btnPlay.clearColorFilter();
                holder.btnPlay.setAlpha(0.5f);
            }
        }

        holder.btnPlay.setOnClickListener(v -> {
            if (listener != null) listener.onVoteClick(track);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTrackClick(track);
        });

        // Uses safe itemView.getContext()
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

        public QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAlbumCover = itemView.findViewById(R.id.ivAlbumCover);
            tvTrackName = itemView.findViewById(R.id.tvTrackName);
            tvArtistName = itemView.findViewById(R.id.tvArtistName);
            btnPlay = itemView.findViewById(R.id.btnPlay);
        }
    }
}