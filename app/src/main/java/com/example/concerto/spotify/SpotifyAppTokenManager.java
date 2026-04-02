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

            appTokenExpiry = System.currentTimeMillis() + ((3600 - 60) * 1000);
        }

        return cachedAppToken;
    }

    public boolean hasUserToken() {
        return cachedUserToken != null && !cachedUserToken.isEmpty();
    }

    private String fetchNewAppTokenFromNetwork() {
        try {
            URL url = new URL("https://accounts." + "spotify.com/api/token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String auth = SpotifyConfig.CLIENT_ID + ":" + SpotifyConfig.CLIENT_SECRET;
            String base64Auth = Base64.encodeToString(auth.getBytes(), Base64.NO_WRAP);
            conn.setRequestProperty("Authorization", "Basic " + base64Auth);
            conn.setDoOutput(true);

            String body = "grant_type=client_credentials";
            OutputStream os = conn.getOutputStream();
            os.write(body.getBytes());
            os.flush();
            os.close();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream is = conn.getInputStream();
                Scanner scanner = new Scanner(is).useDelimiter("\\A");
                String responseBody = scanner.hasNext() ? scanner.next() : "";
                scanner.close();

                JSONObject jsonObject = new JSONObject(responseBody);
                Log.d("SpotifyAuth", "Successfully fetched App Token!");
                return jsonObject.getString("access_token");
            } else {
                Log.e("SpotifyAuth", "Failed to fetch App Token: " + conn.getResponseCode());
            }
        } catch (Exception e) {
            Log.e("SpotifyAuth", "Network error fetching App Token", e);
        }
        return null;
    }

    public String[] refreshAccessToken(String refreshToken) {
        try {
            URL url = new URL("https://accounts.spotify.com/api/token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);

            String body = "grant_type=refresh_token" +
                    "&refresh_token=" + java.net.URLEncoder.encode(refreshToken, "UTF-8") +
                    "&client_id=" + SpotifyConfig.CLIENT_ID;

            OutputStream os = conn.getOutputStream();
            os.write(body.getBytes());
            os.flush();
            os.close();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream is = conn.getInputStream();
                Scanner scanner = new Scanner(is).useDelimiter("\\A");
                String responseBody = scanner.hasNext() ? scanner.next() : "";
                scanner.close();

                JSONObject jsonObject = new JSONObject(responseBody);
                Log.d("SpotifyAuth", "Successfully refreshed User Token in the background!");

                String newAccessToken = jsonObject.getString("access_token");
                // If Spotify provides a new refresh token, grab it. Otherwise, keep using the old one.
                String newRefreshToken = jsonObject.optString("refresh_token", refreshToken);

                // Return both tokens to the AuthManager
                return new String[]{newAccessToken, newRefreshToken};
            } else {
                Log.e("SpotifyAuth", "Failed to refresh User Token: " + conn.getResponseCode());
            }
        } catch (Exception e) {
            Log.e("SpotifyAuth", "Network error refreshing User Token", e);
        }
        return null;
    }

    public void clearUserToken() {
        this.cachedUserToken = null;
        this.cachedAppToken = null;
        this.appTokenExpiry = 0;
    }
}