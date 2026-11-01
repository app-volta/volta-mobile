package com.aula.volta.ui.analysis;

import android.animation.ObjectAnimator;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import com.aula.volta.util.ImageUtils;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.Navigation;

import android.animation.ValueAnimator;
import com.aula.volta.R;
import com.aula.volta.data.api.AiAnalysisAPI;
import com.aula.volta.data.api.ChatbotAPI;
import com.aula.volta.data.api.ChatbotClient;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.ChatbotAuthClient;
import com.aula.volta.data.api.OccurrenceAPI;
import com.aula.volta.data.local.AuditLog;
import com.aula.volta.data.local.NotificationStore;
import com.aula.volta.data.local.OccurrenceStore;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.model.AiAnalysisJSON;
import com.aula.volta.data.model.Notification;
import com.aula.volta.data.model.Occurrence;
import com.aula.volta.data.report.ReportMetricsCalculator;
import com.aula.volta.data.model.incident.AiReportResponseDTO;
import com.aula.volta.data.model.incident.IncidentRequestDTO;
import com.aula.volta.data.model.incident.IncidentResponseDTO;
import com.aula.volta.data.sync.PendingOccurrence;
import com.aula.volta.data.sync.SyncManager;
import com.aula.volta.data.sync.SyncQueue;
import com.aula.volta.data.upload.CloudinaryUploader;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiAnalysisFragment extends Fragment {

    private LinearLayout aiLoadingGroup;
    private LinearLayout aiResultGroup;
    private LinearLayout aiEditGroup;
    private View toastCorrectionSaved;
    private View bottomBarActionsResult;
    private View bottomBarActionsEdit;

    private TextView tvAiHeaderTitle;
    private TextView tvAiHeaderSubtitle;
    private TextView tvAiMaterial;
    private TextView tvAiContamination;
    private TextView tvAiQuantity;
    private TextView tvAiUnits;
    private TextView tvAiUnitsLabel;
    private TextView tvAiRecommendation;
    private TextView tvAiStatusPill;
    private ImageView icAiStatusPill;
    private TextView badgeQuantityEdited;
    private LinearLayout cardAiQuantity;
    private ImageView icMiddleStatus;
    private TextView tvMiddleStatusHint;
    private com.google.android.material.button.MaterialButton btnConfirm;

    private LinearLayout cardAiType;
    private TextView tvAiTypeLabel;
    private TextView badgeAiPriority;

    private TextView tvEditWeightValue;
    private TextView tvEditUnitsValue;
    private int currentWeight = 42;
    private int currentUnits = 11;
    private int initialEstimatedWeight = 42;
    private int initialEstimatedUnits = 11;
    private String selectedMaterial = "";
    private String selectedClass = "";
    private String selectedContamination = "Baixa";
    private boolean isRevised = false;

    private int snapshotWeight = 10;
    private int snapshotUnits = 1;
    private String snapshotMaterial = "";
    private String snapshotClass = "";
    private String snapshotContamination = "Baixa";

    private AiAnalysisJSON analysis;
    private com.google.gson.JsonObject lastPredictJson;
    private String currentPriority = "BAIXA";
    private AnimatedImageDrawable animatedScanDrawable;
    private ImageView imgScannerGif;
    private ObjectAnimator scanAnimator;
    private final Handler scanHandler = new Handler(Looper.getMainLooper());
    private TextView tvScanStatus;
    private long scanStartTime = 0;
    private boolean analysisCompleted = false;
    private Runnable scanTimeoutRunnable;
    private static final long SCAN_MIN_TIME_MS = 2000;
    private static final long SCAN_MAX_TIMEOUT_MS = 60000;
    private final Runnable hideCorrectionToastRunnable = () -> {
        if (isAdded() && toastCorrectionSaved != null) {
            toastCorrectionSaved.animate()
                    .alpha(0f)
                    .setDuration(400)
                    .withEndAction(() -> {
                        if (toastCorrectionSaved != null) {
                            toastCorrectionSaved.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }
    };

    private String photoPath;
    private String remoteImageUrl;
    private String createdIncidentId;
    private String descricao;
    private String setor;
    private String explicitAreaId;

    public AiAnalysisFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai_analysis, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        photoPath = args != null ? args.getString("photoPath") : null;
        if (args != null) {
            remoteImageUrl = args.getString("imageUrl", args.getString("photoUrl", null));
        }
        descricao = args != null && args.getString("descricao") != null
                ? args.getString("descricao").trim() : "";
        setor = args != null ? args.getString("setor") : null;
        explicitAreaId = args != null ? args.getString("areaId") : null;

        aiLoadingGroup = view.findViewById(R.id.aiLoadingGroup);
        tvScanStatus = view.findViewById(R.id.tvScanStatus);
        aiResultGroup = view.findViewById(R.id.aiResultGroup);
        aiEditGroup = view.findViewById(R.id.aiEditGroup);
        toastCorrectionSaved = view.findViewById(R.id.toastCorrectionSaved);
        bottomBarActionsResult = view.findViewById(R.id.bottomBarActionsResult);
        bottomBarActionsEdit = view.findViewById(R.id.bottomBarActionsEdit);

        if (bottomBarActionsResult != null) {
            bottomBarActionsResult.setVisibility(View.GONE);
        }
        if (bottomBarActionsEdit != null) {
            bottomBarActionsEdit.setVisibility(View.GONE);
        }

        tvAiHeaderTitle = view.findViewById(R.id.tvAiHeaderTitle);
        tvAiHeaderSubtitle = view.findViewById(R.id.tvAiHeaderSubtitle);
        tvAiMaterial = view.findViewById(R.id.tvAiMaterial);
        tvAiContamination = view.findViewById(R.id.tvAiContamination);
        tvAiQuantity = view.findViewById(R.id.tvAiQuantity);
        tvAiUnits = view.findViewById(R.id.tvAiUnits);
        tvAiUnitsLabel = view.findViewById(R.id.tvAiUnitsLabel);
        tvAiRecommendation = view.findViewById(R.id.tvAiRecommendation);
        tvAiStatusPill = view.findViewById(R.id.tvAiStatusPill);
        icAiStatusPill = view.findViewById(R.id.icAiStatusPill);
        badgeQuantityEdited = view.findViewById(R.id.badgeQuantityEdited);
        cardAiQuantity = view.findViewById(R.id.cardAiQuantity);
        cardAiType = view.findViewById(R.id.cardAiType);
        tvAiTypeLabel = view.findViewById(R.id.tvAiTypeLabel);
        badgeAiPriority = view.findViewById(R.id.badgeAiPriority);
        icMiddleStatus = view.findViewById(R.id.icMiddleStatus);
        tvMiddleStatusHint = view.findViewById(R.id.tvMiddleStatusHint);

        ImageView imgAiPhoto = view.findViewById(R.id.imgAiPhoto);
        if (photoPath != null && imgAiPhoto != null) {
            imgAiPhoto.setImageBitmap(ImageUtils.decodeFileWithExifRotation(photoPath, 1280));
            imgAiPhoto.setOnClickListener(v -> {
                String title = (tvAiMaterial != null && tvAiMaterial.getText() != null)
                        ? tvAiMaterial.getText().toString() : "Foto do Resíduo";
                com.aula.volta.ui.dialog.ImagePreviewDialog.show(
                        getParentFragmentManager(),
                        photoPath,
                        title,
                        setor != null ? setor : "Análise de IA"
                );
            });
        }

        ImageView imgAiPhotoScan = view.findViewById(R.id.imgAiPhotoScan);
        if (photoPath != null && imgAiPhotoScan != null) {
            imgAiPhotoScan.setImageBitmap(ImageUtils.decodeFileWithExifRotation(photoPath, 1280));
            imgAiPhotoScan.setOnClickListener(v -> {
                com.aula.volta.ui.dialog.ImagePreviewDialog.show(
                        getParentFragmentManager(),
                        photoPath,
                        "Imagem em Análise",
                        setor != null ? setor : "Identificação de IA"
                );
            });
        }

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (aiEditGroup != null && aiEditGroup.getVisibility() == View.VISIBLE) {
                            cancelEditView();
                        } else {
                            showExitConfirmationDialog();
                        }
                    }
                });

        View btnBack = view.findViewById(R.id.btnBackAnalysis);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (aiEditGroup != null && aiEditGroup.getVisibility() == View.VISIBLE) {
                    cancelEditView();
                } else {
                    showExitConfirmationDialog();
                }
            });
        }

        MaterialButton btnEdit = view.findViewById(R.id.btnEditAnalysis);
        btnEdit.setOnClickListener(v -> switchToEditView());

        btnConfirm = view.findViewById(R.id.btnConfirmAnalysis);
        btnConfirm.setOnClickListener(this::confirmOccurrence);

        setupEditControls(view);

        if (toastCorrectionSaved != null) {
            com.aula.volta.util.ToastSwipeHelper.attach(toastCorrectionSaved, hideCorrectionToastRunnable,
                    new com.aula.volta.util.ToastSwipeHelper.OnSwipeListener() {
                        @Override
                        public void onDismissed() {
                            if (toastCorrectionSaved != null) {
                                toastCorrectionSaved.setVisibility(View.GONE);
                            }
                        }

                        @Override
                        public void onSwipeDown() {
                            if (isAdded()) {
                                com.aula.volta.data.report.PgrsReport.openPdf(requireContext(), null);
                            }
                        }
                    });
        }

        if (analysis != null) {
            applyAnalysis();
        } else {
            startScanAnimation(view);
            analyzePhoto();
        }
    }

    @Override
    public void onDestroyView() {
        stopScanAnimation();
        if (toastCorrectionSaved != null) {
            toastCorrectionSaved.removeCallbacks(hideCorrectionToastRunnable);
            toastCorrectionSaved.animate().cancel();
        }
        super.onDestroyView();
    }

    private void startScanAnimation(View root) {
        imgScannerGif = root.findViewById(R.id.imgScannerGif);
        View band = root.findViewById(R.id.viewScanBand);
        View frame = root.findViewById(R.id.scanPhotoFrame);
        tvScanStatus = root.findViewById(R.id.tvScanStatus);

        stopScanAnimation();

        boolean gifStarted = false;
        if (imgScannerGif != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                ImageDecoder.Source source = ImageDecoder.createSource(getResources(), R.drawable.scanner_analisando);
                Drawable drawable = ImageDecoder.decodeDrawable(source);
                imgScannerGif.setImageDrawable(drawable);
                if (drawable instanceof AnimatedImageDrawable) {
                    animatedScanDrawable = (AnimatedImageDrawable) drawable;
                    animatedScanDrawable.setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE);
                    animatedScanDrawable.start();
                    gifStarted = true;
                }
            } catch (Throwable e) {
                try {
                    ImageDecoder.Source source = ImageDecoder.createSource(requireContext().getAssets(), "scanner_analisando.gif");
                    Drawable drawable = ImageDecoder.decodeDrawable(source);
                    imgScannerGif.setImageDrawable(drawable);
                    if (drawable instanceof AnimatedImageDrawable) {
                        animatedScanDrawable = (AnimatedImageDrawable) drawable;
                        animatedScanDrawable.setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE);
                        animatedScanDrawable.start();
                        gifStarted = true;
                    }
                } catch (Throwable ex) {
                    Log.e("AiAnalysisFragment", "Erro ao iniciar scanner GIF", ex);
                }
            }
        }

        if (!gifStarted && band != null && frame != null) {
            band.setVisibility(View.VISIBLE);
            frame.post(() -> {
                if (!isAdded() || getView() == null) {
                    return;
                }
                int frameH = frame.getHeight();
                int bandH = band.getHeight();
                float density = getResources().getDisplayMetrics().density;
                if (frameH <= 0) {
                    frameH = (int) (170 * density);
                }
                if (bandH <= 0) {
                    bandH = (int) (52 * density);
                }

                float maxTranslation = Math.max(10, frameH - bandH);
                scanAnimator = ObjectAnimator.ofFloat(band, "translationY", 0f, maxTranslation);
                scanAnimator.setDuration(1200);
                scanAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
                scanAnimator.setRepeatCount(ValueAnimator.INFINITE);
                scanAnimator.setRepeatMode(ValueAnimator.REVERSE);
                scanAnimator.start();
            });
        }

        scanHandler.removeCallbacksAndMessages(null);
        if (tvScanStatus != null) {
            tvScanStatus.setText("Escaneando resíduo…");
        }
        scanHandler.postDelayed(() -> {
            if (isAdded() && tvScanStatus != null && !analysisCompleted) {
                tvScanStatus.setText("Identificando material…");
            }
        }, 1800);
        scanHandler.postDelayed(() -> {
            if (isAdded() && tvScanStatus != null && !analysisCompleted) {
                tvScanStatus.setText("Consultando IA Gemini Vision…");
            }
        }, 4500);
        scanHandler.postDelayed(() -> {
            if (isAdded() && tvScanStatus != null && !analysisCompleted) {
                tvScanStatus.setText("Avaliando contaminação e volume…");
            }
        }, 9000);
        scanHandler.postDelayed(() -> {
            if (isAdded() && tvScanStatus != null && !analysisCompleted) {
                tvScanStatus.setText("Validando com juiz LLM…");
            }
        }, 14000);
        scanHandler.postDelayed(() -> {
            if (isAdded() && tvScanStatus != null && !analysisCompleted) {
                tvScanStatus.setText("Gerando laudo técnico…");
            }
        }, 18000);
    }

    private void stopScanAnimation() {
        if (animatedScanDrawable != null) {
            try {
                animatedScanDrawable.stop();
            } catch (Exception ignored) {
            }
            animatedScanDrawable = null;
        }
        if (scanAnimator != null) {
            scanAnimator.cancel();
            scanAnimator = null;
        }
        if (scanTimeoutRunnable != null) {
            scanHandler.removeCallbacks(scanTimeoutRunnable);
            scanTimeoutRunnable = null;
        }
        scanHandler.removeCallbacksAndMessages(null);
    }

    private void finishAnalysisWithTransition(AiAnalysisJSON result) {
        if (!isAdded() || analysisCompleted) {
            return;
        }
        analysisCompleted = true;
        if (scanTimeoutRunnable != null) {
            scanHandler.removeCallbacks(scanTimeoutRunnable);
            scanTimeoutRunnable = null;
        }

        long elapsed = System.currentTimeMillis() - scanStartTime;
        long remaining = Math.max(0, SCAN_MIN_TIME_MS - elapsed);

        scanHandler.postDelayed(() -> {
            if (!isAdded()) return;
            this.analysis = result;
            stopScanAnimation();

            if (aiLoadingGroup != null && aiResultGroup != null) {
                aiLoadingGroup.animate()
                        .alpha(0f)
                        .setDuration(220)
                        .withEndAction(() -> {
                            if (!isAdded()) return;
                            aiLoadingGroup.setVisibility(View.GONE);
                            aiLoadingGroup.setAlpha(1f);
                            applyAnalysis();
                            aiResultGroup.setVisibility(View.VISIBLE);
                            aiResultGroup.setAlpha(0f);
                            aiResultGroup.animate()
                                    .alpha(1f)
                                    .setDuration(280)
                                    .start();
                        })
                        .start();
            } else {
                applyAnalysis();
            }
        }, remaining);
    }

    private JsonObject createAnalysisBody(@Nullable String imageUrl) {
        JsonObject body = new JsonObject();
        body.addProperty("setor", setor == null ? "" : setor);
        body.addProperty("descricao", descricao == null ? "" : descricao);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            body.addProperty("photoUrl", imageUrl);
            body.addProperty("imageUrl", imageUrl);
            body.addProperty("foto_url", imageUrl);
        }
        return body;
    }

    private void analyzePhoto() {
        scanStartTime = System.currentTimeMillis();
        analysisCompleted = false;

        scanTimeoutRunnable = () -> {
            if (!isAdded() || analysisCompleted) return;
            Log.i("AiAnalysisFragment", "Limite de tempo de escaneamento atingido (3.8s). Finalizando via análise contratual.");
            executeLocalContractAnalysis(createAnalysisBody(remoteImageUrl));
        };
        scanHandler.postDelayed(scanTimeoutRunnable, SCAN_MAX_TIMEOUT_MS);

        if (photoPath != null && !photoPath.isEmpty()) {
            final File imageFile = new File(photoPath);
            if (imageFile.exists()) {

                CloudinaryUploader.uploadImage(requireContext(), imageFile, new CloudinaryUploader.UploadCallback() {
                    @Override
                    public void onSuccess(String remoteUrl, String publicId) {
                        remoteImageUrl = remoteUrl;
                        Log.i("AiAnalysisFragment", "Cloudinary secure_url gerada: " + remoteUrl);
                    }

                    @Override
                    public void onError(Exception exception, String fallbackLocalPath) {
                        Log.w("AiAnalysisFragment", "Upload Cloudinary falhou: " + exception.getMessage());
                    }
                });

                if (SyncManager.isOnline(requireContext())) {
                    sendImageDirectlyToAi(imageFile);
                    return;
                }
            }
        }

        executeLocalContractAnalysis(createAnalysisBody(remoteImageUrl));
    }

    private void sendImageDirectlyToAi(File imageFile) {
        if (!isAdded() || getContext() == null) {
            return;
        }

        String fileName = imageFile.getName() != null ? imageFile.getName().toLowerCase(Locale.ROOT) : "";
        String mime = (fileName.endsWith(".png")) ? "image/png" : "image/jpeg";
        RequestBody reqFile = RequestBody.create(MediaType.parse(mime), imageFile);
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", imageFile.getName(), reqFile);

        ChatbotAPI chatApi = ChatbotClient.get(requireContext()).create(ChatbotAPI.class);
        chatApi.predictWaste(part).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (!isAdded() || analysisCompleted) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject predictJson = response.body();
                    Log.i("AiAnalysisFragment", "Gemini IA Predict sucesso: " + predictJson);
                    lastPredictJson = predictJson;
                    finishAnalysisWithTransition(AiAnalysisJSON.fromPredict(predictJson, 0));
                } else {
                    Log.w("AiAnalysisFragment", "predictWaste retornou HTTP " + response.code() + ", aplicando análise contratual rápida");
                    executeLocalContractAnalysis(createAnalysisBody(remoteImageUrl));
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (!isAdded() || analysisCompleted) {
                    return;
                }
                Log.w("AiAnalysisFragment", "predictWaste falhou na rede: " + t.getMessage() + ", aplicando análise contratual rápida");
                executeLocalContractAnalysis(createAnalysisBody(remoteImageUrl));
            }
        });
    }

    private void executeAiAnalysisRequest(@Nullable String imageUrl) {
        if (!isAdded()) {
            return;
        }
        final JsonObject body = new JsonObject();
        body.addProperty("setor", setor == null ? "" : setor);
        body.addProperty("descricao", descricao == null ? "" : descricao);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            body.addProperty("photoUrl", imageUrl);
            body.addProperty("imageUrl", imageUrl);
            body.addProperty("foto_url", imageUrl);
        }

        if (imageUrl != null && imageUrl.startsWith("http") && SyncManager.isOnline(requireContext())) {
            final AiAnalysisAPI qaApi = ChatbotAuthClient.get(requireContext()).create(AiAnalysisAPI.class);
            tryQaIncidentAiReportFlow(qaApi, imageUrl, body);
        } else {
            executeLocalContractAnalysis(body);
        }
    }

    private void tryQaIncidentAiReportFlow(final AiAnalysisAPI qaApi,
                                           final String cloudinaryUrl,
                                           final JsonObject fallbackBody) {
        qaApi.getMyAreas().enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(@NonNull Call<JsonArray> call, @NonNull Response<JsonArray> areaResp) {
                if (!isAdded()) {
                    return;
                }
                final String areaId = AiAnalysisJSON.resolveAreaId(areaResp.body(), setor);
                if (areaId == null) {
                    executeLocalContractAnalysis(fallbackBody);
                    return;
                }
                qaApi.getWasteTypes().enqueue(new Callback<JsonArray>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonArray> call, @NonNull Response<JsonArray> wasteResp) {
                        if (!isAdded()) {
                            return;
                        }
                        final String wasteTypeId = AiAnalysisJSON.resolveWasteTypeId(wasteResp.body(), descricao);
                        if (wasteTypeId == null) {
                            executeLocalContractAnalysis(fallbackBody);
                            return;
                        }
                        String empDesc = (descricao != null && !descricao.isEmpty())
                                ? descricao
                                : "Resíduo registrado via app mobile no setor " + (setor != null ? setor : "Operacional");
                        IncidentRequestDTO req = new IncidentRequestDTO(
                                areaId,
                                wasteTypeId,
                                cloudinaryUrl,
                                empDesc,
                                "LOW",
                                38.0,
                                "MEDIUM"
                        );
                        qaApi.createIncident(req).enqueue(new Callback<IncidentResponseDTO>() {
                            @Override
                            public void onResponse(@NonNull Call<IncidentResponseDTO> call,
                                                   @NonNull Response<IncidentResponseDTO> incResp) {
                                if (!isAdded()) {
                                    return;
                                }
                                if (incResp.isSuccessful() && incResp.body() != null && incResp.body().getId() != null) {
                                    final String incidentId = incResp.body().getId();
                                    createdIncidentId = incidentId;
                                    final double estQty = incResp.body().getEstimatedQuantity() != null
                                            ? incResp.body().getEstimatedQuantity() : 38.0;
                                    qaApi.getIncidentAiReport(incidentId).enqueue(new Callback<AiReportResponseDTO>() {
                                        @Override
                                        public void onResponse(@NonNull Call<AiReportResponseDTO> call,
                                                               @NonNull Response<AiReportResponseDTO> reportResp) {
                                            if (!isAdded()) {
                                                return;
                                            }
                                            if (reportResp.isSuccessful() && reportResp.body() != null) {
                                                analysis = AiAnalysisJSON.fromAiReport(reportResp.body(), estQty);
                                                analysis.setIncidentId(incidentId);
                                                applyAnalysis();
                                            } else {
                                                fetchScoringContextOrFallback(qaApi, incidentId, estQty, fallbackBody);
                                            }
                                        }

                                        @Override
                                        public void onFailure(@NonNull Call<AiReportResponseDTO> call, @NonNull Throwable t) {
                                            if (isAdded()) {
                                                fetchScoringContextOrFallback(qaApi, incidentId, estQty, fallbackBody);
                                            }
                                        }
                                    });
                                } else {
                                    executeLocalContractAnalysis(fallbackBody);
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<IncidentResponseDTO> call, @NonNull Throwable t) {
                                if (isAdded()) {
                                    executeLocalContractAnalysis(fallbackBody);
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonArray> call, @NonNull Throwable t) {
                        if (isAdded()) {
                            executeLocalContractAnalysis(fallbackBody);
                        }
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<JsonArray> call, @NonNull Throwable t) {
                if (isAdded()) {
                    executeLocalContractAnalysis(fallbackBody);
                }
            }
        });
    }

    private void fetchScoringContextOrFallback(final AiAnalysisAPI qaApi,
                                               final String incidentId,
                                               final double estQty,
                                               final JsonObject fallbackBody) {
        qaApi.getIncidentScoringContext(incidentId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    AiAnalysisJSON fromCtx = AiAnalysisJSON.fromScoringContext(response.body(), estQty);
                    if (fromCtx != null) {
                        fromCtx.setIncidentId(incidentId);
                        finishAnalysisWithTransition(fromCtx);
                        return;
                    }
                }
                executeLocalContractAnalysis(fallbackBody);
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (isAdded()) {
                    executeLocalContractAnalysis(fallbackBody);
                }
            }
        });
    }

    private void executeLocalContractAnalysis(JsonObject body) {
        if (!isAdded()) {
            return;
        }
        AiAnalysisAPI api = ApiClient.get(requireContext()).create(AiAnalysisAPI.class);
        api.analyzeOccurrence("novo", body).enqueue(new Callback<AiAnalysisJSON>() {
            @Override
            public void onResponse(@NonNull Call<AiAnalysisJSON> call,
                                   @NonNull Response<AiAnalysisJSON> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    AiAnalysisJSON res = response.body();
                    if (createdIncidentId != null && !createdIncidentId.isEmpty()) {
                        res.setIncidentId(createdIncidentId);
                    }
                    finishAnalysisWithTransition(res);
                } else {
                    useOfflineAnalysisFallback();
                }
            }

            @Override
            public void onFailure(@NonNull Call<AiAnalysisJSON> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                useOfflineAnalysisFallback();
            }
        });
    }

    private void useOfflineAnalysisFallback() {
        if (!isAdded() || analysisCompleted) return;
        AiAnalysisJSON off = AiAnalysisJSON.createOfflineFallback(setor);
        if (createdIncidentId != null && !createdIncidentId.isEmpty()) {
            off.setIncidentId(createdIncidentId);
        }
        finishAnalysisWithTransition(off);
    }

    private void applyAnalysis() {
        stopScanAnimation();
        aiLoadingGroup.setVisibility(View.GONE);
        aiEditGroup.setVisibility(View.GONE);
        aiResultGroup.setVisibility(View.VISIBLE);
        if (bottomBarActionsResult != null) {
            bottomBarActionsResult.setVisibility(View.VISIBLE);
        }
        if (bottomBarActionsEdit != null) {
            bottomBarActionsEdit.setVisibility(View.GONE);
        }

        if (analysis == null) {
            return;
        }

        boolean weightChanged = (currentWeight != initialEstimatedWeight);
        boolean unitsChanged = (currentUnits != initialEstimatedUnits);

        if (isRevised) {
            String specific = (analysis != null && analysis.getSpecificItem() != null)
                    ? analysis.getSpecificItem() : "";
            selectPrimaryMaterialFamily(selectedMaterial, false);
            tvAiContamination.setText(selectedContamination);
            tvAiQuantity.setText("≈ " + currentWeight + " kg");

            if (badgeQuantityEdited != null) {
                badgeQuantityEdited.setVisibility(weightChanged ? View.VISIBLE : View.GONE);
            }
            if (cardAiQuantity != null) {
                cardAiQuantity.setBackgroundResource(weightChanged
                        ? R.drawable.bg_card_quantity_edited
                        : R.drawable.bg_card_stat_recycled);
            }

            TextView badgeUnitsResult = getView() != null ? getView().findViewById(R.id.badgeUnitsEditedResult) : null;
            LinearLayout cardUnits = getView() != null ? getView().findViewById(R.id.cardAiUnits) : null;
            if (badgeUnitsResult != null) {
                badgeUnitsResult.setVisibility(unitsChanged ? View.VISIBLE : View.GONE);
            }
            if (cardUnits != null) {
                cardUnits.setBackgroundResource(unitsChanged
                        ? R.drawable.bg_card_units_edited
                        : R.drawable.bg_card_stat_purple);
            }

            if (icAiStatusPill != null) {
                icAiStatusPill.setImageResource(R.drawable.ic_pencil);
                icAiStatusPill.setColorFilter(Color.WHITE);
            }
            tvAiStatusPill.setText("Revisado por você");
            tvMiddleStatusHint.setText("Resultados revisados · prontos pro relatório");
            tvMiddleStatusHint.setTextColor(requireContext().getColor(R.color.vf_text_secondary));
            if (icMiddleStatus != null) {
                icMiddleStatus.setImageResource(R.drawable.ic_check_circle_green);
                icMiddleStatus.setVisibility(View.VISIBLE);
            }
        } else {
            String rawMat = analysis.getMaterial() != null ? analysis.getMaterial().trim() : "Reciclável";
            selectedClass = "";
            String primary = analysis.getPrimaryMaterial();
            String specific = analysis.getSpecificItem();
            String resolvedFamily = ReportMetricsCalculator.resolveMaterialFamily(primary != null ? primary : rawMat);
            selectedMaterial = resolvedFamily;

            selectedContamination = (analysis.getContaminacao() != null
                    && analysis.getContaminacao().getNivel() != null)
                    ? AiAnalysisJSON.mapContaminationLabel(analysis.getContaminacao().getNivel())
                    : "Baixa";

            currentPriority = AiAnalysisJSON.calculatePriority(rawMat, selectedContamination, descricao);
            updatePriorityBadge(currentPriority);

            if (analysis.getQuantidadeEstimada() > 0) {
                initialEstimatedWeight = (int) Math.round(analysis.getQuantidadeEstimada());
                currentWeight = initialEstimatedWeight;
            }
            if (analysis.getUnidades() != null && analysis.getUnidades().getQuantidade() > 0) {
                initialEstimatedUnits = analysis.getUnidades().getQuantidade();
                currentUnits = initialEstimatedUnits;
            }
            if (tvEditWeightValue != null) {
                tvEditWeightValue.setText(String.valueOf(currentWeight));
            }
            if (tvEditUnitsValue != null) {
                tvEditUnitsValue.setText(String.valueOf(currentUnits));
            }

            selectPrimaryMaterialFamily(resolvedFamily, false);
            tvAiContamination.setText(selectedContamination);
            String quantidade = "≈ " + trimNumber(analysis.getQuantidadeEstimada()) + " " + analysis.getUnidade();
            tvAiQuantity.setText(quantidade);
            badgeQuantityEdited.setVisibility(View.GONE);
            cardAiQuantity.setBackgroundResource(R.drawable.bg_card_stat_recycled);
            TextView badgeUnitsResult = getView() != null ? getView().findViewById(R.id.badgeUnitsEditedResult) : null;
            if (badgeUnitsResult != null) badgeUnitsResult.setVisibility(View.GONE);
            LinearLayout cardUnits = getView() != null ? getView().findViewById(R.id.cardAiUnits) : null;
            if (cardUnits != null) cardUnits.setBackgroundResource(R.drawable.bg_card_stat_purple);
            if (icAiStatusPill != null) {
                icAiStatusPill.setImageResource(R.drawable.ic_sparkle);
                icAiStatusPill.setColorFilter(Color.WHITE);
            }
            tvAiStatusPill.setText(getString(R.string.ai_done));
            tvMiddleStatusHint.setText("Confira os resultados antes de gerar o relatório");
            tvMiddleStatusHint.setTextColor(requireContext().getColor(R.color.vf_text_secondary));
            if (icMiddleStatus != null) {
                icMiddleStatus.setVisibility(View.GONE);
            }
        }

        if (analysis.getUnidades() != null && analysis.getUnidades().getTipo() != null) {
            tvAiUnitsLabel.setText(analysis.getUnidades().getTipo());
        } else {
            tvAiUnitsLabel.setText("Caixas");
        }
        tvAiUnits.setText(currentUnits + " un.");

        if (analysis.getObservacoes() != null && !analysis.getObservacoes().isEmpty()) {
            tvAiRecommendation.setText(analysis.getObservacoes());
        }
    }

    private void selectPrimaryMaterialFamily(String rawFamily, boolean userInitiated) {
        String family = ReportMetricsCalculator.resolveMaterialFamily(rawFamily);
        selectedMaterial = family;

        updateTypeCardUi(family);

        String specific = (analysis != null && analysis.getSpecificItem() != null)
                ? analysis.getSpecificItem() : "";
        String formattedMat = AiAnalysisJSON.formatMaterialWithClass(family, specific, selectedClass);
        currentPriority = AiAnalysisJSON.calculatePriority(formattedMat, selectedContamination, descricao);
        updatePriorityBadge(currentPriority);

        if (userInitiated) {
            updateRecommendationForMaterial(family);
        }

        if (getView() != null) {
            updateMaterialChipsUi(getView());
        }
    }

    private void updateTypeCardUi(String family) {
        if (tvAiMaterial == null) return;
        String specific = (analysis != null && analysis.getSpecificItem() != null)
                ? analysis.getSpecificItem() : "";
        String formatted = AiAnalysisJSON.formatMaterialWithClass(family, specific, selectedClass);
        tvAiMaterial.setText(formatted);

        String lower = family != null ? family.toLowerCase(Locale.ROOT) : "";
        int textColor;
        int cardBg;

        if (lower.contains("papel")) {
            textColor = Color.parseColor("#C68A10");
            cardBg = R.drawable.bg_card_type_gold;
        } else if (lower.contains("plást") || lower.contains("plast")) {
            textColor = Color.parseColor("#3B82F6");
            cardBg = R.drawable.bg_card_mat_blue;
        } else if (lower.contains("metal")) {
            textColor = Color.parseColor("#8B6FE0");
            cardBg = R.drawable.bg_card_mat_purple;
        } else if (lower.contains("vidro")) {
            textColor = Color.parseColor("#6B7280");
            cardBg = R.drawable.bg_card_mat_gray;
        } else if (lower.contains("madeira")) {
            textColor = Color.parseColor("#D97706");
            cardBg = R.drawable.bg_card_mat_amber;
        } else if (lower.contains("orgân") || lower.contains("organ")) {
            textColor = Color.parseColor("#10B981");
            cardBg = R.drawable.bg_card_mat_green;
        } else {
            textColor = Color.parseColor("#475569");
            cardBg = R.drawable.bg_card_mat_gray;
        }

        tvAiMaterial.setTextColor(textColor);
        if (tvAiTypeLabel != null) {
            tvAiTypeLabel.setTextColor(textColor);
        }
        if (cardAiType != null) {
            cardAiType.setBackgroundResource(cardBg);
        }
    }

    private void updateRecommendationForMaterial(String family) {
        if (tvAiRecommendation == null) return;
        String lower = family != null ? family.toLowerCase(Locale.ROOT) : "";
        if (lower.contains("papel")) {
            tvAiRecommendation.setText("Armazene em área coberta e seca para preservar a celulose e o valor de reciclagem.");
        } else if (lower.contains("plást") || lower.contains("plast")) {
            tvAiRecommendation.setText("Compacte e enfarde para otimizar espaço e facilitar a logística com a cooperativa parceira.");
        } else if (lower.contains("metal")) {
            tvAiRecommendation.setText("Separe por liga e proteja recipientes com pontas perfurantes para segurança dos operadores.");
        } else if (lower.contains("vidro")) {
            tvAiRecommendation.setText("Acondicione em caixas reforçadas e sinalize para prevenir quebras e acidentes na triagem.");
        } else if (lower.contains("madeira")) {
            tvAiRecommendation.setText("Empilhe paletes em área nivelada e seca para viabilizar reuso imediato ou trituração energética.");
        } else if (lower.contains("orgân") || lower.contains("organ")) {
            tvAiRecommendation.setText("Encaminhe rapidamente para compostagem ou biogás antes de atrair vetores e odores.");
        } else {
            tvAiRecommendation.setText("Identifique e segregue em recipientes adequados conforme as diretrizes ambientais internas.");
        }
    }

    private void updatePriorityBadge(String priority) {
        if (badgeAiPriority == null || getContext() == null) return;
        if (priority == null) priority = "BAIXA";
        String p = priority.toUpperCase(Locale.ROOT);
        if (p.contains("ALT") || p.contains("HIGH")) {
            badgeAiPriority.setText(R.string.priority_alta);
            badgeAiPriority.setBackgroundResource(R.drawable.bg_badge_alta);
            badgeAiPriority.setTextColor(requireContext().getColor(R.color.priority_high_text));
        } else if (p.contains("MÉD") || p.contains("MED") || p.contains("MEDIUM")) {
            badgeAiPriority.setText(R.string.priority_media);
            badgeAiPriority.setBackgroundResource(R.drawable.bg_badge_media);
            badgeAiPriority.setTextColor(requireContext().getColor(R.color.priority_medium_text));
        } else {
            badgeAiPriority.setText(R.string.priority_baixa);
            badgeAiPriority.setBackgroundResource(R.drawable.bg_badge_baixa);
            badgeAiPriority.setTextColor(requireContext().getColor(R.color.priority_low_text));
        }
    }

    private void setupEditControls(View root) {
        tvEditWeightValue = root.findViewById(R.id.tvEditWeightValue);
        tvEditUnitsValue = root.findViewById(R.id.tvEditUnitsValue);

        ImageView imgEditPhotoThumb = root.findViewById(R.id.imgEditPhotoThumb);
        if (photoPath != null && imgEditPhotoThumb != null) {
            imgEditPhotoThumb.setImageBitmap(ImageUtils.decodeFileWithExifRotation(photoPath, 512));
            imgEditPhotoThumb.setOnClickListener(v -> {
                com.aula.volta.ui.dialog.ImagePreviewDialog.show(
                        getParentFragmentManager(),
                        photoPath,
                        "Foto do Resíduo",
                        setor != null ? setor : "Classificação manual"
                );
            });
        }

        View btnWeightMinus = root.findViewById(R.id.btnWeightMinus);
        View btnWeightPlus = root.findViewById(R.id.btnWeightPlus);
        if (btnWeightMinus != null) {
            btnWeightMinus.setOnClickListener(v -> {
                if (currentWeight > 1) {
                    currentWeight--;
                    tvEditWeightValue.setText(String.valueOf(currentWeight));
                    updateEditSteppersUi(root);
                }
            });
        }
        if (btnWeightPlus != null) {
            btnWeightPlus.setOnClickListener(v -> {
                currentWeight++;
                tvEditWeightValue.setText(String.valueOf(currentWeight));
                updateEditSteppersUi(root);
            });
        }

        View btnUnitsMinus = root.findViewById(R.id.btnUnitsMinus);
        View btnUnitsPlus = root.findViewById(R.id.btnUnitsPlus);
        if (btnUnitsMinus != null) {
            btnUnitsMinus.setOnClickListener(v -> {
                if (currentUnits > 1) {
                    currentUnits--;
                    tvEditUnitsValue.setText(String.valueOf(currentUnits));
                    updateEditSteppersUi(root);
                }
            });
        }
        if (btnUnitsPlus != null) {
            btnUnitsPlus.setOnClickListener(v -> {
                currentUnits++;
                tvEditUnitsValue.setText(String.valueOf(currentUnits));
                updateEditSteppersUi(root);
            });
        }
        updateEditSteppersUi(root);

        setupMaterialChips(root);

        setupContaminationSegments(root);

        MaterialButton btnSave = root.findViewById(R.id.btnSaveCorrection);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                isRevised = true;
                switchToResultView();
                showCorrectionSavedToast();
                applyAnalysis();
            });
        }

        MaterialButton btnCancel = root.findViewById(R.id.btnCancelCorrection);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> cancelEditView());
        }
    }

    private void setupMaterialChips(View root) {
        TextView chipPapelao = root.findViewById(R.id.chipMatPapelao);
        TextView chipPlastico = root.findViewById(R.id.chipMatPlastico);
        TextView chipMetal = root.findViewById(R.id.chipMatMetal);
        TextView chipVidro = root.findViewById(R.id.chipMatVidro);
        TextView chipMadeira = root.findViewById(R.id.chipMatMadeira);
        TextView chipOrganico = root.findViewById(R.id.chipMatOrganico);
        TextView chipOutro = root.findViewById(R.id.chipMatOutro);

        TextView[] chips = {chipPapelao, chipPlastico, chipMetal, chipVidro, chipMadeira, chipOrganico, chipOutro};
        for (TextView chip : chips) {
            if (chip == null) continue;
            chip.setOnClickListener(v -> {
                String clean = chip.getText().toString().replace("✓ ", "").trim();
                selectedMaterial = clean;
                updateMaterialChipsUi(root);
            });
        }
        updateMaterialChipsUi(root);
    }

    private void updateMaterialChipsUi(View root) {
        TextView chipPapelao = root.findViewById(R.id.chipMatPapelao);
        TextView chipPlastico = root.findViewById(R.id.chipMatPlastico);
        TextView chipMetal = root.findViewById(R.id.chipMatMetal);
        TextView chipVidro = root.findViewById(R.id.chipMatVidro);
        TextView chipMadeira = root.findViewById(R.id.chipMatMadeira);
        TextView chipOrganico = root.findViewById(R.id.chipMatOrganico);
        TextView chipOutro = root.findViewById(R.id.chipMatOutro);

        TextView[] chips = {chipPapelao, chipPlastico, chipMetal, chipVidro, chipMadeira, chipOrganico, chipOutro};
        for (TextView c : chips) {
            if (c == null) continue;
            c.setBackgroundResource(R.drawable.bg_chip_white_stroke);
            c.setTextColor(requireContext().getColor(R.color.vf_text_secondary));
            String text = c.getText().toString().replace("✓ ", "");
            c.setText(text);
        }

        String lower = selectedMaterial.toLowerCase();
        if (lower.contains("papel") && chipPapelao != null) {
            chipPapelao.setBackgroundResource(R.drawable.bg_chip_gold_selected);
            chipPapelao.setTextColor(Color.parseColor("#C68A10"));
            chipPapelao.setText("✓ " + chipPapelao.getText().toString().replace("✓ ", ""));
        } else if ((lower.contains("plást") || lower.contains("plast")) && chipPlastico != null) {
            chipPlastico.setBackgroundResource(R.drawable.bg_chip_blue_selected);
            chipPlastico.setTextColor(Color.parseColor("#3B82F6"));
            chipPlastico.setText("✓ " + chipPlastico.getText().toString().replace("✓ ", ""));
        } else if (lower.contains("metal") && chipMetal != null) {
            chipMetal.setBackgroundResource(R.drawable.bg_chip_purple_selected);
            chipMetal.setTextColor(Color.parseColor("#8B6FE0"));
            chipMetal.setText("✓ " + chipMetal.getText().toString().replace("✓ ", ""));
        } else if (lower.contains("vidro") && chipVidro != null) {
            chipVidro.setBackgroundResource(R.drawable.bg_chip_gray_selected);
            chipVidro.setTextColor(Color.parseColor("#6B7280"));
            chipVidro.setText("✓ " + chipVidro.getText().toString().replace("✓ ", ""));
        } else if (lower.contains("madeira") && chipMadeira != null) {
            chipMadeira.setBackgroundResource(R.drawable.bg_chip_amber_selected);
            chipMadeira.setTextColor(Color.parseColor("#B45309"));
            chipMadeira.setText("✓ " + chipMadeira.getText().toString().replace("✓ ", ""));
        } else if (lower.contains("org") && chipOrganico != null) {
            chipOrganico.setBackgroundResource(R.drawable.bg_chip_green_selected);
            chipOrganico.setTextColor(Color.parseColor("#12A05E"));
            chipOrganico.setText("✓ " + chipOrganico.getText().toString().replace("✓ ", ""));
        } else if (chipOutro != null) {
            chipOutro.setBackgroundResource(R.drawable.bg_chip_gray_selected);
            chipOutro.setTextColor(Color.parseColor("#475569"));
            chipOutro.setText("✓ " + chipOutro.getText().toString().replace("✓ ", ""));
        }
    }

    private void setupContaminationSegments(View root) {
        TextView segBaixa = root.findViewById(R.id.segContamBaixa);
        TextView segMedia = root.findViewById(R.id.segContamMedia);
        TextView segAlta = root.findViewById(R.id.segContamAlta);

        if (segBaixa != null) {
            segBaixa.setOnClickListener(v -> {
                selectedContamination = "Baixa";
                updateContaminationSegmentsUi(root);
            });
        }
        if (segMedia != null) {
            segMedia.setOnClickListener(v -> {
                selectedContamination = "Média";
                updateContaminationSegmentsUi(root);
            });
        }
        if (segAlta != null) {
            segAlta.setOnClickListener(v -> {
                selectedContamination = "Alta";
                updateContaminationSegmentsUi(root);
            });
        }
        updateContaminationSegmentsUi(root);
    }

    private void updateContaminationSegmentsUi(View root) {
        TextView segBaixa = root.findViewById(R.id.segContamBaixa);
        TextView segMedia = root.findViewById(R.id.segContamMedia);
        TextView segAlta = root.findViewById(R.id.segContamAlta);

        if (segBaixa != null) {
            segBaixa.setBackground(null);
            segBaixa.setTextColor(requireContext().getColor(R.color.vf_text_secondary));
            segBaixa.setText("Baixa");
        }
        if (segMedia != null) {
            segMedia.setBackground(null);
            segMedia.setTextColor(requireContext().getColor(R.color.vf_text_secondary));
            segMedia.setText("Média");
        }
        if (segAlta != null) {
            segAlta.setBackground(null);
            segAlta.setTextColor(requireContext().getColor(R.color.vf_text_secondary));
            segAlta.setText("Alta");
        }

        if ("Baixa".equalsIgnoreCase(selectedContamination) && segBaixa != null) {
            segBaixa.setBackgroundResource(R.drawable.bg_segment_green_selected);
            segBaixa.setTextColor(Color.parseColor("#0E7C48"));
            segBaixa.setText("✓ Baixa");
        } else if (("Média".equalsIgnoreCase(selectedContamination) || "Media".equalsIgnoreCase(selectedContamination)) && segMedia != null) {
            segMedia.setBackgroundResource(R.drawable.bg_segment_gold_selected);
            segMedia.setTextColor(requireContext().getColor(R.color.vf_gold));
            segMedia.setText("✓ Média");
        } else if ("Alta".equalsIgnoreCase(selectedContamination) && segAlta != null) {
            segAlta.setBackgroundResource(R.drawable.bg_segment_pink_selected);
            segAlta.setTextColor(requireContext().getColor(R.color.vf_pink));
            segAlta.setText("✓ Alta");
        }
    }

    private void updateEditSteppersUi(View root) {
        if (root == null) return;
        View stepperWeight = root.findViewById(R.id.stepperWeightContainer);
        View badgeWeight = root.findViewById(R.id.badgeWeightEdited);
        View stepperUnits = root.findViewById(R.id.stepperUnitsContainer);
        View badgeUnits = root.findViewById(R.id.badgeUnitsEdited);

        boolean weightChanged = (currentWeight != initialEstimatedWeight);
        if (stepperWeight != null) {
            stepperWeight.setBackgroundResource(weightChanged ? R.drawable.bg_stepper_blue : R.drawable.bg_stepper_card_normal);
        }
        if (badgeWeight != null) {
            badgeWeight.setVisibility(weightChanged ? View.VISIBLE : View.GONE);
        }

        boolean unitsChanged = (currentUnits != initialEstimatedUnits);
        if (stepperUnits != null) {
            stepperUnits.setBackgroundResource(unitsChanged ? R.drawable.bg_stepper_purple : R.drawable.bg_stepper_card_normal);
        }
        if (badgeUnits != null) {
            badgeUnits.setVisibility(unitsChanged ? View.VISIBLE : View.GONE);
        }
    }

    private void showExitConfirmationDialog() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancelar ocorrência?")
                .setMessage("Se você voltar, a ocorrência será excluída. Deseja realmente sair?")
                .setNegativeButton("Continuar", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Sim, excluir", (dialog, which) -> {
                    dialog.dismiss();
                    if (createdIncidentId != null && !createdIncidentId.isEmpty()) {
                        try {
                            OccurrenceStore.remove(requireContext(), createdIncidentId);
                        } catch (Exception ignored) {
                        }
                    }
                    if (getView() != null) {
                        Navigation.findNavController(getView()).popBackStack(R.id.nav_home, false);
                    } else if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .show();
    }

    private void switchToEditView() {
        snapshotWeight = currentWeight;
        snapshotUnits = currentUnits;
        snapshotMaterial = selectedMaterial;
        snapshotClass = selectedClass;
        snapshotContamination = selectedContamination;

        aiResultGroup.setVisibility(View.GONE);
        aiLoadingGroup.setVisibility(View.GONE);
        aiEditGroup.setVisibility(View.VISIBLE);
        if (bottomBarActionsResult != null) {
            bottomBarActionsResult.setVisibility(View.GONE);
        }
        if (bottomBarActionsEdit != null) {
            bottomBarActionsEdit.setVisibility(View.VISIBLE);
        }
        tvAiHeaderTitle.setText("Editar análise");
        tvAiHeaderSubtitle.setText("Passo 2 de 2 · Revisão");
        if (tvEditWeightValue != null) {
            tvEditWeightValue.setText(String.valueOf(currentWeight));
        }
        if (tvEditUnitsValue != null) {
            tvEditUnitsValue.setText(String.valueOf(currentUnits));
        }
        if (getView() != null) {
            updateMaterialChipsUi(getView());
            updateContaminationSegmentsUi(getView());
            updateEditSteppersUi(getView());
        }
    }

    private void cancelEditView() {
        currentWeight = snapshotWeight;
        currentUnits = snapshotUnits;
        selectedMaterial = snapshotMaterial;
        selectedClass = snapshotClass;
        selectedContamination = snapshotContamination;
        if (tvEditWeightValue != null) {
            tvEditWeightValue.setText(String.valueOf(currentWeight));
        }
        if (tvEditUnitsValue != null) {
            tvEditUnitsValue.setText(String.valueOf(currentUnits));
        }
        if (getView() != null) {
            updateMaterialChipsUi(getView());
            updateContaminationSegmentsUi(getView());
        }
        switchToResultView();
    }

    private void switchToResultView() {
        aiEditGroup.setVisibility(View.GONE);
        aiLoadingGroup.setVisibility(View.GONE);
        aiResultGroup.setVisibility(View.VISIBLE);
        if (bottomBarActionsEdit != null) {
            bottomBarActionsEdit.setVisibility(View.GONE);
        }
        if (bottomBarActionsResult != null) {
            bottomBarActionsResult.setVisibility(View.VISIBLE);
        }
        tvAiHeaderTitle.setText(getString(R.string.ai_title));
        tvAiHeaderSubtitle.setText(getString(R.string.occurrence_step_2));
    }

    private void showCorrectionSavedToast() {
        if (toastCorrectionSaved != null) {
            toastCorrectionSaved.removeCallbacks(hideCorrectionToastRunnable);
            toastCorrectionSaved.animate().cancel();
            toastCorrectionSaved.setAlpha(0f);
            toastCorrectionSaved.setVisibility(View.VISIBLE);
            toastCorrectionSaved.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        if (isAdded() && toastCorrectionSaved != null) {
                            toastCorrectionSaved.postDelayed(hideCorrectionToastRunnable, 4000);
                        }
                    })
                    .start();
        }
    }

    private String trimNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((int) value);
        }
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f", value);
    }

    private void confirmOccurrence(View v) {
        if (!com.aula.volta.util.NetworkUtils.isOnline(requireContext())) {
            showSubmissionOfflineError(v, true);
            return;
        }
        if (btnConfirm != null) {
            btnConfirm.setEnabled(false);
            btnConfirm.setText("Registrando ocorrência...");
        }
        String primary = ReportMetricsCalculator.resolveMaterialFamily(
                (selectedMaterial != null && !selectedMaterial.isEmpty()) ? selectedMaterial :
                (analysis != null && analysis.getPrimaryMaterial() != null ? analysis.getPrimaryMaterial() : "Reciclável"));
        String specific = (analysis != null && analysis.getSpecificItem() != null)
                ? analysis.getSpecificItem()
                : AiAnalysisJSON.extractSpecificItem(primary);
        String material = AiAnalysisJSON.formatMaterialWithClass(primary, specific, selectedClass);
        double qtd = isRevised ? currentWeight : (analysis != null ? analysis.getQuantidadeEstimada() : 5.0);
        String unidade = (analysis != null && analysis.getUnidade() != null) ? analysis.getUnidade() : "kg";
        String calculatedPriority = AiAnalysisJSON.calculatePriority(material, selectedContamination, descricao);

        if (remoteImageUrl == null || remoteImageUrl.isEmpty()) {
            if (photoPath != null && !photoPath.isEmpty()) {
                String cached = CloudinaryUploader.getCachedUrl(photoPath);
                if (cached != null && !cached.isEmpty()) {
                    remoteImageUrl = cached;
                    submitOccurrenceDraftToBackend(v, material, qtd, unidade, cached, calculatedPriority);
                    return;
                }
                File imageFile = new File(photoPath);
                if (imageFile.exists()) {
                    if (btnConfirm != null) {
                        btnConfirm.setText("Enviando foto...");
                    }
                    CloudinaryUploader.uploadImage(requireContext(), imageFile, new CloudinaryUploader.UploadCallback() {
                        @Override
                        public void onSuccess(String remoteUrl, String publicId) {
                            if (!isAdded()) return;
                            remoteImageUrl = remoteUrl;
                            if (btnConfirm != null) {
                                btnConfirm.setText("Registrando ocorrência...");
                            }
                            submitOccurrenceDraftToBackend(v, material, qtd, unidade, remoteUrl, calculatedPriority);
                        }

                        @Override
                        public void onError(Exception exception, String fallbackLocalPath) {
                            if (!isAdded()) return;
                            Log.w("AiAnalysisFragment", "Upload da foto falhou: " + exception.getMessage() + ". Enviando ocorrência sem foto para aprovação no banco.");
                            if (btnConfirm != null) {
                                btnConfirm.setText("Registrando ocorrência...");
                            }
                            submitOccurrenceDraftToBackend(v, material, qtd, unidade, null, calculatedPriority);
                        }
                    });
                    return;
                }
            }
        }

        submitOccurrenceDraftToBackend(v, material, qtd, unidade, remoteImageUrl, calculatedPriority);
    }

    private void submitOccurrenceDraftToBackend(View v, String material, double qtd, String unidade,
                                                @Nullable String photoUrl, String calculatedPriority) {
        submitOccurrenceToBackend(v, material, qtd, unidade, photoUrl, calculatedPriority);
    }

    private void submitOccurrenceToBackend(View v, String material, double qtd, String unidade,
                                           @Nullable String photoUrl, String calculatedPriority) {
        if (!isAdded() || getContext() == null) {
            return;
        }

        final OccurrenceAPI occurrenceApi = ApiClient.get(requireContext()).create(OccurrenceAPI.class);
        final AiAnalysisAPI qaApi = ApiClient.get(requireContext()).create(AiAnalysisAPI.class);

        qaApi.getMyAreas().enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(@NonNull Call<JsonArray> call, @NonNull Response<JsonArray> areaResp) {
                if (!isAdded()) return;
                if (areaResp.isSuccessful() && areaResp.body() != null && areaResp.body().size() > 0) {
                    cacheAreas(areaResp.body());
                    String resolvedAreaId = resolveBestAreaId(areaResp.body());
                    resolveWasteTypeAndCreate(v, occurrenceApi, qaApi, resolvedAreaId, material, qtd, unidade, photoUrl, calculatedPriority);
                } else {
                    fetchAreasAndWasteTypesFromSummary(v, occurrenceApi, qaApi, material, qtd, unidade, photoUrl, calculatedPriority);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonArray> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                fetchAreasAndWasteTypesFromSummary(v, occurrenceApi, qaApi, material, qtd, unidade, photoUrl, calculatedPriority);
            }
        });
    }

    private void fetchAreasAndWasteTypesFromSummary(View v, OccurrenceAPI occurrenceApi, AiAnalysisAPI qaApi,
                                                    String material, double qtd, String unidade,
                                                    @Nullable String photoUrl, String calculatedPriority) {
        qaApi.getSummary().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> sumResp) {
                if (!isAdded()) return;
                JsonArray areasArr = null;
                JsonArray wasteArr = null;
                if (sumResp.isSuccessful() && sumResp.body() != null) {
                    JsonObject sum = sumResp.body();
                    if (sum.has("areas") && sum.get("areas").isJsonArray()) {
                        areasArr = sum.getAsJsonArray("areas");
                        cacheAreas(areasArr);
                    }
                    if (sum.has("wasteTypes") && sum.get("wasteTypes").isJsonArray()) {
                        wasteArr = sum.getAsJsonArray("wasteTypes");
                        PrefsHelper.putJson(requireContext(), "cache_waste_types", "list", wasteArr.toString());
                    }
                }
                String resolvedAreaId = resolveBestAreaId(areasArr);
                if (wasteArr != null && wasteArr.size() > 0) {
                    String searchRef = (selectedMaterial != null && !selectedMaterial.isEmpty()) ? selectedMaterial : material;
                    String resolvedWasteId = AiAnalysisJSON.resolveWasteTypeId(wasteArr, searchRef);
                    if (resolvedWasteId == null) {
                        resolvedWasteId = getCachedOrFallbackWasteTypeId(requireContext(), searchRef);
                    }
                    executeCreateCoreIncident(v, occurrenceApi, resolvedAreaId, resolvedWasteId, material, qtd, unidade, photoUrl, calculatedPriority);
                } else {
                    resolveWasteTypeAndCreate(v, occurrenceApi, qaApi, resolvedAreaId, material, qtd, unidade, photoUrl, calculatedPriority);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                String fallbackAreaId = resolveBestAreaId(null);
                resolveWasteTypeAndCreate(v, occurrenceApi, qaApi, fallbackAreaId, material, qtd, unidade, photoUrl, calculatedPriority);
            }
        });
    }

    private void resolveWasteTypeAndCreate(View v, OccurrenceAPI occurrenceApi, AiAnalysisAPI qaApi,
                                           @Nullable String areaId, String material, double qtd, String unidade,
                                           @Nullable String photoUrl, String calculatedPriority) {
        final String searchRef = (selectedMaterial != null && !selectedMaterial.isEmpty()) ? selectedMaterial : material;
        qaApi.getWasteTypes().enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(@NonNull Call<JsonArray> call2, @NonNull Response<JsonArray> wasteResp) {
                if (!isAdded()) return;
                String resolvedWasteTypeId = null;
                if (wasteResp.isSuccessful() && wasteResp.body() != null && wasteResp.body().size() > 0) {
                    PrefsHelper.putJson(requireContext(), "cache_waste_types", "list", wasteResp.body().toString());
                    resolvedWasteTypeId = AiAnalysisJSON.resolveWasteTypeId(wasteResp.body(), searchRef);
                }
                if (resolvedWasteTypeId == null) {
                    resolvedWasteTypeId = getCachedOrFallbackWasteTypeId(requireContext(), searchRef);
                }
                executeCreateCoreIncident(v, occurrenceApi, areaId, resolvedWasteTypeId, material, qtd, unidade, photoUrl, calculatedPriority);
            }

            @Override
            public void onFailure(@NonNull Call<JsonArray> call2, @NonNull Throwable t) {
                if (!isAdded()) return;
                String fallbackWasteTypeId = getCachedOrFallbackWasteTypeId(requireContext(), searchRef);
                executeCreateCoreIncident(v, occurrenceApi, areaId, fallbackWasteTypeId, material, qtd, unidade, photoUrl, calculatedPriority);
            }
        });
    }

    private void cacheAreas(@Nullable JsonArray areas) {
        if (areas == null || !isAdded()) return;
        for (JsonElement el : areas) {
            if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                if (o.has("id") && !o.get("id").isJsonNull()) {
                    PrefsHelper.putJson(requireContext(), "cache_areas", o.get("id").getAsString(), o.toString());
                }
            }
        }
    }

    @Nullable
    private String resolveBestAreaId(@Nullable JsonArray areas) {
        if (explicitAreaId != null && !explicitAreaId.trim().isEmpty()) {
            return explicitAreaId.trim();
        }
        String resolved = AiAnalysisJSON.resolveAreaId(areas, setor);
        if (resolved != null && !resolved.isEmpty()) {
            return resolved;
        }
        if (areas != null && areas.size() > 0) {
            JsonElement first = areas.get(0);
            if (first.isJsonObject() && first.getAsJsonObject().has("id") && !first.getAsJsonObject().get("id").isJsonNull()) {
                return first.getAsJsonObject().get("id").getAsString();
            }
        }
        return getCachedOrFallbackAreaId(requireContext());
    }

    private String getCachedOrFallbackAreaId(android.content.Context context) {
        try {
            android.content.SharedPreferences prefs = context.getSharedPreferences("cache_areas", android.content.Context.MODE_PRIVATE);
            java.util.Map<String, ?> all = prefs.getAll();
            if (all != null && !all.isEmpty()) {
                for (String key : all.keySet()) {
                    if (key != null && key.length() > 10) {
                        return key;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        try {
            String sumStr = PrefsHelper.getJson(context, "cache_reports", "summary");
            if (sumStr != null && !sumStr.isEmpty()) {
                JsonObject sum = com.google.gson.JsonParser.parseString(sumStr).getAsJsonObject();
                if (sum.has("areas") && sum.get("areas").isJsonArray()) {
                    String fromSum = AiAnalysisJSON.resolveAreaId(sum.getAsJsonArray("areas"), setor);
                    if (fromSum != null && !fromSum.isEmpty()) {
                        return fromSum;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        try {
            java.util.List<Occurrence> cachedOccs = OccurrenceStore.cached(context);
            if (cachedOccs != null) {
                for (Occurrence o : cachedOccs) {
                    if (o != null && o.getId() != null) {
                        String dStr = PrefsHelper.getJson(context, "cache_occurrence_detail", o.getId());
                        if (dStr != null && !dStr.isEmpty()) {
                            JsonObject d = com.google.gson.JsonParser.parseString(dStr).getAsJsonObject();
                            if (d.has("areaId") && !d.get("areaId").isJsonNull()) {
                                String aid = d.get("areaId").getAsString();
                                if (aid != null && aid.length() > 10) return aid;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String getCachedOrFallbackWasteTypeId(android.content.Context context, String searchRef) {
        try {
            String listStr = PrefsHelper.getJson(context, "cache_waste_types", "list");
            if (listStr != null && !listStr.isEmpty()) {
                JsonArray arr = com.google.gson.JsonParser.parseString(listStr).getAsJsonArray();
                String resolved = AiAnalysisJSON.resolveWasteTypeId(arr, searchRef);
                if (resolved != null && !resolved.isEmpty()) {
                    return resolved;
                }
            }
        } catch (Exception ignored) {
        }
        return mapMaterialToWasteTypeId(searchRef);
    }

    private void executeCreateCoreIncident(View v, OccurrenceAPI occurrenceApi, String areaId, String wasteTypeId,
                                           String material, double qtd, String unidade,
                                           @Nullable String photoUrl, String calculatedPriority) {
        if (areaId == null || areaId.trim().isEmpty()) {
            if (btnConfirm != null) {
                btnConfirm.setEnabled(true);
                btnConfirm.setText("Tentar de novo");
            }
            Toast.makeText(requireContext(), "Nenhuma área operacional encontrada para sua unidade no banco de dados.", Toast.LENGTH_LONG).show();
            return;
        }

        String singleWordTitle = AiAnalysisJSON.generateSingleWordTitle(
                material,
                descricao,
                (analysis != null ? analysis.getMaterial() : null)
        );

        String contaminationLevel = "LOW";
        if (selectedContamination != null) {
            String c = selectedContamination.toLowerCase(Locale.ROOT);
            if (c.contains("alta") || c.contains("high")) {
                contaminationLevel = "HIGH";
            } else if (c.contains("méd") || c.contains("med")) {
                contaminationLevel = "MEDIUM";
            }
        }

        String finalPhotoUrl = photoUrl;
        if ((finalPhotoUrl == null || finalPhotoUrl.isEmpty()) && photoPath != null) {
            finalPhotoUrl = CloudinaryUploader.getCachedUrl(photoPath);
        }
        if ((finalPhotoUrl == null || finalPhotoUrl.isEmpty()) && remoteImageUrl != null && !remoteImageUrl.isEmpty()) {
            finalPhotoUrl = remoteImageUrl;
        }

        if (finalPhotoUrl != null) {
            finalPhotoUrl = finalPhotoUrl.trim();
            if (!finalPhotoUrl.startsWith("http://") && !finalPhotoUrl.startsWith("https://")) {
                Log.w("AiAnalysisFragment", "photoUrl não é HTTP/HTTPS válido (" + finalPhotoUrl + "), sanitizando para null para passar pela validação @URL da API");
                finalPhotoUrl = null;
            }
        }
        if (finalPhotoUrl != null && finalPhotoUrl.isEmpty()) {
            finalPhotoUrl = null;
        }

        String apiPriority = IncidentRequestDTO.normalizePriority(calculatedPriority);
        String empDescription = (descricao != null && !descricao.trim().isEmpty())
                ? descricao.trim()
                : singleWordTitle;
        IncidentRequestDTO req = new IncidentRequestDTO(
                areaId,
                wasteTypeId,
                finalPhotoUrl,
                empDescription,
                contaminationLevel,
                qtd,
                apiPriority
        );

        final String sentPhotoUrl = finalPhotoUrl;
        Log.i("AiAnalysisFragment", "Enviando ocorrência diretamente ao banco PostgreSQL (POST /incidents) com photoUrl: " + sentPhotoUrl);

        occurrenceApi.createIncident(req).enqueue(new Callback<IncidentResponseDTO>() {
            @Override
            public void onResponse(@NonNull Call<IncidentResponseDTO> call, @NonNull Response<IncidentResponseDTO> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().getId() != null) {
                    IncidentResponseDTO inc = response.body();
                    String dbIncidentId = inc.getId();
                    String savedPhotoUrl = inc.getPhotoUrl() != null && !inc.getPhotoUrl().isEmpty()
                            ? inc.getPhotoUrl() : sentPhotoUrl;
                    createdIncidentId = dbIncidentId;
                    Log.i("AiAnalysisFragment", "Incidente salvo no banco oficial com sucesso! ID: " + dbIncidentId + ", Photo: " + savedPhotoUrl);

                    handleOccurrenceCreatedSuccess(v, dbIncidentId, material, qtd, unidade, savedPhotoUrl, calculatedPriority, true, null);
                } else {
                    String errDetail = "HTTP " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errBody = response.errorBody().string();
                            Log.w("AiAnalysisFragment", "POST /incidents falhou com " + errDetail + ": " + errBody);
                            if (errBody.toLowerCase(Locale.ROOT).contains("area")) {
                                errDetail += " (Área não encontrada)";
                            } else if (errBody.toLowerCase(Locale.ROOT).contains("waste")) {
                                errDetail += " (Tipo de resíduo inválido)";
                            } else if (errBody.toLowerCase(Locale.ROOT).contains("url")) {
                                errDetail += " (URL da foto inválida)";
                            } else if (response.code() == 403) {
                                errDetail += " (Perfil sem permissão para registrar ocorrência)";
                            }
                        }
                    } catch (Exception ignored) {
                    }
                    final String failReason = errDetail;
                    Log.w("AiAnalysisFragment", "POST /incidents falhou com " + failReason);
                    if (response.code() == 404) {
                        String altAreaId = getCachedOrFallbackAreaId(requireContext());
                        if (altAreaId != null && !altAreaId.equals(req.getAreaId())) {
                            req.setAreaId(altAreaId);
                            occurrenceApi.createIncident(req).enqueue(new Callback<IncidentResponseDTO>() {
                                @Override
                                public void onResponse(@NonNull Call<IncidentResponseDTO> call2, @NonNull Response<IncidentResponseDTO> resp2) {
                                    if (!isAdded()) return;
                                    if (resp2.isSuccessful() && resp2.body() != null && resp2.body().getId() != null) {
                                        IncidentResponseDTO inc2 = resp2.body();
                                        handleOccurrenceCreatedSuccess(v, inc2.getId(), material, qtd, unidade, inc2.getPhotoUrl() != null ? inc2.getPhotoUrl() : sentPhotoUrl, calculatedPriority, true, null);
                                        return;
                                    }
                                    showDatabaseRegistrationError(v, failReason);
                                }

                                @Override
                                public void onFailure(@NonNull Call<IncidentResponseDTO> call2, @NonNull Throwable t2) {
                                    if (!isAdded()) return;
                                    showSubmissionOfflineError(v, !com.aula.volta.util.NetworkUtils.isOnline(requireContext()));
                                }
                            });
                            return;
                        }
                    }
                    showDatabaseRegistrationError(v, failReason);
                }
            }

            @Override
            public void onFailure(@NonNull Call<IncidentResponseDTO> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Log.w("AiAnalysisFragment", "POST /incidents erro de rede: " + t.getMessage());
                boolean isOffline = !com.aula.volta.util.NetworkUtils.isOnline(requireContext());
                showSubmissionOfflineError(v, isOffline);
            }
        });
    }

    private void showDatabaseRegistrationError(View v, String failReason) {
        if (!isAdded()) return;
        if (btnConfirm != null) {
            btnConfirm.setEnabled(true);
            btnConfirm.setText("Tentar de novo");
        }
        Toast.makeText(requireContext(), "Não foi possível registrar no banco (" + failReason + ")", Toast.LENGTH_LONG).show();
    }

    private void showSubmissionOfflineError(View v, boolean isOffline) {
        if (!isAdded()) return;
        if (btnConfirm != null) {
            btnConfirm.setEnabled(true);
            btnConfirm.setText("Tentar de novo");
        }
        Bundle errArgs = new Bundle();
        if (isOffline) {
            errArgs.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_ERROR_TYPE,
                    com.aula.volta.ui.error.OfflineErrorFragment.TYPE_SUBMISSION_FAILED);
            errArgs.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_BADGE, "Você está offline");
            errArgs.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_TITLE, "Não conseguimos enviar");
            errArgs.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_MESSAGE,
                    "Sua foto e os dados continuam aqui. Confira a conexão e tente de novo.");
        } else {
            errArgs.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_ERROR_TYPE,
                    com.aula.volta.ui.error.OfflineErrorFragment.TYPE_SERVICE_UNAVAILABLE);
            errArgs.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_BADGE, "Fora do ar");
            errArgs.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_TITLE, "O VOLTA não respondeu");
            errArgs.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_MESSAGE,
                    "Não é nada com você: o serviço está fora do ar ou demorou demais pra responder. Tente de novo em alguns minutos.");
        }
        View navTarget = v != null ? v : getView();
        if (navTarget != null) {
            try {
                Navigation.findNavController(navTarget).navigate(R.id.nav_offline_error, errArgs);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Falha na conexão. Verifique sua internet.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private String mapMaterialToWasteTypeId(String material) {
        String lowerMat = material != null ? material.toLowerCase(Locale.ROOT) : "";
        if (lowerMat.contains("papel")) {
            return "bd985415-4b25-4c07-beca-cc086dce9b13";
        } else if (lowerMat.contains("plást") || lowerMat.contains("plast")) {
            return "8b3f6a14-9a27-462f-b414-24329d927af7";
        } else if (lowerMat.contains("metal") || lowerMat.contains("aço") || lowerMat.contains("ferro") || lowerMat.contains("alum") || lowerMat.contains("lata")) {
            return "5ee946da-2975-45e7-80d7-a5a8e49b4b2d";
        } else if (lowerMat.contains("vidro")) {
            return "2605dca7-b392-4835-a2a7-99a1aa68a7ab";
        } else if (lowerMat.contains("orgân") || lowerMat.contains("organ")) {
            return "5cb7e3c8-6dac-45e4-870f-92cc64a2a452";
        } else if (lowerMat.contains("eletrôn") || lowerMat.contains("eletron") || lowerMat.contains("bateria") || lowerMat.contains("pilha")) {
            return "7807fd3c-7c11-42c3-95be-48884968d3c1";
        } else if (lowerMat.contains("químic") || lowerMat.contains("quimic") || lowerMat.contains("perigo") || lowerMat.contains("óleo") || lowerMat.contains("oleo")) {
            return "db21c099-511a-4dbc-82dc-2090b4b6d0a5";
        } else if (lowerMat.contains("têxtil") || lowerMat.contains("textil") || lowerMat.contains("tecido") || lowerMat.contains("pano")) {
            return "9221e10b-b15b-40d8-a798-49f372ab9efb";
        } else if (lowerMat.contains("madeira") || lowerMat.contains("pallet") || lowerMat.contains("palete") || lowerMat.contains("tábua") || lowerMat.contains("tabua") || lowerMat.contains("mobil")) {
            return "e9e077a8-7464-422d-a3be-44bc770fd5d2";
        }
        return "dc9b7d17-cd48-475a-a313-d5de98505854";
    }

    private void handleOccurrenceCreatedSuccess(View v, String id, String material, double qtd,
                                                String unidade, @Nullable String photoUrl, String calculatedPriority) {
        handleOccurrenceCreatedSuccess(v, id, material, qtd, unidade, photoUrl, calculatedPriority, true, null);
    }

    private void handleOccurrenceCreatedSuccess(View v, String id, String material, double qtd,
                                                String unidade, @Nullable String photoUrl, String calculatedPriority,
                                                boolean isDatabaseSaved, @Nullable String note) {

        persistOccurrenceDetail(id, material, qtd, unidade, calculatedPriority, photoUrl);

        String title = AiAnalysisJSON.generateSingleWordTitle(
                material,
                descricao,
                (analysis != null ? analysis.getMaterial() : null)
        );
        String primaryFamily = ReportMetricsCalculator.resolveMaterialFamily((selectedMaterial != null && !selectedMaterial.isEmpty()) ? selectedMaterial : material);
        Occurrence occ = new Occurrence(
                id,
                title,
                setor == null ? "Planta Principal" : setor,
                "Hoje",
                calculatedPriority,
                "AGUARDANDO_VALIDACAO",
                primaryFamily,
                photoUrl,
                photoPath
        );
        OccurrenceStore.pushLocal(requireContext(), occ);

        AuditLog.append(requireContext(), com.aula.volta.data.local.SessionManager.name(requireContext()), "CRIAR",
                "ocorrencia", id, null, "BANCO_OFICIAL");
        pushOccurrenceNotification(id, material);

        Toast.makeText(requireContext(), R.string.ai_registered, Toast.LENGTH_SHORT).show();

        navigateToDetail(v, id);
    }

    private void persistOccurrenceDetail(String id, String material, double qtd, String unidade,
                                         String priority, @Nullable String photoUrl) {
        JsonObject detail = new JsonObject();
        detail.addProperty("id", id);
        String formattedMat = AiAnalysisJSON.formatMaterialWithClass(material, selectedClass);
        String specific = (analysis != null && analysis.getSpecificItem() != null)
                ? analysis.getSpecificItem()
                : AiAnalysisJSON.extractSpecificItem(material);
        String primary = (analysis != null && analysis.getPrimaryMaterial() != null)
                ? analysis.getPrimaryMaterial()
                : AiAnalysisJSON.extractPrimaryMaterial(material);
        detail.addProperty("titulo", (descricao != null && !descricao.isEmpty()) ? descricao : specific);
        detail.addProperty("material", formattedMat);
        detail.addProperty("material_principal", primary);
        detail.addProperty("item_especifico", specific);
        detail.addProperty("contaminacao", selectedContamination != null ? selectedContamination : "Baixa");
        detail.addProperty("setor", (setor != null && !setor.isEmpty()) ? setor : "Planta Principal");
        detail.addProperty("peso_estimado", qtd);
        detail.addProperty("unidade", unidade);
        detail.addProperty("status", "AGUARDANDO_VALIDACAO");
        detail.addProperty("prioridade", priority);
        detail.addProperty("quando", "agora");
        if (photoPath != null && !photoPath.isEmpty()) {
            detail.addProperty("photoPath", photoPath);
        }
        String resolvedPhoto = photoUrl != null && !photoUrl.isEmpty() ? photoUrl : remoteImageUrl;
        if (resolvedPhoto != null && !resolvedPhoto.isEmpty()) {
            detail.addProperty("photoUrl", resolvedPhoto);
            detail.addProperty("imageUrl", resolvedPhoto);
            detail.addProperty("foto_url", resolvedPhoto);
        }
        String relatorio = (analysis != null && analysis.getReportText() != null && !analysis.getReportText().isEmpty())
                ? analysis.getReportText()
                : ((analysis != null && analysis.getObservacoes() != null && !analysis.getObservacoes().isEmpty())
                ? analysis.getObservacoes()
                : "Material com umidade superficial, ainda reciclável. Remover em até 24h e guardar em área coberta.");
        detail.addProperty("relatorio_auto", relatorio);

        PrefsHelper.putJson(requireContext(), "cache_occurrence_detail", id, detail.toString());
    }

    private void navigateToDetail(View v, String occId) {
        NavController navController = null;
        try {
            navController = NavHostFragment.findNavController(this);
        } catch (Exception ignored) {
        }
        if (navController == null && getView() != null) {
            try {
                navController = Navigation.findNavController(getView());
            } catch (Exception ignored) {
            }
        }
        if (navController == null && v != null) {
            try {
                navController = Navigation.findNavController(v);
            } catch (Exception ignored) {
            }
        }
        if (navController == null && getActivity() != null) {
            try {
                navController = Navigation.findNavController(requireActivity(), R.id.fragmentContainerView);
            } catch (Exception ignored) {
            }
        }

        if (navController == null) {
            Log.w("AiAnalysisFragment", "NavController não encontrado, voltando para Home");
            navigateToHome(v);
            return;
        }

        Bundle args = new Bundle();
        args.putString("occurrenceId", occId);
        if (photoPath != null && !photoPath.isEmpty()) {
            args.putString("photoPath", photoPath);
        }
        if (remoteImageUrl != null && !remoteImageUrl.isEmpty()) {
            args.putString("photoUrl", remoteImageUrl);
            args.putString("imageUrl", remoteImageUrl);
        }

        try {
            navController.navigate(R.id.action_ai_analysis_to_occurrence_full, args);
            return;
        } catch (Exception e1) {
            Log.w("AiAnalysisFragment", "Falha ao navegar via action explícita: " + e1.getMessage());
        }

        try {
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_home, false)
                    .build();
            navController.navigate(R.id.nav_occurrence_full, args, navOptions);
            return;
        } catch (Exception e2) {
            Log.w("AiAnalysisFragment", "Falha ao navegar com navOptions: " + e2.getMessage());
        }

        try {
            navController.navigate(R.id.nav_occurrence_full, args);
            return;
        } catch (Exception e3) {
            Log.w("AiAnalysisFragment", "Falha ao navegar direto sem navOptions: " + e3.getMessage());
        }

        navigateToHome(v);
    }

    private void navigateToHome(View v) {
        try {
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_home, false)
                    .build();
            View target = getView() != null ? getView() : v;
            if (target != null) {
                Navigation.findNavController(target).navigate(R.id.nav_home, null, navOptions);
            }
        } catch (Exception e) {
            if (getView() != null) {
                Navigation.findNavController(getView()).popBackStack(R.id.nav_home, false);
            }
        }
    }

    private void pushOccurrenceNotification(String occurrenceId, String material) {
        com.aula.volta.data.notification.NotificationHelper.notifyOccurrenceSent(
                requireContext(), occurrenceId, material, setor != null ? setor : "");
    }
}
