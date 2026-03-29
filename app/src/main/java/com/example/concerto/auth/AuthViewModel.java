package com.example.concerto.auth;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.concerto.spotify.SpotifyAppTokenManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AuthViewModel extends AndroidViewModel {

    private final MutableLiveData<String> spotifyToken = new MutableLiveData<>();
    private final SpotifyAppTokenManager tokenManager;
    private final MutableLiveData<String> codeVerifier = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        tokenManager = SpotifyAppTokenManager.getInstance(application);
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

    public void logoutSpotify() {
        spotifyToken.postValue(null);
        tokenManager.clearUserToken(); // Clears the memory cache
    }

    public void restoreSpotifyTokenFromFirebase(Runnable onComplete) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (onComplete != null) onComplete.run();
            return;
        }

        DatabaseReference db = FirebaseDatabase.getInstance("https://concerto-b02f9-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference()
                .child("users")
                .child(user.getUid())
                .child("spotify");

        db.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                DataSnapshot snapshot = task.getResult();
                String accessToken = snapshot.child("accessToken").getValue(String.class);
                Long expiresAt = snapshot.child("expiresAt").getValue(Long.class);

                if (accessToken != null) {
                    if (expiresAt != null && System.currentTimeMillis() < expiresAt) {
                        setSpotifyToken(accessToken); // This loads it from RTDB directly into RAM!
                        Log.d("SpotifyAuth", "Token auto-restored from Firebase!");
                    } else {
                        Log.d("SpotifyAuth", "Token in Firebase is expired.");

                        String refreshToken = snapshot.child("refreshToken").getValue(String.class);

                        if (refreshToken != null) {
                            new Thread(() -> {
                                String newAccessToken = tokenManager.refreshAccessToken(refreshToken);

                                if (newAccessToken != null) {

                                    db.child("accessToken").setValue(newAccessToken);
                                    db.child("expiresAt").setValue(System.currentTimeMillis() + (3600 * 1000));

                                    // Update UI
                                    setSpotifyToken(newAccessToken);

                                    Log.d("SpotifyAuth", "Token refreshed automatically!");
                                } else {
                                    Log.e("SpotifyAuth", "Failed to refresh token");
                                }

                                if (onComplete != null) onComplete.run();

                            }).start();

                            return; // prevent double-calling onComplete
                        }
                    }
                }
            } else {
                if (task.getException() != null) {
                    Log.e("SpotifyAuth", "Firebase DB Error: " + task.getException().getMessage());
                } else {
                    Log.d("SpotifyAuth", "No Spotify data found in Firebase for this user.");
                }
            }
            if (onComplete != null) onComplete.run();
        });
    }
}