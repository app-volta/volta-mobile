package com.aula.volta.data.local;

import android.content.Context;

import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.OccurrenceAPI;
import com.aula.volta.data.model.Occurrence;
import com.aula.volta.data.model.OccurrenceJSON;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Único ponto de leitura/escrita de ocorrências da UI (lista Home + Ver todas).
 *
 * <p>Ocorrências lançadas no app entram via {@link #pushLocal} e sobrevivem ao
 * refresh (mesmo padrão do NotificationStore) — o mock nunca as conheceria.</p>
 */
public final class OccurrenceStore {

    private static final String PREFS = "cache_occurrences";
    private static final String KEY_LIST = "list";
    private static final String KEY_LOCAL = "local_only";

    public interface LoadCallback {
        void onResult(List<Occurrence> occurrences);
    }

    private OccurrenceStore() {
    }

    /** Busca na API, mescla com locais (pushLocal) e devolve; se falhar, devolve o cache. */
    public static void refresh(Context context, LoadCallback callback) {
        Context app = context.getApplicationContext();
        OccurrenceAPI api = ApiClient.get(app).create(OccurrenceAPI.class);
        api.getOccurrences().enqueue(new Callback<List<OccurrenceJSON>>() {
            @Override
            public void onResponse(Call<List<OccurrenceJSON>> call,
                                   Response<List<OccurrenceJSON>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Occurrence> list = new ArrayList<>();
                    for (OccurrenceJSON json : response.body()) {
                        list.add(Occurrence.fromJson(json));
                    }
                    list = mergedWithLocal(app, list);
                    PrefsHelper.putList(app, PREFS, KEY_LIST, list);
                    callback.onResult(list);
                } else {
                    callback.onResult(cached(app));
                }
            }

            @Override
            public void onFailure(Call<List<OccurrenceJSON>> call, Throwable t) {
                callback.onResult(cached(app));
            }
        });
    }

    /** Lista em memória vinda do cache (sem rede). */
    public static List<Occurrence> cached(Context context) {
        return PrefsHelper.getList(context.getApplicationContext(), PREFS, KEY_LIST,
                PrefsHelper.listType(Occurrence.class));
    }

    /** Ocorrência lançada no app: topo da lista e sobrevive ao refresh. */
    public static void pushLocal(Context context, Occurrence occurrence) {
        Context app = context.getApplicationContext();
        List<Occurrence> local = new ArrayList<>(localOnly(app));
        local.add(0, occurrence);
        PrefsHelper.putList(app, PREFS, KEY_LOCAL, local);
        List<Occurrence> list = new ArrayList<>(cached(app));
        list.add(0, occurrence);
        PrefsHelper.putList(app, PREFS, KEY_LIST, list);
    }

    /** Locais ainda não vindos da API (sobrevivem ao refresh). */
    static List<Occurrence> localOnly(Context app) {
        return PrefsHelper.getList(app, PREFS, KEY_LOCAL,
                PrefsHelper.listType(Occurrence.class));
    }

    /** Locais no topo, sem duplicar ids. */
    static List<Occurrence> mergedWithLocal(Context app, List<Occurrence> api) {
        List<Occurrence> merged = new ArrayList<>(localOnly(app));
        if (api != null) {
            for (Occurrence o : api) {
                if (!containsId(merged, o.getId())) {
                    merged.add(o);
                }
            }
        }
        return merged;
    }

    private static boolean containsId(List<Occurrence> list, String id) {
        if (id == null) {
            return false;
        }
        for (Occurrence o : list) {
            if (id.equals(o.getId())) {
                return true;
            }
        }
        return false;
    }
}
