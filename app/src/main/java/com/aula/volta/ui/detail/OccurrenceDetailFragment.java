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

/**
 * Detalhe da Ocorrência — Figma 851:3792.
 * Telas:
 *  - 17 · Ocorrência (851:4196)
 *  - 17a · Alterar classificação 2 (908:5946)
 *  - 17c · Adicionar observação 2 (908:6103)
 *  - 17d · Ocorrência · ações aplicadas 2 (908:6409)
 *  - Diálogo · finalizar (851:4885)
 *  - 19 · Ocorrência · finalizada (851:4403)
 */
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
    private MaterialButton btnFinalizeOcc;
    private View layoutBottomBarOcc;
    private String currentLocalPhoto;
    private String currentRemoteUrl;
    private View toastFinalized;
    private TextView tvToastFinalizedTitle;
    private TextView tvToastFinalizedSub;
    private TextView btnToastOpenPgrs;

    // Tracker 3 steps
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
    private String currentMaterial = "Papelão (Classe A)";
    private String currentMaterialName = "Papelão";
    private String currentQualityClass = "A";
    private String currentPriority = "BAIXA";
    private String currentContamination = "baixa";
    private String currentSector = "Frigorífico — Setor B2";
    private String currentWeight = "38 kg";
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
        btnFinalizeOcc = view.findViewById(R.id.btnFinalizeOcc);
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

        occurrenceId = getArguments() != null ? getArguments().getString("occurrenceId") : "8514";
        if (occurrenceId == null || occurrenceId.isEmpty()) {
            occurrenceId = "8514";
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

        // Ações do responsável (17a, 17c)
        if (rowEditClass != null) {
            rowEditClass.setOnClickListener(this::showEditClassificationSheet);
        }
        if (rowObserve != null) {
            rowObserve.setOnClickListener(this::showAddObservationSheet);
        }

        // Finalizar ocorrência (Diálogo 851:4885)
        if (btnFinalizeOcc != null) {
            btnFinalizeOcc.setOnClickListener(this::showFinalizeDialog);
        }

        // Ação do Toast PGRS (abre PDF mockado)
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
        // 1. Carrega imediatamente do cache local para exibir foto, relatório e dados sem atraso
        String cachedStr = PrefsHelper.getJson(requireContext(), PREFS, occurrenceId);
        if (cachedStr != null && !cachedStr.isEmpty()) {
            try {
                currentDetailJson = JsonParser.parseString(cachedStr).getAsJsonObject();
                applyDetail(currentDetailJson);
            } catch (Exception ignored) {
            }
        }

        // 2. Consulta API oficial do backend (/incidents/{id})
        OccurrenceAPI api = ApiClient.get(requireContext()).create(OccurrenceAPI.class);
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
                    String mat = (dto.getEmployeeDescription() != null && !dto.getEmployeeDescription().isEmpty())
                            ? dto.getEmployeeDescription()
                            : (currentDetailJson != null && currentDetailJson.has("material") ? currentDetailJson.get("material").getAsString() : "Papelão (Classe A)");
                    body.addProperty("titulo", mat);
                    body.addProperty("material", mat);
                    String sector = (currentDetailJson != null && currentDetailJson.has("setor"))
                            ? currentDetailJson.get("setor").getAsString() : "Frigorífico — Setor B2";
                    body.addProperty("setor", sector);
                    double peso = (dto.getEstimatedQuantity() != null && dto.getEstimatedQuantity() > 0)
                            ? dto.getEstimatedQuantity()
                            : (currentDetailJson != null && currentDetailJson.has("peso_estimado") ? currentDetailJson.get("peso_estimado").getAsDouble() : 5.0);
                    body.addProperty("peso_estimado", peso);

                    String stat = (dto.getStatus() != null && !dto.getStatus().isEmpty()) ? dto.getStatus() : currentStatus;
                    body.addProperty("status", stat);

                    String prio = (dto.getPriority() != null && !dto.getPriority().isEmpty())
                            ? dto.getPriority()
                            : (currentDetailJson != null && currentDetailJson.has("prioridade") ? currentDetailJson.get("prioridade").getAsString() : AiAnalysisJSON.calculatePriority(mat, "LOW", dto.getEmployeeDescription()));
                    body.addProperty("prioridade", prio);

                    body.addProperty("quando", dto.getRegisteredAt() != null ? dto.getRegisteredAt() : (currentDetailJson != null && currentDetailJson.has("quando") ? currentDetailJson.get("quando").getAsString() : "agora"));

                    // Preserva foto remota ou local sem sobrescrever com null
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

                    // Preserva relatório automático detalhado da IA sem sobrescrever com descrição simples
                    String relatorio = (currentDetailJson != null && currentDetailJson.has("relatorio_auto") && !currentDetailJson.get("relatorio_auto").getAsString().isEmpty())
                            ? currentDetailJson.get("relatorio_auto").getAsString()
                            : (dto.getEmployeeDescription() != null ? dto.getEmployeeDescription() : "");
                    if (!relatorio.isEmpty()) {
                        body.addProperty("relatorio_auto", relatorio);
                    }

                    currentDetailJson = body;
                    PrefsHelper.putJson(requireContext(), PREFS, occurrenceId, currentDetailJson.toString());
                    applyDetail(currentDetailJson);

                    // Busca relatório oficial de IA se ainda não estiver completo
                    fetchAiReportIfMissing(api, body);
                } else {
                    fallbackOccurrenceDetail(api);
                }
            }

            @Override
            public void onFailure(@NonNull Call<IncidentResponseDTO> call, @NonNull Throwable t) {
                if (isAdded()) {
                    fallbackOccurrenceDetail(api);
                }
            }
        });


        // 4. Consulta a API de Rascunhos para validar se foi aprovado pela gestão web
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
        if (occurrenceId == null || occurrenceId.isEmpty() || !isAdded()) return;
        try {
            ChatbotAPI chatApi = ChatbotClient.get(requireContext()).create(ChatbotAPI.class);
            chatApi.listOccurrenceDrafts().enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    if (!isAdded() || !response.isSuccessful() || response.body() == null) return;
                    JsonObject res = response.body();
                    if (res.has("data") && res.get("data").isJsonArray()) {
                        JsonArray arr = res.getAsJsonArray("data");
                        for (int i = 0; i < arr.size(); i++) {
                            JsonObject draft = arr.get(i).getAsJsonObject();
                            if (draft.has("id") && occurrenceId.equals(draft.get("id").getAsString())) {
                                String st = draft.has("status") ? draft.get("status").getAsString() : "";
                                if ("APROVADO".equalsIgnoreCase(st) || "APPROVED".equalsIgnoreCase(st) || "CONCLUIDO".equalsIgnoreCase(st)) {
                                    if (!"APROVADA".equalsIgnoreCase(currentStatus)) {
                                        currentStatus = "APROVADA";
                                        if (currentDetailJson != null) {
                                            currentDetailJson.addProperty("status", "APROVADA");
                                            PrefsHelper.putJson(requireContext(), PREFS, occurrenceId, currentDetailJson.toString());
                                        }
                                        syncFinalizedUi();
                                        NotificationHelper.notifyOccurrenceApproved(requireContext(), occurrenceId, currentMaterial);
                                    }
                                }
                                break;
                            }
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                }
            });
        } catch (Exception ignored) {
        }
    }



    private void fallbackOccurrenceDetail(OccurrenceAPI api) {
        api.getOccurrence(occurrenceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    if (currentDetailJson != null) {
                        if (!body.has("photoPath") && currentDetailJson.has("photoPath")) {
                            body.addProperty("photoPath", currentDetailJson.get("photoPath").getAsString());
                        }
                        if (!body.has("imageUrl") && currentDetailJson.has("imageUrl")) {
                            body.addProperty("imageUrl", currentDetailJson.get("imageUrl").getAsString());
                        }
                    }
                    currentDetailJson = body;
                    applyDetail(currentDetailJson);
                } else if (currentDetailJson == null) {
                    loadFromCacheOrMock();
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                if (currentDetailJson == null) {
                    loadFromCacheOrMock();
                }
            }
        });
    }

    private void loadFromCacheOrMock() {
        String cached = PrefsHelper.getJson(requireContext(), PREFS, occurrenceId);
        if (cached != null) {
            try {
                currentDetailJson = JsonParser.parseString(cached).getAsJsonObject();
                applyDetail(currentDetailJson);
                return;
            } catch (Exception ignored) {
            }
        }

        // Mock alinhado ao protótipo do Figma 851:3792
        currentDetailJson = new JsonObject();
        currentDetailJson.addProperty("id", occurrenceId);
        currentDetailJson.addProperty("titulo", "Papelão (Classe A)");
        currentDetailJson.addProperty("material", "Papelão (Classe A)");
        currentDetailJson.addProperty("setor", "Frigorífico — Setor B2");
        currentDetailJson.addProperty("peso_estimado", 38.0);
        currentDetailJson.addProperty("status", currentStatus);
        currentDetailJson.addProperty("prioridade", "BAIXA");
        currentDetailJson.addProperty("quando", "hoje, 09:12");
        currentDetailJson.addProperty("relatorio_auto",
                "Material com umidade superficial, ainda reciclável. Remover em até 24h e guardar em área coberta.");
        applyDetail(currentDetailJson);
    }

    private void applyDetail(JsonObject json) {
        if (json == null) {
            return;
        }

        String rawMaterial = opt(json, "material", "Papelão (Classe A)");
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
                String rawPeso = opt(json, "peso_estimado", "38");
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

        currentSector = opt(json, "setor", "Frigorífico — Setor B2");
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
            tvOccSubtitle.setText("Registrada por " + com.aula.volta.data.local.SessionManager.name(requireContext()) + " • " + quando);
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

    /**
     * Sincroniza a interface conforme status da ocorrência (17 vs 19).
     */
    private void syncFinalizedUi() {
        if (!isAdded()) return;

        boolean isDone = "APPROVED".equalsIgnoreCase(currentStatus)
                || "APROVADO".equalsIgnoreCase(currentStatus)
                || "APROVADA".equalsIgnoreCase(currentStatus)
                || "RESOLVIDO".equalsIgnoreCase(currentStatus)
                || "RESOLVIDA".equalsIgnoreCase(currentStatus)
                || "FINALIZADA".equalsIgnoreCase(currentStatus)
                || "FINALIZADO".equalsIgnoreCase(currentStatus)
                || "CONCLUIDA".equalsIgnoreCase(currentStatus)
                || "CONCLUIDO".equalsIgnoreCase(currentStatus);

        // Tracker de 3 passos
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

        if (isDone) {
            if (stepLine1 != null) {
                stepLine1.setBackgroundColor(requireContext().getColor(R.color.vf_green));
            }
            if (stepCircle2 != null) {
                stepCircle2.setBackgroundResource(R.drawable.bg_step_done);
            }
            if (stepCheck2 != null) {
                stepCheck2.setVisibility(View.VISIBLE);
            }
            if (stepLabel2 != null) {
                stepLabel2.setTextColor(requireContext().getColor(R.color.vf_green_deep));
            }

            // Badge superior direito: APROVADA
            if (tvOccStatus != null) {
                tvOccStatus.setText("APROVADA");
                tvOccStatus.setBackgroundResource(R.drawable.bg_badge_aprovada);
                tvOccStatus.setTextColor(requireContext().getColor(R.color.status_aprovada_text));
            }

            // Na tela finalizada pela gestão Web, ações de edição e botão de envio somem
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
            if (stepLine1 != null) {
                stepLine1.setBackgroundColor(requireContext().getColor(R.color.vf_border));
            }
            if (stepCircle2 != null) {
                stepCircle2.setBackgroundResource(R.drawable.bg_notification_circle);
            }
            if (stepCheck2 != null) {
                stepCheck2.setVisibility(View.GONE);
            }
            if (stepLabel2 != null) {
                stepLabel2.setTextColor(requireContext().getColor(R.color.vf_text_secondary));
            }

            // Badge superior direito: EM ANÁLISE
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

    /** Bottom sheet de alteração de classificação (Figma 17a · 908:5946) */
    private void showEditClassificationSheet(View v) {
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
            String prevClass = currentQualityClass;
            String prevMaterial = currentMaterial;
            currentQualityClass = qualityClass;
            currentMaterialName = material;
            currentMaterial = currentMaterialName + " (Classe " + currentQualityClass + ")";
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

            // Subtítulo da ação aplicada (Figma 17d · 908:6409)
            if (tvEditClassSub != null) {
                tvEditClassSub.setVisibility(View.VISIBLE);
                tvEditClassSub.setText("✓ Qualidade " + prevClass + " → " + currentQualityClass + " · prioridade " + priority.toLowerCase());
            }

            AuditLog.append(requireContext(), com.aula.volta.data.local.SessionManager.name(requireContext()), "ALTERAR_CLASSIFICACAO",
                    "ocorrencia", occurrenceId, prevMaterial, currentMaterial);

            Toast.makeText(requireContext(), "Classificação alterada para Classe " + currentQualityClass, Toast.LENGTH_SHORT).show();
        });
        sheet.show(getChildFragmentManager(), "edit_classification");
    }

    /** Bottom sheet de adicionar observação (Figma 17c · 908:6103) */
    private void showAddObservationSheet(View v) {
        AddObservationSheet sheet = AddObservationSheet.newInstance();
        sheet.setOnObservationSavedListener(obs -> {
            // Subtítulo da observação adicionada (Figma 17d · 908:6409)
            if (tvObserveSub != null) {
                tvObserveSub.setVisibility(View.VISIBLE);
                tvObserveSub.setText("✓ 1 observação · agora");
            }

            AuditLog.append(requireContext(), com.aula.volta.data.local.SessionManager.name(requireContext()), "ADICIONAR_OBSERVACAO",
                    "ocorrencia", occurrenceId, null, obs);

            showTopBannerToast("Observação salva!", "Adicionada ao histórico da ocorrência", false, true);
        });
        sheet.show(getChildFragmentManager(), "add_observation");
    }

    /** Diálogo de confirmação para finalizar (Figma 851:4885) */
    private void showFinalizeDialog(View v) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_finalize, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        com.aula.volta.util.BottomSheetImmersiveHelper.configureDialog(dialog);

        TextView tvSummary = dialogView.findViewById(R.id.tvDialogSummaryTitle);
        TextView tvSector = dialogView.findViewById(R.id.tvDialogSummarySector);
        if (tvSummary != null) {
            tvSummary.setText(currentMaterial + " · ≈ " + currentWeight);
        }
        if (tvSector != null) {
            tvSector.setText(currentSector);
        }

        View btnYes = dialogView.findViewById(R.id.btnFinalizeYes);
        if (btnYes != null) {
            btnYes.setOnClickListener(b -> {
                dialog.dismiss();
                currentStatus = "EM_ANALISE";

                AuditLog.append(requireContext(), com.aula.volta.data.local.SessionManager.name(requireContext()), "ENVIAR",
                        "ocorrencia", occurrenceId, "RASCUNHO", "EM_ANALISE");

                Toast.makeText(requireContext(), R.string.occ_finalize_done, Toast.LENGTH_SHORT).show();

                if (currentDetailJson != null) {
                    currentDetailJson.addProperty("status", "EM_ANALISE");
                    PrefsHelper.putJson(requireContext(), PREFS, occurrenceId, currentDetailJson.toString());
                }

                Occurrence occ = new Occurrence(
                        occurrenceId,
                        currentMaterial,
                        currentSector,
                        getString(R.string.notif_now),
                        currentPriority,
                        "EM_ANALISE",
                        currentMaterial);
                OccurrenceStore.update(requireContext(), occ);

                // Disparo de finalização oficial para a API do banco de dados (PATCH /incidents/{id}/close)
                try {
                    OccurrenceAPI occApi = ApiClient.get(requireContext()).create(OccurrenceAPI.class);
                    occApi.closeIncident(occurrenceId).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call,
                                               @NonNull Response<Void> response) {
                        }

                        @Override
                        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        }
                    });
                } catch (Exception ignored) {
                }

                com.aula.volta.data.notification.NotificationHelper.notifyOccurrenceSent(
                        requireContext(), occurrenceId, currentMaterial, currentSector);

                currentStatus = "AGUARDANDO_VALIDACAO";
                if (currentDetailJson != null) {
                    currentDetailJson.addProperty("status", currentStatus);
                    PrefsHelper.putJson(requireContext(), PREFS, occurrenceId, currentDetailJson.toString());
                }
                syncFinalizedUi();

                // Exibe Toast Flutuante de Ocorrência Enviada para Análise
                showTopBannerToast(
                        "Ocorrência enviada",
                        "Encaminhada para a central de gestão e análise técnica",
                        false,
                        true
                );
            });
        }

        View btnNo = dialogView.findViewById(R.id.btnFinalizeNo);
        if (btnNo != null) {
            btnNo.setOnClickListener(b -> dialog.dismiss());
        }

        dialog.show();
    }

    private String opt(JsonObject json, String key, String fallback) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? json.get(key).getAsString() : fallback;
    }
}
