package com.aula.volta.data.local;

import android.content.Context;

import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.NotificationAPI;
import com.aula.volta.data.model.Notification;
import com.aula.volta.data.model.NotificationJSON;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Único ponto de leitura/escrita de notificações da UI (item 5 do plano).
 *
 * <p>Mock local agora; FCM futuro pluga chamando {@link #pushLocal} a partir do
 * {@code FirebaseMessagingService} — zero mudança nas telas.</p>
 */
public final class NotificationStore {

    private static final String PREFS = "cache_notifications";
    private static final String KEY_LIST = "list";
    private static final String KEY_LOCAL = "local_only";
    private static final String KEY_READ_IDS = "read_ids";

    public interface LoadCallback {
        void onResult(List<Notification> notifications);
    }

    private NotificationStore() {
    }

    /** Busca na API, mescla com locais (pushLocal) e preserva estado de lidas; se falhar, devolve o cache. */
    public static void refresh(Context context, LoadCallback callback) {
        Context app = context.getApplicationContext();
        NotificationAPI api = ApiClient.get(app).create(NotificationAPI.class);
        api.getNotifications().enqueue(new Callback<List<NotificationJSON>>() {
            @Override
            public void onResponse(Call<List<NotificationJSON>> call,
                                   Response<List<NotificationJSON>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Notification> list = new ArrayList<>();
                    for (NotificationJSON json : response.body()) {
                        list.add(Notification.fromJson(json));
                    }
                    list = mergedWithLocal(app, list);
                    PrefsHelper.putList(app, PREFS, KEY_LIST, list);
                    callback.onResult(list);
                } else {
                    callback.onResult(cached(app));
                }
            }

            @Override
            public void onFailure(Call<List<NotificationJSON>> call, Throwable t) {
                callback.onResult(cached(app));
            }
        });
    }

    /** Lista em memória vinda do cache (sem rede). */
    public static List<Notification> cached(Context context) {
        Context app = context.getApplicationContext();
        List<Notification> list = PrefsHelper.getList(app, PREFS, KEY_LIST,
                PrefsHelper.listType(Notification.class));
        Set<String> readSet = readIds(app);
        for (Notification n : list) {
            if (n.getId() != null && readSet.contains(n.getId())) {
                n.setLida(true);
            }
        }
        return list;
    }

    public static int unreadCount(List<Notification> notifications) {
        int count = 0;
        if (notifications != null) {
            for (Notification n : notifications) {
                if (!n.isLida()) {
                    count++;
                }
            }
        }
        return count;
    }

    /** Ponto de entrada do FCM futuro (e do mock local agora). */
    public static void pushLocal(Context context, Notification notification) {
        Context app = context.getApplicationContext();
        List<Notification> local = new ArrayList<>(localOnly(app));
        local.add(0, notification);
        PrefsHelper.putList(app, PREFS, KEY_LOCAL, local);
        List<Notification> list = new ArrayList<>(cached(app));
        list.add(0, notification);
        PrefsHelper.putList(app, PREFS, KEY_LIST, list);
    }

    /** Locais ainda não vindos da API (sobrevivem ao refresh). */
    static List<Notification> localOnly(Context app) {
        return PrefsHelper.getList(app, PREFS, KEY_LOCAL,
                PrefsHelper.listType(Notification.class));
    }

    /** Conjunto de IDs já marcados como lidos pelo usuário (sobrevive ao refresh da API/mock). */
    private static Set<String> readIds(Context app) {
        List<String> stored = PrefsHelper.getList(app, PREFS, KEY_READ_IDS,
                PrefsHelper.listType(String.class));
        return new HashSet<>(stored);
    }

    private static void saveReadIds(Context app, Set<String> ids) {
        PrefsHelper.putList(app, PREFS, KEY_READ_IDS, new ArrayList<>(ids));
    }

    /** Mescla locais no topo com itens da API sem duplicar IDs e preservando estado de leitura. */
    static List<Notification> mergedWithLocal(Context app, List<Notification> api) {
        Set<String> readSet = readIds(app);
        for (Notification c : cached(app)) {
            if (c.isLida() && c.getId() != null) {
                readSet.add(c.getId());
            }
        }

        List<Notification> merged = new ArrayList<>(localOnly(app));
        if (api != null) {
            for (Notification n : api) {
                if (!containsId(merged, n.getId())) {
                    merged.add(n);
                }
            }
        }
        for (Notification n : merged) {
            if (n.getId() != null && readSet.contains(n.getId())) {
                n.setLida(true);
            }
        }
        return merged;
    }

    private static boolean containsId(List<Notification> list, String id) {
        if (id == null) {
            return false;
        }
        for (Notification n : list) {
            if (id.equals(n.getId())) {
                return true;
            }
        }
        return false;
    }

    public static void markRead(Context context, String id) {
        if (id == null) {
            return;
        }
        Context app = context.getApplicationContext();
        Set<String> readSet = readIds(app);
        readSet.add(id);
        saveReadIds(app, readSet);

        List<Notification> list = cached(app);
        for (Notification n : list) {
            if (id.equals(n.getId())) {
                n.setLida(true);
            }
        }
        PrefsHelper.putList(app, PREFS, KEY_LIST, list);

        List<Notification> local = localOnly(app);
        for (Notification n : local) {
            if (id.equals(n.getId())) {
                n.setLida(true);
            }
        }
        PrefsHelper.putList(app, PREFS, KEY_LOCAL, local);
    }

    public static void markAllRead(Context context) {
        Context app = context.getApplicationContext();
        Set<String> readSet = readIds(app);

        List<Notification> list = cached(app);
        for (Notification n : list) {
            n.setLida(true);
            if (n.getId() != null) {
                readSet.add(n.getId());
            }
        }
        PrefsHelper.putList(app, PREFS, KEY_LIST, list);

        List<Notification> local = localOnly(app);
        for (Notification n : local) {
            n.setLida(true);
            if (n.getId() != null) {
                readSet.add(n.getId());
            }
        }
        PrefsHelper.putList(app, PREFS, KEY_LOCAL, local);

        saveReadIds(app, readSet);
    }
}
