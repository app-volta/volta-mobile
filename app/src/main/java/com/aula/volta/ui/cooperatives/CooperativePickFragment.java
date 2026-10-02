package com.aula.volta.ui.cooperatives;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aula.volta.CooperativeAdapter;
import com.aula.volta.R;
import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.CooperativeAPI;
import com.aula.volta.data.api.OccurrenceAPI;
import com.aula.volta.data.local.AuditLog;
import com.aula.volta.data.local.NotificationStore;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.model.CooperativeJSON;
import com.aula.volta.data.model.Notification;
import com.aula.volta.data.model.Occurrence;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fase 8 — Destinação contextual: cooperativas compatíveis COM a ocorrência.
 * App só apresenta a lista ordenada do backend/AIC (nunca calcula score).
 * Arg: occurrenceId.
 */
public class CooperativePickFragment extends Fragment {

    private static final String PREFS = "cache_pick";

    private TextView tvPickContext;
    private ProgressBar loadingPick;
    private TextView tvEmptyPick;
    private CooperativeAdapter adapter;
    private String occurrenceId;

    public CooperativePickFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cooperative_pick, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        occurrenceId = getArguments() != null ? getArguments().getString("occurrenceId") : null;
        if (occurrenceId == null || occurrenceId.isEmpty()) {
            Navigation.findNavController(view).navigateUp();
            return;
        }

        tvPickContext = view.findViewById(R.id.tvPickContext);
        loadingPick = view.findViewById(R.id.loadingPick);
        tvEmptyPick = view.findViewById(R.id.tvEmptyPick);

        view.findViewById(R.id.btnBackPick).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        RecyclerView rvPick = view.findViewById(R.id.rvPickCooperatives);
        adapter = new CooperativeAdapter();
        rvPick.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPick.setNestedScrollingEnabled(false);
        rvPick.setAdapter(adapter);

        loadContext();
        loadCooperatives();
    }

    private void loadContext() {
        OccurrenceAPI api = ApiClient.get(requireContext()).create(OccurrenceAPI.class);
        api.getOccurrence(occurrenceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject detail = response.body();
                    String titulo = opt(detail, "titulo");
                    String setor = opt(detail, "setor");
                    tvPickContext.setText("#" + occurrenceId + " · " + titulo + " · " + setor);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                // contexto opcional: lista carrega mesmo assim
            }
        });
    }

    private String opt(JsonObject detail, String member) {
        return detail.has(member) && !detail.get(member).isJsonNull()
                ? detail.get(member).getAsString() : "";
    }

    private void loadCooperatives() {
        CooperativeAPI api = ApiClient.get(requireContext()).create(CooperativeAPI.class);
        api.getRecommended(occurrenceId).enqueue(new Callback<List<CooperativeJSON>>() {
            @Override
            public void onResponse(Call<List<CooperativeJSON>> call,
                                   Response<List<CooperativeJSON>> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    PrefsHelper.putList(requireContext(), PREFS, occurrenceId, response.body());
                    applyCooperatives(response.body());
                } else {
                    applyCooperatives(cached());
                }
                hideLoading();
            }

            @Override
            public void onFailure(Call<List<CooperativeJSON>> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                applyCooperatives(cached());
                Toast.makeText(requireContext(), R.string.offline_cache, Toast.LENGTH_LONG).show();
                hideLoading();
            }
        });
    }

    private List<CooperativeJSON> cached() {
        return PrefsHelper.getList(requireContext(), PREFS, occurrenceId,
                PrefsHelper.listType(CooperativeJSON.class));
    }

    private void applyCooperatives(List<CooperativeJSON> cooperatives) {
        adapter.setOnItemClickListener(this::confirmRequest);
        adapter.setItems(cooperatives);
        tvEmptyPick.setVisibility(
                cooperatives == null || cooperatives.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void confirmRequest(CooperativeJSON coop) {
        if (!isAdded() || getContext() == null) {
            return;
        }
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_confirm_destination, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvMsg = dialogView.findViewById(R.id.tvConfirmDestMessage);
        TextView tvCoop = dialogView.findViewById(R.id.tvConfirmDestCoopName);
        TextView tvDetails = dialogView.findViewById(R.id.tvConfirmDestDetails);

        tvMsg.setText(getString(R.string.pick_confirm_msg, coop.getNome()));
        tvCoop.setText(coop.getNome());
        String info = (coop.getDistanciaKm() > 0 ? String.format(java.util.Locale.getDefault(), "%.1f km da fábrica", coop.getDistanciaKm()) : "Coleta prioritária")
                + (coop.getColeta() != null && !coop.getColeta().isEmpty() ? " • " + coop.getColeta() : "");
        tvDetails.setText(info);

        dialogView.findViewById(R.id.btnConfirmDestNo).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnConfirmDestYes).setOnClickListener(v -> {
            dialog.dismiss();

            AuditLog.append(requireContext(), "Breno Gomes", "SOLICITAR_DESTINACAO",
                    "ocorrencia", occurrenceId, null, coop.getNome());

            NotificationStore.pushLocal(requireContext(), new Notification(
                    "local_" + System.currentTimeMillis(),
                    "COLETA_SOLICITADA",
                    getString(R.string.notif_pick_title, coop.getNome()),
                    getString(R.string.notif_pick_desc, occurrenceId),
                    getString(R.string.notif_now),
                    "truck",
                    "#3B82F6",
                    "#E4EFFF",
                    false,
                    occurrenceId));

            // Disparo de notificação nativa Android
            com.aula.volta.data.notification.NotificationHelper.notifyDestinationRequested(
                    requireContext(), coop.getNome(), occurrenceId);

            // Atualização de status da ocorrência para EM_TRATAMENTO / Destinada
            try {
                String cached = PrefsHelper.getJson(requireContext(), "cache_occurrence_detail", occurrenceId);
                if (cached != null) {
                    JsonObject obj = com.google.gson.JsonParser.parseString(cached).getAsJsonObject();
                    obj.addProperty("status", "EM_TRATAMENTO");
                    PrefsHelper.putJson(requireContext(), "cache_occurrence_detail", occurrenceId, obj.toString());

                    Occurrence occ = new Occurrence(
                            occurrenceId,
                            opt(obj, "titulo"),
                            opt(obj, "setor"),
                            opt(obj, "quando"),
                            opt(obj, "prioridade"),
                            "EM_TRATAMENTO",
                            opt(obj, "material"));
                    com.aula.volta.data.local.OccurrenceStore.update(requireContext(), occ);
                }
            } catch (Exception ignored) {
            }

            Toast.makeText(requireContext(), R.string.pick_request_sent,
                    Toast.LENGTH_LONG).show();
            Navigation.findNavController(requireView()).navigateUp();
        });

        dialog.show();
    }

    private void hideLoading() {
        if (loadingPick != null) {
            loadingPick.setVisibility(View.GONE);
        }
    }
}
