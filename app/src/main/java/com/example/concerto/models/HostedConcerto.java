package com.example.concerto.models;

public class HostedConcerto {
    public String pin;
    public String status;

    public HostedConcerto() {
        // Required empty constructor for Firebase
    }

    public HostedConcerto(String pin, String status) {
        this.pin = pin;
        this.status = status;
    }
}