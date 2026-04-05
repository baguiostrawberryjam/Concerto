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
    private String currentPlayingUri = "";

    private final OnTrackClickListener listener;

    public interface OnTrackClickListener {
        void onTrackClick(Track track, boolean canPlay);
    }

    public TrackAdapter(OnTrackClickListener listener) {
        this.listener = listener;
    }

    // FIXED: Null-safe setter prevents crashes if ViewModel passes null
    public void setTracks(List<Track> tracks) {
        this.trackList = tracks != null ? tracks : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setCanPlayMusic(boolean canPlay) {
        this.canPlayMusic = canPlay;
        notifyDataSetChanged();
    }

    public void setCurrentPlayingUri(String uri) {
        this.currentPlayingUri = uri != null ? uri : "";
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

        boolean isPlaying = track.uri.equals(currentPlayingUri);
        holder.tvTrackName.setText(track.name);
        holder.tvTrackName.setTextColor(isPlaying ? android.graphics.Color.parseColor("#1DB954") : android.graphics.Color.parseColor("#FFFFFF"));
        holder.tvTrackName.setTypeface(null, isPlaying ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        String artistName = (track.artist != null && track.artist.name != null)
                ? track.artist.name
                : "Unknown Artist";
        holder.tvArtistName.setText(artistName);

        // Uses safe itemView.getContext() to prevent memory leaks!
        if (track.imageUri != null && track.imageUri.raw != null && !track.imageUri.raw.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(track.imageUri.raw)
                    .placeholder(R.drawable.img_album_placeholder)
                    .into(holder.ivAlbumCover);
        } else {
            holder.ivAlbumCover.setImageResource(R.drawable.img_album_placeholder);
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
        return trackList.size();
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