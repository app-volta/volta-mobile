package com.aula.volta.data.api;

import com.aula.volta.data.model.auth.LoginRequest;
import com.aula.volta.data.model.auth.LoginResponse;
import com.aula.volta.data.model.chat.ChatHistoryItem;
import com.aula.volta.data.model.chat.ChatMessageRequest;
import com.aula.volta.data.model.chat.ChatMessageResponse;
import com.aula.volta.data.model.chat.CreateSessionResponse;
import com.google.gson.Gson;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Testes unitários dos contratos de API de Login e Chatbot QA.
 */
public class ChatbotIntegrationContractTest {

    private final Gson gson = new Gson();

    @Test
    public void serialize_loginRequest() {
        LoginRequest req = new LoginRequest("qa_user@volta.com", "segredoQA123");
        String json = gson.toJson(req);

        assertTrue(json.contains("\"email\":\"qa_user@volta.com\""));
        assertTrue(json.contains("\"password\":\"segredoQA123\""));
    }

    @Test
    public void deserialize_loginResponseWithToken() {
        String json = "{\"token\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummyPayload\",\"name\":\"Operador QA\",\"email\":\"qa@volta.com\"}";
        LoginResponse res = gson.fromJson(json, LoginResponse.class);

        assertNotNull(res);
        assertTrue(res.hasToken());
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummyPayload", res.getToken());
        assertEquals("Operador QA", res.getName());
        assertEquals("qa@volta.com", res.getEmail());
    }

    @Test
    public void deserialize_loginResponseAlternativeAccessToken() {
        String json = "{\"access_token\":\"jwt_sample_12345\"}";
        LoginResponse res = gson.fromJson(json, LoginResponse.class);

        assertNotNull(res);
        assertTrue(res.hasToken());
        assertEquals("jwt_sample_12345", res.getToken());
    }

    @Test
    public void deserialize_createSessionResponse() {
        String json = "{\"session_id\":\"sess_qa_987654321\",\"created_at\":\"2026-10-04T20:00:00Z\"}";
        CreateSessionResponse res = gson.fromJson(json, CreateSessionResponse.class);

        assertNotNull(res);
        assertEquals("sess_qa_987654321", res.getSessionId());
        assertEquals("2026-10-04T20:00:00Z", res.getCreatedAt());
    }

    @Test
    public void serialize_chatMessageRequest() {
        ChatMessageRequest req = new ChatMessageRequest("sess_123", "Como descartar isopor de marmita?");
        String json = gson.toJson(req);

        assertTrue(json.contains("\"session_id\":\"sess_123\""));
        assertTrue(json.contains("\"message\":\"Como descartar isopor de marmita?\""));
    }

    @Test
    public void deserialize_chatMessageResponseWithNestedResponse() {
        String json = "{"
                + "\"session_id\":\"sess_123\","
                + "\"response\":{"
                + "  \"answer\":\"O isopor limpo pode ser compactado e encaminhado para cooperativas credenciadas.\","
                + "  \"citations\":[{\"doc\":\"Norma ABNT 13230\",\"page\":12}],"
                + "  \"recommended_actions\":[\"Solicitar coleta especial\",\"Verificar contaminação orgânica\"]"
                + "},"
                + "\"requires_human_validation\":true"
                + "}";

        ChatMessageResponse res = gson.fromJson(json, ChatMessageResponse.class);

        assertNotNull(res);
        assertEquals("sess_123", res.getSessionId());
        assertEquals("O isopor limpo pode ser compactado e encaminhado para cooperativas credenciadas.", res.getAnswer());
        assertNotNull(res.getRecommendedActions());
        assertEquals(2, res.getRecommendedActions().size());
        assertEquals("Solicitar coleta especial", res.getRecommendedActions().get(0));
        assertNotNull(res.getCitations());
        assertEquals(1, res.getCitations().size());
        assertTrue(res.isRequiresHumanValidation());
    }

    @Test
    public void deserialize_chatMessageResponseDirectAnswer() {
        String json = "{"
                + "\"session_id\":\"sess_456\","
                + "\"answer\":\"Descarte de papelão ondulado deve ser feito no container azul.\","
                + "\"requires_human_validation\":false"
                + "}";

        ChatMessageResponse res = gson.fromJson(json, ChatMessageResponse.class);

        assertNotNull(res);
        assertEquals("Descarte de papelão ondulado deve ser feito no container azul.", res.getAnswer());
        assertFalse(res.isRequiresHumanValidation());
    }

    @Test
    public void deserialize_chatHistoryItem() {
        String userJson = "{\"role\":\"user\",\"content\":\"O que fazer com caixas úmidas?\"}";
        ChatHistoryItem userMsg = gson.fromJson(userJson, ChatHistoryItem.class);
        assertNotNull(userMsg);
        assertTrue(userMsg.isUser());
        assertEquals("O que fazer com caixas úmidas?", userMsg.getContent());

        String botJson = "{\"role\":\"assistant\",\"content\":\"Devem ser separadas dos fardos secos.\"}";
        ChatHistoryItem botMsg = gson.fromJson(botJson, ChatHistoryItem.class);
        assertNotNull(botMsg);
        assertFalse(botMsg.isUser());
        assertEquals("Devem ser separadas dos fardos secos.", botMsg.getContent());
    }
}
