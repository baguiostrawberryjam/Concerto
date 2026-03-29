package com.example.concerto.models;

public class User {

    // Basic profile
    public String username;
    public String email;

    // Future Concerto-specific fields
    public String joinCode;
    public String playlistId;

    // Required empty constructor for Firebase
    public User() {
    }

    // Constructor for signup (basic fields)
    public User(String username, String email) {
        this.username = username;
        this.email = email;

        // initialize empty values for safety
        this.joinCode = "";
        this.playlistId = "";
    }
}