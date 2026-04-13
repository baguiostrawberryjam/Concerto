package com.example.concerto.models;

public class User {

    public String username;
    public String usernameLower; // Lowercase version for case-insensitive uniqueness checks
    public String email;

    public String joinCode;
    public String playlistId;

    // Required empty constructor for Firebase
    public User() {}

    public User(String username, String email) {
        this.username = username;
        this.usernameLower = username != null ? username.toLowerCase() : "";
        this.email = email != null ? email.toLowerCase() : ""; // Always store email lowercase
        this.joinCode = "";
        this.playlistId = "";
    }
}