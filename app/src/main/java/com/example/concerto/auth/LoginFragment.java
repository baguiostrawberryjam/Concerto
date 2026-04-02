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
                    .commit();
        });
    }

    private void loginFirebase() {
        String email = bind.etEmail.getText().toString().trim();
        String password = bind.etPassword.getText().toString().trim();
        if (email.isEmpty() || password.isEmpty()) return;

        authViewModel.login(email, password, new AuthViewModel.LoginCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(requireContext(), "Logging in... checking Spotify connection", Toast.LENGTH_SHORT).show();

                Log.d("LoginProcess", "1. Auth success! Attempting to restore token...");

                authViewModel.restoreSpotifyTokenFromFirebase(() -> {Log.d("LoginProcess", "2. Firebase Database check finished!");

                    if (getActivity() != null && !getActivity().isFinishing()) {
                        Log.d("LoginProcess", "3. Navigating to Dashboard...");

                        View bottomNav = requireActivity().findViewById(R.id.bottomNav);
                        if (bottomNav != null) {
                            bottomNav.setVisibility(View.VISIBLE);
                        }

                        requireActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.layoutFragmentContainer, new DashboardFragment())
                                .commit();

                        requireActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.layoutPlayerSheetContainer, new PlayerFragment(), "PLAYER")
                                .commit();
                    } else {
                        Log.e("LoginProcess", "ERROR: Activity is null or finishing! Cannot navigate.");
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(requireContext(), "Authentication failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}