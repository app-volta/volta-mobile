package com.aula.volta;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        // Construtor vazio obrigat?rio
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Infla o layout do fragmento
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // _________________________________________________________________________________________
        //            A??ES DA HOME
        // _________________________________________________________________________________________

        // Bot?o do banner CTA para abrir o registro de ocorr?ncia
        MaterialButton btnCtaRegister = view.findViewById(R.id.btnCtaRegister);
        if (btnCtaRegister != null) {
            btnCtaRegister.setOnClickListener(v -> {
                Navigation.findNavController(v).navigate(R.id.nav_register);
            });
        }

        // Link "Ver todas" para navegar para a aba de Ocorr?ncias
        TextView tvSeeAll = view.findViewById(R.id.tvSeeAllOccurrences);
        if (tvSeeAll != null) {
            tvSeeAll.setOnClickListener(v -> {
                Navigation.findNavController(v).navigate(R.id.nav_occurrences);
            });
        }
    }
}
