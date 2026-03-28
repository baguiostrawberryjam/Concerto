package com.example.concerto.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AuthViewModel extends ViewModel {

    private final MutableLiveData<String> spotifyToken = new MutableLiveData<>();

    public void setSpotifyToken(String token) {
        spotifyToken.postValue(token);
    }
    public LiveData<String> getSpotifyToken() {
        return spotifyToken;
    }

    private final MutableLiveData<String> codeVerifier = new MutableLiveData<>();

    public void setCodeVerifier(String verifier) {
        codeVerifier.setValue(verifier);
    }

    public String getCodeVerifier() {
        return codeVerifier.getValue();
    }
}