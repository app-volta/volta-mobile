package com.aula.volta;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Nova ocorrência — Passo 1 de 2 (Fase 3, Figma Registrar Teste A 568:24).
 * Foto OBRIGATÓRIA (sem o texto vermelho "OBRIGATÓRIO"): Câmera/Galeria via Intent,
 * preview + refazer, compressão JPEG 70, botão habilita só com foto. CameraX na Fase 12.
 */
public class NewOccurrenceFragment extends Fragment {

    private static final int MAX_PHOTO_DIMEN = 1280;
    private static final int JPEG_QUALITY = 70;

    private LinearLayout photoEmptyGroup;
    private LinearLayout photoPreviewGroup;
    private ImageView imgPhotoPreview;
    private MaterialButton btnSendAnalysis;
    private TextView tvSendHint;
    private TextView tvSector;
    private EditText etDescription;
    private SwitchMaterial switchNotify;

    private File photoFile;
    private File cameraTempFile;
    private int sectorIndex;

    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;

    public NewOccurrenceFragment() {
        // Construtor vazio obrigatório
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(), success -> {
                    if (success && cameraTempFile != null) {
                        photoFile = compressImage(cameraTempFile);
                        updatePhotoState();
                    }
                });

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        photoFile = compressImage(copyToCache(uri));
                        updatePhotoState();
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_occurrence, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        photoEmptyGroup = view.findViewById(R.id.photoEmptyGroup);
        photoPreviewGroup = view.findViewById(R.id.photoPreviewGroup);
        imgPhotoPreview = view.findViewById(R.id.imgPhotoPreview);
        btnSendAnalysis = view.findViewById(R.id.btnSendAnalysis);
        tvSendHint = view.findViewById(R.id.tvSendHint);
        tvSector = view.findViewById(R.id.tvSector);
        etDescription = view.findViewById(R.id.etDescription);
        switchNotify = view.findViewById(R.id.switchNotify);

        sectorIndex = 0;
        tvSector.setText(getResources().getStringArray(R.array.occurrence_sector_options)[0]);

        View btnBack = view.findViewById(R.id.btnBackRegister);
        btnBack.setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        MaterialButton btnCamera = view.findViewById(R.id.btnCaptureCamera);
        btnCamera.setOnClickListener(v -> openCamera());

        MaterialButton btnGallery = view.findViewById(R.id.btnOpenGallery);
        btnGallery.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        TextView btnRetake = view.findViewById(R.id.btnRetakePhoto);
        btnRetake.setOnClickListener(v -> {
            photoFile = null;
            updatePhotoState();
        });

        View btnSector = view.findViewById(R.id.btnSector);
        btnSector.setOnClickListener(v -> showSectorDialog());

        btnSendAnalysis.setOnClickListener(v -> sendToAnalysis(v));

        updatePhotoState();
    }

    private void openCamera() {
        try {
            cameraTempFile = new File(requireContext().getCacheDir(),
                    "volta_camera_" + System.currentTimeMillis() + ".jpg");
            Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", cameraTempFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                takePictureLauncher.launch(uri);
            } else {
                Toast.makeText(requireContext(), R.string.error_load, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.error_load, Toast.LENGTH_SHORT).show();
        }
    }

    private void showSectorDialog() {
        String[] sectors = getResources().getStringArray(R.array.occurrence_sector_options);
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.occurrence_location_label)
                .setSingleChoiceItems(sectors, sectorIndex, (dialog, which) -> {
                    sectorIndex = which;
                    tvSector.setText(sectors[which]);
                    dialog.dismiss();
                })
                .show();
    }

    /** Mostra preview + habilita envio só com foto (Teste A). */
    private void updatePhotoState() {
        boolean hasPhoto = photoFile != null && photoFile.exists();
        photoEmptyGroup.setVisibility(hasPhoto ? View.GONE : View.VISIBLE);
        photoPreviewGroup.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);
        if (hasPhoto) {
            imgPhotoPreview.setImageBitmap(
                    BitmapFactory.decodeFile(photoFile.getAbsolutePath()));
        } else {
            imgPhotoPreview.setImageDrawable(null);
        }

        btnSendAnalysis.setEnabled(hasPhoto);
        if (hasPhoto) {
            btnSendAnalysis.setBackgroundResource(R.drawable.bg_button_gradient);
            btnSendAnalysis.setTextColor(getResources().getColor(R.color.white, null));
            tvSendHint.setVisibility(View.GONE);
        } else {
            btnSendAnalysis.setBackgroundResource(R.drawable.bg_button_disabled);
            btnSendAnalysis.setTextColor(
                    getResources().getColor(R.color.volta_text_muted_light, null));
            tvSendHint.setVisibility(View.VISIBLE);
        }
    }

    private void sendToAnalysis(View v) {
        if (photoFile == null || !photoFile.exists()) {
            return;
        }
        String[] sectors = getResources().getStringArray(R.array.occurrence_sector_options);
        Bundle args = new Bundle();
        args.putString("photoPath", photoFile.getAbsolutePath());
        args.putString("descricao", etDescription.getText().toString().trim());
        args.putString("setor", sectors[sectorIndex]);
        args.putBoolean("avisar", switchNotify.isChecked());
        Navigation.findNavController(v).navigate(R.id.nav_ai_analysis, args);
    }

    /** Copia conteúdo da galeria para o cache (para comprimir em arquivo próprio). */
    private File copyToCache(Uri uri) {
        try {
            File out = new File(requireContext().getCacheDir(),
                    "volta_gallery_" + System.currentTimeMillis() + ".jpg");
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

    /** Reduz para máx. 1280px e salva JPEG 70 (Fase 3: preserva qualidade p/ análise). */
    private File compressImage(File source) {
        if (source == null || !source.exists()) {
            return null;
        }
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(source.getAbsolutePath(), bounds);

            int sample = 1;
            while (bounds.outWidth / sample > MAX_PHOTO_DIMEN
                    || bounds.outHeight / sample > MAX_PHOTO_DIMEN) {
                sample *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            Bitmap bitmap = BitmapFactory.decodeFile(source.getAbsolutePath(), opts);
            if (bitmap == null) {
                return null;
            }

            File out = new File(requireContext().getCacheDir(),
                    "volta_photo_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos);
            }
            bitmap.recycle();
            if (!source.getName().startsWith("volta_photo_")) {
                source.delete();
            }
            return out;
        } catch (Exception e) {
            return null;
        }
    }
}
