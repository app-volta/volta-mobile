package com.aula.volta.data.model.chat;

import com.google.gson.annotations.SerializedName;

public class ChatMessageRequest {

    @SerializedName("session_id")
    private final String sessionId;

    @SerializedName("message")
    private final String message;

    public ChatMessageRequest(String sessionId, String message) {
        this.sessionId = sessionId;
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getMessage() {
        return message;
    }
}
