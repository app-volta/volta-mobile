package com.aula.volta;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.ReportAPI;
import com.aula.volta.data.local.SessionManager;
import java.io.File;
import com.aula.volta.data.model.EsgMetricResponseDTO;
import com.aula.volta.data.local.NotificationStore;
import com.aula.volta.data.local.OccurrenceStore;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.model.Occurrence;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final String PREFS_REPORTS = "cache_reports";
    private static final String KEY_SUMMARY = "summary";

    private TextView tvStatOpenCount;
    private TextView tvStatResolvedCount;
    private TextView tvStatRecycledCount;
    private ProgressBar loadingHome;
    private TextView tvEmptyOccurrences;
    private TextView tvNotifCount;
    private OccurrenceAdapter adapter;

    /** Contador das 3 cargas (resumo, ocorrências, notificações) para esconder o loading. */
    private int pendingLoads = 3;

    public HomeFragment() {
        // Construtor vazio obrigatório
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvStatOpenCount = view.findViewById(R.id.tvStatOpenCount);
        tvStatResolvedCount = view.findViewById(R.id.tvStatResolvedCount);
        tvStatRecycledCount = view.findViewById(R.id.tvStatRecycledCount);
        tvNotifCount = view.findViewById(R.id.tvNotifCount);

        View avatarContainer = view.findViewById(R.id.avatarContainer);
        if (avatarContainer != null) {
            avatarContainer.setOnClickListener(v ->
                    Navigation.findNavController(v).navigate(R.id.nav_profile));
        }
        loadAvatarPhoto();

        // _________________________________________________________________________________________
        //            AÇÕES DA HOME (navegação interna, sem item na bottom bar)
        // _________________________________________________________________________________________

        // Carrossel da Home (Figma 790:1648): slide CTA + slide Últimos 7 dias (com peek lateral)
        ViewPager2 vpCarousel = view.findViewById(R.id.vpCarousel);
        LinearLayout carouselDots = view.findViewById(R.id.carouselDots);
        if (vpCarousel != null) {
            vpCarousel.setClipToPadding(false);
            vpCarousel.setClipChildren(false);
            vpCarousel.setOffscreenPageLimit(1);
            View innerChild = vpCarousel.getChildAt(0);
            if (innerChild instanceof RecyclerView) {
                RecyclerView rv = (RecyclerView) innerChild;
                rv.setClipToPadding(false);
                rv.setClipChildren(false);
                rv.setOverScrollMode(View.OVER_SCROLL_NEVER);
            }

            CarouselAdapter carouselAdapter = new CarouselAdapter(() ->
                    Navigation.findNavController(view).navigate(R.id.nav_register));
            vpCarousel.setAdapter(carouselAdapter);
            if (carouselDots != null) {
                setupDots(carouselDots, carouselAdapter.getItemCount(), 0);
                vpCarousel.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        setupDots(carouselDots, carouselAdapter.getItemCount(), position);
                    }
                });
            }
        }

        // Link "Ver todas" para navegar para a aba de Ocorrências
        TextView tvSeeAll = view.findViewById(R.id.tvSeeAllOccurrences);
        if (tvSeeAll != null) {
            tvSeeAll.setOnClickListener(v -> {
                Navigation.findNavController(v).navigate(R.id.nav_occurrences);
            });
        }

        // Sino abre as notificações
        View btnNotifications = view.findViewById(R.id.btnNotifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                Navigation.findNavController(v).navigate(R.id.nav_notifications);
            });
        }

        // Mascote VOLTA Flutuante + Balão (Figma 790:1732 e 790:1734) -> abre o assistente
        View fabMascot = view.findViewById(R.id.fabMascot);
        View balloonMascot = view.findViewById(R.id.balloonMascot);
        View.OnClickListener openAssistant = v ->
                Navigation.findNavController(v).navigate(R.id.nav_assistant);
        if (fabMascot != null) {
            fabMascot.setOnClickListener(openAssistant);
        }
        if (balloonMascot != null) {
            balloonMascot.setOnClickListener(openAssistant);
        }

        // _________________________________________________________________________________________
        //            LISTA DINÂMICA (Fase 2 — mesmo visual Figma via adapter)
        // _________________________________________________________________________________________

        RecyclerView rvRecent = view.findViewById(R.id.rvRecentOccurrences);
        loadingHome = view.findViewById(R.id.loadingHome);
        tvEmptyOccurrences = view.findViewById(R.id.tvEmptyOccurrences);

        adapter = new OccurrenceAdapter(occurrence -> {
            Bundle args = new Bundle();
            args.putString("occurrenceId", occurrence.getId());
            if (occurrence.getPhotoUrl() != null && !occurrence.getPhotoUrl().isEmpty()) {
                args.putString("photoUrl", occurrence.getPhotoUrl());
                args.putString("imageUrl", occurrence.getPhotoUrl());
            }
            if (occurrence.getPhotoPath() != null && !occurrence.getPhotoPath().isEmpty()) {
                args.putString("photoPath", occurrence.getPhotoPath());
            }
            Navigation.findNavController(requireView()).navigate(R.id.nav_occurrence_full, args);
        });
        rvRecent.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecent.setNestedScrollingEnabled(false);
        rvRecent.setAdapter(adapter);

        getParentFragmentManager().setFragmentResultListener(
                com.aula.volta.ui.notifications.NotificationsSheet.REQUEST_KEY_UPDATED,
                getViewLifecycleOwner(),
                (requestKey, result) -> loadNotificationDot());

        loadSummary();
        loadOccurrences();
        loadNotificationDot();
        NotificationStore.addListener(this::loadNotificationDot);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        NotificationStore.removeListener(this::loadNotificationDot);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNotificationDot();
        loadAvatarPhoto();
    }

    private void loadAvatarPhoto() {
        if (!isAdded() || getView() == null) return;
        ImageView imgAvatar = getView().findViewById(R.id.imgAvatarPhoto);
        TextView tvInitials = getView().findViewById(R.id.tvAvatarInitials);
        if (imgAvatar == null) return;

        String localPath = SessionManager.getProfilePhotoLocal(requireContext());
        if (localPath != null && !localPath.isEmpty()) {
            File f = new File(localPath);
            if (f.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
                if (bmp != null) {
                    RoundedBitmapDrawable circular = RoundedBitmapDrawableFactory.create(getResources(), bmp);
                    circular.setCircular(true);
                    imgAvatar.setImageDrawable(circular);
                    imgAvatar.setVisibility(View.VISIBLE);
                    if (tvInitials != null) tvInitials.setVisibility(View.GONE);
                    return;
                }
            }
        }

        String remoteUrl = SessionManager.getProfilePhotoUrl(requireContext());
        if (remoteUrl != null && remoteUrl.startsWith("http")) {
            new Thread(() -> {
                try {
                    java.net.URL url = new java.net.URL(remoteUrl);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    conn.connect();
                    try (java.io.InputStream is = conn.getInputStream()) {
                        Bitmap bmp = BitmapFactory.decodeStream(is);
                        if (bmp != null && isAdded()) {
                            File f = new File(requireContext().getFilesDir(), "profile_photo.jpg");
                            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(f)) {
                                bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                            }
                            SessionManager.saveProfilePhotoLocal(requireContext(), f.getAbsolutePath());
                            requireActivity().runOnUiThread(() -> {
                                if (isAdded() && imgAvatar != null) {
                                    RoundedBitmapDrawable circular = RoundedBitmapDrawableFactory.create(getResources(), bmp);
                                    circular.setCircular(true);
                                    imgAvatar.setImageDrawable(circular);
                                    imgAvatar.setVisibility(View.VISIBLE);
                                    if (tvInitials != null) tvInitials.setVisibility(View.GONE);
                                }
                            });
                        }
                    }
                } catch (Exception ignored) {
                }
            }).start();
        } else {
            imgAvatar.setVisibility(View.GONE);
            if (tvInitials != null) tvInitials.setVisibility(View.VISIBLE);
        }
    }

    /** Resumo da unidade via GET /reports/summary (mock → API-ready). */
    private void loadSummary() {
        ReportAPI api = ApiClient.get(requireContext()).create(ReportAPI.class);
        api.getEsgMetrics().enqueue(new Callback<List<EsgMetricResponseDTO>>() {
            @Override
            public void onResponse(@NonNull Call<List<EsgMetricResponseDTO>> call,
                                   @NonNull Response<List<EsgMetricResponseDTO>> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    EsgMetricResponseDTO metric = response.body().get(0);
                    JsonObject summary = new JsonObject();
                    int kg = metric.getTotalRecycledKg() != null ? metric.getTotalRecycledKg().intValue() : 1147;
                    int totalWaste = metric.getTotalWasteKg() != null ? metric.getTotalWasteKg().intValue() : 1500;
                    double rate = metric.getRecyclingPercentage() != null ? metric.getRecyclingPercentage() : 76.5;
                    int resolvidas = (int) Math.round(totalWaste * (rate / 100.0));
                    int abertas = Math.max(0, totalWaste - resolvidas);
                    summary.addProperty("kg_reciclados", kg);
                    summary.addProperty("resolvidas", resolvidas > 0 ? resolvidas : 46);
                    summary.addProperty("abertas", abertas > 0 ? abertas : 6);

                    PrefsHelper.putJson(requireContext(), PREFS_REPORTS, KEY_SUMMARY, summary.toString());
                    applySummary(summary);
                    onLoadFinished();
                } else {
                    fallbackHomeSummary(api);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<EsgMetricResponseDTO>> call, @NonNull Throwable t) {
                if (isAdded()) {
                    fallbackHomeSummary(api);
                }
            }
        });
    }

    private void fallbackHomeSummary(ReportAPI api) {
        api.getSummary().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject summary = response.body();
                    PrefsHelper.putJson(requireContext(), PREFS_REPORTS, KEY_SUMMARY,
                            summary.toString());
                    applySummary(summary);
                } else {
                    loadSummaryFromCache();
                }
                onLoadFinished();
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                loadSummaryFromCache();
                Toast.makeText(requireContext(), R.string.offline_cache, Toast.LENGTH_LONG).show();
                onLoadFinished();
            }
        });
    }

    private void loadSummaryFromCache() {
        String json = PrefsHelper.getJson(requireContext(), PREFS_REPORTS, KEY_SUMMARY);
        if (json != null) {
            try {
                applySummary(com.google.gson.JsonParser.parseString(json).getAsJsonObject());
            } catch (Exception ignored) {
                // cache corrompido: mantém valores do layout
            }
        }
    }

    private void applySummary(JsonObject summary) {
        if (tvStatOpenCount != null && summary.has("abertas")) {
            tvStatOpenCount.setText(String.valueOf(summary.get("abertas").getAsInt()));
        }
        if (tvStatResolvedCount != null && summary.has("resolvidas")) {
            tvStatResolvedCount.setText(String.valueOf(summary.get("resolvidas").getAsInt()));
        }
        if (tvStatRecycledCount != null && summary.has("kg_reciclados")) {
            int kg = summary.get("kg_reciclados").getAsInt();
            tvStatRecycledCount.setText(String.format(new Locale("pt", "BR"), "%,d", kg));
        }
    }

    /** Ocorrências recentes via OccurrenceStore (API + locais lançados no app). */
    private void loadOccurrences() {
        OccurrenceStore.refresh(requireContext(), occurrences -> {
            if (!isAdded()) {
                return;
            }
            applyOccurrences(recentOnly(occurrences));
            onLoadFinished();
        });
    }

    /** Home mostra só as 3 mais recentes; a lista completa vive em nav_occurrences. */
    private List<Occurrence> recentOnly(List<Occurrence> occurrences) {
        if (occurrences == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(occurrences.subList(0, Math.min(3, occurrences.size())));
    }

    private void applyOccurrences(List<Occurrence> occurrences) {
        adapter.setItems(occurrences);
        if (tvEmptyOccurrences != null) {
            tvEmptyOccurrences.setVisibility(
                    occurrences == null || occurrences.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    /** Badge do sino = contagem de não lidas. */
    private void loadNotificationDot() {
        NotificationStore.refresh(requireContext(), notifications -> {
            int unread = NotificationStore.unreadCount(notifications);
            if (tvNotifCount != null) {
                tvNotifCount.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
                tvNotifCount.setText(String.valueOf(unread));
            }
            onLoadFinished();
        });
    }

    private void setupDots(LinearLayout container, int count, int selected) {
        if (!isAdded()) return;
        container.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int activeW = Math.round(22 * density);
        int inactiveW = Math.round(7 * density);
        int h = Math.round(7 * density);
        int margin = Math.round(3 * density);

        for (int i = 0; i < count; i++) {
            View dot = new View(requireContext());
            boolean isActive = (i == selected);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    isActive ? activeW : inactiveW,
                    h);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(isActive
                    ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
            container.addView(dot);
        }
    }

    private void onLoadFinished() {
        pendingLoads--;
        if (pendingLoads <= 0 && loadingHome != null) {
            loadingHome.setVisibility(View.GONE);
        }
    }
}
