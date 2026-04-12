package com.example.concerto.spotify;

import android.content.Context;
import android.util.Log;

import com.spotify.protocol.types.Artist;
import com.spotify.protocol.types.Track;
import com.spotify.protocol.types.ImageUri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SpotifyRepository {

    private static SpotifyRepository instance;
    private final SpotifyAppTokenManager tokenManager;

    private SpotifyRepository(SpotifyAppTokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    public static synchronized SpotifyRepository getInstance(Context context) {
        if (instance == null) {
            instance = new SpotifyRepository(SpotifyAppTokenManager.getInstance(context));
        }
        return instance;
    }

    public interface PremiumCheckCallback {
        void onResult(boolean isPremium);
    }

    public List<Track> getRandomSongs() {
        String token = tokenManager.getAppToken();
        List<Track> tracksList = new ArrayList<>();

        if (token != null) {
            HttpURLConnection conn = null; // FIXED
            try {
                URL url = new URL("https://api.spotify.com/v1/search?type=track&q=chopin:2023&limit=4");

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {

                    // FIXED: Auto-close streams
                    try (InputStream is = conn.getInputStream();
                         Scanner scanner = new Scanner(is).useDelimiter("\\A")) {

                        String responseBody = scanner.hasNext() ? scanner.next() : "";

                        JSONObject jsonObject = new JSONObject(responseBody);
                        // Using optJSONObject to prevent crashes if the 'tracks' wrapper is missing
                        JSONObject tracksObject = jsonObject.optJSONObject("tracks");

                        if (tracksObject != null) {
                            JSONArray itemsArray = tracksObject.optJSONArray("items");

                            if (itemsArray != null) {
                                Log.d("TRACK_COUNT", "Items from API: " + itemsArray.length());

                                for (int i = 0; i < itemsArray.length(); i++) {
                                    JSONObject trackJson = itemsArray.optJSONObject(i);
                                    if (trackJson == null) continue; // Skip invalid entries

                                    // FIXED: Using optString prevents JSONExceptions
                                    String trackName = trackJson.optString("name", "Unknown Track");
                                    String trackUri = trackJson.optString("uri", "");

                                    // Grab ARTIST NAME Safely
                                    String artistName = "Unknown Artist";
                                    JSONArray artistsArray = trackJson.optJSONArray("artists");
                                    if (artistsArray != null && artistsArray.length() > 0) {
                                        JSONObject artistObj = artistsArray.optJSONObject(0);
                                        if (artistObj != null) {
                                            artistName = artistObj.optString("name", "Unknown Artist");
                                        }
                                    }

                                    // Grab ALBUM COVER Safely
                                    String imageUrl = "";
                                    JSONObject albumObject = trackJson.optJSONObject("album");
                                    if (albumObject != null) {
                                        JSONArray imagesArray = albumObject.optJSONArray("images");
                                        if (imagesArray != null && imagesArray.length() > 0) {
                                            JSONObject imageObj = imagesArray.optJSONObject(0);
                                            if (imageObj != null) {
                                                imageUrl = imageObj.optString("url", "");
                                            }
                                        }
                                    }

                                    Artist artist = new Artist(artistName, "");
                                    ImageUri imageUri = new ImageUri(imageUrl);
                                    Track track = new Track(artist, null, null, 0, trackName, trackUri, imageUri, false, false);

                                    tracksList.add(track);
                                }
                            }
                        }
                        Log.d("SpotifyRepo", "Successfully fetched " + tracksList.size() + " songs!");
                    }

                } else {
                    // FIXED: Read error stream
                    try (InputStream errorStream = conn.getErrorStream();
                         Scanner scanner = new Scanner(errorStream).useDelimiter("\\A")) {
                        String errorBody = scanner.hasNext() ? scanner.next() : "No error body";
                        Log.e("SpotifyRepo", "API Call failed: " + responseCode + " - " + errorBody);
                    }
                }

            } catch (Exception e) {
                Log.e("SpotifyRepo", "Network error fetching songs", e);
            } finally {
                // FIXED: Disconnect
                if (conn != null) {
                    conn.disconnect();
                }
            }
        } else {
            Log.e("SpotifyRepo", "No token available to fetch songs.");
        }

        return tracksList;
    }

    public List<Track> getSearchedSongs(String query) {
        String token = tokenManager.getAppToken();
        List<Track> tracksList = new ArrayList<>();

        if (token != null && query != null && !query.trim().isEmpty()) {
            HttpURLConnection conn = null; // FIXED
            try {
                String encodedQuery = java.net.URLEncoder.encode(query.trim(), "UTF-8");
                URL url = new URL("https://api.spotify.com/v1/search?q=" + encodedQuery + "&type=track&limit=10");

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {

                    // FIXED: Auto-close streams
                    try (InputStream is = conn.getInputStream();
                         Scanner scanner = new Scanner(is).useDelimiter("\\A")) {

                        String responseBody = scanner.hasNext() ? scanner.next() : "";

                        JSONObject jsonObject = new JSONObject(responseBody);
                        JSONObject tracksObject = jsonObject.optJSONObject("tracks");

                        if (tracksObject != null) {
                            JSONArray itemsArray = tracksObject.optJSONArray("items");

                            if (itemsArray != null) {
                                Log.d("SpotifyRepo", "Search returned: " + itemsArray.length() + " items");

                                for (int i = 0; i < itemsArray.length(); i++) {
                                    JSONObject trackJson = itemsArray.optJSONObject(i);
                                    if (trackJson == null) continue;

                                    // FIXED: Using optString and optJSONArray for safety
                                    String trackName = trackJson.optString("name", "Unknown Track");
                                    String trackUri = trackJson.optString("uri", "");

                                    String artistName = "Unknown Artist";
                                    JSONArray artistsArray = trackJson.optJSONArray("artists");
                                    if (artistsArray != null && artistsArray.length() > 0) {
                                        JSONObject artistObj = artistsArray.optJSONObject(0);
                                        if (artistObj != null) {
                                            artistName = artistObj.optString("name", "Unknown Artist");
                                        }
                                    }

                                    String imageUrl = "";
                                    JSONObject albumObject = trackJson.optJSONObject("album");
                                    if (albumObject != null) {
                                        JSONArray imagesArray = albumObject.optJSONArray("images");
                                        if (imagesArray != null && imagesArray.length() > 0) {
                                            JSONObject imageObj = imagesArray.optJSONObject(0);
                                            if (imageObj != null) {
                                                imageUrl = imageObj.optString("url", "");
                                            }
                                        }
                                    }

                                    Artist artist = new Artist(artistName, "");
                                    ImageUri imageUri = new ImageUri(imageUrl);
                                    Track track = new Track(artist, null, null, 0, trackName, trackUri, imageUri, false, false);

                                    tracksList.add(track);
                                }
                            }
                        }
                    }
                } else {
                    // FIXED: Read error stream
                    try (InputStream errorStream = conn.getErrorStream();
                         Scanner scanner = new Scanner(errorStream).useDelimiter("\\A")) {
                        String errorBody = scanner.hasNext() ? scanner.next() : "No error body";
                        Log.e("SpotifyRepo", "Search API Call failed: " + responseCode + " - " + errorBody);
                    }
                }

            } catch (Exception e) {
                Log.e("SpotifyRepo", "Network error during search", e);
            } finally {
                // FIXED: Disconnect
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
        return tracksList;
    }

    public void checkPremiumStatus(String userToken, PremiumCheckCallback callback) {
        if (userToken == null || userToken.isEmpty()) {
            if (callback != null) callback.onResult(false);
            return;
        }

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                // FIXED: Use standard Spotify API endpoint for user profile
                URL url = new URL("https://api.spotify.com/v1/me");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + userToken);

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    try (InputStream is = conn.getInputStream();
                         Scanner scanner = new Scanner(is).useDelimiter("\\A")) {
                        String responseBody = scanner.hasNext() ? scanner.next() : "";

                        JSONObject jsonObject = new JSONObject(responseBody);
                        String product = jsonObject.optString("product", "free");

                        boolean isPremium = "premium".equalsIgnoreCase(product);
                        Log.d("SpotifyPremiumCheck", "User account type: " + product);

                        if (callback != null) callback.onResult(isPremium);
                    }
                } else {
                    if (callback != null) callback.onResult(false);
                }
            } catch (Exception e) {
                Log.e("SpotifyPremiumCheck", "Failed to check premium status", e);
                if (callback != null) callback.onResult(false);
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }
}