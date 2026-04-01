package com.example.concerto;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;

import com.example.concerto.auth.AuthViewModel;
import com.example.concerto.databinding.ActivityMainBinding;
import com.example.concerto.player.PlayerFragment;
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

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        if (savedInstanceState == null) {
            if (authViewModel.isUserLoggedIn()) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new DashboardFragment(), null)
                        .commit();

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.player_sheet_container, new PlayerFragment(), "PLAYER")
                        .commit();
            } else {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new LoginFragment(), null)
                        .commit();
            }
        }
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

                // Clean delegation straight to the ViewModel
                authViewModel.exchangeCodeForToken(code, verifier);

            } else if (type == AuthorizationResponse.Type.ERROR) {
                Log.e("SpotifyAuth", "Auth error: " + response.getError());
            }
        }
    }
}