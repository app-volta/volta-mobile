package com.aula.volta.data.notification;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.aula.volta.data.local.NotificationStore;
import com.aula.volta.data.model.Notification;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.UUID;

/**
 * Serviço do Firebase Cloud Messaging (FCM) para o app VOLTA.
 * Recebe mensagens push enviadas via console ou backend e encaminha para o
 * NotificationStore (memória/cache) e para a bandeja do sistema Android.
 */
public class VoltaFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "VoltaFCM";
    private static final String PREFS_FCM = "volta_fcm_prefs";
    private static final String KEY_FCM_TOKEN = "fcm_token";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Novo token FCM gerado: " + token);
        saveTokenLocally(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Mensagem FCM recebida de: " + remoteMessage.getFrom());

        String title = null;
        String body = null;
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        Map<String, String> data = remoteMessage.getData();
        if (data != null && !data.isEmpty()) {
            if (data.containsKey("titulo")) {
                title = data.get("titulo");
            } else if (data.containsKey("title")) {
                title = data.get("title");
            }

            if (data.containsKey("descricao")) {
                body = data.get("descricao");
            } else if (data.containsKey("body")) {
                body = data.get("body");
            }
        }

        if (title == null || title.isEmpty()) {
            title = "VOLTA Notificação";
        }
        if (body == null) {
            body = "";
        }

        String id = (data != null && data.containsKey("id")) ? data.get("id") : "fcm_" + UUID.randomUUID().toString().substring(0, 8);
        String tipo = (data != null && data.containsKey("tipo")) ? data.get("tipo") : "NOVA_OCORRENCIA";
        String tempoRelativo = (data != null && data.containsKey("tempo_relativo")) ? data.get("tempo_relativo") : "Agora";
        String icone = (data != null && data.containsKey("icone")) ? data.get("icone") : "bell";
        String tint = (data != null && data.containsKey("tint")) ? data.get("tint") : "#12A05E";
        String bg = (data != null && data.containsKey("bg")) ? data.get("bg") : "#E2F7EC";
        String occurrenceId = (data != null && data.containsKey("occurrence_id")) ? data.get("occurrence_id") : null;

        Notification notification = new Notification(
                id,
                tipo,
                title,
                body,
                tempoRelativo,
                icone,
                tint,
                bg,
                false,
                occurrenceId
        );

        // 1. Armazena no cache unificado do app (UI atualiza imediatamente)
        NotificationStore.pushLocal(this, notification);

        // 2. Emite notificação visual na bandeja do sistema Android
        String channelId = NotificationHelper.CHANNEL_OCCURRENCES;
        if ("META".equalsIgnoreCase(tipo)) {
            channelId = NotificationHelper.CHANNEL_GOALS;
        } else if ("SYNC".equalsIgnoreCase(tipo)) {
            channelId = NotificationHelper.CHANNEL_SYNC;
        }
        NotificationHelper.notifyRemote(this, channelId, title, body, occurrenceId);
    }

    private void saveTokenLocally(String token) {
        SharedPreferences sp = getSharedPreferences(PREFS_FCM, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_FCM_TOKEN, token).apply();
    }

    public static String getSavedToken(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_FCM, Context.MODE_PRIVATE);
        return sp.getString(KEY_FCM_TOKEN, null);
    }
}
