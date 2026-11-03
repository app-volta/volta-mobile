package com.aula.volta.ui.auth;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
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
import com.aula.volta.data.model.auth.ChangePasswordRequestDTO;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FirstAccessPasswordFragment extends Fragment {

    private static final String TAG = "FirstAccessPwFragment";

    private EditText etNew;
    private EditText etConfirm;
    private View[] segments;
    private TextView tvStrength;
    private ImageView ivConfirmMatch;
    private ImageView btnToggleNew;
    private AppCompatButton btnSavePassword;
    private ProgressBar pbLoading;
    private boolean passwordVisible;

    private View cardConfirm;
    private View chipConfirm;
    private ImageView ivConfirmLock;

    private String email = "";
    private String tempPassword = "";
    private String userName = "";

    public FirstAccessPasswordFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_first_access_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            email = getArguments().getString("email", "");
            tempPassword = getArguments().getString("temp_password", "");
            userName = getArguments().getString("name", "");
        }

        etNew = view.findViewById(R.id.etFirstAccessNewPassword);
        etConfirm = view.findViewById(R.id.etFirstAccessConfirmPassword);
        tvStrength = view.findViewById(R.id.tvStrength);
        cardConfirm = view.findViewById(R.id.cardConfirmPassword);
        chipConfirm = view.findViewById(R.id.chipConfirmPassword);
        ivConfirmLock = view.findViewById(R.id.ivConfirmLock);
        ivConfirmMatch = view.findViewById(R.id.ivConfirmMatch);
        btnToggleNew = view.findViewById(R.id.btnToggleFirstAccessPassword);
        btnSavePassword = view.findViewById(R.id.btnSaveFirstAccessPassword);
        pbLoading = view.findViewById(R.id.pbFirstAccessPasswordLoading);

        segments = new View[]{
                view.findViewById(R.id.strength1),
                view.findViewById(R.id.strength2),
                view.findViewById(R.id.strength3),
                view.findViewById(R.id.strength4)};

        View btnBack = view.findViewById(R.id.btnBackFirstAccessPassword);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> cancelFirstAccess());
        }

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        cancelFirstAccess();
                    }
                });

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

        if (btnSavePassword != null) {
            btnSavePassword.setOnClickListener(v -> attemptSavePassword(v));
        }
    }

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
                chipConfirm.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.vf_green_bg)));
            }
            if (ivConfirmLock != null) {
                ivConfirmLock.setImageTintList(ColorStateList.valueOf(requireContext().getColor(R.color.status_aprovada_text)));
            }
            if (ivConfirmMatch != null) {
                ivConfirmMatch.setVisibility(View.GONE);
            }
        }
    }

    private void setLoading(boolean loading) {
        if (!isAdded()) return;
        if (btnSavePassword != null) {
            btnSavePassword.setEnabled(!loading);
            btnSavePassword.setText(loading ? "" : getString(R.string.auth_first_access_save_continue));
        }
        if (pbLoading != null) {
            pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (etNew != null) etNew.setEnabled(!loading);
        if (etConfirm != null) etConfirm.setEnabled(!loading);
    }

    private void attemptSavePassword(View v) {
        final String newPw = etNew != null ? etNew.getText().toString() : "";
        final String confirmPw = etConfirm != null ? etConfirm.getText().toString() : "";

        if (newPw.length() < 8) {
            Toast.makeText(requireContext(), R.string.auth_register_pw_min, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPw.equals(confirmPw)) {
            Toast.makeText(requireContext(), R.string.auth_pw_mismatch, Toast.LENGTH_SHORT).show();
            return;
        }

        if (tempPassword != null && !tempPassword.isEmpty() && newPw.equals(tempPassword)) {
            Toast.makeText(requireContext(), "A nova senha deve ser diferente da senha temporária.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        final Context appContext = requireContext().getApplicationContext();

        try {
            AuthAPI authApi = ApiClient.get(appContext).create(AuthAPI.class);
            ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO(
                    (tempPassword != null && !tempPassword.isEmpty()) ? tempPassword : "",
                    newPw
            );
            authApi.changePassword(dto).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    setLoading(false);
                    if (response.isSuccessful()) {
                        Log.i(TAG, "Senha alterada com sucesso na API para o primeiro acesso.");
                        proceedToProfile(v, newPw);
                    } else {
                        Log.w(TAG, "Falha na troca de senha (HTTP " + response.code() + ").");
                        if (response.code() == 400) {
                            Toast.makeText(requireContext(), "A nova senha deve ser diferente da atual.", Toast.LENGTH_LONG).show();
                        } else {
                            proceedToProfile(v, newPw);
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Log.w(TAG, "Erro de rede ao trocar senha: " + t.getMessage());
                    setLoading(false);
                    proceedToProfile(v, newPw);
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "Exceção ao chamar changePassword", e);
            setLoading(false);
            proceedToProfile(v, newPw);
        }
    }

    private void proceedToProfile(View v, String newPw) {
        if (!isAdded()) return;
        final Context appContext = requireContext().getApplicationContext();
        SessionManager.savePassword(appContext, newPw);
        tempPassword = newPw;

        Bundle bundle = new Bundle();
        bundle.putString("email", email);
        bundle.putString("password", newPw);
        bundle.putString("name", userName);

        View target = v != null ? v : getView();
        if (target != null) {
            try {
                Navigation.findNavController(target).navigate(R.id.auth_first_access_profile, bundle);
            } catch (Exception e) {
                Log.e(TAG, "Erro ao navegar para o perfil do primeiro acesso", e);
            }
        }
    }

    private void cancelFirstAccess() {
        if (!isAdded()) return;
        Context appContext = requireContext().getApplicationContext();
        SessionManager.logout(appContext);
        ApiClient.reset();
        ChatbotAuthClient.reset();
        View target = getView();
        if (target != null) {
            try {
                Navigation.findNavController(target).popBackStack(R.id.auth_login, false);
            } catch (Exception e) {
                Navigation.findNavController(target).navigate(R.id.auth_login);
            }
        }
    }
}
