package com.example.concerto.concerto;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.concerto.R;
import com.example.concerto.auth.AuthViewModel;
import com.example.concerto.auth.ConnectSpotifyFragment;
import com.example.concerto.databinding.FragmentConcertoLobbyBinding;

public class ConcertoLobbyFragment extends Fragment {

    private FragmentConcertoLobbyBinding bind;
    private ConcertoViewModel concertoViewModel;
    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        bind = FragmentConcertoLobbyBinding.inflate(inflater, container, false);
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViewModels();
        setupButtons();
        setupObservers();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }

    // ==========================================
    // INITIALIZATION METHODS
    // ==========================================

    private void initViewModels() {
        if (getActivity() == null) return;
        concertoViewModel = new ViewModelProvider(requireActivity()).get(ConcertoViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
    }

    private void setupButtons() {
        bind.btnCreateConcerto.setOnClickListener(v -> {
            String token = authViewModel.getSpotifyToken().getValue();
            if (token != null && !token.trim().isEmpty()) {
                concertoViewModel.createConcerto();
            } else {
                if (!isAdded() || getActivity() == null) return;
                Toast.makeText(requireContext(), "Connect Spotify to host a room!", Toast.LENGTH_SHORT).show();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutFragmentContainer, new ConnectSpotifyFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        bind.btnJoinConcerto.setOnClickListener(v -> {
            String pin = bind.etConcertoPin.getText() != null ? bind.etConcertoPin.getText().toString().trim() : "";
            if (pin.length() == 4) {
                concertoViewModel.joinConcerto(pin);
            } else {
                bind.tilPinInput.setError("Please enter a valid 4-digit PIN");
            }
        });
    }

    private void setupObservers() {
        concertoViewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty() && isAdded()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                concertoViewModel.clearToastMessage();
            }
        });

        concertoViewModel.getActiveSessionPin().observe(getViewLifecycleOwner(), pin -> {
            if (pin != null && isAdded() && getActivity() != null) {
                // FIXED: Safe navigation using commitAllowingStateLoss
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutFragmentContainer, new ConcertoFragment())
                        .commitAllowingStateLoss();
            }
        });
    }
}