package com.aula.volta.data.notification;

import android.content.Context;
import android.util.Log;

import com.aula.volta.data.local.NotificationStore;
import com.aula.volta.data.model.Notification;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Repositório e gerenciador em tempo real das notificações no Cloud Firestore.
 * Conecta-se à coleção "notifications" e sincroniza instantaneamente com o NotificationStore e
 * a bandeja do sistema Android.
 */
public class FirestoreNotificationRepository {

    private static final String TAG = "FirestoreNotifRepo";
    public static final String COLLECTION_NOTIFICATIONS = "notifications";

    private static volatile FirestoreNotificationRepository instance;
    private ListenerRegistration listenerRegistration;
    private boolean isListening = false;

    private FirestoreNotificationRepository() {
    }

    public static FirestoreNotificationRepository getInstance() {
        if (instance == null) {
            synchronized (FirestoreNotificationRepository.class) {
                if (instance == null) {
                    instance = new FirestoreNotificationRepository();
                }
            }
        }
        return instance;
    }

    /**
     * Inicia a escuta em tempo real da coleção "notifications" no Firestore.
     */
    public synchronized void startListening(Context context) {
        if (isListening && listenerRegistration != null) {
            return;
        }

        Context appContext = context.getApplicationContext();
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Query query = db.collection(COLLECTION_NOTIFICATIONS)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(50);

            listenerRegistration = query.addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Log.w(TAG, "Erro ao escutar notificações do Firestore", error);
                    return;
                }

                if (snapshots == null || snapshots.isEmpty()) {
                    return;
                }

                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    if (dc.getType() == DocumentChange.Type.ADDED) {
                        try {
                            Notification notification = parseDocument(dc.getDocument().getId(), dc.getDocument().getData());
                            if (notification != null) {
                                boolean alreadyCached = NotificationStore.hasNotification(appContext, notification.getId());

                                // Integra diretamente ao NotificationStore do app
                                NotificationStore.pushLocal(appContext, notification);

                                // Se é uma nova notificação remota não lida, notifica na bandeja do sistema
                                if (!alreadyCached && !notification.isLida()) {
                                    String channelId = NotificationHelper.CHANNEL_OCCURRENCES;
                                    if ("META".equalsIgnoreCase(notification.getTipo())) {
                                        channelId = NotificationHelper.CHANNEL_GOALS;
                                    } else if ("SYNC".equalsIgnoreCase(notification.getTipo())) {
                                        channelId = NotificationHelper.CHANNEL_SYNC;
                                    }
                                    NotificationHelper.notifyRemote(
                                            appContext,
                                            channelId,
                                            notification.getTitulo(),
                                            notification.getDescricao(),
                                            notification.getOccurrenceId()
                                        );
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Falha ao processar notificação do Firestore", e);
                        }
                    }
                }
            });

            isListening = true;
            Log.d(TAG, "Listener do Firestore de notificações iniciado com sucesso.");
        } catch (Exception e) {
            Log.e(TAG, "Erro ao inicializar listener do Firestore", e);
        }
    }

    /**
     * Para a escuta de notificações em tempo real.
     */
    public synchronized void stopListening() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
        isListening = false;
    }

    /**
     * Envia uma nova notificação para o Cloud Firestore.
     */
    public void sendNotification(String titulo, String descricao, String tipo,
                                 String occurrenceId, String icone, String tint, String bg) {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> data = new HashMap<>();
            data.put("titulo", titulo);
            data.put("descricao", descricao);
            data.put("tipo", tipo != null ? tipo : "NOVA_OCORRENCIA");
            data.put("occurrence_id", occurrenceId);
            data.put("icone", icone != null ? icone : "bell");
            data.put("tint", tint != null ? tint : "#12A05E");
            data.put("bg", bg != null ? bg : "#E2F7EC");
            data.put("lida", false);
            data.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
            data.put("user_id", "all");

            db.collection(COLLECTION_NOTIFICATIONS)
                    .add(data)
                    .addOnSuccessListener(documentReference ->
                            Log.d(TAG, "Notificação enviada ao Firestore com ID: " + documentReference.getId()))
                    .addOnFailureListener(e ->
                            Log.w(TAG, "Erro ao enviar notificação ao Firestore", e));
        } catch (Exception e) {
            Log.e(TAG, "Exceção ao enviar notificação", e);
        }
    }

    /**
     * Marca uma notificação como lida no Cloud Firestore.
     */
    public void markAsRead(String docId) {
        if (docId == null || docId.isEmpty()) {
            return;
        }
        try {
            FirebaseFirestore.getInstance()
                    .collection(COLLECTION_NOTIFICATIONS)
                    .document(docId)
                    .update("lida", true)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Notificação " + docId + " marcada como lida no Firestore"))
                    .addOnFailureListener(e -> Log.w(TAG, "Erro ao atualizar status de lida no Firestore", e));
        } catch (Exception e) {
            Log.e(TAG, "Exceção ao marcar como lida no Firestore", e);
        }
    }

    private Notification parseDocument(String docId, Map<String, Object> data) {
        if (data == null) {
            return null;
        }

        String titulo = (String) data.get("titulo");
        if (titulo == null) {
            titulo = (String) data.get("title");
        }
        if (titulo == null) {
            titulo = "VOLTA Notificação";
        }

        String descricao = (String) data.get("descricao");
        if (descricao == null) {
            descricao = (String) data.get("body");
        }
        if (descricao == null) {
            descricao = "";
        }

        String tipo = (String) data.get("tipo");
        if (tipo == null) {
            tipo = "NOVA_OCORRENCIA";
        }

        String tempoRelativo = (String) data.get("tempo_relativo");
        if (tempoRelativo == null) {
            Object tsObj = data.get("timestamp");
            if (tsObj instanceof Timestamp) {
                Date date = ((Timestamp) tsObj).toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                tempoRelativo = "hoje às " + sdf.format(date);
            } else {
                tempoRelativo = "Agora";
            }
        }

        String icone = (String) data.get("icone");
        if (icone == null) {
            icone = "bell";
        }

        String tint = (String) data.get("tint");
        if (tint == null) {
            tint = "#12A05E";
        }

        String bg = (String) data.get("bg");
        if (bg == null) {
            bg = "#E2F7EC";
        }

        Boolean lida = (Boolean) data.get("lida");
        if (lida == null) {
            lida = false;
        }

        String occurrenceId = (String) data.get("occurrence_id");

        return new Notification(
                docId,
                tipo,
                titulo,
                descricao,
                tempoRelativo,
                icone,
                tint,
                bg,
                lida,
                occurrenceId
        );
    }
}
