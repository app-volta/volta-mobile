package com.aula.volta.data.api;

import com.aula.volta.data.model.AiAnalysisJSON;
import com.aula.volta.data.model.auth.LoginRequest;
import com.aula.volta.data.model.auth.LoginResponse;
import com.aula.volta.data.model.chat.ChatHistoryItem;
import com.aula.volta.data.model.chat.ChatMessageRequest;
import com.aula.volta.data.model.chat.ChatMessageResponse;
import com.aula.volta.data.model.chat.CreateSessionResponse;
import com.aula.volta.data.model.incident.AiReportResponseDTO;
import com.aula.volta.data.model.incident.IncidentRequestDTO;
import com.aula.volta.data.model.incident.IncidentResponseDTO;
import com.google.gson.Gson;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Testes unitários dos contratos de API de Login, Chatbot QA e Análise de Imagem por IA.
 */
public class ChatbotIntegrationContractTest {

    private final Gson gson = new Gson();

    @Test
    public void serialize_loginRequest() {
        LoginRequest req = new LoginRequest("volta.brasil.2026@gmail.com", "Volta@2026");
        String json = gson.toJson(req);

        assertTrue(json.contains("\"email\":\"volta.brasil.2026@gmail.com\""));
        assertTrue(json.contains("\"password\":\"Volta@2026\""));
    }

    @Test
    public void deserialize_loginResponseWithTokenAndExpiresIn() {
        String json = "{\"token\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummyPayload\",\"expiresIn\":86400000,\"name\":\"Operador QA\",\"email\":\"qa@volta.com\"}";
        LoginResponse res = gson.fromJson(json, LoginResponse.class);

        assertNotNull(res);
        assertTrue(res.hasToken());
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummyPayload", res.getToken());
        assertEquals(Long.valueOf(86400000L), res.getExpiresIn());
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

    @Test
    public void serializeAndDeserialize_incidentAndAiReportWithCloudinaryUrl() {
        String cloudinaryUrl = "https://res.cloudinary.com/kgou1air/image/upload/v1729500000/Ocorrencia/Assets/Images/volta_photo_1.jpg";
        IncidentRequestDTO req = new IncidentRequestDTO(
                "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                "8ba85f64-5717-4562-b3fc-2c963f66afa9",
                cloudinaryUrl,
                "Caixas de papelão acumuladas ao lado da câmara fria",
                "LOW",
                42.0,
                "HIGH"
        );
        String reqJson = gson.toJson(req);
        assertTrue(reqJson.contains("\"photoUrl\":\"" + cloudinaryUrl + "\""));
        assertTrue(reqJson.contains("\"areaId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\""));

        String incRespJson = "{"
                + "\"id\":\"inc-uuid-001\","
                + "\"photoUrl\":\"" + cloudinaryUrl + "\","
                + "\"contaminationLevel\":\"MEDIUM\","
                + "\"estimatedQuantity\":42.0,"
                + "\"priority\":\"HIGH\","
                + "\"status\":\"PENDING\""
                + "}";
        IncidentResponseDTO incResp = gson.fromJson(incRespJson, IncidentResponseDTO.class);
        assertNotNull(incResp);
        assertEquals("inc-uuid-001", incResp.getId());
        assertEquals(cloudinaryUrl, incResp.getPhotoUrl());

        String aiReportJson = "{"
                + "\"id\":\"rep-uuid-001\","
                + "\"incidentId\":\"inc-uuid-001\","
                + "\"detectedWasteType\":\"Papelão ondulado\","
                + "\"aiContaminationLevel\":\"MEDIUM\","
                + "\"recommendations\":\"Manter em local coberto e acionar coleta em 24h\","
                + "\"reportText\":\"Identificado lote de caixas de papelão com leve umidade superficial.\","
                + "\"generatedAt\":\"2026-10-21T19:00:00\""
                + "}";
        AiReportResponseDTO report = gson.fromJson(aiReportJson, AiReportResponseDTO.class);
        assertNotNull(report);
        assertEquals("Papelão ondulado", report.getDetectedWasteType());
        assertEquals("MEDIUM", report.getAiContaminationLevel());

        AiAnalysisJSON mapped = AiAnalysisJSON.fromAiReport(report, 42.0);
        assertNotNull(mapped);
        assertEquals("inc-uuid-001", mapped.getIncidentId());
        assertEquals("Papelão ondulado", mapped.getMaterial());
        assertEquals(42.0, mapped.getQuantidadeEstimada(), 0.001);
        assertNotNull(mapped.getContaminacao());
        assertEquals("Média", mapped.getContaminacao().getNivel());
        assertTrue(mapped.getContaminacao().isPresente());
        assertEquals("Manter em local coberto e acionar coleta em 24h", mapped.getObservacoes());
        assertEquals("Identificado lote de caixas de papelão com leve umidade superficial.", mapped.getReportText());
    }
}
