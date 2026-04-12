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

    /** Checks whether a username is already taken. Requires .indexOn ["username"] in Firebase rules. */
    public void checkUsernameAvailable(String username, AvailabilityCallback callback) {
        if (username == null || username.trim().isEmpty()) {
            if (callback != null) callback.onResult(false);
            return;
        }
        usersRef.orderByChild("username").equalTo(username.trim()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (callback != null) callback.onResult(!task.getResult().exists());
                    } else {
                        if (callback != null) callback.onResult(true); // allow on network error
                    }
                });
    }

    /** Checks whether an email is already registered. Requires .indexOn ["email"] in Firebase rules. */
    public void checkEmailAvailable(String email, AvailabilityCallback callback) {
        if (email == null || email.trim().isEmpty()) {
            if (callback != null) callback.onResult(false);
            return;
        }
        usersRef.orderByChild("email").equalTo(email.trim().toLowerCase()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (callback != null) callback.onResult(!task.getResult().exists());
                    } else {
                        if (callback != null) callback.onResult(true); // allow on network error
                    }
                });
    }
}