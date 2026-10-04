package com.aula.volta.data.api;

import com.aula.volta.data.model.chat.ChatMessageRequest;
import com.aula.volta.data.model.chat.ChatMessageResponse;
import com.aula.volta.data.model.chat.CreateSessionResponse;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Interface Retrofit para a API do Chatbot VOLTA (QA: https://chat.qa.54.210.1.34.sslip.io).
 *
 * Contratos:
 * 1. Criar sessão: POST /v1/sessions
 * 2. Enviar mensagem: POST /v1/chat
 * 3. Histórico: GET /v1/sessions/{session_id}/history
 * 4. Encerrar sessão: POST /v1/sessions/{session_id}/close
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
}
