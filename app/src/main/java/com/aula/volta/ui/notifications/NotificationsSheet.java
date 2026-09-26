package com.aula.volta.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Bottom sheet de notificações (Fase 7 preenche; stub na Fase 0).
 * Aberto pelo sino da Home.
 */
public class NotificationsSheet extends BottomSheetDialogFragment {

    public NotificationsSheet() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        TextView stub = new TextView(requireContext());
        stub.setText("Notificações (Fase 7)");
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        stub.setPadding(pad, pad, pad, pad);
        return stub;
    }
}
