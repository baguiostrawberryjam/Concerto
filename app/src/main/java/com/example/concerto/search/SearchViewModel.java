package com.example.concerto.search;

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

public class SearchViewModel extends AndroidViewModel {

    // Dedicated thread for background search queries
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // State Holders (LiveData)
    private final MutableLiveData<List<Track>> searchResultsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> canPlayMusicLiveData = new MutableLiveData<>(false);

    private final SpotifyRepository spotifyRepository;
    private final SpotifyAppTokenManager tokenManager;

    public SearchViewModel(@NonNull Application application) {
        super(application);
        this.spotifyRepository = SpotifyRepository.getInstance(application);
        this.tokenManager = SpotifyAppTokenManager.getInstance(application);

        // Initialize play permission immediately
        checkPlayPermission();
    }

    public LiveData<List<Track>> getSearchResults() { return searchResultsLiveData; }
    public LiveData<Boolean> getIsLoading() { return isLoadingLiveData; }
    public LiveData<Boolean> getCanPlayMusic() { return canPlayMusicLiveData; }

    public void loadSearchedSongs(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }

        isLoadingLiveData.setValue(true); // Tell UI to show progress bar

        executorService.execute(() -> {
            List<Track> songs = spotifyRepository.getSearchedSongs(query);

            searchResultsLiveData.postValue(songs);
            isLoadingLiveData.postValue(false); // Tell UI to hide progress bar
        });
    }

    public void checkPlayPermission() {
        canPlayMusicLiveData.setValue(tokenManager.hasUserToken());
    }
}