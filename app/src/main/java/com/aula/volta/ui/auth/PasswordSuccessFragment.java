package com.aula.volta.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aula.volta.R;

/**
 * Senha atualizada — Figma 790:1241. Volta ao login limpando o recovery da pilha.
 */
public class PasswordSuccessFragment extends Fragment {

    public PasswordSuccessFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_password_success, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnGoLogin).setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack(R.id.auth_login, false));
    }
}
