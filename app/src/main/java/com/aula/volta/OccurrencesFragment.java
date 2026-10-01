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
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.model.Occurrence;
import com.aula.volta.data.model.OccurrenceJSON;
import com.aula.volta.ui.occurrences.OccurrencesFilterSheet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Lista completa de ocorrências (aberta via "Ver todas" da Home).
 * Mesmo adapter/cards da Home. Modal Filtrar chega na Fase 5 (feat/occurrences).
 */
public class OccurrencesFragment extends Fragment {

    private static final String PREFS_OCCURRENCES = "cache_occurrences";
    private static final String KEY_OCCURRENCE_LIST = "list";

    private ProgressBar loadingOccurrences;
    private TextView tvEmptyOccurrencesList;
    private OccurrenceAdapter adapter;
    private final List<Occurrence> allOccurrences = new ArrayList<>();
    private final ArrayList<String> filterMaterials = new ArrayList<>();
    private final ArrayList<String> filterStatuses = new ArrayList<>();

    public OccurrencesFragment() {
        // Construtor vazio obrigatório
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_occurrences, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadingOccurrences = view.findViewById(R.id.loadingOccurrences);
        tvEmptyOccurrencesList = view.findViewById(R.id.tvEmptyOccurrencesList);

        RecyclerView rvOccurrences = view.findViewById(R.id.rvOccurrences);
        adapter = new OccurrenceAdapter(occurrence -> {
            Bundle args = new Bundle();
            args.putString("occurrenceId", occurrence.getId());
            Navigation.findNavController(requireView()).navigate(R.id.nav_occurrence_full, args);
        });
        rvOccurrences.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOccurrences.setAdapter(adapter);

        View btnFilter = view.findViewById(R.id.btnFilterOccurrences);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> {
                OccurrencesFilterSheet sheet = OccurrencesFilterSheet.newInstance(
                        new ArrayList<>(filterMaterials), new ArrayList<>(filterStatuses));
                sheet.show(getParentFragmentManager(), "filter");
            });
        }

        getParentFragmentManager().setFragmentResultListener(
                OccurrencesFilterSheet.REQUEST_KEY, this, (requestKey, result) -> {
                    filterMaterials.clear();
                    filterStatuses.clear();
                    ArrayList<String> mats =
                            result.getStringArrayList(OccurrencesFilterSheet.ARG_MATERIALS);
                    ArrayList<String> stats =
                            result.getStringArrayList(OccurrencesFilterSheet.ARG_STATUSES);
                    if (mats != null) {
                        filterMaterials.addAll(mats);
                    }
                    if (stats != null) {
                        filterStatuses.addAll(stats);
                    }
                    applyFilter();
                });

        loadOccurrences();
    }

    private void loadOccurrences() {
        OccurrenceAPI api = ApiClient.get(requireContext()).create(OccurrenceAPI.class);
        api.getOccurrences().enqueue(new Callback<List<OccurrenceJSON>>() {
            @Override
            public void onResponse(Call<List<OccurrenceJSON>> call,
                                   Response<List<OccurrenceJSON>> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    List<Occurrence> occurrences = new ArrayList<>();
                    for (OccurrenceJSON json : response.body()) {
                        occurrences.add(Occurrence.fromJson(json));
                    }
                    PrefsHelper.putList(requireContext(), PREFS_OCCURRENCES,
                            KEY_OCCURRENCE_LIST, occurrences);
                    applyOccurrences(occurrences);
                } else {
                    applyOccurrences(cachedOccurrences());
                }
                hideLoading();
            }

            @Override
            public void onFailure(Call<List<OccurrenceJSON>> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                applyOccurrences(cachedOccurrences());
                Toast.makeText(requireContext(), R.string.offline_cache, Toast.LENGTH_LONG).show();
                hideLoading();
            }
        });
    }

    private List<Occurrence> cachedOccurrences() {
        return PrefsHelper.getList(requireContext(), PREFS_OCCURRENCES, KEY_OCCURRENCE_LIST,
                PrefsHelper.listType(Occurrence.class));
    }

    private void applyOccurrences(List<Occurrence> occurrences) {
        allOccurrences.clear();
        if (occurrences != null) {
            allOccurrences.addAll(occurrences);
        }
        applyFilter();
    }

    /** Filtra localmente (mock); depois vira query param da API. Vazio = mostra tudo. */
    private void applyFilter() {
        List<Occurrence> filtered = new ArrayList<>();
        for (Occurrence o : allOccurrences) {
            if (!filterMaterials.isEmpty() && !matchesMaterial(o)) {
                continue;
            }
            if (!filterStatuses.isEmpty() && !filterStatuses.contains(o.getStatus())) {
                continue;
            }
            filtered.add(o);
        }
        adapter.setItems(filtered);
        if (tvEmptyOccurrencesList != null) {
            tvEmptyOccurrencesList.setVisibility(
                    filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private boolean matchesMaterial(Occurrence o) {
        String material = o.getMaterial() == null ? ""
                : o.getMaterial().toLowerCase(Locale.ROOT);
        for (String filter : filterMaterials) {
            if (material.contains(filter.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private void hideLoading() {
        if (loadingOccurrences != null) {
            loadingOccurrences.setVisibility(View.GONE);
        }
    }
}
