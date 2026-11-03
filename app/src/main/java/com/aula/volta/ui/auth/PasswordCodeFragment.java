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
import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.AuthAPI;
import com.aula.volta.data.model.auth.ForgotPasswordRequestDTO;
import com.aula.volta.data.model.auth.VerifyCodeRequestDTO;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PasswordCodeFragment extends Fragment {

    private static final long RESEND_MILLIS = 48000L;

    private EditText[] boxes;
    private TextView tvResend;
    private View btnConfirmPasscode;
    private CountDownTimer timer;
    private boolean canResend;
    private String email = "";

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

        email = getArguments() != null ? getArguments().getString("email", "") : "";
        TextView tvSub = view.findViewById(R.id.tvPasscodeSub);
        TextView tvEmail = view.findViewById(R.id.tvPasscodeEmail);
        if (tvSub != null) {
            tvSub.setText(R.string.auth_confirm_sub);
        }
        if (tvEmail != null) {
            tvEmail.setText(email == null || email.isEmpty() ? "seu e-mail" : email);
        }

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
        boxes[0].post(() -> {
            if (isAdded()) {
                boxes[0].requestFocus();
            }
        });

        tvResend = view.findViewById(R.id.tvResend);
        if (tvResend != null) {
            tvResend.setOnClickListener(v -> {
                if (canResend) {
                    resendCode();
                }
            });
            startResendTimer();
        }

        btnConfirmPasscode = view.findViewById(R.id.btnConfirmPasscode);
        if (btnConfirmPasscode != null) {
            btnConfirmPasscode.setOnClickListener(v -> verifyCodeAndProceed(v));
        }
    }

    private void resendCode() {
        if (email == null || email.isEmpty()) {
            Toast.makeText(requireContext(), R.string.auth_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        AuthAPI authApi = ApiClient.get(requireContext()).create(AuthAPI.class);
        authApi.forgotPassword(new ForgotPasswordRequestDTO(email)).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), R.string.auth_code_sent, Toast.LENGTH_SHORT).show();
                startResendTimer();
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Erro ao reenviar código.", Toast.LENGTH_SHORT).show();
                startResendTimer();
            }
        });
    }

    private void verifyCodeAndProceed(View v) {
        StringBuilder codeBuilder = new StringBuilder();
        for (EditText box : boxes) {
            if (box != null) {
                codeBuilder.append(box.getText().toString().trim());
            }
        }
        String code = codeBuilder.toString();
        if (code.length() < 4) {
            Toast.makeText(requireContext(), R.string.auth_bad_code, Toast.LENGTH_SHORT).show();
            return;
        }

        if (btnConfirmPasscode != null) {
            btnConfirmPasscode.setEnabled(false);
        }

        AuthAPI authApi = ApiClient.get(requireContext()).create(AuthAPI.class);
        authApi.verifyCode(new VerifyCodeRequestDTO(email, code)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!isAdded()) return;
                if (btnConfirmPasscode != null) {
                    btnConfirmPasscode.setEnabled(true);
                }

                if (response.isSuccessful()) {
                    Bundle args = new Bundle();
                    args.putString("email", email);
                    args.putString("code", code);
                    Navigation.findNavController(v).navigate(R.id.auth_reset, args);
                } else {
                    Toast.makeText(requireContext(), "Código incorreto ou expirado. Verifique seu e-mail.", Toast.LENGTH_SHORT).show();
                    for (EditText box : boxes) {
                        if (box != null) box.setText("");
                    }
                    if (boxes[0] != null) boxes[0].requestFocus();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                if (btnConfirmPasscode != null) {
                    btnConfirmPasscode.setEnabled(true);
                }
                Toast.makeText(requireContext(), "Falha de conexão. Verifique sua internet.", Toast.LENGTH_SHORT).show();
            }
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