package com.example.concerto.spotify;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class SpotifyAppTokenManager {
    private static SpotifyAppTokenManager instance;

    private String cachedAppToken = null;
    private String cachedUserToken = null;
    private long appTokenExpiry = 0;

    private SpotifyAppTokenManager(Context context) {}

    public static synchronized SpotifyAppTokenManager getInstance(Context context) {
        if (instance == null) {
            instance = new SpotifyAppTokenManager(context.getApplicationContext());
        }
        return instance;
    }

    public void setUserToken(String token) {
        this.cachedUserToken = token;
    }

    public String getBestAvailableToken() {
        if (cachedUserToken != null && !cachedUserToken.isEmpty()) {
            return cachedUserToken;
        }
        return getAppToken();
    }

    public String getAppToken() {
        if (cachedAppToken != null && System.currentTimeMillis() < appTokenExpiry) {
            return cachedAppToken;
        }

        String newToken = fetchNewAppTokenFromNetwork();

        if (newToken != null) {
            cachedAppToken = newToken;
            // Expire 60 seconds early to prevent edge-case failures
            appTokenExpiry = System.currentTimeMillis() + ((3600 - 60) * 1000);
        }

        return cachedAppToken;
    }

    public boolean hasUserToken() {
        return cachedUserToken != null && !cachedUserToken.isEmpty();
    }

    private String fetchNewAppTokenFromNetwork() {
        HttpURLConnection conn = null;
        try {
            // FIXED: Using standard Spotify Auth Endpoint
            URL url = new URL("https://accounts.spotify.com/api/token");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String auth = SpotifyConfig.CLIENT_ID + ":" + SpotifyConfig.CLIENT_SECRET;
            String base64Auth = Base64.encodeToString(auth.getBytes(), Base64.NO_WRAP);
            conn.setRequestProperty("Authorization", "Basic " + base64Auth);
            conn.setDoOutput(true);

            String body = "grant_type=client_credentials";

            // FIXED: try-with-resources auto-closes the OutputStream
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes());
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // FIXED: try-with-resources auto-closes the InputStream and Scanner
                try (InputStream is = conn.getInputStream();
                     Scanner scanner = new Scanner(is).useDelimiter("\\A")) {
                    String responseBody = scanner.hasNext() ? scanner.next() : "";

                    JSONObject jsonObject = new JSONObject(responseBody);
                    Log.d("SpotifyAuth", "Successfully fetched App Token!");
                    return jsonObject.getString("access_token");
                }
            } else {
                // FIXED: Read the error stream to see Spotify's exact complaint
                try (InputStream errorStream = conn.getErrorStream();
                     Scanner scanner = new Scanner(errorStream).useDelimiter("\\A")) {
                    String errorBody = scanner.hasNext() ? scanner.next() : "No error body";
                    Log.e("SpotifyAuth", "Failed to fetch App Token: " + responseCode + " - " + errorBody);
                }
            }
        } catch (Exception e) {
            Log.e("SpotifyAuth", "Network error fetching App Token", e);
        } finally {
            // FIXED: Guarantee the connection is closed to prevent socket leaks
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    public String[] refreshAccessToken(String refreshToken) {
        HttpURLConnection conn = null;
        try {
            // FIXED: Using standard Spotify Auth Endpoint
            URL url = new URL("https://accounts.spotify.com/api/token");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);

            String body = "grant_type=refresh_token" +
                    "&refresh_token=" + java.net.URLEncoder.encode(refreshToken, "UTF-8") +
                    "&client_id=" + SpotifyConfig.CLIENT_ID;

            // FIXED: try-with-resources auto-closes the OutputStream
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes());
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // FIXED: try-with-resources auto-closes the InputStream and Scanner
                try (InputStream is = conn.getInputStream();
                     Scanner scanner = new Scanner(is).useDelimiter("\\A")) {
                    String responseBody = scanner.hasNext() ? scanner.next() : "";

                    JSONObject jsonObject = new JSONObject(responseBody);
                    Log.d("SpotifyAuth", "Successfully refreshed User Token in the background!");

                    String newAccessToken = jsonObject.getString("access_token");
                    String newRefreshToken = jsonObject.optString("refresh_token", refreshToken);

                    return new String[]{newAccessToken, newRefreshToken};
                }
            } else {
                // FIXED: Read the error stream
                try (InputStream errorStream = conn.getErrorStream();
                     Scanner scanner = new Scanner(errorStream).useDelimiter("\\A")) {
                    String errorBody = scanner.hasNext() ? scanner.next() : "No error body";
                    Log.e("SpotifyAuth", "Failed to refresh User Token: " + responseCode + " - " + errorBody);
                }
            }
        } catch (Exception e) {
            Log.e("SpotifyAuth", "Network error refreshing User Token", e);
        } finally {
            // FIXED: Guarantee the connection is closed
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    public void clearUserToken() {
        this.cachedUserToken = null;
        this.cachedAppToken = null;
        this.appTokenExpiry = 0;
    }
}