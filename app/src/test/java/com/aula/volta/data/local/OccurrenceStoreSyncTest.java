package com.aula.volta.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Testes unitários para o sistema de listeners e sincronização em tempo real do OccurrenceStore.
 */
public class OccurrenceStoreSyncTest {

    @Test
    public void testListenerNotificationCycle() {
        AtomicInteger callCount = new AtomicInteger(0);
        OccurrenceStore.StoreListener listener = callCount::incrementAndGet;

        OccurrenceStore.addListener(listener);
        OccurrenceStore.notifyListeners();
        assertEquals(1, callCount.get());

        OccurrenceStore.notifyListeners();
        assertEquals(2, callCount.get());

        OccurrenceStore.removeListener(listener);
        OccurrenceStore.notifyListeners();
        assertEquals(2, callCount.get()); // Não deve incrementar após remoção
    }

    @Test
    public void testMultipleListenersNotification() {
        AtomicInteger count1 = new AtomicInteger(0);
        AtomicInteger count2 = new AtomicInteger(0);

        OccurrenceStore.StoreListener l1 = count1::incrementAndGet;
        OccurrenceStore.StoreListener l2 = count2::incrementAndGet;

        OccurrenceStore.addListener(l1);
        OccurrenceStore.addListener(l2);

        OccurrenceStore.notifyListeners();
        assertEquals(1, count1.get());
        assertEquals(1, count2.get());

        OccurrenceStore.removeListener(l1);
        OccurrenceStore.removeListener(l2);
    }
}
