package com.aula.volta.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aula.volta.NotificationAdapter;
import com.aula.volta.R;
import com.aula.volta.data.local.NotificationStore;
import com.aula.volta.data.model.Notification;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

/**
 * Bottom sheet de notificações — Figma (626:780).
 * Lê sempre do NotificationStore (mock agora, FCM depois sem mudar a tela).
 */
public class NotificationsSheet extends BottomSheetDialogFragment {

    private NotificationAdapter adapter;
    private TextView tvNewCount;
    private TextView tvEmptyNotifications;

    public NotificationsSheet() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvNewCount = view.findViewById(R.id.tvNewCount);
        tvEmptyNotifications = view.findViewById(R.id.tvEmptyNotifications);

        View btnClose = view.findViewById(R.id.btnCloseNotifications);
        btnClose.setOnClickListener(v -> dismiss());

        TextView tvMarkAll = view.findViewById(R.id.tvMarkAllRead);
        tvMarkAll.setOnClickListener(v -> {
            NotificationStore.markAllRead(requireContext());
            reload();
        });

        RecyclerView rvNotifications = view.findViewById(R.id.rvNotifications);
        adapter = new NotificationAdapter(notification -> {
            NotificationStore.markRead(requireContext(), notification.getId());
            if (notification.getOccurrenceId() != null
                    && !notification.getOccurrenceId().isEmpty()) {
                Bundle args = new Bundle();
                args.putString("occurrenceId", notification.getOccurrenceId());
                dismiss();
                NavHostFragment.findNavController(this)
                        .navigate(R.id.nav_occurrence_full, args);
            } else {
                reload();
            }
        });
        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvNotifications.setAdapter(adapter);

        reload();
    }

    private void reload() {
        NotificationStore.refresh(requireContext(), this::applyNotifications);
    }

    private void applyNotifications(List<Notification> notifications) {
        if (!isAdded()) {
            return;
        }
        adapter.setItems(notifications);

        int unread = NotificationStore.unreadCount(notifications);
        if (unread > 0) {
            tvNewCount.setVisibility(View.VISIBLE);
            tvNewCount.setText(getString(R.string.notifications_new, unread));
        } else {
            tvNewCount.setVisibility(View.GONE);
        }

        tvEmptyNotifications.setVisibility(
                notifications == null || notifications.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
