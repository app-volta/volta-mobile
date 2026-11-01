package com.aula.volta.ui.occurrences;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.app.Dialog;
import com.aula.volta.R;
import com.aula.volta.util.BottomSheetImmersiveHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;

public class OccurrencesFilterSheet extends BottomSheetDialogFragment {

    public static final String REQUEST_KEY = "occurrences_filter";
    public static final String ARG_MATERIALS = "materials";
    public static final String ARG_STATUSES = "statuses";
    public static final String ARG_PRIORITIES = "priorities";

    private ArrayList<String> materials = new ArrayList<>();
    private ArrayList<String> statuses = new ArrayList<>();
    private ArrayList<String> priorities = new ArrayList<>();

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        BottomSheetImmersiveHelper.configureBottomSheet(dialog);
        return dialog;
    }

    public OccurrencesFilterSheet() {
    }

    public static OccurrencesFilterSheet newInstance(ArrayList<String> materials,
                                                     ArrayList<String> statuses,
                                                     ArrayList<String> priorities) {
        OccurrencesFilterSheet sheet = new OccurrencesFilterSheet();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_MATERIALS, materials);
        args.putStringArrayList(ARG_STATUSES, statuses);
        args.putStringArrayList(ARG_PRIORITIES, priorities);
        sheet.setArguments(args);
        return sheet;
    }

    public static OccurrencesFilterSheet newInstance(ArrayList<String> materials,
                                                     ArrayList<String> statuses) {
        return newInstance(materials, statuses, new ArrayList<>());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            materials = getArguments().getStringArrayList(ARG_MATERIALS);
            statuses = getArguments().getStringArrayList(ARG_STATUSES);
            priorities = getArguments().getStringArrayList(ARG_PRIORITIES);
            if (materials == null) {
                materials = new ArrayList<>();
            }
            if (statuses == null) {
                statuses = new ArrayList<>();
            }
            if (priorities == null) {
                priorities = new ArrayList<>();
            }
        }

        Chip papelao = view.findViewById(R.id.chipMatPapelao);
        Chip plastico = view.findViewById(R.id.chipMatPlastico);
        Chip metal = view.findViewById(R.id.chipMatMetal);
        Chip vidro = view.findViewById(R.id.chipMatVidro);
        Chip madeira = view.findViewById(R.id.chipMatMadeira);
        Chip organico = view.findViewById(R.id.chipMatOrganico);
        Chip outro = view.findViewById(R.id.chipMatOutro);

        Chip prioAlta = view.findViewById(R.id.chipPrioAlta);
        Chip prioMedia = view.findViewById(R.id.chipPrioMedia);
        Chip prioBaixa = view.findViewById(R.id.chipPrioBaixa);

        Chip aberta = view.findViewById(R.id.chipStatusAberta);
        Chip aprovada = view.findViewById(R.id.chipStatusAprovada);

        if (papelao != null) setupMaterialChip(papelao, "papel", "#C68A10", "#FDF3DE", materials);
        if (plastico != null) setupMaterialChip(plastico, "plast", "#3B82F6", "#E4EFFF", materials);
        if (metal != null) setupMaterialChip(metal, "metal", "#8B6FE0", "#EFEAFB", materials);
        if (vidro != null) setupMaterialChip(vidro, "vidro", "#6B7280", "#F1F2F6", materials);
        if (madeira != null) setupMaterialChip(madeira, "madeira", "#B45309", "#FEF3C7", materials);
        if (organico != null) setupMaterialChip(organico, "organ", "#12A05E", "#E6F7EF", materials);
        if (outro != null) setupMaterialChip(outro, "outro", "#475569", "#F1F5F9", materials);

        if (prioAlta != null) setupPriorityChip(prioAlta, "ALTA", "#DC2626", "#FEE2E2", priorities);
        if (prioMedia != null) setupPriorityChip(prioMedia, "MÉDIA", "#D97706", "#FEF3C7", priorities);
        if (prioBaixa != null) setupPriorityChip(prioBaixa, "BAIXA", "#2563EB", "#DBEAFE", priorities);

        if (aberta != null) setupStatusChip(aberta, "ABERTA", statuses);
        if (aprovada != null) setupStatusChip(aprovada, "APROVADA", statuses);

        TextView tvClear = view.findViewById(R.id.tvClearFilter);
        tvClear.setOnClickListener(v -> {
            materials.clear();
            statuses.clear();
            priorities.clear();
            if (papelao != null) papelao.setChecked(false);
            if (plastico != null) plastico.setChecked(false);
            if (metal != null) metal.setChecked(false);
            if (vidro != null) vidro.setChecked(false);
            if (madeira != null) madeira.setChecked(false);
            if (organico != null) organico.setChecked(false);
            if (outro != null) outro.setChecked(false);
            if (prioAlta != null) prioAlta.setChecked(false);
            if (prioMedia != null) prioMedia.setChecked(false);
            if (prioBaixa != null) prioBaixa.setChecked(false);
            if (aberta != null) aberta.setChecked(false);
            if (aprovada != null) aprovada.setChecked(false);
        });

        MaterialButton btnApply = view.findViewById(R.id.btnApplyFilter);
        btnApply.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putStringArrayList(ARG_MATERIALS, materials);
            result.putStringArrayList(ARG_STATUSES, statuses);
            result.putStringArrayList(ARG_PRIORITIES, priorities);
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
            dismiss();
        });
    }

    private void setupPriorityChip(Chip chip, String value, String tintHex, String bgHex,
                                   ArrayList<String> selected) {
        chip.setChecked(selected.contains(value));
        paintMaterialChip(chip, chip.isChecked(), tintHex, bgHex);
        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !selected.contains(value)) {
                selected.add(value);
            } else if (!isChecked) {
                selected.remove(value);
            }
            paintMaterialChip(chip, isChecked, tintHex, bgHex);
        });
    }

    private void setupMaterialChip(Chip chip, String value, String tintHex, String bgHex,
                                   ArrayList<String> selected) {
        chip.setChecked(selected.contains(value));
        paintMaterialChip(chip, chip.isChecked(), tintHex, bgHex);
        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !selected.contains(value)) {
                selected.add(value);
            } else if (!isChecked) {
                selected.remove(value);
            }
            paintMaterialChip(chip, isChecked, tintHex, bgHex);
        });
    }

    private void paintMaterialChip(Chip chip, boolean checked, String tintHex, String bgHex) {
        int tint = Color.parseColor(tintHex);
        int bg = Color.parseColor(bgHex);
        int white = Color.parseColor("#FFFFFF");
        int gray = Color.parseColor("#9CA3AF");
        chip.setChipBackgroundColor(new ColorStateList(
                new int[][]{new int[]{}},
                new int[]{checked ? bg : white}));
        chip.setChipStrokeColor(new ColorStateList(
                new int[][]{new int[]{}},
                new int[]{checked ? bg : Color.parseColor("#E5E7EB")}));
        chip.setTextColor(checked ? tint : gray);
    }

    private void setupStatusChip(Chip chip, String value, ArrayList<String> selected) {
        chip.setChecked(selected.contains(value));
        paintStatusChip(chip, chip.isChecked());
        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !selected.contains(value)) {
                selected.add(value);
            } else if (!isChecked) {
                selected.remove(value);
            }
            paintStatusChip(chip, isChecked);
        });
    }

    private void paintStatusChip(Chip chip, boolean checked) {
        int green = chip.getContext().getColor(R.color.nav_item_selected);
        int white = Color.parseColor("#FFFFFF");
        int gray = Color.parseColor("#9CA3AF");
        chip.setChipBackgroundColor(new ColorStateList(
                new int[][]{new int[]{}},
                new int[]{checked ? green : white}));
        chip.setChipStrokeColor(new ColorStateList(
                new int[][]{new int[]{}},
                new int[]{checked ? green : Color.parseColor("#E5E7EB")}));
        chip.setTextColor(checked ? white : gray);
    }
}
