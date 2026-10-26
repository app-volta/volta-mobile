package com.aula.volta.ui.profile;

import android.content.Context;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aula.volta.R;
import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.AuthAPI;
import com.aula.volta.data.local.SessionManager;
import com.aula.volta.data.model.UserResponseDTO;
import com.aula.volta.data.upload.CloudinaryUploader;
import com.aula.volta.ui.dialog.ProfilePhotoSheet;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 23a · Perfil · editar dados (Figma 1721:1264).
 * Suporte completo a alteração de foto via câmera ou galeria, com upload no Cloudinary (preset Perfil-Photo),
 * sincronização em tempo real no banco oficial (PUT /users/me) e espelhamento no Cloud Firestore.
 */
public class EditProfileFragment extends Fragment {

    private static final String TAG = "EditProfileFragment";

    private ImageView imgEditProfilePhoto;
    private TextView tvEditProfileInitials;
    private EditText etEditProfileName;
    private EditText etEditProfileEmail;
    private TextView tvEditProfileSector;

    private String selectedSector = "";

    private File cameraTempFile;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;

    public EditProfileFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Launcher de captura de foto pela câmera
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(), success -> {
                    if (success && cameraTempFile != null && cameraTempFile.exists()) {
                        processAndSaveProfilePhoto(cameraTempFile);
                    }
                });

        // Launcher de seleção de foto pela galeria
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        File cached = copyUriToProfileCache(uri);
                        if (cached != null) {
                            processAndSaveProfilePhoto(cached);
                        }
                    }
                });

        // Launcher de permissão de câmera
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgEditProfilePhoto = view.findViewById(R.id.imgEditProfilePhoto);
        tvEditProfileInitials = view.findViewById(R.id.tvEditProfileInitials);
        etEditProfileName = view.findViewById(R.id.etEditProfileName);
        etEditProfileEmail = view.findViewById(R.id.etEditProfileEmail);
        tvEditProfileSector = view.findViewById(R.id.tvEditProfileSector);

        view.findViewById(R.id.btnBackEditProfile).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        // Preenche dados atuais da sessão
        String name = SessionManager.name(requireContext());
        if (name == null || name.trim().isEmpty()) {
            name = getString(R.string.profile_name);
        }
        etEditProfileName.setText(name);
        updateInitials(name);

        String email = SessionManager.email(requireContext());
        if (email == null || email.trim().isEmpty()) {
            email = getString(R.string.profile_email_value);
        }
        etEditProfileEmail.setText(email);

        selectedSector = SessionManager.sector(requireContext());
        if (selectedSector == null || selectedSector.trim().isEmpty()) {
            selectedSector = getString(R.string.profile_unit);
        }
        tvEditProfileSector.setText(selectedSector);

        loadAvatar();

        // Trocar foto
        View.OnClickListener photoClick = v -> openPhotoSheet();
        view.findViewById(R.id.avatarContainerEdit).setOnClickListener(photoClick);
        view.findViewById(R.id.btnChangePhoto).setOnClickListener(photoClick);

        // Seleção de Setor
        view.findViewById(R.id.btnSelectSector).setOnClickListener(v -> showSectorDialog());

        // Alterar senha
        view.findViewById(R.id.rowChangePassword).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.nav_change_password));

        // Salvar alterações
        view.findViewById(R.id.btnSaveProfileChanges).setOnClickListener(v -> saveChanges());
    }

    private void updateInitials(String name) {
        if (tvEditProfileInitials == null || name == null) return;
        String initials = "BG";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            initials = ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase(Locale.ROOT);
        } else if (parts.length == 1 && !parts[0].isEmpty()) {
            initials = parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }
        tvEditProfileInitials.setText(initials);
    }

    private void loadAvatar() {
        if (!isAdded()) return;

        // 1. Tenta carregar do cache local
        String localPath = SessionManager.getProfilePhotoLocal(requireContext());
        if (localPath != null && !localPath.isEmpty()) {
            File f = new File(localPath);
            if (f.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
                if (bmp != null) {
                    displayCircularPhoto(bmp);
                    return;
                }
            }
        }

        // 2. Tenta carregar da URL remota do Cloudinary
        String remoteUrl = SessionManager.getProfilePhotoUrl(requireContext());
        if (remoteUrl != null && remoteUrl.startsWith("http")) {
            new Thread(() -> {
                try {
                    URL url = new URL(remoteUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    conn.connect();
                    try (InputStream is = conn.getInputStream()) {
                        Bitmap bmp = BitmapFactory.decodeStream(is);
                        if (bmp != null && isAdded()) {
                            File f = new File(requireContext().getFilesDir(), "profile_photo.jpg");
                            try (FileOutputStream fos = new FileOutputStream(f)) {
                                bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                            }
                            SessionManager.saveProfilePhotoLocal(requireContext(), f.getAbsolutePath());
                            requireActivity().runOnUiThread(() -> displayCircularPhoto(bmp));
                        }
                    }
                } catch (Exception ignored) {
                }
            }).start();
        } else {
            // 3. Fallback: Consulta foto persistida no Cloud Firestore
            try {
                String email = SessionManager.email(requireContext());
                if (email != null && !email.isEmpty()) {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(email)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                if (isAdded() && snapshot != null && snapshot.exists()) {
                                    String firestorePhoto = snapshot.getString("photoUrl");
                                    if (firestorePhoto != null && !firestorePhoto.isEmpty()) {
                                        SessionManager.saveProfilePhotoUrl(requireContext(), firestorePhoto);
                                        loadAvatar();
                                    }
                                }
                            });
                }
            } catch (Exception ignored) {
            }

            if (imgEditProfilePhoto != null) imgEditProfilePhoto.setVisibility(View.GONE);
            if (tvEditProfileInitials != null) tvEditProfileInitials.setVisibility(View.VISIBLE);
        }
    }

    private void displayCircularPhoto(Bitmap bmp) {
        if (imgEditProfilePhoto != null && isAdded()) {
            RoundedBitmapDrawable circular = RoundedBitmapDrawableFactory.create(getResources(), bmp);
            circular.setCircular(true);
            imgEditProfilePhoto.setImageDrawable(circular);
            imgEditProfilePhoto.setVisibility(View.VISIBLE);
            if (tvEditProfileInitials != null) tvEditProfileInitials.setVisibility(View.GONE);
        }
    }

    private void openPhotoSheet() {
        boolean hasPhoto = (SessionManager.getProfilePhotoLocal(requireContext()) != null)
                || (SessionManager.getProfilePhotoUrl(requireContext()) != null);
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
        sheet.show(getChildFragmentManager(), "edit_photo_sheet");
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

    /**
     * Processa a imagem (EXIF + Center Crop 1:1 quadrado + redimensionamento 512x512),
     * atualiza o avatar imediatamente na tela e faz o upload assíncrono para o Cloudinary
     * usando o preset Perfil-Photo e pasta Perfil/Assets/Image.
     */
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

            // Corrige orientação EXIF se necessário
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

            // Recorte quadrado 1:1 centralizado (Center Crop)
            int minDim = Math.min(bitmap.getWidth(), bitmap.getHeight());
            int cropX = (bitmap.getWidth() - minDim) / 2;
            int cropY = (bitmap.getHeight() - minDim) / 2;
            Bitmap square = Bitmap.createBitmap(bitmap, cropX, cropY, minDim, minDim);
            Bitmap finalAvatar = Bitmap.createScaledBitmap(square, 512, 512, true);

            // Salva permanentemente no diretório de arquivos do app
            String myEmail = SessionManager.email(requireContext());
            File avatarFile = new File(requireContext().getFilesDir(), "profile_photo.jpg");
            try (FileOutputStream fos = new FileOutputStream(avatarFile)) {
                finalAvatar.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            }
            if (!myEmail.isEmpty()) {
                File hashedFile = new File(requireContext().getFilesDir(), "profile_photo_" + Math.abs(myEmail.hashCode()) + ".jpg");
                try (FileOutputStream fos2 = new FileOutputStream(hashedFile)) {
                    finalAvatar.compress(Bitmap.CompressFormat.JPEG, 90, fos2);
                }
            }

            // Grava caminho local na sessão
            SessionManager.saveProfilePhotoLocal(requireContext(), myEmail, avatarFile.getAbsolutePath());

            // Atualiza UI instantaneamente com avatar circular sem atraso
            displayCircularPhoto(finalAvatar);

            Toast.makeText(requireContext(), R.string.profile_photo_uploading, Toast.LENGTH_SHORT).show();

            final Context appContext = requireContext().getApplicationContext();

            // Dispara upload para o Cloudinary com public_id determinístico baseado no e-mail (preset Perfil-Photo / pasta Perfil/Assets/Image)
            CloudinaryUploader.uploadProfileImage(appContext, avatarFile, myEmail, new CloudinaryUploader.UploadCallback() {
                @Override
                public void onSuccess(String remoteUrl, String publicId) {
                    SessionManager.saveProfilePhotoUrl(appContext, myEmail, remoteUrl);
                    SessionManager.saveProfilePhotoLocal(appContext, myEmail, avatarFile.getAbsolutePath());
                    Log.i(TAG, "Foto de perfil enviada ao Cloudinary com sucesso: " + remoteUrl);

                    // 1. Salva no banco de dados oficial via PUT /users/me (API Geral)
                    try {
                        AuthAPI authApi = ApiClient.get(appContext).create(AuthAPI.class);
                        JsonObject body = new JsonObject();
                        String nameToSave = etEditProfileName != null && etEditProfileName.getText() != null
                                ? etEditProfileName.getText().toString().trim() : SessionManager.name(appContext);
                        body.addProperty("name", nameToSave);
                        body.addProperty("position", selectedSector != null ? selectedSector : SessionManager.sector(appContext));
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

                    // 2. Replica no Cloud Firestore (coleção users)
                    try {
                        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
                        java.util.Map<String, Object> uDoc = new java.util.HashMap<>();
                        uDoc.put("photoUrl", remoteUrl);
                        uDoc.put("avatarUrl", remoteUrl);
                        uDoc.put("name", etEditProfileName != null && etEditProfileName.getText() != null
                                ? etEditProfileName.getText().toString().trim() : SessionManager.name(appContext));
                        uDoc.put("email", myEmail);
                        uDoc.put("position", selectedSector);
                        uDoc.put("updatedAt", com.google.firebase.Timestamp.now());
                        if (myEmail != null && !myEmail.isEmpty()) {
                            db.collection("users").document(myEmail).set(uDoc, com.google.firebase.firestore.SetOptions.merge());
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Erro ao sincronizar foto no Firestore: " + e.getMessage());
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

    private void removeProfilePhoto() {
        Context appContext = requireContext().getApplicationContext();
        String email = SessionManager.email(appContext);
        File avatarFile = new File(appContext.getFilesDir(), "profile_photo.jpg");
        if (avatarFile.exists()) {
            avatarFile.delete();
        }
        if (!email.isEmpty()) {
            File hashedFile = new File(appContext.getFilesDir(), "profile_photo_" + Math.abs(email.hashCode()) + ".jpg");
            if (hashedFile.exists()) {
                hashedFile.delete();
            }
        }
        SessionManager.clearProfilePhoto(appContext);
        if (imgEditProfilePhoto != null) {
            imgEditProfilePhoto.setImageDrawable(null);
            imgEditProfilePhoto.setVisibility(View.GONE);
        }
        if (tvEditProfileInitials != null) {
            tvEditProfileInitials.setVisibility(View.VISIBLE);
        }

        // Remove do banco oficial via PUT /users/me
        try {
            AuthAPI authApi = ApiClient.get(appContext).create(AuthAPI.class);
            JsonObject body = new JsonObject();
            body.addProperty("name", SessionManager.name(appContext));
            body.addProperty("position", selectedSector);
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

        // Remove do Cloud Firestore
        try {
            if (email != null && !email.isEmpty()) {
                java.util.Map<String, Object> update = new java.util.HashMap<>();
                update.put("photoUrl", null);
                update.put("avatarUrl", null);
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(email)
                        .set(update, com.google.firebase.firestore.SetOptions.merge());
            }
        } catch (Exception ignored) {
        }

        Toast.makeText(requireContext(), R.string.profile_photo_removed, Toast.LENGTH_SHORT).show();
    }

    private void showSectorDialog() {
        String[] options = getResources().getStringArray(R.array.occurrence_sector_options);
        new AlertDialog.Builder(requireContext())
                .setTitle("Selecione o setor")
                .setItems(options, (dialog, which) -> {
                    selectedSector = options[which];
                    tvEditProfileSector.setText(selectedSector);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void saveChanges() {
        String newName = etEditProfileName.getText() != null
                ? etEditProfileName.getText().toString().trim() : "";
        if (newName.isEmpty()) {
            etEditProfileName.setError("Informe seu nome completo");
            return;
        }

        SessionManager.saveName(requireContext(), newName);
        if (selectedSector != null && !selectedSector.isEmpty()) {
            SessionManager.saveSector(requireContext(), selectedSector);
        }

        // Persiste na API Oficial (PUT /users/me)
        try {
            AuthAPI authApi = ApiClient.get(requireContext()).create(AuthAPI.class);
            JsonObject body = new JsonObject();
            body.addProperty("name", newName);
            body.addProperty("position", selectedSector);
            String photo = SessionManager.getProfilePhotoUrl(requireContext());
            if (photo != null && !photo.isEmpty()) {
                body.addProperty("photoUrl", photo);
                body.addProperty("photo_url", photo);
                body.addProperty("avatarUrl", photo);
                body.addProperty("avatar_url", photo);
                body.addProperty("imageUrl", photo);
                body.addProperty("image_url", photo);
                body.addProperty("foto_url", photo);
                body.addProperty("pictureUrl", photo);
            }
            authApi.updateMe(body).enqueue(new Callback<UserResponseDTO>() {
                @Override
                public void onResponse(@NonNull Call<UserResponseDTO> call, @NonNull Response<UserResponseDTO> response) {
                    Log.i(TAG, "Perfil atualizado no backend com sucesso (status " + response.code() + ")");
                }

                @Override
                public void onFailure(@NonNull Call<UserResponseDTO> call, @NonNull Throwable t) {
                    Log.w(TAG, "Falha ao salvar perfil na API: " + t.getMessage());
                }
            });
        } catch (Exception ignored) {
        }

        // Replica no Cloud Firestore
        try {
            String email = SessionManager.email(requireContext());
            if (email != null && !email.isEmpty()) {
                java.util.Map<String, Object> update = new java.util.HashMap<>();
                update.put("name", newName);
                update.put("position", selectedSector);
                String photo = SessionManager.getProfilePhotoUrl(requireContext());
                if (photo != null && !photo.isEmpty()) {
                    update.put("photoUrl", photo);
                    update.put("avatarUrl", photo);
                }
                update.put("updatedAt", com.google.firebase.Timestamp.now());
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(email)
                        .set(update, com.google.firebase.firestore.SetOptions.merge());
            }
        } catch (Exception ignored) {
        }

        if (getView() != null) {
            Bundle result = new Bundle();
            result.putBoolean("profileSaved", true);
            getParentFragmentManager().setFragmentResult("profile_result", result);
            Navigation.findNavController(getView()).navigateUp();
        }
    }
}
