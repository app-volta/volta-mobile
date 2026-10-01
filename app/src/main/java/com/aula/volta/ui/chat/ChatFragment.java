package com.aula.volta.ui.chat;

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

/**
 * Chat com a cooperativa — Figma 790:3191 (Fase 11).
 * Mock local: conversa inicial + respostas rápidas + envio.
 * Backend de mensagens pluga aqui sem mudar o layout.
 * Arg: coopName.
 */
public class ChatFragment extends Fragment {

    private LinearLayout messagesContainer;
    private ScrollView scrollChat;
    private EditText etMessage;
    private String coopName = "";

    public ChatFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null && getArguments().getString("coopName") != null) {
            coopName = getArguments().getString("coopName");
        }

        ((TextView) view.findViewById(R.id.tvChatName)).setText(
                coopName.isEmpty() ? getString(R.string.nav_coops) : coopName);
        ((TextView) view.findViewById(R.id.tvChatAvatar)).setText(initials(coopName));
        ((TextView) view.findViewById(R.id.tvPickupSub)).setText(
                getString(R.string.chat_pickup_sub,
                        coopName.isEmpty() ? getString(R.string.nav_coops) : coopName));

        messagesContainer = view.findViewById(R.id.messagesContainer);
        scrollChat = view.findViewById(R.id.scrollChat);
        etMessage = view.findViewById(R.id.etMessage);

        view.findViewById(R.id.btnBackChat).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        // Conversa inicial (mock, Figma 790:3191).
        addIncoming(getString(R.string.chat_msg_1), getString(R.string.chat_msg_1_time));
        addOutgoing(getString(R.string.chat_msg_2), getString(R.string.chat_msg_2_time));
        addIncoming(getString(R.string.chat_msg_3), getString(R.string.chat_msg_3_time));

        view.findViewById(R.id.chipQuick1).setOnClickListener(v ->
                send(((TextView) v).getText().toString()));
        view.findViewById(R.id.chipQuick2).setOnClickListener(v ->
                send(((TextView) v).getText().toString()));
        view.findViewById(R.id.chipQuick3).setOnClickListener(v ->
                send(((TextView) v).getText().toString()));

        view.findViewById(R.id.btnSend).setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                send(text);
                etMessage.setText("");
            }
        });
    }

    private void send(String text) {
        addOutgoing(text, getString(R.string.chat_now));
    }

    private void addIncoming(String text, String time) {
        LinearLayout col = bubbleColumn(false);
        col.addView(bubble(text, false));
        col.addView(stamp(time, false));
        messagesContainer.addView(col);
        scrollToBottom();
    }

    private void addOutgoing(String text, String time) {
        LinearLayout col = bubbleColumn(true);
        col.addView(bubble(text, true));
        col.addView(stamp(time, true));
        messagesContainer.addView(col);
        scrollToBottom();
    }

    private LinearLayout bubbleColumn(boolean outgoing) {
        LinearLayout col = new LinearLayout(requireContext());
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(6);
        params.bottomMargin = dp(6);
        if (outgoing) {
            params.gravity = android.view.Gravity.END;
        } else {
            params.gravity = android.view.Gravity.START;
        }
        col.setLayoutParams(params);
        return col;
    }

    private TextView bubble(String text, boolean outgoing) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(15);
        int padH = dp(14);
        int padV = dp(10);
        tv.setPadding(padH, padV, padH, padV);
        if (outgoing) {
            tv.setBackgroundResource(R.drawable.bg_button_gradient);
            tv.setTextColor(requireContext().getColor(R.color.white));
        } else {
            tv.setBackgroundResource(R.drawable.bg_chat_in);
            tv.setTextColor(requireContext().getColor(R.color.volta_text_primary_light));
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.width = LinearLayout.LayoutParams.WRAP_CONTENT;
        tv.setLayoutParams(params);
        tv.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.75));
        return tv;
    }

    private TextView stamp(String time, boolean outgoing) {
        TextView tv = new TextView(requireContext());
        tv.setText(time);
        tv.setTextSize(11);
        tv.setTextColor(requireContext().getColor(R.color.volta_text_muted_light));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        if (outgoing) {
            params.gravity = android.view.Gravity.END;
        }
        params.topMargin = dp(2);
        tv.setLayoutParams(params);
        return tv;
    }

    private void scrollToBottom() {
        if (scrollChat != null) {
            scrollChat.post(() -> scrollChat.fullScroll(View.FOCUS_DOWN));
        }
    }

    private String initials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            if (!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
            }
        }
        return sb.toString();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
