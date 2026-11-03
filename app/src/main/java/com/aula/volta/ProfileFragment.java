package com.aula.volta;

import android.content.Context;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.AuthAPI;
import com.aula.volta.data.api.ReportAPI;
import com.aula.volta.data.model.EsgMetricResponseDTO;
import com.aula.volta.data.model.UserResponseDTO;
import com.aula.volta.data.local.NotificationStore;
import com.aula.volta.data.local.OccurrenceStore;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.local.SessionManager;
import com.aula.volta.data.model.Notification;
import com.aula.volta.data.model.Occurrence;
import com.aula.volta.data.upload.CloudinaryUploader;
import com.aula.volta.ui.dialog.LogoutDialog;
import com.aula.volta.ui.dialog.ProfilePhotoSheet;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private static final String PREFS_REPORTS = "cache_reports";
    private static final String KEY_SUMMARY = "summary";

    private static final String TAG = "ProfileFragment";

    private TextView tvProfileInitials;
    private ImageView imgProfilePhoto;
    private ImageView imgProfileCameraBadge;
    private View avatarContainer;
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvProfileUnit;
    private TextView tvProfileUnitRow;
    private TextView tvOccurrences;
    private TextView tvRecovered;
    private TextView tvNotifCount;
    private Bitmap currentProfileBitmap;

    private File cameraTempFile;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;

    public ProfileFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(), success -> {
                    if (success && cameraTempFile != null && cameraTempFile.exists()) {
                        processAndSaveProfilePhoto(cameraTempFile);
                    }
                });

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        File cached = copyUriToProfileCache(uri);
                        if (cached != null) {
                            processAndSaveProfilePhoto(cached);
                        }
                    }
                });

        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        launchCamera();
                    } else {
                        Toast.makeText(requireContext(),
                                R.string.profile_photo_camera_perm_required,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvProfileInitials = view.findViewById(R.id.tvProfileInitials);
        imgProfilePhoto = view.findViewById(R.id.imgProfilePhoto);
        imgProfileCameraBadge = view.findViewById(R.id.imgProfileCameraBadge);
        avatarContainer = view.findViewById(R.id.avatarContainer);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfileUnit = view.findViewById(R.id.tvProfileUnit);
        tvProfileUnitRow = view.findViewById(R.id.tvProfileUnitRow);
        tvOccurrences = view.findViewById(R.id.tvProfileOccurrences);
        tvRecovered = view.findViewById(R.id.tvProfileRecovered);
        tvNotifCount = view.findViewById(R.id.tvProfileNotifCount);

        if (tvOccurrences != null) tvOccurrences.setText("—");
        if (tvRecovered != null) tvRecovered.setText("—");
        if (tvNotifCount != null) tvNotifCount.setText("—");

        bindUserData();

        View.OnClickListener openPhotoPicker = v -> showPhotoOptionsSheet();
        if (avatarContainer != null) {
            avatarContainer.setOnClickListener(v -> {
                if (hasActivePhoto()) {
                    showFullPhotoDialog();
                } else {
                    showPhotoOptionsSheet();
                }
            });
        }
        if (imgProfileCameraBadge != null) {
            imgProfileCameraBadge.setOnClickListener(openPhotoPicker);
        }

        View btnEdit = view.findViewById(R.id.btnEditProfile);
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v ->
                    Navigation.findNavController(v).navigate(R.id.nav_edit_profile));
        }

        view.findViewById(R.id.rowProfileNotifications).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.nav_notification_settings));

        view.findViewById(R.id.rowProfileUnit).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.nav_my_unit));

        view.findViewById(R.id.rowProfileHelp).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.nav_assistant));

        view.findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutDialog());

        getParentFragmentManager().setFragmentResultListener("profile_result", getViewLifecycleOwner(), (requestKey, result) -> {
            if (result.getBoolean("profileSaved", false)) {
                bindUserData();
                loadProfilePhoto();
                showFeedbackToast("Dados salvos", "Alterações salvas com sucesso");
            } else if (result.getBoolean("passwordChanged", false)) {
                showFeedbackToast("Senha alterada", "Sua senha foi atualizada com sucesso");
            }
        });

        loadNumbers();
        loadNotifCount();
        fetchUserProfileFromDatabase();
    }

    private void bindUserData() {
        if (!isAdded()) {
            return;
        }

        String name = SessionManager.name(requireContext());
        if (name == null || name.trim().isEmpty()) {
            name = "Colaborador";
        }
        if (tvProfileName != null) {
            tvProfileName.setText(name);
        }

        if (tvProfileInitials != null) {
            String initials = "US";
            String[] parts = name.trim().split("\\s+");
            if (parts.length >= 2) {
                initials = ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase(Locale.ROOT);
            } else if (parts.length == 1 && !parts[0].isEmpty()) {
                initials = parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
            }
            tvProfileInitials.setText(initials);
        }

        String email = SessionManager.email(requireContext());
        if (email == null || email.trim().isEmpty()) {
            email = getString(R.string.profile_email_value);
        }
        if (tvProfileEmail != null) {
            tvProfileEmail.setText(email);
        }

        loadProfilePhoto();

        String sector = SessionManager.sector(requireContext());
        if (sector == null || sector.trim().isEmpty()) {
            sector = getString(R.string.profile_unit);
        }
        if (tvProfileUnit != null) {
            tvProfileUnit.setText(sector);
        }
        if (tvProfileUnitRow != null) {
            String companyName = SessionManager.getCompanyName(requireContext());
            if (companyName != null && !companyName.trim().isEmpty()) {
                tvProfileUnitRow.setText(companyName.trim());
            } else {
                String shortSector = sector;
                if (shortSector.contains("—")) {
                    shortSector = shortSector.split("—")[0].trim();
                } else if (shortSector.contains("·")) {
                    shortSector = shortSector.split("·")[0].trim();
                }
                tvProfileUnitRow.setText(shortSector);
            }
        }

        try {
            AuthAPI authApi = ApiClient.get(requireContext()).create(AuthAPI.class);
            authApi.getMyCompany().enqueue(new Callback<com.aula.volta.data.model.CompanyResponseDTO>() {
                @Override
                public void onResponse(@NonNull Call<com.aula.volta.data.model.CompanyResponseDTO> call,
                                       @NonNull Response<com.aula.volta.data.model.CompanyResponseDTO> compResp) {
                    if (!isAdded()) return;
                    if (compResp.isSuccessful() && compResp.body() != null) {
                        com.aula.volta.data.model.CompanyResponseDTO c = compResp.body();
                        if (c.getId() != null && !c.getId().trim().isEmpty()) {
                            SessionManager.saveCompanyId(requireContext(), c.getId().trim());
                        }
                        SessionManager.saveCompany(requireContext(), c.getName(), c.getFormattedCnpj(), c.getAddress());
                        if (tvProfileUnitRow != null && c.getName() != null && !c.getName().trim().isEmpty()) {
                            tvProfileUnitRow.setText(c.getName().trim());
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<com.aula.volta.data.model.CompanyResponseDTO> call, @NonNull Throwable t) {
                }
            });
            authApi.getMe().enqueue(new Callback<UserResponseDTO>() {
                @Override
                public void onResponse(@NonNull Call<UserResponseDTO> call,
                                       @NonNull Response<UserResponseDTO> response) {
                    if (!isAdded()) {
                        return;
                    }
                    if (response.isSuccessful() && response.body() != null) {
                        UserResponseDTO u = response.body();
                        String myEmail = SessionManager.email(requireContext());
                        if (myEmail.isEmpty() || (u.getEmail() != null && !u.getEmail().trim().isEmpty() && !u.getEmail().equalsIgnoreCase(myEmail))) {
                            return;
                        }
                        if (u.getId() != null && !u.getId().trim().isEmpty()) {
                            SessionManager.saveUserId(requireContext(), u.getId().trim());
                        }
                        if (u.getCompanyId() != null && !u.getCompanyId().trim().isEmpty()) {
                            SessionManager.saveCompanyId(requireContext(), u.getCompanyId().trim());
                        }
                        if (u.getName() != null && !u.getName().isEmpty()) {
                            SessionManager.saveName(requireContext(), u.getName());
                            if (tvProfileName != null) {
                                tvProfileName.setText(u.getName());
                            }
                            applyInitials(u.getName());
                        }
                        if (u.getEmail() != null && !u.getEmail().isEmpty()) {
                            SessionManager.saveEmail(requireContext(), u.getEmail());
                            if (tvProfileEmail != null) {
                                tvProfileEmail.setText(u.getEmail());
                            }
                        }
                        if (u.getPosition() != null && !u.getPosition().isEmpty()) {
                            SessionManager.saveSector(requireContext(), u.getPosition());
                            if (tvProfileUnit != null) {
                                tvProfileUnit.setText(u.getPosition());
                            }
                            if (tvProfileUnitRow != null && SessionManager.getCompanyName(requireContext()).isEmpty()) {
                                tvProfileUnitRow.setText(u.getPosition());
                            }
                        }
                        if (u.getPhotoUrl() != null && !u.getPhotoUrl().isEmpty()) {
                            String current = SessionManager.getProfilePhotoUrl(requireContext());
                            if (!u.getPhotoUrl().equals(current)) {
                                SessionManager.saveProfilePhotoUrl(requireContext(), u.getPhotoUrl());
                                loadProfilePhoto();
                            }
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<UserResponseDTO> call, @NonNull Throwable t) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void applyInitials(String name) {
        if (tvProfileInitials != null && name != null) {
            String initials = "US";
            String[] parts = name.trim().split("\\s+");
            if (parts.length >= 2) {
                initials = ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase(Locale.ROOT);
            } else if (parts.length == 1 && !parts[0].isEmpty()) {
                initials = parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
            }
            tvProfileInitials.setText(initials);
        }
    }

    private void showPhotoOptionsSheet() {
        if (!isAdded()) return;
        boolean hasPhoto = false;
        String localPath = SessionManager.getProfilePhotoLocal(requireContext());
        if (localPath != null && !localPath.isEmpty() && new File(localPath).exists()) {
            hasPhoto = true;
        } else {
            String remoteUrl = SessionManager.getProfilePhotoUrl(requireContext());
            if (remoteUrl != null && !remoteUrl.isEmpty()) {
                hasPhoto = true;
            }
        }

        ProfilePhotoSheet sheet = ProfilePhotoSheet.newInstance(hasPhoto);
        sheet.setOnPhotoOptionSelectedListener(new ProfilePhotoSheet.OnPhotoOptionSelectedListener() {
            @Override
            public void onCameraSelected() {
                checkCameraPermissionAndLaunch();
            }

            @Override
            public void onGallerySelected() {
                pickImageLauncher.launch("image/*");
            }

            @Override
            public void onRemoveSelected() {
                removeProfilePhoto();
            }
        });
        sheet.show(getChildFragmentManager(), "profile_photo_sheet");
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            cameraTempFile = File.createTempFile("profile_cam_", ".jpg", requireContext().getCacheDir());
            Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", cameraTempFile);
            takePictureLauncher.launch(uri);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Não foi possível abrir a câmera", Toast.LENGTH_SHORT).show();
        }
    }

    private File copyUriToProfileCache(Uri uri) {
        try {
            File out = new File(requireContext().getCacheDir(), "profile_pick_" + System.currentTimeMillis() + ".jpg");
            try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
                 FileOutputStream fos = new FileOutputStream(out)) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, n);
                }
            }
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    private void processAndSaveProfilePhoto(File sourceFile) {
        if (sourceFile == null || !sourceFile.exists()) {
            return;
        }
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(sourceFile.getAbsolutePath());
            if (bitmap == null) {
                Toast.makeText(requireContext(), "Erro ao processar imagem", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                ExifInterface exif = new ExifInterface(sourceFile.getAbsolutePath());
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                int degrees = 0;
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90) degrees = 90;
                else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) degrees = 180;
                else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) degrees = 270;
                if (degrees != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(degrees);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                }
            } catch (Exception ignored) {
            }

            int minDim = Math.min(bitmap.getWidth(), bitmap.getHeight());
            int cropX = (bitmap.getWidth() - minDim) / 2;
            int cropY = (bitmap.getHeight() - minDim) / 2;
            Bitmap square = Bitmap.createBitmap(bitmap, cropX, cropY, minDim, minDim);
            Bitmap finalAvatar = Bitmap.createScaledBitmap(square, 512, 512, true);

            String myEmail = SessionManager.email(requireContext());
            String cleanEmail = myEmail != null ? myEmail.trim().toLowerCase(Locale.ROOT) : "";
            File avatarFile = !cleanEmail.isEmpty()
                    ? new File(requireContext().getFilesDir(), "profile_photo_" + Math.abs(cleanEmail.hashCode()) + ".jpg")
                    : new File(requireContext().getFilesDir(), "profile_photo.jpg");
            try (FileOutputStream fos = new FileOutputStream(avatarFile)) {
                finalAvatar.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            }

            SessionManager.saveProfilePhotoLocal(requireContext(), myEmail, avatarFile.getAbsolutePath());

            displayCircularProfileBitmap(finalAvatar);

            Toast.makeText(requireContext(), R.string.profile_photo_uploading, Toast.LENGTH_SHORT).show();

            final Context appContext = requireContext().getApplicationContext();

            CloudinaryUploader.uploadProfileImage(appContext, avatarFile, myEmail, new CloudinaryUploader.UploadCallback() {
                @Override
                public void onSuccess(String remoteUrl, String publicId) {
                    SessionManager.saveProfilePhotoUrl(appContext, myEmail, remoteUrl);
                    SessionManager.saveProfilePhotoLocal(appContext, myEmail, avatarFile.getAbsolutePath());
                    Log.i(TAG, "Foto de perfil enviada ao Cloudinary com sucesso: " + remoteUrl);

                    try {
                        AuthAPI authApi = ApiClient.get(appContext).create(AuthAPI.class);
                        JsonObject body = new JsonObject();
                        body.addProperty("name", SessionManager.name(appContext));
                        body.addProperty("position", SessionManager.sector(appContext));
                        body.addProperty("photoUrl", remoteUrl);
                        body.addProperty("photo_url", remoteUrl);
                        body.addProperty("avatarUrl", remoteUrl);
                        body.addProperty("avatar_url", remoteUrl);
                        body.addProperty("imageUrl", remoteUrl);
                        body.addProperty("image_url", remoteUrl);
                        body.addProperty("foto_url", remoteUrl);
                        body.addProperty("pictureUrl", remoteUrl);
                        authApi.updateMe(body).enqueue(new Callback<UserResponseDTO>() {
                            @Override
                            public void onResponse(@NonNull Call<UserResponseDTO> call, @NonNull Response<UserResponseDTO> response) {
                                Log.i(TAG, "Foto de perfil salva no banco oficial (PUT /users/me): " + response.code());
                            }

                            @Override
                            public void onFailure(@NonNull Call<UserResponseDTO> call, @NonNull Throwable t) {
                                Log.w(TAG, "Falha ao salvar foto de perfil no banco oficial: " + t.getMessage());
                            }
                        });
                    } catch (Exception e) {
                        Log.w(TAG, "Erro ao chamar updateMe: " + e.getMessage());
                    }

                    if (isAdded()) {
                        Toast.makeText(requireContext(), R.string.profile_photo_success, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(Exception exception, String fallbackLocalPath) {
                    Log.w(TAG, "Falha no upload da foto de perfil no Cloudinary: " + exception.getMessage());
                    if (isAdded()) {
                        Toast.makeText(requireContext(), R.string.profile_photo_offline, Toast.LENGTH_SHORT).show();
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar foto de perfil", e);
            Toast.makeText(requireContext(), "Erro ao processar imagem", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayCircularProfileBitmap(Bitmap bmp) {
        currentProfileBitmap = bmp;
        if (imgProfilePhoto != null && isAdded()) {
            RoundedBitmapDrawable circular = RoundedBitmapDrawableFactory.create(getResources(), bmp);
            circular.setCircular(true);
            imgProfilePhoto.setImageDrawable(circular);
            imgProfilePhoto.setVisibility(View.VISIBLE);
            if (tvProfileInitials != null) {
                tvProfileInitials.setVisibility(View.GONE);
            }
        }
    }

    private boolean hasActivePhoto() {
        if (!isAdded()) return false;
        if (currentProfileBitmap != null) return true;
        String localPath = SessionManager.getProfilePhotoLocal(requireContext());
        if (localPath != null && !localPath.isEmpty() && new File(localPath).exists()) {
            return true;
        }
        String remoteUrl = SessionManager.getProfilePhotoUrl(requireContext());
        return remoteUrl != null && !remoteUrl.isEmpty() && remoteUrl.startsWith("http");
    }

    private void showFullPhotoDialog() {
        if (!isAdded()) return;
        android.app.Dialog dialog = new android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        FrameLayout root = new FrameLayout(requireContext());
        root.setBackgroundColor(Color.parseColor("#E6000000"));

        ImageView ivFull = new ImageView(requireContext());
        FrameLayout.LayoutParams ivParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        ivFull.setLayoutParams(ivParams);
        ivFull.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ivFull.setPadding(32, 90, 32, 140);

        if (currentProfileBitmap != null) {
            ivFull.setImageBitmap(currentProfileBitmap);
        } else {
            String localPath = SessionManager.getProfilePhotoLocal(requireContext());
            if (localPath != null && new File(localPath).exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(localPath);
                if (bmp != null) {
                    currentProfileBitmap = bmp;
                    ivFull.setImageBitmap(bmp);
                }
            } else {
                String remoteUrl = SessionManager.getProfilePhotoUrl(requireContext());
                if (remoteUrl != null && remoteUrl.startsWith("http")) {
                    CloudinaryUploader.fetchRemoteBitmap(remoteUrl, new CloudinaryUploader.BitmapCallback() {
                        @Override
                        public void onBitmapLoaded(Bitmap bmp) {
                            if (dialog.isShowing() && bmp != null && isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    currentProfileBitmap = bmp;
                                    ivFull.setImageBitmap(bmp);
                                });
                            }
                        }
                        @Override
                        public void onError(Exception e) {}
                    });
                }
            }
        }
        root.addView(ivFull);

        ImageView btnClose = new ImageView(requireContext());
        int btnSize = (int) (44 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(btnSize, btnSize);
        closeParams.gravity = Gravity.TOP | Gravity.END;
        closeParams.setMargins(0, (int) (36 * getResources().getDisplayMetrics().density),
                (int) (20 * getResources().getDisplayMetrics().density), 0);
        btnClose.setLayoutParams(closeParams);
        int pad = (int) (10 * getResources().getDisplayMetrics().density);
        btnClose.setPadding(pad, pad, pad, pad);
        btnClose.setImageResource(R.drawable.ic_close);
        btnClose.setColorFilter(Color.WHITE);
        btnClose.setBackgroundResource(R.drawable.bg_circle_camera_badge);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        root.addView(btnClose);

        TextView btnChange = new TextView(requireContext());
        FrameLayout.LayoutParams changeParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                (int) (48 * getResources().getDisplayMetrics().density));
        changeParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        changeParams.setMargins(0, 0, 0, (int) (36 * getResources().getDisplayMetrics().density));
        btnChange.setLayoutParams(changeParams);
        btnChange.setGravity(Gravity.CENTER);
        btnChange.setText("Alterar foto");
        btnChange.setTextColor(Color.WHITE);
        btnChange.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        btnChange.setTypeface(null, Typeface.BOLD);
        btnChange.setPadding((int) (28 * getResources().getDisplayMetrics().density), 0,
                (int) (28 * getResources().getDisplayMetrics().density), 0);
        btnChange.setBackgroundResource(R.drawable.bg_button_gradient);
        btnChange.setOnClickListener(v -> {
            dialog.dismiss();
            showPhotoOptionsSheet();
        });
        root.addView(btnChange);

        dialog.setContentView(root);
        dialog.show();
    }

    private void removeProfilePhoto() {
        currentProfileBitmap = null;
        Context appContext = requireContext().getApplicationContext();
        String email = SessionManager.email(appContext);
        String cleanEmail = email != null ? email.trim().toLowerCase(Locale.ROOT) : "";
        File avatarFile = new File(appContext.getFilesDir(), "profile_photo.jpg");
        if (avatarFile.exists()) {
            avatarFile.delete();
        }
        if (!cleanEmail.isEmpty()) {
            File hashedFile = new File(appContext.getFilesDir(), "profile_photo_" + Math.abs(cleanEmail.hashCode()) + ".jpg");
            if (hashedFile.exists()) {
                hashedFile.delete();
            }
        }
        SessionManager.clearProfilePhoto(appContext);
        if (imgProfilePhoto != null) {
            imgProfilePhoto.setImageDrawable(null);
            imgProfilePhoto.setVisibility(View.GONE);
        }
        if (tvProfileInitials != null) {
            tvProfileInitials.setVisibility(View.VISIBLE);
        }

        try {
            AuthAPI authApi = ApiClient.get(appContext).create(AuthAPI.class);
            JsonObject body = new JsonObject();
            body.addProperty("name", SessionManager.name(appContext));
            body.addProperty("position", SessionManager.sector(appContext));
            body.addProperty("photoUrl", "");
            body.addProperty("photo_url", "");
            body.addProperty("avatarUrl", "");
            body.addProperty("avatar_url", "");
            body.addProperty("imageUrl", "");
            body.addProperty("image_url", "");
            body.addProperty("foto_url", "");
            body.addProperty("pictureUrl", "");
            authApi.updateMe(body).enqueue(new Callback<UserResponseDTO>() {
                @Override
                public void onResponse(@NonNull Call<UserResponseDTO> call, @NonNull Response<UserResponseDTO> response) {
                    Log.i(TAG, "Foto de perfil removida do banco oficial.");
                }

                @Override
                public void onFailure(@NonNull Call<UserResponseDTO> call, @NonNull Throwable t) {
                }
            });
        } catch (Exception ignored) {
        }

        Toast.makeText(requireContext(), R.string.profile_photo_removed, Toast.LENGTH_SHORT).show();
    }

    private void loadProfilePhoto() {
        if (!isAdded()) return;

        String email = SessionManager.email(requireContext());
        String cleanEmail = email != null ? email.trim().toLowerCase(Locale.ROOT) : "";
        String localPath = SessionManager.getProfilePhotoLocal(requireContext());
        File localFile = null;
        if (localPath != null && !localPath.isEmpty()) {
            File f = new File(localPath);
            if (f.exists()) localFile = f;
        }
        if (localFile == null && !cleanEmail.isEmpty()) {
            File userHashed = new File(requireContext().getFilesDir(), "profile_photo_" + Math.abs(cleanEmail.hashCode()) + ".jpg");
            if (userHashed.exists()) localFile = userHashed;
        }

        if (localFile != null && localFile.exists()) {
            Bitmap bmp = BitmapFactory.decodeFile(localFile.getAbsolutePath());
            if (bmp != null) {
                displayCircularProfileBitmap(bmp);
                return;
            }
        }

        String remoteUrl = SessionManager.getProfilePhotoUrl(requireContext());
        if ((remoteUrl == null || remoteUrl.isEmpty()) && !cleanEmail.isEmpty()) {
            remoteUrl = CloudinaryUploader.getDeterministicProfileUrl(cleanEmail);
        }
        if (remoteUrl != null && remoteUrl.startsWith("http")) {
            final Context appContext = requireContext().getApplicationContext();
            CloudinaryUploader.fetchRemoteBitmap(remoteUrl, new CloudinaryUploader.BitmapCallback() {
                @Override
                public void onBitmapLoaded(Bitmap bmp) {
                    if (bmp == null) return;
                    try {
                        if (!cleanEmail.isEmpty()) {
                            File hashedFile = new File(appContext.getFilesDir(), "profile_photo_" + Math.abs(cleanEmail.hashCode()) + ".jpg");
                            try (FileOutputStream fos2 = new FileOutputStream(hashedFile)) {
                                bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos2);
                            }
                            SessionManager.saveProfilePhotoLocal(appContext, cleanEmail, hashedFile.getAbsolutePath());
                        }
                    } catch (Exception ignored) {
                    }
                    if (isAdded() && getActivity() != null) {
                        requireActivity().runOnUiThread(() -> {
                            currentProfileBitmap = bmp;
                            displayCircularProfileBitmap(bmp);
                        });
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.w(TAG, "Falha ao carregar foto remota do Cloudinary: " + e.getMessage());
                    if (isAdded() && getActivity() != null) {
                        requireActivity().runOnUiThread(() -> {
                            if (currentProfileBitmap == null) {
                                if (imgProfilePhoto != null) imgProfilePhoto.setVisibility(View.GONE);
                                if (tvProfileInitials != null) tvProfileInitials.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }
            });
        } else {
            currentProfileBitmap = null;
            if (imgProfilePhoto != null) {
                imgProfilePhoto.setVisibility(View.GONE);
            }
            if (tvProfileInitials != null) {
                tvProfileInitials.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        bindUserData();
        loadProfilePhoto();
        loadNotifCount();
        fetchUserProfileFromDatabase();
    }

    private void loadNumbers() {
        ReportAPI api = ApiClient.get(requireContext()).create(ReportAPI.class);
        api.getEsgMetrics().enqueue(new Callback<List<EsgMetricResponseDTO>>() {
            @Override
            public void onResponse(@NonNull Call<List<EsgMetricResponseDTO>> call,
                                   @NonNull Response<List<EsgMetricResponseDTO>> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    EsgMetricResponseDTO metric = response.body().get(0);
                    JsonObject summary = new JsonObject();
                    int kg = metric.getTotalRecycledKg() != null ? metric.getTotalRecycledKg().intValue() : 0;
                    int openCount = 0;
                    int resolvedCount = 0;
                    List<Occurrence> cachedOccs = OccurrenceStore.cached(requireContext());
                    if (cachedOccs != null) {
                        for (Occurrence occ : cachedOccs) {
                            if (occ.isResolved()) {
                                resolvedCount++;
                            } else {
                                openCount++;
                            }
                        }
                    }
                    summary.addProperty("kg_reciclados", kg);
                    summary.addProperty("resolvidas", resolvedCount);
                    summary.addProperty("abertas", openCount);

                    PrefsHelper.putJson(requireContext(), PREFS_REPORTS, KEY_SUMMARY, summary.toString());
                    applyNumbers(summary);
                } else {
                    fallbackProfileNumbers(api);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<EsgMetricResponseDTO>> call, @NonNull Throwable t) {
                if (isAdded()) {
                    fallbackProfileNumbers(api);
                }
            }
        });
    }

    private void fallbackProfileNumbers(ReportAPI api) {
        api.getSummary().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    PrefsHelper.putJson(requireContext(), PREFS_REPORTS, KEY_SUMMARY,
                            response.body().toString());
                    applyNumbers(response.body());
                } else {
                    loadNumbersFromCache();
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                loadNumbersFromCache();
            }
        });
    }

    private void loadNumbersFromCache() {
        String json = PrefsHelper.getJson(requireContext(), PREFS_REPORTS, KEY_SUMMARY);
        if (json != null) {
            try {
                applyNumbers(com.google.gson.JsonParser.parseString(json).getAsJsonObject());
                return;
            } catch (Exception ignored) {

            }
        }

        int openCount = 0;
        int resolvedCount = 0;
        List<Occurrence> cachedOccs = OccurrenceStore.cached(requireContext());
        if (cachedOccs != null) {
            for (Occurrence occ : cachedOccs) {
                if (occ.isResolved()) {
                    resolvedCount++;
                } else {
                    openCount++;
                }
            }
        }
        JsonObject fallback = new JsonObject();
        fallback.addProperty("abertas", openCount);
        fallback.addProperty("resolvidas", resolvedCount);
        fallback.addProperty("kg_reciclados", 0);
        applyNumbers(fallback);
    }

    private void applyNumbers(JsonObject summary) {
        int total = 0;
        int kg = 0;
        try {
            if (summary.has("abertas")) {
                total += summary.get("abertas").getAsInt();
            }
            if (summary.has("resolvidas")) {
                total += summary.get("resolvidas").getAsInt();
            }
            if (summary.has("kg_reciclados")) {
                kg = summary.get("kg_reciclados").getAsInt();
            }
        } catch (Exception ignored) {

        }
        if (total == 0) {
            List<Occurrence> cachedOccs = OccurrenceStore.cached(requireContext());
            if (cachedOccs != null) {
                total = cachedOccs.size();
            }
        }
        if (tvOccurrences != null) {
            tvOccurrences.setText(getString(R.string.profile_occ_value, total));
        }
        if (tvRecovered != null) {
            tvRecovered.setText(getString(R.string.profile_recovered_value,
                    String.format(new Locale("pt", "BR"), "%,d", kg)));
        }
    }

    private void loadNotifCount() {
        NotificationStore.refresh(requireContext(), this::applyNotifCount);
    }

    private void applyNotifCount(List<Notification> notifications) {
        if (!isAdded() || tvNotifCount == null) {
            return;
        }
        tvNotifCount.setVisibility(View.GONE);
    }

    private void showLogoutDialog() {

        LogoutDialog.newInstance().show(getChildFragmentManager(), "logout_dialog");
    }

    private void showFeedbackToast(String title, String subtitle) {
        View toast = getView() != null ? getView().findViewById(R.id.toastProfileFeedback) : null;
        if (toast == null) return;
        TextView tvTitle = toast.findViewById(R.id.tvToastFeedbackTitle);
        TextView tvSubtitle = toast.findViewById(R.id.tvToastFeedbackSubtitle);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);

        toast.setVisibility(View.VISIBLE);
        toast.setAlpha(0f);
        toast.setTranslationY(60f);
        toast.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .withEndAction(() -> {
                    toast.postDelayed(() -> {
                        if (isAdded() && toast.getVisibility() == View.VISIBLE) {
                            toast.animate()
                                    .alpha(0f)
                                    .translationY(60f)
                                    .setDuration(300)
                                    .withEndAction(() -> toast.setVisibility(View.GONE))
                                    .start();
                        }
                    }, 3000);
                })
                .start();
    }
    private void fetchUserProfileFromDatabase() {
        if (!isAdded()) return;
        final Context app = requireContext().getApplicationContext();
        try {
            AuthAPI api = ApiClient.get(app).create(AuthAPI.class);
            api.getMe().enqueue(new Callback<UserResponseDTO>() {
                @Override
                public void onResponse(@NonNull Call<UserResponseDTO> call, @NonNull Response<UserResponseDTO> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        UserResponseDTO user = response.body();
                        boolean changed = false;
                        if (user.getId() != null && !user.getId().trim().isEmpty()) {
                            SessionManager.saveUserId(app, user.getId().trim());
                        }
                        if (user.getCompanyId() != null && !user.getCompanyId().trim().isEmpty()) {
                            SessionManager.saveCompanyId(app, user.getCompanyId().trim());
                        }
                        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                            String currentPhoto = SessionManager.getProfilePhotoUrl(app);
                            if (!user.getPhotoUrl().equals(currentPhoto)) {
                                SessionManager.saveProfilePhotoUrl(app, user.getPhotoUrl());
                                changed = true;
                            }
                        }
                        if (user.getName() != null && !user.getName().isEmpty()) {
                            SessionManager.saveName(app, user.getName());
                            changed = true;
                        }
                        if (user.getPosition() != null && !user.getPosition().isEmpty()) {
                            SessionManager.saveSector(app, user.getPosition());
                            changed = true;
                        }
                        if (isAdded()) {
                            bindUserData();
                            if (changed) {
                                loadProfilePhoto();
                            }
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<UserResponseDTO> call, @NonNull Throwable t) {
                    Log.w(TAG, "Falha ao puxar perfil do banco oficial (GET /users/me): " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "Erro ao consultar /users/me: " + e.getMessage());
        }
    }

}
