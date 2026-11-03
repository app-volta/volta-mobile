package com.aula.volta.ui.auth;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
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
import com.aula.volta.data.api.CatalogAPI;
import com.aula.volta.data.api.ChatbotAuthClient;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.local.SessionManager;
import com.aula.volta.data.model.AreaResponseDTO;
import com.aula.volta.data.model.UserResponseDTO;
import com.aula.volta.data.repository.AreaRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FirstAccessProfileFragment extends Fragment {

    private static final String TAG = "FirstAccessProfileFrag";

    private EditText etName;
    private EditText etEmail;
    private View cardSector;
    private TextView tvSector;
    private CheckBox cbTerms;
    private TextView tvTerms;
    private AppCompatButton btnConfirm;
    private ProgressBar pbLoading;

    private String email = "";
    private String password = "";
    private String name = "";
    private String selectedSector = "";
    private final List<String> availableAreas = new ArrayList<>();
    private boolean isLoadingAreas = false;

    public FirstAccessProfileFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_first_access_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            email = getArguments().getString("email", "");
            password = getArguments().getString("password", "");
            name = getArguments().getString("name", "");
        }

        etName = view.findViewById(R.id.etFirstAccessName);
        etEmail = view.findViewById(R.id.etFirstAccessEmail);
        cardSector = view.findViewById(R.id.cardFirstAccessSector);
        tvSector = view.findViewById(R.id.tvFirstAccessSector);
        cbTerms = view.findViewById(R.id.cbFirstAccessTerms);
        tvTerms = view.findViewById(R.id.tvFirstAccessTerms);
        btnConfirm = view.findViewById(R.id.btnConfirmFirstAccess);
        pbLoading = view.findViewById(R.id.pbFirstAccessProfileLoading);

        View btnClose = view.findViewById(R.id.btnCloseFirstAccess);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> confirmCancel());
        }

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        confirmCancel();
                    }
                });

        if (email.isEmpty()) {
            email = SessionManager.getEmail(requireContext().getApplicationContext());
        }
        if (name.isEmpty()) {
            name = SessionManager.getName(requireContext().getApplicationContext());
        }

        if (etEmail != null && !email.isEmpty()) {
            etEmail.setText(email);
        }
        if (etName != null && !name.isEmpty()) {
            etName.setText(name);
        }

        if (selectedSector.isEmpty()) {
            String savedSector = SessionManager.getSector(requireContext().getApplicationContext());
            if (savedSector != null && !savedSector.trim().isEmpty()) {
                selectedSector = savedSector.trim();
                if (tvSector != null) {
                    tvSector.setText(selectedSector);
                    tvSector.setTextColor(requireContext().getColor(R.color.vf_text_primary));
                }
            }
        }

        if (tvTerms != null) {
            tvTerms.setText(Html.fromHtml(getString(R.string.auth_register_terms), Html.FROM_HTML_MODE_COMPACT));
            tvTerms.setOnClickListener(v -> {
                if (cbTerms != null) {
                    cbTerms.setChecked(!cbTerms.isChecked());
                }
            });
        }

        loadAreasFromApi();

        if (cardSector != null) {
            cardSector.setOnClickListener(v -> showSectorSelectionDialog());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> attemptSaveProfile(v));
        }

        fetchUserDataIfEmpty();
    }

    private void fetchUserDataIfEmpty() {
        if (!isAdded()) return;
        Context appContext = requireContext().getApplicationContext();
        try {
            AuthAPI authApi = ApiClient.get(appContext).create(AuthAPI.class);
            authApi.getMe().enqueue(new Callback<UserResponseDTO>() {
                @Override
                public void onResponse(@NonNull Call<UserResponseDTO> call, @NonNull Response<UserResponseDTO> response) {
                    if (response.isSuccessful() && response.body() != null && isAdded()) {
                        UserResponseDTO u = response.body();
                        if (u.getName() != null && !u.getName().trim().isEmpty() && etName != null) {
                            if (etName.getText().toString().trim().isEmpty()) {
                                etName.setText(u.getName().trim());
                            }
                        }
                        if (u.getPosition() != null && !u.getPosition().trim().isEmpty() && tvSector != null) {
                            if (selectedSector.isEmpty()) {
                                selectedSector = u.getPosition().trim();
                                tvSector.setText(selectedSector);
                                tvSector.setTextColor(requireContext().getColor(R.color.vf_text_primary));
                            }
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<UserResponseDTO> call, @NonNull Throwable t) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void loadAreasFromApi() {
        if (!isAdded()) return;
        final Context appContext = requireContext().getApplicationContext();
        List<String> cachedNames = AreaRepository.getSectorNames(appContext);
        if (!cachedNames.isEmpty()) {
            availableAreas.clear();
            availableAreas.addAll(cachedNames);
        }
        isLoadingAreas = true;
        AreaRepository.loadAreas(appContext, areas -> {
            if (!isAdded()) return;
            isLoadingAreas = false;
            if (areas != null && !areas.isEmpty()) {
                availableAreas.clear();
                for (AreaResponseDTO area : areas) {
                    if (area.getSectorName() != null && !area.getSectorName().trim().isEmpty()) {
                        String sector = area.getSectorName().trim();
                        if (!availableAreas.contains(sector)) {
                            availableAreas.add(sector);
                        }
                    }
                }
            }
            if (availableAreas.isEmpty()) {
                populateDefaultAreas();
            }
        });
    }

    private void populateDefaultAreas() {
        if (!isAdded()) return;
        availableAreas.clear();
        String[] defaults = getResources().getStringArray(R.array.occurrence_sector_options);
        for (String s : defaults) {
            availableAreas.add(s);
        }
    }

    private void showSectorSelectionDialog() {
        if (!isAdded()) return;
        if (isLoadingAreas && availableAreas.isEmpty()) {
            Toast.makeText(requireContext(), R.string.auth_first_access_area_loading, Toast.LENGTH_SHORT).show();
            return;
        }
        if (availableAreas.isEmpty()) {
            populateDefaultAreas();
        }
        final String[] items = availableAreas.toArray(new String[0]);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.auth_first_access_sector_hint)
                .setItems(items, (dialog, which) -> {
                    selectedSector = items[which];
                    if (tvSector != null && isAdded()) {
                        tvSector.setText(selectedSector);
                        tvSector.setTextColor(requireContext().getColor(R.color.vf_text_primary));
                    }
                })
                .show();
    }

    private void setLoading(boolean loading) {
        if (!isAdded()) return;
        if (btnConfirm != null) {
            btnConfirm.setEnabled(!loading);
            btnConfirm.setText(loading ? "" : getString(R.string.auth_first_access_confirm_enter));
        }
        if (pbLoading != null) {
            pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (etName != null) etName.setEnabled(!loading);
        if (cardSector != null) cardSector.setEnabled(!loading);
        if (cbTerms != null) cbTerms.setEnabled(!loading);
    }

    private void attemptSaveProfile(View v) {
        final String inputName = etName != null ? etName.getText().toString().trim() : "";
        if (inputName.isEmpty()) {
            Toast.makeText(requireContext(), R.string.auth_first_access_name_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedSector.isEmpty()) {
            Toast.makeText(requireContext(), R.string.auth_register_sector_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (cbTerms != null && !cbTerms.isChecked()) {
            Toast.makeText(requireContext(), R.string.auth_register_terms_required, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        final Context appContext = requireContext().getApplicationContext();

        try {
            AuthAPI authApi = ApiClient.get(appContext).create(AuthAPI.class);
            JsonObject body = new JsonObject();
            body.addProperty("name", inputName);
            body.addProperty("position", selectedSector);
            body.addProperty("firstLogin", false);
            body.addProperty("isFirstLogin", false);
            body.addProperty("primeiroLogin", false);
            body.addProperty("first_login", false);

            authApi.updateMe(body).enqueue(new Callback<UserResponseDTO>() {
                @Override
                public void onResponse(@NonNull Call<UserResponseDTO> call, @NonNull Response<UserResponseDTO> response) {
                    if (response.isSuccessful()) {
                        setLoading(false);
                        completeProfileUpdate(v, inputName);
                    } else if (response.code() == 400) {
                        JsonObject fallbackBody = new JsonObject();
                        fallbackBody.addProperty("name", inputName);
                        fallbackBody.addProperty("position", selectedSector);
                        authApi.updateMe(fallbackBody).enqueue(new Callback<UserResponseDTO>() {
                            @Override
                            public void onResponse(@NonNull Call<UserResponseDTO> call2, @NonNull Response<UserResponseDTO> response2) {
                                setLoading(false);
                                if (response2.isSuccessful()) {
                                    completeProfileUpdate(v, inputName);
                                } else {
                                    Toast.makeText(requireContext(), "Erro ao atualizar dados. Verifique as informações.", Toast.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<UserResponseDTO> call2, @NonNull Throwable t2) {
                                setLoading(false);
                                completeProfileUpdate(v, inputName);
                            }
                        });
                    } else {
                        setLoading(false);
                        completeProfileUpdate(v, inputName);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<UserResponseDTO> call, @NonNull Throwable t) {
                    Log.w(TAG, "Falha na requisição updateMe: " + t.getMessage());
                    setLoading(false);
                    completeProfileUpdate(v, inputName);
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "Exceção ao chamar updateMe", e);
            setLoading(false);
            completeProfileUpdate(v, inputName);
        }
    }

    private void completeProfileUpdate(View v, String finalName) {
        if (!isAdded()) return;
        final Context appContext = requireContext().getApplicationContext();

        SessionManager.saveName(appContext, finalName);
        SessionManager.saveSector(appContext, selectedSector);
        SessionManager.setFirstLoginPending(appContext, false);

        Bundle bundle = new Bundle();
        bundle.putString("name", finalName);

        View target = v != null ? v : getView();
        if (target != null) {
            try {
                Navigation.findNavController(target).navigate(R.id.auth_first_access_success, bundle);
            } catch (Exception e) {
                Log.e(TAG, "Erro ao navegar para a tela de sucesso do primeiro acesso", e);
            }
        }
    }

    private void confirmCancel() {
        if (!isAdded()) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sair do primeiro acesso?")
                .setMessage("Se sair agora, você precisará entrar novamente.")
                .setPositiveButton("Sair", (dialog, which) -> {
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
                })
                .setNegativeButton("Continuar", null)
                .show();
    }
}
