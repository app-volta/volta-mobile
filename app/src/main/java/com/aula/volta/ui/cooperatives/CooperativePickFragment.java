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
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.model.CooperativeJSON;
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
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.pick_confirm_title)
                .setMessage(getString(R.string.pick_confirm_msg, coop.getNome()))
                .setPositiveButton(R.string.pick_confirm_yes, (dialog, which) -> {
                    AuditLog.append(requireContext(), "Breno Gomes", "SOLICITAR_DESTINACAO",
                            "ocorrencia", occurrenceId, null, coop.getNome());
                    Toast.makeText(requireContext(), R.string.pick_request_sent,
                            Toast.LENGTH_LONG).show();
                    Navigation.findNavController(requireView()).navigateUp();
                })
                .setNegativeButton(R.string.pick_confirm_no, null)
                .show();
    }

    private void hideLoading() {
        if (loadingPick != null) {
            loadingPick.setVisibility(View.GONE);
        }
    }
}
