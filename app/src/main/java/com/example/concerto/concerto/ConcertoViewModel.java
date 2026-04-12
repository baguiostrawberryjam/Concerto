package com.example.concerto.concerto;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.concerto.models.ConcertoTrack;

import java.util.List;

public class ConcertoViewModel extends ViewModel {

    private final ConcertoManager concertoManager = new ConcertoManager();

    private final MutableLiveData<String> activeSessionPin = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> isHost = new MutableLiveData<>(false);
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<List<ConcertoTrack>> queueLiveData = new MutableLiveData<>();
    private final MutableLiveData<ConcertoTrack> currentlyPlayingLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> sessionStatus = new MutableLiveData<>(null);

    public LiveData<String> getActiveSessionPin() { return activeSessionPin; }
    public LiveData<Boolean> getIsHost() { return isHost; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<List<ConcertoTrack>> getQueue() { return queueLiveData; }
    public LiveData<ConcertoTrack> getCurrentlyPlaying() { return currentlyPlayingLiveData; }
    public LiveData<String> getSessionStatus() { return sessionStatus; }

    public void createConcerto() {
        int randomPin = new java.util.Random().nextInt(9000) + 1000;
        String pin = String.valueOf(randomPin);

        concertoManager.createRoom(pin, new ConcertoManager.ActionCallback() {
            @Override
            public void onSuccess() {
                isHost.postValue(true);
                activeSessionPin.postValue(pin);
                sessionStatus.postValue("active");
                startObserving(pin);
            }

            @Override
            public void onError(String error) {
                if (error.contains("PIN already in use")) {
                    createConcerto();
                } else {
                    toastMessage.postValue(error);
                }
            }
        });
    }

    public void joinConcerto(String pin) {
        concertoManager.joinRoom(pin, new ConcertoManager.JoinCallback() {
            @Override
            public void onSuccess(boolean isUserTheHost) {
                isHost.postValue(isUserTheHost);
                activeSessionPin.postValue(pin);
                sessionStatus.postValue("active");
                // If host is rejoining after stepping out, reset status to active
                if (isUserTheHost) {
                    concertoManager.setRoomStatus(pin, "active");
                }
                startObserving(pin);
            }

            @Override
            public void onError(String error) {
                toastMessage.postValue(error);
            }
        });
    }

    public void joinHostedConcerto(String pin) {
        joinConcerto(pin);
    }

    private void startObserving(String pin) {
        concertoManager.startObservingRoom(pin,
                queue -> queueLiveData.postValue(queue),
                track -> currentlyPlayingLiveData.postValue(track),
                status -> sessionStatus.postValue(status)
        );
    }

    /**
     * Explicit end: host pressed "End Session" or guest pressed "Leave".
     * For host: marks room ended in Firebase then clears local state.
     * For guest: just stops observing and clears local state.
     */
    public void leaveConcerto() {
        String pin = activeSessionPin.getValue();
        Boolean hostStatus = isHost.getValue();

        if (pin != null) {
            if (hostStatus != null && hostStatus) {
                concertoManager.endRoom(pin);
            }
            concertoManager.stopObservingRoom(pin);
        }

        clearLocalSessionState();
    }

    /**
     * Host "Step Out": sets status to host_away so guests are notified and redirected,
     * stops Firebase listeners, but leaves room data intact so host can rejoin.
     * Also called by onCleared() so closing the app doesn't end the room.
     */
    public void disconnectWithoutEnding() {
        String pin = activeSessionPin.getValue();
        Boolean hostStatus = isHost.getValue();

        if (pin != null) {
            if (hostStatus != null && hostStatus) {
                // Notify guests that host has stepped out
                concertoManager.setRoomStatus(pin, "host_away");
            }
            concertoManager.stopObservingRoom(pin);
        }

        clearLocalSessionState();
    }

    private void clearLocalSessionState() {
        activeSessionPin.postValue(null);
        isHost.postValue(false);
        queueLiveData.postValue(null);
        currentlyPlayingLiveData.postValue(null);
    }

    public void toggleVoteForTrack(ConcertoTrack track) {
        String pin = activeSessionPin.getValue();
        if (pin != null && track != null) {
            concertoManager.toggleVoteForTrack(pin, track);
        }
    }

    public void playNextTrack() {
        String pin = activeSessionPin.getValue();
        Boolean hostStatus = isHost.getValue();
        List<ConcertoTrack> currentQueue = queueLiveData.getValue();
        if (hostStatus != null && hostStatus && pin != null) {
            concertoManager.playNextTrack(pin, currentQueue);
        }
    }

    public String getCurrentUserUid() {
        return concertoManager.getCurrentUserUid();
    }

    public void addTrackToQueue(com.spotify.protocol.types.Track track) {
        String pin = activeSessionPin.getValue();
        if (pin != null && track != null) {
            concertoManager.addTrackToQueue(pin, track, new ConcertoManager.ActionCallback() {
                @Override
                public void onSuccess() {
                    toastMessage.postValue("Track added to Concerto queue!");
                }

                @Override
                public void onError(String error) {
                    toastMessage.postValue(error);
                }
            });
        }
    }

    public void deleteTrackFromQueue(ConcertoTrack track) {
        String pin = activeSessionPin.getValue();
        Boolean hostStatus = isHost.getValue();
        if (pin != null && track != null && hostStatus != null && hostStatus) {
            concertoManager.deleteTrackFromQueue(pin, track, new ConcertoManager.ActionCallback() {
                @Override
                public void onSuccess() {}
                @Override
                public void onError(String error) {
                    toastMessage.postValue("Could not delete track: " + error);
                }
            });
        }
    }

    public void clearActiveSessionPin() {
        activeSessionPin.postValue(null);
    }

    public void clearToastMessage() {
        toastMessage.postValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // App closed — stop listeners but don't end the room
        disconnectWithoutEnding();
    }
}