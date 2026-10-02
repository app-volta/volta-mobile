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
import com.aula.volta.data.local.NotificationStore;
import com.aula.volta.data.local.OccurrenceStore;
import com.aula.volta.data.model.AiAnalysisJSON;
import com.aula.volta.data.model.Notification;
import com.aula.volta.data.model.Occurrence;
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
    private android.animation.ObjectAnimator scanAnimator;

    private String photoPath;
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
        descricao = args != null && args.getString("descricao") != null
                ? args.getString("descricao").trim() : "";
        setor = args != null ? args.getString("setor") : null;

        aiLoadingGroup = view.findViewById(R.id.aiLoadingGroup);
        aiResultGroup = view.findViewById(R.id.aiResultGroup);

        ImageView imgAiPhoto = view.findViewById(R.id.imgAiPhoto);
        if (photoPath != null) {
            imgAiPhoto.setImageBitmap(BitmapFactory.decodeFile(photoPath));
        }

        ImageView imgAiPhotoScan = view.findViewById(R.id.imgAiPhotoScan);
        if (imgAiPhotoScan != null && photoPath != null) {
            imgAiPhotoScan.setImageBitmap(BitmapFactory.decodeFile(photoPath));
        }

        View btnBack = view.findViewById(R.id.btnBackAnalysis);
        btnBack.setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        MaterialButton btnEdit = view.findViewById(R.id.btnEditAnalysis);
        btnEdit.setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirmAnalysis);
        btnConfirm.setOnClickListener(v -> confirmOccurrence(v));

        View btnAskVolta = view.findViewById(R.id.btnAskVolta);
        if (btnAskVolta != null) {
            btnAskVolta.setOnClickListener(v ->
                    Navigation.findNavController(v).navigate(R.id.nav_assistant));
        }

        startScanAnimation(view);
        analyzePhoto();
    }

    @Override
    public void onDestroyView() {
        stopScanAnimation();
        super.onDestroyView();
    }

    /** Laser verde varrendo a foto em loop (protótipo novo 851:3975). */
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
            scanAnimator = android.animation.ObjectAnimator.ofFloat(
                    band, "translationY", -band.getHeight(), frame.getHeight());
            scanAnimator.setDuration(1600);
            scanAnimator.setRepeatCount(android.animation.ObjectAnimator.INFINITE);
            scanAnimator.start();
        });
    }

    private void stopScanAnimation() {
        if (scanAnimator != null) {
            scanAnimator.cancel();
            scanAnimator = null;
        }
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
                    useOfflineAnalysisFallback();
                }
            }

            @Override
            public void onFailure(Call<AiAnalysisJSON> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                useOfflineAnalysisFallback();
            }
        });
    }

    private void useOfflineAnalysisFallback() {
        Toast.makeText(requireContext(), R.string.offline_cache, Toast.LENGTH_SHORT).show();
        analysis = com.aula.volta.data.model.AiAnalysisJSON.createOfflineFallback(setor);
        applyAnalysis(requireView());
    }

    private void applyAnalysis(View view) {
        stopScanAnimation();
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

    /** Confirmar: cria a ocorrência (mock/API), audita, avisa e volta à Home com suporte offline. */
    private void confirmOccurrence(View v) {
        String material = analysis != null && analysis.getMaterial() != null ? analysis.getMaterial() : "Resíduo Industrial";
        double qtd = analysis != null ? analysis.getQuantidadeEstimada() : 0.0;
        String unidade = analysis != null && analysis.getUnidade() != null ? analysis.getUnidade() : "kg";

        // Se estiver explicitamente sem conexão, salva diretamente na fila offline
        if (!com.aula.volta.data.sync.SyncManager.isOnline(requireContext())) {
            saveOfflineAndFinish(v, material, qtd, unidade);
            return;
        }

        OccurrenceAPI api = ApiClient.get(requireContext()).create(OccurrenceAPI.class);
        JsonObject body = new JsonObject();
        body.addProperty("setor", setor == null ? "" : setor);
        body.addProperty("material", material);
        body.addProperty("quantidade_estimada", qtd);

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
                pushOccurrenceNotification(id);
                pushOccurrenceToList(id);
                Toast.makeText(requireContext(), R.string.ai_registered,
                        Toast.LENGTH_LONG).show();
                Navigation.findNavController(v).popBackStack(R.id.nav_home, false);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                // Resiliência de fábrica: falha de rede nunca perde a ocorrência
                saveOfflineAndFinish(v, material, qtd, unidade);
            }
        });
    }

    private void saveOfflineAndFinish(View v, String material, double qtd, String unidade) {
        String localId = "off_" + System.currentTimeMillis();
        com.aula.volta.data.sync.PendingOccurrence pending = new com.aula.volta.data.sync.PendingOccurrence(
                localId, photoPath, descricao, setor, material, qtd, unidade, System.currentTimeMillis());
        com.aula.volta.data.sync.SyncQueue.enqueue(requireContext(), pending);

        AuditLog.append(requireContext(), "Breno Gomes", "CRIAR_OFFLINE",
                "ocorrencia", localId, null, "PENDENTE_SYNC");
        pushOccurrenceNotification(localId);
        pushOccurrenceToList(localId);

        Toast.makeText(requireContext(), R.string.sync_saved_offline,
                Toast.LENGTH_LONG).show();
        Navigation.findNavController(v).popBackStack(R.id.nav_home, false);
    }

    /** Toda ocorrência lançada gera uma notificação local (topo, não lida). */
    private void pushOccurrenceNotification(String occurrenceId) {
        String material = analysis != null && analysis.getMaterial() != null
                ? analysis.getMaterial() : "";
        String desc;
        if (!material.isEmpty() && setor != null && !setor.isEmpty()) {
            desc = getString(R.string.notif_occurrence_desc, material, setor);
        } else if (!material.isEmpty()) {
            desc = material;
        } else if (setor != null && !setor.isEmpty()) {
            desc = setor;
        } else {
            desc = getString(R.string.notif_occurrence_desc_fallback);
        }
        Notification notification = new Notification(
                "local_" + System.currentTimeMillis(),
                "NOVA_OCORRENCIA",
                getString(R.string.notif_occurrence_title, occurrenceId),
                desc,
                getString(R.string.notif_now),
                "bell",
                "#C68A10",
                "#FDF3DE",
                false,
                occurrenceId);
        NotificationStore.pushLocal(requireContext(), notification);
    }

    /** Ocorrência nova entra no topo de Recentes + Ver todas (merge local do store). */
    private void pushOccurrenceToList(String occurrenceId) {
        String material = analysis != null && analysis.getMaterial() != null
                ? analysis.getMaterial() : "";
        String title = descricao != null ? descricao : "";
        if (title.isEmpty()) {
            title = material;
        }
        if (title.isEmpty() && setor != null) {
            title = setor;
        }
        OccurrenceStore.pushLocal(requireContext(), new Occurrence(
                occurrenceId,
                title,
                setor == null ? "" : setor,
                getString(R.string.notif_now),
                "MEDIA",
                "NOVO",
                material));
    }
}
