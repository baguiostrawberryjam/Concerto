package com.example.concerto.player;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PlayerViewModel extends ViewModel {

    // Command States
    private final MutableLiveData<String> commandPlayUri = new MutableLiveData<>();
    private final MutableLiveData<Boolean> commandExpand = new MutableLiveData<>();
    private final MutableLiveData<Boolean> commandPause = new MutableLiveData<>();
    private final MutableLiveData<Long> commandSeekTo = new MutableLiveData<>();

    // Playback States
    private final MutableLiveData<String> currentPlayingUri = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isCurrentlyPaused = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLoadingTrack = new MutableLiveData<>(false);

    // Display UI States (For Listeners / UI Syncing)
    private final MutableLiveData<String> displayTrackName = new MutableLiveData<>("Not Playing");
    private final MutableLiveData<String> displayArtistName = new MutableLiveData<>("Tap a song to play");
    private final MutableLiveData<String> displayImageUrl = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> controlsEnabled = new MutableLiveData<>(false);

    // --- Actions ---
    public void playTrack(String uri) {
        if (uri != null && uri.equals(currentPlayingUri.getValue())) {
            expandPlayer(); // Just slide the player up if already playing
            return;
        }
        isLoadingTrack.postValue(true);
        commandPlayUri.postValue(uri);
    }

    public void expandPlayer() { commandExpand.postValue(true); }
    public void pausePlayer() { commandPause.postValue(true); }
    public void seekTo(long positionMs) { commandSeekTo.postValue(positionMs); }

    public void setCurrentPlayingUri(String uri) { currentPlayingUri.postValue(uri); }
    public void setIsCurrentlyPaused(boolean isPaused) { isCurrentlyPaused.postValue(isPaused); }
    public void setIsLoadingTrack(boolean isLoading) { isLoadingTrack.postValue(isLoading); }

    public void setDisplayInfo(String name, String artist, String imageUrl) {
        displayTrackName.postValue(name);
        displayArtistName.postValue(artist);
        displayImageUrl.postValue(imageUrl);
    }

    public void setControlsEnabled(boolean enabled) {
        controlsEnabled.postValue(enabled);
    }

    public void reset() {
        commandPlayUri.postValue(null);
        commandExpand.postValue(false);
        commandPause.postValue(false);
        commandSeekTo.postValue(null);
        currentPlayingUri.postValue("");
        isCurrentlyPaused.postValue(false);
        displayTrackName.postValue("Not Playing");
        displayArtistName.postValue("Tap a song to play");
        displayImageUrl.postValue("");
        controlsEnabled.postValue(false);
        isLoadingTrack.postValue(false);
    }

    // --- Getters ---
    public LiveData<String> getCommandPlayUri() { return commandPlayUri; }
    public LiveData<Boolean> getCommandExpand() { return commandExpand; }
    public LiveData<Boolean> getCommandPause() { return commandPause; }
    public LiveData<Long> getCommandSeekTo() { return commandSeekTo; }

    public LiveData<String> getCurrentPlayingUri() { return currentPlayingUri; }
    public LiveData<Boolean> getIsCurrentlyPaused() { return isCurrentlyPaused; }
    public LiveData<Boolean> getIsLoadingTrack() { return isLoadingTrack; }

    public LiveData<String> getDisplayTrackName() { return displayTrackName; }
    public LiveData<String> getDisplayArtistName() { return displayArtistName; }
    public LiveData<String> getDisplayImageUrl() { return displayImageUrl; }
    public LiveData<Boolean> getControlsEnabled() { return controlsEnabled; }
}