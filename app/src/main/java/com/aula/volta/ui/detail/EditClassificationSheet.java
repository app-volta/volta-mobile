package com.aula.volta.ui.detail;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aula.volta.R;
import com.aula.volta.util.BottomSheetImmersiveHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * BottomSheetDialogFragment de alteração de classificação — Figma 17a (908:5946).
 * Ancorado na base da tela, com seleção dinâmica de material, classe e prioridade.
 */
public class EditClassificationSheet extends BottomSheetDialogFragment {

    public interface OnClassificationChangedListener {
        void onClassificationChanged(String material, String qualityClass, String priority,
                                     boolean isHazard, String reason);
    }

    private static final String ARG_MATERIAL = "arg_material";
    private static final String ARG_CLASS = "arg_class";
    private static final String ARG_PRIORITY = "arg_priority";
    private static final String ARG_HAZARD = "arg_hazard";
    private static final String ARG_REASON = "arg_reason";

    private String selectedMaterial = "Papelão";
    private String selectedClass = "B";
    private String selectedPriority = "Alta";
    private boolean isHazardous = false;
    private String reasonText = "";

    private OnClassificationChangedListener listener;

    public static EditClassificationSheet newInstance(String material, String qualityClass,
                                                     String priority, boolean isHazard, String reason) {
        EditClassificationSheet sheet = new EditClassificationSheet();
        Bundle args = new Bundle();
        args.putString(ARG_MATERIAL, material);
        args.putString(ARG_CLASS, qualityClass);
        args.putString(ARG_PRIORITY, priority);
        args.putBoolean(ARG_HAZARD, isHazard);
        args.putString(ARG_REASON, reason);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnClassificationChangedListener(OnClassificationChangedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        BottomSheetImmersiveHelper.configureBottomSheet(dialog);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_edit_classification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            String mat = getArguments().getString(ARG_MATERIAL);
            if (mat != null && !mat.isEmpty()) {
                selectedMaterial = mat;
            }
            String cls = getArguments().getString(ARG_CLASS);
            if (cls != null && !cls.isEmpty()) {
                selectedClass = cls;
            }
            String prio = getArguments().getString(ARG_PRIORITY);
            if (prio != null && !prio.isEmpty()) {
                selectedPriority = prio;
            }
            isHazardous = getArguments().getBoolean(ARG_HAZARD, false);
            reasonText = getArguments().getString(ARG_REASON, "");
        }

        // Material chips
        TextView chipPapelao = view.findViewById(R.id.chipDialogPapelao);
        TextView chipPlastico = view.findViewById(R.id.chipDialogPlastico);
        TextView chipMetal = view.findViewById(R.id.chipDialogMetal);
        TextView chipVidro = view.findViewById(R.id.chipDialogVidro);

        Runnable updateMaterialUi = () -> {
            boolean isPapelao = selectedMaterial.toLowerCase().contains("papel");
            boolean isPlastico = selectedMaterial.toLowerCase().contains("plást") || selectedMaterial.toLowerCase().contains("plast");
            boolean isMetal = selectedMaterial.toLowerCase().contains("metal");
            boolean isVidro = selectedMaterial.toLowerCase().contains("vidro");

            if (!isPapelao && !isPlastico && !isMetal && !isVidro) {
                isPapelao = true; // default
            }

            if (chipPapelao != null) {
                chipPapelao.setBackgroundResource(isPapelao ? R.drawable.bg_chip_gold_selected : R.drawable.bg_chip_white_stroke);
                chipPapelao.setTextColor(isPapelao ? Color.parseColor("#C68A10") : requireContext().getColor(R.color.vf_text_secondary));
                chipPapelao.setText(isPapelao ? "✓ Papelão" : "Papelão");
            }
            if (chipPlastico != null) {
                chipPlastico.setBackgroundResource(isPlastico ? R.drawable.bg_chip_blue_selected : R.drawable.bg_chip_white_stroke);
                chipPlastico.setTextColor(isPlastico ? Color.parseColor("#3B82F6") : requireContext().getColor(R.color.vf_text_secondary));
                chipPlastico.setText(isPlastico ? "✓ Plástico" : "Plástico");
            }
            if (chipMetal != null) {
                chipMetal.setBackgroundResource(isMetal ? R.drawable.bg_chip_purple_selected : R.drawable.bg_chip_white_stroke);
                chipMetal.setTextColor(isMetal ? Color.parseColor("#8B6FE0") : requireContext().getColor(R.color.vf_text_secondary));
                chipMetal.setText(isMetal ? "✓ Metal" : "Metal");
            }
            if (chipVidro != null) {
                chipVidro.setBackgroundResource(isVidro ? R.drawable.bg_chip_gray_selected : R.drawable.bg_chip_white_stroke);
                chipVidro.setTextColor(isVidro ? Color.parseColor("#6B7280") : requireContext().getColor(R.color.vf_text_secondary));
                chipVidro.setText(isVidro ? "✓ Vidro" : "Vidro");
            }
        };

        if (chipPapelao != null) chipPapelao.setOnClickListener(v -> { selectedMaterial = "Papelão"; updateMaterialUi.run(); });
        if (chipPlastico != null) chipPlastico.setOnClickListener(v -> { selectedMaterial = "Plástico"; updateMaterialUi.run(); });
        if (chipMetal != null) chipMetal.setOnClickListener(v -> { selectedMaterial = "Metal"; updateMaterialUi.run(); });
        if (chipVidro != null) chipVidro.setOnClickListener(v -> { selectedMaterial = "Vidro"; updateMaterialUi.run(); });

        updateMaterialUi.run();

        // Class columns
        LinearLayout colA = view.findViewById(R.id.colClassA);
        LinearLayout colB = view.findViewById(R.id.colClassB);
        LinearLayout colC = view.findViewById(R.id.colClassC);
        TextView tvATitle = view.findViewById(R.id.tvClassATitle);
        TextView tvASub = view.findViewById(R.id.tvClassASub);
        TextView tvBTitle = view.findViewById(R.id.tvClassBTitle);
        TextView tvBSub = view.findViewById(R.id.tvClassBSub);
        TextView tvCTitle = view.findViewById(R.id.tvClassCTitle);
        TextView tvCSub = view.findViewById(R.id.tvClassCSub);

        Runnable updateClassUi = () -> {
            boolean isA = "A".equalsIgnoreCase(selectedClass);
            boolean isB = "B".equalsIgnoreCase(selectedClass);
            boolean isC = "C".equalsIgnoreCase(selectedClass);

            if (colA != null) {
                colA.setBackgroundResource(isA ? R.drawable.bg_chip_gold_selected : 0);
                if (tvATitle != null) tvATitle.setTextColor(isA ? Color.parseColor("#C68A10") : 0xFF0F172A);
                if (tvASub != null) tvASub.setTextColor(isA ? Color.parseColor("#C68A10") : 0xFF64748B);
            }
            if (colB != null) {
                colB.setBackgroundResource(isB ? R.drawable.bg_chip_gold_selected : 0);
                if (tvBTitle != null) tvBTitle.setTextColor(isB ? Color.parseColor("#C68A10") : 0xFF0F172A);
                if (tvBSub != null) tvBSub.setTextColor(isB ? Color.parseColor("#C68A10") : 0xFF64748B);
            }
            if (colC != null) {
                colC.setBackgroundResource(isC ? R.drawable.bg_chip_gold_selected : 0);
                if (tvCTitle != null) tvCTitle.setTextColor(isC ? Color.parseColor("#C68A10") : 0xFF0F172A);
                if (tvCSub != null) tvCSub.setTextColor(isC ? Color.parseColor("#C68A10") : 0xFF64748B);
            }
        };

        if (colA != null) colA.setOnClickListener(v -> { selectedClass = "A"; updateClassUi.run(); });
        if (colB != null) colB.setOnClickListener(v -> { selectedClass = "B"; updateClassUi.run(); });
        if (colC != null) colC.setOnClickListener(v -> { selectedClass = "C"; updateClassUi.run(); });

        updateClassUi.run();

        // Priority segments
        TextView segAlta = view.findViewById(R.id.segPrioAlta);
        TextView segMedia = view.findViewById(R.id.segPrioMedia);
        TextView segBaixa = view.findViewById(R.id.segPrioBaixa);

        Runnable updatePriorityUi = () -> {
            boolean isAlta = "Alta".equalsIgnoreCase(selectedPriority);
            boolean isMedia = "Média".equalsIgnoreCase(selectedPriority) || "Media".equalsIgnoreCase(selectedPriority);
            boolean isBaixa = "Baixa".equalsIgnoreCase(selectedPriority);

            if (segAlta != null) {
                segAlta.setBackgroundResource(isAlta ? R.drawable.bg_segment_pink_selected : 0);
                segAlta.setTextColor(isAlta ? Color.parseColor("#E8556C") : requireContext().getColor(R.color.vf_text_secondary));
            }
            if (segMedia != null) {
                segMedia.setBackgroundResource(isMedia ? R.drawable.bg_segment_gold_selected : 0);
                segMedia.setTextColor(isMedia ? Color.parseColor("#D97706") : requireContext().getColor(R.color.vf_text_secondary));
            }
            if (segBaixa != null) {
                segBaixa.setBackgroundResource(isBaixa ? R.drawable.bg_segment_green_selected : 0);
                segBaixa.setTextColor(isBaixa ? Color.parseColor("#12A05E") : requireContext().getColor(R.color.vf_text_secondary));
            }
        };

        if (segAlta != null) segAlta.setOnClickListener(v -> { selectedPriority = "Alta"; updatePriorityUi.run(); });
        if (segMedia != null) segMedia.setOnClickListener(v -> { selectedPriority = "Média"; updatePriorityUi.run(); });
        if (segBaixa != null) segBaixa.setOnClickListener(v -> { selectedPriority = "Baixa"; updatePriorityUi.run(); });

        updatePriorityUi.run();

        // Hazardous Switch
        SwitchMaterial switchHazard = view.findViewById(R.id.switchHazard);
        if (switchHazard != null) {
            switchHazard.setChecked(isHazardous);
            switchHazard.setOnCheckedChangeListener((btn, isChecked) -> isHazardous = isChecked);
        }

        // Edit Reason
        EditText etEditReason = view.findViewById(R.id.etEditReason);
        if (etEditReason != null && reasonText != null && !reasonText.isEmpty()) {
            etEditReason.setText(reasonText);
        }

        // Close & Confirm
        View btnClose = view.findViewById(R.id.btnCloseSheet);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        View btnConfirm = view.findViewById(R.id.btnConfirmEditClass);
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String reason = etEditReason != null ? etEditReason.getText().toString().trim() : "";
                if (listener != null) {
                    listener.onClassificationChanged(selectedMaterial, selectedClass,
                            selectedPriority, isHazardous, reason);
                }
                dismiss();
            });
        }
    }
}
