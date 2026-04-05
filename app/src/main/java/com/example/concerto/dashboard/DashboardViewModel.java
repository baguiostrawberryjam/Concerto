package com.example.concerto.dashboard;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.concerto.spotify.SpotifyAppTokenManager;
import com.example.concerto.spotify.SpotifyRepository;
import com.spotify.protocol.types.Track;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardViewModel extends AndroidViewModel {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<Track>> songsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> canPlayMusicLiveData = new MutableLiveData<>();

    private final SpotifyRepository spotifyRepository;
    private final SpotifyAppTokenManager tokenManager;

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        this.spotifyRepository = SpotifyRepository.getInstance(application);
        this.tokenManager = SpotifyAppTokenManager.getInstance(application);
    }

    public LiveData<List<Track>> getSongs() { return songsLiveData; }
    public LiveData<Boolean> getCanPlayMusic() { return canPlayMusicLiveData; }

    public void loadRandomSongs() {
        executorService.execute(() -> {
            List<Track> songs = spotifyRepository.getRandomSongs();
            boolean canPlay = canPlayMusic();

            songsLiveData.postValue(songs);
            canPlayMusicLiveData.postValue(canPlay);
        });
    }

    public boolean canPlayMusic() {
        return tokenManager.hasUserToken();
    }

    // --- FIXED: Prevent Memory Leaks by shutting down the background thread ---
    @Override
    protected void onCleared() {
        super.onCleared();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}