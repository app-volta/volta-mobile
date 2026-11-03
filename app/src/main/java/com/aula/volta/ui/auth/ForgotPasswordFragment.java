package com.aula.volta.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aula.volta.R;
import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.AuthAPI;
import com.aula.volta.data.model.auth.ForgotPasswordRequestDTO;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordFragment extends Fragment {

    private AppCompatButton btnSendCode;
    private EditText etForgotEmail;

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

        etForgotEmail = view.findViewById(R.id.etForgotEmail);
        btnSendCode = view.findViewById(R.id.btnSendCode);

        view.findViewById(R.id.btnBackForgot).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
        view.findViewById(R.id.btnBackToLogin).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        if (btnSendCode != null) {
            btnSendCode.setOnClickListener(v -> sendRecoveryCode(v));
        }
    }

    private void sendRecoveryCode(View v) {
        if (etForgotEmail == null) {
            return;
        }
        String email = etForgotEmail.getText().toString().trim();
        if (!email.contains("@") || !email.contains(".")) {
            Toast.makeText(requireContext(), R.string.auth_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        btnSendCode.setEnabled(false);
        btnSendCode.setText("Enviando código...");

        AuthAPI authApi = ApiClient.get(requireContext()).create(AuthAPI.class);
        authApi.forgotPassword(new ForgotPasswordRequestDTO(email)).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (!isAdded()) {
                    return;
                }
                btnSendCode.setEnabled(true);
                btnSendCode.setText(R.string.auth_send_code);

                Toast.makeText(requireContext(), "Código de 4 dígitos enviado para seu e-mail!", Toast.LENGTH_LONG).show();

                Bundle args = new Bundle();
                args.putString("email", email);
                Navigation.findNavController(v).navigate(R.id.auth_passcode, args);
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                btnSendCode.setEnabled(true);
                btnSendCode.setText(R.string.auth_send_code);

                Toast.makeText(requireContext(), "Não foi possível conectar ao servidor. Verifique sua conexão.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}