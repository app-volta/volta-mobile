package com.aula.volta;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Nova ocorrência — Passo 1 de 2 (Figma 851:3792).
 * Captura real de fotos através de CameraX no visor embutido ou Câmera do Sistema.
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

    // Elementos do visor embutido de Câmera
    private FrameLayout cameraViewfinderOverlay;
    private PreviewView cameraPreviewView;
    private ImageView cameraPreviewImage;

    // CameraX
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private Camera camera;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private boolean isTorchOn = false;

    private File photoFile;
    private File cameraTempFile;
    private int sectorIndex;

    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;

    public NewOccurrenceFragment() {
        // Construtor vazio obrigatório
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Launcher para Câmera do Sistema (fallback e opção direta)
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(), success -> {
                    if (success && cameraTempFile != null && cameraTempFile.exists()) {
                        photoFile = compressImage(cameraTempFile);
                        updatePhotoState();
                    }
                });

        // Launcher para Galeria de Fotos
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        File cached = copyToCache(uri);
                        if (cached != null) {
                            photoFile = compressImage(cached);
                            updatePhotoState();
                        }
                    }
                });

        // Launcher de Permissão de Câmera
        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        openCameraViewfinder();
                    } else {
                        Toast.makeText(requireContext(),
                                "Permissão de câmera é necessária para fotografar.",
                                Toast.LENGTH_SHORT).show();
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
        cameraPreviewView = view.findViewById(R.id.cameraPreviewView);
        cameraPreviewImage = view.findViewById(R.id.cameraPreviewImage);

        sectorIndex = 0;
        tvSector.setText(getResources().getStringArray(R.array.occurrence_sector_options)[0]);

        View btnBack = view.findViewById(R.id.btnBackRegister);
        btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Botão Câmera -> Inicia captura de foto real
        MaterialButton btnCamera = view.findViewById(R.id.btnCaptureCamera);
        btnCamera.setOnClickListener(v -> checkPermissionAndOpenCamera());

        // Botão Galeria -> Seleciona arquivo do dispositivo
        MaterialButton btnGallery = view.findViewById(R.id.btnOpenGallery);
        btnGallery.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // Botão Trocar foto
        View btnRetake = view.findViewById(R.id.btnRetakePhoto);
        btnRetake.setOnClickListener(v -> {
            photoFile = null;
            updatePhotoState();
        });

        // Seletor de Setor / Localização
        View btnSector = view.findViewById(R.id.btnSector);
        btnSector.setOnClickListener(v -> showSectorDialog());

        // Botão Enviar para análise
        btnSendAnalysis.setOnClickListener(this::sendToAnalysis);

        // Controles do Visor da Câmera (Tela 13 do Figma)
        View btnCameraClose = view.findViewById(R.id.btnCameraClose);
        btnCameraClose.setOnClickListener(v -> closeCameraOverlay());

        View btnCameraShutter = view.findViewById(R.id.btnCameraShutter);
        btnCameraShutter.setOnClickListener(v -> captureRealPhoto());

        View btnCameraPickGallery = view.findViewById(R.id.btnCameraPickGallery);
        btnCameraPickGallery.setOnClickListener(v -> {
            closeCameraOverlay();
            pickImageLauncher.launch("image/*");
        });

        View btnCameraFlash = view.findViewById(R.id.btnCameraFlash);
        btnCameraFlash.setOnClickListener(v -> toggleTorch());

        View btnCameraFlip = view.findViewById(R.id.btnCameraFlip);
        btnCameraFlip.setOnClickListener(v -> flipCamera());

        updatePhotoState();
    }

    /**
     * Verifica permissão de câmera antes de abrir o visor.
     */
    private void checkPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCameraViewfinder();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * Abre o visor de câmera embutido e inicia o CameraX para visualização em tempo real.
     */
    private void openCameraViewfinder() {
        if (cameraViewfinderOverlay != null) {
            cameraViewfinderOverlay.setVisibility(View.VISIBLE);
        }
        startCameraX();
    }

    private void closeCameraOverlay() {
        stopCameraX();
        if (cameraViewfinderOverlay != null) {
            cameraViewfinderOverlay.setVisibility(View.GONE);
        }
    }

    /**
     * Inicia a sessão de câmera com CameraX.
     */
    private void startCameraX() {
        if (getContext() == null) {
            return;
        }
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (Exception e) {
                // Se o CameraX falhar no dispositivo, abre a câmera padrão do sistema
                closeCameraOverlay();
                openSystemCamera();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null || cameraPreviewView == null || !isAdded()) {
            return;
        }

        try {
            cameraProvider.unbindAll();

            CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build();

            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(cameraPreviewView.getSurfaceProvider());

            imageCapture = new ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build();

            camera = cameraProvider.bindToLifecycle(
                    getViewLifecycleOwner(), cameraSelector, preview, imageCapture);

            if (cameraPreviewImage != null) {
                cameraPreviewImage.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            // Emulador sem suporte Camera2 ou câmera indisponível
            if (cameraPreviewImage != null) {
                cameraPreviewImage.setVisibility(View.VISIBLE);
            }
        }
    }

    private void stopCameraX() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    /**
     * Dispara a captura real da foto através do use-case do CameraX.
     */
    private void captureRealPhoto() {
        if (imageCapture == null) {
            // Fallback: se CameraX não estiver ativo, abre a câmera do sistema
            closeCameraOverlay();
            openSystemCamera();
            return;
        }

        File photoOut = new File(requireContext().getCacheDir(),
                "volta_photo_" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoOut).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        if (!isAdded()) {
                            return;
                        }
                        photoFile = compressImage(photoOut);
                        closeCameraOverlay();
                        updatePhotoState();
                        Toast.makeText(requireContext(), "Foto capturada com sucesso!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        if (!isAdded()) {
                            return;
                        }
                        // Em caso de falha de hardware, tenta a câmera nativa do sistema
                        closeCameraOverlay();
                        openSystemCamera();
                    }
                });
    }

    /**
     * Alterna a lanterna / flash do dispositivo.
     */
    private void toggleTorch() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            isTorchOn = !isTorchOn;
            camera.getCameraControl().enableTorch(isTorchOn);
            Toast.makeText(requireContext(), isTorchOn ? "Flash ligado" : "Flash desligado", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Flash não disponível", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Alterna entre câmera frontal e traseira.
     */
    private void flipCamera() {
        lensFacing = (lensFacing == CameraSelector.LENS_FACING_BACK)
                ? CameraSelector.LENS_FACING_FRONT
                : CameraSelector.LENS_FACING_BACK;
        bindCameraUseCases();
    }

    /**
     * Dispara a câmera nativa do sistema Android caso o usuário prefira ou como fallback.
     */
    private void openSystemCamera() {
        try {
            cameraTempFile = new File(requireContext().getCacheDir(),
                    "volta_camera_" + System.currentTimeMillis() + ".jpg");
            Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", cameraTempFile);
            takePictureLauncher.launch(uri);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Não foi possível abrir a câmera", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSectorDialog() {
        com.aula.volta.ui.occurrence.SelectSectorSheet sheet =
                com.aula.volta.ui.occurrence.SelectSectorSheet.newInstance(sectorIndex);
        sheet.setOnSectorSelectedListener((index, sectorName) -> {
            sectorIndex = index;
            tvSector.setText(sectorName);
        });
        sheet.show(getChildFragmentManager(), "select_sector");
    }

    /** Atualiza estado vazio vs. foto anexada (Figma 12 vs 14). */
    private void updatePhotoState() {
        boolean hasPhoto = photoFile != null && photoFile.exists();
        photoEmptyGroup.setVisibility(hasPhoto ? View.GONE : View.VISIBLE);
        photoPreviewGroup.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);

        if (hasPhoto) {
            Bitmap bmp = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
            imgPhotoPreview.setImageBitmap(bmp);
            btnSendAnalysis.setEnabled(true);
            btnSendAnalysis.setBackgroundTintList(null);
            btnSendAnalysis.setBackgroundResource(R.drawable.bg_btn_primary_v);
            btnSendAnalysis.setTextColor(requireContext().getColor(R.color.white));
        } else {
            imgPhotoPreview.setImageDrawable(null);
            btnSendAnalysis.setEnabled(false);
            btnSendAnalysis.setBackgroundTintList(null);
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

    @Override
    public void onDestroyView() {
        stopCameraX();
        super.onDestroyView();
    }
}
