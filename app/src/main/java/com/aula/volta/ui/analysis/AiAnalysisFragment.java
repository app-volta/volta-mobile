package com.aula.volta.ui.analysis;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aula.volta.R;
import com.aula.volta.data.api.AiAnalysisAPI;
import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.OccurrenceAPI;
import com.aula.volta.data.local.AuditLog;
import com.aula.volta.data.model.AiAnalysisJSON;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Análise da IA — Passo 2 de 2 (Fase 4, Figma 626:327).
 * IA copiloto: sugere via contrato AiAnalysisJSON; humano confirma ou edita.
 * Args: photoPath, descricao, setor, avisar.
 */
public class AiAnalysisFragment extends Fragment {

    private LinearLayout aiLoadingGroup;
    private LinearLayout aiResultGroup;
    private AiAnalysisJSON analysis;

    private String photoPath;
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
        setor = args != null ? args.getString("setor") : null;

        aiLoadingGroup = view.findViewById(R.id.aiLoadingGroup);
        aiResultGroup = view.findViewById(R.id.aiResultGroup);

        ImageView imgAiPhoto = view.findViewById(R.id.imgAiPhoto);
        if (photoPath != null) {
            imgAiPhoto.setImageBitmap(BitmapFactory.decodeFile(photoPath));
        }

        View btnBack = view.findViewById(R.id.btnBackAnalysis);
        btnBack.setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        MaterialButton btnEdit = view.findViewById(R.id.btnEditAnalysis);
        btnEdit.setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirmAnalysis);
        btnConfirm.setOnClickListener(v -> confirmOccurrence(v));

        analyzePhoto();
    }

    /** Chama POST /occurrences/{id}/analysis (mock com delay → loading real). */
    private void analyzePhoto() {
        AiAnalysisAPI api = ApiClient.get(requireContext()).create(AiAnalysisAPI.class);
        JsonObject body = new JsonObject();
        body.addProperty("setor", setor == null ? "" : setor);
        api.analyzeOccurrence("novo", body).enqueue(new Callback<AiAnalysisJSON>() {
            @Override
            public void onResponse(Call<AiAnalysisJSON> call,
                                   Response<AiAnalysisJSON> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    analysis = response.body();
                    applyAnalysis(requireView());
                } else {
                    Toast.makeText(requireContext(), R.string.error_load,
                            Toast.LENGTH_LONG).show();
                    Navigation.findNavController(requireView()).navigateUp();
                }
            }

            @Override
            public void onFailure(Call<AiAnalysisJSON> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(), R.string.offline_cache,
                        Toast.LENGTH_LONG).show();
                Navigation.findNavController(requireView()).navigateUp();
            }
        });
    }

    private void applyAnalysis(View view) {
        aiLoadingGroup.setVisibility(View.GONE);
        aiResultGroup.setVisibility(View.VISIBLE);

        setText(view, R.id.tvAiMaterial, analysis.getMaterial());

        String contaminacao = analysis.getContaminacao() != null
                ? analysis.getContaminacao().getNivel() : "-";
        setText(view, R.id.tvAiContamination, contaminacao);

        String quantidade = "≈" + trimNumber(analysis.getQuantidadeEstimada())
                + " " + analysis.getUnidade();
        setText(view, R.id.tvAiQuantity, quantidade);

        if (analysis.getUnidades() != null) {
            setText(view, R.id.tvAiUnitsLabel, analysis.getUnidades().getTipo());
            setText(view, R.id.tvAiUnits,
                    analysis.getUnidades().getQuantidade() + " un.");
        } else {
            setText(view, R.id.tvAiUnitsLabel, "");
            setText(view, R.id.tvAiUnits, "-");
        }

        int percent = (int) Math.round(analysis.getConfianca() * 100);
        setText(view, R.id.tvAiConfidence, percent + "%");
        View bar = view.findViewById(R.id.viewAiConfidence);
        LinearLayout.LayoutParams params =
                (LinearLayout.LayoutParams) bar.getLayoutParams();
        params.weight = Math.max(percent, 2);
        bar.setLayoutParams(params);

        setText(view, R.id.tvAiRecommendation,
                analysis.getObservacoes() == null ? "" : analysis.getObservacoes());
    }

    private void setText(View root, int id, String text) {
        TextView tv = root.findViewById(id);
        if (tv != null) {
            tv.setText(text);
        }
    }

    private String trimNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((int) value);
        }
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f", value);
    }

    /** Confirmar: cria a ocorrência (mock), audita, avisa e volta à Home. */
    private void confirmOccurrence(View v) {
        OccurrenceAPI api = ApiClient.get(requireContext()).create(OccurrenceAPI.class);
        JsonObject body = new JsonObject();
        body.addProperty("setor", setor == null ? "" : setor);
        if (analysis != null) {
            body.addProperty("material", analysis.getMaterial());
            body.addProperty("quantidade_estimada", analysis.getQuantidadeEstimada());
        }
        api.createOccurrence(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!isAdded()) {
                    return;
                }
                String id = response.body() != null && response.body().has("id")
                        ? response.body().get("id").getAsString() : "novo";
                AuditLog.append(requireContext(), "Breno Gomes", "CRIAR",
                        "ocorrencia", id, null, "NOVO");
                Toast.makeText(requireContext(), R.string.ai_registered,
                        Toast.LENGTH_LONG).show();
                Navigation.findNavController(v).popBackStack(R.id.nav_home, false);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(), R.string.offline_cache,
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
