package com.aula.volta.ui.profile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aula.volta.R;
import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.AuthAPI;
import com.aula.volta.data.local.SessionManager;
import com.aula.volta.data.model.UserResponseDTO;
import com.aula.volta.ui.dialog.ProfilePhotoSheet;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 23a · Perfil · editar dados (Figma 1721:1264).
 */
public class EditProfileFragment extends Fragment {

    private static final String TAG = "EditProfileFragment";

    private ImageView imgEditProfilePhoto;
    private TextView tvEditProfileInitials;
    private EditText etEditProfileName;
    private EditText etEditProfileEmail;
    private TextView tvEditProfileSector;

    private String selectedSector = "";

    public EditProfileFragment() {
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
                            requireActivity().runOnUiThread(() -> displayCircularPhoto(bmp));
                        }
                    }
                } catch (Exception ignored) {
                }
            }).start();
        } else {
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
                Toast.makeText(requireContext(), "Abra a tela de perfil para capturar nova foto", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onGallerySelected() {
                Toast.makeText(requireContext(), "Abra a tela de perfil para escolher nova foto", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRemoveSelected() {
                SessionManager.clearProfilePhoto(requireContext());
                if (imgEditProfilePhoto != null) imgEditProfilePhoto.setVisibility(View.GONE);
                if (tvEditProfileInitials != null) tvEditProfileInitials.setVisibility(View.VISIBLE);
            }
        });
        sheet.show(getChildFragmentManager(), "edit_photo_sheet");
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
