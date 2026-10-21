package com.aula.volta.ui.analysis;

import android.animation.ObjectAnimator;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
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
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.aula.volta.R;
import com.aula.volta.data.api.AiAnalysisAPI;
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

/**
 * Análise da IA — Passo 2 de 2 (Figma 851:3792).
 * Contempla:
 *  - 15 · Análise da IA · escaneando (851:3975)
 *  - 16 · Análise da IA (851:4075)
 *  - 16a · Editar análise 1 (907:5210)
 *  - 16b · Análise da IA · revisada 1 (907:5084)
 */
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

    // Campos do modo de edição (16a)
    private TextView tvEditWeightValue;
    private TextView tvEditUnitsValue;
    private int currentWeight = 42;
    private int currentUnits = 11;
    private String selectedMaterial = "Papelão";
    private String selectedClass = "Classe A";
    private String selectedContamination = "Baixa";
    private boolean isRevised = false;

    // Snapshot para restaurar caso o usuário cancele a edição (16a -> Cancelar/Voltar)
    private int snapshotWeight = 42;
    private int snapshotUnits = 11;
    private String snapshotMaterial = "Papelão";
    private String snapshotClass = "Classe A";
    private String snapshotContamination = "Baixa";

    private AiAnalysisJSON analysis;
    private ObjectAnimator scanAnimator;

    private String photoPath;
    private String remoteImageUrl;
    private String createdIncidentId;
    private String descricao;
    private String setor;

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

        aiLoadingGroup = view.findViewById(R.id.aiLoadingGroup);
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
        icMiddleStatus = view.findViewById(R.id.icMiddleStatus);
        tvMiddleStatusHint = view.findViewById(R.id.tvMiddleStatusHint);

        ImageView imgAiPhoto = view.findViewById(R.id.imgAiPhoto);
        if (photoPath != null && imgAiPhoto != null) {
            imgAiPhoto.setImageBitmap(BitmapFactory.decodeFile(photoPath));
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
            imgAiPhotoScan.setImageBitmap(BitmapFactory.decodeFile(photoPath));
            imgAiPhotoScan.setOnClickListener(v -> {
                com.aula.volta.ui.dialog.ImagePreviewDialog.show(
                        getParentFragmentManager(),
                        photoPath,
                        "Imagem em Análise",
                        setor != null ? setor : "Identificação de IA"
                );
            });
        }

        View btnBack = view.findViewById(R.id.btnBackAnalysis);
        btnBack.setOnClickListener(v -> {
            if (aiEditGroup.getVisibility() == View.VISIBLE) {
                cancelEditView();
            } else {
                Navigation.findNavController(v).navigateUp();
            }
        });

        // Botão Editar (16 -> 16a)
        MaterialButton btnEdit = view.findViewById(R.id.btnEditAnalysis);
        btnEdit.setOnClickListener(v -> switchToEditView());

        // Botão Confirmar e gerar relatório (16 / 16b -> 17)
        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirmAnalysis);
        btnConfirm.setOnClickListener(this::confirmOccurrence);

        setupEditControls(view);

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
        super.onDestroyView();
    }

    private void startScanAnimation(View root) {
        View band = root.findViewById(R.id.viewScanBand);
        View frame = root.findViewById(R.id.scanPhotoFrame);
        if (band == null || frame == null) {
            return;
        }
        frame.post(() -> {
            if (!isAdded() || getView() == null) {
                return;
            }
            stopScanAnimation();
            scanAnimator = ObjectAnimator.ofFloat(
                    band, "translationY", -band.getHeight(), frame.getHeight());
            scanAnimator.setDuration(1600);
            scanAnimator.setRepeatCount(ObjectAnimator.INFINITE);
            scanAnimator.start();
        });
    }

    private void stopScanAnimation() {
        if (scanAnimator != null) {
            scanAnimator.cancel();
            scanAnimator = null;
        }
    }

    private void analyzePhoto() {
        if (remoteImageUrl != null && !remoteImageUrl.isEmpty()) {
            executeAiAnalysisRequest(remoteImageUrl);
            return;
        }
        if (photoPath != null && !photoPath.isEmpty()) {
            String cachedUrl = CloudinaryUploader.getCachedUrl(photoPath);
            if (cachedUrl != null && !cachedUrl.isEmpty()) {
                remoteImageUrl = cachedUrl;
                executeAiAnalysisRequest(cachedUrl);
                return;
            }
            File imageFile = new File(photoPath);
            if (imageFile.exists()) {
                CloudinaryUploader.uploadImage(requireContext(), imageFile, new CloudinaryUploader.UploadCallback() {
                    @Override
                    public void onSuccess(String remoteUrl, String publicId) {
                        remoteImageUrl = remoteUrl;
                        Log.i("AiAnalysisFragment", "Cloudinary secure_url gerada: " + remoteUrl);
                        executeAiAnalysisRequest(remoteUrl);
                    }

                    @Override
                    public void onError(Exception exception, String fallbackLocalPath) {
                        Log.w("AiAnalysisFragment", "Upload Cloudinary falhou, seguindo com caminho local: " + exception.getMessage());
                        executeAiAnalysisRequest(fallbackLocalPath);
                    }
                });
                return;
            }
        }
        executeAiAnalysisRequest(null);
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

        // Se temos link HTTPS gerado pelo Cloudinary e internet ativa, executa o fluxo OpenAPI v3 QA
        if (imageUrl != null && imageUrl.startsWith("http") && SyncManager.isOnline(requireContext())) {
            final AiAnalysisAPI qaApi = ChatbotAuthClient.get(requireContext()).create(AiAnalysisAPI.class);
            tryQaIncidentAiReportFlow(qaApi, imageUrl, body);
        } else {
            executeLocalContractAnalysis(body);
        }
    }

    /**
     * Fluxo OpenAPI v3 QA: resolve areaId (/areas/mine) e wasteTypeId (/waste-types),
     * registra incidente com photoUrl do Cloudinary (POST /incidents) e consulta
     * o relatório da IA (GET /incidents/{id}/ai-report e GET /incidents/{id}/scoring-context).
     */
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
                        analysis = fromCtx;
                        applyAnalysis();
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
                    analysis = response.body();
                    if (createdIncidentId != null && !createdIncidentId.isEmpty()) {
                        analysis.setIncidentId(createdIncidentId);
                    }
                    applyAnalysis();
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
        analysis = AiAnalysisJSON.createOfflineFallback(setor);
        if (createdIncidentId != null && !createdIncidentId.isEmpty()) {
            analysis.setIncidentId(createdIncidentId);
        }
        applyAnalysis();
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

        if (isRevised) {
            String classLetter = selectedClass != null ? selectedClass.replace("Classe ", "").trim() : "A";
            tvAiMaterial.setText(selectedMaterial + (classLetter.isEmpty() ? "" : " " + classLetter));
            tvAiContamination.setText(selectedContamination);
            tvAiQuantity.setText("≈ " + currentWeight + " kg");
            badgeQuantityEdited.setVisibility(View.VISIBLE);
            cardAiQuantity.setBackgroundResource(R.drawable.bg_card_quantity_edited);
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
            String rawMat = analysis.getMaterial() != null ? analysis.getMaterial().trim() : "Papelão A";
            if (rawMat.matches("(?i).*(\\bClasse\\s+B\\b|\\s+B$).*")) {
                selectedClass = "Classe B";
            } else if (rawMat.matches("(?i).*(\\bClasse\\s+C\\b|\\s+C$).*")) {
                selectedClass = "Classe C";
            } else {
                selectedClass = "Classe A";
            }
            String cleanMat = rawMat
                    .replaceAll("(?i)\\s*\\(Classe\\s+[ABC]\\)", "")
                    .replaceAll("\\s+[ABC]$", "")
                    .trim();
            selectedMaterial = cleanMat.isEmpty() ? "Papelão" : cleanMat;

            selectedContamination = (analysis.getContaminacao() != null
                    && analysis.getContaminacao().getNivel() != null)
                    ? analysis.getContaminacao().getNivel() : "Baixa";

            if (analysis.getQuantidadeEstimada() > 0) {
                currentWeight = (int) Math.round(analysis.getQuantidadeEstimada());
            }
            if (analysis.getUnidades() != null && analysis.getUnidades().getQuantidade() > 0) {
                currentUnits = analysis.getUnidades().getQuantidade();
            }
            if (tvEditWeightValue != null) {
                tvEditWeightValue.setText(String.valueOf(currentWeight));
            }
            if (tvEditUnitsValue != null) {
                tvEditUnitsValue.setText(String.valueOf(currentUnits));
            }

            tvAiMaterial.setText(rawMat);
            tvAiContamination.setText(selectedContamination);
            String quantidade = "≈ " + trimNumber(analysis.getQuantidadeEstimada()) + " " + analysis.getUnidade();
            tvAiQuantity.setText(quantidade);
            badgeQuantityEdited.setVisibility(View.GONE);
            cardAiQuantity.setBackgroundResource(R.drawable.bg_card_stat_recycled);
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

    /** Configura o painel interativo de edição (16a) */
    private void setupEditControls(View root) {
        tvEditWeightValue = root.findViewById(R.id.tvEditWeightValue);
        tvEditUnitsValue = root.findViewById(R.id.tvEditUnitsValue);

        ImageView imgEditPhotoThumb = root.findViewById(R.id.imgEditPhotoThumb);
        if (photoPath != null && imgEditPhotoThumb != null) {
            imgEditPhotoThumb.setImageBitmap(BitmapFactory.decodeFile(photoPath));
            imgEditPhotoThumb.setOnClickListener(v -> {
                com.aula.volta.ui.dialog.ImagePreviewDialog.show(
                        getParentFragmentManager(),
                        photoPath,
                        "Foto do Resíduo",
                        setor != null ? setor : "Classificação manual"
                );
            });
        }

        // Stepper Peso
        View btnWeightMinus = root.findViewById(R.id.btnWeightMinus);
        View btnWeightPlus = root.findViewById(R.id.btnWeightPlus);
        if (btnWeightMinus != null) {
            btnWeightMinus.setOnClickListener(v -> {
                if (currentWeight > 1) {
                    currentWeight--;
                    tvEditWeightValue.setText(String.valueOf(currentWeight));
                }
            });
        }
        if (btnWeightPlus != null) {
            btnWeightPlus.setOnClickListener(v -> {
                currentWeight++;
                tvEditWeightValue.setText(String.valueOf(currentWeight));
            });
        }

        // Stepper Unidades
        View btnUnitsMinus = root.findViewById(R.id.btnUnitsMinus);
        View btnUnitsPlus = root.findViewById(R.id.btnUnitsPlus);
        if (btnUnitsMinus != null) {
            btnUnitsMinus.setOnClickListener(v -> {
                if (currentUnits > 1) {
                    currentUnits--;
                    tvEditUnitsValue.setText(String.valueOf(currentUnits));
                }
            });
        }
        if (btnUnitsPlus != null) {
            btnUnitsPlus.setOnClickListener(v -> {
                currentUnits++;
                tvEditUnitsValue.setText(String.valueOf(currentUnits));
            });
        }

        // Chips de Material
        setupMaterialChips(root);

        // Pills de Classe
        setupClassPills(root);

        // Segmentos de Contaminação
        setupContaminationSegments(root);

        // Botão Salvar Correção (16a -> 16b)
        MaterialButton btnSave = root.findViewById(R.id.btnSaveCorrection);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                isRevised = true;
                switchToResultView();
                showCorrectionSavedToast();
                applyAnalysis();
            });
        }

        // Botão Cancelar (16a -> volta para 16 restaurando snapshot)
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
        TextView chipOrganico = root.findViewById(R.id.chipMatOrganico);
        TextView chipOutro = root.findViewById(R.id.chipMatOutro);

        TextView[] chips = {chipPapelao, chipPlastico, chipMetal, chipVidro, chipOrganico, chipOutro};
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
        TextView chipOrganico = root.findViewById(R.id.chipMatOrganico);
        TextView chipOutro = root.findViewById(R.id.chipMatOutro);

        TextView[] chips = {chipPapelao, chipPlastico, chipMetal, chipVidro, chipOrganico, chipOutro};
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

    private void setupClassPills(View root) {
        TextView pillA = root.findViewById(R.id.pillClassA);
        TextView pillB = root.findViewById(R.id.pillClassB);
        TextView pillC = root.findViewById(R.id.pillClassC);

        TextView[] pills = {pillA, pillB, pillC};
        for (TextView pill : pills) {
            if (pill == null) continue;
            pill.setOnClickListener(v -> {
                selectedClass = "Classe " + pill.getText().toString();
                updateClassPillsUi(root);
            });
        }
        updateClassPillsUi(root);
    }

    private void updateClassPillsUi(View root) {
        TextView pillA = root.findViewById(R.id.pillClassA);
        TextView pillB = root.findViewById(R.id.pillClassB);
        TextView pillC = root.findViewById(R.id.pillClassC);

        TextView[] pills = {pillA, pillB, pillC};
        for (TextView p : pills) {
            if (p == null) continue;
            p.setBackgroundResource(R.drawable.bg_class_circle_unselected);
            p.setTextColor(requireContext().getColor(R.color.vf_text_secondary));
        }

        if (selectedClass.endsWith("A") && pillA != null) {
            pillA.setBackgroundResource(R.drawable.bg_class_circle_selected);
            pillA.setTextColor(Color.WHITE);
        } else if (selectedClass.endsWith("B") && pillB != null) {
            pillB.setBackgroundResource(R.drawable.bg_class_circle_selected);
            pillB.setTextColor(Color.WHITE);
        } else if (selectedClass.endsWith("C") && pillC != null) {
            pillC.setBackgroundResource(R.drawable.bg_class_circle_selected);
            pillC.setTextColor(Color.WHITE);
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
            updateClassPillsUi(getView());
            updateContaminationSegmentsUi(getView());
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
            updateClassPillsUi(getView());
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
            toastCorrectionSaved.setAlpha(0f);
            toastCorrectionSaved.setVisibility(View.VISIBLE);
            toastCorrectionSaved.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .withEndAction(() -> toastCorrectionSaved.postDelayed(() -> {
                        if (isAdded() && toastCorrectionSaved != null) {
                            toastCorrectionSaved.animate().alpha(0f).setDuration(400)
                                    .withEndAction(() -> toastCorrectionSaved.setVisibility(View.GONE))
                                    .start();
                        }
                    }, 3500))
                    .start();
        }
    }

    private String trimNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((int) value);
        }
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f", value);
    }

    /**
     * Confirmar e gerar relatório: cria a ocorrência e NAVEGA DIRETAMENTE
     * para a tela de Ocorrência (Figma 17 · 851:4196 / R.id.nav_occurrence_full).
     */
    private void confirmOccurrence(View v) {
        String material = isRevised ? selectedMaterial : (analysis != null && analysis.getMaterial() != null ? analysis.getMaterial() : "Papelão");
        double qtd = isRevised ? currentWeight : (analysis != null ? analysis.getQuantidadeEstimada() : 38.0);
        String unidade = "kg";

        if (!SyncManager.isOnline(requireContext())) {
            saveOfflineAndNavigateToDetail(v, material, qtd, unidade);
            return;
        }

        OccurrenceAPI api = ApiClient.get(requireContext()).create(OccurrenceAPI.class);
        JsonObject body = new JsonObject();
        body.addProperty("setor", setor == null ? "" : setor);
        body.addProperty("material", material);
        body.addProperty("quantidade_estimada", qtd);
        if (remoteImageUrl != null && !remoteImageUrl.isEmpty()) {
            body.addProperty("photoUrl", remoteImageUrl);
            body.addProperty("imageUrl", remoteImageUrl);
            body.addProperty("foto_url", remoteImageUrl);
        }

        api.createOccurrence(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (!isAdded()) {
                    return;
                }
                String id = (analysis != null && analysis.getIncidentId() != null && !analysis.getIncidentId().isEmpty())
                        ? analysis.getIncidentId()
                        : (response.body() != null && response.body().has("id")
                        ? response.body().get("id").getAsString() : "8514");
                registerAndNavigate(v, id, material, qtd, unidade);
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                saveOfflineAndNavigateToDetail(v, material, qtd, unidade);
            }
        });
    }

    private void persistOccurrenceDetail(String id, String material, double qtd, String unidade) {
        JsonObject detail = new JsonObject();
        detail.addProperty("id", id);
        String baseMat = material != null
                ? material.replaceAll("(?i)\\s*\\(Classe\\s+[ABC]\\)", "").replaceAll("\\s+[ABC]$", "").trim()
                : "Papelão";
        if (baseMat.isEmpty()) {
            baseMat = "Papelão";
        }
        String formattedMat = baseMat + " (" + selectedClass + ")";
        detail.addProperty("titulo", (descricao != null && !descricao.isEmpty()) ? descricao : formattedMat);
        detail.addProperty("material", formattedMat);
        detail.addProperty("contaminacao", selectedContamination != null ? selectedContamination : "Baixa");
        detail.addProperty("setor", (setor != null && !setor.isEmpty()) ? setor : "Frigorífico — Setor B2");
        detail.addProperty("peso_estimado", qtd);
        detail.addProperty("unidade", unidade);
        detail.addProperty("status", "EM_ANALISE");
        detail.addProperty("prioridade", "ALTA");
        detail.addProperty("quando", "agora");
        if (photoPath != null && !photoPath.isEmpty()) {
            detail.addProperty("photoPath", photoPath);
        }
        if (remoteImageUrl != null && !remoteImageUrl.isEmpty()) {
            detail.addProperty("photoUrl", remoteImageUrl);
            detail.addProperty("imageUrl", remoteImageUrl);
            detail.addProperty("foto_url", remoteImageUrl);
        }
        String relatorio = (analysis != null && analysis.getReportText() != null && !analysis.getReportText().isEmpty())
                ? analysis.getReportText()
                : ((analysis != null && analysis.getObservacoes() != null && !analysis.getObservacoes().isEmpty())
                ? analysis.getObservacoes()
                : "Material com umidade superficial, ainda reciclável. Remover em até 24h e guardar em área coberta.");
        detail.addProperty("relatorio_auto", relatorio);

        PrefsHelper.putJson(requireContext(), "cache_occurrence_detail", id, detail.toString());
        PrefsHelper.putJson(requireContext(), "cache_occurrence_detail", "latest_photo", detail.toString());
    }

    private void registerAndNavigate(View v, String id, String material, double qtd, String unidade) {
        if (!isAdded() || getContext() == null) {
            return;
        }
        persistOccurrenceDetail(id, material, qtd, unidade);
        AuditLog.append(requireContext(), "Breno Gomes", "CRIAR",
                "ocorrencia", id, null, "NOVO");
        pushOccurrenceNotification(id, material);
        pushOccurrenceToList(id, material);

        Toast.makeText(requireContext(), R.string.ai_registered, Toast.LENGTH_SHORT).show();

        // Navega para a tela 17 · Ocorrência (OccurrenceDetailFragment) removendo a tela de loading da pilha
        try {
            Bundle args = new Bundle();
            args.putString("occurrenceId", id);
            args.putString("photoPath", photoPath);
            if (remoteImageUrl != null && !remoteImageUrl.isEmpty()) {
                args.putString("imageUrl", remoteImageUrl);
            }
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_ai_analysis, true)
                    .build();
            View target = getView() != null ? getView() : v;
            if (target != null) {
                Navigation.findNavController(target).navigate(R.id.nav_occurrence_full, args, navOptions);
            }
        } catch (Exception e) {
            if (getView() != null) {
                Navigation.findNavController(getView()).popBackStack(R.id.nav_home, false);
            }
        }
    }

    private void saveOfflineAndNavigateToDetail(View v, String material, double qtd, String unidade) {
        if (!isAdded() || getContext() == null) {
            return;
        }
        String localId = String.valueOf(10000 + (System.currentTimeMillis() % 90000));
        persistOccurrenceDetail(localId, material, qtd, unidade);
        PendingOccurrence pending = new PendingOccurrence(
                localId, photoPath, descricao, setor, material, qtd, unidade, System.currentTimeMillis());
        SyncQueue.enqueue(requireContext(), pending);

        AuditLog.append(requireContext(), "Breno Gomes", "CRIAR_OFFLINE",
                "ocorrencia", localId, null, "PENDENTE_SYNC");
        pushOccurrenceNotification(localId, material);
        pushOccurrenceToList(localId, material);

        Toast.makeText(requireContext(), R.string.sync_saved_offline, Toast.LENGTH_SHORT).show();

        try {
            Bundle args = new Bundle();
            args.putString("occurrenceId", localId);
            args.putString("photoPath", photoPath);
            if (remoteImageUrl != null && !remoteImageUrl.isEmpty()) {
                args.putString("imageUrl", remoteImageUrl);
            }
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_ai_analysis, true)
                    .build();
            View target = getView() != null ? getView() : v;
            if (target != null) {
                Navigation.findNavController(target).navigate(R.id.nav_occurrence_full, args, navOptions);
            }
        } catch (Exception e) {
            if (getView() != null) {
                Navigation.findNavController(getView()).popBackStack(R.id.nav_home, false);
            }
        }
    }

    private void pushOccurrenceNotification(String occurrenceId, String material) {
        com.aula.volta.data.notification.NotificationHelper.notifyOccurrenceCreated(
                requireContext(), occurrenceId, material, setor != null ? setor : "");
    }

    private void pushOccurrenceToList(String occurrenceId, String material) {
        String title = (descricao != null && !descricao.isEmpty()) ? descricao : material;
        OccurrenceStore.pushLocal(requireContext(), new Occurrence(
                occurrenceId,
                title,
                setor == null ? "Frigorífico — Setor B2" : setor,
                getString(R.string.notif_now),
                "ALTA",
                "NOVO",
                material));
    }
}
