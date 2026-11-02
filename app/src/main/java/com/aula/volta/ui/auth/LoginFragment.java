package com.aula.volta.ui.auth;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aula.volta.R;
import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.AuthAPI;
import com.aula.volta.data.api.ChatbotAuthClient;
import com.aula.volta.data.local.SessionManager;
import com.aula.volta.data.model.auth.LoginRequest;
import com.aula.volta.data.model.auth.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";

    private EditText etEmail;
    private EditText etPassword;
    private AppCompatButton btnLogin;
    private ProgressBar pbLoading;
    private View btnForgotPassword;
    private View btnGoRegister;
    private View btnGoogle;
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
        pbLoading = view.findViewById(R.id.pbLoginLoading);
        btnForgotPassword = view.findViewById(R.id.btnForgotPassword);
        btnGoRegister = view.findViewById(R.id.btnGoRegister);
        btnGoogle = view.findViewById(R.id.btnGoogle);

        view.findViewById(R.id.btnTogglePassword).setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            android.graphics.Typeface tf = etPassword.getTypeface();
            etPassword.setInputType(passwordVisible
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            etPassword.setTypeface(tf);
            etPassword.setSelection(etPassword.getText().length());
        });

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> tryLogin(v));
        }

        if (btnGoRegister != null) {
            btnGoRegister.setOnClickListener(v ->
                    Navigation.findNavController(v).navigate(R.id.auth_register));
        }
        if (btnForgotPassword != null) {
            btnForgotPassword.setOnClickListener(v ->
                    Navigation.findNavController(v).navigate(R.id.auth_forgot));
        }
        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v ->
                    Toast.makeText(requireContext(), R.string.occ_soon,
                            Toast.LENGTH_SHORT).show());
        }

        getParentFragmentManager().setFragmentResultListener(
                com.aula.volta.ui.error.OfflineErrorFragment.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    if (result.getBoolean(com.aula.volta.ui.error.OfflineErrorFragment.RESULT_ONLINE, false)
                            && getView() != null && etEmail != null && etPassword != null) {
                        String em = etEmail.getText().toString().trim();
                        String pw = etPassword.getText().toString();
                        if (em.contains("@") && pw.length() >= 4) {
                            tryLogin(btnLogin != null ? btnLogin : getView().findViewById(R.id.btnLogin));
                        }
                    }
                });
    }

    private void setLoading(boolean loading) {
        if (!isAdded()) {
            return;
        }
        if (btnLogin != null) {
            btnLogin.setEnabled(!loading);
            if (loading) {
                btnLogin.setText("");
            } else {
                btnLogin.setText(R.string.auth_login_btn);
            }
        }
        if (pbLoading != null) {
            pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (etEmail != null) {
            etEmail.setEnabled(!loading);
        }
        if (etPassword != null) {
            etPassword.setEnabled(!loading);
        }
        if (btnForgotPassword != null) {
            btnForgotPassword.setEnabled(!loading);
        }
        if (btnGoRegister != null) {
            btnGoRegister.setEnabled(!loading);
        }
        if (btnGoogle != null) {
            btnGoogle.setEnabled(!loading);
        }
    }

    private void tryLogin(View v) {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString();
        if (!email.contains("@") || password.length() < 4) {
            Toast.makeText(requireContext(), R.string.auth_invalid,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        final Context appContext = requireContext().getApplicationContext();
        if (!com.aula.volta.util.NetworkUtils.isOnline(appContext)) {
            showOfflineError(v, true);
            return;
        }
        final String name = displayName(email);

        setLoading(true);

        try {
            AuthAPI authApi = ChatbotAuthClient.get(appContext).create(AuthAPI.class);
            authApi.login(new LoginRequest(email, password)).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().hasToken()) {
                        SessionManager.login(appContext, email, password, name, "");
                        ApiClient.reset();
                        ChatbotAuthClient.reset();

                        String token = response.body().getToken().trim();
                        SessionManager.saveToken(appContext, token);
                        ApiClient.saveToken(appContext, token);
                        Log.i(TAG, "Token JWT da API obtido com sucesso no login.");

                        try {
                            authApi.getMyCompany().enqueue(new Callback<com.aula.volta.data.model.CompanyResponseDTO>() {
                                @Override
                                public void onResponse(@NonNull Call<com.aula.volta.data.model.CompanyResponseDTO> call,
                                                       @NonNull Response<com.aula.volta.data.model.CompanyResponseDTO> compResp) {
                                    if (compResp.isSuccessful() && compResp.body() != null) {
                                        com.aula.volta.data.model.CompanyResponseDTO c = compResp.body();
                                        if (c.getId() != null && !c.getId().trim().isEmpty()) {
                                            SessionManager.saveCompanyId(appContext, c.getId().trim());
                                        }
                                        SessionManager.saveCompany(appContext, c.getName(), c.getFormattedCnpj(), c.getAddress());
                                    }
                                }

                                @Override
                                public void onFailure(@NonNull Call<com.aula.volta.data.model.CompanyResponseDTO> call, @NonNull Throwable t) {
                                }
                            });
                            authApi.getMe().enqueue(new Callback<com.aula.volta.data.model.UserResponseDTO>() {
                                @Override
                                public void onResponse(@NonNull Call<com.aula.volta.data.model.UserResponseDTO> call,
                                                       @NonNull Response<com.aula.volta.data.model.UserResponseDTO> meResp) {
                                    if (meResp.isSuccessful() && meResp.body() != null) {
                                        com.aula.volta.data.model.UserResponseDTO u = meResp.body();
                                        if (u.getId() != null && !u.getId().trim().isEmpty()) {
                                            SessionManager.saveUserId(appContext, u.getId().trim());
                                        }
                                        if (u.getCompanyId() != null && !u.getCompanyId().trim().isEmpty()) {
                                            SessionManager.saveCompanyId(appContext, u.getCompanyId().trim());
                                        }
                                        if (u.getName() != null && !u.getName().trim().isEmpty()) {
                                            SessionManager.saveName(appContext, u.getName().trim());
                                        }
                                        if (u.getPosition() != null && !u.getPosition().trim().isEmpty()) {
                                            SessionManager.saveSector(appContext, u.getPosition().trim());
                                        }
                                        if (u.getPhotoUrl() != null && !u.getPhotoUrl().trim().isEmpty()) {
                                            SessionManager.saveProfilePhotoUrl(appContext, email, u.getPhotoUrl().trim());
                                        }
                                    }
                                    completeLogin();
                                }

                                @Override
                                public void onFailure(@NonNull Call<com.aula.volta.data.model.UserResponseDTO> call, @NonNull Throwable t) {
                                    completeLogin();
                                }
                            });
                        } catch (Exception ignored) {
                            completeLogin();
                        }
                    } else {
                        Log.w(TAG, "Falha de autenticação (HTTP " + response.code() + "). Rejeitando login.");
                        setLoading(false);
                        if (isAdded() && getContext() != null) {
                            Toast.makeText(requireContext(), R.string.auth_invalid, Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                    Log.w(TAG, "API offline ou inalcançável no login: " + t.getMessage());
                    setLoading(false);
                    if (!isAdded()) {
                        return;
                    }
                    boolean isOffline = !com.aula.volta.util.NetworkUtils.isOnline(appContext);
                    showOfflineError(v, isOffline);
                }

                private void completeLogin() {
                    if (isAdded() && getActivity() != null) {
                        ((AuthActivity) requireActivity()).finishLogin();
                    }
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "Erro ao iniciar autenticação", e);
            setLoading(false);
            if (isAdded() && getContext() != null) {
                Toast.makeText(requireContext(), R.string.auth_invalid, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showOfflineError(View v, boolean isOffline) {
        if (!isAdded()) {
            return;
        }
        Bundle args = new Bundle();
        if (isOffline) {
            args.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_ERROR_TYPE,
                    com.aula.volta.ui.error.OfflineErrorFragment.TYPE_OFFLINE);
            args.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_BADGE, "Você está offline");
            args.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_TITLE, "Sem conexão por aqui");
            args.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_MESSAGE,
                    "Você precisa de uma conexão com a internet para entrar no VOLTA. Confira sua rede e tente de novo.");
            args.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_INFO_TITLE, "Login bloqueado sem internet");
            args.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_INFO_BODY,
                    "A autenticação valida suas credenciais diretamente no banco de dados da unidade.");
        } else {
            args.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_ERROR_TYPE,
                    com.aula.volta.ui.error.OfflineErrorFragment.TYPE_SERVICE_UNAVAILABLE);
            args.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_BADGE, "Fora do ar");
            args.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_TITLE, "O VOLTA não respondeu");
            args.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_MESSAGE,
                    "Não é nada com você: o serviço está fora do ar ou demorou demais pra responder. Tente de novo em alguns minutos.");
        }
        View navTarget = v != null ? v : getView();
        if (navTarget != null) {
            try {
                Navigation.findNavController(navTarget).navigate(R.id.auth_offline_error, args);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Sem conexão com a internet.", Toast.LENGTH_SHORT).show();
            }
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