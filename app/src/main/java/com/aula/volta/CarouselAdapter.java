package com.aula.volta;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aula.volta.data.model.Occurrence;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CarouselAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_CTA = 0;
    public static final int VIEW_TYPE_WEEK = 1;

    public interface OnCarouselClickListener {
        void onRegisterClick();
        void onReportsClick();
    }

    private final OnCarouselClickListener listener;
    private final List<Occurrence> occurrences = new ArrayList<>();
    private int goalPercent = 0;

    public CarouselAdapter(OnCarouselClickListener listener) {
        this.listener = listener;
    }

    public void setOccurrences(List<Occurrence> list) {
        this.occurrences.clear();
        if (list != null) {
            this.occurrences.addAll(list);
        }
        notifyItemChanged(VIEW_TYPE_WEEK);
    }

    public void setGoalPercent(int percent) {
        this.goalPercent = percent;
        notifyItemChanged(VIEW_TYPE_WEEK);
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_CTA : VIEW_TYPE_WEEK;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_WEEK) {
            View view = inflater.inflate(R.layout.item_carousel_week, parent, false);
            return new WeekHolder(view);
        }
        View view = inflater.inflate(R.layout.item_carousel_cta, parent, false);
        return new CtaHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof CtaHolder) {
            CtaHolder cta = (CtaHolder) holder;
            if (cta.registerButton != null) {
                cta.registerButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onRegisterClick();
                    }
                });
            }
        } else if (holder instanceof WeekHolder) {
            WeekHolder week = (WeekHolder) holder;
            week.bind(occurrences, goalPercent, listener);
        }
    }

    static class CtaHolder extends RecyclerView.ViewHolder {
        final View registerButton;

        CtaHolder(@NonNull View itemView) {
            super(itemView);
            registerButton = itemView.findViewById(R.id.btnSlideRegister);
        }
    }

    static class WeekHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvSubtitle;
        final View btnReports;
        final BarChart barChart;

        WeekHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvSlideWeekTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSlideWeekSubtitle);
            btnReports = itemView.findViewById(R.id.btnSlideReports);
            barChart = itemView.findViewById(R.id.chartCarouselWeek);
        }

        void bind(List<Occurrence> occurrences, int goalPercent, OnCarouselClickListener listener) {
            if (btnReports != null) {
                btnReports.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onReportsClick();
                    }
                });
            }

            SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
            SimpleDateFormat labelFormat = new SimpleDateFormat("EEE", new Locale("pt", "BR"));

            String[] dayKeys = new String[7];
            String[] dayLabels = new String[7];
            float[] dayCounts = new float[7];

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -6);
            for (int i = 0; i < 7; i++) {
                Date d = cal.getTime();
                dayKeys[i] = keyFormat.format(d);
                String rawLbl = labelFormat.format(d).replace(".", "").trim();
                dayLabels[i] = rawLbl.length() > 0
                        ? Character.toUpperCase(rawLbl.charAt(0)) + (rawLbl.length() > 1 ? rawLbl.substring(1) : "")
                        : "";
                dayCounts[i] = 0f;
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }

            float total7Days = 0f;
            if (occurrences != null) {
                for (Occurrence occ : occurrences) {
                    if (occ == null) continue;
                    String dateStr = occ.getTempoRelativo();
                    for (int i = 0; i < 7; i++) {
                        if (dateStr != null && dateStr.contains(dayKeys[i])) {
                            dayCounts[i] += 1f;
                            total7Days += 1f;
                            break;
                        }
                    }
                }
            }

            if (tvSubtitle != null) {
                if (goalPercent > 0) {
                    tvSubtitle.setText(goalPercent + "% da meta da unidade.\nVeja tudo nos relatórios.");
                } else if (total7Days > 0) {
                    tvSubtitle.setText(String.format(Locale.getDefault(), "%.0f ocorrências nos últimos 7 dias.\nVeja tudo nos relatórios.", total7Days));
                } else {
                    tvSubtitle.setText("Acompanhe o ritmo da unidade nos relatórios.");
                }
            }

            if (barChart != null) {
                List<BarEntry> entries = new ArrayList<>();
                for (int i = 0; i < 7; i++) {
                    entries.add(new BarEntry(i, Math.max(dayCounts[i], 0.15f)));
                }

                BarDataSet set = new BarDataSet(entries, "7 Dias");
                set.setColor(Color.parseColor("#E6FFFFFF"));
                set.setDrawValues(false);
                set.setHighLightAlpha(0);

                BarData data = new BarData(set);
                data.setBarWidth(0.55f);

                barChart.setData(data);
                barChart.getDescription().setEnabled(false);
                barChart.getLegend().setEnabled(false);
                barChart.setTouchEnabled(false);
                barChart.setDrawGridBackground(false);
                barChart.setDrawBarShadow(false);

                XAxis xAxis = barChart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setDrawGridLines(false);
                xAxis.setDrawAxisLine(false);
                xAxis.setTextColor(Color.parseColor("#E6FFFFFF"));
                xAxis.setTextSize(9f);
                xAxis.setGranularity(1f);
                xAxis.setValueFormatter(new IndexAxisValueFormatter(dayLabels));

                barChart.getAxisLeft().setEnabled(false);
                barChart.getAxisRight().setEnabled(false);
                barChart.setViewPortOffsets(8f, 4f, 8f, 20f);
                barChart.invalidate();
            }
        }
    }
}