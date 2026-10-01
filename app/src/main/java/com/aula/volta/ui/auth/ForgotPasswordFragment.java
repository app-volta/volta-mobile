package com.aula.volta.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aula.volta.R;

/**
 * Esqueci a senha — Figma 790:1119 (Fase 13b, mock: e-mail válido segue ao código).
 */
public class ForgotPasswordFragment extends Fragment {

    public ForgotPasswordFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnBackForgot).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
        view.findViewById(R.id.btnBackToLogin).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        view.findViewById(R.id.btnSendCode).setOnClickListener(v -> {
            android.widget.EditText et =
                    view.findViewById(R.id.etForgotEmail);
            String email = et.getText().toString().trim();
            if (!email.contains("@")) {
                Toast.makeText(requireContext(), R.string.auth_invalid,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Bundle args = new Bundle();
            args.putString("email", email);
            Navigation.findNavController(v).navigate(R.id.auth_passcode, args);
        });
    }
}
