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
 * Cadastro — espelho do Login (Fase 13, mock: valida e segue ao código).
 */
public class RegisterFragment extends Fragment {

    public RegisterFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnCreateAccount).setOnClickListener(v -> {
            String name = textOf(view, R.id.etRegisterName).trim();
            String email = textOf(view, R.id.etRegisterEmail).trim();
            String password = textOf(view, R.id.etRegisterPassword);
            if (name.isEmpty() || !email.contains("@") || password.length() < 4) {
                Toast.makeText(requireContext(), R.string.auth_invalid,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Bundle args = new Bundle();
            args.putString("email", email);
            args.putString("name", name);
            Navigation.findNavController(v).navigate(R.id.auth_confirm, args);
        });
        view.findViewById(R.id.btnGoLogin).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
    }

    private String textOf(View root, int id) {
        android.widget.EditText et = root.findViewById(id);
        return et == null ? "" : et.getText().toString();
    }
}
