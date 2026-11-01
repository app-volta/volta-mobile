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
import com.aula.volta.data.api.ChatbotAuthClient;
import com.aula.volta.data.api.OccurrenceAPI;
import com.aula.volta.data.local.AuditLog;
import com.aula.volta.data.local.NotificationStore;
import com.aula.volta.data.model.Notification;
import com.aula.volta.data.model.incident.IncidentRequestDTO;
import com.aula.volta.data.model.incident.IncidentResponseDTO;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class SyncManager {

    private static final String TAG = "VOLTA_SyncManager";
    private static boolean isRegistered = false;

    private SyncManager() {
    }

    public static boolean isOnline(Context context) {
        if (context == null) {
            return false;
        }
        try {
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
                    (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                            || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                            || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
        } catch (Exception e) {
            Log.w(TAG, "Falha ao verificar conectividade: " + e.getMessage());
            return true;
        }
    }

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

    private static String getCachedAreaId(Context app) {
        try {
            android.content.SharedPreferences prefs = app.getSharedPreferences("cache_areas", Context.MODE_PRIVATE);
            java.util.Map<String, ?> all = prefs.getAll();
            if (all != null && !all.isEmpty()) {
                for (String key : all.keySet()) {
                    if (key != null && key.length() > 10) {
                        return key;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "3059a21a-a46d-4e88-a1be-bf69c240ae0c";
    }

    private static String mapMaterialToWasteTypeId(String material) {
        if (material == null) return "bd985415-4b25-4c07-beca-cc086dce9b13";
        String lower = material.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("papel")) return "bd985415-4b25-4c07-beca-cc086dce9b13";
        if (lower.contains("plast") || lower.contains("plást")) return "8b3f6a14-9a27-462f-b414-24329d927af7";
        if (lower.contains("metal") || lower.contains("lata") || lower.contains("ferro") || lower.contains("aço") || lower.contains("alum")) return "5ee946da-2975-45e7-80d7-a5a8e49b4b2d";
        if (lower.contains("vidro")) return "2605dca7-b392-4835-a2a7-99a1aa68a7ab";
        if (lower.contains("organ") || lower.contains("orgân")) return "5cb7e3c8-6dac-45e4-870f-92cc64a2a452";
        if (lower.contains("eletron") || lower.contains("eletrôn")) return "7807fd3c-7c11-42c3-95be-48884968d3c1";
        if (lower.contains("quimic") || lower.contains("químic") || lower.contains("oleo") || lower.contains("óleo")) return "db21c099-511a-4dbc-82dc-2090b4b6d0a5";
        if (lower.contains("textil") || lower.contains("têxtil") || lower.contains("tecido")) return "9221e10b-b15b-40d8-a798-49f372ab9efb";
        if (lower.contains("madeira") || lower.contains("pallet") || lower.contains("palete") || lower.contains("tabua") || lower.contains("tábua") || lower.contains("mobil")) return "e9e077a8-7464-422d-a3be-44bc770fd5d2";
        return "dc9b7d17-cd48-475a-a313-d5de98505854";
    }

    public static synchronized void syncPending(Context context) {
        Context app = context.getApplicationContext();
        List<PendingOccurrence> pendingList = SyncQueue.getAll(app);
        OccurrenceAPI api = ChatbotAuthClient.get(app).create(OccurrenceAPI.class);
        String defaultAreaId = getCachedAreaId(app);

        for (PendingOccurrence item : pendingList) {
            String areaId = defaultAreaId;
            String wasteTypeId = mapMaterialToWasteTypeId(item.getMaterial());
            String photoUrl = null;
            if (item.getFotoPath() != null && !item.getFotoPath().isEmpty()) {
                photoUrl = com.aula.volta.data.upload.CloudinaryUploader.getCachedUrl(item.getFotoPath());
                if (photoUrl != null && !photoUrl.startsWith("http://") && !photoUrl.startsWith("https://")) {
                    photoUrl = null;
                }
            }

            IncidentRequestDTO req = new IncidentRequestDTO(
                    areaId,
                    wasteTypeId,
                    photoUrl,
                    item.getDescricao() != null && !item.getDescricao().isEmpty() ? item.getDescricao() : item.getMaterial(),
                    "LOW",
                    item.getQuantidadeEstimada() > 0 ? item.getQuantidadeEstimada() : 10.0,
                    "LOW"
            );

            api.createIncident(req).enqueue(new Callback<IncidentResponseDTO>() {
                @Override
                public void onResponse(Call<IncidentResponseDTO> call, Response<IncidentResponseDTO> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getId() != null) {
                        String serverId = response.body().getId();

                        SyncQueue.remove(app, item.getLocalId());
                        com.aula.volta.data.local.OccurrenceStore.remove(app, item.getLocalId());

                        AuditLog.append(app, "Sistema Offline", "SYNC_ONLINE",
                                "ocorrencia", serverId, item.getLocalId(), "SINCRONIZADO");

                        com.aula.volta.data.notification.NotificationHelper.notifySyncComplete(
                                app, serverId, item.getMaterial(), item.getSetor());
                        Log.d(TAG, "Ocorrência offline " + item.getLocalId() + " sincronizada no banco oficial com sucesso: " + serverId);
                    }
                }

                @Override
                public void onFailure(Call<IncidentResponseDTO> call, Throwable t) {
                    Log.w(TAG, "Falha temporária ao sincronizar ocorrência " + item.getLocalId() + ". Tentará novamente.", t);
                }
            });
        }

        List<com.aula.volta.data.model.Occurrence> locals = com.aula.volta.data.local.OccurrenceStore.localOnly(app);
        if (locals != null) {
            for (com.aula.volta.data.model.Occurrence occ : locals) {
                if (occ != null && occ.getId() != null && occ.getId().startsWith("off_")) {
                    String photoUrl = occ.getPhotoUrl();
                    if ((photoUrl == null || photoUrl.isEmpty()) && occ.getPhotoPath() != null) {
                        photoUrl = com.aula.volta.data.upload.CloudinaryUploader.getCachedUrl(occ.getPhotoPath());
                    }
                    if (photoUrl != null && !photoUrl.startsWith("http://") && !photoUrl.startsWith("https://")) {
                        photoUrl = null;
                    }
                    IncidentRequestDTO req = new IncidentRequestDTO(
                            defaultAreaId,
                            mapMaterialToWasteTypeId(occ.getMaterial()),
                            photoUrl,
                            occ.getTitulo() != null ? occ.getTitulo() : occ.getMaterial(),
                            "LOW",
                            15.0,
                            "LOW"
                    );
                    final String offId = occ.getId();
                    api.createIncident(req).enqueue(new Callback<IncidentResponseDTO>() {
                        @Override
                        public void onResponse(Call<IncidentResponseDTO> call, Response<IncidentResponseDTO> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().getId() != null) {
                                String serverId = response.body().getId();
                                com.aula.volta.data.local.OccurrenceStore.remove(app, offId);
                                Log.i(TAG, "Ocorrência pendente " + offId + " enviada com sucesso ao banco PostgreSQL: " + serverId);
                            }
                        }

                        @Override
                        public void onFailure(Call<IncidentResponseDTO> call, Throwable t) {
                            Log.w(TAG, "Falha ao enviar ocorrência pendente " + offId + ": " + t.getMessage());
                        }
                    });
                }
            }
        }
    }
}
