package com.aula.volta;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aula.volta.data.filter.OccurrenceFilter;
import com.aula.volta.data.local.OccurrenceStore;
import com.aula.volta.data.model.Occurrence;
import com.aula.volta.ui.occurrences.OccurrencesFilterSheet;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista completa de ocorrências ("Ver todas" da Home) com busca instantânea e filtros compostos (Figma 554:24).
 */
public class OccurrencesFragment extends Fragment {

    private ProgressBar loadingOccurrences;
    private TextView tvEmptyOccurrencesList;
    private TextView tvOccurrencesCount;
    private TextView btnClearFiltersChip;
    private TextView tvFilterBadge;
    private FrameLayout btnFilterOccurrences;
    private EditText etSearchOccurrences;
    private ImageView btnClearSearch;

    private OccurrenceAdapter adapter;
    private final List<Occurrence> allOccurrences = new ArrayList<>();
    private final ArrayList<String> filterMaterials = new ArrayList<>();
    private final ArrayList<String> filterStatuses = new ArrayList<>();
    private String currentSearchQuery = "";

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
        tvOccurrencesCount = view.findViewById(R.id.tvOccurrencesCount);
        btnClearFiltersChip = view.findViewById(R.id.btnClearFiltersChip);
        tvFilterBadge = view.findViewById(R.id.tvFilterBadge);
        btnFilterOccurrences = view.findViewById(R.id.btnFilterOccurrences);
        etSearchOccurrences = view.findViewById(R.id.etSearchOccurrences);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);

        RecyclerView rvOccurrences = view.findViewById(R.id.rvOccurrences);
        adapter = new OccurrenceAdapter(occurrence -> {
            Bundle args = new Bundle();
            args.putString("occurrenceId", occurrence.getId());
            Navigation.findNavController(requireView()).navigate(R.id.nav_occurrence_full, args);
        });
        rvOccurrences.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOccurrences.setAdapter(adapter);

        setupSearchAndFilters();
        loadOccurrences();
    }

    private void setupSearchAndFilters() {
        if (etSearchOccurrences != null) {
            etSearchOccurrences.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s != null ? s.toString() : "";
                    if (btnClearSearch != null) {
                        btnClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    applyFilter();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                if (etSearchOccurrences != null) {
                    etSearchOccurrences.setText("");
                }
            });
        }

        if (btnClearFiltersChip != null) {
            btnClearFiltersChip.setOnClickListener(v -> clearAllFilters());
        }

        if (btnFilterOccurrences != null) {
            btnFilterOccurrences.setOnClickListener(v -> {
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
    }

    private void clearAllFilters() {
        if (etSearchOccurrences != null) {
            etSearchOccurrences.setText("");
        }
        currentSearchQuery = "";
        filterMaterials.clear();
        filterStatuses.clear();
        applyFilter();
    }

    private void loadOccurrences() {
        OccurrenceStore.refresh(requireContext(), occurrences -> {
            if (!isAdded()) {
                return;
            }
            applyOccurrences(occurrences);
            hideLoading();
        });
    }

    private void applyOccurrences(List<Occurrence> occurrences) {
        allOccurrences.clear();
        if (occurrences != null) {
            allOccurrences.addAll(occurrences);
        }
        applyFilter();
    }

    /**
     * Aplica filtros compostos e atualiza contadores visuais e badges.
     */
    private void applyFilter() {
        List<Occurrence> filtered = OccurrenceFilter.filter(
                allOccurrences, currentSearchQuery, filterMaterials, filterStatuses);

        adapter.setItems(filtered);

        boolean hasActive = OccurrenceFilter.hasActiveFilters(
                currentSearchQuery, filterMaterials, filterStatuses);

        // Atualizar estado vazio
        if (tvEmptyOccurrencesList != null) {
            if (filtered.isEmpty()) {
                tvEmptyOccurrencesList.setVisibility(View.VISIBLE);
                tvEmptyOccurrencesList.setText(hasActive
                        ? R.string.occurrences_no_results
                        : R.string.empty_list);
            } else {
                tvEmptyOccurrencesList.setVisibility(View.GONE);
            }
        }

        // Atualizar texto de contagem
        if (tvOccurrencesCount != null && getContext() != null) {
            if (hasActive) {
                tvOccurrencesCount.setText(getString(
                        R.string.occurrences_count_format, filtered.size(), allOccurrences.size()));
            } else {
                tvOccurrencesCount.setText(getString(
                        R.string.occurrences_count_all, allOccurrences.size()));
            }
        }

        // Atualizar botão de reset de filtros
        if (btnClearFiltersChip != null) {
            btnClearFiltersChip.setVisibility(hasActive ? View.VISIBLE : View.GONE);
        }

        // Atualizar badge do botão de filtros
        int categoryFilterCount = filterMaterials.size() + filterStatuses.size();
        if (tvFilterBadge != null) {
            if (categoryFilterCount > 0) {
                tvFilterBadge.setVisibility(View.VISIBLE);
                tvFilterBadge.setText(String.valueOf(categoryFilterCount));
            } else {
                tvFilterBadge.setVisibility(View.GONE);
            }
        }

        if (btnFilterOccurrences != null && getContext() != null) {
            btnFilterOccurrences.setBackground(ContextCompat.getDrawable(
                    requireContext(),
                    categoryFilterCount > 0 ? R.drawable.bg_filter_button_active : R.drawable.bg_notification_circle));
        }
    }

    private void hideLoading() {
        if (loadingOccurrences != null) {
            loadingOccurrences.setVisibility(View.GONE);
        }
    }
}
