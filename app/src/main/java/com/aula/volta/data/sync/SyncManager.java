package com.aula.volta.data.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import androidx.annotation.NonNull;

import com.aula.volta.R;
import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.OccurrenceAPI;
import com.aula.volta.data.local.AuditLog;
import com.aula.volta.data.local.NotificationStore;
import com.aula.volta.data.model.Notification;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Gerencia a sincronização de dados offline quando a conectividade é restabelecida.
 */
public final class SyncManager {

    private static final String TAG = "VOLTA_SyncManager";
    private static boolean isRegistered = false;

    private SyncManager() {
    }

    /**
     * Verifica se o dispositivo possui conexão com a internet ativa.
     */
    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) {
            return false;
        }
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    /**
     * Registra o monitor de rede para sincronizar automaticamente ao reconectar.
     */
    public static synchronized void registerNetworkCallback(Context context) {
        if (isRegistered) {
            return;
        }
        Context app = context.getApplicationContext();
        ConnectivityManager cm = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return;
        }

        try {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();

            cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    Log.d(TAG, "Conexão de rede ativa. Iniciando sincronização da fila pendente...");
                    syncPending(app);
                }
            });
            isRegistered = true;
        } catch (Exception e) {
            Log.e(TAG, "Falha ao registrar NetworkCallback", e);
        }
    }

    /**
     * Processa a fila de ocorrências criadas offline e as envia à API.
     */
    public static synchronized void syncPending(Context context) {
        Context app = context.getApplicationContext();
        List<PendingOccurrence> pendingList = SyncQueue.getAll(app);
        if (pendingList.isEmpty()) {
            return;
        }

        OccurrenceAPI api = ApiClient.get(app).create(OccurrenceAPI.class);

        for (PendingOccurrence item : pendingList) {
            JsonObject body = new JsonObject();
            body.addProperty("foto_path", item.getFotoPath());
            body.addProperty("descricao", item.getDescricao() != null ? item.getDescricao() : "");
            body.addProperty("setor", item.getSetor() != null ? item.getSetor() : "");
            body.addProperty("material", item.getMaterial() != null ? item.getMaterial() : "");
            body.addProperty("quantidade_estimada", item.getQuantidadeEstimada());

            api.createOccurrence(body).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String serverId = response.body().has("id")
                                ? response.body().get("id").getAsString() : item.getLocalId();

                        // Remove da fila pendente
                        SyncQueue.remove(app, item.getLocalId());

                        // Registra auditoria da sincronização
                        AuditLog.append(app, "Sistema Offline", "SYNC_ONLINE",
                                "ocorrencia", serverId, item.getLocalId(), "SINCRONIZADO");

                        // Emite notificação local de sucesso na sincronização
                        Notification notification = new Notification(
                                "sync_" + System.currentTimeMillis(),
                                "SYNC_CONCLUIDO",
                                app.getString(R.string.sync_success_title, serverId),
                                app.getString(R.string.sync_success_desc, item.getMaterial(), item.getSetor()),
                                app.getString(R.string.notif_now),
                                "check",
                                "#0E7C48",
                                "#E2F7EC",
                                false,
                                serverId);
                        NotificationStore.pushLocal(app, notification);
                        Log.d(TAG, "Ocorrência offline " + item.getLocalId() + " sincronizada com sucesso: " + serverId);
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.w(TAG, "Falha temporária ao sincronizar ocorrência " + item.getLocalId() + ". Tentará novamente.", t);
                }
            });
        }
    }
}
