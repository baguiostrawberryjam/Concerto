package com.example.concerto.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.concerto.R;
import com.spotify.protocol.types.Track;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {

    private List<Track> trackList = new ArrayList<>();
    private boolean canPlayMusic = false;

    // 1. Create the Walkie-Talkie (Interface)
    private final OnTrackClickListener listener;

    public interface OnTrackClickListener {
        void onTrackClick(Track track, boolean canPlay);
    }

    // 2. Require the listener when the Adapter is created
    public TrackAdapter(OnTrackClickListener listener) {
        this.listener = listener;
    }

    public void setTracks(List<Track> tracks) {
        this.trackList = tracks;
        notifyDataSetChanged();
    }

    public void setCanPlayMusic(boolean canPlay) {
        this.canPlayMusic = canPlay;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        Track track = trackList.get(position);

        holder.tvTrackName.setText(track.name);

        String artistName = (track.artist != null && track.artist.name != null)
                ? track.artist.name
                : "Unknown Artist";
        holder.tvArtistName.setText(artistName);

        if (track.imageUri != null && track.imageUri.raw != null && !track.imageUri.raw.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(track.imageUri.raw)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivAlbumCover);
        } else {
            holder.ivAlbumCover.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.btnPlay.setAlpha(canPlayMusic ? 1.0f : 0.3f);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTrackClick(track, canPlayMusic);
            }
        });
    }

    @Override
    public int getItemCount() {
        return trackList != null ? trackList.size() : 0;
    }

    static class TrackViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAlbumCover;
        TextView tvTrackName;
        TextView tvArtistName;
        ImageButton btnPlay;

        public TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAlbumCover = itemView.findViewById(R.id.ivAlbumCover);
            tvTrackName = itemView.findViewById(R.id.tvTrackName);
            tvArtistName = itemView.findViewById(R.id.tvArtistName);
            btnPlay = itemView.findViewById(R.id.btnPlay);
        }
    }
}