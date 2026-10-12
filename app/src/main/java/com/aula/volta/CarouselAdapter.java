package com.aula.volta;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Carrossel da Home (Figma 790:1648): slide CTA (790:1649) + slide Últimos 7 dias (790:1665).
 * Barras do slide 2: 7 colunas com trilho translúcido de 90dp + barra preenchida nas proporções do Figma.
 */
public class CarouselAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_CTA = 0;
    private static final int TYPE_WEEK = 1;

    /** Alturas em dp das 7 barras preenchidas no Figma (790:1671: máx 90dp). */
    private static final int[] WEEK_FILL_HEIGHTS_DP = {40, 52, 32, 74, 47, 61, 90};

    public interface OnRegisterClickListener {
        void onRegisterClick();
    }

    private final OnRegisterClickListener listener;

    public CarouselAdapter(OnRegisterClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_CTA : TYPE_WEEK;
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_CTA) {
            return new CtaHolder(inflater.inflate(R.layout.item_carousel_cta, parent, false));
        }
        return new WeekHolder(inflater.inflate(R.layout.item_carousel_week, parent, false));
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
            if (week.bars != null && week.bars.getChildCount() == 0) {
                float density = week.bars.getResources().getDisplayMetrics().density;
                int trackHeightPx = Math.round(90 * density);
                int gapPx = Math.round(10 * density);

                for (int i = 0; i < WEEK_FILL_HEIGHTS_DP.length; i++) {
                    FrameLayout track = new FrameLayout(week.bars.getContext());
                    LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(
                            0, trackHeightPx, 1f);
                    if (i < WEEK_FILL_HEIGHTS_DP.length - 1) {
                        trackParams.setMarginEnd(gapPx);
                    }
                    track.setLayoutParams(trackParams);
                    track.setBackgroundResource(R.drawable.bg_chart_bar_track);

                    View fillBar = new View(week.bars.getContext());
                    int fillHeightPx = Math.round(WEEK_FILL_HEIGHTS_DP[i] * density);
                    FrameLayout.LayoutParams fillParams = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            fillHeightPx,
                            Gravity.BOTTOM);
                    fillBar.setLayoutParams(fillParams);
                    boolean isLast = (i == WEEK_FILL_HEIGHTS_DP.length - 1);
                    fillBar.setBackgroundResource(isLast
                            ? R.drawable.bg_chart_bar_active
                            : R.drawable.bg_chart_bar_fill);

                    track.addView(fillBar);
                    week.bars.addView(track);
                }
            }
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
        final LinearLayout bars;

        WeekHolder(@NonNull View itemView) {
            super(itemView);
            bars = itemView.findViewById(R.id.weekBars);
        }
    }
}
