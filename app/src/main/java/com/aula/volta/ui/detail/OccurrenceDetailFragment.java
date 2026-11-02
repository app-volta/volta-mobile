package com.aula.volta.ui.detail;

import android.app.AlertDialog;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aula.volta.R;
import com.aula.volta.util.ImageUtils;
import android.os.Handler;
import android.os.Looper;
import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.ChatbotAPI;
import com.aula.volta.data.api.ChatbotAuthClient;
import com.aula.volta.data.api.ChatbotClient;
import com.aula.volta.data.api.OccurrenceAPI;
import com.google.gson.JsonArray;
import com.aula.volta.data.model.incident.IncidentResponseDTO;
import com.aula.volta.data.model.incident.AiReportResponseDTO;
import com.aula.volta.data.model.AiAnalysisJSON;
import com.aula.volta.data.notification.NotificationHelper;
import com.aula.volta.data.local.AuditLog;
import com.aula.volta.data.local.NotificationStore;
import com.aula.volta.data.local.OccurrenceStore;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.model.Notification;
import com.aula.volta.data.model.Occurrence;
import com.aula.volta.data.notification.NotificationHelper;
import com.aula.volta.data.report.PgrsReport;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OccurrenceDetailFragment extends Fragment {

    private static final String PREFS = "cache_occurrence_detail";

    private TextView tvOccTitle;
    private TextView tvOccSubtitle;
    private TextView tvOccStatus;
    private ImageView imgOccThumbnail;
    private TextView tvOccCardTitle;
    private TextView tvOccCardLine;
    private TextView tvOccCardSubtitle;
    private TextView tvOccPriority;
    private TextView badgeForwarded;
    private TextView tvOccAutoReport;
    private View rowEditClass;
    private TextView tvEditClassSub;
    private View rowObserve;
    private TextView tvObserveSub;
    private View layoutResponsibleActions;
    private TextView tvResponsibleActionsTitle;
    private View rowRejectOcc;
    private View layoutRejectedCard;
    private TextView tvRejectedSectionTitle;
    private TextView tvRejectedReasonTitle;
    private TextView tvRejectedBySub;
    private TextView tvRejectedHintText;
    private MaterialButton btnRegisterAgain;
    private MaterialButton btnFinalizeOcc;
    private boolean isSubmitted = false;
    private int changesCount = 0;
    private boolean hasPendingChanges = false;
    private String pendingMaterial = null;
    private String pendingPriority = null;
    private String pendingObservation = null;
    private boolean isSending = false;
    private View layoutBottomBarOcc;
    private String currentLocalPhoto;
    private String currentRemoteUrl;
    private View toastFinalized;
    private TextView tvToastFinalizedTitle;
    private TextView tvToastFinalizedSub;
    private TextView btnToastOpenPgrs;

    private FrameLayout stepCircle0;
    private ImageView stepCheck0;
    private TextView stepLabel0;
    private View stepLine0;
    private FrameLayout stepCircle1;
    private ImageView stepCheck1;
    private TextView stepLabel1;
    private View stepLine1;
    private FrameLayout stepCircle2;
    private ImageView stepCheck2;
    private TextView stepLabel2;

    private String occurrenceId;
    private String currentStatus = "EM_ANALISE";
    private JsonObject currentDetailJson;
    private String currentMaterial = "Resíduo Reciclável";
    private String currentMaterialName = "Resíduo";
    private String currentQualityClass = "A";
    private String currentPriority = "BAIXA";
    private String currentContamination = "baixa";
    private String currentSector = "Planta Principal";
    private String currentWeight = "0 kg";
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollStatusRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded() && !"APROVADA".equalsIgnoreCase(currentStatus)) {
                checkDraftApprovalStatus();
                pollHandler.postDelayed(this, 5000);
            }
        }
    };
    private final Runnable hideBannerToastRunnable = () -> {
        if (isAdded() && toastFinalized != null) {
            toastFinalized.animate()
                    .alpha(0f)
                    .setDuration(400)
                    .withEndAction(() -> {
                        if (toastFinalized != null) {
                            toastFinalized.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }
    };

    public OccurrenceDetailFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_occurrence_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvOccTitle = view.findViewById(R.id.tvOccTitle);
        tvOccSubtitle = view.findViewById(R.id.tvOccSubtitle);
        tvOccStatus = view.findViewById(R.id.tvOccStatus);
        imgOccThumbnail = view.findViewById(R.id.imgOccThumbnail);
        if (imgOccThumbnail != null) {
            imgOccThumbnail.setOnClickListener(v -> {
                boolean hasPhoto = (currentLocalPhoto != null && !currentLocalPhoto.isEmpty())
                        || (currentRemoteUrl != null && !currentRemoteUrl.isEmpty());
                if (!hasPhoto) {
                    return;
                }
                com.aula.volta.ui.dialog.ImagePreviewDialog.show(
                        getParentFragmentManager(),
                        currentLocalPhoto,
                        currentRemoteUrl,
                        currentMaterial != null ? currentMaterial : "Foto da Ocorrência",
                        currentSector != null ? currentSector : ("Ocorrência #" + occurrenceId)
                );
            });
        }
        tvOccCardTitle = view.findViewById(R.id.tvOccCardTitle);
        tvOccCardLine = view.findViewById(R.id.tvOccCardLine);
        tvOccCardSubtitle = view.findViewById(R.id.tvOccCardSubtitle);
        tvOccPriority = view.findViewById(R.id.tvOccPriority);
        badgeForwarded = view.findViewById(R.id.badgeForwarded);
        tvOccAutoReport = view.findViewById(R.id.tvOccAutoReport);
        rowEditClass = view.findViewById(R.id.rowEditClass);
        tvEditClassSub = view.findViewById(R.id.tvEditClassSub);
        rowObserve = view.findViewById(R.id.rowObserve);
        tvObserveSub = view.findViewById(R.id.tvObserveSub);
        layoutResponsibleActions = view.findViewById(R.id.layoutResponsibleActions);
        tvResponsibleActionsTitle = view.findViewById(R.id.tvResponsibleActionsTitle);
        rowRejectOcc = view.findViewById(R.id.rowRejectOcc);
        layoutRejectedCard = view.findViewById(R.id.layoutRejectedCard);
        tvRejectedSectionTitle = view.findViewById(R.id.tvRejectedSectionTitle);
        tvRejectedReasonTitle = view.findViewById(R.id.tvRejectedReasonTitle);
        tvRejectedBySub = view.findViewById(R.id.tvRejectedBySub);
        tvRejectedHintText = view.findViewById(R.id.tvRejectedHintText);
        btnRegisterAgain = view.findViewById(R.id.btnRegisterAgain);
        btnFinalizeOcc = view.findViewById(R.id.btnFinalizeOcc);
        if (rowRejectOcc != null) {
            rowRejectOcc.setOnClickListener(v -> showRejectSheet());
        }
        layoutBottomBarOcc = view.findViewById(R.id.layoutBottomBarOcc);
        toastFinalized = view.findViewById(R.id.toastFinalized);
        tvToastFinalizedTitle = view.findViewById(R.id.tvToastFinalizedTitle);
        tvToastFinalizedSub = view.findViewById(R.id.tvToastFinalizedSub);
        btnToastOpenPgrs = view.findViewById(R.id.btnToastOpenPgrs);

        stepCircle0 = view.findViewById(R.id.stepCircle0);
        stepCheck0 = view.findViewById(R.id.stepCheck0);
        stepLabel0 = view.findViewById(R.id.stepLabel0);
        stepLine0 = view.findViewById(R.id.stepLine0);
        stepCircle1 = view.findViewById(R.id.stepCircle1);
        stepCheck1 = view.findViewById(R.id.stepCheck1);
        stepLabel1 = view.findViewById(R.id.stepLabel1);
        stepLine1 = view.findViewById(R.id.stepLine1);
        stepCircle2 = view.findViewById(R.id.stepCircle2);
        stepCheck2 = view.findViewById(R.id.stepCheck2);
        stepLabel2 = view.findViewById(R.id.stepLabel2);

        occurrenceId = getArguments() != null ? getArguments().getString("occurrenceId") : null;
        if (occurrenceId == null || occurrenceId.isEmpty()) {
            Navigation.findNavController(view).navigateUp(); return;
        }

        String argPhotoPath = getArguments() != null ? getArguments().getString("photoPath") : null;
        String argImageUrl = getArguments() != null
                ? getArguments().getString("photoUrl", getArguments().getString("imageUrl"))
                : null;
        displayOccurrencePhoto(argPhotoPath, argImageUrl);

        View btnBack = view.findViewById(R.id.btnBackOcc);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        }

        if (rowEditClass != null) {
            rowEditClass.setOnClickListener(this::showEditClassificationSheet);
        }
        if (rowObserve != null) {
            rowObserve.setOnClickListener(this::showAddObservationSheet);
        }

        if (btnFinalizeOcc != null) {
            btnFinalizeOcc.setOnClickListener(this::handleMainActionButtonClick);
        }

        View.OnClickListener openPgrsAction = v -> {
            try {
                File report = PgrsReport.generatePdf(requireContext());
                if (report != null) {
                    PgrsReport.openPdf(requireContext(), report);
                } else {
                    Toast.makeText(requireContext(), R.string.reports_nothing, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(requireContext(), R.string.reports_nothing, Toast.LENGTH_SHORT).show();
            }
        };
        if (btnToastOpenPgrs != null) {
            btnToastOpenPgrs.setOnClickListener(openPgrsAction);
        }
        if (toastFinalized != null) {
            com.aula.volta.util.ToastSwipeHelper.attach(toastFinalized, hideBannerToastRunnable,
                    new com.aula.volta.util.ToastSwipeHelper.OnSwipeListener() {
                        @Override
                        public void onDismissed() {
                            if (toastFinalized != null) {
                                toastFinalized.setVisibility(View.GONE);
                            }
                        }

                        @Override
                        public void onSwipeDown() {
                            openPgrsAction.onClick(toastFinalized);
                        }
                    });
            toastFinalized.setOnClickListener(v -> {
                if (btnToastOpenPgrs != null && btnToastOpenPgrs.getVisibility() == View.VISIBLE) {
                    openPgrsAction.onClick(v);
                } else {
                    toastFinalized.removeCallbacks(hideBannerToastRunnable);
                    toastFinalized.animate().cancel();
                    toastFinalized.setVisibility(View.GONE);
                }
            });
        }

        loadDetail();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDetail();
        checkDraftApprovalStatus();
        pollHandler.removeCallbacks(pollStatusRunnable);
        pollHandler.postDelayed(pollStatusRunnable, 5000);
    }

    @Override
    public void onPause() {
        pollHandler.removeCallbacks(pollStatusRunnable);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        pollHandler.removeCallbacks(pollStatusRunnable);
        if (toastFinalized != null) {
            toastFinalized.removeCallbacks(hideBannerToastRunnable);
            toastFinalized.animate().cancel();
        }
        super.onDestroyView();
    }

    private void loadDetail() {
        if (occurrenceId != null && occurrenceId.startsWith("draft_")) {
            loadFromCache();
            return;
        }

        String cachedStr = PrefsHelper.getJson(requireContext(), PREFS, occurrenceId);
        if (cachedStr != null && !cachedStr.isEmpty()) {
            try {
                currentDetailJson = JsonParser.parseString(cachedStr).getAsJsonObject();
                applyDetail(currentDetailJson);
            } catch (Exception ignored) {
            }
        }

        OccurrenceAPI api = ChatbotAuthClient.get(requireContext()).create(OccurrenceAPI.class);
        api.getIncidentById(occurrenceId).enqueue(new Callback<IncidentResponseDTO>() {
            @Override
            public void onResponse(@NonNull Call<IncidentResponseDTO> call, @NonNull Response<IncidentResponseDTO> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    IncidentResponseDTO dto = response.body();
                    JsonObject body = new JsonObject();
                    body.addProperty("id", dto.getId());
                    String cleanTitle = (dto.getTitle() != null && !dto.getTitle().trim().isEmpty())
                            ? dto.getTitle().trim()
                            : com.aula.volta.data.model.AiAnalysisJSON.generateSingleWordTitle(
                                    (currentDetailJson != null && currentDetailJson.has("material")) ? currentDetailJson.get("material").getAsString() : null,
                                    null,
                                    dto.getEmployeeDescription()
                            );
                    if (cleanTitle.isEmpty() || cleanTitle.equalsIgnoreCase("Resíduo")) {
                        cleanTitle = "Resíduo Reciclável";
                    }
                    body.addProperty("titulo", cleanTitle);
                    String resolvedMat = (currentDetailJson != null && currentDetailJson.has("material")
                            && !currentDetailJson.get("material").getAsString().isEmpty()
                            && !currentDetailJson.get("material").getAsString().equalsIgnoreCase("Resíduo Reciclável"))
                            ? currentDetailJson.get("material").getAsString()
                            : cleanTitle;
                    body.addProperty("material", resolvedMat);
                    String sector = (currentDetailJson != null && currentDetailJson.has("setor"))
                            ? currentDetailJson.get("setor").getAsString()
                            : com.aula.volta.data.model.Occurrence.resolveSector(dto, getContext());
                    body.addProperty("setor", sector);
                    double peso = (dto.getEstimatedQuantity() != null && dto.getEstimatedQuantity() > 0)
                            ? dto.getEstimatedQuantity()
                            : (currentDetailJson != null && currentDetailJson.has("peso_estimado") ? currentDetailJson.get("peso_estimado").getAsDouble() : 0.0);
                    body.addProperty("peso_estimado", peso);

                    if (dto.getContaminationLevel() != null && !dto.getContaminationLevel().isEmpty()) {
                        body.addProperty("contaminacao", com.aula.volta.data.model.AiAnalysisJSON.mapContaminationLabel(dto.getContaminationLevel()));
                    }

                    String stat = (dto.getStatus() != null && !dto.getStatus().isEmpty()) ? dto.getStatus() : currentStatus;
                    body.addProperty("status", stat);

                    String prio = (dto.getPriority() != null && !dto.getPriority().isEmpty())
                            ? dto.getPriority()
                            : (currentDetailJson != null && currentDetailJson.has("prioridade") ? currentDetailJson.get("prioridade").getAsString() : AiAnalysisJSON.calculatePriority(cleanTitle, "LOW", dto.getEmployeeDescription()));
                    body.addProperty("prioridade", prio);

                    body.addProperty("quando", dto.getRegisteredAt() != null ? dto.getRegisteredAt() : (currentDetailJson != null && currentDetailJson.has("quando") ? currentDetailJson.get("quando").getAsString() : "agora"));

                    String photoUrl = dto.getPhotoUrl();
                    if ((photoUrl == null || photoUrl.isEmpty()) && currentDetailJson != null) {
                        if (currentDetailJson.has("photoUrl")) photoUrl = currentDetailJson.get("photoUrl").getAsString();
                        else if (currentDetailJson.has("imageUrl")) photoUrl = currentDetailJson.get("imageUrl").getAsString();
                    }
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        body.addProperty("imageUrl", photoUrl);
                        body.addProperty("photoUrl", photoUrl);
                    }

                    if (currentDetailJson != null && currentDetailJson.has("photoPath")) {
                        body.addProperty("photoPath", currentDetailJson.get("photoPath").getAsString());
                    }

                    String relatorio = "";
                    if (dto.getAiDescription() != null && !dto.getAiDescription().trim().isEmpty()) {
                        relatorio = dto.getAiDescription().trim();
                    } else if (currentDetailJson != null && currentDetailJson.has("relatorio_auto") && !currentDetailJson.get("relatorio_auto").getAsString().isEmpty()) {
                        relatorio = currentDetailJson.get("relatorio_auto").getAsString();
                    } else if (dto.getEmployeeDescription() != null) {
                        relatorio = dto.getEmployeeDescription();
                    }
                    if (!relatorio.isEmpty()) {
                        body.addProperty("relatorio_auto", relatorio);
                    }

                    currentDetailJson = body;
                    PrefsHelper.putJson(requireContext(), PREFS, occurrenceId, currentDetailJson.toString());
                    applyDetail(currentDetailJson);

                    fetchAiReportIfMissing(api, body);
                } else if (currentDetailJson == null) {
                    loadFromCache();
                }
            }

            @Override
            public void onFailure(@NonNull Call<IncidentResponseDTO> call, @NonNull Throwable t) {
                if (isAdded() && currentDetailJson == null) {
                    loadFromCache();
                }
            }
        });

        checkDraftApprovalStatus();
    }

    private void fetchAiReportIfMissing(OccurrenceAPI api, JsonObject body) {
        if (occurrenceId == null || occurrenceId.isEmpty()) return;
        api.getIncidentAiReport(occurrenceId).enqueue(new Callback<AiReportResponseDTO>() {
            @Override
            public void onResponse(@NonNull Call<AiReportResponseDTO> call, @NonNull Response<AiReportResponseDTO> repResp) {
                if (!isAdded() || repResp.body() == null) return;
                AiReportResponseDTO rep = repResp.body();
                String repText = rep.getReportText() != null && !rep.getReportText().isEmpty()
                        ? rep.getReportText() : rep.getRecommendations();
                if (repText != null && !repText.isEmpty()) {
                    body.addProperty("relatorio_auto", repText);
                    currentDetailJson = body;
                    PrefsHelper.putJson(requireContext(), PREFS, occurrenceId, currentDetailJson.toString());
                    if (tvOccAutoReport != null) {
                        tvOccAutoReport.setText(repText);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<AiReportResponseDTO> call, @NonNull Throwable t) {
            }
        });
    }

    private void checkDraftApprovalStatus() {
        if (occurrenceId == null || occurrenceId.isEmpty() || occurrenceId.startsWith("draft_") || !isAdded()) return;
        try {
            OccurrenceAPI occApi = ChatbotAuthClient.get(requireContext()).create(OccurrenceAPI.class);
            occApi.getIncidentById(occurrenceId).enqueue(new Callback<IncidentResponseDTO>() {
                @Override
                public void onResponse(@NonNull Call<IncidentResponseDTO> call, @NonNull Response<IncidentResponseDTO> response) {
                    if (!isAdded() || !response.isSuccessful() || response.body() == null) return;
                    String st = response.body().getStatus() != null ? response.body().getStatus() : "";
                    if ("APPROVED".equalsIgnoreCase(st) || "APROVADO".equalsIgnoreCase(st)
                            || "APROVADA".equalsIgnoreCase(st) || "CLOSED".equalsIgnoreCase(st)) {
                        if (!"APROVADA".equalsIgnoreCase(currentStatus)) {
                            currentStatus = "APROVADA";
                            if (currentDetailJson != null) {
                                currentDetailJson.addProperty("status", "APROVADA");
                                PrefsHelper.putJson(requireContext(), PREFS, occurrenceId, currentDetailJson.toString());
                            }
                            OccurrenceStore.updateStatus(requireContext(), occurrenceId, "APROVADA");
                            syncFinalizedUi();
                            NotificationHelper.notifyOccurrenceApproved(requireContext(), occurrenceId, currentMaterial);
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<IncidentResponseDTO> call, @NonNull Throwable t) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void loadFromCache() {
        String cached = PrefsHelper.getJson(requireContext(), PREFS, occurrenceId);
        if (cached != null) {
            try {
                currentDetailJson = JsonParser.parseString(cached).getAsJsonObject();
                applyDetail(currentDetailJson);
            } catch (Exception ignored) {
            }
        }
    }

    private void applyDetail(JsonObject json) {
        if (json == null) {
            return;
        }

        isSubmitted = json.has("is_submitted")
                ? json.get("is_submitted").getAsBoolean()
                : (occurrenceId != null && !occurrenceId.startsWith("draft_"));
        changesCount = json.has("changes_count")
                ? json.get("changes_count").getAsInt()
                : 0;

        String rawMaterial = opt(json, "material", "Resíduo Reciclável");
        String rawClasse = opt(json, "classe", "");
        currentQualityClass = com.aula.volta.data.model.AiAnalysisJSON.extractQualityClassLetter(rawMaterial, rawClasse);
        currentMaterialName = com.aula.volta.data.model.AiAnalysisJSON.extractMaterialName(rawMaterial);
        currentMaterial = com.aula.volta.data.model.AiAnalysisJSON.formatMaterialWithClass(rawMaterial, rawClasse);

        if (json.has("peso_estimado") && !json.get("peso_estimado").isJsonNull()) {
            try {
                double peso = json.get("peso_estimado").getAsDouble();
                String pesoStr = (peso == Math.rint(peso))
                        ? String.valueOf((int) peso)
                        : String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f", peso);
                String unidade = opt(json, "unidade", "kg");
                currentWeight = pesoStr + " " + unidade;
            } catch (Exception e) {
                String rawPeso = opt(json, "peso_estimado", "0");
                currentWeight = rawPeso.endsWith("kg") ? rawPeso : (rawPeso + " kg");
            }
        }
        String contam = opt(json, "contaminacao",
                opt(json, "contaminacao_nivel", "baixa")).trim();
        if (contam.isEmpty()) {
            contam = "baixa";
        }
        currentContamination = contam.toLowerCase(java.util.Locale.forLanguageTag("pt-BR"));
        currentPriority = opt(json, "prioridade", "BAIXA");
        applyPriorityBadge(currentPriority);

        currentSector = opt(json, "setor", "Planta Principal");
        String quando = opt(json, "quando", opt(json, "data", "hoje, 09:12"));
        currentStatus = opt(json, "status", "EM_ANALISE");

        if (tvOccTitle != null) {
            String customTitle = opt(json, "titulo", "");
            if (!customTitle.isEmpty() && !customTitle.startsWith("Ocorrência")) {
                tvOccTitle.setText(customTitle);
            } else {
                tvOccTitle.setText(getString(R.string.occ_title));
            }
        }
        if (tvOccSubtitle != null) {
            String brQuando = com.aula.volta.data.model.Occurrence.formatBrazilianDateTime(quando);
            tvOccSubtitle.setText("Registrada por " + com.aula.volta.data.local.SessionManager.name(requireContext()) + " · " + brQuando);
        }

        if (tvOccCardTitle != null) {
            tvOccCardTitle.setText(currentMaterial);
        }
        if (tvOccCardLine != null) {
            tvOccCardLine.setText("Contaminação " + currentContamination + " · ≈ " + currentWeight);
        }
        if (tvOccCardSubtitle != null) {
            tvOccCardSubtitle.setText(currentSector);
        }

        String autoReport = opt(json, "relatorio_auto",
                opt(json, "relatorio_automatico",
                        "Material com umidade superficial, ainda reciclável. Remover em até 24h e guardar em área coberta."));
        if (tvOccAutoReport != null) {
            tvOccAutoReport.setText(autoReport);
        }

        String localPhoto = opt(json, "photoPath",
                getArguments() != null ? getArguments().getString("photoPath", "") : "");
        String remoteUrl = opt(json, "photoUrl",
                opt(json, "imageUrl",
                        opt(json, "foto_url",
                                getArguments() != null
                                        ? getArguments().getString("photoUrl", getArguments().getString("imageUrl", ""))
                                        : "")));

        displayOccurrencePhoto(localPhoto, remoteUrl);

        syncFinalizedUi();
        updateActionButtonsState();
    }

    private void applyPriorityBadge(String priority) {
        if (tvOccPriority == null || !isAdded()) {
            return;
        }
        String norm = priority != null
                ? priority.trim().toUpperCase(java.util.Locale.forLanguageTag("pt-BR"))
                : "ALTA";
        if (norm.contains("BAIX") || norm.contains("LOW")) {
            tvOccPriority.setText(getString(R.string.priority_baixa));
            tvOccPriority.setBackgroundResource(R.drawable.bg_badge_baixa);
            tvOccPriority.setTextColor(requireContext().getColor(R.color.priority_low_text));
        } else if (norm.contains("MÉD") || norm.contains("MED")) {
            tvOccPriority.setText(getString(R.string.priority_media));
            tvOccPriority.setBackgroundResource(R.drawable.bg_badge_media);
            tvOccPriority.setTextColor(requireContext().getColor(R.color.priority_medium_text));
        } else {
            tvOccPriority.setText(getString(R.string.priority_alta));
            tvOccPriority.setBackgroundResource(R.drawable.bg_badge_alta);
            tvOccPriority.setTextColor(requireContext().getColor(R.color.priority_high_text));
        }
    }

    private void showTopBannerToast(String title, String subtitle, boolean showPgrsButton, boolean autoHide) {
        if (toastFinalized == null || !isAdded()) {
            return;
        }
        if (tvToastFinalizedTitle != null) {
            tvToastFinalizedTitle.setText(title);
        }
        if (tvToastFinalizedSub != null) {
            tvToastFinalizedSub.setText(subtitle);
        }
        if (btnToastOpenPgrs != null) {
            btnToastOpenPgrs.setVisibility(showPgrsButton ? View.VISIBLE : View.GONE);
        }
        toastFinalized.removeCallbacks(hideBannerToastRunnable);
        toastFinalized.animate().cancel();
        toastFinalized.setAlpha(0f);
        toastFinalized.setVisibility(View.VISIBLE);
        toastFinalized.setTranslationX(0f);
        toastFinalized.setTranslationY(0f);
        toastFinalized.animate()
                .alpha(1f)
                .setDuration(300)
                .withEndAction(() -> {
                    if (isAdded() && toastFinalized != null) {
                        toastFinalized.postDelayed(hideBannerToastRunnable, 4000);
                    }
                })
                .start();
    }

    private void displayOccurrencePhoto(@Nullable String localPath, @Nullable String remoteUrl) {
        boolean hasLocal = localPath != null && !localPath.isEmpty() && new File(localPath).exists();
        boolean hasRemote = remoteUrl != null && !remoteUrl.isEmpty() && remoteUrl.startsWith("http");

        this.currentLocalPhoto = hasLocal ? localPath : null;
        this.currentRemoteUrl = hasRemote ? remoteUrl : null;

        if (imgOccThumbnail == null) {
            return;
        }

        if (hasLocal) {
            android.graphics.Bitmap bmp = ImageUtils.decodeFileWithExifRotation(localPath, 1280);
            if (bmp != null) {
                imgOccThumbnail.setImageBitmap(bmp);
                return;
            }
        }

        if (hasRemote) {
            com.aula.volta.data.upload.CloudinaryUploader.fetchRemoteBitmap(remoteUrl, new com.aula.volta.data.upload.CloudinaryUploader.BitmapCallback() {
                @Override
                public void onBitmapLoaded(android.graphics.Bitmap bmp) {
                    if (bmp != null && isAdded() && imgOccThumbnail != null) {
                        imgOccThumbnail.setImageBitmap(bmp);
                    }
                }

                @Override
                public void onError(Exception e) {
                    android.util.Log.w("OccurrenceDetail", "Falha ao carregar thumbnail remota: " + e.getMessage());
                    if (isAdded() && imgOccThumbnail != null) {
                        imgOccThumbnail.setImageResource(R.drawable.img_caixas_papelao);
                    }
                }
            });
            return;
        }

        imgOccThumbnail.setImageResource(R.drawable.img_caixas_papelao);
    }

    private void showRejectSheet() {
        RejectOccurrenceSheet sheet = RejectOccurrenceSheet.newInstance();
        sheet.setOnRejectConfirmedListener(this::performReject);
        sheet.show(getChildFragmentManager(), "reject_occurrence_sheet");
    }

    private void performReject(String reason) {
        currentStatus = "RECUSADA";
        String myName = com.aula.volta.data.local.SessionManager.name(requireContext());
        java.text.SimpleDateFormat timeFmt = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.forLanguageTag("pt-BR"));
        String nowStr = "hoje, " + timeFmt.format(new java.util.Date());

        if (currentDetailJson == null) {
            currentDetailJson = new JsonObject();
        }
        currentDetailJson.addProperty("status", "RECUSADA");
        currentDetailJson.addProperty("reject_reason", reason);
        currentDetailJson.addProperty("rejected_by", myName);
        currentDetailJson.addProperty("rejected_at", nowStr);
        PrefsHelper.putJson(requireContext(), PREFS, occurrenceId, currentDetailJson.toString());

        OccurrenceStore.updateStatus(requireContext(), occurrenceId, "RECUSADA");

        AuditLog.append(requireContext(), myName, "RECUSAR", "ocorrencia", occurrenceId, "EM_ANALISE", reason);

        try {
            OccurrenceAPI occApi = ChatbotAuthClient.get(requireContext()).create(OccurrenceAPI.class);
            JsonObject body = new JsonObject();
            body.addProperty("reason", reason);
            body.addProperty("motivo", reason);
            body.addProperty("rejectedBy", myName);
            body.addProperty("rejectionReason", reason);
            occApi.rejectIncident(occurrenceId, body).enqueue(new Callback<IncidentResponseDTO>() {
                @Override
                public void onResponse(@NonNull Call<IncidentResponseDTO> call, @NonNull Response<IncidentResponseDTO> response) {
                    if (!response.isSuccessful()) {
                        occApi.rejectIncidentSingular(occurrenceId, body).enqueue(new Callback<IncidentResponseDTO>() {
                            @Override
                            public void onResponse(@NonNull Call<IncidentResponseDTO> call2, @NonNull Response<IncidentResponseDTO> response2) {}
                            @Override
                            public void onFailure(@NonNull Call<IncidentResponseDTO> call2, @NonNull Throwable t) {}
                        });
                    }
                }

                @Override
                public void onFailure(@NonNull Call<IncidentResponseDTO> call, @NonNull Throwable t) {
                    try {
                        occApi.rejectIncidentSingular(occurrenceId, body).enqueue(new Callback<IncidentResponseDTO>() {
                            @Override
                            public void onResponse(@NonNull Call<IncidentResponseDTO> call2, @NonNull Response<IncidentResponseDTO> response2) {}
                            @Override
                            public void onFailure(@NonNull Call<IncidentResponseDTO> call2, @NonNull Throwable t) {}
                        });
                    } catch (Exception ignored) {}
                }
            });
        } catch (Exception ignored) {}

        syncFinalizedUi();
        showTopBannerToast("Ocorrência recusada", "Quem registrou recebe o motivo", false, true);
    }

    private void syncFinalizedUi() {
        if (!isAdded()) return;

        boolean isDone = "APPROVED".equalsIgnoreCase(currentStatus)
                || "APROVADO".equalsIgnoreCase(currentStatus)
                || "APROVADA".equalsIgnoreCase(currentStatus)
                || "CLOSED".equalsIgnoreCase(currentStatus)
                || "RESOLVIDO".equalsIgnoreCase(currentStatus)
                || "RESOLVIDA".equalsIgnoreCase(currentStatus)
                || "FINALIZADA".equalsIgnoreCase(currentStatus)
                || "FINALIZADO".equalsIgnoreCase(currentStatus)
                || "CONCLUIDA".equalsIgnoreCase(currentStatus)
                || "CONCLUIDO".equalsIgnoreCase(currentStatus);

        boolean isRejected = "RECUSADA".equalsIgnoreCase(currentStatus)
                || "REJECTED".equalsIgnoreCase(currentStatus)
                || "RECUSADO".equalsIgnoreCase(currentStatus);

        if (stepCircle0 != null) {
            stepCircle0.setBackgroundResource(R.drawable.bg_step_done);
        }
        if (stepCheck0 != null) {
            stepCheck0.setVisibility(View.VISIBLE);
        }
        if (stepLabel0 != null) {
            stepLabel0.setTextColor(requireContext().getColor(R.color.vf_green_deep));
        }
        if (stepLine0 != null) {
            stepLine0.setBackgroundColor(requireContext().getColor(R.color.vf_green));
        }

        if (stepCircle1 != null) {
            stepCircle1.setBackgroundResource(R.drawable.bg_step_done);
        }
        if (stepCheck1 != null) {
            stepCheck1.setVisibility(View.VISIBLE);
        }
        if (stepLabel1 != null) {
            stepLabel1.setTextColor(requireContext().getColor(R.color.vf_green_deep));
        }

        if (isRejected) {
            if (stepLine1 != null) {
                stepLine1.setBackgroundColor(android.graphics.Color.parseColor("#E8556C"));
            }
            if (stepCircle2 != null) {
                stepCircle2.setBackgroundResource(R.drawable.bg_step_rejected);
            }
            if (stepCheck2 != null) {
                stepCheck2.setImageResource(R.drawable.ic_close_white);
                stepCheck2.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
                stepCheck2.setVisibility(View.VISIBLE);
            }
            if (stepLabel2 != null) {
                stepLabel2.setText("Recusada");
                stepLabel2.setTextColor(android.graphics.Color.parseColor("#E8556C"));
            }

            if (tvOccStatus != null) {
                tvOccStatus.setText("RECUSADA");
                tvOccStatus.setBackgroundResource(R.drawable.bg_badge_recusada);
                tvOccStatus.setTextColor(android.graphics.Color.parseColor("#E8556C"));
            }

            if (layoutResponsibleActions != null) {
                layoutResponsibleActions.setVisibility(View.GONE);
            }
            if (tvResponsibleActionsTitle != null) {
                tvResponsibleActionsTitle.setVisibility(View.GONE);
            }
            if (rowEditClass != null) {
                rowEditClass.setVisibility(View.GONE);
            }
            if (rowObserve != null) {
                rowObserve.setVisibility(View.GONE);
            }
            if (btnFinalizeOcc != null) {
                btnFinalizeOcc.setVisibility(View.GONE);
            }

            String myName = com.aula.volta.data.local.SessionManager.name(requireContext());
            String rejectedBy = opt(currentDetailJson, "rejected_by", "");
            String registeredBy = opt(currentDetailJson, "registered_by", opt(currentDetailJson, "user_name", ""));
            String rejectReason = opt(currentDetailJson, "reject_reason", "Foto não mostra o resíduo");
            String rejectAt = opt(currentDetailJson, "rejected_at", "hoje, 09:40");

            boolean isMeWhoRejected = !rejectedBy.isEmpty() && (rejectedBy.equalsIgnoreCase(myName) || "você".equalsIgnoreCase(rejectedBy));

            if (layoutRejectedCard != null) {
                layoutRejectedCard.setVisibility(View.VISIBLE);
            }
            if (tvRejectedReasonTitle != null) {
                tvRejectedReasonTitle.setText(rejectReason);
            }

            if (isMeWhoRejected) {
                if (tvRejectedSectionTitle != null) {
                    tvRejectedSectionTitle.setText("Motivo da recusa");
                }
                if (tvRejectedBySub != null) {
                    tvRejectedBySub.setText("Recusada por você · " + rejectAt);
                }
                if (tvRejectedHintText != null) {
                    tvRejectedHintText.setText("Quem registrou recebe o motivo e pode registrar de novo.");
                }
                if (layoutBottomBarOcc != null) {
                    layoutBottomBarOcc.setVisibility(View.GONE);
                }
                if (btnRegisterAgain != null) {
                    btnRegisterAgain.setVisibility(View.GONE);
                }
            } else {
                if (tvRejectedSectionTitle != null) {
                    tvRejectedSectionTitle.setText("Por que foi recusada");
                }
                if (tvRejectedBySub != null) {
                    String displayAuthor = rejectedBy.isEmpty() ? "Ana Souza" : rejectedBy;
                    tvRejectedBySub.setText("Recusada por " + displayAuthor + " · " + rejectAt);
                }
                if (tvRejectedHintText != null) {
                    tvRejectedHintText.setText("Tire outra foto, com boa luz e o resíduo visível, e registre de novo.");
                }
                if (layoutBottomBarOcc != null) {
                    layoutBottomBarOcc.setVisibility(View.VISIBLE);
                }
                if (btnRegisterAgain != null) {
                    btnRegisterAgain.setVisibility(View.VISIBLE);
                    btnRegisterAgain.setOnClickListener(v -> androidx.navigation.Navigation.findNavController(v).navigate(R.id.nav_register));
                }
            }
        } else if (isDone) {
            if (layoutRejectedCard != null) {
                layoutRejectedCard.setVisibility(View.GONE);
            }
            if (btnRegisterAgain != null) {
                btnRegisterAgain.setVisibility(View.GONE);
            }
            if (layoutResponsibleActions != null) {
                layoutResponsibleActions.setVisibility(View.GONE);
            }
            if (stepLine1 != null) {
                stepLine1.setBackgroundColor(requireContext().getColor(R.color.vf_green));
            }
            if (stepCircle2 != null) {
                stepCircle2.setBackgroundResource(R.drawable.bg_step_done);
            }
            if (stepCheck2 != null) {
                stepCheck2.setImageResource(R.drawable.ic_notif_check);
                stepCheck2.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
                stepCheck2.setVisibility(View.VISIBLE);
            }
            if (stepLabel2 != null) {
                stepLabel2.setText(R.string.occ_step_approval);
                stepLabel2.setTextColor(requireContext().getColor(R.color.vf_green_deep));
            }

            if (tvOccStatus != null) {
                tvOccStatus.setText("APROVADA");
                tvOccStatus.setBackgroundResource(R.drawable.bg_badge_aprovada);
                tvOccStatus.setTextColor(requireContext().getColor(R.color.status_aprovada_text));
            }

            if (rowEditClass != null) {
                rowEditClass.setVisibility(View.GONE);
            }
            if (rowObserve != null) {
                rowObserve.setVisibility(View.GONE);
            }
            if (layoutBottomBarOcc != null) {
                layoutBottomBarOcc.setVisibility(View.GONE);
            } else if (btnFinalizeOcc != null) {
                btnFinalizeOcc.setVisibility(View.GONE);
            }
        } else {
            if (layoutRejectedCard != null) {
                layoutRejectedCard.setVisibility(View.GONE);
            }
            if (btnRegisterAgain != null) {
                btnRegisterAgain.setVisibility(View.GONE);
            }
            if (layoutResponsibleActions != null) {
                layoutResponsibleActions.setVisibility(View.VISIBLE);
            }
            if (tvResponsibleActionsTitle != null) {
                tvResponsibleActionsTitle.setVisibility(View.VISIBLE);
            }
            if (stepLine1 != null) {
                stepLine1.setBackgroundColor(requireContext().getColor(R.color.vf_border));
            }
            if (stepCircle2 != null) {
                stepCircle2.setBackgroundResource(R.drawable.bg_notification_circle);
            }
            if (stepCheck2 != null) {
                stepCheck2.setImageResource(R.drawable.ic_notif_check);
                stepCheck2.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
                stepCheck2.setVisibility(View.GONE);
            }
            if (stepLabel2 != null) {
                stepLabel2.setText(R.string.occ_step_approval);
                stepLabel2.setTextColor(requireContext().getColor(R.color.vf_text_secondary));
            }

            if (tvOccStatus != null) {
                tvOccStatus.setText("EM ANÁLISE");
                tvOccStatus.setBackgroundResource(R.drawable.bg_badge_media);
                tvOccStatus.setTextColor(requireContext().getColor(R.color.vf_gold));
            }

            if (rowEditClass != null) {
                rowEditClass.setVisibility(View.VISIBLE);
            }
            if (rowObserve != null) {
                rowObserve.setVisibility(View.VISIBLE);
            }
            if (layoutBottomBarOcc != null) {
                layoutBottomBarOcc.setVisibility(View.VISIBLE);
            }
            if (btnFinalizeOcc != null) {
                btnFinalizeOcc.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showEditClassificationSheet(View v) {
        if (changesCount >= 1) {
            Toast.makeText(requireContext(), "As informações só podem ser alteradas uma vez.", Toast.LENGTH_SHORT).show();
            return;
        }
        String initialPrio = "Alta";
        if (currentPriority != null) {
            String pUpper = currentPriority.toUpperCase(java.util.Locale.forLanguageTag("pt-BR"));
            if (pUpper.contains("BAIX") || pUpper.contains("LOW")) {
                initialPrio = "Baixa";
            } else if (pUpper.contains("MÉD") || pUpper.contains("MED")) {
                initialPrio = "Média";
            }
        }
        EditClassificationSheet sheet = EditClassificationSheet.newInstance(
                currentMaterialName, currentQualityClass, initialPrio, false, "");
        sheet.setOnClassificationChangedListener((material, qualityClass, priority, isHazard, reason) -> {
            String prevMaterial = currentMaterial;
            currentQualityClass = "";
            currentMaterialName = material;
            currentMaterial = currentMaterialName;
            currentPriority = priority != null
                    ? priority.toUpperCase(java.util.Locale.forLanguageTag("pt-BR"))
                    : "ALTA";
            if (tvOccCardTitle != null) {
                tvOccCardTitle.setText(currentMaterial);
            }
            applyPriorityBadge(currentPriority);

            if (currentDetailJson != null) {
                currentDetailJson.addProperty("material", currentMaterial);
                currentDetailJson.addProperty("prioridade", currentPriority);
                PrefsHelper.putJson(requireContext(), PREFS, occurrenceId, currentDetailJson.toString());
            }

            OccurrenceStore.update(requireContext(), new Occurrence(
                    occurrenceId,
                    currentMaterial,
                    currentSector,
                    getString(R.string.notif_now),
                    currentPriority,
                    currentStatus,
                    currentMaterial));

            if (tvEditClassSub != null) {
                tvEditClassSub.setVisibility(View.VISIBLE);
                tvEditClassSub.setText("✓ " + currentMaterialName + " · " + priority.toLowerCase(java.util.Locale.forLanguageTag("pt-BR")));
            }

            AuditLog.append(requireContext(), com.aula.volta.data.local.SessionManager.name(requireContext()), "ALTERAR_CLASSIFICACAO",
                    "ocorrencia", occurrenceId, prevMaterial, currentMaterial);

            hasPendingChanges = true;
            pendingMaterial = currentMaterialName;
            pendingPriority = currentPriority;
            updateActionButtonsState();
        });
        sheet.show(getChildFragmentManager(), "edit_classification");
    }

    private String resolveWasteTypeId(String material) {
        if (!isAdded() || getContext() == null) return null;
        try {
            String listStr = PrefsHelper.getJson(requireContext(), "cache_waste_types", "list");
            if (listStr != null && !listStr.isEmpty()) {
                JsonArray arr = JsonParser.parseString(listStr).getAsJsonArray();
                String resolved = AiAnalysisJSON.resolveWasteTypeId(arr, material);
                if (resolved != null && !resolved.isEmpty()) {
                    return resolved;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void showAddObservationSheet(View v) {
        if (changesCount >= 1) {
            Toast.makeText(requireContext(), "As informações só podem ser alteradas uma vez.", Toast.LENGTH_SHORT).show();
            return;
        }
        AddObservationSheet sheet = AddObservationSheet.newInstance();
        sheet.setOnObservationSavedListener(obs -> {
            if (obs != null && !obs.trim().isEmpty()) {
                String cleanObs = obs.trim();
                if (tvObserveSub != null) {
                    tvObserveSub.setVisibility(View.VISIBLE);
                    tvObserveSub.setText("✓ " + cleanObs);
                }

                String baseReport = tvOccAutoReport != null && tvOccAutoReport.getText() != null
                        ? tvOccAutoReport.getText().toString().trim() : "";
                String combinedReport = baseReport.isEmpty() ? cleanObs : (baseReport + "\n" + cleanObs);
                if (tvOccAutoReport != null) {
                    tvOccAutoReport.setText(combinedReport);
                }

                if (currentDetailJson != null) {
                    currentDetailJson.addProperty("relatorio_auto", combinedReport);
                    PrefsHelper.putJson(requireContext(), PREFS, occurrenceId, currentDetailJson.toString());
                }

                AuditLog.append(requireContext(), com.aula.volta.data.local.SessionManager.name(requireContext()), "ADICIONAR_OBSERVACAO",
                        "ocorrencia", occurrenceId, null, cleanObs);

                hasPendingChanges = true;
                pendingObservation = cleanObs;
                updateActionButtonsState();
            }
        });
        sheet.show(getChildFragmentManager(), "add_observation");
    }

    private void updateActionButtonsState() {
        if (btnFinalizeOcc == null || !isAdded()) return;
        boolean isDone = "APPROVED".equalsIgnoreCase(currentStatus)
                || "APROVADO".equalsIgnoreCase(currentStatus)
                || "APROVADA".equalsIgnoreCase(currentStatus)
                || "RECUSADA".equalsIgnoreCase(currentStatus)
                || "REJECTED".equalsIgnoreCase(currentStatus);
        if (isDone) {
            btnFinalizeOcc.setVisibility(View.GONE);
            return;
        }
        btnFinalizeOcc.setVisibility(View.VISIBLE);
        if (isSending) {
            btnFinalizeOcc.setEnabled(false);
            btnFinalizeOcc.setText("Enviando ocorrência...");
            btnFinalizeOcc.setBackgroundResource(R.drawable.bg_btn_disabled_v);
            btnFinalizeOcc.setTextColor(android.graphics.Color.parseColor("#8E9590"));
            return;
        }
        if (hasPendingChanges) {
            btnFinalizeOcc.setEnabled(true);
            btnFinalizeOcc.setText("Alterar ocorrência");
            btnFinalizeOcc.setBackgroundResource(R.drawable.bg_btn_primary_v);
            btnFinalizeOcc.setTextColor(requireContext().getColor(R.color.white));
        } else if (!isSubmitted) {
            btnFinalizeOcc.setEnabled(true);
            btnFinalizeOcc.setText("Enviar ocorrência");
            btnFinalizeOcc.setBackgroundResource(R.drawable.bg_btn_primary_v);
            btnFinalizeOcc.setTextColor(requireContext().getColor(R.color.white));
        } else {
            btnFinalizeOcc.setEnabled(false);
            btnFinalizeOcc.setText("Ocorrência enviada");
            btnFinalizeOcc.setBackgroundResource(R.drawable.bg_btn_disabled_v);
            btnFinalizeOcc.setTextColor(android.graphics.Color.parseColor("#8E9590"));
        }
    }

    private void handleMainActionButtonClick(View v) {
        if (isSending) return;
        if (hasPendingChanges) {
            savePendingChangesToBackend();
        } else if (!isSubmitted) {
            executeSendOccurrenceToBackend();
        }
    }

    private void executeSendOccurrenceToBackend() {
        if (!com.aula.volta.util.NetworkUtils.isOnline(requireContext())) {
            Toast.makeText(requireContext(), "Sem conexão com a internet. Verifique sua rede.", Toast.LENGTH_SHORT).show();
            return;
        }
        isSending = true;
        updateActionButtonsState();

        String areaId = opt(currentDetailJson, "areaId", "");
        if (areaId.isEmpty()) {
            areaId = getCachedOrFallbackAreaId(requireContext());
        }
        if (areaId == null || areaId.isEmpty()) {
            areaId = "00000000-0000-0000-0000-000000000001";
        }

        String wasteTypeId = opt(currentDetailJson, "wasteTypeId", "");
        if (wasteTypeId.isEmpty()) {
            wasteTypeId = resolveWasteTypeId(currentMaterialName != null ? currentMaterialName : currentMaterial);
        }
        if (wasteTypeId == null || wasteTypeId.isEmpty()) {
            wasteTypeId = "00000000-0000-0000-0000-000000000001";
        }

        String photoUrl = opt(currentDetailJson, "photoUrl", opt(currentDetailJson, "imageUrl", ""));
        if (photoUrl != null && (photoUrl.isEmpty() || !photoUrl.startsWith("http"))) {
            photoUrl = null;
        }

        String empDescription = opt(currentDetailJson, "titulo", currentMaterial);
        String contamination = com.aula.volta.data.model.incident.IncidentRequestDTO.normalizeContamination(
                opt(currentDetailJson, "contaminacao", "LOW"));
        double qtd = 1.0;
        if (currentDetailJson != null && currentDetailJson.has("peso_estimado") && !currentDetailJson.get("peso_estimado").isJsonNull()) {
            try {
                qtd = currentDetailJson.get("peso_estimado").getAsDouble();
            } catch (Exception ignored) {
            }
        }
        String prio = com.aula.volta.data.model.incident.IncidentRequestDTO.normalizePriority(currentPriority);

        com.aula.volta.data.model.incident.IncidentRequestDTO req = new com.aula.volta.data.model.incident.IncidentRequestDTO(
                areaId,
                wasteTypeId,
                photoUrl,
                empDescription,
                contamination,
                qtd,
                prio
        );

        final String finalPhotoUrl = photoUrl;
        try {
            OccurrenceAPI occApi = ChatbotAuthClient.get(requireContext()).create(OccurrenceAPI.class);
            occApi.createIncident(req).enqueue(new Callback<IncidentResponseDTO>() {
                @Override
                public void onResponse(@NonNull Call<IncidentResponseDTO> call, @NonNull Response<IncidentResponseDTO> response) {
                    if (!isAdded()) return;
                    isSending = false;
                    if (response.isSuccessful() && response.body() != null && response.body().getId() != null) {
                        String newId = response.body().getId();
                        String oldDraftId = occurrenceId;
                        occurrenceId = newId;
                        isSubmitted = true;
                        hasPendingChanges = false;

                        if (currentDetailJson == null) {
                            currentDetailJson = new JsonObject();
                        }
                        currentDetailJson.addProperty("id", newId);
                        currentDetailJson.addProperty("is_submitted", true);
                        currentDetailJson.addProperty("changes_count", changesCount);
                        if (response.body().getPhotoUrl() != null && !response.body().getPhotoUrl().isEmpty()) {
                            currentDetailJson.addProperty("photoUrl", response.body().getPhotoUrl());
                        }
                        PrefsHelper.putJson(requireContext(), PREFS, newId, currentDetailJson.toString());

                        if (oldDraftId != null && !oldDraftId.equals(newId)) {
                            requireContext().getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).edit().remove(oldDraftId).apply();
                            OccurrenceStore.remove(requireContext(), oldDraftId);
                        }

                        Occurrence occ = new Occurrence(
                                newId,
                                currentMaterial,
                                currentSector,
                                getString(R.string.notif_now),
                                currentPriority,
                                "AGUARDANDO_VALIDACAO",
                                currentMaterial,
                                finalPhotoUrl,
                                currentLocalPhoto
                        );
                        OccurrenceStore.pushLocal(requireContext(), occ);

                        AuditLog.append(requireContext(), com.aula.volta.data.local.SessionManager.name(requireContext()), "ENVIAR",
                                "ocorrencia", newId, "RASCUNHO", "BANCO_OFICIAL");

                        NotificationHelper.notifyOccurrenceSent(requireContext(), newId, currentMaterial, currentSector);

                        updateActionButtonsState();

                        showTopBannerToast(
                                "Ocorrência enviada",
                                "Encaminhada para a central de gestão e análise técnica",
                                false,
                                true
                        );
                    } else {
                        updateActionButtonsState();
                        Toast.makeText(requireContext(), "Erro ao enviar ocorrência ao banco. Tente novamente.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<IncidentResponseDTO> call, @NonNull Throwable t) {
                    if (!isAdded()) return;
                    isSending = false;
                    updateActionButtonsState();
                    Toast.makeText(requireContext(), "Falha de rede ao enviar ocorrência. Verifique sua conexão.", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            isSending = false;
            updateActionButtonsState();
            Toast.makeText(requireContext(), "Erro inesperado ao enviar ocorrência.", Toast.LENGTH_SHORT).show();
        }
    }

    private void savePendingChangesToBackend() {
        if (!com.aula.volta.util.NetworkUtils.isOnline(requireContext())) {
            Toast.makeText(requireContext(), "Sem conexão com a internet. Verifique sua rede.", Toast.LENGTH_SHORT).show();
            return;
        }
        isSending = true;
        updateActionButtonsState();

        if (!isSubmitted) {
            executeSendOccurrenceToBackend();
            changesCount = 1;
            hasPendingChanges = false;
            if (currentDetailJson != null) {
                currentDetailJson.addProperty("changes_count", 1);
                PrefsHelper.putJson(requireContext(), PREFS, occurrenceId, currentDetailJson.toString());
            }
            return;
        }

        try {
            OccurrenceAPI occApi = ChatbotAuthClient.get(requireContext()).create(OccurrenceAPI.class);
            JsonObject updateBody = new JsonObject();
            if (pendingMaterial != null && !pendingMaterial.isEmpty()) {
                updateBody.addProperty("title", pendingMaterial);
                String wid = resolveWasteTypeId(pendingMaterial);
                if (wid != null && !wid.isEmpty()) {
                    JsonArray arr = new JsonArray();
                    arr.add(wid);
                    updateBody.add("wasteTypeIds", arr);
                }
            }
            if (pendingPriority != null && !pendingPriority.isEmpty()) {
                updateBody.addProperty("priority", com.aula.volta.data.model.incident.IncidentRequestDTO.normalizePriority(pendingPriority));
            }
            if (pendingObservation != null && !pendingObservation.isEmpty()) {
                String baseReport = tvOccAutoReport != null && tvOccAutoReport.getText() != null
                        ? tvOccAutoReport.getText().toString().trim() : "";
                updateBody.addProperty("aiDescription", baseReport);
                updateBody.addProperty("changeReason", pendingObservation);
            } else {
                updateBody.addProperty("changeReason", "Alteração de classificação pelo usuário");
            }

            occApi.updateIncident(occurrenceId, updateBody).enqueue(new Callback<IncidentResponseDTO>() {
                @Override
                public void onResponse(@NonNull Call<IncidentResponseDTO> call, @NonNull Response<IncidentResponseDTO> response) {
                    if (!isAdded()) return;
                    isSending = false;
                    changesCount = 1;
                    hasPendingChanges = false;
                    if (currentDetailJson != null) {
                        currentDetailJson.addProperty("changes_count", 1);
                        PrefsHelper.putJson(requireContext(), PREFS, occurrenceId, currentDetailJson.toString());
                    }
                    updateActionButtonsState();
                    showTopBannerToast("Ocorrência alterada", "Alterações salvas com sucesso", false, true);
                }

                @Override
                public void onFailure(@NonNull Call<IncidentResponseDTO> call, @NonNull Throwable t) {
                    if (!isAdded()) return;
                    isSending = false;
                    updateActionButtonsState();
                    Toast.makeText(requireContext(), "Falha ao salvar alteração no banco. Verifique sua conexão.", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            isSending = false;
            updateActionButtonsState();
            Toast.makeText(requireContext(), "Erro inesperado ao salvar alteração.", Toast.LENGTH_SHORT).show();
        }
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
        return "00000000-0000-0000-0000-000000000001";
    }

    private String opt(JsonObject json, String key, String fallback) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? json.get(key).getAsString() : fallback;
    }
}
