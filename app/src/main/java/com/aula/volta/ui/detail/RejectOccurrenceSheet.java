package com.aula.volta.ui.detail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aula.volta.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class RejectOccurrenceSheet extends BottomSheetDialogFragment {

    public interface OnRejectConfirmedListener {
        void onReject(String reason);
    }

    private OnRejectConfirmedListener listener;

    private View row1;
    private View row2;
    private View row3;
    private View row4;
    private RadioButton rb1;
    private RadioButton rb2;
    private RadioButton rb3;
    private RadioButton rb4;
    private View layoutOtherReason;
    private EditText etOtherReason;
    private View layoutRejectError;
    private TextView tvRejectErrorText;

    public static RejectOccurrenceSheet newInstance() {
        return new RejectOccurrenceSheet();
    }

    public void setOnRejectConfirmedListener(OnRejectConfirmedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_reject_occurrence, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        row1 = view.findViewById(R.id.rowRejectReason1);
        row2 = view.findViewById(R.id.rowRejectReason2);
        row3 = view.findViewById(R.id.rowRejectReason3);
        row4 = view.findViewById(R.id.rowRejectReason4);
        rb1 = view.findViewById(R.id.rbRejectReason1);
        rb2 = view.findViewById(R.id.rbRejectReason2);
        rb3 = view.findViewById(R.id.rbRejectReason3);
        rb4 = view.findViewById(R.id.rbRejectReason4);
        layoutOtherReason = view.findViewById(R.id.layoutOtherReason);
        etOtherReason = view.findViewById(R.id.etOtherReason);
        layoutRejectError = view.findViewById(R.id.layoutRejectError);
        tvRejectErrorText = view.findViewById(R.id.tvRejectErrorText);

        View.OnClickListener select1 = v -> selectOption(1);
        View.OnClickListener select2 = v -> selectOption(2);
        View.OnClickListener select3 = v -> selectOption(3);
        View.OnClickListener select4 = v -> selectOption(4);

        if (row1 != null) row1.setOnClickListener(select1);
        if (row2 != null) row2.setOnClickListener(select2);
        if (row3 != null) row3.setOnClickListener(select3);
        if (row4 != null) row4.setOnClickListener(select4);

        View btnClose = view.findViewById(R.id.btnRejectClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        View btnConfirm = view.findViewById(R.id.btnConfirmReject);
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> confirmReject());
        }
    }

    private void selectOption(int index) {
        if (layoutRejectError != null) {
            layoutRejectError.setVisibility(View.GONE);
        }
        if (rb1 != null) rb1.setChecked(index == 1);
        if (rb2 != null) rb2.setChecked(index == 2);
        if (rb3 != null) rb3.setChecked(index == 3);
        if (rb4 != null) rb4.setChecked(index == 4);

        if (row1 != null) row1.setBackgroundResource(index == 1 ? R.drawable.bg_radio_row_selected : R.drawable.bg_radio_row_default);
        if (row2 != null) row2.setBackgroundResource(index == 2 ? R.drawable.bg_radio_row_selected : R.drawable.bg_radio_row_default);
        if (row3 != null) row3.setBackgroundResource(index == 3 ? R.drawable.bg_radio_row_selected : R.drawable.bg_radio_row_default);
        if (row4 != null) row4.setBackgroundResource(index == 4 ? R.drawable.bg_radio_row_selected : R.drawable.bg_radio_row_default);

        if (layoutOtherReason != null) {
            layoutOtherReason.setVisibility(index == 4 ? View.VISIBLE : View.GONE);
            if (index == 4 && etOtherReason != null) {
                etOtherReason.requestFocus();
            }
        }
    }

    private void confirmReject() {
        String chosen = null;
        if (rb1 != null && rb1.isChecked()) {
            chosen = "Foto não mostra o resíduo";
        } else if (rb2 != null && rb2.isChecked()) {
            chosen = "Não é resíduo (item em uso)";
        } else if (rb3 != null && rb3.isChecked()) {
            chosen = "Ocorrência duplicada";
        } else if (rb4 != null && rb4.isChecked()) {
            String custom = etOtherReason != null && etOtherReason.getText() != null
                    ? etOtherReason.getText().toString().trim() : "";
            if (custom.isEmpty()) {
                if (layoutRejectError != null && tvRejectErrorText != null) {
                    tvRejectErrorText.setText("Digite o motivo da recusa.");
                    layoutRejectError.setVisibility(View.VISIBLE);
                }
                return;
            }
            chosen = custom;
        }

        if (chosen == null) {
            if (layoutRejectError != null && tvRejectErrorText != null) {
                tvRejectErrorText.setText("Escolha um motivo pra recusar.");
                layoutRejectError.setVisibility(View.VISIBLE);
            }
            return;
        }

        dismiss();
        if (listener != null) {
            listener.onReject(chosen);
        }
    }
}
