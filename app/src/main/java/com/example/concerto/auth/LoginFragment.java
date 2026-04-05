package com.example.concerto.auth;

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

import com.example.concerto.dashboard.DashboardFragment;
import com.example.concerto.R;
import com.example.concerto.databinding.FragmentLoginBinding;
import com.example.concerto.player.PlayerFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding bind;
    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        bind = FragmentLoginBinding.inflate(inflater, container, false);
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViewModels();
        setupButtons();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }

    private void initViewModels() {
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
    }

    private void setupButtons() {
        bind.btnLogin.setOnClickListener(v -> loginFirebase());

        bind.btnGoToSignup.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.layoutFragmentContainer, new SignupFragment())
                    .addToBackStack(null)
                    .commit(); // Normal commit is fine here because it's an instant button click
        });
    }

    private void loginFirebase() {
        String email = bind.etEmail.getText().toString().trim();
        String password = bind.etPassword.getText().toString().trim();
        if (email.isEmpty() || password.isEmpty()) return;

        authViewModel.login(email, password, new AuthViewModel.LoginCallback() {
            @Override
            public void onSuccess() {
                // FIXED: Ensure fragment is still attached before making UI calls
                if (!isAdded()) return;

                Toast.makeText(requireContext(), "Logging in... checking Spotify connection", Toast.LENGTH_SHORT).show();
                Log.d("LoginProcess", "1. Auth success! Attempting to restore token...");

                authViewModel.restoreSpotifyTokenFromFirebase(() -> {
                    Log.d("LoginProcess", "2. Firebase Database check finished!");

                    // FIXED: Strict attachment checks for async callbacks
                    if (!isAdded() || getActivity() == null || getActivity().isFinishing()) {
                        Log.e("LoginProcess", "ERROR: Activity is null, finishing, or detached! Cannot navigate.");
                        return;
                    }

                    Log.d("LoginProcess", "3. Navigating to Dashboard...");
                    BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNav);
                    if (bottomNav != null) {
                        bottomNav.setVisibility(View.VISIBLE);
                        bottomNav.getMenu().findItem(R.id.nav_home).setChecked(true);
                    }

                    // FIXED: commitAllowingStateLoss prevents crashes if app is backgrounded
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.layoutFragmentContainer, new DashboardFragment())
                            .commitAllowingStateLoss();

                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.layoutPlayerSheetContainer, new PlayerFragment(), "PLAYER")
                            .commitAllowingStateLoss();
                });
            }

            @Override
            public void onError(String errorMessage) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Authentication failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}