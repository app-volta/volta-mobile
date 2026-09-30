package com.aula.volta;

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

import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.OccurrenceAPI;
import com.aula.volta.data.api.ReportAPI;
import com.aula.volta.data.local.NotificationStore;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.model.Notification;
import com.aula.volta.data.model.Occurrence;
import com.aula.volta.data.model.OccurrenceJSON;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final String PREFS_OCCURRENCES = "cache_occurrences";
    private static final String KEY_OCCURRENCE_LIST = "list";
    private static final String PREFS_REPORTS = "cache_reports";
    private static final String KEY_SUMMARY = "summary";

    private TextView tvStatOpenCount;
    private TextView tvStatResolvedCount;
    private TextView tvStatRecycledCount;
    private ProgressBar loadingHome;
    private TextView tvEmptyOccurrences;
    private View notificationDot;
    private OccurrenceAdapter adapter;

    /** Contador das 3 cargas (resumo, ocorrências, notificações) para esconder o loading. */
    private int pendingLoads = 3;

    public HomeFragment() {
        // Construtor vazio obrigatório
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Infla o layout do fragmento
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvStatOpenCount = view.findViewById(R.id.tvStatOpenCount);
        tvStatResolvedCount = view.findViewById(R.id.tvStatResolvedCount);
        tvStatRecycledCount = view.findViewById(R.id.tvStatRecycledCount);
        notificationDot = view.findViewById(R.id.notificationDot);

        // _________________________________________________________________________________________
        //            AÇÕES DA HOME (navegação interna, sem item na bottom bar)
        // _________________________________________________________________________________________

        // Botão do banner CTA para abrir o registro de ocorrência
        MaterialButton btnCtaRegister = view.findViewById(R.id.btnCtaRegister);
        if (btnCtaRegister != null) {
            btnCtaRegister.setOnClickListener(v -> {
                Navigation.findNavController(v).navigate(R.id.nav_register);
            });
        }

        // Link "Ver todas" para navegar para a aba de Ocorrências
        TextView tvSeeAll = view.findViewById(R.id.tvSeeAllOccurrences);
        if (tvSeeAll != null) {
            tvSeeAll.setOnClickListener(v -> {
                Navigation.findNavController(v).navigate(R.id.nav_occurrences);
            });
        }

        // Sino abre as notificações (bottom sheet da Fase 7; stub na Fase 0)
        View btnNotifications = view.findViewById(R.id.btnNotifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                Navigation.findNavController(v).navigate(R.id.nav_notifications);
            });
        }

        // _________________________________________________________________________________________
        //            LISTA DINÂMICA (Fase 2 — mesmo visual Figma via adapter)
        // _________________________________________________________________________________________

        RecyclerView rvRecent = view.findViewById(R.id.rvRecentOccurrences);
        loadingHome = view.findViewById(R.id.loadingHome);
        tvEmptyOccurrences = view.findViewById(R.id.tvEmptyOccurrences);

        adapter = new OccurrenceAdapter(occurrence -> {
            Bundle args = new Bundle();
            args.putString("occurrenceId", occurrence.getId());
            Navigation.findNavController(requireView()).navigate(R.id.nav_occurrence_detail, args);
        });
        rvRecent.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecent.setNestedScrollingEnabled(false);
        rvRecent.setAdapter(adapter);

        loadSummary();
        loadOccurrences();
        loadNotificationDot();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Atualiza o dot ao voltar do sheet de notificações (marcar como lidas)
        loadNotificationDot();
    }

    /** Resumo da unidade via GET /reports/summary (mock → API-ready). */
    private void loadSummary() {
        ReportAPI api = ApiClient.get(requireContext()).create(ReportAPI.class);
        api.getSummary().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject summary = response.body();
                    PrefsHelper.putJson(requireContext(), PREFS_REPORTS, KEY_SUMMARY,
                            summary.toString());
                    applySummary(summary);
                } else {
                    loadSummaryFromCache();
                }
                onLoadFinished();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                loadSummaryFromCache();
                Toast.makeText(requireContext(), R.string.offline_cache, Toast.LENGTH_LONG).show();
                onLoadFinished();
            }
        });
    }

    private void loadSummaryFromCache() {
        String json = PrefsHelper.getJson(requireContext(), PREFS_REPORTS, KEY_SUMMARY);
        if (json != null) {
            try {
                applySummary(com.google.gson.JsonParser.parseString(json).getAsJsonObject());
            } catch (Exception ignored) {
                // cache corrompido: mantém valores do layout
            }
        }
    }

    private void applySummary(JsonObject summary) {
        if (tvStatOpenCount != null && summary.has("abertas")) {
            tvStatOpenCount.setText(String.valueOf(summary.get("abertas").getAsInt()));
        }
        if (tvStatResolvedCount != null && summary.has("resolvidas")) {
            tvStatResolvedCount.setText(String.valueOf(summary.get("resolvidas").getAsInt()));
        }
        if (tvStatRecycledCount != null && summary.has("kg_reciclados")) {
            int kg = summary.get("kg_reciclados").getAsInt();
            tvStatRecycledCount.setText(String.format(new Locale("pt", "BR"), "%,d", kg));
        }
    }

    /** Ocorrências recentes via GET /occurrences (mock → API-ready). */
    private void loadOccurrences() {
        OccurrenceAPI api = ApiClient.get(requireContext()).create(OccurrenceAPI.class);
        api.getOccurrences().enqueue(new Callback<List<OccurrenceJSON>>() {
            @Override
            public void onResponse(Call<List<OccurrenceJSON>> call,
                                   Response<List<OccurrenceJSON>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Occurrence> occurrences = new ArrayList<>();
                    for (OccurrenceJSON json : response.body()) {
                        occurrences.add(Occurrence.fromJson(json));
                    }
                    PrefsHelper.putList(requireContext(), PREFS_OCCURRENCES,
                            KEY_OCCURRENCE_LIST, occurrences);
                    // Home exibe só as 3 mais recentes (mock ordenado); "Ver todas" abre a lista
                    applyOccurrences(recentOnly(occurrences));
                } else {
                    applyOccurrences(cachedOccurrences());
                }
                onLoadFinished();
            }

            @Override
            public void onFailure(Call<List<OccurrenceJSON>> call, Throwable t) {
                applyOccurrences(cachedOccurrences());
                Toast.makeText(requireContext(), R.string.offline_cache, Toast.LENGTH_LONG).show();
                onLoadFinished();
            }
        });
    }

    private List<Occurrence> cachedOccurrences() {
        return recentOnly(PrefsHelper.getList(requireContext(), PREFS_OCCURRENCES,
                KEY_OCCURRENCE_LIST, PrefsHelper.listType(Occurrence.class)));
    }

    /** Home mostra só as 3 mais recentes; a lista completa vive em nav_occurrences. */
    private List<Occurrence> recentOnly(List<Occurrence> occurrences) {
        if (occurrences == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(occurrences.subList(0, Math.min(3, occurrences.size())));
    }

    private void applyOccurrences(List<Occurrence> occurrences) {
        adapter.setItems(occurrences);
        if (tvEmptyOccurrences != null) {
            tvEmptyOccurrences.setVisibility(
                    occurrences == null || occurrences.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    /** Dot do sino = não lidas via NotificationStore (Fase 7 deixa a lista real). */
    private void loadNotificationDot() {
        NotificationStore.refresh(requireContext(), notifications -> {
            int unread = NotificationStore.unreadCount(notifications);
            if (notificationDot != null) {
                notificationDot.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
            }
            onLoadFinished();
        });
    }

    private void onLoadFinished() {
        pendingLoads--;
        if (pendingLoads <= 0 && loadingHome != null) {
            loadingHome.setVisibility(View.GONE);
        }
    }
}
