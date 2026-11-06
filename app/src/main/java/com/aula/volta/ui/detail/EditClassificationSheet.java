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
    private String selectedClass = "";
    private String selectedPriority = "Baixa";
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

        TextView chipPapelao = view.findViewById(R.id.chipDialogPapelao);
        TextView chipPlastico = view.findViewById(R.id.chipDialogPlastico);
        TextView chipMetal = view.findViewById(R.id.chipDialogMetal);
        TextView chipVidro = view.findViewById(R.id.chipDialogVidro);
        TextView chipMadeira = view.findViewById(R.id.chipDialogMadeira);
        TextView chipOrganico = view.findViewById(R.id.chipDialogOrganico);
        TextView chipOutro = view.findViewById(R.id.chipDialogOutro);

        TextView[] chips = {chipPapelao, chipPlastico, chipMetal, chipVidro, chipMadeira, chipOrganico, chipOutro};

        Runnable updateMaterialUi = () -> {
            for (TextView c : chips) {
                if (c == null) continue;
                c.setBackgroundResource(R.drawable.bg_chip_white_stroke);
                c.setTextColor(requireContext().getColor(R.color.vf_text_secondary));
                String text = c.getText().toString().replace("✓ ", "");
                c.setText(text);
            }

            String lower = selectedMaterial != null ? selectedMaterial.toLowerCase(java.util.Locale.ROOT) : "";
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
            } else if (lower.contains("madeira") && chipMadeira != null) {
                chipMadeira.setBackgroundResource(R.drawable.bg_chip_amber_selected);
                chipMadeira.setTextColor(Color.parseColor("#B45309"));
                chipMadeira.setText("✓ " + chipMadeira.getText().toString().replace("✓ ", ""));
            } else if (lower.contains("org") && chipOrganico != null) {
                chipOrganico.setBackgroundResource(R.drawable.bg_chip_green_selected);
                chipOrganico.setTextColor(Color.parseColor("#12A05E"));
                chipOrganico.setText("✓ " + chipOrganico.getText().toString().replace("✓ ", ""));
            } else if (chipOutro != null) {
                chipOutro.setBackgroundResource(R.drawable.bg_chip_gray_selected);
                chipOutro.setTextColor(Color.parseColor("#475569"));
                chipOutro.setText("✓ " + chipOutro.getText().toString().replace("✓ ", ""));
            }
        };

        for (TextView chip : chips) {
            if (chip == null) continue;
            chip.setOnClickListener(v -> {
                String clean = chip.getText().toString().replace("✓ ", "").trim();
                selectedMaterial = clean;
                updateMaterialUi.run();
            });
        }

        updateMaterialUi.run();

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

        SwitchMaterial switchHazard = view.findViewById(R.id.switchHazard);
        if (switchHazard != null) {
            switchHazard.setChecked(isHazardous);
            switchHazard.setOnCheckedChangeListener((btn, isChecked) -> isHazardous = isChecked);
        }

        EditText etEditReason = view.findViewById(R.id.etEditReason);
        if (etEditReason != null && reasonText != null && !reasonText.isEmpty()) {
            etEditReason.setText(reasonText);
        }

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
