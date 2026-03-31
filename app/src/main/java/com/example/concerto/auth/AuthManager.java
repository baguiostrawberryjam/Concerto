package com.example.concerto.auth;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.concerto.spotify.SpotifyAppTokenManager;
import com.example.concerto.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.UserProfileChangeRequest;

public class AuthManager {

    private final SpotifyAppTokenManager tokenManager;

    public AuthManager(SpotifyAppTokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    public interface LoginListener {
        void onSuccess();
        void onError(String errorMessage);
    }

    public interface SignupListener {
        void onSuccess();
        void onError(String errorMessage);
    }

    public interface TokenRestoreListener {
        void onSuccess(String token);
        void onComplete();
    }

    public String getCurrentUsername() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            return user.getDisplayName();
        }
        return null;
    }

    public void loginWithEmailAndPassword(String email, String password, LoginListener listener) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (listener != null) listener.onSuccess();
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        if (listener != null) listener.onError(error);
                    }
                });
    }

    public void signupWithEmailAndPassword(String email, String password, String username, SignupListener listener) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (firebaseUser != null) {
                            String uid = firebaseUser.getUid();

                            User newUser = new User(username, email);

                            DatabaseReference ref = FirebaseDatabase.getInstance("https://concerto-b02f9-default-rtdb.asia-southeast1.firebasedatabase.app/")
                                    .getReference("users");

                            ref.child(uid).setValue(newUser).addOnCompleteListener(dbTask -> {
                                if (dbTask.isSuccessful()) {
                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                            .setDisplayName(username)
                                            .build();

                                    firebaseUser.updateProfile(profileUpdates).addOnCompleteListener(profileTask -> {
                                        if (listener != null) listener.onSuccess();
                                    });
                                } else {
                                    String error = dbTask.getException() != null ? dbTask.getException().getMessage() : "Database error";
                                    if (listener != null) listener.onError(error);
                                }
                            });
                        }
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        if (listener != null) listener.onError(error);
                    }
                });
    }

    public void restoreSpotifyTokenFromFirebase(TokenRestoreListener listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (listener != null) listener.onComplete();
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
                        Log.d("SpotifyAuth", "Token auto-restored from Firebase!");

                        if (listener != null) {
                            listener.onSuccess(accessToken);
                            listener.onComplete();
                        }
                    } else {
                        Log.d("SpotifyAuth", "Token in Firebase is expired.");
                        String refreshToken = snapshot.child("refreshToken").getValue(String.class);

                        if (refreshToken != null) {
                            new Thread(() -> {
                                String newAccessToken = tokenManager.refreshAccessToken(refreshToken);

                                if (newAccessToken != null) {
                                    db.child("accessToken").setValue(newAccessToken);
                                    db.child("expiresAt").setValue(System.currentTimeMillis() + (3600 * 1000));
                                    Log.d("SpotifyAuth", "Token refreshed automatically!");

                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        if (listener != null) {
                                            listener.onSuccess(newAccessToken);
                                            listener.onComplete();
                                        }
                                    });
                                } else {
                                    Log.e("SpotifyAuth", "Failed to refresh token");
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        if (listener != null) listener.onComplete();
                                    });
                                }
                            }).start();
                            return;
                        }
                    }
                }
            } else {
                Log.d("SpotifyAuth", "No Spotify data found in Firebase or error occurred.");
            }

            if (listener != null) listener.onComplete();
        });
    }

    public void logoutFirebase() {
        FirebaseAuth.getInstance().signOut();
    }
}