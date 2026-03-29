package com.example.concerto;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.concerto.auth.AuthViewModel;
import com.example.concerto.databinding.FragmentSignupBinding;
import com.example.concerto.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignupFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FragmentSignupBinding bind;
    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        bind = FragmentSignupBinding.inflate(inflater, container, false);
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuth = FirebaseAuth.getInstance();

        // Grab the AuthViewModel
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        bind.btnSignup.setOnClickListener(v -> createFirebaseAccount());

        bind.btnGoToLogin.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });
    }

    private void createFirebaseAccount() {
        String username = bind.etUsername.getText().toString().trim();
        String email = bind.etEmail.getText().toString().trim();
        String pass = bind.etPassword.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();

                            User newUser = new User(username, email);

                            DatabaseReference ref = FirebaseDatabase.getInstance()
                                    .getReference("users");

                            ref.child(uid).setValue(newUser)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(requireContext(), "User saved!", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(requireContext(),
                                                "Failed: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    });

                            authViewModel.logoutSpotify();

                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (isAdded()) {
                                            requireActivity().getSupportFragmentManager().beginTransaction()
                                                    .replace(R.id.fragment_container, new DashboardFragment())
                                                    .commit();
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(requireContext(), "Sign Up Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }
}