package com.aula.volta;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
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

import com.aula.volta.data.api.AiAnalysisAPI;
import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.util.ImageUtils;
import com.aula.volta.util.NetworkUtils;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewOccurrenceFragment extends Fragment {

    private static final int MAX_PHOTO_DIMEN = 1280;
    private static final int JPEG_QUALITY = 75;

    private LinearLayout photoEmptyGroup;
    private View photoPreviewGroup;
    private ImageView imgPhotoPreview;
    private MaterialButton btnSendAnalysis;
    private View cardSubmissionError;
    private TextView tvSector;
    private EditText etDescription;

    private FrameLayout cameraViewfinderOverlay;
    private PreviewView cameraPreviewView;
    private ImageView cameraPreviewImage;

    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private Camera camera;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private boolean isTorchOn = false;
    private OrientationEventListener orientationEventListener;

    private File photoFile;
    private File cameraTempFile;
    private int sectorIndex;
    private String selectedAreaId;
    private String selectedSectorName;

    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;

    private void showCancelRegistrationDialog() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancelar ocorrência?")
                .setMessage("Se você voltar, a ocorrência será excluída. Deseja realmente sair?")
                .setNegativeButton("Continuar", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Sim, excluir", (dialog, which) -> {
                    dialog.dismiss();
                    photoFile = null;
                    if (getView() != null) {
                        Navigation.findNavController(getView()).popBackStack(R.id.nav_home, false);
                    }
                })
                .show();
    }

    public NewOccurrenceFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(), success -> {
                    if (success && cameraTempFile != null && cameraTempFile.exists()) {
                        photoFile = compressImage(cameraTempFile);
                        updatePhotoState();
                    }
                });

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
        if (imgPhotoPreview != null) {
            imgPhotoPreview.setOnClickListener(v -> {
                if (photoFile != null && photoFile.exists()) {
                    String sectorName = selectedSectorName != null ? selectedSectorName : tvSector.getText().toString();
                    com.aula.volta.ui.dialog.ImagePreviewDialog.show(
                            getParentFragmentManager(),
                            photoFile.getAbsolutePath(),
                            "Foto do Resíduo",
                            sectorName
                    );
                }
            });
        }
        btnSendAnalysis = view.findViewById(R.id.btnSendAnalysis);
        cardSubmissionError = view.findViewById(R.id.cardSubmissionError);
        tvSector = view.findViewById(R.id.tvSector);
        etDescription = view.findViewById(R.id.etDescription);

        cameraViewfinderOverlay = view.findViewById(R.id.cameraViewfinderOverlay);
        cameraPreviewView = view.findViewById(R.id.cameraPreviewView);
        cameraPreviewImage = view.findViewById(R.id.cameraPreviewImage);

        sectorIndex = 0;
        tvSector.setText(getResources().getStringArray(R.array.occurrence_sector_options)[0]);
        preloadAreasAndWasteTypes();

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (cameraViewfinderOverlay != null && cameraViewfinderOverlay.getVisibility() == View.VISIBLE) {
                            closeCameraOverlay();
                        } else if (photoFile != null && photoFile.exists()) {
                            showCancelRegistrationDialog();
                        } else if (getView() != null) {
                            Navigation.findNavController(getView()).navigateUp();
                        } else {
                            setEnabled(false);
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
                });

        View btnBack = view.findViewById(R.id.btnBackRegister);
        btnBack.setOnClickListener(v -> {
            if (photoFile != null && photoFile.exists()) {
                showCancelRegistrationDialog();
            } else {
                Navigation.findNavController(v).navigateUp();
            }
        });

        MaterialButton btnCamera = view.findViewById(R.id.btnCaptureCamera);
        btnCamera.setOnClickListener(v -> {
            if (!ensureOnlineOrShowError(v)) {
                return;
            }
            checkPermissionAndOpenCamera();
        });

        MaterialButton btnGallery = view.findViewById(R.id.btnOpenGallery);
        btnGallery.setOnClickListener(v -> {
            if (!ensureOnlineOrShowError(v)) {
                return;
            }
            pickImageLauncher.launch("image/*");
        });

        View btnRetake = view.findViewById(R.id.btnRetakePhoto);
        btnRetake.setOnClickListener(v -> {
            if (!ensureOnlineOrShowError(v)) {
                return;
            }
            photoFile = null;
            updatePhotoState();
        });

        View btnSector = view.findViewById(R.id.btnSector);
        btnSector.setOnClickListener(v -> showSectorDialog());

        btnSendAnalysis.setOnClickListener(this::sendToAnalysis);

        View btnCameraClose = view.findViewById(R.id.btnCameraClose);
        btnCameraClose.setOnClickListener(v -> closeCameraOverlay());

        View btnCameraShutter = view.findViewById(R.id.btnCameraShutter);
        btnCameraShutter.setOnClickListener(v -> captureRealPhoto());

        View btnCameraPickGallery = view.findViewById(R.id.btnCameraPickGallery);
        btnCameraPickGallery.setOnClickListener(v -> {
            if (!ensureOnlineOrShowError(v)) {
                closeCameraOverlay();
                return;
            }
            closeCameraOverlay();
            pickImageLauncher.launch("image/*");
        });

        View btnCameraFlash = view.findViewById(R.id.btnCameraFlash);
        btnCameraFlash.setOnClickListener(v -> toggleTorch());

        View btnCameraFlip = view.findViewById(R.id.btnCameraFlip);
        btnCameraFlip.setOnClickListener(v -> flipCamera());

        updatePhotoState();

        getParentFragmentManager().setFragmentResultListener(
                com.aula.volta.ui.error.OfflineErrorFragment.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    if (result.getBoolean(com.aula.volta.ui.error.OfflineErrorFragment.RESULT_ONLINE, false)
                            && photoFile != null && photoFile.exists() && btnSendAnalysis != null) {
                        if (cardSubmissionError != null) {
                            cardSubmissionError.setVisibility(View.GONE);
                        }
                        btnSendAnalysis.setText("Enviar para análise");
                        sendToAnalysis(btnSendAnalysis);
                    }
                });

        if (!NetworkUtils.isOnline(requireContext())) {
            ensureOnlineOrShowError(view);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (btnSendAnalysis != null) {
            btnSendAnalysis.setEnabled(photoFile != null && photoFile.exists());
        }
    }

    private void preloadAreasAndWasteTypes() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        AiAnalysisAPI api = ApiClient.get(requireContext()).create(AiAnalysisAPI.class);
        api.getMyAreas().enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(@NonNull Call<JsonArray> call, @NonNull Response<JsonArray> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().size() > 0) {
                    applyAreasArray(response.body());
                } else {
                    api.getSummary().enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(@NonNull Call<JsonObject> call2, @NonNull Response<JsonObject> sumResp) {
                            if (!isAdded() || !sumResp.isSuccessful() || sumResp.body() == null) return;
                            JsonObject sum = sumResp.body();
                            if (sum.has("areas") && sum.get("areas").isJsonArray()) {
                                applyAreasArray(sum.getAsJsonArray("areas"));
                            }
                            if (sum.has("wasteTypes") && sum.get("wasteTypes").isJsonArray()) {
                                PrefsHelper.putJson(requireContext(), "cache_waste_types", "list", sum.getAsJsonArray("wasteTypes").toString());
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<JsonObject> call2, @NonNull Throwable t) {
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonArray> call, @NonNull Throwable t) {
            }
        });

        api.getWasteTypes().enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(@NonNull Call<JsonArray> call, @NonNull Response<JsonArray> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().size() > 0) {
                    PrefsHelper.putJson(requireContext(), "cache_waste_types", "list", response.body().toString());
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonArray> call, @NonNull Throwable t) {
            }
        });
    }

    private void applyAreasArray(JsonArray areas) {
        if (areas == null || areas.size() == 0 || !isAdded()) return;
        for (int i = 0; i < areas.size(); i++) {
            JsonElement el = areas.get(i);
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                if (obj.has("id") && !obj.get("id").isJsonNull()) {
                    String id = obj.get("id").getAsString();
                    PrefsHelper.putJson(requireContext(), "cache_areas", id, obj.toString());
                    if (selectedAreaId == null || selectedAreaId.isEmpty()) {
                        selectedAreaId = id;
                        if (obj.has("sectorName") && !obj.get("sectorName").isJsonNull()) {
                            selectedSectorName = obj.get("sectorName").getAsString().trim();
                            if (tvSector != null && !selectedSectorName.isEmpty()) {
                                tvSector.setText(selectedSectorName);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean ensureOnlineOrShowError(@Nullable View anchor) {
        if (!isAdded() || getContext() == null) {
            return false;
        }
        if (NetworkUtils.isOnline(requireContext())) {
            return true;
        }
        Bundle errArgs = new Bundle();
        errArgs.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_ERROR_TYPE,
                com.aula.volta.ui.error.OfflineErrorFragment.TYPE_OFFLINE);
        errArgs.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_BADGE, "Você está offline");
        errArgs.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_TITLE, "Sem conexão para fotografar");
        errArgs.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_MESSAGE,
                "Você precisa de conexão com a internet para tirar fotos e registrar ocorrências no banco de dados.");
        errArgs.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_INFO_TITLE, "Câmera e registro bloqueados offline");
        errArgs.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_INFO_BODY,
                "Todas as ocorrências são enviadas diretamente ao banco oficial da unidade. Conecte-se ao Wi-Fi ou dados móveis para continuar.");
        View navTarget = anchor != null ? anchor : getView();
        if (navTarget != null) {
            try {
                Navigation.findNavController(navTarget).navigate(R.id.nav_offline_error, errArgs);
            } catch (Exception ignored) {
                Toast.makeText(requireContext(), "Sem conexão com a internet para tirar fotos.", Toast.LENGTH_SHORT).show();
            }
        }
        return false;
    }

    private void checkPermissionAndOpenCamera() {
        if (!ensureOnlineOrShowError(getView())) {
            return;
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCameraViewfinder();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private int getDisplayRotation() {
        if (cameraPreviewView != null && cameraPreviewView.getDisplay() != null) {
            return cameraPreviewView.getDisplay().getRotation();
        }
        if (getActivity() != null && getActivity().getWindowManager() != null) {
            return getActivity().getWindowManager().getDefaultDisplay().getRotation();
        }
        return Surface.ROTATION_0;
    }

    private void setupOrientationListener() {
        if (orientationEventListener == null && getContext() != null) {
            orientationEventListener = new OrientationEventListener(requireContext()) {
                @Override
                public void onOrientationChanged(int orientation) {
                    if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN || imageCapture == null) {
                        return;
                    }
                    int rotation;
                    if (orientation >= 45 && orientation < 135) {
                        rotation = Surface.ROTATION_270;
                    } else if (orientation >= 135 && orientation < 225) {
                        rotation = Surface.ROTATION_180;
                    } else if (orientation >= 225 && orientation < 315) {
                        rotation = Surface.ROTATION_90;
                    } else {
                        rotation = Surface.ROTATION_0;
                    }
                    try {
                        imageCapture.setTargetRotation(rotation);
                    } catch (Exception ignored) {
                    }
                }
            };
        }
        if (orientationEventListener != null && orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
        }
    }

    private void disableOrientationListener() {
        if (orientationEventListener != null) {
            orientationEventListener.disable();
        }
    }

    private void openCameraViewfinder() {
        if (!ensureOnlineOrShowError(getView())) {
            return;
        }
        if (cameraViewfinderOverlay != null) {
            cameraViewfinderOverlay.setVisibility(View.VISIBLE);
        }
        setupOrientationListener();
        startCameraX();
    }

    private void closeCameraOverlay() {
        disableOrientationListener();
        stopCameraX();
        if (cameraViewfinderOverlay != null) {
            cameraViewfinderOverlay.setVisibility(View.GONE);
        }
    }

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

            int rotation = getDisplayRotation();

            imageCapture = new ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(rotation)
                    .build();

            camera = cameraProvider.bindToLifecycle(
                    getViewLifecycleOwner(), cameraSelector, preview, imageCapture);

            if (cameraPreviewImage != null) {
                cameraPreviewImage.setVisibility(View.GONE);
            }
        } catch (Exception e) {

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

    private void captureRealPhoto() {
        if (!ensureOnlineOrShowError(getView())) {
            closeCameraOverlay();
            return;
        }
        if (imageCapture == null) {

            closeCameraOverlay();
            openSystemCamera();
            return;
        }

        try {
            imageCapture.setTargetRotation(getDisplayRotation());
        } catch (Exception ignored) {
        }

        File photoOut = new File(requireContext().getCacheDir(),
                "volta_photo_raw_" + System.currentTimeMillis() + ".jpg");

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

                        closeCameraOverlay();
                        openSystemCamera();
                    }
                });
    }

    private void toggleTorch() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            isTorchOn = !isTorchOn;
            camera.getCameraControl().enableTorch(isTorchOn);
            Toast.makeText(requireContext(), isTorchOn ? "Flash ligado" : "Flash desligado", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Flash não disponível", Toast.LENGTH_SHORT).show();
        }
    }

    private void flipCamera() {
        lensFacing = (lensFacing == CameraSelector.LENS_FACING_BACK)
                ? CameraSelector.LENS_FACING_FRONT
                : CameraSelector.LENS_FACING_BACK;
        bindCameraUseCases();
    }

    private void openSystemCamera() {
        if (!ensureOnlineOrShowError(getView())) {
            return;
        }
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
        sheet.setOnSectorSelectedListener((index, sectorName, areaId) -> {
            sectorIndex = index;
            selectedSectorName = sectorName;
            selectedAreaId = areaId;
            tvSector.setText(sectorName);
        });
        sheet.show(getChildFragmentManager(), "select_sector");
    }

    private void updatePhotoState() {
        boolean hasPhoto = photoFile != null && photoFile.exists();
        photoEmptyGroup.setVisibility(hasPhoto ? View.GONE : View.VISIBLE);
        photoPreviewGroup.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);

        if (hasPhoto) {
            Bitmap bmp = ImageUtils.decodeFileWithExifRotation(photoFile.getAbsolutePath(), MAX_PHOTO_DIMEN);
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
        if (!NetworkUtils.isOnline(requireContext())) {
            if (cardSubmissionError != null) {
                cardSubmissionError.setVisibility(View.VISIBLE);
            }
            if (btnSendAnalysis != null) {
                btnSendAnalysis.setEnabled(true);
                btnSendAnalysis.setText("Tentar de novo");
            }
            ensureOnlineOrShowError(v);
            return;
        }
        if (cardSubmissionError != null) {
            cardSubmissionError.setVisibility(View.GONE);
        }
        btnSendAnalysis.setEnabled(false);

        com.aula.volta.data.upload.CloudinaryUploader.uploadImage(requireContext(), photoFile, null);

        Bundle args = new Bundle();
        args.putString("photoPath", photoFile.getAbsolutePath());
        args.putString("descricao", etDescription.getText().toString().trim());
        String sector = selectedSectorName != null ? selectedSectorName : tvSector.getText().toString();
        args.putString("setor", sector);
        if (selectedAreaId != null && !selectedAreaId.isEmpty()) {
            args.putString("areaId", selectedAreaId);
        }
        args.putBoolean("avisar", false);
        Navigation.findNavController(v).navigate(R.id.nav_ai_analysis, args);
    }

    private File copyToCache(Uri uri) {
        if (!isAdded() || getContext() == null) {
            return null;
        }
        return ImageUtils.copyUriToCache(requireContext(), uri);
    }

    private File compressImage(File source) {
        if (source == null || !source.exists() || !isAdded() || getContext() == null) {
            return null;
        }
        return ImageUtils.fixOrientationAndCompress(requireContext(), source, MAX_PHOTO_DIMEN, JPEG_QUALITY);
    }

    @Override
    public void onDestroyView() {
        disableOrientationListener();
        stopCameraX();
        super.onDestroyView();
    }
}
