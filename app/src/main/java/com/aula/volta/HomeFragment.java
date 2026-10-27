package com.aula.volta;

import android.content.Context;

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
import com.aula.volta.data.api.AuthAPI;
import com.aula.volta.data.model.UserResponseDTO;
import java.util.Calendar;
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
    private final OccurrenceStore.StoreListener storeListener = this::loadOccurrences;
    private static final String KEY_SUMMARY = "summary";

    private TextView tvStatOpenCount;
    private TextView tvStatResolvedCount;
    private TextView tvStatRecycledCount;
    private View loadingHome;
    private TextView tvEmptyOccurrences;
    private TextView tvNotifCount;
    private TextView tvGreeting;
    private TextView tvUserName;
    private TextView tvAvatarInitials;
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
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvAvatarInitials = view.findViewById(R.id.tvAvatarInitials);

        updateGreetingAndUser();

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

        // 1. Carregamento Cache-First Imediato (sem tela em branco e sem esperar rede)
        loadSummaryFromCache();
        applyOccurrences(recentOnly(OccurrenceStore.cached(requireContext())));

        // 2. Loading verde animado com timer de segurança de no máximo 3s
        if (loadingHome != null) {
            loadingHome.setVisibility(View.VISIBLE);
            loadingHome.postDelayed(() -> {
                if (isAdded() && loadingHome != null) {
                    loadingHome.setVisibility(View.GONE);
                }
            }, 3000);
        }

        // 3. Atualização em segundo plano via rede
        OccurrenceStore.addListener(storeListener);
        loadSummary();
        loadOccurrences();
        loadNotificationDot();
        NotificationStore.addListener(this::loadNotificationDot);
        fetchUserProfileFromDatabase();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        OccurrenceStore.removeListener(storeListener);
        NotificationStore.removeListener(this::loadNotificationDot);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateGreetingAndUser();
        loadNotificationDot();
        loadAvatarPhoto();
        loadOccurrences();
    }

    /**
     * Atualiza a saudação ("Bom dia,", "Boa tarde,", "Boa noite,") dependendo do horário local
     * e vincula o nome e iniciais do colaborador obtidos do SessionManager e da API oficial do banco (GET /users/me).
     */
    private void updateGreetingAndUser() {
        if (!isAdded() || getView() == null) return;
        if (tvGreeting == null) tvGreeting = getView().findViewById(R.id.tvGreeting);
        if (tvUserName == null) tvUserName = getView().findViewById(R.id.tvUserName);
        if (tvAvatarInitials == null) tvAvatarInitials = getView().findViewById(R.id.tvAvatarInitials);

        // 1. Saudação contextual baseada na hora do dia
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour >= 5 && hour < 12) {
            greeting = "Bom dia,";
        } else if (hour >= 12 && hour < 18) {
            greeting = "Boa tarde,";
        } else {
            greeting = "Boa noite,";
        }
        if (tvGreeting != null) {
            tvGreeting.setText(greeting);
        }

        // 2. Nome da sessão local
        String currentName = SessionManager.name(requireContext());
        if (currentName == null || currentName.trim().isEmpty() || "Breno Liu".equalsIgnoreCase(currentName)) {
            currentName = "Colaborador";
        }
        applyUserNameAndInitials(currentName);

        // 3. Atualização ao vivo via API oficial do banco (GET /users/me)
        try {
            AuthAPI authApi = ApiClient.get(requireContext()).create(AuthAPI.class);
            authApi.getMe().enqueue(new Callback<UserResponseDTO>() {
                @Override
                public void onResponse(@NonNull Call<UserResponseDTO> call, @NonNull Response<UserResponseDTO> response) {
                    if (isAdded() && response.isSuccessful() && response.body() != null) {
                        UserResponseDTO u = response.body();
                        String myEmail = SessionManager.email(requireContext());
                        if (myEmail.isEmpty() || (u.getEmail() != null && !u.getEmail().trim().isEmpty() && !u.getEmail().equalsIgnoreCase(myEmail))) {
                            return;
                        }
                        if (u.getName() != null && !u.getName().trim().isEmpty()) {
                            SessionManager.saveName(requireContext(), u.getName().trim());
                            applyUserNameAndInitials(u.getName().trim());
                        }
                        if (u.getPhotoUrl() != null && !u.getPhotoUrl().isEmpty()) {
                            SessionManager.saveProfilePhotoUrl(requireContext(), u.getPhotoUrl());
                            loadAvatarPhoto();
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<UserResponseDTO> call, @NonNull Throwable t) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void applyUserNameAndInitials(String name) {
        if (!isAdded() || name == null) return;
        if (tvUserName != null) {
            tvUserName.setText(name);
        }
        if (tvAvatarInitials != null) {
            String initials = "BG";
            String[] parts = name.trim().split("\\s+");
            if (parts.length >= 2) {
                initials = ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase(Locale.ROOT);
            } else if (parts.length == 1 && !parts[0].isEmpty()) {
                initials = parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
            }
            tvAvatarInitials.setText(initials);
        }
    }

    private void loadAvatarPhoto() {
        if (!isAdded() || getView() == null) return;
        ImageView imgAvatar = getView().findViewById(R.id.imgAvatarPhoto);
        TextView tvInitials = getView().findViewById(R.id.tvAvatarInitials);
        if (imgAvatar == null) return;

        String email = SessionManager.email(requireContext());
        String cleanEmail = email != null ? email.trim().toLowerCase(java.util.Locale.ROOT) : "";
        String localPath = SessionManager.getProfilePhotoLocal(requireContext());
        File localFile = null;
        if (localPath != null && !localPath.isEmpty()) {
            File f = new File(localPath);
            if (f.exists()) localFile = f;
        }
        if (localFile == null && !cleanEmail.isEmpty()) {
            File userHashed = new File(requireContext().getFilesDir(), "profile_photo_" + Math.abs(cleanEmail.hashCode()) + ".jpg");
            if (userHashed.exists()) localFile = userHashed;
        }

        if (localFile != null && localFile.exists()) {
            Bitmap bmp = BitmapFactory.decodeFile(localFile.getAbsolutePath());
            if (bmp != null) {
                RoundedBitmapDrawable circular = RoundedBitmapDrawableFactory.create(getResources(), bmp);
                circular.setCircular(true);
                imgAvatar.setImageDrawable(circular);
                imgAvatar.setVisibility(View.VISIBLE);
                if (tvInitials != null) tvInitials.setVisibility(View.GONE);
                return;
            }
        }

        String remoteUrl = SessionManager.getProfilePhotoUrl(requireContext());
        if (remoteUrl != null && remoteUrl.startsWith("http")) {
            final Context appContext = requireContext().getApplicationContext();
            com.aula.volta.data.upload.CloudinaryUploader.fetchRemoteBitmap(remoteUrl, new com.aula.volta.data.upload.CloudinaryUploader.BitmapCallback() {
                @Override
                public void onBitmapLoaded(Bitmap bmp) {
                    if (bmp == null) return;
                    try {
                        if (!cleanEmail.isEmpty()) {
                            File hashedFile = new File(appContext.getFilesDir(), "profile_photo_" + Math.abs(cleanEmail.hashCode()) + ".jpg");
                            try (java.io.FileOutputStream fos2 = new java.io.FileOutputStream(hashedFile)) {
                                bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos2);
                            }
                            SessionManager.saveProfilePhotoLocal(appContext, cleanEmail, hashedFile.getAbsolutePath());
                        }
                    } catch (Exception ignored) {
                    }
                    if (isAdded() && imgAvatar != null) {
                        RoundedBitmapDrawable circular = RoundedBitmapDrawableFactory.create(getResources(), bmp);
                        circular.setCircular(true);
                        imgAvatar.setImageDrawable(circular);
                        imgAvatar.setVisibility(View.VISIBLE);
                        if (tvInitials != null) tvInitials.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onError(Exception e) {
                    android.util.Log.w("HomeFragment", "Falha ao carregar foto remota de perfil: " + e.getMessage());
                }
            });
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
                return;
            } catch (Exception ignored) {
                // cache corrompido: fallback para padrões
            }
        }
        // Fallback padrão inicial (Figma 790:1685..1703)
        if (tvStatOpenCount != null && (tvStatOpenCount.getText() == null || "—".contentEquals(tvStatOpenCount.getText()))) {
            tvStatOpenCount.setText("9");
        }
        if (tvStatResolvedCount != null && (tvStatResolvedCount.getText() == null || "—".contentEquals(tvStatResolvedCount.getText()))) {
            tvStatResolvedCount.setText("43");
        }
        if (tvStatRecycledCount != null && (tvStatRecycledCount.getText() == null || "—".contentEquals(tvStatRecycledCount.getText()))) {
            tvStatRecycledCount.setText("1.147");
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
    private void fetchUserProfileFromDatabase() {
        if (!isAdded()) return;
        final Context app = requireContext().getApplicationContext();
        try {
            AuthAPI api = ApiClient.get(app).create(AuthAPI.class);
            api.getMe().enqueue(new Callback<UserResponseDTO>() {
                @Override
                public void onResponse(@NonNull Call<UserResponseDTO> call, @NonNull Response<UserResponseDTO> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        UserResponseDTO user = response.body();
                        boolean changed = false;
                        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                            String currentPhoto = SessionManager.getProfilePhotoUrl(app);
                            if (!user.getPhotoUrl().equals(currentPhoto)) {
                                SessionManager.saveProfilePhotoUrl(app, user.getPhotoUrl());
                                changed = true;
                            }
                        }
                        if (user.getName() != null && !user.getName().isEmpty()) {
                            SessionManager.saveName(app, user.getName());
                            changed = true;
                        }
                        if (isAdded()) {
                            updateGreetingAndUser();
                            if (changed) {
                                loadAvatarPhoto();
                            }
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<UserResponseDTO> call, @NonNull Throwable t) {
                }
            });
        } catch (Exception ignored) {
        }
    }

}
