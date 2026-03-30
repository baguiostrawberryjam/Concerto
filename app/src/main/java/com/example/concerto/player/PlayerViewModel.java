package com.example.concerto.player;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PlayerViewModel extends ViewModel {

    private final MutableLiveData<String> commandPlayUri = new MutableLiveData<>();
    private final MutableLiveData<Boolean> commandExpand = new MutableLiveData<>();
    private final MutableLiveData<Boolean> commandPause = new MutableLiveData<>();

    private final MutableLiveData<String> currentPlayingUri = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isCurrentlyPaused = new MutableLiveData<>(false);

    public void playTrack(String uri) { commandPlayUri.setValue(uri); }
    public LiveData<String> getCommandPlayUri() { return commandPlayUri; }

    public void expandPlayer() { commandExpand.setValue(true); }
    public LiveData<Boolean> getCommandExpand() { return commandExpand; }

    public void pausePlayer() { commandPause.setValue(true); }
    public LiveData<Boolean> getCommandPause() { return commandPause; }

    public void setCurrentPlayingUri(String uri) { currentPlayingUri.setValue(uri); }
    public void setIsCurrentlyPaused(boolean isPaused) { isCurrentlyPaused.setValue(isPaused); }

    public LiveData<String> getCurrentPlayingUri() { return currentPlayingUri; }
    public LiveData<Boolean> getIsCurrentlyPaused() { return isCurrentlyPaused; }
}