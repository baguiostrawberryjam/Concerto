package com.example.concerto.auth;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.concerto.spotify.SpotifyConfig;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

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

    public interface TokenExchangeListener {
        void onSuccess(String accessToken);
        void onError(String errorMessage);
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

                Object accessObj = snapshot.child("accessToken").getValue();
                String accessToken = accessObj != null ? String.valueOf(accessObj) : null;

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
                                String[] newTokens = tokenManager.refreshAccessToken(refreshToken);

                                if (newTokens != null) {
                                    db.child("accessToken").setValue(newTokens[0]);
                                    db.child("refreshToken").setValue(newTokens[1]);
                                    db.child("expiresAt").setValue(System.currentTimeMillis() + (3600 * 1000));
                                    Log.d("SpotifyAuth", "Token refreshed and rotated automatically!");

                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        if (listener != null) {
                                            listener.onSuccess(newTokens[0]);
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

    public void tradeCodeForToken(String code, String verifier, TokenExchangeListener listener) {
        new Thread(() -> {
            HttpURLConnection conn = null; // FIXED: Declare outside try block
            try {
                URL url = new URL("https://accounts.spotify.com/api/token");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setDoOutput(true);

                String body = "client_id=" + SpotifyConfig.CLIENT_ID +
                        "&grant_type=authorization_code" +
                        "&code=" + code +
                        "&redirect_uri=" + SpotifyConfig.REDIRECT_URI +
                        "&code_verifier=" + verifier;

                // FIXED: Auto-close OutputStream
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes());
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {

                    // FIXED: Auto-close InputStream and Scanner
                    try (InputStream is = conn.getInputStream();
                         Scanner scanner = new Scanner(is).useDelimiter("\\A")) {
                        String responseBody = scanner.hasNext() ? scanner.next() : "";

                        JSONObject jsonObject = new JSONObject(responseBody);
                        String accessToken = jsonObject.getString("access_token");
                        String refreshToken = jsonObject.optString("refresh_token");
                        int expiresIn = jsonObject.getInt("expires_in");

                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (currentUser != null) {
                            String uid = currentUser.getUid();
                            DatabaseReference db = FirebaseDatabase.getInstance("https://concerto-b02f9-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();

                            Map<String, Object> spotifyData = new HashMap<>();
                            spotifyData.put("accessToken", accessToken);
                            spotifyData.put("expiresAt", System.currentTimeMillis() + (expiresIn * 1000));

                            if (!refreshToken.isEmpty()) {
                                spotifyData.put("refreshToken", refreshToken);
                            }

                            db.child("users").child(uid).child("spotify").updateChildren(spotifyData)
                                    .addOnCompleteListener(task -> {
                                        new Handler(Looper.getMainLooper()).post(() -> {
                                            if (listener != null) listener.onSuccess(accessToken);
                                        });
                                    });
                        } else {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (listener != null) listener.onError("User not logged in");
                            });
                        }
                    }
                } else {
                    // FIXED: Read Error stream to catch Spotify API complaints
                    try (InputStream errorStream = conn.getErrorStream();
                         Scanner scanner = new Scanner(errorStream).useDelimiter("\\A")) {
                        String errorBody = scanner.hasNext() ? scanner.next() : "No error body";
                        Log.e("SpotifyAuth", "Trade code failed: " + responseCode + " - " + errorBody);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (listener != null) listener.onError("Failed with code: " + responseCode);
                        });
                    }
                }

            } catch (Exception e) {
                Log.e("SpotifyAuth", "Failed to trade code for token: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
            } finally {
                // FIXED: Guarantee connection closure
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    public void logoutFirebase() {
        FirebaseAuth.getInstance().signOut();
    }

    public boolean isUserLoggedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }
}