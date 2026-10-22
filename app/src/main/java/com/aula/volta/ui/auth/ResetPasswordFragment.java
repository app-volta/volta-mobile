package com.aula.volta.ui.auth;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aula.volta.R;

/**
 * Criar nova senha — Figma 790:1190.
 * Medidor de força com 4 barras e texto abaixo alinhado à esquerda,
 * validação visual do campo de confirmação com borda ativa #1BA463,
 * chip verde e ícone de check circular, e validação de mínimo de 8 caracteres.
 */
public class ResetPasswordFragment extends Fragment {

    private EditText etNew;
    private EditText etConfirm;
    private View[] segments;
    private TextView tvStrength;
    private View cardConfirm;
    private View chipConfirm;
    private ImageView ivConfirmLock;
    private ImageView ivConfirmMatch;
    private ImageView btnToggleNew;
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
        cardConfirm = view.findViewById(R.id.cardConfirmPassword);
        chipConfirm = view.findViewById(R.id.chipConfirmPassword);
        ivConfirmLock = view.findViewById(R.id.ivConfirmLock);
        ivConfirmMatch = view.findViewById(R.id.ivConfirmMatch);
        btnToggleNew = view.findViewById(R.id.btnToggleNewPassword);

        segments = new View[]{
                view.findViewById(R.id.strength1),
                view.findViewById(R.id.strength2),
                view.findViewById(R.id.strength3),
                view.findViewById(R.id.strength4)};

        view.findViewById(R.id.btnBackReset).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        if (btnToggleNew != null && etNew != null) {
            btnToggleNew.setOnClickListener(v -> {
                passwordVisible = !passwordVisible;
                Typeface tf = etNew.getTypeface();
                etNew.setInputType(passwordVisible
                        ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                etNew.setTypeface(tf);
                etNew.setSelection(etNew.getText().length());
                btnToggleNew.setColorFilter(requireContext().getColor(
                        passwordVisible ? R.color.login_btn_text_green : R.color.vf_text_secondary));
            });
        }

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateStrength(etNew != null ? etNew.getText().length() : 0);
                updateConfirmState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        if (etNew != null) {
            etNew.addTextChangedListener(watcher);
        }
        if (etConfirm != null) {
            etConfirm.addTextChangedListener(watcher);
        }

        updateStrength(0);
        updateConfirmState();

        view.findViewById(R.id.btnSavePassword).setOnClickListener(v -> {
            String first = etNew != null ? etNew.getText().toString() : "";
            String second = etConfirm != null ? etConfirm.getText().toString() : "";
            if (!AuthValidator.isValidPassword(first)) {
                Toast.makeText(requireContext(), R.string.auth_register_pw_min,
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

    /** 1 Fraca (#E5484D) · 2 Média (#E8893A) · 3 Boa (#1BA463) · 4 Forte (#0E7C48). */
    private void updateStrength(int length) {
        if (!isAdded() || tvStrength == null) return;
        if (length == 0) {
            tvStrength.setText("");
            for (View segment : segments) {
                if (segment != null) {
                    segment.setBackgroundResource(R.drawable.bg_strength_bar_track);
                    segment.setBackgroundTintList(null);
                }
            }
            return;
        }

        int level = AuthValidator.calculateStrengthLevel(length);
        int labelRes = AuthValidator.getStrengthLabelRes(level);
        int colorRes = AuthValidator.getStrengthColorRes(level);

        int activeColor = requireContext().getColor(colorRes);
        for (int i = 0; i < segments.length; i++) {
            if (segments[i] == null) continue;
            if (i < level) {
                segments[i].setBackgroundResource(R.drawable.bg_strength_bar_filled);
                segments[i].setBackgroundTintList(ColorStateList.valueOf(activeColor));
            } else {
                segments[i].setBackgroundResource(R.drawable.bg_strength_bar_track);
                segments[i].setBackgroundTintList(null);
            }
        }
        tvStrength.setText(getString(labelRes));
        tvStrength.setTextColor(activeColor);
    }

    /**
     * Atualiza o campo de confirmação:
     * Quando matching (e length >= 8): borda ativa #1BA463, chip verde e ícone de check circular.
     */
    private void updateConfirmState() {
        if (!isAdded() || cardConfirm == null || etNew == null || etConfirm == null) return;
        String first = etNew.getText().toString();
        String second = etConfirm.getText().toString();
        boolean matching = AuthValidator.isPasswordMatch(first, second);

        if (matching) {
            cardConfirm.setBackgroundResource(R.drawable.bg_card_surface_matching);
            if (chipConfirm != null) {
                chipConfirm.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.vf_green_bg)));
            }
            if (ivConfirmLock != null) {
                ivConfirmLock.setImageTintList(ColorStateList.valueOf(requireContext().getColor(R.color.status_aprovada_text)));
            }
            if (ivConfirmMatch != null) {
                ivConfirmMatch.setVisibility(View.VISIBLE);
            }
        } else {
            cardConfirm.setBackgroundResource(R.drawable.bg_card_surface_default);
            if (chipConfirm != null) {
                chipConfirm.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.confidence_row_bg)));
            }
            if (ivConfirmLock != null) {
                ivConfirmLock.setImageTintList(ColorStateList.valueOf(requireContext().getColor(R.color.volta_text_muted_light)));
            }
            if (ivConfirmMatch != null) {
                ivConfirmMatch.setVisibility(View.GONE);
            }
        }
    }
}
