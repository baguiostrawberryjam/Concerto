package com.example.concerto.auth;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.concerto.R;
import com.example.concerto.databinding.FragmentSignupBinding;
import com.example.concerto.onboarding.OnboardingFragment;
import com.example.concerto.player.PlayerFragment;

public class SignupFragment extends Fragment {

    private FragmentSignupBinding bind;
    private AuthViewModel authViewModel;
    private SignupViewModel signupViewModel;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable usernameCheckRunnable;
    private Runnable emailCheckRunnable;

    // Validation state flags
    private boolean usernameValid = false;
    private boolean emailValid = false;
    private boolean passwordValid = false;
    private boolean usernameCheckPending = false;
    private boolean emailCheckPending = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        bind = FragmentSignupBinding.inflate(inflater, container, false);
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViewModels();
        setupRealtimeValidation();
        setupButtons();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (usernameCheckRunnable != null) debounceHandler.removeCallbacks(usernameCheckRunnable);
        if (emailCheckRunnable != null) debounceHandler.removeCallbacks(emailCheckRunnable);
        bind = null;
    }

    private void initViewModels() {
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        signupViewModel = new ViewModelProvider(this).get(SignupViewModel.class);
    }

    // ==========================================
    // REAL-TIME VALIDATION
    // ==========================================

    private void setupRealtimeValidation() {
        bind.etUsername.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateUsernameFormat(s.toString().trim());
            }
        });

        bind.etEmail.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateEmailFormat(s.toString().trim());
            }
        });

        bind.etPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePassword(s.toString());
            }
        });
    }

    // ---- USERNAME ----

    private void validateUsernameFormat(String username) {
        if (usernameCheckRunnable != null) debounceHandler.removeCallbacks(usernameCheckRunnable);

        if (username.isEmpty()) {
            setUsernameState(false, "3–20 characters", false); return;
        }
        if (username.length() < 3) {
            setUsernameState(false, "Too short — minimum 3 characters", false); return;
        }
        if (username.length() > 20) {
            setUsernameState(false, "Too long — maximum 20 characters", false); return;
        }
        if (!username.matches("[a-zA-Z0-9_]+")) {
            setUsernameState(false, "Only letters, numbers, and underscores", false); return;
        }

        setUsernameState(false, "Checking availability...", true);
        usernameCheckPending = true;
        updateSignupButton();

        usernameCheckRunnable = () -> signupViewModel.checkUsernameAvailable(username, available -> {
            if (bind == null) return;
            usernameCheckPending = false;
            if (available) {
                setUsernameState(true, "✓  Username is available", false);
            } else {
                setUsernameState(false, "✗  Username is already taken", false);
            }
            updateSignupButton();
        });
        debounceHandler.postDelayed(usernameCheckRunnable, 600);
    }

    private void setUsernameState(boolean valid, String message, boolean loading) {
        if (bind == null) return;
        usernameValid = valid;
        bind.tvUsernameStatus.setText(message);
        bind.tvUsernameStatus.setTextColor(valid
                ? android.graphics.Color.parseColor("#1DB954")
                : android.graphics.Color.parseColor("#AAAAAA"));
        bind.pbUsernameChecking.setVisibility(loading ? View.VISIBLE : View.GONE);
        bind.ivUsernameStatus.setVisibility(loading ? View.GONE : View.VISIBLE);
        bind.ivUsernameStatus.setImageResource(valid
                ? android.R.drawable.presence_online
                : android.R.drawable.presence_offline);
    }

    // ---- EMAIL ----

    private void validateEmailFormat(String email) {
        if (emailCheckRunnable != null) debounceHandler.removeCallbacks(emailCheckRunnable);

        if (email.isEmpty()) {
            setEmailState(false, "Must be a valid email address", false); return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setEmailState(false, "Must be a valid email address (e.g. name@example.com)", false); return;
        }

        // Format valid — debounce uniqueness check
        setEmailState(false, "Checking availability...", true);
        emailCheckPending = true;
        updateSignupButton();

        emailCheckRunnable = () -> signupViewModel.checkEmailAvailable(email, available -> {
            if (bind == null) return;
            emailCheckPending = false;
            if (available) {
                setEmailState(true, "✓  Email is available", false);
            } else {
                setEmailState(false, "✗  An account with this email already exists", false);
            }
            updateSignupButton();
        });
        debounceHandler.postDelayed(emailCheckRunnable, 600);
    }

    private void setEmailState(boolean valid, String message, boolean loading) {
        if (bind == null) return;
        emailValid = valid;
        bind.tvEmailStatus.setText(message);
        bind.tvEmailStatus.setTextColor(valid
                ? android.graphics.Color.parseColor("#1DB954")
                : android.graphics.Color.parseColor("#AAAAAA"));
        // Reuse username spinner/icon views pattern — email uses tvEmailStatus only
        // (no separate spinner in layout, spinner feedback via text is sufficient)
    }

    // ---- PASSWORD ----

    private void validatePassword(String password) {
        if (bind == null) return;
        boolean hasLength  = password.length() >= 12;
        boolean hasUpper   = password.matches(".*[A-Z].*");
        boolean hasLower   = password.matches(".*[a-z].*");
        boolean hasNumber  = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

        setRequirementView(bind.tvReqLength,  hasLength,  "At least 12 characters");
        setRequirementView(bind.tvReqUpper,   hasUpper,   "At least 1 uppercase letter");
        setRequirementView(bind.tvReqLower,   hasLower,   "At least 1 lowercase letter");
        setRequirementView(bind.tvReqNumber,  hasNumber,  "At least 1 number");
        setRequirementView(bind.tvReqSpecial, hasSpecial, "At least 1 special character (!@#$...)");

        passwordValid = hasLength && hasUpper && hasLower && hasNumber && hasSpecial;
        updateSignupButton();
    }

    private void setRequirementView(TextView tv, boolean met, String label) {
        tv.setText(met ? "✓  " + label : "✗  " + label);
        tv.setTextColor(met
                ? android.graphics.Color.parseColor("#1DB954")
                : android.graphics.Color.parseColor("#FF5555"));
    }

    private void updateSignupButton() {
        if (bind == null) return;
        boolean allValid = usernameValid && emailValid && passwordValid
                && !usernameCheckPending && !emailCheckPending;
        bind.btnSignup.setEnabled(allValid);
        bind.btnSignup.setAlpha(allValid ? 1.0f : 0.4f);
    }

    // ==========================================
    // BUTTONS
    // ==========================================

    private void setupButtons() {
        bind.btnSignup.setEnabled(false);
        bind.btnSignup.setAlpha(0.4f);

        bind.btnSignup.setOnClickListener(v -> createAccount());
        bind.btnGoToLogin.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
    }

    private void createAccount() {
        if (bind == null) return;
        String username = bind.etUsername.getText().toString().trim();
        String email    = bind.etEmail.getText().toString().trim();
        String password = bind.etPassword.getText().toString().trim();

        if (!usernameValid || !emailValid || !passwordValid) return;

        bind.btnSignup.setEnabled(false);
        bind.btnSignup.setText("Creating account...");

        authViewModel.signup(email, password, username, new AuthViewModel.SignupCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded() || getActivity() == null || getActivity().isFinishing()) return;

                authViewModel.logoutSpotify();

                View bottomNav = getActivity().findViewById(R.id.bottomNav);
                if (bottomNav != null) bottomNav.setVisibility(View.GONE);

                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutFragmentContainer, new OnboardingFragment())
                        .commitAllowingStateLoss();

                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutPlayerSheetContainer, new PlayerFragment(), "PLAYER")
                        .commitAllowingStateLoss();
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                if (bind != null) {
                    bind.btnSignup.setEnabled(true);
                    bind.btnSignup.setText("Sign Up");
                }
                showErrorDialog("Sign Up Failed", errorMessage);
            }
        });
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private void showErrorDialog(String title, String message) {
        if (!isAdded() || getActivity() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void afterTextChanged(Editable s) {}
    }
}