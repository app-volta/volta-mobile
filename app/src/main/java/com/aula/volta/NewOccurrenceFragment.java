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
import android.widget.FrameLayout;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Nova ocorrência — Passo 1 de 2 (Figma 851:3792).
 * Telas:
 *  - 12 · Registrar (851:3793)
 *  - 13 · Registrar · câmera (851:3852)
 *  - 14 · Registrar · foto anexada (851:3904)
 */
public class NewOccurrenceFragment extends Fragment {

    private static final int MAX_PHOTO_DIMEN = 1280;
    private static final int JPEG_QUALITY = 75;

    private LinearLayout photoEmptyGroup;
    private View photoPreviewGroup;
    private ImageView imgPhotoPreview;
    private MaterialButton btnSendAnalysis;
    private TextView tvSector;
    private EditText etDescription;
    private FrameLayout cameraViewfinderOverlay;

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
        tvSector = view.findViewById(R.id.tvSector);
        etDescription = view.findViewById(R.id.etDescription);
        cameraViewfinderOverlay = view.findViewById(R.id.cameraViewfinderOverlay);

        sectorIndex = 0;
        tvSector.setText(getResources().getStringArray(R.array.occurrence_sector_options)[0]);

        View btnBack = view.findViewById(R.id.btnBackRegister);
        btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Botão Câmera -> Abre overlay do Viewfinder (Tela 13 do Figma)
        MaterialButton btnCamera = view.findViewById(R.id.btnCaptureCamera);
        btnCamera.setOnClickListener(v -> openCameraOverlay());

        // Botão Galeria -> Seleciona arquivo do sistema
        MaterialButton btnGallery = view.findViewById(R.id.btnOpenGallery);
        btnGallery.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // Botão Trocar foto
        View btnRetake = view.findViewById(R.id.btnRetakePhoto);
        btnRetake.setOnClickListener(v -> {
            photoFile = null;
            updatePhotoState();
        });

        // Setor / Localização
        View btnSector = view.findViewById(R.id.btnSector);
        btnSector.setOnClickListener(v -> showSectorDialog());

        // Botão Enviar para análise
        btnSendAnalysis.setOnClickListener(this::sendToAnalysis);

        // Controles da Câmera (Tela 13)
        View btnCameraClose = view.findViewById(R.id.btnCameraClose);
        btnCameraClose.setOnClickListener(v -> closeCameraOverlay());

        View btnCameraShutter = view.findViewById(R.id.btnCameraShutter);
        btnCameraShutter.setOnClickListener(v -> captureSamplePhoto());

        View btnCameraPickGallery = view.findViewById(R.id.btnCameraPickGallery);
        btnCameraPickGallery.setOnClickListener(v -> {
            closeCameraOverlay();
            pickImageLauncher.launch("image/*");
        });

        View btnCameraFlash = view.findViewById(R.id.btnCameraFlash);
        btnCameraFlash.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Flash automático", Toast.LENGTH_SHORT).show());

        View btnCameraFlip = view.findViewById(R.id.btnCameraFlip);
        btnCameraFlip.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Câmera principal", Toast.LENGTH_SHORT).show());

        updatePhotoState();
    }

    private void openCameraOverlay() {
        if (cameraViewfinderOverlay != null) {
            cameraViewfinderOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void closeCameraOverlay() {
        if (cameraViewfinderOverlay != null) {
            cameraViewfinderOverlay.setVisibility(View.GONE);
        }
    }

    /**
     * Captura a foto da cena do Figma (caixas de papelão da câmara fria).
     * Salva em arquivo de cache e preenche a descrição contextualizada do protótipo.
     */
    private void captureSamplePhoto() {
        try {
            File out = new File(requireContext().getCacheDir(),
                    "volta_photo_" + System.currentTimeMillis() + ".jpg");
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img_caixas_papelao);
            try (FileOutputStream fos = new FileOutputStream(out)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            }
            photoFile = out;

            // Preenche automaticamente com o texto do protótipo do Figma
            if (etDescription != null && etDescription.getText().toString().trim().isEmpty()) {
                etDescription.setText("Caixas de papelão molhadas ao lado da câmara fria.");
            }

            closeCameraOverlay();
            updatePhotoState();
        } catch (Exception e) {
            closeCameraOverlay();
            openSystemCamera();
        }
    }

    private void openSystemCamera() {
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

    /** Atualiza estado vazio vs. foto anexada (Figma 12 vs 14). */
    private void updatePhotoState() {
        boolean hasPhoto = photoFile != null && photoFile.exists();
        photoEmptyGroup.setVisibility(hasPhoto ? View.GONE : View.VISIBLE);
        photoPreviewGroup.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);

        if (hasPhoto) {
            imgPhotoPreview.setImageBitmap(
                    BitmapFactory.decodeFile(photoFile.getAbsolutePath()));
            btnSendAnalysis.setEnabled(true);
            btnSendAnalysis.setBackgroundResource(R.drawable.bg_btn_primary_v);
            btnSendAnalysis.setTextColor(requireContext().getColor(R.color.white));
        } else {
            imgPhotoPreview.setImageDrawable(null);
            btnSendAnalysis.setEnabled(false);
            btnSendAnalysis.setBackgroundResource(R.drawable.bg_btn_disabled_v);
            btnSendAnalysis.setTextColor(requireContext().getColor(R.color.vf_disabled_text));
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
        args.putBoolean("avisar", false);
        Navigation.findNavController(v).navigate(R.id.nav_ai_analysis, args);
    }

    /** Copia conteúdo da galeria para o cache. */
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

    /** Reduz dimensões para análise performática. */
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
