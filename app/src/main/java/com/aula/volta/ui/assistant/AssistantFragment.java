package com.aula.volta.ui.assistant;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aula.volta.R;
import com.aula.volta.data.api.ChatbotAPI;
import com.aula.volta.data.api.ChatbotClient;
import com.aula.volta.data.local.SessionManager;
import com.aula.volta.data.model.chat.ChatMessageRequest;
import com.aula.volta.data.model.chat.ChatMessageResponse;
import com.aula.volta.data.model.chat.CreateSessionResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Assistente VOLTA — Integração com Chatbot no ambiente QA (https://chat.qa.54.210.1.34.sslip.io).
 *
 * Fluxo oficial:
 * 1. POST /v1/sessions (cria e recupera session_id)
 * 2. POST /v1/chat (envia mensagem, exibe response.answer, citations, recommended_actions)
 * 3. GET /v1/sessions/{session_id}/history (recupera histórico prévio)
 * 4. POST /v1/sessions/{session_id}/close (encerra sessão e indexa resumo)
 */
public class AssistantFragment extends Fragment {

    private static final String TAG = "AssistantFragment";

    private LinearLayout messages;
    private ScrollView scroll;
    private EditText input;
    private FrameLayout btnSend;
    private View typingIndicatorView;

    private String currentSessionId;
    private boolean isSending;

    public AssistantFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_assistant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        messages = view.findViewById(R.id.assistantMessages);
        scroll = view.findViewById(R.id.scrollAssistant);
        input = view.findViewById(R.id.etAssistant);
        btnSend = view.findViewById(R.id.btnSendAssistant);

        view.findViewById(R.id.btnBackAssistant).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        // Botão para encerrar sessão e iniciar nova conversa limpa
        View btnCloseSession = view.findViewById(R.id.btnCloseSession);
        if (btnCloseSession != null) {
            btnCloseSession.setOnClickListener(v -> restartSession());
        }

        // Sugestões rápidas de início
        View chipIsopor = view.findViewById(R.id.chipSuggestIsopor);
        if (chipIsopor != null) {
            chipIsopor.setOnClickListener(v -> ask(((TextView) v).getText().toString()));
        }

        View chipPapelao = view.findViewById(R.id.chipSuggestPapelao);
        if (chipPapelao != null) {
            chipPapelao.setOnClickListener(v -> ask(((TextView) v).getText().toString()));
        }

        View chipMeta = view.findViewById(R.id.chipSuggestMeta);
        if (chipMeta != null) {
            chipMeta.setOnClickListener(v -> ask(((TextView) v).getText().toString()));
        }

        btnSend.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (!text.isEmpty() && !isSending) {
                ask(text);
                input.setText("");
            }
        });

        // Inicializa a sessão do chatbot (ou recupera existente)
        initChatSession();
    }

    /**
     * Inicializa a sessão com o Chatbot. Se já houver um session_id salvo, tenta carregar o histórico.
     */
    private void initChatSession() {
        currentSessionId = SessionManager.getChatSessionId(requireContext());

        if (currentSessionId != null && !currentSessionId.trim().isEmpty()) {
            loadSessionHistory(currentSessionId);
        } else {
            createNewSession();
        }
    }

    /**
     * Cria uma nova sessão no Chatbot (POST /v1/sessions).
     */
    private void createNewSession() {
        ChatbotAPI api = ChatbotClient.get(requireContext()).create(ChatbotAPI.class);
        api.createSession().enqueue(new Callback<CreateSessionResponse>() {
            @Override
            public void onResponse(@NonNull Call<CreateSessionResponse> call,
                                   @NonNull Response<CreateSessionResponse> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null && response.body().getSessionId() != null) {
                    currentSessionId = response.body().getSessionId();
                    SessionManager.saveChatSessionId(requireContext(), currentSessionId);
                    Log.i(TAG, "Nova sessão criada com sucesso: " + currentSessionId);

                    messages.removeAllViews();
                    addBot(getString(R.string.assistant_intro), null, null, false);
                    addActionChips();
                } else {
                    Log.e(TAG, "Falha ao criar sessão no Chatbot: " + response.code());
                    handleConnectionError();
                }
            }

            @Override
            public void onFailure(@NonNull Call<CreateSessionResponse> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                Log.e(TAG, "Erro de rede ao criar sessão", t);
                handleConnectionError();
            }
        });
    }

    /**
     * Carrega o histórico da sessão existente (GET /v1/sessions/{session_id}/history).
     */
    private void loadSessionHistory(String sessionId) {
        ChatbotAPI api = ChatbotClient.get(requireContext()).create(ChatbotAPI.class);
        api.getHistory(sessionId).enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(@NonNull Call<JsonElement> call, @NonNull Response<JsonElement> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    messages.removeAllViews();
                    boolean hasMessages = parseAndRenderHistory(response.body());
                    if (!hasMessages) {
                        addBot(getString(R.string.assistant_intro), null, null, false);
                        addActionChips();
                    }
                } else if (response.code() == 404 || response.code() == 400) {
                    // Sessão anterior expirou ou não existe mais no servidor
                    createNewSession();
                } else {
                    handleConnectionError();
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonElement> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                Log.w(TAG, "Falha ao carregar histórico: " + t.getMessage());
                // Fallback: se estiver offline, mostra mensagem padrão
                messages.removeAllViews();
                addBot(getString(R.string.assistant_intro), null, null, false);
                addActionChips();
            }
        });
    }

    private boolean parseAndRenderHistory(JsonElement json) {
        try {
            JsonArray list = null;
            if (json.isJsonArray()) {
                list = json.getAsJsonArray();
            } else if (json.isJsonObject()) {
                JsonObject obj = json.getAsJsonObject();
                if (obj.has("history") && obj.get("history").isJsonArray()) {
                    list = obj.getAsJsonArray("history");
                } else if (obj.has("messages") && obj.get("messages").isJsonArray()) {
                    list = obj.getAsJsonArray("messages");
                }
            }

            if (list != null && list.size() > 0) {
                for (JsonElement el : list) {
                    if (el.isJsonObject()) {
                        JsonObject item = el.getAsJsonObject();
                        String role = item.has("role") ? item.get("role").getAsString() : "";
                        String content = "";
                        if (item.has("content")) {
                            content = item.get("content").getAsString();
                        } else if (item.has("message")) {
                            content = item.get("message").getAsString();
                        } else if (item.has("answer")) {
                            content = item.get("answer").getAsString();
                        }

                        if ("user".equalsIgnoreCase(role)) {
                            addUser(content);
                        } else {
                            addBot(content, null, null, false);
                        }
                    }
                }
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao analisar histórico", e);
        }
        return false;
    }

    /**
     * Encerra a sessão atual (POST /v1/sessions/{session_id}/close) e cria uma nova.
     */
    private void restartSession() {
        if (currentSessionId != null && !currentSessionId.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.chatbot_session_closed, Toast.LENGTH_SHORT).show();
            ChatbotAPI api = ChatbotClient.get(requireContext()).create(ChatbotAPI.class);
            api.closeSession(currentSessionId).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    SessionManager.clearChatSessionId(requireContext());
                    createNewSession();
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    SessionManager.clearChatSessionId(requireContext());
                    createNewSession();
                }
            });
        } else {
            createNewSession();
        }
    }

    /**
     * Envia mensagem do usuário para o Chatbot (POST /v1/chat).
     */
    private void ask(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        addUser(text);
        showTypingIndicator();

        if (currentSessionId == null || currentSessionId.trim().isEmpty()) {
            // Se por algum motivo a sessão não estiver pronta, cria primeiro
            ChatbotAPI api = ChatbotClient.get(requireContext()).create(ChatbotAPI.class);
            api.createSession().enqueue(new Callback<CreateSessionResponse>() {
                @Override
                public void onResponse(@NonNull Call<CreateSessionResponse> call,
                                       @NonNull Response<CreateSessionResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        currentSessionId = response.body().getSessionId();
                        SessionManager.saveChatSessionId(requireContext(), currentSessionId);
                        dispatchChatMessage(text);
                    } else {
                        hideTypingIndicator();
                        addBot(getString(R.string.chatbot_error_network), null, null, false);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<CreateSessionResponse> call, @NonNull Throwable t) {
                    hideTypingIndicator();
                    addBot(getString(R.string.chatbot_error_network), null, null, false);
                }
            });
        } else {
            dispatchChatMessage(text);
        }
    }

    private void dispatchChatMessage(String text) {
        isSending = true;
        ChatbotAPI api = ChatbotClient.get(requireContext()).create(ChatbotAPI.class);
        api.sendMessage(new ChatMessageRequest(currentSessionId, text)).enqueue(new Callback<ChatMessageResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChatMessageResponse> call,
                                   @NonNull Response<ChatMessageResponse> response) {
                if (!isAdded()) {
                    return;
                }
                isSending = false;
                hideTypingIndicator();

                if (response.isSuccessful() && response.body() != null) {
                    ChatMessageResponse body = response.body();
                    String answer = body.getAnswer();
                    if (answer == null || answer.trim().isEmpty()) {
                        answer = "Recebi sua mensagem, mas não obtive resposta formatada.";
                    }

                    addBot(answer, body.getRecommendedActions(), body.getCitations(), body.isRequiresHumanValidation());
                } else if (response.code() == 401) {
                    addBot("Sua sessão expirou. Por favor, faça login novamente no app para continuar conversando com a IA.",
                            null, null, false);
                } else {
                    addBot("Erro temporário ao processar sua dúvida (" + response.code() + "). Tente novamente em instantes.",
                            null, null, false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ChatMessageResponse> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                isSending = false;
                hideTypingIndicator();
                Log.e(TAG, "Falha na chamada de chat", t);
                addBot(getString(R.string.chatbot_error_network), null, null, false);
            }
        });
    }

    private void handleConnectionError() {
        messages.removeAllViews();
        addBot(getString(R.string.assistant_intro), null, null, false);
        addActionChips();
        Toast.makeText(requireContext(), R.string.chatbot_error_network, Toast.LENGTH_LONG).show();
    }

    private void showTypingIndicator() {
        if (typingIndicatorView != null) {
            return;
        }

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(6);
        rowParams.bottomMargin = dp(6);
        row.setLayoutParams(rowParams);

        ImageView mascot = new ImageView(requireContext());
        mascot.setImageResource(R.drawable.mascote_volta);
        LinearLayout.LayoutParams mascotParams = new LinearLayout.LayoutParams(dp(28), dp(32));
        mascotParams.rightMargin = dp(8);
        mascot.setLayoutParams(mascotParams);
        row.addView(mascot);

        TextView tvTyping = new TextView(requireContext());
        tvTyping.setText(getString(R.string.chatbot_thinking));
        tvTyping.setTextSize(13);
        tvTyping.setTextColor(requireContext().getColor(R.color.volta_green_dark));
        tvTyping.setBackgroundResource(R.drawable.bg_chat_in);
        tvTyping.setPadding(dp(12), dp(8), dp(12), dp(8));
        row.addView(tvTyping);

        typingIndicatorView = row;
        messages.addView(typingIndicatorView);
        scrollToBottom();
    }

    private void hideTypingIndicator() {
        if (typingIndicatorView != null) {
            messages.removeView(typingIndicatorView);
            typingIndicatorView = null;
        }
    }

    /**
     * Exibe mensagem do bot, incluindo citações, ações recomendadas e badge de validação humana.
     */
    private void addBot(String text, List<String> recommendedActions, List<Object> citations, boolean requiresHumanValidation) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.TOP);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(6);
        rowParams.bottomMargin = dp(6);
        row.setLayoutParams(rowParams);

        // Mascote avatar à esquerda
        ImageView mascot = new ImageView(requireContext());
        mascot.setImageResource(R.drawable.mascote_volta);
        LinearLayout.LayoutParams mascotParams = new LinearLayout.LayoutParams(dp(32), dp(36));
        mascotParams.rightMargin = dp(8);
        mascotParams.topMargin = dp(4);
        mascot.setLayoutParams(mascotParams);
        row.addView(mascot);

        // Coluna com balão + metadados + ações
        LinearLayout contentCol = new LinearLayout(requireContext());
        contentCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        contentCol.setLayoutParams(colParams);

        // Balão de texto principal
        contentCol.addView(bubble(text, false));

        // Badge de Validação Humana se aplicável
        if (requiresHumanValidation) {
            TextView tvValidation = new TextView(requireContext());
            tvValidation.setText(getString(R.string.chatbot_human_validation_badge));
            tvValidation.setTextSize(11);
            tvValidation.setTextColor(requireContext().getColor(R.color.priority_medium_text));
            tvValidation.setBackgroundResource(R.drawable.bg_pill_dark);
            tvValidation.setPadding(dp(8), dp(4), dp(8), dp(4));
            LinearLayout.LayoutParams valParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            valParams.topMargin = dp(4);
            tvValidation.setLayoutParams(valParams);
            contentCol.addView(tvValidation);
        }

        // Citações / Fontes se houver
        if (citations != null && !citations.isEmpty()) {
            TextView tvCitations = new TextView(requireContext());
            tvCitations.setText("📚 Fontes consultadas: " + citations.size() + " referências");
            tvCitations.setTextSize(11);
            tvCitations.setTextColor(requireContext().getColor(R.color.volta_text_muted_light));
            LinearLayout.LayoutParams citParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            citParams.topMargin = dp(3);
            tvCitations.setLayoutParams(citParams);
            contentCol.addView(tvCitations);
        }

        // Ações recomendadas retornadas pelo Chatbot (recommended_actions)
        if (recommendedActions != null && !recommendedActions.isEmpty()) {
            LinearLayout actionsRow = new LinearLayout(requireContext());
            actionsRow.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            actionsParams.topMargin = dp(6);
            actionsRow.setLayoutParams(actionsParams);

            for (String action : recommendedActions) {
                if (action != null && !action.trim().isEmpty()) {
                    TextView btnAction = chip("💡 " + action.trim());
                    btnAction.setOnClickListener(v -> ask(action.trim()));
                    actionsRow.addView(btnAction);
                }
            }
            contentCol.addView(actionsRow);
        }

        row.addView(contentCol);
        messages.addView(row);
        scrollToBottom();
    }

    private void addUser(String text) {
        messages.addView(bubble(text, true));
        scrollToBottom();
    }

    private TextView bubble(String text, boolean outgoing) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(15);
        int padH = dp(14);
        int padV = dp(10);
        tv.setPadding(padH, padV, padH, padV);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(4);
        params.bottomMargin = dp(4);
        if (outgoing) {
            params.gravity = android.view.Gravity.END;
            tv.setBackgroundResource(R.drawable.bg_button_gradient);
            tv.setTextColor(requireContext().getColor(R.color.white));
        } else {
            params.gravity = android.view.Gravity.START;
            tv.setBackgroundResource(R.drawable.bg_chat_in);
            tv.setTextColor(requireContext().getColor(R.color.volta_text_primary_light));
        }
        tv.setLayoutParams(params);
        tv.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.78));
        return tv;
    }

    /** Atalhos estáticos dentro da conversa: Registrar / Cooperativas */
    private void addActionChips() {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(4);
        params.bottomMargin = dp(6);
        row.setLayoutParams(params);

        TextView register = chip(getString(R.string.assistant_chip_register));
        register.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigate(R.id.nav_register));
        TextView coops = chip(getString(R.string.assistant_chip_coops));
        coops.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigate(R.id.nav_cooperatives));

        row.addView(register);
        row.addView(coops);
        messages.addView(row);
        scrollToBottom();
    }

    private TextView chip(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        tv.setTextColor(requireContext().getColor(R.color.volta_green_primary));
        tv.setBackgroundResource(R.drawable.bg_chip_unselected);
        int padH = dp(14);
        tv.setPadding(padH, dp(8), padH, dp(8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.rightMargin = dp(8);
        params.topMargin = dp(4);
        params.bottomMargin = dp(4);
        tv.setLayoutParams(params);
        return tv;
    }

    private void scrollToBottom() {
        if (scroll != null) {
            scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
