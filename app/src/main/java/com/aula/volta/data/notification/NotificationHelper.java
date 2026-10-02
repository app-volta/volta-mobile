package com.aula.volta.data.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.aula.volta.MainActivity;
import com.aula.volta.R;

/**
 * Utilitário central para criação de canais e emissão de notificações
 * na bandeja do sistema Android.
 * Fase 26 — Canais de Notificação Android & Notificações Locais em Tempo Real.
 */
public final class NotificationHelper {

    public static final String CHANNEL_OCCURRENCES = "volta_occurrences";
    public static final String CHANNEL_GOALS = "volta_goals";
    public static final String CHANNEL_SYNC = "volta_sync";

    private NotificationHelper() {
    }

    /**
     * Inicializa os canais de notificação exigidos no Android 8.0 (API 26) ou superior.
     */
    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        // 1. Canal de Ocorrências e Coletas (Alta prioridade / som / popup)
        NotificationChannel occChannel = new NotificationChannel(
                CHANNEL_OCCURRENCES,
                context.getString(R.string.channel_occurrences_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        occChannel.setDescription(context.getString(R.string.channel_occurrences_desc));
        occChannel.enableVibration(true);
        manager.createNotificationChannel(occChannel);

        // 2. Canal de Metas e Relatórios PGRS (Importância padrão)
        NotificationChannel goalsChannel = new NotificationChannel(
                CHANNEL_GOALS,
                context.getString(R.string.channel_goals_name),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        goalsChannel.setDescription(context.getString(R.string.channel_goals_desc));
        manager.createNotificationChannel(goalsChannel);

        // 3. Canal de Sincronização Offline (Silencioso)
        NotificationChannel syncChannel = new NotificationChannel(
                CHANNEL_SYNC,
                context.getString(R.string.channel_sync_name),
                NotificationManager.IMPORTANCE_LOW
        );
        syncChannel.setDescription(context.getString(R.string.channel_sync_desc));
        manager.createNotificationChannel(syncChannel);
    }

    /**
     * Emite notificação de nova ocorrência registrada.
     */
    public static void notifyOccurrenceCreated(Context context, String occurrenceId,
                                              String material, String setor) {
        String title = context.getString(R.string.notif_occurrence_title, occurrenceId);
        String body = context.getString(R.string.notif_occurrence_desc, material, setor);
        showNotification(context, CHANNEL_OCCURRENCES, (int) System.currentTimeMillis(),
                title, body, occurrenceId);
    }

    /**
     * Emite notificação de coleta agendada com a cooperativa.
     */
    public static void notifyPickupScheduled(Context context, String coopName, String horario) {
        String title = context.getString(R.string.notif_pickup_title);
        String body = context.getString(R.string.notif_pickup_body, coopName, horario);
        showNotification(context, CHANNEL_OCCURRENCES, (int) System.currentTimeMillis(),
                title, body, null);
    }

    /**
     * Emite notificação de ocorrência finalizada no PGRS.
     */
    public static void notifyPgrsFinalized(Context context, String occurrenceId) {
        String title = context.getString(R.string.notif_pgrs_finalized_title, occurrenceId);
        String body = context.getString(R.string.notif_pgrs_finalized_body);
        showNotification(context, CHANNEL_GOALS, (int) System.currentTimeMillis(),
                title, body, occurrenceId);
    }

    /**
     * Emite notificação quando uma ocorrência offline é sincronizada na nuvem.
     */
    public static void notifySyncComplete(Context context, String occurrenceId,
                                          String material, String setor) {
        String title = context.getString(R.string.sync_success_title, occurrenceId);
        String body = context.getString(R.string.sync_success_desc, material, setor);
        showNotification(context, CHANNEL_SYNC, (int) System.currentTimeMillis(),
                title, body, occurrenceId);
    }

    private static void showNotification(Context context, String channelId, int notificationId,
                                        String title, String body, String occurrenceId) {
        try {
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            if (!manager.areNotificationsEnabled()) {
                return;
            }

            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            if (occurrenceId != null) {
                intent.putExtra("occurrenceId", occurrenceId);
            }

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, intent, flags);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_notification_bell)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                    .setPriority(CHANNEL_OCCURRENCES.equals(channelId)
                            ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            manager.notify(notificationId, builder.build());
        } catch (SecurityException ignored) {
            // Permissão POST_NOTIFICATIONS não concedida pelo usuário
        }
    }
}
