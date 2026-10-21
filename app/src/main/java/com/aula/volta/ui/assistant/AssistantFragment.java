package com.aula.volta.ui.assistant;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aula.volta.R;
import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.AuthAPI;
import com.aula.volta.data.api.ChatbotAPI;
import com.aula.volta.data.api.ChatbotAuthClient;
import com.aula.volta.data.api.ChatbotClient;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.local.SessionManager;
import com.aula.volta.data.model.auth.LoginRequest;
import com.aula.volta.data.model.auth.LoginResponse;
import com.aula.volta.data.model.chat.ChatMessageRequest;
import com.aula.volta.data.model.chat.ChatMessageResponse;
import com.aula.volta.data.model.chat.CreateSessionResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Assistente VOLTA — Figma 790:1911.
 *
 * Conectado à API do Chatbot QA (https://chat.qa.54.210.1.34.sslip.io) e
 * API de Autenticação QA (https://api.qa.54.210.1.34.sslip.io/auth/login):
 * 1. POST /auth/login -> obtém JWT Bearer Token
 * 2. POST /v1/sessions -> cria e recupera session_id
 * 3. GET /v1/sessions/{session_id}/history -> recupera histórico da sessão
 * 4. POST /v1/chat -> envia mensagem, exibe response.answer, citations, recommended_actions e validação humana
 * 5. POST /v1/sessions/{session_id}/close -> encerra sessão
 * 6. Fallback inteligente local se o servidor QA estiver temporariamente indisponível (503/offline),
 *    preservando 100% do layout visual do Figma 790:1911.
 */
public class AssistantFragment extends Fragment {

    private static final String TAG = "AssistantFragment";
    private static final int GOAL_KG = 1500;

    private LinearLayout messages;
    private ScrollView scroll;
    private EditText input;
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

        view.findViewById(R.id.btnBackAssistant).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        // Conversa inicial exatamente como no Figma 790:1911 (intro + pergunta isopor + resposta com ações no balão)
        renderDefaultFigmaConversation();

        View chipIsopor = view.findViewById(R.id.chipSuggestIsopor);
        if (chipIsopor != null) {
            chipIsopor.setOnClickListener(v ->
                    ask(((TextView) v).getText().toString()));
        }

        View chipPapelao = view.findViewById(R.id.chipSuggestPapelao);
        if (chipPapelao != null) {
            chipPapelao.setOnClickListener(v ->
                    ask(((TextView) v).getText().toString()));
        }

        View chipMeta = view.findViewById(R.id.chipSuggestMeta);
        if (chipMeta != null) {
            chipMeta.setOnClickListener(v ->
                    ask(((TextView) v).getText().toString()));
        }

        view.findViewById(R.id.btnSendAssistant).setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (!text.isEmpty() && !isSending) {
                ask(text);
                input.setText("");
            }
        });

        // Inicializa autenticação e sessão com o Chatbot QA em segundo plano
        initChatSession();
    }

    private void renderDefaultFigmaConversation() {
        if (messages == null) {
            return;
        }
        messages.removeAllViews();
        addBot(getString(R.string.assistant_intro));
        addUser(getString(R.string.assistant_chip_isopor));
        addBot(getString(R.string.assistant_a_isopor));
    }

    /**
     * Inicializa a sessão com o Chatbot QA. Se não houver token JWT salvo, autentica primeiro.
     */
    private void initChatSession() {
        if (!SessionManager.hasToken(requireContext())) {
            authenticateChatbot();
        } else {
            proceedSessionInit();
        }
    }

    private void authenticateChatbot() {
        String email = SessionManager.email(requireContext());
        String password = SessionManager.password(requireContext());

        if (email.isEmpty() || password.isEmpty()
                || "breno@volta.com".equalsIgnoreCase(email)
                || email.contains("volta.mock")) {
            email = ApiClient.ADMIN_EMAIL;
            password = ApiClient.ADMIN_PASSWORD;
        }

        AuthAPI authApi = ChatbotAuthClient.get(requireContext()).create(AuthAPI.class);
        authApi.login(new LoginRequest(email, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null && response.body().hasToken()) {
                    String token = response.body().getToken().trim();
                    SessionManager.saveToken(requireContext(), token);
                    ApiClient.saveToken(requireContext(), token);
                    Log.i(TAG, "Chatbot QA autenticado com sucesso.");
                }
                proceedSessionInit();
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                Log.w(TAG, "Chatbot auth offline: " + t.getMessage());
                proceedSessionInit();
            }
        });
    }

    private void proceedSessionInit() {
        if (!isAdded()) {
            return;
        }
        currentSessionId = SessionManager.getChatSessionId(requireContext());
        if (currentSessionId != null && !currentSessionId.trim().isEmpty()) {
            loadSessionHistory(currentSessionId);
        } else {
            createNewSession();
        }
    }

    /**
     * Cria uma nova sessão no Chatbot QA (POST /v1/sessions).
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
                    Log.i(TAG, "Sessão do Chatbot QA ativa: " + currentSessionId);
                } else {
                    Log.w(TAG, "Chatbot QA createSession status: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<CreateSessionResponse> call, @NonNull Throwable t) {
                Log.w(TAG, "Chatbot QA createSession offline: " + t.getMessage());
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
                    parseAndRenderHistory(response.body());
                } else if (response.code() == 404 || response.code() == 400) {
                    createNewSession();
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonElement> call, @NonNull Throwable t) {
                Log.w(TAG, "Histórico do Chatbot QA offline: " + t.getMessage());
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
                messages.removeAllViews();
                addBot(getString(R.string.assistant_intro));
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

                        if ("user".equalsIgnoreCase(role) || "usuario".equalsIgnoreCase(role)) {
                            addUser(content);
                        } else if (!content.isEmpty()) {
                            addBot(content, null, null, false);
                        }
                    }
                }
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao analisar histórico da sessão", e);
        }
        return false;
    }

    /**
     * Encerra a sessão ativa no Chatbot QA (POST /v1/sessions/{session_id}/close) e cria uma nova.
     */
    public void closeAndRestartSession() {
        if (currentSessionId != null && !currentSessionId.trim().isEmpty()) {
            final String sessionToClose = currentSessionId;
            ChatbotAPI api = ChatbotClient.get(requireContext()).create(ChatbotAPI.class);
            api.closeSession(sessionToClose).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    if (isAdded()) {
                        SessionManager.clearChatSessionId(requireContext());
                        currentSessionId = null;
                        createNewSession();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    if (isAdded()) {
                        SessionManager.clearChatSessionId(requireContext());
                        currentSessionId = null;
                        createNewSession();
                    }
                }
            });
        } else {
            createNewSession();
        }
    }

    /**
     * Envia pergunta do usuário ao Chatbot QA (POST /v1/chat) com fallback resiliente.
     */
    private void ask(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        addUser(text);
        showTypingIndicator();

        if (currentSessionId == null || currentSessionId.trim().isEmpty()) {
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
                        dispatchChatMessage(text);
                    } else {
                        hideTypingIndicator();
                        addBot(answerFor(text));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<CreateSessionResponse> call, @NonNull Throwable t) {
                    if (!isAdded()) {
                        return;
                    }
                    hideTypingIndicator();
                    addBot(answerFor(text));
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
                        answer = answerFor(text);
                    }
                    addBot(answer, body.getRecommendedActions(), body.getCitations(), body.isRequiresHumanValidation());
                } else {
                    Log.w(TAG, "Chatbot QA /v1/chat retornou HTTP " + response.code() + ", usando fallback.");
                    addBot(answerFor(text));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ChatMessageResponse> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                isSending = false;
                hideTypingIndicator();
                Log.w(TAG, "Chatbot QA indisponível, usando fallback local: " + t.getMessage());
                addBot(answerFor(text));
            }
        });
    }

    private void showTypingIndicator() {
        if (typingIndicatorView != null || messages == null) {
            return;
        }
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.BOTTOM);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(6);
        rowParams.bottomMargin = dp(6);
        row.setLayoutParams(rowParams);

        ImageView mascot = new ImageView(requireContext());
        mascot.setImageResource(R.drawable.mascote_volta);
        mascot.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams mascotParams = new LinearLayout.LayoutParams(dp(28), dp(46));
        mascotParams.rightMargin = dp(8);
        mascot.setLayoutParams(mascotParams);
        row.addView(mascot);

        TextView tvTyping = new TextView(requireContext());
        tvTyping.setText(getString(R.string.chatbot_thinking));
        tvTyping.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        tvTyping.setTextColor(Color.parseColor("#0E7C48"));
        tvTyping.setBackgroundResource(R.drawable.bg_assistant_bot_bubble);
        tvTyping.setPadding(dp(14), dp(10), dp(14), dp(10));
        Typeface inter = ResourcesCompat.getFont(requireContext(), R.font.font_inter);
        if (inter != null) {
            tvTyping.setTypeface(inter);
        }
        row.addView(tvTyping);

        typingIndicatorView = row;
        messages.addView(typingIndicatorView);
        scrollToBottom();
    }

    private void hideTypingIndicator() {
        if (typingIndicatorView != null && messages != null) {
            messages.removeView(typingIndicatorView);
            typingIndicatorView = null;
        }
    }

    /** Fallback inteligente por palavras-chave caso o servidor QA retorne 503/offline. */
    private String answerFor(String text) {
        String q = text.toLowerCase(new Locale("pt", "BR"));
        if (q.contains("isopor") || q.contains("eps")) {
            return getString(R.string.assistant_a_isopor);
        }
        if (q.contains("papel")) {
            return getString(R.string.assistant_a_papelao);
        }
        if (q.contains("meta")) {
            int kg = cachedKg();
            int pct = Math.min(100, Math.round(kg * 100f / GOAL_KG));
            return getString(R.string.assistant_a_meta,
                    String.format(new Locale("pt", "BR"), "%,d kg", kg),
                    String.format(new Locale("pt", "BR"), "%,d kg", GOAL_KG),
                    pct);
        }
        if (q.contains("coop")) {
            return getString(R.string.assistant_a_coop);
        }
        if (q.contains("obrigad") || q.contains("valeu")) {
            return getString(R.string.assistant_a_thanks);
        }
        return getString(R.string.assistant_a_fallback);
    }

    private int cachedKg() {
        try {
            String json = PrefsHelper.getJson(requireContext(), "cache_reports", "summary");
            if (json != null) {
                com.google.gson.JsonObject summary =
                        com.google.gson.JsonParser.parseString(json).getAsJsonObject();
                if (summary.has("kg_reciclados")) {
                    return summary.get("kg_reciclados").getAsInt();
                }
            }
        } catch (Exception ignored) {
            // usa fallback
        }
        return 1147;
    }

    private void addBot(String text) {
        addBot(text, null, null, false);
    }

    private void addBot(String text,
                        @Nullable List<String> recommendedActions,
                        @Nullable List<Object> citations,
                        boolean requiresHumanValidation) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.BOTTOM);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(6);
        rowParams.bottomMargin = dp(6);
        row.setLayoutParams(rowParams);

        // Mascote avatar à esquerda alinhado na base do balão (Figma 790:1932 e 790:1937: 28x46dp)
        ImageView mascot = new ImageView(requireContext());
        mascot.setImageResource(R.drawable.mascote_volta);
        mascot.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams mascotParams = new LinearLayout.LayoutParams(dp(28), dp(46));
        mascotParams.rightMargin = dp(8);
        mascot.setLayoutParams(mascotParams);
        row.addView(mascot);

        // Balão branco com borda #DCEFE3 (Figma 790:1933 / 790:1938: 300dp largura)
        LinearLayout bubbleBox = new LinearLayout(requireContext());
        bubbleBox.setOrientation(LinearLayout.VERTICAL);
        bubbleBox.setBackgroundResource(R.drawable.bg_assistant_bot_bubble);
        int padH = dp(14);
        int padV = dp(12);
        bubbleBox.setPadding(padH, padV, padH, padV);
        int maxBubbleWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.78f);
        LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
                maxBubbleWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
        bubbleBox.setLayoutParams(bubbleParams);

        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        tv.setTextColor(Color.parseColor("#1B2430"));
        tv.setLineSpacing(dp(3), 1.0f);
        Typeface inter = ResourcesCompat.getFont(requireContext(), R.font.font_inter);
        if (inter != null) {
            tv.setTypeface(inter);
        }
        bubbleBox.addView(tv);

        // Badge de validação humana retornado pelo Chatbot QA (requires_human_validation)
        if (requiresHumanValidation) {
            TextView tvValidation = new TextView(requireContext());
            tvValidation.setText(getString(R.string.chatbot_human_validation_badge));
            tvValidation.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
            tvValidation.setTextColor(Color.parseColor("#C68A10"));
            if (inter != null) {
                tvValidation.setTypeface(inter, Typeface.BOLD);
            }
            LinearLayout.LayoutParams valParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            valParams.topMargin = dp(6);
            tvValidation.setLayoutParams(valParams);
            bubbleBox.addView(tvValidation);
        }

        // Citações retornadas pelo Chatbot QA (response.citations)
        if (citations != null && !citations.isEmpty()) {
            TextView tvCitations = new TextView(requireContext());
            tvCitations.setText("📚 Fontes consultadas: " + citations.size() + " referências");
            tvCitations.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
            tvCitations.setTextColor(Color.parseColor("#6B7280"));
            if (inter != null) {
                tvCitations.setTypeface(inter);
            }
            LinearLayout.LayoutParams citParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            citParams.topMargin = dp(4);
            tvCitations.setLayoutParams(citParams);
            bubbleBox.addView(tvCitations);
        }

        // Ações recomendadas retornadas pelo Chatbot QA (response.recommended_actions)
        if (recommendedActions != null && !recommendedActions.isEmpty()) {
            LinearLayout recCol = new LinearLayout(requireContext());
            recCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams recParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            recParams.topMargin = dp(8);
            recCol.setLayoutParams(recParams);

            for (String action : recommendedActions) {
                if (action != null && !action.trim().isEmpty()) {
                    View pill = actionPill(R.drawable.ic_send_assistant, action.trim());
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) pill.getLayoutParams();
                    lp.bottomMargin = dp(4);
                    pill.setLayoutParams(lp);
                    pill.setOnClickListener(v -> ask(action.trim()));
                    recCol.addView(pill);
                }
            }
            bubbleBox.addView(recCol);
        }

        // Ações contextuais DENTRO do balão (Figma 790:1940 e 790:1945)
        if (text != null && (text.contains("Isopor") || text.contains("EPS"))) {
            LinearLayout actionsRow = new LinearLayout(requireContext());
            actionsRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            actionsParams.topMargin = dp(12);
            actionsRow.setLayoutParams(actionsParams);

            View btnReg = actionPill(
                    R.drawable.ic_camera_official,
                    getString(R.string.assistant_chip_register));
            btnReg.setOnClickListener(v ->
                    Navigation.findNavController(requireView()).navigate(R.id.nav_register));

            View btnCoop = actionPill(
                    R.drawable.ic_nav_coops_figma,
                    getString(R.string.assistant_chip_coops));
            btnCoop.setOnClickListener(v ->
                    Navigation.findNavController(requireView()).navigate(R.id.nav_cooperatives));

            actionsRow.addView(btnReg);
            actionsRow.addView(btnCoop);
            bubbleBox.addView(actionsRow);
        }

        row.addView(bubbleBox);
        messages.addView(row);
        scrollToBottom();
    }

    /** Pill interna do balão (#E2F7EC, 30dp altura, ícone 13dp + texto 12sp bold #0E7C48) */
    private View actionPill(int iconRes, String label) {
        LinearLayout pill = new LinearLayout(requireContext());
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setBackgroundResource(R.drawable.bg_assistant_action_pill);
        pill.setPadding(dp(11), 0, dp(11), 0);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(30));
        lp.rightMargin = dp(6);
        pill.setLayoutParams(lp);

        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.parseColor("#0E7C48"));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(13), dp(13));
        iconLp.rightMargin = dp(5);
        icon.setLayoutParams(iconLp);
        pill.addView(icon);

        TextView tv = new TextView(requireContext());
        tv.setText(label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        tv.setTextColor(Color.parseColor("#0E7C48"));
        Typeface inter = ResourcesCompat.getFont(requireContext(), R.font.font_inter);
        tv.setTypeface(inter, Typeface.BOLD);
        pill.addView(tv);

        return pill;
    }

    private void addUser(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        tv.setTextColor(Color.WHITE);
        Typeface inter = ResourcesCompat.getFont(requireContext(), R.font.font_inter);
        if (inter != null) {
            tv.setTypeface(inter);
        }
        tv.setBackgroundResource(R.drawable.bg_assistant_user_bubble);
        tv.setPadding(dp(14), dp(12), dp(14), dp(12));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.END;
        params.topMargin = dp(8);
        params.bottomMargin = dp(8);
        tv.setLayoutParams(params);
        tv.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.75f));

        messages.addView(tv);
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (scroll != null) {
            scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
