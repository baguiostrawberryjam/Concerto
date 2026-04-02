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

    public List<Track> getRandomSongs() {
        String token = tokenManager.getBestAvailableToken();
        List<Track> tracksList = new ArrayList<>();

        if (token != null) {
            try {
                URL url = new URL("https://api.spotify.com/v1/search?type=track&q=chopin:2023&limit=4");

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {

                    InputStream is = conn.getInputStream();
                    Scanner scanner = new Scanner(is).useDelimiter("\\A");
                    String responseBody = scanner.hasNext() ? scanner.next() : "";
                    scanner.close();

                    JSONObject jsonObject = new JSONObject(responseBody);
                    JSONObject tracksObject = jsonObject.getJSONObject("tracks");
                    JSONArray itemsArray = tracksObject.getJSONArray("items");

                    Log.d("TRACK_COUNT", "Items from API: " + itemsArray.length());

                    for (int i = 0; i < itemsArray.length(); i++) {
                        JSONObject trackJson = itemsArray.getJSONObject(i);

                        String trackName = trackJson.getString("name");
                        String trackUri = trackJson.getString("uri");

                        Log.d("TRACK_NAME", trackName);

                        // Grab ARTIST NAME
                        String artistName = "";
                        JSONArray artistsArray = trackJson.getJSONArray("artists");
                        if (artistsArray.length() > 0) {
                            artistName = artistsArray.getJSONObject(0).getString("name");
                        }

                        // Grab ALBUM COVER
                        String imageUrl = "";
                        JSONObject albumObject = trackJson.optJSONObject("album");
                        if (albumObject != null) {
                            JSONArray imagesArray = albumObject.optJSONArray("images");
                            // Spotify usually returns 3 sizes. Index 0 is the highest quality.
                            if (imagesArray != null && imagesArray.length() > 0) {
                                imageUrl = imagesArray.getJSONObject(0).getString("url");
                            }
                        }

                        Artist artist = new Artist(artistName, "");
                        ImageUri imageUri = new ImageUri(imageUrl);
                        Track track = new Track(artist, null, null, 0, trackName, trackUri, imageUri, false, false);

                        tracksList.add(track);
                    }

                    Log.d("SpotifyRepo", "Successfully fetched " + tracksList.size() + " songs!");

                } else {
                    Log.e("SpotifyRepo", "API Call failed with response code: " + responseCode);
                }

            } catch (Exception e) {
                Log.e("SpotifyRepo", "Network error fetching songs", e);
                e.printStackTrace();
            }
        } else {
            Log.e("SpotifyRepo", "No token available to fetch songs.");
        }

        return tracksList;
    }

    public List<Track> getSearchedSongs(String query) {
        String token = tokenManager.getBestAvailableToken();
        List<Track> tracksList = new ArrayList<>();

        if (token != null && query != null && !query.trim().isEmpty()) {
            try {
                String encodedQuery = java.net.URLEncoder.encode(query.trim(), "UTF-8");
                URL url = new URL("https://api.spotify.com/v1/search?q=" + encodedQuery + "&type=track&limit=10");

                Log.d("SEARCH_QUERY", "Raw: " + query);
                Log.d("SEARCH_QUERY", "Encoded: " + encodedQuery);
                Log.d("SEARCH_URL", url.toString());
                Log.d("TOKEN_TYPE", token.substring(0, 20));

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {

                    InputStream is = conn.getInputStream();
                    Scanner scanner = new Scanner(is).useDelimiter("\\A");
                    String responseBody = scanner.hasNext() ? scanner.next() : "";
                    scanner.close();

                    JSONObject jsonObject = new JSONObject(responseBody);
                    JSONObject tracksObject = jsonObject.getJSONObject("tracks");
                    JSONArray itemsArray = tracksObject.getJSONArray("items");

                    Log.d("SpotifyRepo", "Search returned: " + itemsArray.length() + " items");

                    for (int i = 0; i < itemsArray.length(); i++) {
                        JSONObject trackJson = itemsArray.getJSONObject(i);

                        String trackName = trackJson.getString("name");
                        String trackUri = trackJson.getString("uri");

                        String artistName = "";
                        JSONArray artistsArray = trackJson.getJSONArray("artists");
                        if (artistsArray.length() > 0) {
                            artistName = artistsArray.getJSONObject(0).getString("name");
                        }

                        String imageUrl = "";
                        JSONObject albumObject = trackJson.optJSONObject("album");
                        if (albumObject != null) {
                            JSONArray imagesArray = albumObject.optJSONArray("images");
                            if (imagesArray != null && imagesArray.length() > 0) {
                                imageUrl = imagesArray.getJSONObject(0).getString("url");
                            }
                        }

                        Artist artist = new Artist(artistName, "");
                        ImageUri imageUri = new ImageUri(imageUrl);
                        Track track = new Track(artist, null, null, 0, trackName, trackUri, imageUri, false, false);

                        tracksList.add(track);
                    }
                } else {
                    Log.e("SpotifyRepo", "Search API Call failed: " + responseCode);
                }

            } catch (Exception e) {
                Log.e("SpotifyRepo", "Network error during search", e);
            }
        }
        return tracksList;
    }
}