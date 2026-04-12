package com.example.concerto.search;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.concerto.spotify.SpotifyRepository;
import com.spotify.protocol.types.Track;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchViewModel extends AndroidViewModel {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<Track>> searchResultsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);

    private final SpotifyRepository spotifyRepository;

    public SearchViewModel(@NonNull Application application) {
        super(application);
        this.spotifyRepository = SpotifyRepository.getInstance(application);
    }

    public LiveData<List<Track>> getSearchResults() { return searchResultsLiveData; }
    public LiveData<Boolean> getIsLoading() { return isLoadingLiveData; }

    public void loadSearchedSongs(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }

        isLoadingLiveData.setValue(true);

        executorService.execute(() -> {
            List<Track> songs = spotifyRepository.getSearchedSongs(query);
            searchResultsLiveData.postValue(songs);
            isLoadingLiveData.postValue(false);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}