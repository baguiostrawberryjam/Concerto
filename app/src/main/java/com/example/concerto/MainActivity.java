package com.example.concerto;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.concerto.auth.AuthViewModel;
import com.example.concerto.auth.ConnectSpotifyFragment;
import com.example.concerto.auth.LoginFragment;
import com.example.concerto.auth.SignupFragment;
import com.example.concerto.concerto.ConcertoFragment;
import com.example.concerto.concerto.ConcertoLobbyFragment;
import com.example.concerto.concerto.ConcertoViewModel;
import com.example.concerto.dashboard.DashboardFragment;
import com.example.concerto.databinding.ActivityMainBinding;
import com.example.concerto.onboarding.OnboardingFragment;
import com.example.concerto.player.PlayerFragment;
import com.example.concerto.profile.ProfileFragment;
import com.example.concerto.search.SearchFragment;
import com.example.concerto.utils.NetworkMonitor;
import com.spotify.sdk.android.auth.AuthorizationResponse;

public class MainActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private ConcertoViewModel concertoViewModel;
    private ActivityMainBinding bind;
    private NetworkMonitor networkMonitor;

    // Fragments that should NEVER show the bottom nav bar
    private static boolean isNoNavFragment(Fragment f) {
        return f instanceof LoginFragment
                || f instanceof SignupFragment
                || f instanceof ConnectSpotifyFragment
                || f instanceof OnboardingFragment
                || f instanceof PlayerFragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.core.splashscreen.SplashScreen.installSplashScreen(this);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);

        bind = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(bind.getRoot());

        initViewModels();
        setupUI(savedInstanceState);
        setupButtons();
        setupNetworkMonitor();
        setupNavVisibilityListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        networkMonitor.startMonitoring();
    }

    @Override
    protected void onPause() {
        super.onPause();
        networkMonitor.stopMonitoring();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Uri uri = intent.getData();

        if (uri != null) {
            AuthorizationResponse response = AuthorizationResponse.fromUri(uri);
            AuthorizationResponse.Type type = response.getType();

            if (type == AuthorizationResponse.Type.CODE) {
                String code = response.getCode();
                String verifier = authViewModel.getCodeVerifier();
                authViewModel.exchangeCodeForToken(code, verifier);
            } else if (type == AuthorizationResponse.Type.ERROR) {
                Log.e("SpotifyAuth", "Auth error: " + response.getError());
            }
        }
    }

    // ==========================================
    // CENTRALIZED NAV VISIBILITY
    // ==========================================

    private void setupNavVisibilityListener() {
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentResumed(fm, f);

                if (f instanceof PlayerFragment) {
                    return;
                }

                bind.bottomNav.setVisibility(isNoNavFragment(f) ? View.GONE : View.VISIBLE);
            }
        }, true);
    }

    // ==========================================
    // INIT
    // ==========================================

    private void initViewModels() {
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        concertoViewModel = new ViewModelProvider(this).get(ConcertoViewModel.class);
    }

    private void setupUI(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            if (authViewModel.isUserLoggedIn()) {
                authViewModel.restoreSpotifyTokenFromFirebase(() -> {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.layoutFragmentContainer, new DashboardFragment())
                            .commitAllowingStateLoss();

                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.layoutPlayerSheetContainer, new PlayerFragment(), "PLAYER")
                            .commitAllowingStateLoss();

                    bind.bottomNav.setVisibility(View.VISIBLE);
                });
            } else {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutFragmentContainer, new LoginFragment())
                        .commitAllowingStateLoss();

                bind.bottomNav.setVisibility(View.GONE);
            }
        }
    }

    private void setupButtons() {
        bind.bottomNav.setOnItemSelectedListener(item -> {

            // Trigger the custom squish animation
            View navItemView = bind.bottomNav.findViewById(item.getItemId());
            if (navItemView != null) {
                navItemView.animate().cancel();

                navItemView.animate()
                        .scaleX(0.9f)
                        .scaleY(0.8f)
                        .setDuration(80)
                        .setInterpolator(new android.view.animation.AccelerateInterpolator())
                        .withEndAction(() -> {
                            navItemView.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(180)
                                    .setInterpolator(new android.view.animation.OvershootInterpolator(2f))
                                    .start();
                        })
                        .start();
            }

            // Handle fragment routing
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new DashboardFragment();
            } else if (itemId == R.id.nav_search) {
                selectedFragment = new SearchFragment();
            } else if (itemId == R.id.nav_concerto) {
                boolean isInActiveConcerto = concertoViewModel.getActiveSessionPin().getValue() != null;
                selectedFragment = isInActiveConcerto ? new ConcertoFragment() : new ConcertoLobbyFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutFragmentContainer, selectedFragment)
                        .commitAllowingStateLoss();
                return true;
            }
            return false;
        });
    }

    private void setupNetworkMonitor() {
        networkMonitor = NetworkMonitor.getInstance(this);

        networkMonitor.getIsConnected().observe(this, isConnected -> {
            if (isConnected) {
                bind.tvNetworkBanner.setVisibility(View.GONE);
            } else {
                bind.tvNetworkBanner.setVisibility(View.VISIBLE);
                bind.tvNetworkBanner.setText("No Internet Connection");
            }
        });
    }
}