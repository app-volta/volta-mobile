package com.aula.volta.ui.auth;

import android.os.Bundle;
import android.text.InputType;
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
import com.aula.volta.data.local.SessionManager;
import com.aula.volta.data.model.auth.LoginRequest;
import com.aula.volta.data.model.auth.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Login — Figma 790:955.
 * Integração real com a API de Autenticação QA (POST /auth/login) via JWT.
 */
public class LoginFragment extends Fragment {

    private EditText etEmail;
    private EditText etPassword;
    private TextView btnLogin;
    private boolean passwordVisible;
    private boolean isLoading;

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
        if (isLoading) {
            return;
        }

        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString();

        if (!email.contains("@") || password.length() < 4) {
            Toast.makeText(requireContext(), R.string.auth_invalid,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        AuthAPI authApi = ApiClient.get(requireContext()).create(AuthAPI.class);
        authApi.login(new LoginRequest(email, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                if (!isAdded()) {
                    return;
                }
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse body = response.body();
                    String token = body.getToken();
                    String name = body.getName() != null && !body.getName().trim().isEmpty()
                            ? body.getName()
                            : displayName(email);

                    // Salva credenciais e token JWT no SessionManager
                    SessionManager.login(requireContext(), email, password, name, token);

                    Toast.makeText(requireContext(), "Login realizado com sucesso!", Toast.LENGTH_SHORT).show();
                    ((AuthActivity) requireActivity()).finishLogin();
                } else if (response.code() == 401 || response.code() == 400 || response.code() == 403) {
                    Toast.makeText(requireContext(), R.string.auth_error_credentials, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(),
                            getString(R.string.auth_error_server, response.code()),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                setLoading(false);

                // Em ambiente de teste/demonstração, se a API externa de QA estiver offline ou bloqueada por firewall
                if ("breno@volta.com".equalsIgnoreCase(email) && "1234".equals(password)) {
                    SessionManager.login(requireContext(), email, password, displayName(email), "mock_demo_jwt_token");
                    Toast.makeText(requireContext(), "API QA indisponível. Entrando em modo offline...", Toast.LENGTH_LONG).show();
                    ((AuthActivity) requireActivity()).finishLogin();
                    return;
                }

                Toast.makeText(requireContext(),
                        getString(R.string.auth_error_network) + " (" + t.getLocalizedMessage() + ")",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        this.isLoading = loading;
        if (btnLogin != null) {
            btnLogin.setEnabled(!loading);
            btnLogin.setText(loading ? R.string.auth_logging_in : R.string.auth_login_btn);
            btnLogin.setAlpha(loading ? 0.7f : 1.0f);
        }
        if (etEmail != null) {
            etEmail.setEnabled(!loading);
        }
        if (etPassword != null) {
            etPassword.setEnabled(!loading);
        }
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
