package com.example.concerto;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.concerto.auth.AuthViewModel;
import com.example.concerto.auth.LoginFragment;
import com.example.concerto.concerto.ConcertoFragment;
import com.example.concerto.dashboard.DashboardFragment;
import com.example.concerto.databinding.ActivityMainBinding;
import com.example.concerto.player.PlayerFragment;
import com.example.concerto.profile.ProfileFragment;
import com.example.concerto.search.SearchFragment;
import com.spotify.sdk.android.auth.AuthorizationResponse;

public class MainActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private ActivityMainBinding bind;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);

        bind = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(bind.getRoot());

        initViewModels();
        setupUI(savedInstanceState);
        setupButtons(); // Handles Nav Bar clicks
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

    private void initViewModels() {
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
    }

    private void setupUI(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            if (authViewModel.isUserLoggedIn()) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutFragmentContainer, new DashboardFragment(), null)
                        .commit();

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutPlayerSheetContainer, new PlayerFragment(), "PLAYER")
                        .commit();

                bind.bottomNav.setVisibility(View.VISIBLE);
            } else {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutFragmentContainer, new LoginFragment(), null)
                        .commit();

                bind.bottomNav.setVisibility(View.GONE);
            }
        }
    }

    private void setupButtons() {
        bind.bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new DashboardFragment();
            } else if (itemId == R.id.nav_search) {
                selectedFragment = new SearchFragment(); // Next feature!
            } else if (itemId == R.id.nav_concerto) {
                selectedFragment = new ConcertoFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutFragmentContainer, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });
    }
}