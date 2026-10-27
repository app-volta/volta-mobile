package com.aula.volta.ui.notifications;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aula.volta.NotificationAdapter;
import com.aula.volta.R;
import com.aula.volta.data.local.NotificationStore;
import com.aula.volta.data.model.Notification;
import com.aula.volta.data.notification.FirestoreNotificationRepository;
import com.aula.volta.util.BottomSheetImmersiveHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Bottom sheet Central de Notificações — Volta Design System e Figma (790:1854).
 * Suporta filtros por abas, sincronização Firestore em tempo real, deep linking de alta fidelidade,
 * swipe para arquivar com desfazer e estado vazio enriquecido com o mascote VOLTA.
 */
public class NotificationsSheet extends BottomSheetDialogFragment {

    public static final String REQUEST_KEY_UPDATED = "notifications_updated";

    public enum FilterType {
        ALL,
        UNREAD,
        OCCURRENCES,
        GOALS
    }

    private NotificationAdapter adapter;
    private TextView tvNewCount;
    private View skeletonNotifications;
    private View layoutEmptyNotifications;
    private TextView tvEmptyNotificationsTitle;
    private TextView tvEmptyNotifications;
    private RecyclerView rvNotifications;

    private TextView chipFilterAll;
    private TextView chipFilterUnread;
    private TextView chipFilterOccurrences;
    private TextView chipFilterGoals;

    private FilterType currentFilter = FilterType.ALL;
    private final List<Notification> allNotifications = new ArrayList<>();

    private final NotificationStore.ChangeListener storeListener = () -> {
        if (isAdded()) {
            applyNotifications(NotificationStore.cached(requireContext()));
        }
    };

    public NotificationsSheet() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        BottomSheetImmersiveHelper.configureCollapsibleBottomSheet(dialog);
        return dialog;
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
        skeletonNotifications = view.findViewById(R.id.skeletonNotifications);
        layoutEmptyNotifications = view.findViewById(R.id.layoutEmptyNotifications);
        tvEmptyNotificationsTitle = view.findViewById(R.id.tvEmptyNotificationsTitle);
        tvEmptyNotifications = view.findViewById(R.id.tvEmptyNotifications);
        rvNotifications = view.findViewById(R.id.rvNotifications);

        chipFilterAll = view.findViewById(R.id.chipFilterAll);
        chipFilterUnread = view.findViewById(R.id.chipFilterUnread);
        chipFilterOccurrences = view.findViewById(R.id.chipFilterOccurrences);
        chipFilterGoals = view.findViewById(R.id.chipFilterGoals);

        setupHeaderAndActionButtons(view);
        setupFilterChips();
        setupRecyclerView();

        NotificationStore.addListener(storeListener);
        reload();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (skeletonNotifications != null) {
            com.aula.volta.util.SkeletonPulseAnimator.stop(skeletonNotifications);
        }
        NotificationStore.removeListener(storeListener);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        notifyParentUpdated();
    }

    private void setupHeaderAndActionButtons(@NonNull View view) {
        View btnClose = view.findViewById(R.id.btnCloseNotifications);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        View btnSettings = view.findViewById(R.id.btnNotificationSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                dismiss();
                try {
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.nav_notification_settings);
                } catch (Exception ignored) {
                }
            });
        }

        TextView tvMarkAll = view.findViewById(R.id.tvMarkAllRead);
        if (tvMarkAll != null) {
            tvMarkAll.setOnClickListener(v -> {
                List<Notification> cachedList = NotificationStore.cached(requireContext());
                for (Notification n : cachedList) {
                    if (!n.isLida() && n.getId() != null) {
                        FirestoreNotificationRepository.getInstance().markAsRead(n.getId());
                    }
                }
                NotificationStore.markAllRead(requireContext());
                applyNotifications(NotificationStore.cached(requireContext()));
                notifyParentUpdated();
            });
        }

        TextView tvClearRead = view.findViewById(R.id.tvClearRead);
        if (tvClearRead != null) {
            tvClearRead.setOnClickListener(v -> {
                NotificationStore.clearRead(requireContext());
                applyNotifications(NotificationStore.cached(requireContext()));
                notifyParentUpdated();
            });
        }
    }

    private void setupFilterChips() {
        if (chipFilterAll != null) {
            chipFilterAll.setOnClickListener(v -> selectFilter(FilterType.ALL));
        }
        if (chipFilterUnread != null) {
            chipFilterUnread.setOnClickListener(v -> selectFilter(FilterType.UNREAD));
        }
        if (chipFilterOccurrences != null) {
            chipFilterOccurrences.setOnClickListener(v -> selectFilter(FilterType.OCCURRENCES));
        }
        if (chipFilterGoals != null) {
            chipFilterGoals.setOnClickListener(v -> selectFilter(FilterType.GOALS));
        }
    }

    private void selectFilter(FilterType filter) {
        currentFilter = filter;
        updateChipsUi();
        filterAndDisplay();
    }

    private void updateChipsUi() {
        updateChipState(chipFilterAll, currentFilter == FilterType.ALL);
        updateChipState(chipFilterUnread, currentFilter == FilterType.UNREAD);
        updateChipState(chipFilterOccurrences, currentFilter == FilterType.OCCURRENCES);
        updateChipState(chipFilterGoals, currentFilter == FilterType.GOALS);
    }

    private void updateChipState(TextView chip, boolean selected) {
        if (chip == null) {
            return;
        }
        if (selected) {
            chip.setBackgroundResource(R.drawable.bg_chip_selected);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_unselected);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.volta_text_primary_light));
            chip.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(this::onNotificationClicked);
        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvNotifications.setAdapter(adapter);

        // Deslizar para remover notificação (Swipe-to-dismiss com Undo)
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position >= 0 && position < adapter.getItemCount()) {
                    Notification swipedItem = adapter.getItem(position);
                    if (swipedItem != null) {
                        NotificationStore.delete(requireContext(), swipedItem.getId());
                        if (swipedItem.getId() != null) {
                            FirestoreNotificationRepository.getInstance().deleteNotification(swipedItem.getId());
                        }
                        notifyParentUpdated();

                        View root = getView();
                        if (root != null) {
                            Snackbar snackbar = Snackbar.make(root, R.string.notifications_deleted, Snackbar.LENGTH_LONG);
                            snackbar.setAction(R.string.notifications_undo, undoView -> {
                                NotificationStore.pushLocal(requireContext(), swipedItem);
                                notifyParentUpdated();
                            });
                            snackbar.show();
                        }
                    }
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                   @NonNull RecyclerView.ViewHolder viewHolder,
                                   float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;
                if (dX != 0) {
                    Paint paint = new Paint();
                    paint.setColor(Color.parseColor("#E53935"));
                    Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_trash_white);

                    if (dX > 0) { // Deslizando para a direita
                        c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(), dX, (float) itemView.getBottom(), paint);
                        if (icon != null) {
                            int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                            int iconTop = itemView.getTop() + iconMargin;
                            int iconBottom = iconTop + icon.getIntrinsicHeight();
                            int iconLeft = itemView.getLeft() + iconMargin;
                            int iconRight = iconLeft + icon.getIntrinsicWidth();
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                            icon.draw(c);
                        }
                    } else { // Deslizando para a esquerda
                        c.drawRect((float) itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom(), paint);
                        if (icon != null) {
                            int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                            int iconTop = itemView.getTop() + iconMargin;
                            int iconBottom = iconTop + icon.getIntrinsicHeight();
                            int iconRight = itemView.getRight() - iconMargin;
                            int iconLeft = iconRight - icon.getIntrinsicWidth();
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                            icon.draw(c);
                        }
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(rvNotifications);
    }

    private void onNotificationClicked(Notification notification) {
        if (notification == null) {
            return;
        }

        NotificationStore.markRead(requireContext(), notification.getId());
        if (notification.getId() != null) {
            FirestoreNotificationRepository.getInstance().markAsRead(notification.getId());
        }
        notifyParentUpdated();

        String occurrenceId = notification.getOccurrenceId();
        String tipo = notification.getTipo();

        if (occurrenceId != null && !occurrenceId.trim().isEmpty()) {
            Bundle args = new Bundle();
            args.putString("occurrenceId", occurrenceId);
            dismiss();
            try {
                NavHostFragment.findNavController(this)
                        .navigate(R.id.nav_occurrence_full, args);
            } catch (Exception ignored) {
            }
        } else if (tipo != null && (tipo.equalsIgnoreCase("META")
                || tipo.equalsIgnoreCase("RANKING")
                || tipo.equalsIgnoreCase("PGRS"))) {
            dismiss();
            try {
                NavHostFragment.findNavController(this)
                        .navigate(R.id.nav_reports);
            } catch (Exception ignored) {
            }
        } else if (tipo != null && (tipo.equalsIgnoreCase("COLETA_AGENDADA")
                || tipo.equalsIgnoreCase("COLETA"))) {
            dismiss();
            try {
                NavHostFragment.findNavController(this)
                        .navigate(R.id.nav_cooperatives);
            } catch (Exception ignored) {
            }
        } else {
            applyNotifications(NotificationStore.cached(requireContext()));
        }
    }

    private void notifyParentUpdated() {
        if (isAdded()) {
            getParentFragmentManager().setFragmentResult(REQUEST_KEY_UPDATED, new Bundle());
        }
    }

    private void reload() {
        if (skeletonNotifications != null) {
            skeletonNotifications.setVisibility(View.VISIBLE);
            com.aula.volta.util.SkeletonPulseAnimator.start(skeletonNotifications);
            rvNotifications.setVisibility(View.GONE);
            layoutEmptyNotifications.setVisibility(View.GONE);
        }
        NotificationStore.refresh(requireContext(), this::applyNotifications);
    }

    private void applyNotifications(List<Notification> notifications) {
        if (!isAdded()) {
            return;
        }

        if (skeletonNotifications != null) {
            com.aula.volta.util.SkeletonPulseAnimator.stop(skeletonNotifications);
            skeletonNotifications.setVisibility(View.GONE);
        }

        allNotifications.clear();
        if (notifications != null) {
            allNotifications.addAll(notifications);
        }

        int unread = NotificationStore.unreadCount(allNotifications);
        if (unread > 0) {
            tvNewCount.setVisibility(View.VISIBLE);
            tvNewCount.setText(getString(R.string.notifications_new, unread));
        } else {
            tvNewCount.setVisibility(View.GONE);
        }

        filterAndDisplay();
    }

    private void filterAndDisplay() {
        List<Notification> filtered = new ArrayList<>();
        for (Notification n : allNotifications) {
            if (matchesFilter(n, currentFilter)) {
                filtered.add(n);
            }
        }

        adapter.setItems(filtered);

        if (filtered.isEmpty()) {
            rvNotifications.setVisibility(View.GONE);
            layoutEmptyNotifications.setVisibility(View.VISIBLE);
            if (allNotifications.isEmpty()) {
                tvEmptyNotificationsTitle.setText(R.string.notifications_empty_title);
                tvEmptyNotifications.setText(R.string.notifications_empty_desc);
            } else {
                tvEmptyNotificationsTitle.setText(R.string.filter_title);
                tvEmptyNotifications.setText(R.string.notifications_empty_filter_desc);
            }
        } else {
            layoutEmptyNotifications.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
        }
    }

    private boolean matchesFilter(Notification n, FilterType filter) {
        if (n == null) {
            return false;
        }
        switch (filter) {
            case UNREAD:
                return !n.isLida();
            case OCCURRENCES:
                String occId = n.getOccurrenceId();
                if (occId != null && !occId.trim().isEmpty()) {
                    return true;
                }
                String tipoOcc = n.getTipo() != null ? n.getTipo().toUpperCase() : "";
                return tipoOcc.contains("OCORRENCIA") || tipoOcc.contains("ANALISE") || tipoOcc.contains("STATUS");
            case GOALS:
                String tipoGoals = n.getTipo() != null ? n.getTipo().toUpperCase() : "";
                return tipoGoals.contains("META") || tipoGoals.contains("COLETA") || tipoGoals.contains("RANKING") || tipoGoals.contains("PGRS");
            case ALL:
            default:
                return true;
        }
    }
}
