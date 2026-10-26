package com.aula.volta.data.local;

import android.content.Context;
import android.util.Log;

import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.ChatbotAPI;
import com.aula.volta.data.api.ChatbotClient;
import com.aula.volta.data.api.OccurrenceAPI;
import com.aula.volta.data.model.AiAnalysisJSON;
import com.aula.volta.data.model.Occurrence;
import com.aula.volta.data.model.OccurrenceJSON;
import com.aula.volta.data.model.incident.IncidentResponseDTO;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Único ponto de leitura/escrita de ocorrências da UI (lista Home + Ver todas).
 * Conecta-se à API Oficial do Banco de Dados (/incidents) com suporte a fallback offline.
 *
 * <p>Ocorrências lançadas no app entram via {@link #pushLocal} e sobrevivem ao
 * refresh (mesmo padrão do NotificationStore).</p>
 */
public final class OccurrenceStore {

    private static final String TAG = "OccurrenceStore";
    private static final String PREFS = "cache_occurrences";

    private static String getPrefsName(Context context) {
        String email = SessionManager.email(context);
        if (email == null || email.trim().isEmpty()) {
            return PREFS;
        }
        return PREFS + "_" + Math.abs(email.trim().toLowerCase().hashCode());
    }

    private static final String KEY_LIST = "list";
    private static final String KEY_LOCAL = "local_only";

    public interface LoadCallback {
        void onResult(List<Occurrence> occurrences);
    }

    private OccurrenceStore() {
    }

    /** Busca na API do Banco (/incidents), mescla com locais (pushLocal) e devolve; se falhar, devolve o cache. */
    public static void refresh(Context context, LoadCallback callback) {
        Context app = context.getApplicationContext();
        OccurrenceAPI api = ApiClient.get(app).create(OccurrenceAPI.class);

        // 1. Tenta carregar do backend oficial do banco (/incidents)
        api.getIncidents().enqueue(new Callback<List<IncidentResponseDTO>>() {
            @Override
            public void onResponse(Call<List<IncidentResponseDTO>> call,
                                   Response<List<IncidentResponseDTO>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<Occurrence> list = new ArrayList<>();
                    for (IncidentResponseDTO inc : response.body()) {
                        Occurrence occ = Occurrence.fromIncident(inc, app);
                        if (occ != null) {
                            list.add(occ);
                            if (inc.getId() != null && inc.getPhotoUrl() != null && !inc.getPhotoUrl().isEmpty()) {
                                try {
                                    String existing = PrefsHelper.getJson(app, "cache_occurrence_detail", inc.getId());
                                    JsonObject d = (existing != null && !existing.isEmpty())
                                            ? JsonParser.parseString(existing).getAsJsonObject()
                                            : new JsonObject();
                                    d.addProperty("id", inc.getId());
                                    d.addProperty("photoUrl", inc.getPhotoUrl());
                                    d.addProperty("imageUrl", inc.getPhotoUrl());
                                    d.addProperty("material", occ.getMaterial());
                                    d.addProperty("setor", occ.getSetor());
                                    d.addProperty("titulo", occ.getTitulo());
                                    PrefsHelper.putJson(app, "cache_occurrence_detail", inc.getId(), d.toString());
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                    list = mergedWithLocal(app, list);
                    Log.d(TAG, "Carregadas " + list.size() + " ocorrências do banco de dados oficial.");
                    syncDraftsStatusAndComplete(app, list, callback);
                    return;
                }

                // 2. Se a rota /incidents não retornou itens ou retornou erro, tenta /occurrences legado
                fallbackToOccurrencesLegacy(app, api, callback);
            }

            @Override
            public void onFailure(Call<List<IncidentResponseDTO>> call, Throwable t) {
                Log.w(TAG, "Falha ao consultar /incidents, ativando rota de ocorrências legada/cache: " + t.getMessage());
                fallbackToOccurrencesLegacy(app, api, callback);
            }
        });
    }

    private static void fallbackToOccurrencesLegacy(Context app, OccurrenceAPI api, LoadCallback callback) {
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
                    syncDraftsStatusAndComplete(app, list, callback);
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

    /** Lista em memória vinda do cache (sem rede); se vazia, carrega o catálogo inicial do app. */
    public static List<Occurrence> cached(Context context) {
        Context app = context.getApplicationContext();
        String prefsName = getPrefsName(app);
        List<Occurrence> list = PrefsHelper.getList(app, prefsName, KEY_LIST,
                PrefsHelper.listType(Occurrence.class));
        if (list == null || list.isEmpty()) {
            String email = SessionManager.email(app);
            if ("breno@volta.com".equalsIgnoreCase(email) || "demo@volta.com".equalsIgnoreCase(email)) {
                list = loadInitialOccurrencesFromAssets(app);
                if (!list.isEmpty()) {
                    PrefsHelper.putList(app, prefsName, KEY_LIST, list);
                }
            } else {
                return new ArrayList<>();
            }
        }
        return list != null ? list : new ArrayList<>();
    }

    private static List<Occurrence> loadInitialOccurrencesFromAssets(Context context) {
        try (java.io.InputStream in = context.getAssets().open("mock/occurrences.json")) {
            int size = in.available();
            byte[] buffer = new byte[size];
            in.read(buffer);
            String json = new String(buffer, java.nio.charset.StandardCharsets.UTF_8);
            com.google.gson.reflect.TypeToken<List<OccurrenceJSON>> token =
                    new com.google.gson.reflect.TypeToken<List<OccurrenceJSON>>() {};
            List<OccurrenceJSON> jsonList = new com.google.gson.Gson().fromJson(json, token.getType());
            List<Occurrence> result = new ArrayList<>();
            if (jsonList != null) {
                for (OccurrenceJSON j : jsonList) {
                    result.add(Occurrence.fromJson(j));
                }
            }
            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** Ocorrência lançada no app: topo da lista e sobrevive ao refresh. */
    public static void pushLocal(Context context, Occurrence occurrence) {
        Context app = context.getApplicationContext();
        List<Occurrence> local = new ArrayList<>(localOnly(app));
        local.add(0, occurrence);
        PrefsHelper.putList(app, getPrefsName(app), KEY_LOCAL, local);
        List<Occurrence> list = new ArrayList<>(cached(app));
        list.add(0, occurrence);
        PrefsHelper.putList(app, getPrefsName(app), KEY_LIST, list);
    }

    /**
     * Atualiza uma ocorrência existente no cache mantendo a ordem e refletindo mutações operacionais.
     */
    public static void update(Context context, Occurrence updated) {
        if (updated == null || updated.getId() == null) {
            return;
        }
        Context app = context.getApplicationContext();

        // 1. Atualiza na lista local (se existir lá)
        List<Occurrence> local = new ArrayList<>(localOnly(app));
        boolean foundLocal = false;
        for (int i = 0; i < local.size(); i++) {
            if (updated.getId().equals(local.get(i).getId())) {
                local.set(i, updated);
                foundLocal = true;
                break;
            }
        }
        if (!foundLocal) {
            local.add(0, updated);
        }
        PrefsHelper.putList(app, getPrefsName(app), KEY_LOCAL, local);

        // 2. Atualiza na lista geral do cache
        List<Occurrence> list = new ArrayList<>(cached(app));
        for (int i = 0; i < list.size(); i++) {
            if (updated.getId().equals(list.get(i).getId())) {
                list.set(i, updated);
                break;
            }
        }
        PrefsHelper.putList(app, getPrefsName(app), KEY_LIST, list);
    }

    /** Locais ainda não vindos da API (sobrevivem ao refresh). */
    static List<Occurrence> localOnly(Context app) {
        return PrefsHelper.getList(app, getPrefsName(app), KEY_LOCAL,
                PrefsHelper.listType(Occurrence.class));
    }

    /**
     * Mescla ocorrências locais com a API oficial do banco de dados.
     * Itens retornados pelo banco (/incidents) são a fonte primária de verdade e contêm a URL da foto.
     * Itens locais ainda pendentes de sincronização permanecem no topo da lista.
     */
    static List<Occurrence> mergedWithLocal(Context app, List<Occurrence> api) {
        List<Occurrence> local = localOnly(app);
        List<Occurrence> result = new ArrayList<>();

        java.util.Map<String, Occurrence> localMap = new java.util.HashMap<>();
        if (local != null) {
            for (Occurrence loc : local) {
                if (loc != null && loc.getId() != null) {
                    localMap.put(loc.getId(), loc);
                }
            }
        }

        // Mantém no topo apenas itens locais que ainda não estão presentes na resposta do banco
        List<Occurrence> remainingLocal = new ArrayList<>();
        if (local != null) {
            for (Occurrence loc : local) {
                if (loc != null && loc.getId() != null && !containsId(api, loc.getId())) {
                    remainingLocal.add(loc);
                    result.add(loc);
                }
            }
        }
        PrefsHelper.putList(app, getPrefsName(app), KEY_LOCAL, remainingLocal);

        // Adiciona itens do banco, enriquecendo fotos e dados do cache local caso a API retorne null
        if (api != null) {
            for (Occurrence o : api) {
                if (o != null && o.getId() != null && localMap.containsKey(o.getId())) {
                    Occurrence loc = localMap.get(o.getId());
                    if (loc != null) {
                        String photo = (o.getPhotoUrl() != null && !o.getPhotoUrl().isEmpty())
                                ? o.getPhotoUrl() : loc.getPhotoUrl();
                        String path = (o.getPhotoPath() != null && !o.getPhotoPath().isEmpty())
                                ? o.getPhotoPath() : loc.getPhotoPath();
                        String mat = (o.getMaterial() != null && !o.getMaterial().isEmpty() && !o.getMaterial().equals("Resíduo Reciclável"))
                                ? o.getMaterial() : loc.getMaterial();
                        result.add(new Occurrence(
                                o.getId(),
                                o.getTitulo(),
                                o.getSetor(),
                                o.getTempoRelativo(),
                                o.getPrioridade(),
                                o.getStatus(),
                                mat,
                                photo,
                                path
                        ));
                        continue;
                    }
                }
                result.add(o);
            }
        }
        return result;
    }

    /**
     * Consulta a API de Rascunhos (/v1/occurrences/drafts) para sincronizar status de aprovação web.
     * Se uma ocorrência foi aprovada na web (status == 'APROVADO'), atualiza o status na lista e salva em cache.
     */
    private static void syncDraftsStatusAndComplete(Context app, List<Occurrence> currentList, LoadCallback callback) {
        try {
            ChatbotAPI chatApi = ChatbotClient.get(app).create(ChatbotAPI.class);
            chatApi.listOccurrenceDrafts().enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().has("data")) {
                        JsonArray dataArr = response.body().getAsJsonArray("data");
                        java.util.Map<String, JsonObject> draftsMap = new java.util.HashMap<>();
                        for (int i = 0; i < dataArr.size(); i++) {
                            JsonObject d = dataArr.get(i).getAsJsonObject();
                            if (d.has("id")) {
                                draftsMap.put(d.get("id").getAsString(), d);
                            }
                        }

                        List<Occurrence> updatedList = new ArrayList<>();
                        for (Occurrence occ : currentList) {
                            if (occ != null && occ.getId() != null && draftsMap.containsKey(occ.getId())) {
                                JsonObject draft = draftsMap.get(occ.getId());
                                String draftStatus = draft.has("status") ? draft.get("status").getAsString() : "";
                                String finalStatus = ("APROVADO".equalsIgnoreCase(draftStatus) || "APPROVED".equalsIgnoreCase(draftStatus))
                                        ? "APROVADA" : occ.getStatus();

                                String photoUrl = occ.getPhotoUrl();
                                String photoPath = occ.getPhotoPath();
                                String cachedStr = PrefsHelper.getJson(app, "cache_occurrence_detail", occ.getId());
                                if (cachedStr != null && !cachedStr.isEmpty()) {
                                    try {
                                        JsonObject cd = JsonParser.parseString(cachedStr).getAsJsonObject();
                                        cd.addProperty("status", finalStatus);
                                        PrefsHelper.putJson(app, "cache_occurrence_detail", occ.getId(), cd.toString());
                                    } catch (Exception ignored) {}
                                }

                                updatedList.add(new Occurrence(
                                        occ.getId(),
                                        occ.getTitulo(),
                                        occ.getSetor(),
                                        occ.getTempoRelativo(),
                                        occ.getPrioridade(),
                                        finalStatus,
                                        occ.getMaterial(),
                                        photoUrl,
                                        photoPath
                                ));
                            } else {
                                updatedList.add(occ);
                            }
                        }
                        PrefsHelper.putList(app, getPrefsName(app), KEY_LIST, updatedList);
                        callback.onResult(updatedList);
                        return;
                    }
                    PrefsHelper.putList(app, getPrefsName(app), KEY_LIST, currentList);
                    callback.onResult(currentList);
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    PrefsHelper.putList(app, getPrefsName(app), KEY_LIST, currentList);
                    callback.onResult(currentList);
                }
            });
        } catch (Exception e) {
            PrefsHelper.putList(app, getPrefsName(app), KEY_LIST, currentList);
            callback.onResult(currentList);
        }
    }

    public static void remove(Context context, String id) {
        if (id == null) {
            return;
        }
        Context app = context.getApplicationContext();
        List<Occurrence> local = new ArrayList<>(localOnly(app));
        for (int i = local.size() - 1; i >= 0; i--) {
            if (id.equals(local.get(i).getId())) {
                local.remove(i);
            }
        }
        PrefsHelper.putList(app, getPrefsName(app), KEY_LOCAL, local);

        List<Occurrence> list = new ArrayList<>(cached(app));
        for (int i = list.size() - 1; i >= 0; i--) {
            if (id.equals(list.get(i).getId())) {
                list.remove(i);
            }
        }
        PrefsHelper.putList(app, getPrefsName(app), KEY_LIST, list);
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
