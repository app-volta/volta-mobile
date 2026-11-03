package com.aula.volta.ui.occurrence;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aula.volta.R;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.util.BottomSheetImmersiveHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SelectSectorSheet extends BottomSheetDialogFragment {

    public static class SectorOption {
        public final String id;
        public final String name;

        public SectorOption(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @FunctionalInterface
    public interface OnSectorSelectedListener {
        void onSectorSelected(int index, String sectorName, @Nullable String areaId);
    }

    private static final String ARG_CURRENT_INDEX = "arg_current_index";
    private int selectedIndex = 0;
    private OnSectorSelectedListener listener;

    public static SelectSectorSheet newInstance(int currentIndex) {
        SelectSectorSheet sheet = new SelectSectorSheet();
        Bundle args = new Bundle();
        args.putInt(ARG_CURRENT_INDEX, currentIndex);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnSectorSelectedListener(OnSectorSelectedListener listener) {
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
        return inflater.inflate(R.layout.dialog_select_sector, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            selectedIndex = getArguments().getInt(ARG_CURRENT_INDEX, 0);
        }

        final List<SectorOption> sectors = loadSectorOptions();
        RadioGroup rgSectors = view.findViewById(R.id.rgSectors);
        View btnClose = view.findViewById(R.id.btnSectorClose);
        View btnConfirm = view.findViewById(R.id.btnConfirmSector);

        final int[] tempIndex = {Math.max(0, Math.min(selectedIndex, sectors.size() - 1))};

        populateSectors(rgSectors, sectors, tempIndex);

        com.aula.volta.data.repository.AreaRepository.loadAreas(requireContext(), loaded -> {
            if (!isAdded() || getContext() == null || rgSectors == null) return;
            List<SectorOption> refreshed = loadSectorOptions();
            if (refreshed.size() != sectors.size() || !hasSameSectorNames(sectors, refreshed)) {
                sectors.clear();
                sectors.addAll(refreshed);
                tempIndex[0] = Math.max(0, Math.min(selectedIndex, sectors.size() - 1));
                populateSectors(rgSectors, sectors, tempIndex);
            }
        });

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                selectedIndex = tempIndex[0];
                if (listener != null && selectedIndex >= 0 && selectedIndex < sectors.size()) {
                    SectorOption chosen = sectors.get(selectedIndex);
                    listener.onSectorSelected(selectedIndex, chosen.name, chosen.id);
                }
                dismiss();
            });
        }
    }

    private List<SectorOption> loadSectorOptions() {
        List<SectorOption> options = new ArrayList<>();
        com.aula.volta.data.repository.AreaRepository.preloadAreas(requireContext());
        List<com.aula.volta.data.model.AreaResponseDTO> areas = com.aula.volta.data.repository.AreaRepository.getCachedAreas(requireContext());
        if (areas != null && !areas.isEmpty()) {
            for (com.aula.volta.data.model.AreaResponseDTO a : areas) {
                if (a.getSectorName() != null && !a.getSectorName().trim().isEmpty()) {
                    options.add(new SectorOption(a.getId(), a.getSectorName().trim()));
                }
            }
        }

        if (options.isEmpty()) {
            try {
                String sumStr = PrefsHelper.getJson(requireContext(), "cache_reports", "summary");
                if (sumStr != null && !sumStr.isEmpty()) {
                    JsonObject sum = JsonParser.parseString(sumStr).getAsJsonObject();
                    if (sum.has("areas") && sum.get("areas").isJsonArray()) {
                        for (JsonElement el : sum.getAsJsonArray("areas")) {
                            if (el.isJsonObject()) {
                                JsonObject obj = el.getAsJsonObject();
                                String id = obj.has("id") && !obj.get("id").isJsonNull() ? obj.get("id").getAsString() : null;
                                String name = obj.has("sectorName") && !obj.get("sectorName").isJsonNull() ? obj.get("sectorName").getAsString() : null;
                                if (id != null && name != null && !name.trim().isEmpty()) {
                                    options.add(new SectorOption(id, name.trim()));
                                    PrefsHelper.putJson(requireContext(), "cache_areas", id, obj.toString());
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (!options.isEmpty()) {
            options.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
            return options;
        }

        String[] fallbackNames = getResources().getStringArray(R.array.occurrence_sector_options);
        for (String fallbackName : fallbackNames) {
            options.add(new SectorOption(null, fallbackName));
        }
        return options;
    }

    private void populateSectors(RadioGroup rgSectors, List<SectorOption> sectors, int[] tempIndex) {
        if (rgSectors == null) return;
        rgSectors.removeAllViews();
        for (int i = 0; i < sectors.size(); i++) {
            final int index = i;
            View itemView = getLayoutInflater().inflate(R.layout.item_sector_option, rgSectors, false);
            TextView tvName = itemView.findViewById(R.id.tvSectorOptionName);
            RadioButton rb = itemView.findViewById(R.id.rbSectorOption);

            tvName.setText(sectors.get(i).name);
            boolean isCurrent = (i == tempIndex[0]);
            rb.setChecked(isCurrent);
            itemView.setBackgroundResource(isCurrent ? R.drawable.bg_action_row_selected : R.drawable.bg_action_row);

            View.OnClickListener selectAction = v -> {
                tempIndex[0] = index;
                for (int j = 0; j < rgSectors.getChildCount(); j++) {
                    View child = rgSectors.getChildAt(j);
                    RadioButton childRb = child.findViewById(R.id.rbSectorOption);
                    boolean isSelected = (j == index);
                    if (childRb != null) {
                        childRb.setChecked(isSelected);
                    }
                    child.setBackgroundResource(isSelected ? R.drawable.bg_action_row_selected : R.drawable.bg_action_row);
                }
            };

            itemView.setOnClickListener(selectAction);
            if (rb != null) {
                rb.setClickable(true);
                rb.setOnClickListener(selectAction);
            }

            rgSectors.addView(itemView);
        }
    }

    private boolean hasSameSectorNames(List<SectorOption> a, List<SectorOption> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).name.equals(b.get(i).name)) return false;
        }
        return true;
    }
}
