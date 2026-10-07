package com.aula.volta.ui.auth;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
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
import com.aula.volta.data.api.AuthAPI;
import com.aula.volta.data.api.ChatbotAuthClient;
import com.aula.volta.data.local.SessionManager;
import com.aula.volta.data.model.auth.LoginRequest;
import com.aula.volta.data.model.auth.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Login — Figma 790:955.
 *
 * Realiza a autenticação no app e autentica em background com a API do Chatbot QA
 * sem travar ou bloquear a navegação do usuário no aplicativo.
 */
public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";

    private EditText etEmail;
    private EditText etPassword;
    private TextView btnLogin;
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
        btnLogin = view.findViewById(R.id.btnLogin);

        view.findViewById(R.id.btnTogglePassword).setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            etPassword.setInputType(passwordVisible
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            etPassword.setSelection(etPassword.getText().length());
        });

        btnLogin.setOnClickListener(v -> tryLogin());

        View btnDemo = view.findViewById(R.id.btnDemoLogin);
        if (btnDemo != null) {
            btnDemo.setOnClickListener(v -> {
                etEmail.setText("breno@volta.com");
                etPassword.setText("1234");
                tryLogin();
            });
        }

        view.findViewById(R.id.btnGoRegister).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.auth_register));
        view.findViewById(R.id.btnForgotPassword).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.auth_forgot));
        view.findViewById(R.id.btnGoogle).setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.occ_soon,
                        Toast.LENGTH_SHORT).show());
    }

    private void tryLogin() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString();

        if (!email.contains("@") || password.length() < 4) {
            Toast.makeText(requireContext(), R.string.auth_invalid,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        final Context appContext = requireContext().getApplicationContext();
        final String name = displayName(email);

        // 1. Salva a sessão local do usuário e permite entrada imediata no app
        SessionManager.login(appContext, email, password, name, "");

        // 2. Dispara autenticação em background com a API do Chatbot para obter o JWT
        try {
            AuthAPI authApi = ChatbotAuthClient.get(appContext).create(AuthAPI.class);
            authApi.login(new LoginRequest(email, password)).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().hasToken()) {
                        String token = response.body().getToken();
                        SessionManager.saveToken(appContext, token);
                        Log.i(TAG, "Token JWT do Chatbot obtido com sucesso no login.");
                    } else {
                        Log.w(TAG, "Chatbot login retornou status: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                    Log.w(TAG, "Chatbot login offline ou inalcançável: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "Erro ao iniciar autenticação do chatbot", e);
        }

        // Conclui login no app sem bloqueios
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
