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
 * Único ponto de leitura/escrita de notificações da UI.
 * Integrado ao Cloud Firestore e Firebase Cloud Messaging com deduplicação rigorosa.
 */
public final class NotificationStore {

    private static final String PREFS = "cache_notifications";
    private static final String KEY_LIST = "list";
    private static final String KEY_LOCAL = "local_only";
    private static final String KEY_READ_IDS = "read_ids";

    public interface LoadCallback {
        void onResult(List<Notification> notifications);
    }

    public interface ChangeListener {
        void onChange();
    }

    private static final List<ChangeListener> changeListeners = new ArrayList<>();

    public static void addListener(ChangeListener listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public static void removeListener(ChangeListener listener) {
        changeListeners.remove(listener);
    }

    private static void notifyChange() {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            for (ChangeListener l : new ArrayList<>(changeListeners)) {
                try {
                    l.onChange();
                } catch (Exception ignored) {
                }
            }
        });
    }

    private NotificationStore() {
    }

    /**
     * Determina se duas notificações representam o mesmo evento.
     * Considera id idêntico, mesma ocorrência + tipo, ou mesmo título + descrição.
     */
    public static boolean isSameNotification(Notification a, Notification b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getId() != null && b.getId() != null && a.getId().equals(b.getId())) {
            return true;
        }
        if (a.getOccurrenceId() != null && b.getOccurrenceId() != null
                && a.getOccurrenceId().equals(b.getOccurrenceId())) {
            if (a.getTipo() != null && b.getTipo() != null
                    && a.getTipo().equalsIgnoreCase(b.getTipo())) {
                return true;
            }
        }
        if (a.getTitulo() != null && b.getTitulo() != null && a.getTitulo().equals(b.getTitulo())
                && a.getDescricao() != null && b.getDescricao() != null && a.getDescricao().equals(b.getDescricao())) {
            return true;
        }
        return false;
    }

    /**
     * Localiza o índice de uma notificação equivalente na lista.
     */
    public static int findIndex(List<Notification> list, Notification target) {
        if (list == null || target == null) {
            return -1;
        }
        for (int i = 0; i < list.size(); i++) {
            if (isSameNotification(list.get(i), target)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Sanitiza e deduplica uma lista de notificações preservando a ordem original.
     */
    public static List<Notification> deduplicate(List<Notification> list) {
        if (list == null || list.size() <= 1) {
            return list != null ? list : new ArrayList<>();
        }
        List<Notification> result = new ArrayList<>();
        for (Notification n : list) {
            if (findIndex(result, n) < 0) {
                result.add(n);
            }
        }
        return result;
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
                    list = deduplicate(list);
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

    /** Lista em memória vinda do cache (sem rede), com sanitização automática de duplicatas antigas. */
    public static List<Notification> cached(Context context) {
        Context app = context.getApplicationContext();
        List<Notification> rawList = PrefsHelper.getList(app, PREFS, KEY_LIST,
                PrefsHelper.listType(Notification.class));
        List<Notification> list = deduplicate(rawList);
        if (list.size() != rawList.size()) {
            PrefsHelper.putList(app, PREFS, KEY_LIST, list);
        }
        Set<String> readSet = readIds(app);
        for (Notification n : list) {
            if (n.getId() != null && readSet.contains(n.getId())) {
                n.setLida(true);
            }
        }
        return list;
    }

    public static boolean hasNotification(Context context, String id) {
        if (id == null) {
            return false;
        }
        for (Notification n : cached(context)) {
            if (id.equals(n.getId())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasNotification(Context context, Notification candidate) {
        if (candidate == null) {
            return false;
        }
        return findIndex(cached(context), candidate) >= 0;
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

    /** Ponto de entrada do Firestore, FCM e notificações locais criadas no app. */
    public static void pushLocal(Context context, Notification notification) {
        if (notification == null) {
            return;
        }
        Context app = context.getApplicationContext();

        List<Notification> local = new ArrayList<>(deduplicate(localOnly(app)));
        int existingLocalIdx = findIndex(local, notification);
        if (existingLocalIdx >= 0) {
            // Já existe; atualiza o registro preservando ou aprimorando dados
            local.set(existingLocalIdx, notification);
        } else {
            local.add(0, notification);
        }
        PrefsHelper.putList(app, PREFS, KEY_LOCAL, local);

        List<Notification> list = new ArrayList<>(cached(app));
        int existingListIdx = findIndex(list, notification);
        if (existingListIdx >= 0) {
            list.set(existingListIdx, notification);
        } else {
            list.add(0, notification);
        }
        PrefsHelper.putList(app, PREFS, KEY_LIST, list);
        notifyChange();
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

    /** Mescla locais no topo com itens da API sem duplicar e preservando estado de leitura. */
    static List<Notification> mergedWithLocal(Context app, List<Notification> api) {
        Set<String> readSet = readIds(app);
        for (Notification c : cached(app)) {
            if (c.isLida() && c.getId() != null) {
                readSet.add(c.getId());
            }
        }

        List<Notification> merged = new ArrayList<>(deduplicate(localOnly(app)));
        if (api != null) {
            for (Notification n : api) {
                if (findIndex(merged, n) < 0) {
                    merged.add(n);
                }
            }
        }
        for (Notification n : merged) {
            if (n.getId() != null && readSet.contains(n.getId())) {
                n.setLida(true);
            }
        }
        return deduplicate(merged);
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
        notifyChange();
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
        notifyChange();
    }
}
