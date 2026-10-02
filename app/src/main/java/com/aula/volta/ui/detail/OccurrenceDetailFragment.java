package com.aula.volta.ui.detail;

import android.app.AlertDialog;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aula.volta.R;
import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.OccurrenceAPI;
import com.aula.volta.data.local.AuditLog;
import com.aula.volta.data.local.NotificationStore;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.model.Notification;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Tela cheia da Ocorrência — Figma (790:2725): tracker, foto, ações do responsável,
 * finalizar (diálogo + toast) e "Escolher cooperativa para coleta".
 * Arg: occurrenceId.
 */
public class OccurrenceDetailFragment extends Fragment {

    private static final String PREFS = "cache_occurrence_detail";

    private TextView tvTitle;
    private TextView tvSubtitle;
    private TextView tvStatus;
    private TextView tvCardTitle;
    private TextView tvCardLine;
    private TextView tvCardSubtitle;
    private TextView tvPriority;
    private TextView tvAutoReport;
    private TextView tvWeight;
    private TextView tvAuthor;
    private TextView tvDate;
    private TextView tvConfidence;
    private View toastFinalized;
    private TextView btnToastOpenPgrs;
    private MaterialButton btnChooseCoop;
    private String occurrenceId;
    private String currentStatus;

    public OccurrenceDetailFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_occurrence_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTitle = view.findViewById(R.id.tvOccTitle);
        tvSubtitle = view.findViewById(R.id.tvOccSubtitle);
        tvStatus = view.findViewById(R.id.tvOccStatus);
        tvCardTitle = view.findViewById(R.id.tvOccCardTitle);
        tvCardLine = view.findViewById(R.id.tvOccCardLine);
        tvCardSubtitle = view.findViewById(R.id.tvOccCardSubtitle);
        tvPriority = view.findViewById(R.id.tvOccPriority);
        tvAutoReport = view.findViewById(R.id.tvOccAutoReport);
        tvWeight = view.findViewById(R.id.tvOccWeight);
        tvAuthor = view.findViewById(R.id.tvOccAuthor);
        tvDate = view.findViewById(R.id.tvOccDate);
        tvConfidence = view.findViewById(R.id.tvOccConfidence);
        toastFinalized = view.findViewById(R.id.toastFinalized);
        btnToastOpenPgrs = view.findViewById(R.id.btnToastOpenPgrs);
        btnChooseCoop = view.findViewById(R.id.btnChooseCoop);

        occurrenceId = getArguments() != null ? getArguments().getString("occurrenceId") : null;
        if (occurrenceId == null || occurrenceId.isEmpty()) {
            Navigation.findNavController(view).navigateUp();
            return;
        }

        view.findViewById(R.id.btnBackOcc).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        tintChip(view, R.id.chipEditClass, R.id.iconEditClass,
                R.color.occurrence_icon_bg, R.color.occurrence_icon_tint);
        tintChip(view, R.id.chipForward, R.id.iconForward,
                R.color.occurrence_icon_bg_blue, R.color.occurrence_icon_tint_blue);
        tintChip(view, R.id.chipObserve, R.id.iconObserve,
                R.color.occurrence_icon_bg_purple, R.color.occurrence_icon_tint_purple);

        view.findViewById(R.id.rowEditClass).setOnClickListener(v -> soon(v));
        view.findViewById(R.id.rowForward).setOnClickListener(v -> soon(v));
        view.findViewById(R.id.rowObserve).setOnClickListener(v -> soon(v));

        MaterialButton btnFinalize = view.findViewById(R.id.btnFinalizeOcc);
        btnFinalize.setOnClickListener(v -> showFinalizeDialog(v));

        if (btnChooseCoop != null) {
            btnChooseCoop.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("occurrenceId", occurrenceId);
                Navigation.findNavController(v).navigate(R.id.nav_cooperative_pick, args);
            });
        }

        View.OnClickListener openPgrsAction = v -> {
            try {
                java.io.File report = com.aula.volta.data.report.PgrsReport.generate(requireContext());
                if (report != null) {
                    com.aula.volta.data.report.PgrsReport.share(requireContext(), report);
                } else {
                    Toast.makeText(requireContext(), R.string.reports_nothing, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(requireContext(), R.string.reports_nothing, Toast.LENGTH_SHORT).show();
            }
        };
        if (btnToastOpenPgrs != null) {
            btnToastOpenPgrs.setOnClickListener(openPgrsAction);
        }
        if (toastFinalized != null) {
            toastFinalized.setOnClickListener(openPgrsAction);
        }

        loadDetail();
    }

    private void soon(View v) {
        Toast.makeText(v.getContext(), R.string.occ_soon, Toast.LENGTH_SHORT).show();
    }

    private void tintChip(View root, int chipId, int iconId,
                          @ColorRes int chipColorRes, @ColorRes int iconColorRes) {
        FrameLayout chip = root.findViewById(chipId);
        ImageView icon = root.findViewById(iconId);
        chip.getBackground().mutate().setTint(requireContext().getColor(chipColorRes));
        icon.setColorFilter(new PorterDuffColorFilter(
                requireContext().getColor(iconColorRes), PorterDuff.Mode.SRC_IN));
    }

    private void loadDetail() {
        OccurrenceAPI api = ApiClient.get(requireContext()).create(OccurrenceAPI.class);
        api.getOccurrence(occurrenceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject detail = response.body();
                    PrefsHelper.putJson(requireContext(), PREFS, occurrenceId, detail.toString());
                    applyDetail(detail);
                } else {
                    loadFromCache();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                loadFromCache();
                Toast.makeText(requireContext(), R.string.offline_cache, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadFromCache() {
        if (!isAdded()) {
            return;
        }
        String json = PrefsHelper.getJson(requireContext(), PREFS, occurrenceId);
        if (json != null) {
            try {
                applyDetail(com.google.gson.JsonParser.parseString(json).getAsJsonObject());
            } catch (Exception ignored) {
                Navigation.findNavController(requireView()).navigateUp();
            }
        } else {
            Toast.makeText(requireContext(), R.string.error_load, Toast.LENGTH_LONG).show();
            Navigation.findNavController(requireView()).navigateUp();
        }
    }

    private void applyDetail(JsonObject detail) {
        String titulo = opt(detail, "titulo");
        String setor = opt(detail, "setor");
        currentStatus = opt(detail, "status");

        tvTitle.setText(titulo.isEmpty() ? getString(R.string.occ_title) : titulo);
        tvSubtitle.setText(setor);

        String material = opt(detail, "material");
        String classe = opt(detail, "classe");
        if (!material.isEmpty() && !classe.isEmpty()) {
            tvCardTitle.setText(material + " (" + classe + ")");
        } else {
            tvCardTitle.setText(titulo);
        }
        String cont = capitalize(opt(detail, "contaminacao_nivel"));
        String peso = opt(detail, "peso_estimado");
        String unidade = opt(detail, "unidade");
        String line = "";
        if (!cont.isEmpty()) {
            line = "Contaminação " + cont.toLowerCase(new java.util.Locale("pt", "BR"));
        }
        if (!peso.isEmpty()) {
            if (!line.isEmpty()) {
                line += " · ";
            }
            line += "≈" + peso + (unidade.isEmpty() ? "" : " " + unidade);
        }
        tvCardLine.setText(line);
        tvCardSubtitle.setText(setor);
        applyPriority(opt(detail, "prioridade"));
        setText(tvAutoReport, opt(detail, "relatorio_automatico"));

        tvStatus.setText(prettyStatus(currentStatus));
        applyTracker(requireView(), doneSteps(currentStatus));
        syncFinalizedUi();

        tvWeight.setText(peso.isEmpty() ? "-" : "≈" + peso + (unidade.isEmpty() ? "" : " " + unidade));
        setText(tvAuthor, opt(detail, "registrado_por"));
        setText(tvDate, opt(detail, "data"));
        String confianca = opt(detail, "confianca_ia");
        tvConfidence.setText(confianca.isEmpty() ? "-" : confianca + "%");

        renderHistory(detail);
    }

    private void setText(TextView tv, String text) {
        if (tv != null) {
            tv.setText(text);
        }
    }

    /** Linha do tempo simples a partir de "historico": [{acao, usuario, quando}]. */
    private void renderHistory(JsonObject detail) {
        if (!isAdded() || getView() == null) {
            return;
        }
        android.widget.LinearLayout container = getView().findViewById(R.id.historyContainer);
        android.view.View empty = getView().findViewById(R.id.tvHistoryEmpty);
        if (container == null) {
            return;
        }
        // Remove linhas anteriores (preserva o empty para reutilizar).
        for (int i = container.getChildCount() - 1; i >= 0; i--) {
            android.view.View child = container.getChildAt(i);
            if (child.getId() != R.id.tvHistoryEmpty) {
                container.removeViewAt(i);
            }
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        List<View> timelineViews = new java.util.ArrayList<>();

        // 1. Eventos registrados localmente via AuditLog
        List<com.aula.volta.data.model.OccurrenceHistory> localHistory =
                AuditLog.list(requireContext(), occurrenceId);
        if (localHistory != null) {
            for (com.aula.volta.data.model.OccurrenceHistory item : localHistory) {
                View itemView = buildTimelineRow(inflater, container,
                        item.getAcao(), item.getUsuario(), item.getTimestamp(),
                        item.getValorAnterior(), item.getValorNovo());
                if (itemView != null) {
                    timelineViews.add(itemView);
                }
            }
        }

        // 2. Eventos originais vindos do backend/mock
        try {
            if (detail.has("historico") && detail.get("historico").isJsonArray()) {
                for (com.google.gson.JsonElement el : detail.getAsJsonArray("historico")) {
                    if (!el.isJsonObject()) {
                        continue;
                    }
                    JsonObject h = el.getAsJsonObject();
                    View itemView = buildTimelineRow(inflater, container,
                            opt(h, "acao"), opt(h, "usuario"), opt(h, "quando"),
                            opt(h, "de"), opt(h, "para"));
                    if (itemView != null) {
                        timelineViews.add(itemView);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // Adiciona à UI e remove linha final do último item
        for (int i = 0; i < timelineViews.size(); i++) {
            View row = timelineViews.get(i);
            if (i == timelineViews.size() - 1) {
                View line = row.findViewById(R.id.timelineLine);
                if (line != null) {
                    line.setVisibility(View.GONE);
                }
            }
            container.addView(row);
        }

        if (empty != null) {
            empty.setVisibility(timelineViews.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private View buildTimelineRow(LayoutInflater inflater, ViewGroup parent,
                                  String acao, String usuario, String quando,
                                  String de, String para) {
        if (acao == null || acao.isEmpty()) {
            return null;
        }
        View view = inflater.inflate(R.layout.item_history_timeline, parent, false);
        TextView tvTitle = view.findViewById(R.id.tvTimelineTitle);
        TextView tvSub = view.findViewById(R.id.tvTimelineSub);
        TextView tvTime = view.findViewById(R.id.tvTimelineTime);
        View dot = view.findViewById(R.id.timelineDot);

        tvTitle.setText(prettyAction(acao));
        tvTime.setText(quando != null ? quando : "");

        StringBuilder sub = new StringBuilder();
        if (usuario != null && !usuario.isEmpty()) {
            sub.append("Por ").append(usuario);
        }
        if (de != null && !de.isEmpty() && para != null && !para.isEmpty()) {
            if (sub.length() > 0) {
                sub.append(" · ");
            }
            sub.append(de).append(" → ").append(para);
        }
        tvSub.setText(sub.toString());
        tvSub.setVisibility(sub.length() > 0 ? View.VISIBLE : View.GONE);

        // Estilo e cor do ponto na timeline
        if ("FINALIZAR".equals(acao) || "APROVADA".equals(acao) || "SYNC_ONLINE".equals(acao)) {
            dot.setBackgroundResource(R.drawable.bg_dot_circle_green);
        } else if ("IA_ANALISOU".equals(acao) || "SOLICITAR_DESTINACAO".equals(acao)) {
            dot.setBackgroundResource(R.drawable.bg_dot_circle_blue);
        } else {
            dot.setBackgroundResource(R.drawable.bg_dot_circle_orange);
        }

        return view;
    }

    private String prettyAction(String action) {
        if ("CRIAR".equals(action)) {
            return "Ocorrência registrada";
        }
        if ("CRIAR_OFFLINE".equals(action)) {
            return "Registrada offline (salvo local)";
        }
        if ("SYNC_ONLINE".equals(action)) {
            return "Sincronizada com a nuvem";
        }
        if ("IA_ANALISOU".equals(action)) {
            return "Classificação assistida pela IA";
        }
        if ("FINALIZAR".equals(action)) {
            return "Aprovada e enviada ao PGRS";
        }
        if ("SOLICITAR_DESTINACAO".equals(action)) {
            return "Destinação para cooperativa solicitada";
        }
        return action;
    }

    /** Tracker: NOVO=1, EM_ANALISE=2, demais=4 (protótipo novo 851). */
    private int doneSteps(String status) {
        if ("NOVO".equals(status)) {
            return 1;
        }
        if ("EM_ANALISE".equals(status)) {
            return 2;
        }
        return 4;
    }

    /** Badge de prioridade do card (ALTA/MÉDIA/BAIXA, cores do Figma). */
    private void applyPriority(String priority) {
        if (tvPriority == null) {
            return;
        }
        if ("ALTA".equals(priority)) {
            tvPriority.setText(R.string.priority_alta);
            tvPriority.setBackgroundResource(R.drawable.bg_badge_alta);
            tvPriority.setTextColor(requireContext().getColor(R.color.priority_high_text));
            tvPriority.setVisibility(View.VISIBLE);
        } else if ("MEDIA".equals(priority)) {
            tvPriority.setText(R.string.priority_media);
            tvPriority.setBackgroundResource(R.drawable.bg_badge_media);
            tvPriority.setTextColor(requireContext().getColor(R.color.priority_medium_text));
            tvPriority.setVisibility(View.VISIBLE);
        } else if ("BAIXA".equals(priority)) {
            tvPriority.setText(R.string.priority_baixa);
            tvPriority.setBackgroundResource(R.drawable.bg_badge_baixa);
            tvPriority.setTextColor(requireContext().getColor(R.color.priority_low_text));
            tvPriority.setVisibility(View.VISIBLE);
        } else {
            tvPriority.setVisibility(View.GONE);
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return s.substring(0, 1).toUpperCase(new java.util.Locale("pt", "BR")) + s.substring(1);
    }

    /** Estado finalizada (851:4403): só Observação resta, sem botões de ação, banner PGRS visível. */
    private void syncFinalizedUi() {
        boolean done = isFinalized(currentStatus);
        View root = getView();
        if (root == null) {
            return;
        }
        View rowEdit = root.findViewById(R.id.rowEditClass);
        View rowForward = root.findViewById(R.id.rowForward);
        View btnFinalize = root.findViewById(R.id.btnFinalizeOcc);
        View btnCoop = root.findViewById(R.id.btnChooseCoop);
        View toast = root.findViewById(R.id.toastFinalized);
        if (rowEdit != null) {
            rowEdit.setVisibility(done ? View.GONE : View.VISIBLE);
        }
        if (rowForward != null) {
            rowForward.setVisibility(done ? View.GONE : View.VISIBLE);
        }
        if (btnFinalize != null) {
            btnFinalize.setVisibility(done ? View.GONE : View.VISIBLE);
        }
        if (btnCoop != null) {
            btnCoop.setVisibility(done ? View.GONE : View.VISIBLE);
        }
        if (toast != null) {
            toast.setVisibility(done ? View.VISIBLE : View.GONE);
        }
        if (tvStatus != null) {
            if (done) {
                tvStatus.setText(R.string.status_aprovada);
                tvStatus.setBackgroundResource(R.drawable.bg_badge_aprovada);
                tvStatus.setTextColor(requireContext().getColor(R.color.status_aprovada_text));
            } else {
                tvStatus.setText(prettyStatus(currentStatus));
                if ("NOVO".equals(currentStatus)) {
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_alta);
                    tvStatus.setTextColor(requireContext().getColor(R.color.priority_high_text));
                } else {
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_media);
                    tvStatus.setTextColor(requireContext().getColor(R.color.priority_medium_text));
                }
            }
        }
    }

    private boolean isFinalized(String status) {
        return "RESOLVIDO".equals(status) || "APROVADA".equals(status);
    }

    private void applyTracker(View root, int done) {
        int[] circles = {R.id.stepCircle0, R.id.stepCircle1, R.id.stepCircle2, R.id.stepCircle3};
        int[] checks = {R.id.stepCheck0, R.id.stepCheck1, R.id.stepCheck2, R.id.stepCheck3};
        int[] labels = {R.id.stepLabel0, R.id.stepLabel1, R.id.stepLabel2, R.id.stepLabel3};
        int[] lines = {R.id.stepLine0, R.id.stepLine1, R.id.stepLine2};
        int green = requireContext().getColor(R.color.nav_item_selected);
        int grayBg = requireContext().getColor(R.color.notification_circle_border);
        int greenText = requireContext().getColor(R.color.volta_green_deep);
        int grayText = requireContext().getColor(R.color.volta_text_secondary_light);
        for (int i = 0; i < circles.length; i++) {
            boolean stepDone = i < done;
            root.findViewById(circles[i]).setBackgroundResource(
                    stepDone ? R.drawable.bg_step_done : R.drawable.bg_notification_circle);
            root.findViewById(checks[i]).setVisibility(stepDone ? View.VISIBLE : View.GONE);
            ((TextView) root.findViewById(labels[i]))
                    .setTextColor(stepDone ? greenText : grayText);
        }
        for (int i = 0; i < lines.length; i++) {
            root.findViewById(lines[i]).setBackgroundColor(i < done - 1 ? green : grayBg);
        }
    }

    private void showFinalizeDialog(View v) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_finalize, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialogView.findViewById(R.id.btnFinalizeYes).setOnClickListener(b -> {
            AuditLog.append(requireContext(), "Breno Gomes", "FINALIZAR",
                    "ocorrencia", occurrenceId, currentStatus, "APROVADA");
            NotificationStore.pushLocal(requireContext(), new Notification(
                    "local_" + System.currentTimeMillis(),
                    "OCORRENCIA_FINALIZADA",
                    getString(R.string.notif_finalize_title, occurrenceId),
                    getString(R.string.notif_finalize_desc),
                    getString(R.string.notif_now),
                    "check",
                    "#12A05E",
                    "#E2F7EC",
                    false,
                    occurrenceId));
            com.aula.volta.data.notification.NotificationHelper.notifyPgrsFinalized(requireContext(), occurrenceId);
            dialog.dismiss();
            currentStatus = "APROVADA";
            try {
                String cached = PrefsHelper.getJson(requireContext(), PREFS, occurrenceId);
                if (cached != null) {
                    JsonObject obj = com.google.gson.JsonParser.parseString(cached).getAsJsonObject();
                    obj.addProperty("status", "APROVADA");
                    PrefsHelper.putJson(requireContext(), PREFS, occurrenceId, obj.toString());
                }
            } catch (Exception ignored) {
            }
            if (getView() != null) {
                applyTracker(getView(), doneSteps(currentStatus));
            }
            syncFinalizedUi();
            if (toastFinalized != null) {
                toastFinalized.setAlpha(0f);
                toastFinalized.setVisibility(View.VISIBLE);
                toastFinalized.animate().alpha(1f).setDuration(350).start();
            }
        });
        dialogView.findViewById(R.id.btnFinalizeNo).setOnClickListener(b -> dialog.dismiss());
        dialog.show();
    }

    private String opt(JsonObject detail, String member) {
        return detail.has(member) && !detail.get(member).isJsonNull()
                ? detail.get(member).getAsString() : "";
    }

    private String prettyStatus(String status) {
        if ("EM_ANALISE".equals(status)) {
            return "EM ANÁLISE";
        }
        if ("RESOLVIDO".equals(status) || "APROVADA".equals(status)) {
            return "APROVADA";
        }
        if ("NOVO".equals(status)) {
            return "NOVO";
        }
        return status;
    }
}
