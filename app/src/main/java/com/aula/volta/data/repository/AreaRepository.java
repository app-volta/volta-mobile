package com.aula.volta.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.CatalogAPI;
import com.aula.volta.data.model.AreaResponseDTO;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class AreaRepository {

    public interface AreasCallback {
        void onLoaded(@NonNull List<AreaResponseDTO> areas);
    }

    private static final String PREF_CACHE_AREAS = "cache_areas";
    private static final Gson gson = new Gson();

    private AreaRepository() {
    }

    public static List<AreaResponseDTO> getCachedAreas(Context context) {
        if (context == null) {
            return new ArrayList<>();
        }
        List<AreaResponseDTO> result = new ArrayList<>();
        try {
            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences(PREF_CACHE_AREAS, Context.MODE_PRIVATE);
            Map<String, ?> all = prefs.getAll();
            if (all != null && !all.isEmpty()) {
                for (Map.Entry<String, ?> entry : all.entrySet()) {
                    if (entry.getValue() instanceof String) {
                        try {
                            String jsonStr = (String) entry.getValue();
                            AreaResponseDTO dto = gson.fromJson(jsonStr, AreaResponseDTO.class);
                            if (dto != null && dto.getSectorName() != null && !dto.getSectorName().trim().isEmpty()) {
                                if (dto.getId() == null || dto.getId().trim().isEmpty()) {
                                    dto.setId(entry.getKey());
                                }
                                result.add(dto);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        Collections.sort(result, (a, b) -> {
            String nameA = a.getSectorName() != null ? a.getSectorName() : "";
            String nameB = b.getSectorName() != null ? b.getSectorName() : "";
            return nameA.compareToIgnoreCase(nameB);
        });
        return result;
    }

    public static List<String> getSectorNames(Context context) {
        List<AreaResponseDTO> areas = getCachedAreas(context);
        List<String> names = new ArrayList<>();
        for (AreaResponseDTO area : areas) {
            if (area.getSectorName() != null && !area.getSectorName().trim().isEmpty()) {
                String trimmed = area.getSectorName().trim();
                if (!names.contains(trimmed)) {
                    names.add(trimmed);
                }
            }
        }
        Collections.sort(names, String::compareToIgnoreCase);
        return names;
    }

    public static void saveAreas(Context context, List<AreaResponseDTO> areas) {
        if (context == null || areas == null || areas.isEmpty()) {
            return;
        }
        try {
            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences(PREF_CACHE_AREAS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            for (AreaResponseDTO area : areas) {
                if (area != null) {
                    String key = (area.getId() != null && !area.getId().trim().isEmpty())
                            ? area.getId().trim()
                            : (area.getSectorName() != null ? area.getSectorName().trim() : null);
                    if (key != null) {
                        editor.putString(key, gson.toJson(area));
                    }
                }
            }
            editor.apply();
        } catch (Exception ignored) {
        }
    }

    public static void preloadAreas(Context context) {
        loadAreas(context, null);
    }

    public static void loadAreas(Context context, AreasCallback callback) {
        if (context == null) {
            if (callback != null) {
                callback.onLoaded(new ArrayList<>());
            }
            return;
        }
        final Context appContext = context.getApplicationContext();
        try {
            CatalogAPI api = ApiClient.get(appContext).create(CatalogAPI.class);
            api.getMyAreas().enqueue(new Callback<List<AreaResponseDTO>>() {
                @Override
                public void onResponse(@NonNull Call<List<AreaResponseDTO>> call,
                                       @NonNull Response<List<AreaResponseDTO>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        saveAreas(appContext, response.body());
                        if (callback != null) {
                            callback.onLoaded(response.body());
                        }
                    } else {
                        fetchFallback(api, appContext, callback);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<AreaResponseDTO>> call, @NonNull Throwable t) {
                    fetchFallback(api, appContext, callback);
                }
            });
        } catch (Exception e) {
            if (callback != null) {
                callback.onLoaded(getCachedAreas(appContext));
            }
        }
    }

    private static void fetchFallback(CatalogAPI api, Context appContext, AreasCallback callback) {
        try {
            api.getAllAreas().enqueue(new Callback<List<AreaResponseDTO>>() {
                @Override
                public void onResponse(@NonNull Call<List<AreaResponseDTO>> call,
                                       @NonNull Response<List<AreaResponseDTO>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        saveAreas(appContext, response.body());
                        if (callback != null) {
                            callback.onLoaded(response.body());
                        }
                    } else if (callback != null) {
                        callback.onLoaded(getCachedAreas(appContext));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<AreaResponseDTO>> call, @NonNull Throwable t) {
                    if (callback != null) {
                        callback.onLoaded(getCachedAreas(appContext));
                    }
                }
            });
        } catch (Exception e) {
            if (callback != null) {
                callback.onLoaded(getCachedAreas(appContext));
            }
        }
    }
}
