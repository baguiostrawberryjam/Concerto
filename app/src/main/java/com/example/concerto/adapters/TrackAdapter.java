package com.example.concerto.adapters;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.concerto.ConnectSpotifyFragment;
import com.example.concerto.PlayerFragment;
import com.example.concerto.R;
import com.spotify.protocol.types.Track;

import java.util.ArrayList;
import java.util.List;

import com.bumptech.glide.Glide;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {

    private List<Track> trackList = new ArrayList<>();
    private boolean canPlayMusic = false;

    // Method to update the list when the ViewModel gets new data
    public void setTracks(List<Track> tracks) {
        this.trackList = tracks;
        notifyDataSetChanged();
    }

    // Method to enable/disable the play buttons based on Spotify connection
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
                    .placeholder(android.R.drawable.ic_menu_gallery) // Shows while loading
                    .into(holder.ivAlbumCover);
        } else {
            holder.ivAlbumCover.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        if (canPlayMusic) {
            holder.btnPlay.setAlpha(1.0f);
        } else {
            holder.btnPlay.setAlpha(0.3f);
        }

        holder.itemView.setOnClickListener(v -> {
            AppCompatActivity activity = (AppCompatActivity) v.getContext();

            if (canPlayMusic) {
                Toast.makeText(v.getContext(), "Loading: " + track.name, Toast.LENGTH_SHORT).show();

                PlayerFragment playerFragment = new PlayerFragment();
                Bundle args = new Bundle();
                args.putString("TRACK_URI", track.uri);
                playerFragment.setArguments(args);

                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, playerFragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                Toast.makeText(v.getContext(), "Connect Spotify to play full tracks!", Toast.LENGTH_SHORT).show();
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ConnectSpotifyFragment())
                        .addToBackStack(null)
                        .commit();
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