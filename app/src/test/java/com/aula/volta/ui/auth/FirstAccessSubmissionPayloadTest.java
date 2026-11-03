package com.aula.volta.ui.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class FirstAccessSubmissionPayloadTest {

    private final Gson gson = new Gson();

    @Test
    public void buildProfileSubmissionPayload_includesExplicitFirstLoginFalseFlags() {
        JsonObject body = new JsonObject();
        body.addProperty("name", "Carlos Silva");
        body.addProperty("position", "Almoxarifado");
        body.addProperty("firstLogin", false);
        body.addProperty("isFirstLogin", false);
        body.addProperty("primeiroLogin", false);
        body.addProperty("first_login", false);

        String json = gson.toJson(body);
        JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();

        assertEquals("Carlos Silva", parsed.get("name").getAsString());
        assertEquals("Almoxarifado", parsed.get("position").getAsString());
        assertFalse(parsed.get("firstLogin").getAsBoolean());
        assertFalse(parsed.get("isFirstLogin").getAsBoolean());
        assertFalse(parsed.get("primeiroLogin").getAsBoolean());
        assertFalse(parsed.get("first_login").getAsBoolean());
    }

    @Test
    public void payloadAllowsJsonDeserializationIntoTargetDtoSafely() {
        JsonObject body = new JsonObject();
        body.addProperty("name", "Mariana Souza");
        body.addProperty("position", "Produção");
        body.addProperty("firstLogin", false);

        com.aula.volta.data.model.UserResponseDTO dto =
                gson.fromJson(body, com.aula.volta.data.model.UserResponseDTO.class);

        assertEquals("Mariana Souza", dto.getName());
        assertEquals("Produção", dto.getPosition());
    }
}
