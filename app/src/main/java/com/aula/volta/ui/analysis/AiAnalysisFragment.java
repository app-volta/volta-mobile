package com.aula.volta.ui.analysis;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Análise da IA — Passo 2 de 2 (Fase 4 preenche; stub na Fase 3).
 * Recebe via args: photoPath, descricao, setor, avisar.
 */
public class AiAnalysisFragment extends Fragment {

    public AiAnalysisFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        TextView stub = new TextView(requireContext());
        stub.setText("Análise da IA (Fase 4)");
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        stub.setPadding(pad, pad, pad, pad);
        return stub;
    }
}
