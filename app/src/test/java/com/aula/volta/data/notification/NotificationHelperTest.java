package com.aula.volta.data.notification;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Testes unitários para validar constantes e integridade dos canais de notificação.
 * Fase 26 — Canais de Notificação Android & Notificações Locais em Tempo Real.
 */
public class NotificationHelperTest {

    @Test
    public void notificationChannels_constantsAreDefinedCorrectly() {
        assertEquals("volta_occurrences", NotificationHelper.CHANNEL_OCCURRENCES);
        assertEquals("volta_goals", NotificationHelper.CHANNEL_GOALS);
        assertEquals("volta_sync", NotificationHelper.CHANNEL_SYNC);
    }

    @Test
    public void notificationChannels_areDistinct() {
        assertNotEquals(NotificationHelper.CHANNEL_OCCURRENCES, NotificationHelper.CHANNEL_GOALS);
        assertNotEquals(NotificationHelper.CHANNEL_OCCURRENCES, NotificationHelper.CHANNEL_SYNC);
        assertNotEquals(NotificationHelper.CHANNEL_GOALS, NotificationHelper.CHANNEL_SYNC);
    }
}
