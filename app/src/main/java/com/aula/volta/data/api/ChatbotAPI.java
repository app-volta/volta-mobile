package com.aula.volta.data.api;

import com.aula.volta.data.model.chat.ChatMessageRequest;
import com.aula.volta.data.model.chat.ChatMessageResponse;
import com.aula.volta.data.model.chat.CreateSessionResponse;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

/**
 * Interface Retrofit para a API do Chatbot e IA de Visao VOLTA (QA: https://chat.qa.54.210.1.34.sslip.io).
 *
 * Contratos mapeados:
 * 1. Criar sessao: POST /v1/sessions
 * 2. Enviar mensagem: POST /v1/chat
 * 3. Historico: GET /v1/sessions/{session_id}/history
 * 4. Encerrar sessao: POST /v1/sessions/{session_id}/close
 * 5. Predicao de Residuo por Imagem: POST /v1/occurrences/predict (Gemini Vision + Judge LLM)
 * 6. Criacao de Rascunho Auditado: POST /v1/occurrences/drafts
 * 7. Aprovacao de Rascunho: POST /v1/occurrences/drafts/{draft_id}/approve
 */
public interface ChatbotAPI {

    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @POST("v1/sessions")
    Call<CreateSessionResponse> createSession();

    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @POST("v1/chat")
    Call<ChatMessageResponse> sendMessage(@Body ChatMessageRequest request);

    @Headers({
            "Accept: application/json; charset=utf-8"
    })
    @GET("v1/sessions/{session_id}/history")
    Call<JsonElement> getHistory(@Path("session_id") String sessionId);

    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @POST("v1/sessions/{session_id}/close")
    Call<JsonObject> closeSession(@Path("session_id") String sessionId);

    /**
     * Envia a foto do residuo diretamente para o Gemini Vision analisar tipo, nivel de contaminacao, volume e risco.
     * Retorna JSON validado e homologado pelo juiz LLM (AnaliseResiduoIA).
     */
    @Multipart
    @POST("v1/occurrences/predict")
    Call<JsonObject> predictWaste(@Part MultipartBody.Part file);

    /**
     * Registra o rascunho de ocorrencia com a proveniencia criptografica da IA.
     */
    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @POST("v1/occurrences/drafts")
    Call<JsonObject> createOccurrenceDraft(@Body JsonObject request);

    /**
     * Homologa/aprova rascunho no banco transacional.
     */
    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @POST("v1/occurrences/drafts/{draft_id}/approve")
    Call<JsonObject> approveOccurrenceDraft(@Path("draft_id") String draftId);
}
