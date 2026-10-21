package com.aula.volta.data.model.chat;

import com.google.gson.annotations.SerializedName;

/**
 * Representa um item ou mensagem no histórico da sessão (GET /v1/sessions/{session_id}/history).
 */
public class ChatHistoryItem {

    @SerializedName(value = "role", alternate = {"sender", "type", "author"})
    private String role;

    @SerializedName(value = "content", alternate = {"message", "text", "answer"})
    private String content;

    @SerializedName(value = "created_at", alternate = {"timestamp", "time"})
    private String createdAt;

    public ChatHistoryItem() {
    }

    public ChatHistoryItem(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isUser() {
        return "user".equalsIgnoreCase(role) || "usuario".equalsIgnoreCase(role);
    }
}
