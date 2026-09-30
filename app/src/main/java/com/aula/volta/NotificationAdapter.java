package com.aula.volta;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.aula.volta.data.model.Notification;

import java.util.ArrayList;
import java.util.List;

/**
 * Cards de notificação — Figma Notificações (626:780).
 * Ícone + tint + fundo vêm dos dados (data-driven); badge NOVA só nas não lidas.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Notification notification);
    }

    private final List<Notification> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public NotificationAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Notification> notifications) {
        items.clear();
        if (notifications != null) {
            items.addAll(notifications);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = items.get(position);

        holder.title.setText(notification.getTitulo());
        holder.description.setText(notification.getDescricao());
        holder.time.setText(notification.getTempoRelativo());

        holder.icon.setImageResource(iconFor(notification.getIcone()));
        applyTintAndBg(holder, notification);

        holder.badge.setVisibility(notification.isLida() ? View.GONE : View.VISIBLE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(notification);
            }
        });
    }

    private int iconFor(String icone) {
        if (icone == null) {
            return R.drawable.ic_notif_bell;
        }
        switch (icone) {
            case "check":
                return R.drawable.ic_notif_check;
            case "truck":
                return R.drawable.ic_notif_truck;
            case "trophy":
                return R.drawable.ic_notif_trophy;
            case "star":
                return R.drawable.ic_notif_star;
            case "bell":
            default:
                return R.drawable.ic_notif_bell;
        }
    }

    /** Aplica tint do ícone e fundo do círculo vindos da API (hex). */
    private void applyTintAndBg(ViewHolder holder, Notification notification) {
        try {
            if (notification.getTint() != null) {
                holder.icon.setColorFilter(Color.parseColor(notification.getTint()));
            }
        } catch (IllegalArgumentException ignored) {
            holder.icon.clearColorFilter();
        }
        try {
            if (notification.getBg() != null) {
                Drawable bg = holder.iconContainer.getBackground().mutate();
                Drawable wrapped = DrawableCompat.wrap(bg);
                DrawableCompat.setTint(wrapped, Color.parseColor(notification.getBg()));
                holder.iconContainer.setBackground(wrapped);
            }
        } catch (Exception ignored) {
            // mantém fundo padrão do layout
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
        final TextView description;
        final TextView time;
        final TextView badge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconContainer = itemView.findViewById(R.id.notifIconContainer);
            icon = itemView.findViewById(R.id.notifIcon);
            title = itemView.findViewById(R.id.notifTitle);
            description = itemView.findViewById(R.id.notifDescription);
            time = itemView.findViewById(R.id.notifTime);
            badge = itemView.findViewById(R.id.notifBadge);
        }
    }
}
