package com.aula.volta;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.ReportAPI;
import com.aula.volta.data.local.NotificationStore;
import com.aula.volta.data.model.Notification;
import com.aula.volta.data.local.PrefsHelper;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Perfil — Figma 790:3470 (header, sobre você, configurações, sair).
 * Números vêm do mesmo GET /reports/summary da Home/Relatórios.
 * Sair: auth chega depois — por enquanto confirma e avisa.
 */
public class ProfileFragment extends Fragment {

    private static final String PREFS_REPORTS = "cache_reports";
    private static final String KEY_SUMMARY = "summary";

    private TextView tvOccurrences;
    private TextView tvRecovered;
    private TextView tvNotifCount;

    public ProfileFragment() {
        // Construtor vazio obrigatório
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvOccurrences = view.findViewById(R.id.tvProfileOccurrences);
        tvRecovered = view.findViewById(R.id.tvProfileRecovered);
        tvNotifCount = view.findViewById(R.id.tvProfileNotifCount);

        view.findViewById(R.id.btnEditProfile).setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.occ_soon,
                        Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.rowProfileNotifications).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.nav_notifications));

        view.findViewById(R.id.rowProfileUnit).setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.occ_soon,
                        Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.rowProfileHelp).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.nav_assistant));

        view.findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutDialog());

        loadNumbers();
        loadNotifCount();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNotifCount();
    }

    private void loadNumbers() {
        ReportAPI api = ApiClient.get(requireContext()).create(ReportAPI.class);
        api.getSummary().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    PrefsHelper.putJson(requireContext(), PREFS_REPORTS, KEY_SUMMARY,
                            response.body().toString());
                    applyNumbers(response.body());
                } else {
                    loadNumbersFromCache();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                loadNumbersFromCache();
            }
        });
    }

    private void loadNumbersFromCache() {
        String json = PrefsHelper.getJson(requireContext(), PREFS_REPORTS, KEY_SUMMARY);
        if (json != null) {
            try {
                applyNumbers(com.google.gson.JsonParser.parseString(json).getAsJsonObject());
            } catch (Exception ignored) {
                // mantém placeholders
            }
        }
    }

    private void applyNumbers(JsonObject summary) {
        int total = 0;
        int kg = 0;
        try {
            if (summary.has("abertas")) {
                total += summary.get("abertas").getAsInt();
            }
            if (summary.has("resolvidas")) {
                total += summary.get("resolvidas").getAsInt();
            }
            if (summary.has("kg_reciclados")) {
                kg = summary.get("kg_reciclados").getAsInt();
            }
        } catch (Exception ignored) {
            // usa zeros
        }
        tvOccurrences.setText(getString(R.string.profile_occ_value, total));
        tvRecovered.setText(getString(R.string.profile_recovered_value,
                String.format(new Locale("pt", "BR"), "%,d", kg)));
    }

    private void loadNotifCount() {
        NotificationStore.refresh(requireContext(), this::applyNotifCount);
    }

    private void applyNotifCount(List<Notification> notifications) {
        if (!isAdded() || tvNotifCount == null) {
            return;
        }
        int unread = NotificationStore.unreadCount(notifications);
        if (unread > 0) {
            tvNotifCount.setVisibility(View.VISIBLE);
            tvNotifCount.setText(getString(R.string.notifications_new, unread));
        } else {
            tvNotifCount.setVisibility(View.GONE);
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_logout_title)
                .setMessage(R.string.profile_logout_msg)
                .setPositiveButton(R.string.profile_logout, (dialog, which) -> {
                    com.aula.volta.data.local.SessionManager.logout(requireContext());
                    android.content.Intent intent = new android.content.Intent(
                            requireContext(), com.aula.volta.ui.auth.AuthActivity.class);
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton(R.string.pick_confirm_no, null)
                .show();
    }
}
