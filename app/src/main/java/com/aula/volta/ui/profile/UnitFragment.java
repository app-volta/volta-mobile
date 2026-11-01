package com.aula.volta.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aula.volta.R;
import com.aula.volta.data.local.OccurrenceStore;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.local.SessionManager;
import com.aula.volta.data.model.CooperativeJSON;
import com.aula.volta.data.model.Occurrence;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.Locale;

public class UnitFragment extends Fragment {

    private TextView tvUnitSubtitle;
    private TextView tvUnitOccurrences;
    private TextView tvUnitRecovered;
    private TextView tvUnitNextCollection;
    private TextView tvUnitManager;
    private TextView tvUnitGoalBadge;
    private TextView tvUnitGoalCurrent;
    private TextView tvUnitGoalTarget;
    private ProgressBar pbUnitGoal;
    private TextView tvUnitPartnerCoopName;

    public UnitFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_unit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvUnitSubtitle = view.findViewById(R.id.tvUnitSubtitle);
        tvUnitOccurrences = view.findViewById(R.id.tvUnitOccurrences);
        tvUnitRecovered = view.findViewById(R.id.tvUnitRecovered);
        tvUnitNextCollection = view.findViewById(R.id.tvUnitNextCollection);
        tvUnitManager = view.findViewById(R.id.tvUnitManager);
        tvUnitGoalBadge = view.findViewById(R.id.tvUnitGoalBadge);
        tvUnitGoalCurrent = view.findViewById(R.id.tvUnitGoalCurrent);
        tvUnitGoalTarget = view.findViewById(R.id.tvUnitGoalTarget);
        pbUnitGoal = view.findViewById(R.id.pbUnitGoal);
        tvUnitPartnerCoopName = view.findViewById(R.id.tvUnitPartnerCoopName);

        view.findViewById(R.id.btnBackUnit).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        bindDynamicData();

        View rowPartner = view.findViewById(R.id.rowUnitPartnerCoop);
        if (rowPartner != null) {
            rowPartner.setOnClickListener(v -> {
                try {
                    Navigation.findNavController(v).navigate(R.id.nav_cooperatives);
                } catch (Exception ignored) {
                    Navigation.findNavController(v).navigateUp();
                }
            });
        }
    }

    private void bindDynamicData() {
        if (!isAdded() || getContext() == null) return;

        updateUnitSubtitle();
        fetchCompanyLive();

        String managerName = SessionManager.name(requireContext());
        if (managerName == null || managerName.trim().isEmpty() || "Colaborador".equalsIgnoreCase(managerName.trim())) {
            managerName = "Gestor da Unidade";
        }
        if (tvUnitManager != null) {
            tvUnitManager.setText(managerName);
        }

        int totalOcc = 0;
        int kg = 0;
        int targetKg = 0;

        String summaryJson = PrefsHelper.getJson(requireContext(), "cache_reports", "summary");
        if (summaryJson != null && !summaryJson.isEmpty()) {
            try {
                JsonObject obj = JsonParser.parseString(summaryJson).getAsJsonObject();
                int ab = obj.has("abertas") ? obj.get("abertas").getAsInt() : 0;
                int res = obj.has("resolvidas") ? obj.get("resolvidas").getAsInt() : 0;
                totalOcc = ab + res;
                if (obj.has("kg_reciclados")) {
                    kg = obj.get("kg_reciclados").getAsInt();
                }
                if (obj.has("monthlyTarget") && !obj.get("monthlyTarget").isJsonNull()) {
                    double mt = obj.get("monthlyTarget").getAsDouble();
                    if (mt > 0) targetKg = (int) Math.round(mt);
                }
            } catch (Exception ignored) {
            }
        }

        if (totalOcc == 0) {
            List<Occurrence> list = OccurrenceStore.cached(requireContext());
            totalOcc = list != null ? list.size() : 0;
        }

        if (tvUnitOccurrences != null) {
            tvUnitOccurrences.setText(totalOcc + " no mês");
        }

        if (tvUnitRecovered != null) {
            tvUnitRecovered.setText(String.format(new Locale("pt", "BR"), "%,d kg", kg));
        }

        if (tvUnitNextCollection != null) {
            tvUnitNextCollection.setText("Sob demanda");
        }

        int percent = targetKg > 0 ? Math.min(100, (int) Math.round(kg * 100.0 / targetKg)) : 0;
        if (tvUnitGoalBadge != null) {
            tvUnitGoalBadge.setText(percent + "%");
        }
        if (tvUnitGoalCurrent != null) {
            tvUnitGoalCurrent.setText(String.format(new Locale("pt", "BR"), "%,d kg", kg));
        }
        if (tvUnitGoalTarget != null) {
            tvUnitGoalTarget.setText("de " + String.format(new Locale("pt", "BR"), "%,d kg", targetKg));
        }
        if (pbUnitGoal != null) {
            pbUnitGoal.setProgress(percent);
        }

        if (tvUnitPartnerCoopName != null) {
            List<CooperativeJSON> coops = PrefsHelper.getList(requireContext(),
                    "cache_cooperatives_db", "db_list", CooperativeJSON[].class);
            if (coops != null && !coops.isEmpty()) {
                tvUnitPartnerCoopName.setText(coops.get(0).getNome());
            } else {
                tvUnitPartnerCoopName.setText("Ver parceiras");
            }
        }
    }

    private void updateUnitSubtitle() {
        if (!isAdded() || tvUnitSubtitle == null) return;
        String companyName = SessionManager.getCompanyName(requireContext());
        String sector = SessionManager.sector(requireContext());
        String address = SessionManager.getCompanyAddress(requireContext());

        StringBuilder sb = new StringBuilder();
        if (companyName != null && !companyName.trim().isEmpty()) {
            sb.append(companyName.trim());
        } else {
            sb.append("Unidade Industrial");
        }
        if (sector != null && !sector.trim().isEmpty()) {
            sb.append(" · ").append(sector.trim());
        }
        if (address != null && !address.trim().isEmpty()) {
            sb.append(" · ").append(address.trim());
        }
        tvUnitSubtitle.setText(sb.toString());
    }

    private void fetchCompanyLive() {
        if (!isAdded()) return;
        try {
            com.aula.volta.data.api.AuthAPI authApi = com.aula.volta.data.api.ApiClient
                    .get(requireContext()).create(com.aula.volta.data.api.AuthAPI.class);
            authApi.getMyCompany().enqueue(new retrofit2.Callback<com.aula.volta.data.model.CompanyResponseDTO>() {
                @Override
                public void onResponse(@NonNull retrofit2.Call<com.aula.volta.data.model.CompanyResponseDTO> call,
                                       @NonNull retrofit2.Response<com.aula.volta.data.model.CompanyResponseDTO> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful() && response.body() != null) {
                        com.aula.volta.data.model.CompanyResponseDTO c = response.body();
                        if (c.getId() != null && !c.getId().trim().isEmpty()) {
                            SessionManager.saveCompanyId(requireContext(), c.getId().trim());
                        }
                        SessionManager.saveCompany(requireContext(), c.getName(), c.getFormattedCnpj(), c.getAddress());
                        updateUnitSubtitle();
                    }
                }

                @Override
                public void onFailure(@NonNull retrofit2.Call<com.aula.volta.data.model.CompanyResponseDTO> call,
                                      @NonNull Throwable t) {
                }
            });
        } catch (Exception ignored) {
        }
    }
}
