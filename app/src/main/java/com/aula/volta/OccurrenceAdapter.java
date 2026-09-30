package com.aula.volta;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aula.volta.data.model.Occurrence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter dos cards de ocorrência (Fase 2). Mesmo visual do Figma, 100% orientado a dados:
 * badge por prioridade, ícone/fundo por material. Reutilizado na Fase 5 (lista Ocorrências).
 */
public class OccurrenceAdapter extends RecyclerView.Adapter<OccurrenceAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Occurrence occurrence);
    }

    private final List<Occurrence> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public OccurrenceAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Occurrence> occurrences) {
        items.clear();
        if (occurrences != null) {
            items.addAll(occurrences);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_occurrence_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Occurrence occurrence = items.get(position);

        holder.title.setText(occurrence.getTitulo());
        holder.subtitle.setText(occurrence.getSetor() + " · " + occurrence.getTempoRelativo());

        applyPriorityStyle(holder);
        applyMaterialStyle(holder);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(occurrence);
            }
        });
    }

    private void applyPriorityStyle(ViewHolder holder) {
        Occurrence occurrence = items.get(holder.getBindingAdapterPosition());
        String priority = occurrence.getPrioridade() == null ? "" : occurrence.getPrioridade();

        if (priority.equalsIgnoreCase("ALTA")) {
            holder.badge.setBackgroundResource(R.drawable.bg_badge_alta);
            holder.badge.setTextColor(holder.itemView.getContext()
                    .getColor(R.color.priority_high_text));
            holder.badge.setText(R.string.priority_alta);
        } else if (priority.equalsIgnoreCase("BAIXA")) {
            holder.badge.setBackgroundResource(R.drawable.bg_badge_baixa);
            holder.badge.setTextColor(holder.itemView.getContext()
                    .getColor(R.color.priority_low_text));
            holder.badge.setText(R.string.priority_baixa);
        } else {
            holder.badge.setBackgroundResource(R.drawable.bg_badge_media);
            holder.badge.setTextColor(holder.itemView.getContext()
                    .getColor(R.color.priority_medium_text));
            holder.badge.setText(R.string.priority_media);
        }
    }

    private void applyMaterialStyle(ViewHolder holder) {
        Occurrence occurrence = items.get(holder.getBindingAdapterPosition());
        String titulo = occurrence.getTitulo() == null ? ""
                : occurrence.getTitulo().toLowerCase(Locale.ROOT);

        if (titulo.contains("plast")) {
            holder.iconContainer.setBackgroundResource(R.drawable.bg_occurrence_icon_blue);
            holder.icon.setImageResource(R.drawable.ic_occurrence_waste_blue);
        } else if (titulo.contains("metal") || titulo.contains("sucata")
                || titulo.contains("ferro")) {
            holder.iconContainer.setBackgroundResource(R.drawable.bg_occurrence_icon_purple);
            holder.icon.setImageResource(R.drawable.ic_occurrence_waste_purple);
        } else {
            holder.iconContainer.setBackgroundResource(R.drawable.bg_occurrence_icon);
            holder.icon.setImageResource(R.drawable.ic_occurrence_waste);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout iconContainer;
        final ImageView icon;
        final TextView title;
        final TextView subtitle;
        final TextView badge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconContainer = itemView.findViewById(R.id.cardIconContainer);
            icon = itemView.findViewById(R.id.cardIcon);
            title = itemView.findViewById(R.id.cardTitle);
            subtitle = itemView.findViewById(R.id.cardSubtitle);
            badge = itemView.findViewById(R.id.cardBadge);
        }
    }
}
