package com.example.concerto.profile;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.concerto.R;
import com.example.concerto.adapters.HostedConcertoAdapter;
import com.example.concerto.auth.AuthViewModel;
import com.example.concerto.auth.ConnectSpotifyFragment;
import com.example.concerto.auth.LoginFragment;
import com.example.concerto.concerto.ConcertoFragment;
import com.example.concerto.concerto.ConcertoViewModel;
import com.example.concerto.databinding.FragmentProfileBinding;
import com.example.concerto.player.PlayerViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding bind;
    private AuthViewModel authViewModel;
    private ProfileViewModel profileViewModel;
    private PlayerViewModel playerViewModel;
    private ConcertoViewModel concertoViewModel;
    private HostedConcertoAdapter adapter;

    private boolean isLoggingOut = false;
    private boolean isAttemptingToJoin = false;
    // Prevents ProfileFragment.onDestroyView() from flashing the nav bar when
    // navigating to another no-nav fragment (e.g. ConnectSpotify)
    private boolean isNavigatingToNoNavFragment = false;

    // Camera photo URI (needed when using camera intent)
    private Uri cameraPhotoUri;

    private final DatabaseReference usersDb = FirebaseDatabase
            .getInstance("https://concerto-b02f9-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("users");

    // ==========================================
    // ACTIVITY RESULT LAUNCHERS
    // ==========================================

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchCamera();
                } else {
                    Toast.makeText(requireContext(),
                            "Camera permission is required to take a photo.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> storagePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchGallery();
                } else {
                    Toast.makeText(requireContext(),
                            "Storage permission is required to choose a photo.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && cameraPhotoUri != null) {
                    processAndUploadImage(cameraPhotoUri);
                }
            });

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedUri = result.getData().getData();
                    if (selectedUri != null) {
                        processAndUploadImage(selectedUri);
                    }
                }
            });

    // ==========================================
    // FRAGMENT LIFECYCLE
    // ==========================================

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        bind = FragmentProfileBinding.inflate(inflater, container, false);
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViewModels();
        setupUI();
        setupObservers();
        setupButtons();

        profileViewModel.fetchUserConcertos();
        loadProfileImage();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }

    // ==========================================
    // INITIALIZATION
    // ==========================================

    private void initViewModels() {
        if (getActivity() == null) return;
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        playerViewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        concertoViewModel = new ViewModelProvider(requireActivity()).get(ConcertoViewModel.class);
    }

    private void setupUI() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            bind.tvUsername.setText(authViewModel.getCurrentUsername());
            bind.tvEmail.setText(user.getEmail());
        }

        adapter = new HostedConcertoAdapter((pin, isActive) -> {
            if (!isActive) {
                if (isAdded()) Toast.makeText(getContext(), "Session already ended", Toast.LENGTH_SHORT).show();
                return;
            }
            isAttemptingToJoin = true;
            concertoViewModel.joinHostedConcerto(pin);
            if (isAdded()) Toast.makeText(getContext(), "Joining Room: " + pin, Toast.LENGTH_SHORT).show();
        });

        bind.rvHostedConcertos.setLayoutManager(new LinearLayoutManager(requireContext()));
        bind.rvHostedConcertos.setAdapter(adapter);
    }

    private void setupObservers() {
        authViewModel.getSpotifyToken().observe(getViewLifecycleOwner(), token -> {
            if (bind == null || !isAdded()) return;
            boolean isConnected = (token != null && !token.trim().isEmpty());
            if (isConnected) {
                bind.tvSpotifyStatus.setText("Spotify: Connected");
                bind.tvSpotifyStatus.setTextColor(android.graphics.Color.parseColor("#7C72E0"));
                bind.btnConnectSpotify.setVisibility(View.GONE);
            } else {
                bind.tvSpotifyStatus.setText("Spotify: Disconnected");
                bind.tvSpotifyStatus.setTextColor(android.graphics.Color.parseColor("#FF5555"));
                bind.btnConnectSpotify.setVisibility(View.VISIBLE);
            }
        });

        profileViewModel.getHostedConcertos().observe(getViewLifecycleOwner(), concertos -> {
            if (bind == null || !isAdded()) return;
            if (concertos != null && !concertos.isEmpty()) {
                adapter.setConcertos(concertos);
            }
        });

        concertoViewModel.getActiveSessionPin().observe(getViewLifecycleOwner(), pin -> {
            if (pin != null && isAttemptingToJoin && isAdded() && getActivity() != null) {
                isAttemptingToJoin = false;
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutFragmentContainer, new ConcertoFragment())
                        .commitAllowingStateLoss();
            }
        });

        concertoViewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty() && isAdded()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                concertoViewModel.clearToastMessage();
            }
        });
    }

    private void setupButtons() {
        bind.frameProfileImage.setOnClickListener(v -> showPhotoOptionsDialog());

        bind.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNav);
                if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_home);
            }
        });

        bind.btnSettings.setOnClickListener(v -> {
            if (isAdded()) Toast.makeText(requireContext(), "Settings coming soon!", Toast.LENGTH_SHORT).show();
        });

        bind.btnEditProfile.setOnClickListener(v -> {
            if (isAdded()) Toast.makeText(requireContext(), "Edit Profile coming soon!", Toast.LENGTH_SHORT).show();
        });

        bind.btnConnectSpotify.setOnClickListener(v -> {
            if (getActivity() != null) {
                isNavigatingToNoNavFragment = true;
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutFragmentContainer, new ConnectSpotifyFragment())
                        .addToBackStack(null)
                        .commitAllowingStateLoss();
            }
        });

        bind.btnLogout.setOnClickListener(v -> {
            isLoggingOut = true;
            playerViewModel.pausePlayer();
            playerViewModel.reset();
            concertoViewModel.leaveConcerto();

            if (getActivity() != null) {
                androidx.fragment.app.Fragment playerFragment =
                        getActivity().getSupportFragmentManager().findFragmentByTag("PLAYER");
                if (playerFragment != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .remove(playerFragment)
                            .commitAllowingStateLoss();
                }

                authViewModel.logoutFull();
                getActivity().getSupportFragmentManager().popBackStack(
                        null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layoutFragmentContainer, new LoginFragment())
                        .commitAllowingStateLoss();
            }
        });
    }

    // ==========================================
    // PROFILE IMAGE — DIALOG
    // ==========================================

    private void showPhotoOptionsDialog() {
        // "Remove Photo" only appears if the user already has a custom picture in DB
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Check DB for existing photo to decide whether to show "Remove" option
        usersDb.child(user.getUid()).child("profileImageBase64").get()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    boolean hasPhoto = task.isSuccessful()
                            && task.getResult().exists()
                            && task.getResult().getValue() != null;

                    String[] options = hasPhoto
                            ? new String[]{"Take Photo", "Choose from Gallery", "Remove Photo"}
                            : new String[]{"Take Photo", "Choose from Gallery"};

                    // FIX: Use MaterialAlertDialogBuilder with a forced dark theme
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                            requireContext(),
                            android.R.style.Theme_DeviceDefault_Dialog_Alert)
                            .setTitle("Profile Photo")
                            .setItems(options, (dialog, which) -> {
                                if (which == 0) checkCameraPermissionAndLaunch();
                                else if (which == 1) checkGalleryPermissionAndLaunch();
                                else if (which == 2) removeProfilePhoto();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
    }

    // ==========================================
    // PERMISSIONS
    // ==========================================

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void checkGalleryPermissionAndLaunch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launchGallery(); // Android 13+ photo picker needs no permission
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                launchGallery();
            } else {
                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    // ==========================================
    // CAMERA & GALLERY LAUNCH
    // ==========================================

    private void launchCamera() {
        try {
            File photoFile = createImageFile();
            cameraPhotoUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
            cameraLauncher.launch(intent);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Failed to create image file.", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("PROFILE_" + timeStamp, ".jpg", storageDir);
    }

    // ==========================================
    // IMAGE PROCESSING: CROP → COMPRESS → BASE64 → DATABASE
    // ==========================================

    private void processAndUploadImage(Uri imageUri) {
        new Thread(() -> {
            try {
                // 1. Decode with inSampleSize to avoid OOM on large camera photos
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                try (InputStream probe = requireContext().getContentResolver().openInputStream(imageUri)) {
                    BitmapFactory.decodeStream(probe, null, options);
                }

                options.inSampleSize = calculateInSampleSize(options, 512, 512);
                options.inJustDecodeBounds = false;

                Bitmap raw;
                try (InputStream stream = requireContext().getContentResolver().openInputStream(imageUri)) {
                    raw = BitmapFactory.decodeStream(stream, null, options);
                }

                if (raw == null) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Failed to read image.", Toast.LENGTH_SHORT).show());
                    return;
                }

                // 2. Square-crop from center
                Bitmap squared = squareCrop(raw);

                // 3. Circle-crop at 256px
                Bitmap circular = circleCrop(squared, 256);

                // 4. Compress to JPEG at 75% quality — keeps it well under 50KB
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                circular.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                byte[] imageBytes = baos.toByteArray();

                // 5. Encode to Base64 string for Realtime Database storage
                String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

                requireActivity().runOnUiThread(() -> saveBase64ToDatabase(base64Image, circular));

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Image processing failed.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private Bitmap squareCrop(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        int x = (bitmap.getWidth() - size) / 2;
        int y = (bitmap.getHeight() - size) / 2;
        return Bitmap.createBitmap(bitmap, x, y, size, size);
    }

    private Bitmap circleCrop(Bitmap bitmap, int outputSize) {
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, outputSize, outputSize, true);
        Bitmap output = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(new BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        float radius = outputSize / 2f;
        canvas.drawCircle(radius, radius, radius, paint);
        return output;
    }

    // ==========================================
    // SAVE TO FIREBASE REALTIME DATABASE
    // ==========================================

    private void saveBase64ToDatabase(String base64Image, Bitmap previewBitmap) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Show preview immediately — instant UX feedback before DB write completes
        if (bind != null && isAdded()) {
            bind.ivProfileImage.setImageBitmap(previewBitmap);
        }

        usersDb.child(user.getUid()).child("profileImageBase64").setValue(base64Image)
                .addOnSuccessListener(unused -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                "Profile picture updated!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                "Failed to save photo. Check your connection.", Toast.LENGTH_SHORT).show();
                        // Revert the preview on failure
                        loadProfileImage();
                    }
                });
    }

    // ==========================================
    // REMOVE PHOTO
    // ==========================================

    private void removeProfilePhoto() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        usersDb.child(user.getUid()).child("profileImageBase64").removeValue()
                .addOnCompleteListener(task -> {
                    if (isAdded() && bind != null) {
                        // FIX: Updated to custom placeholder
                        bind.ivProfileImage.setImageResource(R.drawable.img_profile_placeholder);
                        Toast.makeText(requireContext(),
                                "Profile photo removed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ==========================================
    // LOAD EXISTING PROFILE IMAGE FROM DATABASE
    // ==========================================

    private void loadProfileImage() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || bind == null || !isAdded()) return;

        usersDb.child(user.getUid()).child("profileImageBase64").get()
                .addOnCompleteListener(task -> {
                    if (!isAdded() || bind == null) return;

                    if (task.isSuccessful() && task.getResult().exists()) {
                        String base64 = task.getResult().getValue(String.class);
                        if (base64 != null && !base64.isEmpty()) {
                            // Decode on background thread to avoid blocking main thread
                            new Thread(() -> {
                                byte[] bytes = Base64.decode(base64, Base64.NO_WRAP);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                if (bitmap != null && isAdded() && bind != null) {
                                    requireActivity().runOnUiThread(() -> {
                                        if (bind != null) {
                                            // Use Glide's CircleCrop for the display transform
                                            Glide.with(this)
                                                    .load(bytes)
                                                    .transform(new CircleCrop())
                                                    .into(bind.ivProfileImage);
                                        }
                                    });
                                }
                            }).start();
                            return;
                        }
                    }
                    // FIX: Updated to custom placeholder
                    bind.ivProfileImage.setImageResource(R.drawable.img_profile_placeholder);
                });
    }
}