package com.aula.volta.ui.auth;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aula.volta.R;
import com.aula.volta.data.local.SessionManager;

/**
 * Login — Figma 790:955 (Fase 13).
 * Mock: qualquer e-mail válido + senha ≥ 4 entra. OAuth e recovery chegam depois.
 */
public class LoginFragment extends Fragment {

    private EditText etEmail;
    private EditText etPassword;
    private boolean passwordVisible;

    public LoginFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etEmail = view.findViewById(R.id.etLoginEmail);
        etPassword = view.findViewById(R.id.etLoginPassword);

        view.findViewById(R.id.btnTogglePassword).setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            etPassword.setInputType(passwordVisible
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            etPassword.setSelection(etPassword.getText().length());
        });

        view.findViewById(R.id.btnLogin).setOnClickListener(v -> tryLogin(v));
        view.findViewById(R.id.btnGoRegister).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.auth_register));
        view.findViewById(R.id.btnForgotPassword).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.auth_forgot));
        view.findViewById(R.id.btnGoogle).setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.occ_soon,
                        Toast.LENGTH_SHORT).show());
    }

    private void tryLogin(View v) {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        if (!email.contains("@") || password.length() < 4) {
            Toast.makeText(requireContext(), R.string.auth_invalid,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        SessionManager.login(requireContext(), email, displayName(email));
        ((AuthActivity) requireActivity()).finishLogin();
    }

    private String displayName(String email) {
        String prefix = email.split("@")[0].replace('.', ' ').replace('_', ' ').trim();
        if (prefix.isEmpty()) {
            return email;
        }
        String[] parts = prefix.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0)));
                if (p.length() > 1) {
                    sb.append(p.substring(1));
                }
                sb.append(' ');
            }
        }
        return sb.toString().trim();
    }
}
