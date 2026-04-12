package com.example.concerto.models;

import java.util.HashMap;
import java.util.Map;

public class ConcertoTrack {
    public String uri;
    public String name;
    public String artist;
    public String imageUrl;
    public Map<String, Boolean> voters; // Tracks which UIDs have voted

    // Required empty constructor for Firebase
    public ConcertoTrack() {
    }

    public ConcertoTrack(String uri, String name, String artist, String imageUrl) {
        this.uri = uri;
        this.name = name;
        this.artist = artist;
        this.imageUrl = imageUrl;
        this.voters = new HashMap<>();
    }
}