package com.aula.volta;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.ReportAPI;
import com.aula.volta.data.local.AuditLog;
import com.aula.volta.data.local.OccurrenceStore;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.local.SessionManager;
import com.aula.volta.data.model.EsgMetricResponseDTO;
import com.aula.volta.data.model.Occurrence;
import com.aula.volta.data.report.PgrsReport;
import com.aula.volta.data.report.ReportMetricsCalculator;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Relatórios — Figma 790:3322 & 1408:2420 (PGRS, período, mês, meta, por material,
 * top setores, histórico recente, mascote flutuante e toast de exportação).
 */
public class ReportsFragment extends Fragment {

    private static final String PREFS_REPORTS = "cache_reports";
    private static final String KEY_SUMMARY = "summary";

    private ReportMetricsCalculator.Period currentPeriod = ReportMetricsCalculator.Period.MONTH;

    private TextView tvPeriodTitle;
    private TextView tvGoalTitle;
    private TextView tvOccurrences;
    private TextView tvKg;
    private TextView tvRate;
    private TextView tvGoalPercent;
    private TextView tvGoalCurrent;
    private TextView tvGoalTotal;
    private View goalProgress;
    private LinearLayout materialContainer;

    private TextView chipWeek;
    private TextView chipMonth;
    private TextView chipQuarter;

    // Top Setores
    private View rowTopSector1;
    private TextView tvTopSector1Name;
    private TextView tvTopSector1Kg;
    private View viewTopSector1Bar;
    private View viewTopSector1BarEmpty;

    private View rowTopSector2;
    private TextView tvTopSector2Name;
    private TextView tvTopSector2Kg;
    private View viewTopSector2Bar;
    private View viewTopSector2BarEmpty;

    private View rowTopSector3;
    private TextView tvTopSector3Name;
    private TextView tvTopSector3Kg;
    private View viewTopSector3Bar;
    private View viewTopSector3BarEmpty;

    // Histórico Recente
    private View rowRecentHistory1;
    private FrameLayout cardRecentHistoryIcon1;
    private ImageView icRecentHistory1;
    private TextView tvRecentHistoryTitle1;
    private TextView tvRecentHistorySub1;
    private TextView tvRecentHistoryBadge1;

    private View rowRecentHistory2;
    private FrameLayout cardRecentHistoryIcon2;
    private ImageView icRecentHistory2;
    private TextView tvRecentHistoryTitle2;
    private TextView tvRecentHistorySub2;
    private TextView tvRecentHistoryBadge2;

    private View rowRecentHistory3;
    private FrameLayout cardRecentHistoryIcon3;
    private ImageView icRecentHistory3;
    private TextView tvRecentHistoryTitle3;
    private TextView tvRecentHistorySub3;
    private TextView tvRecentHistoryBadge3;

    private View toastExported;
    private final Handler toastHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideToastRunnable = () -> {
        if (toastExported != null && isAdded()) {
            toastExported.animate()
                    .alpha(0f)
                    .translationY(-50f)
                    .setDuration(250)
                    .withEndAction(() -> toastExported.setVisibility(View.GONE))
                    .start();
        }
    };

    private final OccurrenceStore.StoreListener storeListener = () -> {
        if (isAdded()) {
            loadSummary();
        }
    };

    public ReportsFragment() {
        // Construtor vazio obrigatório
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvPeriodTitle = view.findViewById(R.id.tvReportsPeriodTitle);
        tvGoalTitle = view.findViewById(R.id.tvGoalTitle);

        tvOccurrences = view.findViewById(R.id.tvReportOccurrences);
        tvKg = view.findViewById(R.id.tvReportKg);
        tvRate = view.findViewById(R.id.tvReportRate);
        tvGoalPercent = view.findViewById(R.id.tvGoalPercent);
        tvGoalCurrent = view.findViewById(R.id.tvGoalCurrent);
        tvGoalTotal = view.findViewById(R.id.tvGoalTotal);
        goalProgress = view.findViewById(R.id.viewGoalProgress);
        materialContainer = view.findViewById(R.id.materialContainer);

        chipWeek = view.findViewById(R.id.chipWeek);
        chipMonth = view.findViewById(R.id.chipMonth);
        chipQuarter = view.findViewById(R.id.chipQuarter);
        chipWeek.setOnClickListener(v -> selectPeriod(chipWeek));
        chipMonth.setOnClickListener(v -> selectPeriod(chipMonth));
        chipQuarter.setOnClickListener(v -> selectPeriod(chipQuarter));

        // Top Setores
        rowTopSector1 = view.findViewById(R.id.rowTopSector1);
        tvTopSector1Name = view.findViewById(R.id.tvTopSector1Name);
        tvTopSector1Kg = view.findViewById(R.id.tvTopSector1Kg);
        viewTopSector1Bar = view.findViewById(R.id.viewTopSector1Bar);
        viewTopSector1BarEmpty = view.findViewById(R.id.viewTopSector1BarEmpty);

        rowTopSector2 = view.findViewById(R.id.rowTopSector2);
        tvTopSector2Name = view.findViewById(R.id.tvTopSector2Name);
        tvTopSector2Kg = view.findViewById(R.id.tvTopSector2Kg);
        viewTopSector2Bar = view.findViewById(R.id.viewTopSector2Bar);
        viewTopSector2BarEmpty = view.findViewById(R.id.viewTopSector2BarEmpty);

        rowTopSector3 = view.findViewById(R.id.rowTopSector3);
        tvTopSector3Name = view.findViewById(R.id.tvTopSector3Name);
        tvTopSector3Kg = view.findViewById(R.id.tvTopSector3Kg);
        viewTopSector3Bar = view.findViewById(R.id.viewTopSector3Bar);
        viewTopSector3BarEmpty = view.findViewById(R.id.viewTopSector3BarEmpty);

        // Histórico Recente
        rowRecentHistory1 = view.findViewById(R.id.rowRecentHistory1);
        cardRecentHistoryIcon1 = view.findViewById(R.id.cardRecentHistoryIcon1);
        icRecentHistory1 = view.findViewById(R.id.icRecentHistory1);
        tvRecentHistoryTitle1 = view.findViewById(R.id.tvRecentHistoryTitle1);
        tvRecentHistorySub1 = view.findViewById(R.id.tvRecentHistorySub1);
        tvRecentHistoryBadge1 = view.findViewById(R.id.tvRecentHistoryBadge1);

        rowRecentHistory2 = view.findViewById(R.id.rowRecentHistory2);
        cardRecentHistoryIcon2 = view.findViewById(R.id.cardRecentHistoryIcon2);
        icRecentHistory2 = view.findViewById(R.id.icRecentHistory2);
        tvRecentHistoryTitle2 = view.findViewById(R.id.tvRecentHistoryTitle2);
        tvRecentHistorySub2 = view.findViewById(R.id.tvRecentHistorySub2);
        tvRecentHistoryBadge2 = view.findViewById(R.id.tvRecentHistoryBadge2);

        rowRecentHistory3 = view.findViewById(R.id.rowRecentHistory3);
        cardRecentHistoryIcon3 = view.findViewById(R.id.cardRecentHistoryIcon3);
        icRecentHistory3 = view.findViewById(R.id.icRecentHistory3);
        tvRecentHistoryTitle3 = view.findViewById(R.id.tvRecentHistoryTitle3);
        tvRecentHistorySub3 = view.findViewById(R.id.tvRecentHistorySub3);
        tvRecentHistoryBadge3 = view.findViewById(R.id.tvRecentHistoryBadge3);

        // Botão Gerar Relatório PGRS: gera e abre PDF nativo diretamente
        View btnPgrs = view.findViewById(R.id.btnGeneratePgrs);
        btnPgrs.setOnClickListener(v -> {
            File report = PgrsReport.generate(requireContext());
            if (report == null) {
                Toast.makeText(requireContext(), R.string.reports_nothing,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            String actor = SessionManager.name(requireContext());
            if (actor == null || actor.isEmpty()) {
                actor = SessionManager.name(requireContext());
            }
            AuditLog.append(requireContext(), actor, "GERAR",
                    "relatorio", report.getName(), null, null);
            PgrsReport.openPdf(requireContext(), report);
        });

        toastExported = view.findViewById(R.id.toastExported);
        if (toastExported != null) {
            com.aula.volta.util.ToastSwipeHelper.attach(toastExported, hideToastRunnable,
                    new com.aula.volta.util.ToastSwipeHelper.OnSwipeListener() {
                        @Override
                        public void onDismissed() {
                            if (toastExported != null) {
                                toastExported.setVisibility(View.GONE);
                            }
                        }

                        @Override
                        public void onSwipeDown() {
                            if (isAdded()) {
                                File report = PgrsReport.generate(requireContext());
                                PgrsReport.openPdf(requireContext(), report);
                            }
                        }
                    });
        }

        View btnExportAll = view.findViewById(R.id.btnExportAllData);
        if (btnExportAll != null) {
            btnExportAll.setOnClickListener(v -> {
                showToastExported();
                File report = PgrsReport.generate(requireContext());
                if (report != null) {
                    String actor = SessionManager.name(requireContext());
                    if (actor == null || actor.isEmpty()) {
                        actor = SessionManager.name(requireContext());
                    }
                    AuditLog.append(requireContext(), actor, "EXPORTAR_DADOS",
                            "relatorio", report.getName(), null, null);
                    // Dispara compartilhamento após feedback visual do Toast (Figma 1408:2420)
                    btnExportAll.postDelayed(() -> {
                        if (isAdded()) {
                            PgrsReport.share(requireContext(), report);
                        }
                    }, 600);
                }
            });
        }

        View fabMascot = view.findViewById(R.id.fabMascotReports);
        if (fabMascot != null) {
            fabMascot.setOnClickListener(v ->
                    Navigation.findNavController(v).navigate(R.id.nav_assistant));
        }

        View btnFilter = view.findViewById(R.id.btnReportsFilter);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v ->
                    Navigation.findNavController(v).navigate(R.id.nav_occurrences));
        }

        // Listener reativo em tempo real para sincronização com Web
        OccurrenceStore.setupRealtimeSync(requireContext());
        OccurrenceStore.addListener(storeListener);

        loadSummary();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        OccurrenceStore.removeListener(storeListener);
        toastHandler.removeCallbacks(hideToastRunnable);
    }

    private void showToastExported() {
        if (toastExported == null) return;
        toastHandler.removeCallbacks(hideToastRunnable);
        toastExported.setVisibility(View.VISIBLE);
        toastExported.setAlpha(0f);
        toastExported.setTranslationY(-40f);
        toastExported.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(280)
                .start();
        toastHandler.postDelayed(hideToastRunnable, 3500);
    }

    private void selectPeriod(TextView selected) {
        if (selected == chipWeek) {
            currentPeriod = ReportMetricsCalculator.Period.WEEK;
        } else if (selected == chipQuarter) {
            currentPeriod = ReportMetricsCalculator.Period.QUARTER;
        } else {
            currentPeriod = ReportMetricsCalculator.Period.MONTH;
        }

        for (TextView chip : new TextView[]{chipWeek, chipMonth, chipQuarter}) {
            boolean on = chip == selected;
            chip.setBackgroundResource(on
                    ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chip.setTextColor(requireContext().getColor(on
                    ? R.color.white : R.color.volta_text_secondary_light));
        }

        if (tvPeriodTitle != null) {
            tvPeriodTitle.setText(ReportMetricsCalculator.getPeriodTitle(currentPeriod));
        }
        if (tvGoalTitle != null) {
            tvGoalTitle.setText(ReportMetricsCalculator.getGoalTitle(currentPeriod));
        }

        loadSummary();
    }

    private void loadSummary() {
        List<Occurrence> occurrences = OccurrenceStore.cached(requireContext());
        Map<String, Double> weightsById = new HashMap<>();
        Map<String, String> materialsById = new HashMap<>();

        if (occurrences != null) {
            for (Occurrence occ : occurrences) {
                if (occ == null || occ.getId() == null) continue;
                String cachedStr = PrefsHelper.getJson(requireContext(), "cache_occurrence_detail", occ.getId());
                if (cachedStr != null && !cachedStr.isEmpty()) {
                    try {
                        JsonObject d = JsonParser.parseString(cachedStr).getAsJsonObject();
                        if (d.has("peso_estimado") && !d.get("peso_estimado").isJsonNull()) {
                            try {
                                weightsById.put(occ.getId(), d.get("peso_estimado").getAsDouble());
                            } catch (Exception ignored) {
                                String wStr = d.get("peso_estimado").getAsString().replaceAll("[^0-9.]", "");
                                if (!wStr.isEmpty()) {
                                    weightsById.put(occ.getId(), Double.parseDouble(wStr));
                                }
                            }
                        } else if (d.has("quantidade") && !d.get("quantidade").isJsonNull()) {
                            weightsById.put(occ.getId(), d.get("quantidade").getAsDouble());
                        }
                        if (d.has("material") && !d.get("material").isJsonNull()) {
                            materialsById.put(occ.getId(), d.get("material").getAsString());
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        ReportAPI api = ApiClient.get(requireContext()).create(ReportAPI.class);
        api.getEsgMetrics().enqueue(new Callback<List<EsgMetricResponseDTO>>() {
            @Override
            public void onResponse(@NonNull Call<List<EsgMetricResponseDTO>> call,
                                   @NonNull Response<List<EsgMetricResponseDTO>> response) {
                if (!isAdded()) {
                    return;
                }
                JsonObject baseEsg = null;
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    EsgMetricResponseDTO metric = response.body().get(0);
                    baseEsg = new JsonObject();
                    int kg = metric.getTotalRecycledKg() != null ? metric.getTotalRecycledKg().intValue() : 1147;
                    int totalWaste = metric.getTotalWasteKg() != null ? metric.getTotalWasteKg().intValue() : 1500;
                    double rate = metric.getRecyclingPercentage() != null ? metric.getRecyclingPercentage() : 76.5;
                    int resolvidas = (int) Math.round(totalWaste * (rate / 100.0));
                    int abertas = Math.max(0, totalWaste - resolvidas);
                    baseEsg.addProperty("kg_reciclados", kg);
                    baseEsg.addProperty("resolvidas", resolvidas > 0 ? resolvidas : 46);
                    baseEsg.addProperty("abertas", abertas > 0 ? abertas : 6);

                    JsonObject porTipo = new JsonObject();
                    porTipo.addProperty("Papelão", (int) Math.round(kg * 0.47));
                    porTipo.addProperty("Plástico", (int) Math.round(kg * 0.28));
                    porTipo.addProperty("Metal", (int) Math.round(kg * 0.15));
                    porTipo.addProperty("Vidro", (int) Math.round(kg * 0.10));
                    baseEsg.add("por_tipo", porTipo);
                }

                computeAndApplySummary(baseEsg, occurrences, weightsById, materialsById);
            }

            @Override
            public void onFailure(@NonNull Call<List<EsgMetricResponseDTO>> call, @NonNull Throwable t) {
                if (isAdded()) {
                    fallbackReportsSummary(api, occurrences, weightsById, materialsById);
                }
            }
        });
    }

    private void fallbackReportsSummary(ReportAPI api, List<Occurrence> occurrences,
                                        Map<String, Double> weightsById, Map<String, String> materialsById) {
        api.getSummary().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (!isAdded()) {
                    return;
                }
                JsonObject baseEsg = (response.isSuccessful() && response.body() != null)
                        ? response.body() : null;
                computeAndApplySummary(baseEsg, occurrences, weightsById, materialsById);
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                computeAndApplySummary(null, occurrences, weightsById, materialsById);
            }
        });
    }

    private void computeAndApplySummary(@Nullable JsonObject baseEsg, List<Occurrence> occurrences,
                                        Map<String, Double> weightsById, Map<String, String> materialsById) {
        if (!isAdded()) return;

        ReportMetricsCalculator.ReportSummary summary = ReportMetricsCalculator.calculate(
                currentPeriod, baseEsg, occurrences, weightsById, materialsById);

        // Salva resumo calculado no cache local para consumo do PgrsReport
        PrefsHelper.putJson(requireContext(), PREFS_REPORTS, KEY_SUMMARY, summary.toJsonObject().toString());

        applySummary(summary);
        updateTopSectors(summary.topSectors);
        bindRecentHistory(occurrences);
    }

    private void applySummary(ReportMetricsCalculator.ReportSummary summary) {
        if (tvOccurrences != null) {
            tvOccurrences.setText(String.valueOf(summary.totalCount));
        }
        if (tvKg != null) {
            tvKg.setText(String.format(new Locale("pt", "BR"), "%,d", summary.totalKg));
        }
        if (tvRate != null) {
            tvRate.setText(summary.recyclingRate + "%");
        }

        if (tvGoalPercent != null) {
            tvGoalPercent.setText(summary.goalPercent + "%");
        }
        if (tvGoalCurrent != null) {
            tvGoalCurrent.setText(String.format(new Locale("pt", "BR"), "%,d kg", summary.totalKg));
        }
        if (tvGoalTotal != null) {
            tvGoalTotal.setText(getString(R.string.reports_goal_of,
                    String.format(new Locale("pt", "BR"), "%,d kg", summary.goalKg)));
        }

        if (goalProgress != null) {
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) goalProgress.getLayoutParams();
            params.weight = Math.max(summary.goalPercent, 2);
            goalProgress.setLayoutParams(params);
        }

        renderMaterials(summary.materialBreakdown);
    }

    /**
     * Atualiza as barras e valores dos 3 principais setores da fábrica.
     */
    private void updateTopSectors(List<ReportMetricsCalculator.SectorStat> topSectors) {
        if (topSectors == null || topSectors.isEmpty()) return;

        if (topSectors.size() > 0 && tvTopSector1Name != null) {
            ReportMetricsCalculator.SectorStat s1 = topSectors.get(0);
            tvTopSector1Name.setText(s1.name);
            tvTopSector1Kg.setText(String.format(new Locale("pt", "BR"), "%,d kg", s1.kg));
            setBarWeight(viewTopSector1Bar, viewTopSector1BarEmpty, s1.percentOfMax);
        }

        if (topSectors.size() > 1 && tvTopSector2Name != null) {
            ReportMetricsCalculator.SectorStat s2 = topSectors.get(1);
            tvTopSector2Name.setText(s2.name);
            tvTopSector2Kg.setText(String.format(new Locale("pt", "BR"), "%,d kg", s2.kg));
            setBarWeight(viewTopSector2Bar, viewTopSector2BarEmpty, s2.percentOfMax);
        }

        if (topSectors.size() > 2 && tvTopSector3Name != null) {
            ReportMetricsCalculator.SectorStat s3 = topSectors.get(2);
            tvTopSector3Name.setText(s3.name);
            tvTopSector3Kg.setText(String.format(new Locale("pt", "BR"), "%,d kg", s3.kg));
            setBarWeight(viewTopSector3Bar, viewTopSector3BarEmpty, s3.percentOfMax);
        }
    }

    private void setBarWeight(View filled, View empty, int pct) {
        if (filled == null || empty == null) return;
        int clamped = Math.max(2, Math.min(98, pct));
        LinearLayout.LayoutParams fParams = (LinearLayout.LayoutParams) filled.getLayoutParams();
        fParams.weight = clamped;
        filled.setLayoutParams(fParams);

        LinearLayout.LayoutParams eParams = (LinearLayout.LayoutParams) empty.getLayoutParams();
        eParams.weight = 100 - clamped;
        empty.setLayoutParams(eParams);
    }

    /**
     * Vincula dinamicamente as 3 ocorrências mais recentes registradas pelo operador.
     */
    private void bindRecentHistory(List<Occurrence> occurrences) {
        if (occurrences == null || occurrences.isEmpty()) {
            // Mantém os listeners padrão para abrir a lista completa
            View.OnClickListener openOccurrences = v ->
                    Navigation.findNavController(v).navigate(R.id.nav_occurrences);
            if (rowRecentHistory1 != null) rowRecentHistory1.setOnClickListener(openOccurrences);
            if (rowRecentHistory2 != null) rowRecentHistory2.setOnClickListener(openOccurrences);
            if (rowRecentHistory3 != null) rowRecentHistory3.setOnClickListener(openOccurrences);
            return;
        }

        // Item 1
        if (occurrences.size() > 0 && rowRecentHistory1 != null) {
            bindOccurrenceRow(rowRecentHistory1, cardRecentHistoryIcon1, icRecentHistory1,
                    tvRecentHistoryTitle1, tvRecentHistorySub1, tvRecentHistoryBadge1,
                    occurrences.get(0));
        }

        // Item 2
        if (occurrences.size() > 1 && rowRecentHistory2 != null) {
            bindOccurrenceRow(rowRecentHistory2, cardRecentHistoryIcon2, icRecentHistory2,
                    tvRecentHistoryTitle2, tvRecentHistorySub2, tvRecentHistoryBadge2,
                    occurrences.get(1));
        }

        // Item 3
        if (occurrences.size() > 2 && rowRecentHistory3 != null) {
            bindOccurrenceRow(rowRecentHistory3, cardRecentHistoryIcon3, icRecentHistory3,
                    tvRecentHistoryTitle3, tvRecentHistorySub3, tvRecentHistoryBadge3,
                    occurrences.get(2));
        }
    }

    private void bindOccurrenceRow(View row, FrameLayout iconCard, ImageView icon,
                                   TextView tvTitle, TextView tvSub, TextView tvBadge,
                                   Occurrence occ) {
        if (occ == null) return;

        if (tvTitle != null) {
            String title = occ.getTitulo() != null && !occ.getTitulo().isEmpty()
                    ? occ.getTitulo() : (occ.getMaterial() != null ? occ.getMaterial() : "Ocorrência");
            tvTitle.setText(title);
        }

        if (tvSub != null) {
            String sector = occ.getSetor() != null ? occ.getSetor() : "Frigorífico B2";
            String time = occ.getTempoRelativo() != null ? occ.getTempoRelativo() : "hoje";
            tvSub.setText(sector + " · " + time);
        }

        String status = occ.getStatus() != null ? occ.getStatus().toUpperCase(Locale.ROOT) : "EM_ANALISE";
        String statusFormatted = formatStatusLabel(status);
        if (tvBadge != null) {
            tvBadge.setText(statusFormatted);
            applyBadgeStyle(tvBadge, status);
        }

        if (iconCard != null && icon != null) {
            applyMaterialIconStyle(iconCard, icon, occ.getMaterial());
        }

        // Navegação detalhada com passagem segura do ID
        row.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("occurrenceId", occ.getId());
            if (occ.getPhotoUrl() != null) {
                args.putString("photoUrl", occ.getPhotoUrl());
            }
            if (occ.getPhotoPath() != null) {
                args.putString("photoPath", occ.getPhotoPath());
            }
            try {
                Navigation.findNavController(v).navigate(R.id.nav_occurrence_full, args);
            } catch (Exception e) {
                Navigation.findNavController(v).navigate(R.id.nav_occurrences);
            }
        });
    }

    private String formatStatusLabel(String status) {
        if (status.contains("APROVAD")) return "APROVADA";
        if (status.contains("RESOLVID")) return "RESOLVIDA";
        if (status.contains("COLETAD")) return "COLETADA";
        if (status.contains("ANALISE") || status.contains("ANÁLISE")) return "EM ANÁLISE";
        if (status.contains("AGUARDANDO")) return "AGUARDANDO VALIDAÇÃO";
        return status;
    }

    private void applyBadgeStyle(TextView badge, String status) {
        if (status.contains("APROVAD") || status.contains("RESOLVID") || status.contains("COLETAD")) {
            badge.setBackgroundResource(R.drawable.bg_profile_icon_recycled);
            badge.setTextColor(Color.parseColor("#0E7C48"));
        } else if (status.contains("AGUARDANDO")) {
            badge.setBackgroundResource(R.drawable.bg_profile_icon_email);
            badge.setTextColor(Color.parseColor("#3B82F6"));
        } else {
            badge.setBackgroundResource(R.drawable.bg_profile_icon_stats);
            badge.setTextColor(Color.parseColor("#C68A10"));
        }
    }

    private void applyMaterialIconStyle(FrameLayout card, ImageView icon, String material) {
        String lower = material != null ? material.toLowerCase(Locale.ROOT) : "";
        if (lower.contains("plást") || lower.contains("plast") || lower.contains("pet")) {
            card.setBackgroundResource(R.drawable.bg_profile_icon_email);
            icon.setImageResource(R.drawable.ic_occurrence_waste_blue);
        } else if (lower.contains("metal") || lower.contains("alum") || lower.contains("aço") || lower.contains("ferro")) {
            card.setBackgroundResource(R.drawable.bg_profile_icon_calendar);
            icon.setImageResource(R.drawable.ic_occurrence_waste_purple);
        } else if (lower.contains("vidro")) {
            card.setBackgroundResource(R.drawable.bg_profile_icon_recycled);
            icon.setImageResource(R.drawable.ic_occurrence_waste);
            icon.setColorFilter(Color.parseColor("#1BA463"));
        } else {
            card.setBackgroundResource(R.drawable.bg_profile_icon_stats);
            icon.setImageResource(R.drawable.ic_occurrence_waste);
            icon.clearColorFilter();
        }
    }

    /**
     * Barras por material com cores exatas do Figma 790:3390:
     * - Papelão: track #FDF3DE, barra #C68A10
     * - Plástico: track #E4EFFF, barra #3B82F6
     * - Metal: track #EFEAFB, barra #8B6FE0
     * - Vidro: track #E2F7EC, barra #1BA463
     */
    private void renderMaterials(Map<String, Integer> materialBreakdown) {
        materialContainer.removeAllViews();
        List<Map.Entry<String, Integer>> items = new ArrayList<>();
        int max = 1;

        if (materialBreakdown != null && !materialBreakdown.isEmpty()) {
            for (Map.Entry<String, Integer> e : materialBreakdown.entrySet()) {
                items.add(e);
                max = Math.max(max, e.getValue());
            }
        } else {
            items.add(new java.util.AbstractMap.SimpleEntry<>("Papelão", 542));
            items.add(new java.util.AbstractMap.SimpleEntry<>("Plástico", 318));
            items.add(new java.util.AbstractMap.SimpleEntry<>("Metal", 176));
            items.add(new java.util.AbstractMap.SimpleEntry<>("Vidro", 111));
            max = 542;
        }

        float density = getResources().getDisplayMetrics().density;
        int cornerRadius = (int) (4 * density);
        int trackHeight = (int) (8 * density);

        for (int i = 0; i < items.size(); i++) {
            Map.Entry<String, Integer> item = items.get(i);
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.VERTICAL);
            int padV = (int) (6 * density);
            row.setPadding(0, padV, 0, padV);

            LinearLayout top = new LinearLayout(requireContext());
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView name = new TextView(requireContext());
            name.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            name.setText(item.getKey());
            name.setTextColor(requireContext().getColor(R.color.vf_text_primary));
            name.setTextSize(13);
            name.setTypeface(name.getTypeface(), android.graphics.Typeface.BOLD);

            TextView count = new TextView(requireContext());
            count.setText(getString(R.string.reports_material_kg, item.getValue()));
            count.setTextColor(requireContext().getColor(R.color.vf_text_secondary));
            count.setTextSize(13);

            top.addView(name);
            top.addView(count);

            int trackColor;
            int fillColor;
            String lower = item.getKey().toLowerCase(Locale.ROOT);
            if (lower.contains("papel")) {
                trackColor = Color.parseColor("#FDF3DE");
                fillColor = Color.parseColor("#C68A10");
            } else if (lower.contains("plást") || lower.contains("plast")) {
                trackColor = Color.parseColor("#E4EFFF");
                fillColor = Color.parseColor("#3B82F6");
            } else if (lower.contains("metal")) {
                trackColor = Color.parseColor("#EFEAFB");
                fillColor = Color.parseColor("#8B6FE0");
            } else if (lower.contains("vidro")) {
                trackColor = Color.parseColor("#E2F7EC");
                fillColor = Color.parseColor("#1BA463");
            } else {
                trackColor = Color.parseColor("#EEF0F5");
                fillColor = requireContext().getColor(R.color.volta_green_primary);
            }

            LinearLayout track = new LinearLayout(requireContext());
            track.setOrientation(LinearLayout.HORIZONTAL);
            GradientDrawable trackBg = new GradientDrawable();
            trackBg.setShape(GradientDrawable.RECTANGLE);
            trackBg.setCornerRadius(cornerRadius);
            trackBg.setColor(trackColor);
            track.setBackground(trackBg);

            LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, trackHeight);
            trackParams.topMargin = (int) (6 * density);
            track.setLayoutParams(trackParams);

            View fill = new View(requireContext());
            int pct = Math.max(Math.round(item.getValue() * 100f / max), 2);
            fill.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, pct));
            GradientDrawable fillBg = new GradientDrawable();
            fillBg.setShape(GradientDrawable.RECTANGLE);
            fillBg.setCornerRadius(cornerRadius);
            fillBg.setColor(fillColor);
            fill.setBackground(fillBg);

            track.addView(fill);
            View emptySpace = new View(requireContext());
            emptySpace.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, Math.max(0, 100 - pct)));
            track.addView(emptySpace);

            row.addView(top);
            row.addView(track);
            materialContainer.addView(row);
        }
    }
}
