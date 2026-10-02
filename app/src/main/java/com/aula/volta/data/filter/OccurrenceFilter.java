package com.aula.volta.data.filter;

import com.aula.volta.data.model.Occurrence;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Utilitário puro de filtragem composta e busca textual instantânea de ocorrências.
 * Desacoplado do framework Android para permitir testes unitários rápidos e determinísticos.
 */
public final class OccurrenceFilter {

    private OccurrenceFilter() {
        // Utilitário estático
    }

    /**
     * Aplica filtros combinados (busca textual E/OU materiais E/OU status).
     * Retorna uma nova lista imutável com os resultados.
     */
    public static List<Occurrence> filter(List<Occurrence> source,
                                          String query,
                                          List<String> filterMaterials,
                                          List<String> filterStatuses) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedQuery = normalize(query);
        String cleanIdQuery = normalizedQuery.startsWith("#") ? normalizedQuery.substring(1) : normalizedQuery;

        boolean hasQuery = !normalizedQuery.isEmpty();
        boolean hasMaterials = filterMaterials != null && !filterMaterials.isEmpty();
        boolean hasStatuses = filterStatuses != null && !filterStatuses.isEmpty();

        if (!hasQuery && !hasMaterials && !hasStatuses) {
            return new ArrayList<>(source);
        }

        List<Occurrence> results = new ArrayList<>();
        for (Occurrence occurrence : source) {
            if (occurrence == null) {
                continue;
            }

            if (hasQuery && !matchesQuery(occurrence, normalizedQuery, cleanIdQuery)) {
                continue;
            }

            if (hasMaterials && !matchesMaterial(occurrence, filterMaterials)) {
                continue;
            }

            if (hasStatuses && !matchesStatus(occurrence, filterStatuses)) {
                continue;
            }

            results.add(occurrence);
        }

        return results;
    }

    /**
     * Verifica se há qualquer filtro ativo (texto ou categorias).
     */
    public static boolean hasActiveFilters(String query, List<String> materials, List<String> statuses) {
        boolean hasText = query != null && !query.trim().isEmpty();
        boolean hasMats = materials != null && !materials.isEmpty();
        boolean hasStats = statuses != null && !statuses.isEmpty();
        return hasText || hasMats || hasStats;
    }

    private static boolean matchesQuery(Occurrence occurrence, String query, String cleanId) {
        // Match por ID (suporta "48291", "#48291", "48")
        if (occurrence.getId() != null) {
            String normId = normalize(occurrence.getId());
            if (normId.contains(cleanId) || normId.contains(query)) {
                return true;
            }
        }

        // Match por título
        if (occurrence.getTitulo() != null && normalize(occurrence.getTitulo()).contains(query)) {
            return true;
        }

        // Match por setor / localização
        if (occurrence.getSetor() != null && normalize(occurrence.getSetor()).contains(query)) {
            return true;
        }

        // Match por tipo de material
        if (occurrence.getMaterial() != null && normalize(occurrence.getMaterial()).contains(query)) {
            return true;
        }

        // Match por prioridade (ex: "alta", "media")
        if (occurrence.getPrioridade() != null && normalize(occurrence.getPrioridade()).contains(query)) {
            return true;
        }

        return false;
    }

    private static boolean matchesMaterial(Occurrence occurrence, List<String> filterMaterials) {
        if (occurrence.getMaterial() == null) {
            return false;
        }
        String material = normalize(occurrence.getMaterial());
        for (String filter : filterMaterials) {
            if (filter != null && material.contains(normalize(filter))) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesStatus(Occurrence occurrence, List<String> filterStatuses) {
        String status = occurrence.getStatus();
        if (status == null) {
            return false;
        }

        for (String filter : filterStatuses) {
            if (filter == null) {
                continue;
            }
            if (status.equalsIgnoreCase(filter)) {
                return true;
            }
            // Compatibilidade: RESOLVIDO abrange tanto RESOLVIDO quanto APROVADA
            if ("RESOLVIDO".equalsIgnoreCase(filter) && "APROVADA".equalsIgnoreCase(status)) {
                return true;
            }
            if ("APROVADA".equalsIgnoreCase(filter) && "RESOLVIDO".equalsIgnoreCase(status)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove diacríticos (acentos) e converte para minúsculas para busca robusta.
     */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim().toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
