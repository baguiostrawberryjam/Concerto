package com.example.concerto.auth;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.concerto.spotify.SpotifyAppTokenManager;
import com.example.concerto.spotify.SpotifyRepository;

public class AuthViewModel extends AndroidViewModel {

    // State Holders (LiveData)
    private final MutableLiveData<String> spotifyToken = new MutableLiveData<>();
    private final MutableLiveData<String> codeVerifier = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isPremiumUser = new MutableLiveData<>(false);

    // Managers
    private final SpotifyAppTokenManager tokenManager;
    private final AuthManager authManager;
    private final SpotifyRepository spotifyRepository;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        tokenManager = SpotifyAppTokenManager.getInstance(application);
        authManager = new AuthManager(tokenManager);
        spotifyRepository = SpotifyRepository.getInstance(application); // Added
    }

    // --- Interfaces ---
    public interface LoginCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public interface SignupCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    // --- Getters & Setters ---
    public String getCurrentUsername() {
        return authManager.getCurrentUsername();
    }

    public LiveData<String> getSpotifyToken() {
        return spotifyToken;
    }

    public void setSpotifyToken(String token) {
        spotifyToken.postValue(token);
        tokenManager.setUserToken(token);

        // --- THE FIX: Automatically verify Premium status ---
        if (token != null && !token.trim().isEmpty()) {
            spotifyRepository.checkPremiumStatus(token, isPremium -> {
                isPremiumUser.postValue(isPremium);
            });
        } else {
            isPremiumUser.postValue(false);
        }
    }

    public void setCodeVerifier(String verifier) {
        // FIXED: Always use postValue for thread safety
        codeVerifier.postValue(verifier);
    }

    public String getCodeVerifier() {
        return codeVerifier.getValue();
    }

    public boolean isUserLoggedIn() {
        return authManager.isUserLoggedIn();
    }

    public LiveData<Boolean> getIsPremiumUser() {
        return isPremiumUser;
    }

    // --- Core Auth Actions ---
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

    // --- Logout Handling ---
    public void logoutFull() {
        authManager.logoutFirebase();
        logoutSpotify();

        // FIXED: Wipe volatile state to prevent ghost data for the next user
        codeVerifier.postValue(null);
    }

    public void logoutSpotify() {
        spotifyToken.postValue(null);
        tokenManager.clearUserToken();
    }

    // --- Spotify Token Management ---
    public void restoreSpotifyTokenFromFirebase(Runnable onComplete) {
        authManager.restoreSpotifyTokenFromFirebase(new AuthManager.TokenRestoreListener() {

            // We use an array so it can be mutated safely inside the inner class callbacks
            final boolean[] tokenReceived = {false};

            @Override
            public void onSuccess(String token) {
                tokenReceived[0] = true;
                setSpotifyToken(token);
            }

            @Override
            public void onComplete() {
                if (onComplete != null) {
                    // If no token was found/restored, explicitly set an empty string to trigger observers
                    if (!tokenReceived[0]) {
                        setSpotifyToken("");
                    }
                    onComplete.run();
                }
            }
        });
    }

    public void exchangeCodeForToken(String code, String verifier) {
        authManager.tradeCodeForToken(code, verifier, new AuthManager.TokenExchangeListener() {
            @Override
            public void onSuccess(String accessToken) {
                setSpotifyToken(accessToken);
                Log.d("SpotifyAuth", "SUCCESS! Token secured using PKCE.");
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("SpotifyAuth", "Token exchange failed: " + errorMessage);
                // Optional: You could post an error LiveData here if the UI needs to show a Toast
            }
        });
    }
}