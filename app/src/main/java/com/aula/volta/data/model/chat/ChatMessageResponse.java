package com.aula.volta.data.model.chat;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Resposta retornada pelo endpoint POST /v1/chat do Chatbot VOLTA.
 * Contém answer, citations, recommended_actions e requires_human_validation.
 */
public class ChatMessageResponse {

    @SerializedName(value = "session_id", alternate = {"sessionId"})
    private String sessionId;

    @SerializedName("response")
    private ChatAnswerData responseData;

    @SerializedName("answer")
    private String directAnswer;

    @SerializedName("requires_human_validation")
    private Boolean requiresHumanValidation;

    public ChatMessageResponse() {
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public ChatAnswerData getResponseData() {
        return responseData;
    }

    public void setResponseData(ChatAnswerData responseData) {
        this.responseData = responseData;
    }

    public String getAnswer() {
        if (responseData != null && responseData.getAnswer() != null) {
            return responseData.getAnswer();
        }
        return directAnswer != null ? directAnswer : "";
    }

    public List<String> getRecommendedActions() {
        if (responseData != null && responseData.getRecommendedActions() != null) {
            return responseData.getRecommendedActions();
        }
        return new ArrayList<>();
    }

    public List<Object> getCitations() {
        if (responseData != null && responseData.getCitations() != null) {
            return responseData.getCitations();
        }
        return new ArrayList<>();
    }

    public boolean isRequiresHumanValidation() {
        return requiresHumanValidation != null && requiresHumanValidation;
    }

    public void setRequiresHumanValidation(Boolean requiresHumanValidation) {
        this.requiresHumanValidation = requiresHumanValidation;
    }

    /**
     * Estrutura interna de dados da resposta de chat (response).
     */
    public static class ChatAnswerData {

        @SerializedName(value = "answer", alternate = {"text", "message", "content"})
        private String answer;

        @SerializedName("citations")
        private List<Object> citations;

        @SerializedName(value = "recommended_actions", alternate = {"actions", "suggested_actions"})
        private List<String> recommendedActions;

        public ChatAnswerData() {
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }

        public List<Object> getCitations() {
            return citations;
        }

        public void setCitations(List<Object> citations) {
            this.citations = citations;
        }

        public List<String> getRecommendedActions() {
            return recommendedActions;
        }

        public void setRecommendedActions(List<String> recommendedActions) {
            this.recommendedActions = recommendedActions;
        }
    }
}
