package com.aula.volta.ui.detail;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aula.volta.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * BottomSheetDialogFragment de adicionar observação — Figma 17c (908:6103).
 * Ancorado na base da tela, com atalhos de texto totalmente neutros (sem cor).
 */
public class AddObservationSheet extends BottomSheetDialogFragment {

    public interface OnObservationSavedListener {
        void onObservationSaved(String observation);
    }

    private static final String ARG_INITIAL_TEXT = "arg_initial_text";
    private String initialText = "";
    private OnObservationSavedListener listener;

    public static AddObservationSheet newInstance() {
        return new AddObservationSheet();
    }

    public static AddObservationSheet newInstance(String initialText) {
        AddObservationSheet sheet = new AddObservationSheet();
        Bundle args = new Bundle();
        args.putString(ARG_INITIAL_TEXT, initialText);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnObservationSavedListener(OnObservationSavedListener listener) {
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
        return inflater.inflate(R.layout.dialog_add_observation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            String txt = getArguments().getString(ARG_INITIAL_TEXT);
            if (txt != null) {
                initialText = txt;
            }
        }

        EditText etInput = view.findViewById(R.id.etObservationInput);
        TextView tvCounter = view.findViewById(R.id.tvCharCounter);

        TextView shortcutWet = view.findViewById(R.id.shortcutWet);
        TextView shortcutBlocked = view.findViewById(R.id.shortcutBlocked);
        TextView shortcutEpi = view.findViewById(R.id.shortcutEpi);

        if (etInput != null) {
            if (initialText != null && !initialText.isEmpty()) {
                etInput.setText(initialText);
                etInput.setSelection(initialText.length());
            }

            int len = etInput.getText() != null ? etInput.getText().length() : 0;
            if (tvCounter != null) {
                tvCounter.setText(len + "/280");
            }

            etInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (tvCounter != null) {
                        tvCounter.setText(s.length() + "/280");
                    }
                    syncShortcutStyles(s != null ? s.toString() : "", shortcutWet, shortcutBlocked, shortcutEpi);
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        setupShortcut(shortcutWet, "Material separado", etInput);
        setupShortcut(shortcutBlocked, "Área isolada", etInput);
        setupShortcut(shortcutEpi, "Pede pallet", etInput);

        String currentText = etInput != null && etInput.getText() != null ? etInput.getText().toString() : "";
        syncShortcutStyles(currentText, shortcutWet, shortcutBlocked, shortcutEpi);

        // Fechar e Cancelar
        View btnClose = view.findViewById(R.id.btnCloseObsSheet);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        View btnCancel = view.findViewById(R.id.btnCancelObservation);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dismiss());
        }

        // Salvar
        View btnSave = view.findViewById(R.id.btnSaveObservation);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String obs = etInput != null && etInput.getText() != null
                        ? etInput.getText().toString().trim() : "";
                if (listener != null) {
                    listener.onObservationSaved(obs);
                }
                dismiss();
            });
        }
    }

    private void setupShortcut(TextView chip, String phrase, EditText etInput) {
        if (chip == null || etInput == null) return;
        chip.setOnClickListener(v -> {
            String text = etInput.getText() != null ? etInput.getText().toString().trim() : "";
            boolean contains = text.toLowerCase().contains(phrase.toLowerCase());
            if (!contains) {
                // Adiciona o atalho
                if (text.isEmpty()) {
                    etInput.setText(phrase + ".");
                } else {
                    etInput.setText(text + " " + phrase + ".");
                }
            } else {
                // Remove o atalho
                String replaced = text
                        .replaceAll("(?i)\\b" + java.util.regex.Pattern.quote(phrase) + "\\.?", "")
                        .replaceAll("  +", " ")
                        .trim();
                etInput.setText(replaced);
            }
            if (etInput.getText() != null) {
                etInput.setSelection(etInput.getText().length());
            }
        });
    }

    private void syncShortcutStyles(String text, TextView... chips) {
        String lower = text.toLowerCase();
        for (TextView chip : chips) {
            if (chip == null) continue;
            String chipText = chip.getText() != null ? chip.getText().toString().trim() : "";
            boolean active = !chipText.isEmpty() && lower.contains(chipText.toLowerCase());
            paintNeutralChip(chip, active);
        }
    }

    private void paintNeutralChip(TextView chip, boolean active) {
        if (chip == null) return;
        if (active) {
            // Estilo ativo neutro: cinza suave com borda slate e texto escuro
            chip.setBackgroundResource(R.drawable.bg_shortcut_selected_neutral);
            chip.setTextColor(0xFF0F172A);
        } else {
            // Estilo inativo neutro: fundo branco com borda suave e texto cinza secundário
            chip.setBackgroundResource(R.drawable.bg_shortcut_unselected);
            chip.setTextColor(requireContext().getColor(R.color.vf_text_secondary));
        }
    }
}
