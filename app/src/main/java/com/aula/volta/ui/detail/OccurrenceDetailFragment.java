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
import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.OccurrenceAPI;
import com.aula.volta.data.local.AuditLog;
import com.aula.volta.data.local.NotificationStore;
import com.aula.volta.data.local.OccurrenceStore;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.model.Notification;
import com.aula.volta.data.model.Occurrence;
import com.aula.volta.data.notification.NotificationHelper;
import com.aula.volta.data.report.PgrsReport;
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
    private View toastFinalized;
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
    private String currentSector = "Frigorífico — Setor B2";
    private String currentWeight = "38 kg";

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
        toastFinalized = view.findViewById(R.id.toastFinalized);
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

        view.findViewById(R.id.btnBackOcc).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        // Ações do responsável (17a, 17c)
        rowEditClass.setOnClickListener(this::showEditClassificationSheet);
        rowObserve.setOnClickListener(this::showAddObservationSheet);

        // Finalizar ocorrência (Diálogo 851:4885)
        btnFinalizeOcc.setOnClickListener(this::showFinalizeDialog);

        // Ação do Toast PGRS
        View.OnClickListener openPgrsAction = v -> {
            try {
                File report = PgrsReport.generate(requireContext());
                if (report != null) {
                    PgrsReport.share(requireContext(), report);
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
            toastFinalized.setOnClickListener(openPgrsAction);
        }

        loadDetail();
    }

    private void loadDetail() {
        OccurrenceAPI api = ApiClient.get(requireContext()).create(OccurrenceAPI.class);
        api.getOccurrence(occurrenceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    currentDetailJson = response.body();
                    applyDetail(currentDetailJson);
                } else {
                    loadFromCacheOrMock();
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                loadFromCacheOrMock();
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
        currentDetailJson.addProperty("prioridade", "ALTA");
        currentDetailJson.addProperty("quando", "hoje, 09:12");
        currentDetailJson.addProperty("relatorio_auto",
                "Ocorrência registrada no setor Frigorífico com 38 kg de papelão Classe A. Destinação recomendada: enfardamento para cooperativa parceira.");
        applyDetail(currentDetailJson);
    }

    private void applyDetail(JsonObject json) {
        if (json == null) {
            return;
        }

        currentMaterial = opt(json, "material", "Papelão (Classe A)");
        currentSector = opt(json, "setor", "Frigorífico — Setor B2");
        String quando = opt(json, "quando", "hoje, 09:12");
        currentStatus = opt(json, "status", "EM_ANALISE");

        tvOccTitle.setText(getString(R.string.occ_title));
        tvOccSubtitle.setText("Registrada por Breno G. · " + quando);

        tvOccCardTitle.setText(currentMaterial);
        tvOccCardLine.setText("Contaminação baixa · ≈ " + currentWeight);
        tvOccCardSubtitle.setText(currentSector);

        String autoReport = opt(json, "relatorio_auto",
                "Ocorrência registrada no setor Frigorífico com 38 kg de papelão Classe A. Destinação recomendada: enfardamento para cooperativa parceira.");
        tvOccAutoReport.setText(autoReport);

        syncFinalizedUi();
    }

    /**
     * Sincroniza a interface conforme status da ocorrência (17 vs 19).
     */
    private void syncFinalizedUi() {
        boolean isDone = "APROVADA".equals(currentStatus) || "RESOLVIDO".equals(currentStatus);

        // Tracker de 3 passos
        stepCircle0.setBackgroundResource(R.drawable.bg_step_done);
        stepCheck0.setVisibility(View.VISIBLE);
        stepLabel0.setTextColor(requireContext().getColor(R.color.vf_green_deep));
        stepLine0.setBackgroundColor(requireContext().getColor(R.color.vf_green));

        stepCircle1.setBackgroundResource(R.drawable.bg_step_done);
        stepCheck1.setVisibility(View.VISIBLE);
        stepLabel1.setTextColor(requireContext().getColor(R.color.vf_green_deep));

        if (isDone) {
            stepLine1.setBackgroundColor(requireContext().getColor(R.color.vf_green));
            stepCircle2.setBackgroundResource(R.drawable.bg_step_done);
            stepCheck2.setVisibility(View.VISIBLE);
            stepLabel2.setTextColor(requireContext().getColor(R.color.vf_green_deep));

            // Badge superior direito
            tvOccStatus.setText("APROVADA");
            tvOccStatus.setBackgroundResource(R.drawable.bg_badge_aprovada);
            tvOccStatus.setTextColor(requireContext().getColor(R.color.status_aprovada_text));

            // Na tela 19, 'Alterar classificação' e 'Finalizar ocorrência' somem
            rowEditClass.setVisibility(View.GONE);
            btnFinalizeOcc.setVisibility(View.GONE);
        } else {
            stepLine1.setBackgroundColor(requireContext().getColor(R.color.vf_border));
            stepCircle2.setBackgroundResource(R.drawable.bg_notification_circle);
            stepCheck2.setVisibility(View.GONE);
            stepLabel2.setTextColor(requireContext().getColor(R.color.vf_text_secondary));

            // Badge superior direito
            tvOccStatus.setText("EM ANÁLISE");
            tvOccStatus.setBackgroundResource(R.drawable.bg_badge_media);
            tvOccStatus.setTextColor(requireContext().getColor(R.color.vf_gold));

            rowEditClass.setVisibility(View.VISIBLE);
            btnFinalizeOcc.setVisibility(View.VISIBLE);
        }
    }

    /** Bottom sheet de alteração de classificação (Figma 17a · 908:5946) */
    private void showEditClassificationSheet(View v) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_classification, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnCloseSheet).setOnClickListener(b -> dialog.dismiss());
        dialogView.findViewById(R.id.btnCancelEditClass).setOnClickListener(b -> dialog.dismiss());

        dialogView.findViewById(R.id.btnConfirmEditClass).setOnClickListener(b -> {
            dialog.dismiss();
            currentMaterial = "Papelão (Classe B)";
            tvOccCardTitle.setText(currentMaterial);

            // Subtítulo da ação aplicada (Figma 17d · 908:6409)
            tvEditClassSub.setVisibility(View.VISIBLE);
            tvEditClassSub.setText("✓ Qualidade A → B · prioridade alta");

            AuditLog.append(requireContext(), "Breno Gomes", "ALTERAR_CLASSIFICACAO",
                    "ocorrencia", occurrenceId, "Papelão (Classe A)", currentMaterial);

            Toast.makeText(requireContext(), "Classificação alterada para Classe B", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    /** Bottom sheet de adicionar observação (Figma 17c · 908:6103) */
    private void showAddObservationSheet(View v) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_observation, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etInput = dialogView.findViewById(R.id.etObservationInput);
        TextView tvCounter = dialogView.findViewById(R.id.tvCharCounter);

        if (etInput != null && tvCounter != null) {
            tvCounter.setText(etInput.getText().length() + "/280");
            etInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    tvCounter.setText(s.length() + "/280");
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        // Atalhos rápidos
        View shortcutWet = dialogView.findViewById(R.id.shortcutWet);
        if (shortcutWet != null && etInput != null) {
            shortcutWet.setOnClickListener(b -> {
                String cur = etInput.getText().toString();
                if (!cur.contains("Material molhado")) {
                    etInput.setText(cur.isEmpty() ? "Material molhado." : cur + " Material molhado.");
                }
            });
        }

        dialogView.findViewById(R.id.btnCloseObsSheet).setOnClickListener(b -> dialog.dismiss());
        dialogView.findViewById(R.id.btnCancelObservation).setOnClickListener(b -> dialog.dismiss());

        dialogView.findViewById(R.id.btnSaveObservation).setOnClickListener(b -> {
            String obs = etInput != null ? etInput.getText().toString().trim() : "";
            dialog.dismiss();

            // Subtítulo da observação adicionada (Figma 17d · 908:6409)
            tvObserveSub.setVisibility(View.VISIBLE);
            tvObserveSub.setText("✓ 1 observação · agora");

            AuditLog.append(requireContext(), "Breno Gomes", "ADICIONAR_OBSERVACAO",
                    "ocorrencia", occurrenceId, null, obs);

            Toast.makeText(requireContext(), "Observação adicionada com sucesso", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
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

        TextView tvSummary = dialogView.findViewById(R.id.tvDialogSummaryTitle);
        TextView tvSector = dialogView.findViewById(R.id.tvDialogSummarySector);
        if (tvSummary != null) {
            tvSummary.setText(currentMaterial + " · ≈ " + currentWeight);
        }
        if (tvSector != null) {
            tvSector.setText(currentSector);
        }

        dialogView.findViewById(R.id.btnFinalizeYes).setOnClickListener(b -> {
            dialog.dismiss();
            currentStatus = "APROVADA";

            AuditLog.append(requireContext(), "Breno Gomes", "FINALIZAR",
                    "ocorrencia", occurrenceId, "EM_ANALISE", "APROVADA");

            NotificationStore.pushLocal(requireContext(), new Notification(
                    "local_" + System.currentTimeMillis(),
                    "OCORRENCIA_FINALIZADA",
                    getString(R.string.notif_finalize_title, occurrenceId),
                    getString(R.string.notif_finalize_desc),
                    getString(R.string.notif_now),
                    "check",
                    "#12A05E",
                    "#E2F7EC",
                    false,
                    occurrenceId));

            NotificationHelper.notifyPgrsFinalized(requireContext(), occurrenceId);

            if (currentDetailJson != null) {
                currentDetailJson.addProperty("status", "APROVADA");
                PrefsHelper.putJson(requireContext(), PREFS, occurrenceId, currentDetailJson.toString());
            }

            Occurrence occ = new Occurrence(
                    occurrenceId,
                    currentMaterial,
                    currentSector,
                    getString(R.string.notif_now),
                    "ALTA",
                    "RESOLVIDO",
                    currentMaterial);
            OccurrenceStore.update(requireContext(), occ);

            syncFinalizedUi();

            // Exibe Toast Flutuante de Ocorrência Finalizada (Figma 19 · 851:4403)
            if (toastFinalized != null) {
                toastFinalized.setAlpha(0f);
                toastFinalized.setVisibility(View.VISIBLE);
                toastFinalized.animate().alpha(1f).setDuration(350).start();
            }
        });

        dialogView.findViewById(R.id.btnFinalizeNo).setOnClickListener(b -> dialog.dismiss());
        dialog.show();
    }

    private String opt(JsonObject json, String key, String fallback) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? json.get(key).getAsString() : fallback;
    }
}
