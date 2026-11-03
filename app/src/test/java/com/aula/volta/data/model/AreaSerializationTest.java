package com.aula.volta.data.model;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AreaSerializationTest {

    private final Gson gson = new Gson();

    @Test
    public void serializesAndDeserializesAreaResponseDTO() {
        AreaResponseDTO dto = new AreaResponseDTO();
        dto.setId("area-uuid-123");
        dto.setCompanyId("company-uuid-456");
        dto.setSectorName("Almoxarifado Central");
        dto.setLocationDescription("Galpão 3, Térreo");

        String json = gson.toJson(dto);
        AreaResponseDTO parsed = gson.fromJson(json, AreaResponseDTO.class);

        assertEquals("area-uuid-123", parsed.getId());
        assertEquals("company-uuid-456", parsed.getCompanyId());
        assertEquals("Almoxarifado Central", parsed.getSectorName());
        assertEquals("Galpão 3, Térreo", parsed.getLocationDescription());
    }

    @Test
    public void parsesBackendAreaList_andExtractsSortedSectorNames() {
        String jsonList = "[{\"id\":\"1\",\"companyId\":\"c1\",\"sectorName\":\"Manutenção\",\"locationDescription\":\"Setor B\"}," +
                "{\"id\":\"2\",\"companyId\":\"c1\",\"sectorName\":\"Almoxarifado\",\"locationDescription\":\"Setor A\"}," +
                "{\"id\":\"3\",\"companyId\":\"c1\",\"sectorName\":\"Produção\",\"locationDescription\":\"Setor C\"}]";

        JsonArray arr = JsonParser.parseString(jsonList).getAsJsonArray();
        List<AreaResponseDTO> list = new ArrayList<>();
        List<String> names = new ArrayList<>();

        for (int i = 0; i < arr.size(); i++) {
            AreaResponseDTO dto = gson.fromJson(arr.get(i), AreaResponseDTO.class);
            list.add(dto);
            if (dto.getSectorName() != null && !names.contains(dto.getSectorName())) {
                names.add(dto.getSectorName());
            }
        }

        Collections.sort(names, String::compareToIgnoreCase);

        assertEquals(3, list.size());
        assertEquals("Almoxarifado", names.get(0));
        assertEquals("Manutenção", names.get(1));
        assertEquals("Produção", names.get(2));
    }

    @Test
    public void handlesNullSectorNameSafely() {
        String json = "{\"id\":\"4\",\"companyId\":\"c2\"}";
        AreaResponseDTO dto = gson.fromJson(json, AreaResponseDTO.class);

        assertNotNull(dto);
        assertEquals("4", dto.getId());
        assertEquals("c2", dto.getCompanyId());
        assertTrue(dto.getSectorName() == null);
    }

    @Test
    public void deserializesAlternateFieldNames_nameAndLocation() {
        String json = "{\"areaId\":\"area-99\",\"name\":\"Galpão Logístico\",\"location\":\"Portão 4\"}";
        AreaResponseDTO dto = gson.fromJson(json, AreaResponseDTO.class);

        assertNotNull(dto);
        assertEquals("area-99", dto.getId());
        assertEquals("Galpão Logístico", dto.getSectorName());
        assertEquals("Portão 4", dto.getLocationDescription());
    }

    @Test
    public void deserializesAlternateFieldNames_sectorAndDescription() {
        String json = "{\"area_id\":\"area-88\",\"sector\":\"Triagem\",\"description\":\"Setor Externo\"}";
        AreaResponseDTO dto = gson.fromJson(json, AreaResponseDTO.class);

        assertNotNull(dto);
        assertEquals("area-88", dto.getId());
        assertEquals("Triagem", dto.getSectorName());
        assertEquals("Setor Externo", dto.getLocationDescription());
    }
}
