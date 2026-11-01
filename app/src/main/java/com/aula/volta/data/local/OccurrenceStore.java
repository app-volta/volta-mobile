package com.aula.volta.data.local;

import android.content.Context;
import android.util.Log;

import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.ChatbotAPI;
import com.aula.volta.data.api.ChatbotAuthClient;
import com.aula.volta.data.api.ChatbotClient;
import com.aula.volta.data.api.OccurrenceAPI;
import com.aula.volta.data.model.AiAnalysisJSON;
import com.aula.volta.data.model.Occurrence;
import com.aula.volta.data.model.OccurrenceJSON;
import com.aula.volta.data.model.incident.IncidentResponseDTO;
import com.aula.volta.data.notification.NotificationHelper;
import java.util.concurrent.CopyOnWriteArrayList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

    public interface StoreListener {
        void onStoreUpdated();
    }

    private static final List<StoreListener> listeners = new CopyOnWriteArrayList<>();

    public static void addListener(StoreListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeListener(StoreListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public static void notifyListeners() {
        for (StoreListener l : listeners) {
            try {
                l.onStoreUpdated();
            } catch (Exception ignored) {
            }
        }
    }

    public static void setupRealtimeSync(Context context) {

    }

    public static void updateStatus(Context context, String id, String newStatus) {
        if (context == null || id == null || newStatus == null) return;
        Context app = context.getApplicationContext();
        List<Occurrence> list = new ArrayList<>(cached(app));
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            Occurrence o = list.get(i);
            if (id.equals(o.getId())) {
                list.set(i, new Occurrence(
                        o.getId(),
                        o.getTitulo(),
                        o.getSetor(),
                        o.getTempoRelativo(),
                        o.getPrioridade(),
                        newStatus,
                        o.getMaterial(),
                        o.getPhotoUrl(),
                        o.getPhotoPath()
                ));
                found = true;
                break;
            }
        }
        if (found) {
            PrefsHelper.putList(app, getPrefsName(app), KEY_LIST, list);
        }
        List<Occurrence> local = new ArrayList<>(localOnly(app));
        for (int i = 0; i < local.size(); i++) {
            Occurrence o = local.get(i);
            if (id.equals(o.getId())) {
                local.set(i, new Occurrence(
                        o.getId(),
                        o.getTitulo(),
                        o.getSetor(),
                        o.getTempoRelativo(),
                        o.getPrioridade(),
                        newStatus,
                        o.getMaterial(),
                        o.getPhotoUrl(),
                        o.getPhotoPath()
                ));
                break;
            }
        }
        PrefsHelper.putList(app, getPrefsName(app), KEY_LOCAL, local);
        notifyListeners();
    }

    private OccurrenceStore() {
    }

    public static void refresh(Context context, LoadCallback callback) {
        Context app = context.getApplicationContext();
        OccurrenceAPI api = ChatbotAuthClient.get(app).create(OccurrenceAPI.class);

        api.getIncidents().enqueue(new Callback<List<IncidentResponseDTO>>() {
            @Override
            public void onResponse(Call<List<IncidentResponseDTO>> call,
                                   Response<List<IncidentResponseDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Occurrence> list = new ArrayList<>();
                    for (IncidentResponseDTO inc : response.body()) {

                        if (inc.getWasteTypeId() == null && (inc.getPhotoUrl() == null || inc.getPhotoUrl().isEmpty())
                                && "AGUARDANDO_VALIDACAO".equalsIgnoreCase(inc.getStatus())) {
                            continue;
                        }
                        Occurrence occ = Occurrence.fromIncident(inc, app);
                        if (occ != null) {
                            list.add(occ);
                            if (inc.getId() != null) {
                                try {
                                    String existing = PrefsHelper.getJson(app, "cache_occurrence_detail", inc.getId());
                                    JsonObject d = (existing != null && !existing.isEmpty())
                                            ? JsonParser.parseString(existing).getAsJsonObject()
                                            : new JsonObject();
                                    d.addProperty("id", inc.getId());
                                    if (inc.getPhotoUrl() != null && !inc.getPhotoUrl().isEmpty()) {
                                        d.addProperty("photoUrl", inc.getPhotoUrl());
                                        d.addProperty("imageUrl", inc.getPhotoUrl());
                                    }
                                    d.addProperty("material", occ.getMaterial());
                                    d.addProperty("setor", occ.getSetor());
                                    d.addProperty("titulo", occ.getTitulo());
                                    d.addProperty("status", occ.getStatus());
                                    d.addProperty("prioridade", occ.getPrioridade());
                                    if (inc.getContaminationLevel() != null && !inc.getContaminationLevel().isEmpty()) {
                                        d.addProperty("contaminacao", AiAnalysisJSON.mapContaminationLabel(inc.getContaminationLevel()));
                                    }
                                    if (inc.getAiDescription() != null && !inc.getAiDescription().trim().isEmpty()) {
                                        d.addProperty("relatorio_auto", inc.getAiDescription().trim());
                                    }
                                    if (inc.getRegisteredAt() != null) {
                                        d.addProperty("registeredAt", inc.getRegisteredAt());
                                    }
                                    double weight = (inc.getEstimatedQuantity() != null && inc.getEstimatedQuantity() > 0)
                                            ? inc.getEstimatedQuantity() : 25.0;
                                    d.addProperty("peso_estimado", weight);
                                    d.addProperty("quantidade", weight);
                                    PrefsHelper.putJson(app, "cache_occurrence_detail", inc.getId(), d.toString());
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                    list = mergedWithLocal(app, list);
                    PrefsHelper.putList(app, getPrefsName(app), KEY_LIST, list);
                    callback.onResult(list);
                    return;
                }
                callback.onResult(cached(app));
            }

            @Override
            public void onFailure(Call<List<IncidentResponseDTO>> call, Throwable t) {
                callback.onResult(cached(app));
            }
        });
    }

    public static List<Occurrence> cached(Context context) {
        Context app = context.getApplicationContext();
        String prefsName = getPrefsName(app);
        List<Occurrence> list = PrefsHelper.getList(app, prefsName, KEY_LIST,
                PrefsHelper.listType(Occurrence.class));
        if (list == null || list.isEmpty()) {
            List<Occurrence> local = localOnly(app);
            if (local != null && !local.isEmpty()) {
                list = new ArrayList<>(local);
                PrefsHelper.putList(app, prefsName, KEY_LIST, list);
                return list;
            }
            return new ArrayList<>();
        }
        return list != null ? list : new ArrayList<>();
    }

    public static void pushLocal(Context context, Occurrence occurrence) {
        Context app = context.getApplicationContext();
        List<Occurrence> local = new ArrayList<>(localOnly(app));
        local.add(0, occurrence);
        PrefsHelper.putList(app, getPrefsName(app), KEY_LOCAL, local);
        List<Occurrence> list = new ArrayList<>(cached(app));
        list.add(0, occurrence);
        PrefsHelper.putList(app, getPrefsName(app), KEY_LIST, list);
        notifyListeners();
    }

    public static void update(Context context, Occurrence updated) {
        if (updated == null || updated.getId() == null) {
            return;
        }
        Context app = context.getApplicationContext();

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

        List<Occurrence> list = new ArrayList<>(cached(app));
        for (int i = 0; i < list.size(); i++) {
            if (updated.getId().equals(list.get(i).getId())) {
                list.set(i, updated);
                break;
            }
        }
        PrefsHelper.putList(app, getPrefsName(app), KEY_LIST, list);
        notifyListeners();
    }

    public static List<Occurrence> localOnly(Context app) {
        return PrefsHelper.getList(app, getPrefsName(app), KEY_LOCAL,
                PrefsHelper.listType(Occurrence.class));
    }

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
                        String st = o.getStatus();
                        try {
                            String cStr = PrefsHelper.getJson(app, "cache_occurrence_detail", o.getId());
                            if (cStr != null && !cStr.isEmpty()) {
                                JsonObject cd = JsonParser.parseString(cStr).getAsJsonObject();
                                if (cd.has("status") && !cd.get("status").isJsonNull()) {
                                    String cStat = cd.get("status").getAsString();
                                    if ("APROVADA".equalsIgnoreCase(cStat) || "APPROVED".equalsIgnoreCase(cStat) || "APROVADO".equalsIgnoreCase(cStat)) {
                                        st = "APROVADA";
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                        result.add(new Occurrence(
                                o.getId(),
                                o.getTitulo(),
                                o.getSetor(),
                                o.getTempoRelativo(),
                                o.getPrioridade(),
                                st,
                                mat,
                                photo,
                                path
                        ));
                        continue;
                    }
                }
                String st = o.getStatus();
                try {
                    String cStr = PrefsHelper.getJson(app, "cache_occurrence_detail", o.getId());
                    if (cStr != null && !cStr.isEmpty()) {
                        JsonObject cd = JsonParser.parseString(cStr).getAsJsonObject();
                        if (cd.has("status") && !cd.get("status").isJsonNull()) {
                            String cStat = cd.get("status").getAsString();
                            if ("APROVADA".equalsIgnoreCase(cStat) || "APPROVED".equalsIgnoreCase(cStat) || "APROVADO".equalsIgnoreCase(cStat)) {
                                st = "APROVADA";
                            }
                        }
                    }
                } catch (Exception ignored) {}
                result.add(new Occurrence(
                        o.getId(),
                        o.getTitulo(),
                        o.getSetor(),
                        o.getTempoRelativo(),
                        o.getPrioridade(),
                        st,
                        o.getMaterial(),
                        o.getPhotoUrl(),
                        o.getPhotoPath()
                ));
            }
        }
        return result;
    }

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
