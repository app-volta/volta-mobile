package com.aula.volta.data.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.aula.volta.MainActivity;
import com.aula.volta.R;
import com.aula.volta.data.local.NotificationStore;
import com.aula.volta.data.model.Notification;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utilitário central para criação de canais, emissão de notificações na bandeja do sistema Android,
 * armazenamento no cache unificado e replicação em tempo real no Cloud Firestore com garantia anti-duplicação.
 */
public final class NotificationHelper {

    private static final String TAG = "NotificationHelper";

    public static final String CHANNEL_OCCURRENCES = "volta_occurrences";
    public static final String CHANNEL_GOALS = "volta_goals";
    public static final String CHANNEL_SYNC = "volta_sync";

    // Debouncing in-memory para suprimir qualquer emissão duplicada no sistema Android dentro da janela de 3s
    private static final Map<String, Long> sRecentlyEmitted = new ConcurrentHashMap<>();
    private static final long DEBOUNCE_WINDOW_MS = 3000L;

    private NotificationHelper() {
    }

    /**
     * Calcula um ID numérico determinístico para a notificação na bandeja do Android.
     * Isso assegura que re-emissões do mesmo evento apenas atualizem a notificação existente,
     * em vez de empilhar notificações duplicadas.
     */
    public static int computeNotificationId(String occurrenceId, String title, String body) {
        if (occurrenceId != null && !occurrenceId.isEmpty()) {
            return Math.abs(occurrenceId.hashCode());
        }
        String key = (title != null ? title : "") + "_" + (body != null ? body : "");
        return Math.abs(key.hashCode());
    }

    private static boolean isDuplicateDebounced(String occurrenceId, String title, String body) {
        long now = System.currentTimeMillis();
        String key = (occurrenceId != null ? occurrenceId : "") + "|" + (title != null ? title : "") + "|" + (body != null ? body : "");
        Long last = sRecentlyEmitted.get(key);
        if (last != null && (now - last) < DEBOUNCE_WINDOW_MS) {
            Log.d(TAG, "Notificação suprimida por debouncing recente: " + key);
            return true;
        }
        sRecentlyEmitted.put(key, now);
        if (sRecentlyEmitted.size() > 60) {
            sRecentlyEmitted.entrySet().removeIf(entry -> (now - entry.getValue()) > 10000L);
        }
        return false;
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
     * Emite notificação de nova ocorrência registrada e replica no Firestore com ID determinístico.
     */
    public static void notifyOccurrenceCreated(Context context, String occurrenceId,
                                              String material, String setor) {
        String title = context.getString(R.string.notif_occurrence_title, occurrenceId);
        String body = context.getString(R.string.notif_occurrence_desc, material, setor);
        String singularId = "occ_" + occurrenceId + "_" + java.util.UUID.randomUUID().toString();
        int notificationId = computeNotificationId(occurrenceId, title, body);

        showNotification(context, CHANNEL_OCCURRENCES, notificationId,
                title, body, occurrenceId);

        NotificationStore.pushLocal(context, new Notification(
                singularId,
                "NOVA_OCORRENCIA",
                title,
                body,
                context.getString(R.string.notif_now),
                "bell",
                "#C68A10",
                "#FDF3DE",
                false,
                occurrenceId
        ));

        String myEmail = com.aula.volta.data.local.SessionManager.email(context);
        FirestoreNotificationRepository.getInstance().sendNotification(
                singularId, title, body, "NOVA_OCORRENCIA", occurrenceId, "bell", "#C68A10", "#FDF3DE", myEmail);
    }

    /**
     * Emite notificação de ocorrência enviada para análise técnica e replica no Firestore.
     */
    public static void notifyOccurrenceSent(Context context, String occurrenceId,
                                            String material, String setor) {
        String title = "Ocorrência #" + occurrenceId + " enviada";
        String body = (material != null && !material.isEmpty())
                ? material + " · Enviada para análise técnica"
                : "Enviada para análise · Aguardando validação técnica";
        String singularId = "sent_" + occurrenceId + "_" + java.util.UUID.randomUUID().toString();
        int notificationId = computeNotificationId(occurrenceId, title, body);

        showNotification(context, CHANNEL_OCCURRENCES, notificationId,
                title, body, occurrenceId);

        NotificationStore.pushLocal(context, new Notification(
                singularId,
                "NOVA_OCORRENCIA",
                title,
                body,
                context.getString(R.string.notif_now),
                "bell",
                "#12A05E",
                "#E2F7EC",
                false,
                occurrenceId
        ));

        String myEmail = com.aula.volta.data.local.SessionManager.email(context);
        FirestoreNotificationRepository.getInstance().sendNotification(
                singularId, title, body, "NOVA_OCORRENCIA", occurrenceId, "bell", "#12A05E", "#E2F7EC", myEmail);
    }

    /**
     * Emite notificação de ocorrência aprovada pela gestão web e replica no Firestore.
     */
    public static void notifyOccurrenceApproved(Context context, String occurrenceId, String material) {
        String title = "Ocorrência #" + occurrenceId + " aprovada";
        String body = (material != null && !material.isEmpty())
                ? material + " · Aprovada e homologada no PGRS"
                : "Aprovada pela gestão técnica e contabilizada no PGRS";
        String singularId = "appr_" + occurrenceId + "_" + java.util.UUID.randomUUID().toString();
        int notificationId = computeNotificationId(occurrenceId, title, body);

        showNotification(context, CHANNEL_OCCURRENCES, notificationId,
                title, body, occurrenceId);

        NotificationStore.pushLocal(context, new Notification(
                singularId,
                "APROVACAO",
                title,
                body,
                context.getString(R.string.notif_now),
                "check",
                "#12A05E",
                "#E2F7EC",
                false,
                occurrenceId
        ));

        String myEmail = com.aula.volta.data.local.SessionManager.email(context);
        FirestoreNotificationRepository.getInstance().sendNotification(
                singularId, title, body, "APROVACAO", occurrenceId, "check", "#12A05E", "#E2F7EC", myEmail);
    }

    /**
     * Emite notificação de coleta agendada com a cooperativa e replica no Firestore.
     */
    public static void notifyPickupScheduled(Context context, String coopName, String horario) {
        String title = context.getString(R.string.notif_pickup_title);
        String body = context.getString(R.string.notif_pickup_body, coopName, horario);
        String singularId = "pickup_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString();
        int notificationId = computeNotificationId(null, title, body);

        showNotification(context, CHANNEL_OCCURRENCES, notificationId,
                title, body, null);

        NotificationStore.pushLocal(context, new Notification(
                singularId,
                "COLETA_AGENDADA",
                title,
                body,
                context.getString(R.string.notif_now),
                "truck",
                "#3B82F6",
                "#E4EFFF",
                false,
                null
        ));

        String myEmail = com.aula.volta.data.local.SessionManager.email(context);
        FirestoreNotificationRepository.getInstance().sendNotification(
                singularId, title, body, "COLETA_AGENDADA", null, "truck", "#3B82F6", "#E4EFFF", myEmail);
    }

    /**
     * Emite notificação nativa de destinação solicitada para cooperativa e replica no Firestore.
     */
    public static void notifyDestinationRequested(Context context, String coopName, String occurrenceId) {
        String title = context.getString(R.string.notif_pick_title, coopName);
        String body = context.getString(R.string.notif_pick_desc, occurrenceId);
        String singularId = "dest_" + occurrenceId + "_" + java.util.UUID.randomUUID().toString();
        int notificationId = computeNotificationId(occurrenceId, title, body);

        showNotification(context, CHANNEL_OCCURRENCES, notificationId,
                title, body, occurrenceId);

        NotificationStore.pushLocal(context, new Notification(
                singularId,
                "COLETA_AGENDADA",
                title,
                body,
                context.getString(R.string.notif_now),
                "truck",
                "#3B82F6",
                "#E4EFFF",
                false,
                occurrenceId
        ));

        String myEmail = com.aula.volta.data.local.SessionManager.email(context);
        FirestoreNotificationRepository.getInstance().sendNotification(
                singularId, title, body, "COLETA_AGENDADA", occurrenceId, "truck", "#3B82F6", "#E4EFFF", myEmail);
    }

    /**
     * Emite notificação de ocorrência finalizada no PGRS e replica no Firestore.
     */
    public static void notifyPgrsFinalized(Context context, String occurrenceId) {
        String title = context.getString(R.string.notif_pgrs_finalized_title, occurrenceId);
        String body = context.getString(R.string.notif_pgrs_finalized_body);
        String singularId = "pgrs_" + occurrenceId + "_" + java.util.UUID.randomUUID().toString();
        int notificationId = computeNotificationId(occurrenceId, title, body);

        showNotification(context, CHANNEL_GOALS, notificationId,
                title, body, occurrenceId);

        NotificationStore.pushLocal(context, new Notification(
                singularId,
                "META",
                title,
                body,
                context.getString(R.string.notif_now),
                "trophy",
                "#8B6FE0",
                "#EFEAFB",
                false,
                occurrenceId
        ));

        String myEmail = com.aula.volta.data.local.SessionManager.email(context);
        FirestoreNotificationRepository.getInstance().sendNotification(
                singularId, title, body, "META", occurrenceId, "trophy", "#8B6FE0", "#EFEAFB", myEmail);
    }

    /**
     * Emite notificação quando uma ocorrência offline é sincronizada na nuvem e replica no Firestore.
     */
    public static void notifySyncComplete(Context context, String occurrenceId,
                                          String material, String setor) {
        String title = context.getString(R.string.sync_success_title, occurrenceId);
        String body = context.getString(R.string.sync_success_desc, material, setor);
        String notifId = "sync_" + occurrenceId;
        int notificationId = computeNotificationId(occurrenceId, title, body);

        showNotification(context, CHANNEL_SYNC, notificationId,
                title, body, occurrenceId);

        NotificationStore.pushLocal(context, new Notification(
                notifId,
                "SYNC",
                title,
                body,
                context.getString(R.string.notif_now),
                "check",
                "#0E7C48",
                "#E2F7EC",
                false,
                occurrenceId
        ));

        FirestoreNotificationRepository.getInstance().sendNotification(
                notifId, title, body, "SYNC", occurrenceId, "check", "#0E7C48", "#E2F7EC");
    }

    /**
     * Emite notificação remota vinda do Firebase (FCM / Firestore).
     */
    public static void notifyRemote(Context context, String channelId, String title,
                                    String body, String occurrenceId) {
        if (channelId == null || channelId.isEmpty()) {
            channelId = CHANNEL_OCCURRENCES;
        }
        int notificationId = computeNotificationId(occurrenceId, title, body);
        showNotification(context, channelId, notificationId,
                title, body, occurrenceId);
    }

    private static void showNotification(Context context, String channelId, int notificationId,
                                        String title, String body, String occurrenceId) {
        if (isDuplicateDebounced(occurrenceId, title, body)) {
            return;
        }
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
