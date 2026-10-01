package com.aula.volta;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aula.volta.data.model.CooperativeJSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Cards de cooperativas — Figma Cooperativas (626:582).
 * Primeiro item em destaque verde; demais alternam dourado/azul/roxo.
 * Sem nada de ocorrências aqui: só cooperativas (conceito do plano).
 */
public class CooperativeAdapter extends RecyclerView.Adapter<CooperativeAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(CooperativeJSON cooperative);
    }

    private final List<CooperativeJSON> items = new ArrayList<>();
    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<CooperativeJSON> cooperatives) {
        items.clear();
        if (cooperatives != null) {
            items.addAll(cooperatives);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cooperative_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CooperativeJSON coop = items.get(position);

        holder.name.setText(coop.getNome());
        holder.description.setText(coop.getDescricao() == null ? "" : coop.getDescricao());

        String distance = String.format(new Locale("pt", "BR"), "%,.1f km", coop.getDistanciaKm());
        if (coop.getColeta() != null && !coop.getColeta().isEmpty()) {
            distance = distance + " · " + coop.getColeta();
        }
        holder.distance.setText(distance);

        if (coop.getTag() != null && !coop.getTag().isEmpty()) {
            holder.tag.setVisibility(View.VISIBLE);
            holder.tag.setText(coop.getTag());
        } else {
            holder.tag.setVisibility(View.GONE);
        }

        if (coop.getAvaliacao() != null) {
            holder.ratingContainer.setVisibility(View.VISIBLE);
            holder.rating.setText(String.format(new Locale("pt", "BR"), "%.1f", coop.getAvaliacao()));
        } else {
            holder.ratingContainer.setVisibility(View.GONE);
        }

        applyPositionStyle(holder, position);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(coop);
            }
        });
    }

    /** Destaque verde no primeiro; dourado/azul/roxo nos demais (Figma). */
    private void applyPositionStyle(ViewHolder holder, int position) {
        int style = position % 4;
        if (style == 0) {
            holder.itemLayout.setBackgroundResource(R.drawable.bg_card_stat_resolved);
            holder.iconContainer.setBackgroundResource(R.drawable.bg_occurrence_icon_green);
            tintIcon(holder, R.color.stat_resolved_icon);
            holder.distance.setTextColor(holder.itemView.getContext().getColor(R.color.stat_resolved_icon));
        } else if (style == 1) {
            holder.itemLayout.setBackgroundResource(R.drawable.bg_card_surface);
            holder.iconContainer.setBackgroundResource(R.drawable.bg_occurrence_icon);
            tintIcon(holder, R.color.occurrence_icon_tint);
            holder.distance.setTextColor(holder.itemView.getContext().getColor(R.color.occurrence_icon_tint));
        } else if (style == 2) {
            holder.itemLayout.setBackgroundResource(R.drawable.bg_card_surface);
            holder.iconContainer.setBackgroundResource(R.drawable.bg_occurrence_icon_blue);
            tintIcon(holder, R.color.occurrence_icon_tint_blue);
            holder.distance.setTextColor(holder.itemView.getContext().getColor(R.color.occurrence_icon_tint_blue));
        } else {
            holder.itemLayout.setBackgroundResource(R.drawable.bg_card_surface);
            holder.iconContainer.setBackgroundResource(R.drawable.bg_occurrence_icon_purple);
            tintIcon(holder, R.color.occurrence_icon_tint_purple);
            holder.distance.setTextColor(holder.itemView.getContext().getColor(R.color.occurrence_icon_tint_purple));
        }
    }

    private void tintIcon(ViewHolder holder, @ColorRes int colorRes) {
        int color = holder.itemView.getContext().getColor(colorRes);
        holder.icon.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout itemLayout;
        final FrameLayout iconContainer;
        final ImageView icon;
        final TextView name;
        final TextView tag;
        final TextView description;
        final TextView distance;
        final LinearLayout ratingContainer;
        final TextView rating;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemLayout = (LinearLayout) itemView;
            iconContainer = itemView.findViewById(R.id.coopIconContainer);
            icon = itemView.findViewById(R.id.coopIcon);
            name = itemView.findViewById(R.id.coopName);
            tag = itemView.findViewById(R.id.coopTag);
            description = itemView.findViewById(R.id.coopDescription);
            distance = itemView.findViewById(R.id.coopDistance);
            ratingContainer = itemView.findViewById(R.id.coopRatingContainer);
            rating = itemView.findViewById(R.id.coopRating);
        }
    }
}
