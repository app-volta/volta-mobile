package com.aula.volta.ui.assistant;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
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
import com.aula.volta.data.local.PrefsHelper;

import java.util.Locale;

/**
 * Assistente VOLTA — Figma 790:1911 (Fase 12).
 * Mock local por palavras-chave; atalhos navegam de verdade.
 * Backend de IA pluga em {@link #answerFor(String)} sem mudar o layout.
 */
public class AssistantFragment extends Fragment {

    private static final int GOAL_KG = 1500;

    private LinearLayout messages;
    private ScrollView scroll;
    private EditText input;

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
        addBot(getString(R.string.assistant_intro));
        addUser(getString(R.string.assistant_chip_isopor));
        addBot(getString(R.string.assistant_a_isopor));

        View chipIsopor = view.findViewById(R.id.chipSuggestIsopor);
        if (chipIsopor != null) {
            chipIsopor.setOnClickListener(v ->
                    ask(((TextView) v).getText().toString()));
        }

        view.findViewById(R.id.chipSuggestPapelao).setOnClickListener(v ->
                ask(((TextView) v).getText().toString()));
        view.findViewById(R.id.chipSuggestMeta).setOnClickListener(v ->
                ask(((TextView) v).getText().toString()));
        view.findViewById(R.id.btnSendAssistant).setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (!text.isEmpty()) {
                ask(text);
                input.setText("");
            }
        });
    }

    private void ask(String text) {
        addUser(text);
        addBot(answerFor(text));
    }

    /** Mock por palavras-chave (Fase 12). Trocar pelo backend sem tocar na UI. */
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
                    R.drawable.ic_nav_coops,
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
