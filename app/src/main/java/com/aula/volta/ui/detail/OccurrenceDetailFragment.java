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
    private TextView tvCardSubtitle;
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
        tvCardSubtitle = view.findViewById(R.id.tvOccCardSubtitle);

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
        tintChip(view, R.id.chipFinalize, R.id.iconFinalize,
                R.color.stat_resolved_bg, R.color.stat_resolved_icon);

        view.findViewById(R.id.rowEditClass).setOnClickListener(v -> soon(v));
        view.findViewById(R.id.rowForward).setOnClickListener(v -> soon(v));
        view.findViewById(R.id.rowObserve).setOnClickListener(v -> soon(v));
        view.findViewById(R.id.rowFinalize).setOnClickListener(v -> showFinalizeDialog(v));

        MaterialButton btnPick = view.findViewById(R.id.btnPickCoop);
        btnPick.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("occurrenceId", occurrenceId);
            Navigation.findNavController(v).navigate(R.id.nav_cooperative_pick, args);
        });

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
        tvCardTitle.setText(titulo);
        tvCardSubtitle.setText(setor);
        tvStatus.setText(prettyStatus(currentStatus));
        applyTracker(requireView(), doneSteps(currentStatus));
    }

    /** Tracker: NOVO=1, EM_ANALISE=2, demais=4 (Figma 790:2725). */
    private int doneSteps(String status) {
        if ("NOVO".equals(status)) {
            return 1;
        }
        if ("EM_ANALISE".equals(status)) {
            return 2;
        }
        return 4;
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
                    "ocorrencia", occurrenceId, currentStatus, "RESOLVIDO");
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
            Toast.makeText(requireContext(), R.string.occ_finalize_done,
                    Toast.LENGTH_LONG).show();
            dialog.dismiss();
            currentStatus = "RESOLVIDO";
            tvStatus.setText(prettyStatus(currentStatus));
            applyTracker(requireView(), doneSteps(currentStatus));
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
        if ("RESOLVIDO".equals(status)) {
            return "RESOLVIDO";
        }
        if ("NOVO".equals(status)) {
            return "NOVO";
        }
        return status;
    }
}
