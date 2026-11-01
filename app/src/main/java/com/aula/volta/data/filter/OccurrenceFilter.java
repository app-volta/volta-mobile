package com.aula.volta.data.filter;

import com.aula.volta.data.model.Occurrence;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class OccurrenceFilter {

    private OccurrenceFilter() {

    }

    public static List<Occurrence> filter(List<Occurrence> source,
                                          String query,
                                          List<String> filterMaterials,
                                          List<String> filterStatuses) {
        return filter(source, query, filterMaterials, filterStatuses, null);
    }

    public static List<Occurrence> filter(List<Occurrence> source,
                                          String query,
                                          List<String> filterMaterials,
                                          List<String> filterStatuses,
                                          List<String> filterPriorities) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedQuery = normalize(query);
        String cleanIdQuery = normalizedQuery.startsWith("#") ? normalizedQuery.substring(1) : normalizedQuery;

        boolean hasQuery = !normalizedQuery.isEmpty();
        boolean hasMaterials = filterMaterials != null && !filterMaterials.isEmpty();
        boolean hasStatuses = filterStatuses != null && !filterStatuses.isEmpty();
        boolean hasPriorities = filterPriorities != null && !filterPriorities.isEmpty();

        if (!hasQuery && !hasMaterials && !hasStatuses && !hasPriorities) {
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

            if (hasPriorities && !matchesPriority(occurrence, filterPriorities)) {
                continue;
            }

            results.add(occurrence);
        }

        return results;
    }

    public static boolean hasActiveFilters(String query, List<String> materials, List<String> statuses) {
        return hasActiveFilters(query, materials, statuses, null);
    }

    public static boolean hasActiveFilters(String query, List<String> materials, List<String> statuses, List<String> priorities) {
        boolean hasText = query != null && !query.trim().isEmpty();
        boolean hasMats = materials != null && !materials.isEmpty();
        boolean hasStats = statuses != null && !statuses.isEmpty();
        boolean hasPrio = priorities != null && !priorities.isEmpty();
        return hasText || hasMats || hasStats || hasPrio;
    }

    private static boolean matchesQuery(Occurrence occurrence, String query, String cleanId) {

        if (occurrence.getId() != null) {
            String normId = normalize(occurrence.getId());
            if (normId.contains(cleanId) || normId.contains(query)) {
                return true;
            }
        }

        if (occurrence.getTitulo() != null && normalize(occurrence.getTitulo()).contains(query)) {
            return true;
        }

        if (occurrence.getSetor() != null && normalize(occurrence.getSetor()).contains(query)) {
            return true;
        }

        if (occurrence.getMaterial() != null && normalize(occurrence.getMaterial()).contains(query)) {
            return true;
        }

        if (occurrence.getPrioridade() != null && normalize(occurrence.getPrioridade()).contains(query)) {
            return true;
        }

        return false;
    }

    private static boolean isElectronic(String material, String titulo) {
        String c = (material != null ? material : "") + " " + (titulo != null ? titulo : "");
        return c.contains("eletron") || c.contains("computad") || c.contains("monitor")
                || c.contains("tela") || c.contains("placa") || c.contains("teclado")
                || c.contains("mouse") || c.contains("bateria") || c.contains("pilha")
                || c.contains("periferic") || c.contains("cabo") || c.contains("fio");
    }

    private static boolean isFurniture(String material, String titulo) {
        String c = (material != null ? material : "") + " " + (titulo != null ? titulo : "");
        return c.contains("mobilia") || c.contains("mobiliari") || c.contains("cadeir")
                || c.contains("mesa") || c.contains("sofa") || c.contains("armario")
                || c.contains("estante") || c.contains("moveis") || c.contains("movel");
    }

        private static boolean matchesMaterial(Occurrence occurrence, List<String> filterMaterials) {
        if (occurrence.getMaterial() == null && occurrence.getTitulo() == null) {
            return false;
        }
        String material = occurrence.getMaterial() != null ? normalize(occurrence.getMaterial()) : "";
        String titulo = occurrence.getTitulo() != null ? normalize(occurrence.getTitulo()) : "";

        boolean electronicItem = isElectronic(material, titulo);
        boolean furnitureItem = isFurniture(material, titulo);

        for (String filter : filterMaterials) {
            if (filter == null || filter.trim().isEmpty()) {
                continue;
            }
            String normFilter = normalize(filter);

            if (electronicItem) {
                if (normFilter.contains("eletron")) {
                    return true;
                }
                continue;
            }

            if (furnitureItem && (normFilter.contains("mobilia") || normFilter.contains("moveis"))) {
                return true;
            }

            if (material.contains(normFilter) || titulo.contains(normFilter)) {
                return true;
            }

            if (normFilter.contains("madeira")) {

                if ((material.contains("papel") || titulo.contains("papel") || material.contains("plast") || titulo.contains("plast"))
                        && !material.contains("madeira") && !titulo.contains("madeira")) {

                } else if (material.contains("pallet") || material.contains("palete")
                        || material.contains("estrado") || material.contains("caixote") || material.contains("tabua")
                        || material.contains("pbr") || titulo.contains("madeira")) {
                    return true;
                }
            }
            if (normFilter.contains("plast") && (material.contains("pet") || material.contains("isopor")
                    || material.contains("eps") || material.contains("filme") || material.contains("bombona")
                    || material.contains("copo") || titulo.contains("plast") || titulo.contains("isopor") || titulo.contains("eps"))) {
                return true;
            }
            if (normFilter.contains("metal") && (material.contains("alumin") || material.contains("ferro")
                    || material.contains("aco") || material.contains("lata") || material.contains("sucata")
                    || titulo.contains("metal") || titulo.contains("alumin") || titulo.contains("ferro") || titulo.contains("aco") || titulo.contains("sucata"))) {
                return true;
            }
            if (normFilter.contains("papel") && (material.contains("cartao") || material.contains("kraft")
                    || material.contains("papelao") || titulo.contains("papel") || titulo.contains("papelao") || titulo.contains("kraft"))) {
                return true;
            }
            if (normFilter.contains("vidro") && (material.contains("garrafa") || material.contains("frasco")
                    || material.contains("caco") || material.contains("pote") || titulo.contains("vidro"))) {
                return true;
            }
            if (normFilter.contains("organ") && (material.contains("alimento") || material.contains("comida")
                    || material.contains("refeicao") || material.contains("resto") || titulo.contains("organ") || titulo.contains("alimento") || titulo.contains("comida"))) {
                return true;
            }
            if (normFilter.contains("quimic") && (material.contains("oleo") || material.contains("graxa")
                    || material.contains("solvent") || material.contains("tinta") || material.contains("perig")
                    || material.contains("tox") || material.contains("quim") || titulo.contains("quimic") || titulo.contains("oleo"))) {
                return true;
            }
            if (normFilter.contains("textil") && (material.contains("pano") || material.contains("tecido")
                    || material.contains("uniforme") || material.contains("algod") || titulo.contains("textil") || titulo.contains("uniforme"))) {
                return true;
            }
            if (normFilter.contains("misto") && (material.contains("misto") || material.contains("indiferenc")
                    || material.contains("diverso") || titulo.contains("misto"))) {
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
        String s = status.toUpperCase(Locale.ROOT);

        for (String filter : filterStatuses) {
            if (filter == null) {
                continue;
            }
            String f = filter.toUpperCase(Locale.ROOT);
            if (s.equalsIgnoreCase(f)) {
                return true;
            }

            if ("ABERTA".equals(f)) {
                if (s.contains("NOVO") || s.contains("ABERT") || s.contains("ANALIS")
                        || s.contains("PENDING") || s.contains("AGUARDANDO")) {
                    return true;
                }
            }

            if ("APROVADA".equals(f) || "RESOLVIDO".equals(f)) {
                if (s.contains("APROVAD") || s.contains("RESOLVID") || s.contains("CLOSED")
                        || s.contains("CONCLUID") || s.contains("COLETAD")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesPriority(Occurrence occurrence, List<String> filterPriorities) {
        String priority = occurrence.getPrioridade();
        if (priority == null) {
            return false;
        }
        String p = normalize(priority).toUpperCase(Locale.ROOT);

        for (String filter : filterPriorities) {
            if (filter == null) continue;
            String f = normalize(filter).toUpperCase(Locale.ROOT);
            if (p.contains(f) || f.contains(p)) {
                return true;
            }
            if (f.contains("ALT") && (p.contains("ALT") || p.contains("HIGH"))) {
                return true;
            }
            if (f.contains("MED") && (p.contains("MED") || p.contains("MID"))) {
                return true;
            }
            if (f.contains("BAIX") && (p.contains("BAIX") || p.contains("LOW"))) {
                return true;
            }
        }
        return false;
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim().toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
