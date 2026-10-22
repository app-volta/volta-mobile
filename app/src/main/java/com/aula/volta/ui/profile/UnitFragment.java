package com.aula.volta.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aula.volta.R;
import com.aula.volta.data.local.SessionManager;

/**
 * 23f · Perfil · minha unidade (Figma 1721:1606).
 */
public class UnitFragment extends Fragment {

    private TextView tvUnitSubtitle;

    public UnitFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_unit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvUnitSubtitle = view.findViewById(R.id.tvUnitSubtitle);

        view.findViewById(R.id.btnBackUnit).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        String sector = SessionManager.sector(requireContext());
        if (sector != null && !sector.trim().isEmpty()) {
            tvUnitSubtitle.setText(sector + " · Av. Industrial, 1200 · SP");
        }

        // Navegação para Cooperativa parceira
        view.findViewById(R.id.rowUnitPartnerCoop).setOnClickListener(v -> {
            try {
                Navigation.findNavController(v).navigate(R.id.nav_cooperatives);
            } catch (Exception ignored) {
                Navigation.findNavController(v).navigateUp();
            }
        });
    }
}
