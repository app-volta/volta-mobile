package com.aula.volta.data.sync;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Testes unitários para validar o modelo de sincronização offline.
 * Fase 25 — Sincronização Offline Industrial & Resiliência.
 */
public class SyncQueueModelTest {

    @Test
    public void pendingOccurrence_constructsAndRetrievesFieldsCorrectly() {
        long now = System.currentTimeMillis();
        PendingOccurrence pending = new PendingOccurrence(
                "off_123456",
                "/cache/photo.jpg",
                "Papelão úmido no setor B2",
                "Frigorífico",
                "Papelão",
                45.5,
                "kg",
                now
        );

        assertEquals("off_123456", pending.getLocalId());
        assertEquals("/cache/photo.jpg", pending.getFotoPath());
        assertEquals("Papelão úmido no setor B2", pending.getDescricao());
        assertEquals("Frigorífico", pending.getSetor());
        assertEquals("Papelão", pending.getMaterial());
        assertEquals(45.5, pending.getQuantidadeEstimada(), 0.001);
        assertEquals("kg", pending.getUnidade());
        assertEquals(now, pending.getTimestamp());
    }

    @Test
    public void pendingOccurrence_settersUpdateValues() {
        PendingOccurrence pending = new PendingOccurrence();
        pending.setLocalId("off_999");
        pending.setMaterial("Plástico Filme");
        pending.setQuantidadeEstimada(12.0);
        pending.setUnidade("kg");
        pending.setSetor("Expedição");

        assertEquals("off_999", pending.getLocalId());
        assertEquals("Plástico Filme", pending.getMaterial());
        assertEquals(12.0, pending.getQuantidadeEstimada(), 0.001);
        assertEquals("kg", pending.getUnidade());
        assertEquals("Expedição", pending.getSetor());
    }
}
