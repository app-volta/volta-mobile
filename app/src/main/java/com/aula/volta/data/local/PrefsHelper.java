package com.aula.volta.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Cache local genérico (Fase 0). Mesmo padrão do CarrinhoDeCompras:
 * salva lista como JSON; lê de volta no offline/falha.
 */
public final class PrefsHelper {

    private static final Gson GSON = new Gson();

    private PrefsHelper() {
    }

    public static void putJson(Context context, String prefsName, String key, String json) {
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        prefs.edit().putString(key, json).apply();
    }

    public static String getJson(Context context, String prefsName, String key) {
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        return prefs.getString(key, null);
    }

    public static <T> void putList(Context context, String prefsName, String key, List<T> list) {
        putJson(context, prefsName, key, GSON.toJson(list));
    }

    public static <T> List<T> getList(Context context, String prefsName, String key, Class<T[]> arrayClass) {
        String json = getJson(context, prefsName, key);
        if (json == null) {
            return new ArrayList<>();
        }
        T[] array = GSON.fromJson(json, arrayClass);
        List<T> result = new ArrayList<>();
        if (array != null) {
            for (T item : array) {
                result.add(item);
            }
        }
        return result;
    }

    /** Leitura tipada via TypeToken (para listas de models sem array simples). */
    public static <T> List<T> getList(Context context, String prefsName, String key, Type type) {
        String json = getJson(context, prefsName, key);
        if (json == null) {
            return new ArrayList<>();
        }
        List<T> result = GSON.fromJson(json, type);
        return result != null ? result : new ArrayList<>();
    }

    public static Type listType(Class<?> elementClass) {
        return TypeToken.getParameterized(List.class, elementClass).getType();
    }
}
