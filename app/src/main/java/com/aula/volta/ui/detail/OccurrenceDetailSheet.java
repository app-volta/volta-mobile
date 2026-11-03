package com.aula.volta.ui.detail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import android.app.Dialog;
import com.aula.volta.R;
import com.aula.volta.util.BottomSheetImmersiveHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.ChatbotAuthClient;
import com.aula.volta.data.api.OccurrenceAPI;
import com.aula.volta.data.model.incident.IncidentResponseDTO;
import androidx.annotation.NonNull;
import com.aula.volta.data.local.AuditLog;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.report.PgrsReport;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OccurrenceDetailSheet extends BottomSheetDialogFragment {

    private static final String PREFS = "cache_occurrence_detail";

    private TextView tvTitle;
    private TextView tvSubtitle;
    private TextView tvStatus;
    private TextView tvWeight;
    private TextView tvAuthor;
    private TextView tvDate;
    private TextView tvConfidence;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        BottomSheetImmersiveHelper.configureBottomSheet(dialog);
        return dialog;
    }

    public OccurrenceDetailSheet() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_occurrence_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTitle = view.findViewById(R.id.tvDetailTitle);
        tvSubtitle = view.findViewById(R.id.tvDetailSubtitle);
        tvStatus = view.findViewById(R.id.tvDetailStatus);
        tvWeight = view.findViewById(R.id.tvDetailWeight);
        tvAuthor = view.findViewById(R.id.tvDetailAuthor);
        tvDate = view.findViewById(R.id.tvDetailDate);
        tvConfidence = view.findViewById(R.id.tvDetailConfidence);

        View btnReport = view.findViewById(R.id.btnFullReport);
        btnReport.setOnClickListener(v -> {
            java.io.File report = PgrsReport.generate(requireContext());
            if (report == null) {
                Toast.makeText(requireContext(), R.string.reports_nothing,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            AuditLog.append(requireContext(), com.aula.volta.data.local.SessionManager.name(requireContext()), "GERAR",
                    "relatorio", report.getName(), null, null);
            PgrsReport.share(requireContext(), report);
        });

        String occurrenceId = getArguments() != null
                ? getArguments().getString("occurrenceId") : null;
        if (occurrenceId == null || occurrenceId.isEmpty()) {
            dismiss();
            return;
        }

        View btnPickup = view.findViewById(R.id.btnRequestPickup);
        if (btnPickup != null) {
            btnPickup.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("occurrenceId", occurrenceId);
                dismiss();
                NavHostFragment.findNavController(this)
                        .navigate(R.id.nav_cooperative_pick, args);
            });
        }
        loadDetail(occurrenceId);
    }

    private void loadDetail(String occurrenceId) {
        OccurrenceAPI api = ChatbotAuthClient.get(requireContext()).create(OccurrenceAPI.class);
        api.getIncidentById(occurrenceId).enqueue(new Callback<IncidentResponseDTO>() {
            @Override
            public void onResponse(@NonNull Call<IncidentResponseDTO> call, @NonNull Response<IncidentResponseDTO> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    IncidentResponseDTO dto = response.body();
                    JsonObject detail = new JsonObject();
                    detail.addProperty("id", dto.getId());
                    String cleanTitle;
                    if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty() && !com.aula.volta.data.model.AiAnalysisJSON.isGenericLabel(dto.getTitle())) {
                        cleanTitle = dto.getTitle().trim();
                    } else if (dto.getEmployeeDescription() != null && !dto.getEmployeeDescription().trim().isEmpty() && !com.aula.volta.data.model.AiAnalysisJSON.isGenericLabel(dto.getEmployeeDescription())) {
                        cleanTitle = dto.getEmployeeDescription().trim();
                    } else if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
                        cleanTitle = dto.getTitle().trim();
                    } else if (dto.getEmployeeDescription() != null && !dto.getEmployeeDescription().trim().isEmpty()) {
                        cleanTitle = dto.getEmployeeDescription().trim();
                    } else {
                        cleanTitle = "Resíduo Reciclável";
                    }
                    detail.addProperty("titulo", cleanTitle);
                    detail.addProperty("material", cleanTitle);
                    detail.addProperty("setor", com.aula.volta.data.model.Occurrence.resolveSector(dto, getContext()));
                    detail.addProperty("peso_estimado", dto.getEstimatedQuantity() != null ? dto.getEstimatedQuantity() : 0.0);
                    detail.addProperty("status", dto.getStatus() != null ? dto.getStatus() : "EM_ANALISE");
                    detail.addProperty("prioridade", dto.getPriority() != null ? dto.getPriority() : "ALTA");
                    detail.addProperty("quando", dto.getRegisteredAt() != null ? dto.getRegisteredAt() : "agora");
                    if (dto.getAiDescription() != null && !dto.getAiDescription().trim().isEmpty()) {
                        detail.addProperty("relatorio_auto", dto.getAiDescription().trim());
                    }
                    if (dto.getPhotoUrl() != null && !dto.getPhotoUrl().isEmpty()) {
                        detail.addProperty("imageUrl", dto.getPhotoUrl());
                        detail.addProperty("photoUrl", dto.getPhotoUrl());
                    }
                    PrefsHelper.putJson(requireContext(), PREFS, occurrenceId, detail.toString());
                    applyDetail(detail);
                } else {
                    loadFromCache(occurrenceId);
                }
            }

            @Override
            public void onFailure(@NonNull Call<IncidentResponseDTO> call, @NonNull Throwable t) {
                if (isAdded()) {
                    loadFromCache(occurrenceId);
                }
            }
        });
    }

    private void loadFromCache(String occurrenceId) {
        if (!isAdded()) {
            return;
        }
        String json = PrefsHelper.getJson(requireContext(), PREFS, occurrenceId);
        if (json != null) {
            try {
                applyDetail(com.google.gson.JsonParser.parseString(json).getAsJsonObject());
            } catch (Exception ignored) {
                dismiss();
            }
        } else {
            Toast.makeText(requireContext(), R.string.error_load, Toast.LENGTH_LONG).show();
            dismiss();
        }
    }

    private void applyDetail(JsonObject detail) {
        setText(tvTitle, opt(detail, "titulo"));
        String setor = opt(detail, "setor");
        tvSubtitle.setText(setor);
        tvStatus.setText(prettyStatus(opt(detail, "status")));

        String peso = opt(detail, "peso_estimado");
        String unidade = opt(detail, "unidade");
        tvWeight.setText("≈" + peso + (unidade.isEmpty() ? "" : " " + unidade));

        setText(tvAuthor, opt(detail, "registrado_por"));
        String rawDate = opt(detail, "data");
        if (rawDate.isEmpty()) rawDate = opt(detail, "quando");
        setText(tvDate, com.aula.volta.data.model.Occurrence.formatBrazilianDateTime(rawDate));
        String confianca = opt(detail, "confianca_ia");
        tvConfidence.setText(confianca.isEmpty() ? "-" : confianca + "%");
    }

    private String opt(JsonObject detail, String member) {
        return detail.has(member) && !detail.get(member).isJsonNull()
                ? detail.get(member).getAsString() : "";
    }

    private void setText(TextView tv, String text) {
        if (tv != null) {
            tv.setText(text);
        }
    }

    private String prettyStatus(String status) {
        if ("EM_ANALISE".equals(status)) {
            return "EM ANÁLISE";
        }
        if ("RESOLVIDO".equals(status)) {
            return "RESOLVIDO";
        }
        if ("NOVO".equals(status)) {
            return "NOVO";
        }
        return status;
    }
}
