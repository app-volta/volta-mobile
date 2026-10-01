package com.aula.volta.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
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
 * Criar nova senha — Figma 790:1190 (Fase 13b, mock + medidor de força).
 */
public class ResetPasswordFragment extends Fragment {

    private EditText etNew;
    private EditText etConfirm;
    private View[] segments;
    private TextView tvStrength;
    private boolean passwordVisible;

    public ResetPasswordFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reset_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etNew = view.findViewById(R.id.etNewPassword);
        etConfirm = view.findViewById(R.id.etConfirmPassword);
        tvStrength = view.findViewById(R.id.tvStrength);
        segments = new View[]{
                view.findViewById(R.id.strength1),
                view.findViewById(R.id.strength2),
                view.findViewById(R.id.strength3),
                view.findViewById(R.id.strength4)};

        view.findViewById(R.id.btnBackReset).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        view.findViewById(R.id.btnToggleNewPassword).setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            etNew.setInputType(passwordVisible
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            etNew.setSelection(etNew.getText().length());
        });

        etNew.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateStrength(s.length());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        updateStrength(0);

        view.findViewById(R.id.btnSavePassword).setOnClickListener(v -> {
            String first = etNew.getText().toString();
            String second = etConfirm.getText().toString();
            if (first.length() < 4) {
                Toast.makeText(requireContext(), R.string.auth_invalid,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (!first.equals(second)) {
                Toast.makeText(requireContext(), R.string.auth_pw_mismatch,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Navigation.findNavController(v).navigate(R.id.auth_passok);
        });
    }

    /** 1 cinza Fraca · 2 Razoável · 3 Boa · 4 verde Forte. */
    private void updateStrength(int length) {
        int level;
        int labelRes;
        if (length >= 8) {
            level = 4;
            labelRes = R.string.auth_pw_strong;
        } else if (length >= 6) {
            level = 3;
            labelRes = R.string.auth_pw_good;
        } else if (length >= 4) {
            level = 2;
            labelRes = R.string.auth_pw_fair;
        } else {
            level = length == 0 ? 0 : 1;
            labelRes = R.string.auth_pw_weak;
        }
        for (int i = 0; i < segments.length; i++) {
            segments[i].setBackgroundResource(i < level
                    ? R.drawable.bg_progress_filled : R.drawable.bg_progress_track);
        }
        tvStrength.setText(getString(labelRes));
    }
}
