package com.aula.volta.data.local;

import android.content.Context;

import com.aula.volta.data.model.OccurrenceHistory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class AuditLog {

    private static final String PREFS = "cache_audit";
    private static final String KEY_PREFIX = "audit_";

    private AuditLog() {
    }

    public static void append(Context context, String usuario, String acao, String entidade,
                              String entidadeId, String valorAnterior, String valorNovo) {
        String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date());
        OccurrenceHistory entry = new OccurrenceHistory(
                UUID.randomUUID().toString(), usuario, acao, entidade,
                entidadeId, valorAnterior, valorNovo, timestamp);

        List<OccurrenceHistory> list = list(context, entidadeId);
        list.add(entry);
        PrefsHelper.putList(context, PREFS, KEY_PREFIX + entidadeId, list);
    }

    public static List<OccurrenceHistory> list(Context context, String entidadeId) {
        return PrefsHelper.getList(context, PREFS, KEY_PREFIX + entidadeId,
                PrefsHelper.listType(OccurrenceHistory.class));
    }
}
