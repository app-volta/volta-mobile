package com.aula.volta.ui.auth;

import android.os.Bundle;
import android.os.CountDownTimer;
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
 * Código da nova senha — Figma 790:1154 (Fase 13b).
 * Mock: qualquer código de 4 números; reenvio com timer de 48s.
 * Arg: email.
 */
public class PasswordCodeFragment extends Fragment {

    private static final long RESEND_MILLIS = 48000L;

    private EditText[] boxes;
    private TextView tvResend;
    private CountDownTimer timer;
    private boolean canResend;

    public PasswordCodeFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_password_code, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String email = getArguments() != null ? getArguments().getString("email") : "";
        ((TextView) view.findViewById(R.id.tvPasscodeSub)).setText(
                getString(R.string.auth_code_sub, email == null ? "" : email));

        view.findViewById(R.id.btnBackPasscode).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        boxes = new EditText[]{
                view.findViewById(R.id.etPass1),
                view.findViewById(R.id.etPass2),
                view.findViewById(R.id.etPass3),
                view.findViewById(R.id.etPass4)};
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

        tvResend = view.findViewById(R.id.tvResend);
        tvResend.setOnClickListener(v -> {
            if (canResend) {
                Toast.makeText(requireContext(), R.string.auth_code_sent,
                        Toast.LENGTH_SHORT).show();
                startResendTimer();
            }
        });
        startResendTimer();

        view.findViewById(R.id.btnConfirmPasscode).setOnClickListener(v -> {
            StringBuilder code = new StringBuilder();
            for (EditText box : boxes) {
                code.append(box.getText().toString());
            }
            if (code.length() < 4) {
                Toast.makeText(requireContext(), R.string.auth_bad_code,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Navigation.findNavController(v).navigate(R.id.auth_reset);
        });
    }

    private void startResendTimer() {
        canResend = false;
        cancelTimer();
        timer = new CountDownTimer(RESEND_MILLIS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (!isAdded() || tvResend == null) {
                    return;
                }
                int sec = (int) (millisUntilFinished / 1000);
                tvResend.setText(getString(R.string.auth_resend_in, sec));
            }

            @Override
            public void onFinish() {
                if (!isAdded() || tvResend == null) {
                    return;
                }
                canResend = true;
                tvResend.setText(getString(R.string.auth_resend));
            }
        }.start();
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void onDestroyView() {
        cancelTimer();
        super.onDestroyView();
    }
}
