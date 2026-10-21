package com.aula.volta.ui.analysis;

import android.animation.ObjectAnimator;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
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
import com.aula.volta.data.sync.PendingOccurrence;
import com.aula.volta.data.sync.SyncManager;
import com.aula.volta.data.sync.SyncQueue;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;

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

    private AiAnalysisJSON analysis;
    private ObjectAnimator scanAnimator;

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
        }

        ImageView imgAiPhotoScan = view.findViewById(R.id.imgAiPhotoScan);
        if (photoPath != null && imgAiPhotoScan != null) {
            imgAiPhotoScan.setImageBitmap(BitmapFactory.decodeFile(photoPath));
        }

        View btnBack = view.findViewById(R.id.btnBackAnalysis);
        btnBack.setOnClickListener(v -> {
            if (aiEditGroup.getVisibility() == View.VISIBLE) {
                switchToResultView();
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

        startScanAnimation(view);
        analyzePhoto();
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
        AiAnalysisAPI api = ApiClient.get(requireContext()).create(AiAnalysisAPI.class);
        JsonObject body = new JsonObject();
        body.addProperty("setor", setor == null ? "" : setor);
        api.analyzeOccurrence("novo", body).enqueue(new Callback<AiAnalysisJSON>() {
            @Override
            public void onResponse(@NonNull Call<AiAnalysisJSON> call,
                                   @NonNull Response<AiAnalysisJSON> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    analysis = response.body();
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

        tvAiMaterial.setText(analysis.getMaterial() != null ? analysis.getMaterial() : "Papelão");
        String contaminacao = analysis.getContaminacao() != null ? analysis.getContaminacao().getNivel() : "Baixa";
        tvAiContamination.setText(contaminacao);

        if (isRevised) {
            tvAiQuantity.setText(currentWeight + " kg");
            badgeQuantityEdited.setVisibility(View.VISIBLE);
            cardAiQuantity.setBackgroundResource(R.drawable.bg_card_quantity_edited);
            tvAiStatusPill.setText("Revisado por você");
            tvMiddleStatusHint.setText("Resultados revisados · prontos pro relatório");
            tvMiddleStatusHint.setTextColor(requireContext().getColor(R.color.vf_green_deep));
            icMiddleStatus.setVisibility(View.VISIBLE);
        } else {
            String quantidade = "≈ " + trimNumber(analysis.getQuantidadeEstimada()) + " " + analysis.getUnidade();
            tvAiQuantity.setText(quantidade);
            badgeQuantityEdited.setVisibility(View.GONE);
            cardAiQuantity.setBackgroundResource(R.drawable.bg_card_stat_recycled);
            tvAiStatusPill.setText(getString(R.string.ai_done));
            tvMiddleStatusHint.setText("Confira os resultados antes de gerar o relatório");
            tvMiddleStatusHint.setTextColor(requireContext().getColor(R.color.vf_text_secondary));
            icMiddleStatus.setVisibility(View.GONE);
        }

        if (analysis.getUnidades() != null) {
            tvAiUnitsLabel.setText(analysis.getUnidades().getTipo());
            tvAiUnits.setText(currentUnits + " un.");
        } else {
            tvAiUnitsLabel.setText("Caixas");
            tvAiUnits.setText("11 un.");
        }

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

        // Botão Cancelar (16a -> volta para 16)
        MaterialButton btnCancel = root.findViewById(R.id.btnCancelCorrection);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> switchToResultView());
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
        if (getView() != null) {
            updateMaterialChipsUi(getView());
            updateClassPillsUi(getView());
            updateContaminationSegmentsUi(getView());
        }
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

        api.createOccurrence(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (!isAdded()) {
                    return;
                }
                String id = response.body() != null && response.body().has("id")
                        ? response.body().get("id").getAsString() : "8514";
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

    private void registerAndNavigate(View v, String id, String material, double qtd, String unidade) {
        if (!isAdded() || getContext() == null) {
            return;
        }
        AuditLog.append(requireContext(), "Breno Gomes", "CRIAR",
                "ocorrencia", id, null, "NOVO");
        pushOccurrenceNotification(id, material);
        pushOccurrenceToList(id, material);

        Toast.makeText(requireContext(), R.string.ai_registered, Toast.LENGTH_SHORT).show();

        // Navega para a tela 17 · Ocorrência (OccurrenceDetailFragment)
        try {
            Bundle args = new Bundle();
            args.putString("occurrenceId", id);
            args.putString("photoPath", photoPath);
            View target = getView() != null ? getView() : v;
            if (target != null) {
                Navigation.findNavController(target).navigate(R.id.nav_occurrence_full, args);
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
        String localId = "8514";
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
            View target = getView() != null ? getView() : v;
            if (target != null) {
                Navigation.findNavController(target).navigate(R.id.nav_occurrence_full, args);
            }
        } catch (Exception e) {
            if (getView() != null) {
                Navigation.findNavController(getView()).popBackStack(R.id.nav_home, false);
            }
        }
    }

    private void pushOccurrenceNotification(String occurrenceId, String material) {
        String desc = (setor != null && !setor.isEmpty())
                ? getString(R.string.notif_occurrence_desc, material, setor)
                : material;
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
