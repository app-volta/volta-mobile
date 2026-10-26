package com.aula.volta.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.aula.volta.data.model.Notification;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Testes unitários para lógica do NotificationStore:
 * Deduplicação, contagem de não lidas, identificação de duplicatas e listeners.
 */
public class NotificationStoreTest {

    @Test
    public void testIsSameNotification_byId() {
        Notification n1 = new Notification("id_1", "OCORRENCIA", "Título 1", "Desc 1", "Agora", "bell", "#12A05E", "#E2F7EC", false, "occ_100");
        Notification n2 = new Notification("id_1", "OUTRO", "Outro título", "Outra desc", "Agora", "bell", "#12A05E", "#E2F7EC", true, null);

        assertTrue(NotificationStore.isSameNotification(n1, n2));
    }

    @Test
    public void testIsSameNotification_byOccurrenceAndTipo() {
        Notification n1 = new Notification("id_1", "COLETA", "Título 1", "Desc 1", "Agora", "bell", "#12A05E", "#E2F7EC", false, "occ_100");
        Notification n2 = new Notification("id_2", "coleta", "Título 2", "Desc 2", "Agora", "bell", "#12A05E", "#E2F7EC", false, "occ_100");

        assertTrue(NotificationStore.isSameNotification(n1, n2));
    }

    @Test
    public void testIsSameNotification_byTitleAndDescription() {
        Notification n1 = new Notification("id_1", "TIPO_A", "Mesmo Título", "Mesma Descrição", "Agora", "bell", "#12A05E", "#E2F7EC", false, null);
        Notification n2 = new Notification("id_2", "TIPO_B", "Mesmo Título", "Mesma Descrição", "10 min", "truck", "#12A05E", "#E2F7EC", true, null);

        assertTrue(NotificationStore.isSameNotification(n1, n2));
    }

    @Test
    public void testIsSameNotification_differentAreFalse() {
        Notification n1 = new Notification("id_1", "TIPO_A", "Título A", "Desc A", "Agora", "bell", "#12A05E", "#E2F7EC", false, "occ_1");
        Notification n2 = new Notification("id_2", "TIPO_B", "Título B", "Desc B", "Agora", "bell", "#12A05E", "#E2F7EC", false, "occ_2");

        assertFalse(NotificationStore.isSameNotification(n1, n2));
        assertFalse(NotificationStore.isSameNotification(null, n2));
        assertFalse(NotificationStore.isSameNotification(n1, null));
    }

    @Test
    public void testDeduplicate_removesRedundantEntries() {
        Notification n1 = new Notification("id_1", "OCORRENCIA", "Nova ocorrência", "Papelão 10kg", "Agora", "bell", "#12A05E", "#E2F7EC", false, "occ_1");
        Notification n2 = new Notification("id_2", "META", "Meta alcançada", "Parabéns!", "Ontem", "trophy", "#12A05E", "#E2F7EC", true, null);
        Notification n3 = new Notification("id_1", "OCORRENCIA", "Nova ocorrência", "Papelão 10kg", "Agora", "bell", "#12A05E", "#E2F7EC", false, "occ_1");

        List<Notification> list = Arrays.asList(n1, n2, n3);
        List<Notification> deduped = NotificationStore.deduplicate(list);

        assertEquals(2, deduped.size());
        assertEquals("id_1", deduped.get(0).getId());
        assertEquals("id_2", deduped.get(1).getId());
    }

    @Test
    public void testUnreadCount_calculatesAccurately() {
        Notification n1 = new Notification("id_1", "TIPO", "T1", "D1", "Agora", "bell", null, null, false, null);
        Notification n2 = new Notification("id_2", "TIPO", "T2", "D2", "Agora", "bell", null, null, true, null);
        Notification n3 = new Notification("id_3", "TIPO", "T3", "D3", "Agora", "bell", null, null, false, null);

        assertEquals(2, NotificationStore.unreadCount(Arrays.asList(n1, n2, n3)));
        assertEquals(0, NotificationStore.unreadCount(new ArrayList<>()));
        assertEquals(0, NotificationStore.unreadCount(null));
    }

    @Test
    public void testListenerLifecycle() {
        AtomicInteger count = new AtomicInteger(0);
        NotificationStore.ChangeListener listener = count::incrementAndGet;

        NotificationStore.addListener(listener);
        NotificationStore.notifyChange();
        assertEquals(1, count.get());

        NotificationStore.notifyChange();
        assertEquals(2, count.get());

        NotificationStore.removeListener(listener);
        NotificationStore.notifyChange();
        assertEquals(2, count.get()); // Não deve incrementar após remoção
    }
}
