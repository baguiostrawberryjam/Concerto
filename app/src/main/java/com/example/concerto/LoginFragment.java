package com.example.concerto;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.concerto.auth.AuthViewModel;
import com.example.concerto.databinding.FragmentLoginBinding;
import com.google.firebase.auth.FirebaseAuth;

public class LoginFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FragmentLoginBinding bind;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        bind = FragmentLoginBinding.inflate(inflater, container, false);
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuth = FirebaseAuth.getInstance();

        bind.btnLogin.setOnClickListener(v -> loginFirebase());

        bind.btnGoToSignup.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SignupFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void loginFirebase() {
        String email = bind.etEmail.getText().toString().trim();
        String password = bind.etPassword.getText().toString().trim();
        if (email.isEmpty() || password.isEmpty()) return;

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        // FIREBASE LOGIN SUCCESS!

                        Toast.makeText(requireContext(), "Logging in... checking Spotify connection", Toast.LENGTH_SHORT).show();

                        // Attempt to restore the token

                        AuthViewModel authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

                        Log.d("LoginProcess", "1. Auth success! Attempting to restore token...");

                        authViewModel.restoreSpotifyTokenFromFirebase(() -> {
                            Log.d("LoginProcess", "2. Firebase Database check finished!");

                            if (getActivity() != null && !getActivity().isFinishing()) {
                                Log.d("LoginProcess", "3. Navigating to Dashboard...");
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.fragment_container, new DashboardFragment())
                                        .commitAllowingStateLoss(); // Prevents crashes if the system is busy
                            } else {
                                Log.e("LoginProcess", "ERROR: Activity is null or finishing! Cannot navigate.");
                            }
                        });

                    } else {
                        // Firebase Login Failed
                        Toast.makeText(requireContext(), "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }
}