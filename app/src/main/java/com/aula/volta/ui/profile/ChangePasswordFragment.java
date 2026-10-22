package com.aula.volta.ui.profile;

import android.content.res.ColorStateList;
import android.graphics.Color;
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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aula.volta.R;
import com.aula.volta.data.local.SessionManager;

/**
 * 23c · Perfil · alterar senha (Figma 1721:1390).
 */
public class ChangePasswordFragment extends Fragment {

    private EditText etCurrentPassword;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private ImageView btnToggleCurrentPassword;
    private ImageView btnToggleNewPassword;
    private ImageView imgConfirmCheck;
    private TextView tvPasswordStrength;

    private View[] strengthBars;

    private boolean currentPasswordVisible = false;
    private boolean newPasswordVisible = false;

    public ChangePasswordFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_change_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etCurrentPassword = view.findViewById(R.id.etCurrentPassword);
        etNewPassword = view.findViewById(R.id.etNewPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        btnToggleCurrentPassword = view.findViewById(R.id.btnToggleCurrentPassword);
        btnToggleNewPassword = view.findViewById(R.id.btnToggleNewPassword);
        imgConfirmCheck = view.findViewById(R.id.imgConfirmCheck);
        tvPasswordStrength = view.findViewById(R.id.tvPasswordStrength);

        strengthBars = new View[]{
                view.findViewById(R.id.strengthBar1),
                view.findViewById(R.id.strengthBar2),
                view.findViewById(R.id.strengthBar3),
                view.findViewById(R.id.strengthBar4)
        };

        view.findViewById(R.id.btnBackChangePassword).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        // Toggle visibilidade da senha atual
        btnToggleCurrentPassword.setOnClickListener(v -> {
            currentPasswordVisible = !currentPasswordVisible;
            togglePasswordVisibility(etCurrentPassword, btnToggleCurrentPassword, currentPasswordVisible);
        });

        // Toggle visibilidade da nova senha
        btnToggleNewPassword.setOnClickListener(v -> {
            newPasswordVisible = !newPasswordVisible;
            togglePasswordVisibility(etNewPassword, btnToggleNewPassword, newPasswordVisible);
        });

        // Link "Esqueci a senha"
        view.findViewById(R.id.btnForgotCurrentPassword).setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Redefinir senha")
                    .setMessage("Um link de recuperação será enviado para o e-mail cadastrado na sua unidade: "
                            + SessionManager.email(requireContext()))
                    .setPositiveButton("Enviar link", (d, w) ->
                            Toast.makeText(requireContext(), "Link de redefinição enviado!", Toast.LENGTH_SHORT).show())
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateStrengthMeter();
                updateConfirmMatch();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        etNewPassword.addTextChangedListener(textWatcher);
        etConfirmPassword.addTextChangedListener(textWatcher);

        updateStrengthMeter();
        updateConfirmMatch();

        view.findViewById(R.id.btnSaveNewPassword).setOnClickListener(v -> saveNewPassword());
    }

    private void togglePasswordVisibility(EditText editText, ImageView toggleBtn, boolean visible) {
        if (editText == null) return;
        Typeface tf = editText.getTypeface();
        editText.setInputType(visible
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        editText.setTypeface(tf);
        editText.setSelection(editText.getText().length());
        toggleBtn.setColorFilter(visible ? Color.parseColor("#1BA463") : Color.parseColor("#7C8698"));
    }

    private void updateStrengthMeter() {
        if (etNewPassword == null || tvPasswordStrength == null) return;
        String pw = etNewPassword.getText().toString();
        int score = calculateStrength(pw);

        if (pw.isEmpty()) {
            tvPasswordStrength.setText("");
            for (View bar : strengthBars) {
                if (bar != null) {
                    bar.setBackgroundResource(R.drawable.bg_strength_bar_track);
                    bar.setBackgroundTintList(null);
                }
            }
            return;
        }

        String label;
        int color;
        switch (score) {
            case 1:
                label = "Fraca";
                color = Color.parseColor("#E5484D");
                break;
            case 2:
                label = "Média";
                color = Color.parseColor("#E8893A");
                break;
            case 3:
                label = "Boa";
                color = Color.parseColor("#1BA463");
                break;
            case 4:
            default:
                label = "Forte";
                color = Color.parseColor("#0E7C48");
                break;
        }

        tvPasswordStrength.setText(label);
        tvPasswordStrength.setTextColor(color);

        for (int i = 0; i < strengthBars.length; i++) {
            View bar = strengthBars[i];
            if (bar == null) continue;
            if (i < score) {
                bar.setBackgroundResource(R.drawable.bg_strength_bar_filled);
                bar.setBackgroundTintList(ColorStateList.valueOf(color));
            } else {
                bar.setBackgroundResource(R.drawable.bg_strength_bar_track);
                bar.setBackgroundTintList(null);
            }
        }
    }

    private int calculateStrength(String pw) {
        if (pw == null || pw.isEmpty()) return 0;
        int score = 0;
        if (pw.length() >= 8) score++;
        if (pw.matches(".*[A-Z].*") || pw.matches(".*[a-z].*")) score++;
        if (pw.matches(".*\\d.*")) score++;
        if (pw.matches(".*[!@#$%^&*()\\-_=+{};:,<.>].*") || pw.length() >= 12) score++;
        return Math.max(1, score);
    }

    private void updateConfirmMatch() {
        if (etNewPassword == null || etConfirmPassword == null || imgConfirmCheck == null) return;
        String newPw = etNewPassword.getText().toString();
        String confirmPw = etConfirmPassword.getText().toString();

        if (!confirmPw.isEmpty() && confirmPw.equals(newPw)) {
            imgConfirmCheck.setVisibility(View.VISIBLE);
        } else {
            imgConfirmCheck.setVisibility(View.GONE);
        }
    }

    private void saveNewPassword() {
        String currentPw = etCurrentPassword.getText().toString().trim();
        String newPw = etNewPassword.getText().toString().trim();
        String confirmPw = etConfirmPassword.getText().toString().trim();

        if (currentPw.isEmpty()) {
            Toast.makeText(requireContext(), "Informe a senha atual", Toast.LENGTH_SHORT).show();
            etCurrentPassword.requestFocus();
            return;
        }

        if (newPw.length() < 8) {
            Toast.makeText(requireContext(), "A nova senha deve ter pelo menos 8 caracteres", Toast.LENGTH_SHORT).show();
            etNewPassword.requestFocus();
            return;
        }

        if (!newPw.equals(confirmPw)) {
            Toast.makeText(requireContext(), "As senhas não coincidem", Toast.LENGTH_SHORT).show();
            etConfirmPassword.requestFocus();
            return;
        }

        // Salva na sessão e propaga resultado
        Bundle result = new Bundle();
        result.putBoolean("passwordChanged", true);
        getParentFragmentManager().setFragmentResult("profile_result", result);

        if (getView() != null) {
            Navigation.findNavController(getView()).navigateUp();
        }
    }
}
