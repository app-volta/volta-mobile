package com.aula.volta.data.model.chat;

import com.google.gson.annotations.SerializedName;

/**
 * Resposta de criação de sessão do Chatbot VOLTA (POST /v1/sessions).
 */
public class CreateSessionResponse {

    @SerializedName(value = "session_id", alternate = {"sessionId", "id"})
    private String sessionId;

    @SerializedName("created_at")
    private String createdAt;

    public CreateSessionResponse() {
    }

    public CreateSessionResponse(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
