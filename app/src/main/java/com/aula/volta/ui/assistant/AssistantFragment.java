package com.aula.volta.ui.assistant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

        addBot(getString(R.string.assistant_intro));
        addActionChips();

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

    /** Atalhos dentro da conversa: Registrar / Cooperativas (navegam de verdade). */
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
        tv.setLayoutParams(params);
        return tv;
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
        row.setGravity(android.view.Gravity.TOP);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(6);
        rowParams.bottomMargin = dp(6);
        row.setLayoutParams(rowParams);

        // Mascote avatar à esquerda (Figma 790:1932)
        android.widget.ImageView mascot = new android.widget.ImageView(requireContext());
        mascot.setImageResource(R.drawable.mascote_volta);
        LinearLayout.LayoutParams mascotParams = new LinearLayout.LayoutParams(dp(32), dp(36));
        mascotParams.rightMargin = dp(8);
        mascotParams.topMargin = dp(4);
        mascot.setLayoutParams(mascotParams);
        row.addView(mascot);

        // Coluna com balão + ações contextuais
        LinearLayout contentCol = new LinearLayout(requireContext());
        contentCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        contentCol.setLayoutParams(colParams);

        contentCol.addView(bubble(text, false));

        // Se a resposta for sobre isopor, exibe ações contextuais (Figma 790:1940 e 790:1945)
        if (text != null && (text.contains("Isopor") || text.contains("EPS"))) {
            LinearLayout actionsRow = new LinearLayout(requireContext());
            actionsRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            actionsParams.topMargin = dp(6);
            actionsRow.setLayoutParams(actionsParams);

            TextView btnReg = chip(getString(R.string.assistant_chip_register));
            btnReg.setOnClickListener(v ->
                    Navigation.findNavController(requireView()).navigate(R.id.nav_register));

            TextView btnCoop = chip(getString(R.string.assistant_chip_coops));
            btnCoop.setOnClickListener(v ->
                    Navigation.findNavController(requireView()).navigate(R.id.nav_cooperatives));

            actionsRow.addView(btnReg);
            actionsRow.addView(btnCoop);
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
        tv.setPadding(padH, dp(10), padH, dp(10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(6);
        params.bottomMargin = dp(6);
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
        tv.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.75));
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
