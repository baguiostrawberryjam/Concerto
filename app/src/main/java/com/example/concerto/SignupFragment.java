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
import com.example.concerto.player.PlayerFragment;

public class SignupFragment extends Fragment {

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
        bind.btnSignup.setOnClickListener(v -> createAccount());

        bind.btnGoToLogin.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });
    }

    private void createAccount() {
        String username = bind.etUsername.getText().toString().trim();
        String email = bind.etEmail.getText().toString().trim();
        String pass = bind.etPassword.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        authViewModel.signup(email, pass, username, new AuthViewModel.SignupCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(requireContext(), "Account created successfully!", Toast.LENGTH_SHORT).show();

                authViewModel.logoutSpotify();

                if (isAdded()) {
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new DashboardFragment())
                            .commit();

                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.player_sheet_container, new PlayerFragment(), "PLAYER")
                            .commit();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(requireContext(), "Sign Up Failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}