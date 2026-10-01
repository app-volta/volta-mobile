package com.aula.volta.data.model;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Testes unitários para modelo de Notificação (Fase 15).
 */
public class NotificationModelTest {

    @Test
    public void notification_gettersAndReadState() {
        Notification n = new Notification(
                "n1", "ocorrencia", "Coleta confirmada",
                "Cooperativa aceitou o pedido", "há 5 min",
                "ic_check", "#12A05E", "#E8F9F2", false, "48291");

        assertEquals("n1", n.getId());
        assertEquals("ocorrencia", n.getTipo());
        assertEquals("Coleta confirmada", n.getTitulo());
        assertEquals("Cooperativa aceitou o pedido", n.getDescricao());
        assertEquals("há 5 min", n.getTempoRelativo());
        assertEquals("ic_check", n.getIcone());
        assertEquals("#12A05E", n.getTint());
        assertEquals("#E8F9F2", n.getBg());
        assertFalse(n.isLida());
        assertEquals("48291", n.getOccurrenceId());

        n.setLida(true);
        assertTrue(n.isLida());
    }
}
