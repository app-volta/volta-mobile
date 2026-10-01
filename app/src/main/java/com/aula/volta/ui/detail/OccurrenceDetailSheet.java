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

import com.aula.volta.R;
import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.OccurrenceAPI;
import com.aula.volta.data.local.PrefsHelper;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Bottom sheet de detalhe da ocorrência — Figma (554:27).
 * GET /occurrences/{id} (mock → API-ready), cache por id, offline via prefs.
 * Ações do responsável técnico (validar, observações, destinação) chegam na Fase 6b.
 */
public class OccurrenceDetailSheet extends BottomSheetDialogFragment {

    private static final String PREFS = "cache_occurrence_detail";

    private TextView tvTitle;
    private TextView tvSubtitle;
    private TextView tvStatus;
    private TextView tvWeight;
    private TextView tvAuthor;
    private TextView tvDate;
    private TextView tvConfidence;

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
        btnReport.setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.detail_report_soon,
                        Toast.LENGTH_SHORT).show());

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
        OccurrenceAPI api = ApiClient.get(requireContext()).create(OccurrenceAPI.class);
        api.getOccurrence(occurrenceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject detail = response.body();
                    PrefsHelper.putJson(requireContext(), PREFS, occurrenceId,
                            detail.toString());
                    applyDetail(detail);
                } else {
                    loadFromCache(occurrenceId);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                loadFromCache(occurrenceId);
                Toast.makeText(requireContext(), R.string.offline_cache,
                        Toast.LENGTH_LONG).show();
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
        setText(tvDate, opt(detail, "data"));
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
