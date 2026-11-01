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

    public List<Notification> getItems() {
        return items;
    }

    public Notification getItem(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    public void removeItem(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
        }
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
                if (notification.getTimestamp() > 0) {
            holder.time.setText(formatRelativeTime(notification.getTimestamp()));
        } else {
            String time = notification.getTempoRelativo();
            if (time != null) {
                if (time.toLowerCase(java.util.Locale.ROOT).contains("há ontem")) {
                    time = time.replace("há ontem", "ontem").replace("Há ontem", "Ontem");
                }
                if (!time.isEmpty() && Character.isLowerCase(time.charAt(0))) {
                    time = Character.toUpperCase(time.charAt(0)) + time.substring(1);
                }
            }
            holder.time.setText(time != null ? time : "Agora");
        }

        holder.icon.setImageResource(iconFor(notification.getIcone()));
        applyTintAndBg(holder, notification);

        boolean unread = !notification.isLida();
        holder.itemView.setBackgroundResource(
                unread ? R.drawable.bg_notif_card_unread : R.drawable.bg_notif_card_read);
        holder.badge.setVisibility(unread ? View.VISIBLE : View.GONE);

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
    public static String formatRelativeTime(long timestamp) {
        if (timestamp <= 0) return "Agora";
        long now = System.currentTimeMillis();
        long diff = Math.max(0, now - timestamp);
        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);

        if (minutes < 1) {
            return "Agora";
        } else if (minutes < 60) {
            return "há " + minutes + " min";
        } else if (hours < 24) {
            return "há " + hours + " h";
        } else if (days == 1) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            return "Ontem às " + sdf.format(new java.util.Date(timestamp));
        } else if (days < 7) {
            return "há " + days + " dias";
        } else {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault());
            return sdf.format(new java.util.Date(timestamp));
        }
    }

}
