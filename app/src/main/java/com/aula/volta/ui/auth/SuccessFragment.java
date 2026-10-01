package com.aula.volta.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aula.volta.R;
import com.aula.volta.data.local.SessionManager;

/**
 * Sucesso — conta criada. "Começar" grava a sessão e entra (Fase 13).
 * Args: email, name.
 */
public class SuccessFragment extends Fragment {

    public SuccessFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auth_success, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnStart).setOnClickListener(v -> {
            String email = getArguments() != null ? getArguments().getString("email") : "";
            String name = getArguments() != null ? getArguments().getString("name") : "";
            SessionManager.login(requireContext(),
                    email == null ? "" : email, name == null ? "" : name);
            ((AuthActivity) requireActivity()).finishLogin();
        });
    }
}
