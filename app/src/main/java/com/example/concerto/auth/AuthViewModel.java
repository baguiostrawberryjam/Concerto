package com.example.concerto.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.concerto.spotify.SpotifyAppTokenManager;

public class AuthViewModel extends AndroidViewModel {

    private final MutableLiveData<String> spotifyToken = new MutableLiveData<>();
    private final SpotifyAppTokenManager tokenManager;
    private final MutableLiveData<String> codeVerifier = new MutableLiveData<>();

    private final AuthManager authManager;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        tokenManager = SpotifyAppTokenManager.getInstance(application);

        authManager = new AuthManager(tokenManager);
    }

    public interface LoginCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public interface SignupCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public String getCurrentUsername() {
        return authManager.getCurrentUsername();
    }

    public void setSpotifyToken(String token) {
        spotifyToken.postValue(token);
        tokenManager.setUserToken(token); // Update memory cache ONLY!
    }

    public LiveData<String> getSpotifyToken() {
        return spotifyToken;
    }

    public void setCodeVerifier(String verifier) {
        codeVerifier.setValue(verifier);
    }

    public String getCodeVerifier() {
        return codeVerifier.getValue();
    }

    public void login(String email, String password, LoginCallback callback) {
        authManager.loginWithEmailAndPassword(email, password, new AuthManager.LoginListener() {
            @Override
            public void onSuccess() {
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onError(String errorMessage) {
                if (callback != null) callback.onError(errorMessage);
            }
        });
    }

    public void signup(String email, String password, String username, SignupCallback callback) {
        authManager.signupWithEmailAndPassword(email, password, username, new AuthManager.SignupListener() {
            @Override
            public void onSuccess() {
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onError(String errorMessage) {
                if (callback != null) callback.onError(errorMessage);
            }
        });
    }

    public void logoutFull() {
        authManager.logoutFirebase();
        logoutSpotify(); // We call the existing Spotify logout method here!
    }

    public void logoutSpotify() {
        spotifyToken.postValue(null);
        tokenManager.clearUserToken();
    }

    public void restoreSpotifyTokenFromFirebase(Runnable onComplete) {
        authManager.restoreSpotifyTokenFromFirebase(new AuthManager.TokenRestoreListener() {
            @Override
            public void onSuccess(String token) {
                setSpotifyToken(token);
            }

            @Override
            public void onComplete() {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }
}