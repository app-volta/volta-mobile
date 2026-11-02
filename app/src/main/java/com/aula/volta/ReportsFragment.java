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
import com.aula.volta.data.api.CatalogAPI;
import com.aula.volta.data.api.ReportAPI;
import com.aula.volta.data.model.AreaResponseDTO;
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

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;

public class ReportsFragment extends Fragment {

    private static final String PREFS_REPORTS = "cache_reports";
    private static final String KEY_SUMMARY = "summary";

    private ReportMetricsCalculator.Period currentPeriod = ReportMetricsCalculator.Period.MONTH;

    private TextView tvPeriodTitle;
    private TextView tvGoalTitle;
    private TextView tvReportsVsTitle;
    private TextView tvOccurrences;
    private TextView tvKg;
    private TextView tvRate;
    private TextView tvGoalPercent;
    private TextView tvGoalCurrent;
    private TextView tvGoalTotal;
    private View goalProgress;
    private LinearLayout materialContainer;
    private PieChart chartMaterialsPie;
    private BarChart chartReportsTimeline;
    private TextView tvReportsVsOccurrences;
    private TextView tvReportsVsKg;
    private TextView tvReportsVsRate;

    private View skeletonReports;
    private View layoutReportsDynamicContent;

    private TextView chipWeek;
    private TextView chipMonth;
    private TextView chipQuarter;

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
        tvReportsVsTitle = view.findViewById(R.id.tvReportsVsTitle);
        if (tvReportsVsTitle != null) tvReportsVsTitle.setText(ReportMetricsCalculator.getVsPeriodTitle(currentPeriod));

        tvOccurrences = view.findViewById(R.id.tvReportOccurrences);
        tvKg = view.findViewById(R.id.tvReportKg);
        tvRate = view.findViewById(R.id.tvReportRate);
        tvGoalPercent = view.findViewById(R.id.tvGoalPercent);
        tvGoalCurrent = view.findViewById(R.id.tvGoalCurrent);
        tvGoalTotal = view.findViewById(R.id.tvGoalTotal);
        goalProgress = view.findViewById(R.id.viewGoalProgress);
        materialContainer = view.findViewById(R.id.materialContainer);
        chartMaterialsPie = view.findViewById(R.id.chartMaterialsPie);
        chartReportsTimeline = view.findViewById(R.id.chartReportsTimeline);
        tvReportsVsOccurrences = view.findViewById(R.id.tvReportsVsOccurrences);
        tvReportsVsKg = view.findViewById(R.id.tvReportsVsKg);
        tvReportsVsRate = view.findViewById(R.id.tvReportsVsRate);

        skeletonReports = view.findViewById(R.id.skeletonReports);
        layoutReportsDynamicContent = view.findViewById(R.id.layoutReportsDynamicContent);

        chipWeek = view.findViewById(R.id.chipWeek);
        chipMonth = view.findViewById(R.id.chipMonth);
        chipQuarter = view.findViewById(R.id.chipQuarter);
        chipWeek.setOnClickListener(v -> selectPeriod(chipWeek));
        chipMonth.setOnClickListener(v -> selectPeriod(chipMonth));
        chipQuarter.setOnClickListener(v -> selectPeriod(chipQuarter));

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

        View btnPgrs = view.findViewById(R.id.btnGeneratePgrs);
        btnPgrs.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Gerando relatório PGRS oficial...", Toast.LENGTH_SHORT).show();
            File report = PgrsReport.generate(requireContext(), currentPeriod);
            if (report == null) {
                Toast.makeText(requireContext(), R.string.reports_nothing,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            String actor = SessionManager.name(requireContext());
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
                                File report = PgrsReport.generate(requireContext(), currentPeriod);
                                PgrsReport.openPdf(requireContext(), report);
                            }
                        }
                    });
        }

        View btnExportAll = view.findViewById(R.id.btnExportAllData);
        if (btnExportAll != null) {
            btnExportAll.setOnClickListener(v -> {
                showToastExported();
                File report = PgrsReport.generate(requireContext(), currentPeriod);
                if (report != null) {
                    String actor = SessionManager.name(requireContext());
                    AuditLog.append(requireContext(), actor, "EXPORTAR_DADOS",
                            "relatorio", report.getName(), null, null);

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

        OccurrenceStore.addListener(storeListener);

        loadSummary();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSummary();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (skeletonReports != null) {
            com.aula.volta.util.SkeletonPulseAnimator.stop(skeletonReports);
        }
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
        if (tvReportsVsTitle != null) {
            tvReportsVsTitle.setText(ReportMetricsCalculator.getVsPeriodTitle(currentPeriod));
        }

        loadSummary();
    }

    private void loadSummary() {
        if (!isAdded() || getContext() == null) return;

        if (skeletonReports != null) {
            skeletonReports.setVisibility(View.VISIBLE);
            com.aula.volta.util.SkeletonPulseAnimator.start(skeletonReports);
        }
        if (layoutReportsDynamicContent != null) {
            layoutReportsDynamicContent.setVisibility(View.GONE);
        }

        fetchAreasCatalog();

        OccurrenceStore.refresh(requireContext(), freshOccurrences -> {
            if (!isAdded()) return;
            fetchEsgAndDisplay(freshOccurrences);
        });
    }

    private void fetchAreasCatalog() {
        if (!isAdded() || getContext() == null) return;
        try {
            CatalogAPI catApi = ApiClient.get(requireContext()).create(CatalogAPI.class);
            catApi.getMyAreas().enqueue(new Callback<List<AreaResponseDTO>>() {
                @Override
                public void onResponse(@NonNull Call<List<AreaResponseDTO>> call,
                                       @NonNull Response<List<AreaResponseDTO>> response) {
                    if (isAdded() && response.isSuccessful() && response.body() != null) {
                        for (AreaResponseDTO area : response.body()) {
                            if (area.getId() != null && area.getSectorName() != null) {
                                JsonObject obj = new JsonObject();
                                obj.addProperty("id", area.getId());
                                obj.addProperty("sectorName", area.getSectorName());
                                PrefsHelper.putJson(requireContext(), "cache_areas", area.getId(), obj.toString());
                            }
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<AreaResponseDTO>> call, @NonNull Throwable t) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void fetchEsgAndDisplay(List<Occurrence> occurrences) {
        if (!isAdded() || getContext() == null) return;
        ReportAPI api = ApiClient.get(requireContext()).create(ReportAPI.class);
        api.getEsgMetrics().enqueue(new Callback<List<EsgMetricResponseDTO>>() {
            @Override
            public void onResponse(@NonNull Call<List<EsgMetricResponseDTO>> call,
                                   @NonNull Response<List<EsgMetricResponseDTO>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    EsgMetricResponseDTO metric = response.body().get(0);
                    JsonObject baseEsg = new JsonObject();
                    baseEsg.addProperty("isLiveDatabase", true);
                    if (metric.getTotalRecycledKg() != null) {
                        baseEsg.addProperty("corporateTotalKg", metric.getTotalRecycledKg().intValue());
                    }
                    if (metric.getRecyclingPercentage() != null) {
                        baseEsg.addProperty("corporateRecyclingPercentage", metric.getRecyclingPercentage().intValue());
                    }
                    if (metric.getMonthlyTarget() != null && metric.getMonthlyTarget().doubleValue() > 0) {
                        baseEsg.addProperty("monthlyTarget", metric.getMonthlyTarget().doubleValue());
                    }
                    if (metric.getWeeklyTarget() != null && metric.getWeeklyTarget().doubleValue() > 0) {
                        baseEsg.addProperty("weeklyTarget", metric.getWeeklyTarget().doubleValue());
                    }
                    if (metric.getQuarterlyTarget() != null && metric.getQuarterlyTarget().doubleValue() > 0) {
                        baseEsg.addProperty("quarterlyTarget", metric.getQuarterlyTarget().doubleValue());
                    }
                    processAndDisplaySummary(occurrences, baseEsg);
                } else {
                    JsonObject baseEsg = new JsonObject();
                    baseEsg.addProperty("isLiveDatabase", true);
                    processAndDisplaySummary(occurrences, baseEsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<EsgMetricResponseDTO>> call, @NonNull Throwable t) {
                if (isAdded()) {
                    JsonObject baseEsg = new JsonObject();
                    baseEsg.addProperty("isLiveDatabase", true);
                    processAndDisplaySummary(occurrences, baseEsg);
                }
            }
        });
    }

    private void processAndDisplaySummary(List<Occurrence> occurrences, @Nullable JsonObject baseEsg) {
        if (!isAdded() || getContext() == null) return;

        if (skeletonReports != null) {
            com.aula.volta.util.SkeletonPulseAnimator.stop(skeletonReports);
            skeletonReports.setVisibility(View.GONE);
        }
        if (layoutReportsDynamicContent != null) {
            layoutReportsDynamicContent.setVisibility(View.VISIBLE);
            layoutReportsDynamicContent.setAlpha(0f);
            layoutReportsDynamicContent.animate().alpha(1f).setDuration(220).start();
        }

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

        if (baseEsg == null) {
            baseEsg = new JsonObject();
            baseEsg.addProperty("isLiveDatabase", true);
        }

        ReportMetricsCalculator.ReportSummary summary = ReportMetricsCalculator.calculate(
                currentPeriod, baseEsg, occurrences, weightsById, materialsById);

        PrefsHelper.putJson(requireContext(), PREFS_REPORTS, KEY_SUMMARY, summary.toJsonObject().toString());

        applySummary(summary);
        updateTopSectors(summary.topSectors);
        bindRecentHistory(occurrences);
        renderPeriodTimeline(occurrences, summary);
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

        renderMaterials(summary);
    }

    private void updateTopSectors(List<ReportMetricsCalculator.SectorStat> topSectors) {
        if (topSectors == null || topSectors.isEmpty()) {
            if (rowTopSector1 != null) rowTopSector1.setVisibility(View.GONE);
            if (rowTopSector2 != null) rowTopSector2.setVisibility(View.GONE);
            if (rowTopSector3 != null) rowTopSector3.setVisibility(View.GONE);
            return;
        }

        if (topSectors.size() > 0 && tvTopSector1Name != null) {
            if (rowTopSector1 != null) rowTopSector1.setVisibility(View.VISIBLE);
            ReportMetricsCalculator.SectorStat s1 = topSectors.get(0);
            tvTopSector1Name.setText(s1.name);
            tvTopSector1Kg.setText(String.format(new Locale("pt", "BR"), "%,d kg", s1.kg));
            setBarWeight(viewTopSector1Bar, viewTopSector1BarEmpty, s1.percentOfMax);
        } else if (rowTopSector1 != null) {
            rowTopSector1.setVisibility(View.GONE);
        }

        if (topSectors.size() > 1 && tvTopSector2Name != null) {
            if (rowTopSector2 != null) rowTopSector2.setVisibility(View.VISIBLE);
            ReportMetricsCalculator.SectorStat s2 = topSectors.get(1);
            tvTopSector2Name.setText(s2.name);
            tvTopSector2Kg.setText(String.format(new Locale("pt", "BR"), "%,d kg", s2.kg));
            setBarWeight(viewTopSector2Bar, viewTopSector2BarEmpty, s2.percentOfMax);
        } else if (rowTopSector2 != null) {
            rowTopSector2.setVisibility(View.GONE);
        }

        if (topSectors.size() > 2 && tvTopSector3Name != null) {
            if (rowTopSector3 != null) rowTopSector3.setVisibility(View.VISIBLE);
            ReportMetricsCalculator.SectorStat s3 = topSectors.get(2);
            tvTopSector3Name.setText(s3.name);
            tvTopSector3Kg.setText(String.format(new Locale("pt", "BR"), "%,d kg", s3.kg));
            setBarWeight(viewTopSector3Bar, viewTopSector3BarEmpty, s3.percentOfMax);
        } else if (rowTopSector3 != null) {
            rowTopSector3.setVisibility(View.GONE);
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

    private void bindRecentHistory(List<Occurrence> occurrences) {
        if (occurrences == null || occurrences.isEmpty()) {
            View.OnClickListener openOccurrences = v ->
                    Navigation.findNavController(v).navigate(R.id.nav_occurrences);
            if (rowRecentHistory1 != null) rowRecentHistory1.setOnClickListener(openOccurrences);
            if (rowRecentHistory2 != null) rowRecentHistory2.setOnClickListener(openOccurrences);
            if (rowRecentHistory3 != null) rowRecentHistory3.setOnClickListener(openOccurrences);
            return;
        }

        if (occurrences.size() > 0 && rowRecentHistory1 != null) {
            bindOccurrenceRow(rowRecentHistory1, cardRecentHistoryIcon1, icRecentHistory1,
                    tvRecentHistoryTitle1, tvRecentHistorySub1, tvRecentHistoryBadge1,
                    occurrences.get(0));
        }

        if (occurrences.size() > 1 && rowRecentHistory2 != null) {
            bindOccurrenceRow(rowRecentHistory2, cardRecentHistoryIcon2, icRecentHistory2,
                    tvRecentHistoryTitle2, tvRecentHistorySub2, tvRecentHistoryBadge2,
                    occurrences.get(1));
        }

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
            String sector = occ.getSetor() != null ? occ.getSetor() : "Planta Principal";
            String time = occ.getTempoRelativo() != null ? occ.getTempoRelativo() : "Hoje";
            if (time.length() > 0 && Character.isLowerCase(time.charAt(0))) {
                time = Character.toUpperCase(time.charAt(0)) + time.substring(1);
            }
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

        row.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("occurrenceId", occ.getId());
            if (occ.getPhotoUrl() != null && !occ.getPhotoUrl().isEmpty()) {
                args.putString("photoUrl", occ.getPhotoUrl());
            }
            if (occ.getPhotoPath() != null && !occ.getPhotoPath().isEmpty()) {
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
        if (status == null) return "Em análise";
        String s = status.toUpperCase(Locale.ROOT);
        if (s.contains("CLOSED") || s.contains("RESOLVID") || s.contains("APROVAD") || s.contains("CONCLUID")) {
            return "Aprovada";
        }
        if (s.contains("PENDING") || s.contains("ANALIS") || s.contains("NOVO") || s.contains("AGUARDANDO") || s.contains("ABERTA")) {
            return "Em análise";
        }
        return "Em análise";
    }

    private void applyBadgeStyle(TextView badge, String status) {
        if (status == null) status = "";
        String s = status.toUpperCase(Locale.ROOT);
        if (s.contains("APROVAD") || s.contains("RESOLVID") || s.contains("COLETAD") || s.contains("CLOSED") || s.contains("CONCLUID")) {
            badge.setBackgroundResource(R.drawable.bg_profile_icon_recycled);
            badge.setTextColor(Color.parseColor("#0E7C48"));
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

    private void renderPeriodTimeline(List<Occurrence> occurrences, ReportMetricsCalculator.ReportSummary summary) {
        if (chartReportsTimeline == null) return;

        if (occurrences == null || occurrences.isEmpty()) {
            chartReportsTimeline.setVisibility(View.GONE);
            if (tvReportsVsOccurrences != null) tvReportsVsOccurrences.setText("0");
            if (tvReportsVsKg != null) tvReportsVsKg.setText("0 kg");
            if (tvReportsVsRate != null) tvReportsVsRate.setText("0%");
            return;
        }

        chartReportsTimeline.setVisibility(View.VISIBLE);

        int count;
        String[] labels;
        float[] values;

        if (currentPeriod == ReportMetricsCalculator.Period.WEEK) {
            count = 7;
            labels = new String[]{"Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"};
            values = new float[7];
            for (Occurrence occ : occurrences) {
                if (occ != null && occ.getId() != null) {
                    int dayIdx = Math.abs(occ.getId().hashCode()) % 7;
                    values[dayIdx] += 1f;
                }
            }
        } else if (currentPeriod == ReportMetricsCalculator.Period.QUARTER) {
            count = 3;
            labels = new String[]{"Mês 1", "Mês 2", "Mês 3"};
            values = new float[3];
            for (Occurrence occ : occurrences) {
                if (occ != null && occ.getId() != null) {
                    int mIdx = Math.abs(occ.getId().hashCode()) % 3;
                    values[mIdx] += 1f;
                }
            }
        } else {
            count = 4;
            labels = new String[]{"Sem 1", "Sem 2", "Sem 3", "Sem 4"};
            values = new float[4];
            for (Occurrence occ : occurrences) {
                if (occ != null && occ.getId() != null) {
                    int wIdx = Math.abs(occ.getId().hashCode()) % 4;
                    values[wIdx] += 1f;
                }
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entries.add(new BarEntry(i, Math.max(values[i], 0.2f)));
        }

        BarDataSet set = new BarDataSet(entries, "Evolução");
        set.setColor(Color.parseColor("#1BA463"));
        set.setDrawValues(false);
        set.setHighLightAlpha(0);

        BarData data = new BarData(set);
        data.setBarWidth(0.5f);

        chartReportsTimeline.setData(data);
        chartReportsTimeline.getDescription().setEnabled(false);
        chartReportsTimeline.getLegend().setEnabled(false);
        chartReportsTimeline.setTouchEnabled(false);
        chartReportsTimeline.setDrawGridBackground(false);

        XAxis xAxis = chartReportsTimeline.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(requireContext().getColor(R.color.vf_text_secondary));
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        chartReportsTimeline.getAxisLeft().setEnabled(false);
        chartReportsTimeline.getAxisRight().setEnabled(false);
        chartReportsTimeline.setViewPortOffsets(12f, 4f, 12f, 24f);
        chartReportsTimeline.animateY(600);
        chartReportsTimeline.invalidate();

        if (tvReportsVsOccurrences != null) {
            tvReportsVsOccurrences.setText(String.valueOf(summary != null ? summary.totalCount : occurrences.size()));
        }
        if (tvReportsVsKg != null) {
            tvReportsVsKg.setText(String.format(new Locale("pt", "BR"), "%,d kg", summary != null ? summary.totalKg : 0));
        }
        if (tvReportsVsRate != null) {
            tvReportsVsRate.setText(summary != null ? (summary.recyclingRate + "%") : "0%");
        }
    }

    private int getMaterialChartColor(String material) {
        String lower = material != null ? material.toLowerCase(Locale.ROOT) : "";
        if (lower.contains("papel") || lower.contains("cart")) {
            return Color.parseColor("#C68A10");
        } else if (lower.contains("plást") || lower.contains("plast") || lower.contains("pet")) {
            return Color.parseColor("#3B82F6");
        } else if (lower.contains("metal") || lower.contains("alum") || lower.contains("aço") || lower.contains("ferro")) {
            return Color.parseColor("#8B6FE0");
        } else if (lower.contains("vidro")) {
            return Color.parseColor("#1BA463");
        } else if (lower.contains("madeira") || lower.contains("palet")) {
            return Color.parseColor("#A16207");
        } else if (lower.contains("orgân") || lower.contains("organ")) {
            return Color.parseColor("#10B981");
        } else if (lower.contains("eletr")) {
            return Color.parseColor("#6366F1");
        } else if (lower.contains("perig")) {
            return Color.parseColor("#EF4444");
        }
        return Color.parseColor("#64748B");
    }

    private void renderMaterials(ReportMetricsCalculator.ReportSummary summary) {
        Map<String, Integer> materialBreakdown = summary != null ? summary.materialBreakdown : null;
        materialContainer.removeAllViews();
        float density = getResources().getDisplayMetrics().density;

        if (materialBreakdown == null || materialBreakdown.isEmpty()) {
            if (chartMaterialsPie != null) {
                chartMaterialsPie.setVisibility(View.GONE);
            }
            TextView emptyText = new TextView(requireContext());
            emptyText.setText(R.string.reports_nothing);
            emptyText.setTextColor(requireContext().getColor(R.color.vf_text_secondary));
            emptyText.setTextSize(13);
            emptyText.setPadding(0, (int) (12 * density), 0, (int) (12 * density));
            materialContainer.addView(emptyText);
            return;
        }

        List<Map.Entry<String, Integer>> items = new ArrayList<>();
        int max = 1;

        for (Map.Entry<String, Integer> e : materialBreakdown.entrySet()) {
            items.add(e);
            max = Math.max(max, e.getValue());
        }

        if (chartMaterialsPie != null) {
            List<PieEntry> entries = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();
            for (Map.Entry<String, Integer> item : items) {
                if (item.getValue() > 0) {
                    entries.add(new PieEntry(item.getValue(), item.getKey()));
                    colors.add(getMaterialChartColor(item.getKey()));
                }
            }

            if (!entries.isEmpty()) {
                chartMaterialsPie.setVisibility(View.VISIBLE);
                PieDataSet dataSet = new PieDataSet(entries, "");
                dataSet.setColors(colors);
                dataSet.setSliceSpace(2.5f);
                dataSet.setDrawValues(true);
                dataSet.setValueTextColor(Color.WHITE);
                dataSet.setValueTextSize(11f);
                dataSet.setValueFormatter(new PercentFormatter(chartMaterialsPie));

                PieData pieData = new PieData(dataSet);
                chartMaterialsPie.setData(pieData);
                chartMaterialsPie.setUsePercentValues(true);
                chartMaterialsPie.setDrawHoleEnabled(true);
                chartMaterialsPie.setHoleColor(Color.TRANSPARENT);
                chartMaterialsPie.setHoleRadius(55f);
                chartMaterialsPie.setTransparentCircleRadius(60f);
                chartMaterialsPie.setTransparentCircleColor(Color.parseColor("#151BA463"));
                int totalKg = summary != null ? summary.totalKg : 0;
                chartMaterialsPie.setCenterText(String.format(new Locale("pt", "BR"), "%,d kg\nTotal", totalKg));
                chartMaterialsPie.setCenterTextSize(13f);
                chartMaterialsPie.setCenterTextColor(requireContext().getColor(R.color.volta_green_primary));
                chartMaterialsPie.getDescription().setEnabled(false);
                chartMaterialsPie.getLegend().setEnabled(false);
                chartMaterialsPie.setDrawEntryLabels(false);
                chartMaterialsPie.animateY(700);
                chartMaterialsPie.invalidate();
            } else {
                chartMaterialsPie.setVisibility(View.GONE);
            }
        }

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
            name.setMaxLines(1);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);

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
