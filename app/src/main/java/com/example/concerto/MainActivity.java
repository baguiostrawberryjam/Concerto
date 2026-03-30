package com.example.concerto;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.concerto.auth.AuthViewModel; // Ensure this import is correct
import com.example.concerto.databinding.ActivityMainBinding;
import com.example.concerto.player.PlayerFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class MainActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private ActivityMainBinding bind;

    private static final String CLIENT_ID = "a75d7664e93f41d4863ea21af859cb34";
    private static final String REDIRECT_URI = "concerto-app://callback";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        if (savedInstanceState == null) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
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

                tradeCodeForToken(code, verifier);

            } else if (type == AuthorizationResponse.Type.ERROR) {
                Log.e("SpotifyAuth", "Auth error: " + response.getError());
            }
        }
    }

    private void tradeCodeForToken(String code, String verifier) {
        new Thread(() -> {
            try {
                URL url = new URL("https://accounts.spotify.com/api/token");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setDoOutput(true);

                String body = "client_id=" + CLIENT_ID +
                        "&grant_type=authorization_code" +
                        "&code=" + code +
                        "&redirect_uri=" + REDIRECT_URI +
                        "&code_verifier=" + verifier;

                OutputStream os = conn.getOutputStream();
                os.write(body.getBytes());
                os.flush();
                os.close();

                // Read response from Spotify
                InputStream is = conn.getInputStream();
                Scanner scanner = new Scanner(is).useDelimiter("\\A");
                String responseBody = scanner.hasNext() ? scanner.next() : "";

                // Extract token from the JSON
                JSONObject jsonObject = new JSONObject(responseBody);

                String accessToken = jsonObject.getString("access_token");
                String refreshToken = jsonObject.optString("refresh_token");
                int expiresIn = jsonObject.getInt("expires_in");

                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference db = FirebaseDatabase.getInstance().getReference();

                Map<String, Object> spotifyData = new HashMap<>();

                spotifyData.put("accessToken", accessToken);
                spotifyData.put("expiresAt", System.currentTimeMillis() + (expiresIn * 1000));

                if (!refreshToken.isEmpty()) {
                    spotifyData.put("refreshToken", refreshToken);
                }

                db.child("users")
                        .child(uid)
                        .child("spotify")
                        .updateChildren(spotifyData);

                authViewModel.setSpotifyToken(accessToken);

                Log.d("SpotifyAuth", "SUCCESS! Token secured using PKCE.");
            } catch (Exception e) {
                Log.e("SpotifyAuth", "Failed to trade code for token: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
}