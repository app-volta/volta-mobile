package com.aula.volta;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Carrossel da Home (Figma 790:1624): slide CTA + slide Últimos 7 dias.
 * Barras do slide 2 nas proporções do Figma (mock; API depois).
 */
public class CarouselAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_CTA = 0;
    private static final int TYPE_WEEK = 1;

    /** Alturas das 14 mini barras (proporção Figma, máx 90). */
    private static final int[] WEEK_BARS = {90, 40, 90, 52, 90, 32, 90, 74, 90, 47, 90, 61, 90, 90};

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
            cta.registerButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRegisterClick();
                }
            });
        } else if (holder instanceof WeekHolder) {
            WeekHolder week = (WeekHolder) holder;
            if (week.bars.getChildCount() == 0) {
                float density = week.bars.getResources().getDisplayMetrics().density;
                for (int height : WEEK_BARS) {
                    View bar = new View(week.bars.getContext());
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            0, (int) (height / 90f * 72 * density), 1f);
                    params.setMargins((int) (2 * density), 0, (int) (2 * density), 0);
                    params.gravity = android.view.Gravity.BOTTOM;
                    bar.setLayoutParams(params);
                    bar.setBackgroundResource(R.drawable.bg_bar_mini);
                    week.bars.addView(bar);
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
