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

import com.aula.volta.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;

/**
 * Bottom sheet Filtrar ocorrências — Figma (554:24).
 * Chips multi-seleção; devolve ArrayLists via FragmentResult ("filter").
 * Depois vira query param da API; hoje filtra o adapter localmente.
 */
public class OccurrencesFilterSheet extends BottomSheetDialogFragment {

    public static final String REQUEST_KEY = "occurrences_filter";
    public static final String ARG_MATERIALS = "materials";
    public static final String ARG_STATUSES = "statuses";

    private ArrayList<String> materials = new ArrayList<>();
    private ArrayList<String> statuses = new ArrayList<>();

    public OccurrencesFilterSheet() {
    }

    /** Pré-seleciona os filtros atuais da lista. */
    public static OccurrencesFilterSheet newInstance(ArrayList<String> materials,
                                                     ArrayList<String> statuses) {
        OccurrencesFilterSheet sheet = new OccurrencesFilterSheet();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_MATERIALS, materials);
        args.putStringArrayList(ARG_STATUSES, statuses);
        sheet.setArguments(args);
        return sheet;
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
            if (materials == null) {
                materials = new ArrayList<>();
            }
            if (statuses == null) {
                statuses = new ArrayList<>();
            }
        }

        Chip papelao = view.findViewById(R.id.chipMatPapelao);
        Chip plastico = view.findViewById(R.id.chipMatPlastico);
        Chip metal = view.findViewById(R.id.chipMatMetal);
        Chip vidro = view.findViewById(R.id.chipMatVidro);
        Chip aberta = view.findViewById(R.id.chipStatusAberta);
        Chip analise = view.findViewById(R.id.chipStatusAnalise);
        Chip coletada = view.findViewById(R.id.chipStatusColetada);

        setupMaterialChip(papelao, "papel", "#C68A10", "#FDF3DE", materials);
        setupMaterialChip(plastico, "plast", "#3B82F6", "#E4EFFF", materials);
        setupMaterialChip(metal, "metal", "#8B6FE0", "#EFEAFB", materials);
        setupMaterialChip(vidro, "vidro", "#6B7280", "#F1F2F6", materials);

        setupStatusChip(aberta, "NOVO", statuses);
        setupStatusChip(analise, "EM_ANALISE", statuses);
        setupStatusChip(coletada, "RESOLVIDO", statuses);

        TextView tvClear = view.findViewById(R.id.tvClearFilter);
        tvClear.setOnClickListener(v -> {
            materials.clear();
            statuses.clear();
            papelao.setChecked(false);
            plastico.setChecked(false);
            metal.setChecked(false);
            vidro.setChecked(false);
            aberta.setChecked(false);
            analise.setChecked(false);
            coletada.setChecked(false);
        });

        MaterialButton btnApply = view.findViewById(R.id.btnApplyFilter);
        btnApply.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putStringArrayList(ARG_MATERIALS, materials);
            result.putStringArrayList(ARG_STATUSES, statuses);
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
            dismiss();
        });
    }

    /** Chip de material: selecionado = fundo/tinta da família (Figma); solto = branco/cinza. */
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

    /** Chip de status: selecionado = verde sólido + texto branco (Figma). */
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
