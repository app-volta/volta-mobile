package com.aula.volta.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aula.volta.R;

/**
 * Confirmar e-mail — 4 caixas com avanço automático (Fase 13).
 * Mock: aceita qualquer código de 4 números. Args: email, name.
 */
public class ConfirmEmailFragment extends Fragment {

    private EditText[] boxes;

    public ConfirmEmailFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_confirm_email, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String email = getArguments() != null ? getArguments().getString("email") : "";
        String name = getArguments() != null ? getArguments().getString("name") : "";
        ((TextView) view.findViewById(R.id.tvConfirmSub)).setText(
                getString(R.string.auth_confirm_sub, email == null ? "" : email));

        boxes = new EditText[]{
                view.findViewById(R.id.etCode1),
                view.findViewById(R.id.etCode2),
                view.findViewById(R.id.etCode3),
                view.findViewById(R.id.etCode4)};
        for (int i = 0; i < boxes.length; i++) {
            final int index = i;
            boxes[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < boxes.length - 1) {
                        boxes[index + 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            boxes[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL
                        && event.getAction() == android.view.KeyEvent.ACTION_DOWN
                        && boxes[index].getText().length() == 0 && index > 0) {
                    boxes[index - 1].requestFocus();
                    boxes[index - 1].setText("");
                    return true;
                }
                return false;
            });
        }
        boxes[0].post(() -> {
            if (isAdded()) {
                boxes[0].requestFocus();
            }
        });

        view.findViewById(R.id.btnConfirmCode).setOnClickListener(v -> {
            StringBuilder code = new StringBuilder();
            for (EditText box : boxes) {
                code.append(box.getText().toString());
            }
            if (code.length() < 4) {
                Toast.makeText(requireContext(), R.string.auth_bad_code,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Bundle args = new Bundle();
            args.putString("email", email);
            args.putString("name", name);
            Navigation.findNavController(v).navigate(R.id.auth_success, args);
        });
    }
}
