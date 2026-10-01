package com.aula.volta.data.local;

import android.content.Context;

import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.NotificationAPI;
import com.aula.volta.data.model.Notification;
import com.aula.volta.data.model.NotificationJSON;

import java.util.ArrayList;
import java.util.List;

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

    public interface LoadCallback {
        void onResult(List<Notification> notifications);
    }

    private NotificationStore() {
    }

    /** Busca na API, mescla com locais (pushLocal) e devolve; se falhar, devolve o cache. */
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
        return PrefsHelper.getList(context.getApplicationContext(), PREFS, KEY_LIST,
                PrefsHelper.listType(Notification.class));
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

    /** API primeiro? Não — locais no topo, sem duplicar ids. */
    static List<Notification> mergedWithLocal(Context app, List<Notification> api) {
        List<Notification> merged = new ArrayList<>(localOnly(app));
        if (api != null) {
            for (Notification n : api) {
                if (!containsId(merged, n.getId())) {
                    merged.add(n);
                }
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
        Context app = context.getApplicationContext();
        List<Notification> list = cached(app);
        for (Notification n : list) {
            if (n.getId().equals(id)) {
                n.setLida(true);
            }
        }
        PrefsHelper.putList(app, PREFS, KEY_LIST, list);
        List<Notification> local = localOnly(app);
        for (Notification n : local) {
            if (n.getId().equals(id)) {
                n.setLida(true);
            }
        }
        PrefsHelper.putList(app, PREFS, KEY_LOCAL, local);
    }

    public static void markAllRead(Context context) {
        Context app = context.getApplicationContext();
        List<Notification> list = cached(app);
        for (Notification n : list) {
            n.setLida(true);
        }
        PrefsHelper.putList(app, PREFS, KEY_LIST, list);
        List<Notification> local = localOnly(app);
        for (Notification n : local) {
            n.setLida(true);
        }
        PrefsHelper.putList(app, PREFS, KEY_LOCAL, local);
    }
}
