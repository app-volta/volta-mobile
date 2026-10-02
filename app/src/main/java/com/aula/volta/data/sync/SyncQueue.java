package com.aula.volta.data.sync;

import android.content.Context;

import com.aula.volta.data.local.PrefsHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Fila persistente de ocorrências criadas offline, aguardando envio para o servidor.
 */
public final class SyncQueue {

    private static final String PREFS_SYNC = "volta_sync_queue";
    private static final String KEY_PENDING = "pending_occurrences";

    private SyncQueue() {
    }

    /**
     * Adiciona uma ocorrência à fila de sincronização.
     */
    public static synchronized void enqueue(Context context, PendingOccurrence item) {
        Context app = context.getApplicationContext();
        List<PendingOccurrence> list = new ArrayList<>(getAll(app));
        list.add(item);
        PrefsHelper.putList(app, PREFS_SYNC, KEY_PENDING, list);
    }

    /**
     * Retorna todas as ocorrências pendentes de envio.
     */
    public static synchronized List<PendingOccurrence> getAll(Context context) {
        Context app = context.getApplicationContext();
        return PrefsHelper.getList(app, PREFS_SYNC, KEY_PENDING,
                PrefsHelper.listType(PendingOccurrence.class));
    }

    /**
     * Remove uma ocorrência pelo seu ID local após envio com sucesso.
     */
    public static synchronized void remove(Context context, String localId) {
        if (localId == null) {
            return;
        }
        Context app = context.getApplicationContext();
        List<PendingOccurrence> list = new ArrayList<>(getAll(app));
        boolean removed = false;
        for (int i = 0; i < list.size(); i++) {
            if (localId.equals(list.get(i).getLocalId())) {
                list.remove(i);
                removed = true;
                break;
            }
        }
        if (removed) {
            PrefsHelper.putList(app, PREFS_SYNC, KEY_PENDING, list);
        }
    }

    /**
     * Retorna a quantidade de itens aguardando sincronização.
     */
    public static synchronized int count(Context context) {
        return getAll(context).size();
    }

    /**
     * Limpa toda a fila (ex: em testes ou logout).
     */
    public static synchronized void clear(Context context) {
        PrefsHelper.clear(context.getApplicationContext(), PREFS_SYNC);
    }
}
