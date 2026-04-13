package com.example.concerto.auth;

import androidx.lifecycle.ViewModel;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignupViewModel extends ViewModel {

    private final DatabaseReference usersRef = FirebaseDatabase
            .getInstance("https://concerto-b02f9-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("users");

    public interface AvailabilityCallback {
        void onResult(boolean isAvailable);
    }

    /**
     * Checks whether a username is already taken.
     * Stores and queries as lowercase so "Alice" and "alice" are treated as the same.
     * Requires .indexOn ["usernameLower"] in Firebase rules.
     */
    public void checkUsernameAvailable(String username, AvailabilityCallback callback) {
        if (username == null || username.trim().isEmpty()) {
            if (callback != null) callback.onResult(false);
            return;
        }
        // Query the lowercase field to catch case-insensitive duplicates
        String usernameLower = username.trim().toLowerCase();
        usersRef.orderByChild("usernameLower").equalTo(usernameLower).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (callback != null) callback.onResult(!task.getResult().exists());
                    } else {
                        // Network error — allow optimistically; Firebase Auth catches at write time
                        if (callback != null) callback.onResult(true);
                    }
                });
    }

    /**
     * Checks whether an email is already registered.
     * Queries as lowercase so casing differences don't bypass the check.
     * Requires .indexOn ["email"] in Firebase rules.
     */
    public void checkEmailAvailable(String email, AvailabilityCallback callback) {
        if (email == null || email.trim().isEmpty()) {
            if (callback != null) callback.onResult(false);
            return;
        }
        String emailLower = email.trim().toLowerCase();
        usersRef.orderByChild("email").equalTo(emailLower).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (callback != null) callback.onResult(!task.getResult().exists());
                    } else {
                        if (callback != null) callback.onResult(true);
                    }
                });
    }
}