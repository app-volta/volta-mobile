package com.aula.volta.ui.occurrence;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aula.volta.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * BottomSheetDialogFragment para seleção de Setor / Local (Figma 851:3836).
 * Abre ancorado na base da tela, idêntico ao NotificationsSheet.
 */
public class SelectSectorSheet extends BottomSheetDialogFragment {

    public interface OnSectorSelectedListener {
        void onSectorSelected(int index, String sectorName);
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
        dialog.setOnShowListener(d -> {
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
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

        String[] sectors = getResources().getStringArray(R.array.occurrence_sector_options);
        RadioGroup rgSectors = view.findViewById(R.id.rgSectors);
        View btnClose = view.findViewById(R.id.btnSectorClose);
        View btnConfirm = view.findViewById(R.id.btnConfirmSector);

        final int[] tempIndex = {selectedIndex};

        if (rgSectors != null) {
            rgSectors.removeAllViews();
            for (int i = 0; i < sectors.length; i++) {
                final int index = i;
                View itemView = getLayoutInflater().inflate(R.layout.item_sector_option, rgSectors, false);
                TextView tvName = itemView.findViewById(R.id.tvSectorOptionName);
                RadioButton rb = itemView.findViewById(R.id.rbSectorOption);

                tvName.setText(sectors[i]);
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

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                selectedIndex = tempIndex[0];
                if (listener != null && selectedIndex >= 0 && selectedIndex < sectors.length) {
                    listener.onSectorSelected(selectedIndex, sectors[selectedIndex]);
                }
                dismiss();
            });
        }
    }
}
