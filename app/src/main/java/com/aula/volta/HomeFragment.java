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
    private View skeletonHomeSummary;
    private View skeletonRecentOccurrences;
    private View layoutSummaryCards;
    private androidx.recyclerview.widget.RecyclerView rvRecentOccurrences;
    private TextView tvEmptyOccurrences;
    private TextView tvNotifCount;
    private TextView tvGreeting;
    private TextView tvUserName;
    private TextView tvAvatarInitials;
    private OccurrenceAdapter adapter;
    private CarouselAdapter carouselAdapter;

    private int pendingLoads = 3;

    public HomeFragment() {

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

        ViewPager2 vpCarousel = view.findViewById(R.id.vpCarousel);
        LinearLayout carouselDots = view.findViewById(R.id.carouselDots);
        if (vpCarousel != null) {
            carouselAdapter = new CarouselAdapter(new CarouselAdapter.OnCarouselClickListener() {
                @Override
                public void onRegisterClick() {
                    if (!com.aula.volta.util.NetworkUtils.isOnline(requireContext())) {
                        Bundle errArgs = new Bundle();
                        errArgs.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_ERROR_TYPE,
                                com.aula.volta.ui.error.OfflineErrorFragment.TYPE_OFFLINE);
                        errArgs.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_BADGE, "Você está offline");
                        errArgs.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_TITLE, "Sem conexão para fotografar");
                        errArgs.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_MESSAGE,
                                "Você precisa de conexão com a internet para tirar fotos e registrar ocorrências no banco de dados.");
                        errArgs.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_INFO_TITLE, "Câmera e registro bloqueados offline");
                        errArgs.putString(com.aula.volta.ui.error.OfflineErrorFragment.ARG_INFO_BODY,
                                "Todas as ocorrências são enviadas diretamente ao banco oficial da unidade. Conecte-se ao Wi-Fi ou dados móveis para continuar.");
                        Navigation.findNavController(view).navigate(R.id.nav_offline_error, errArgs);
                        return;
                    }
                    Navigation.findNavController(view).navigate(R.id.nav_register);
                }

                @Override
                public void onReportsClick() {
                    Navigation.findNavController(view).navigate(R.id.nav_reports);
                }
            });
            vpCarousel.setAdapter(carouselAdapter);
            if (carouselDots != null) {
                if (carouselAdapter.getItemCount() > 1) {
                    carouselDots.setVisibility(View.VISIBLE);
                    setupDots(carouselDots, carouselAdapter.getItemCount(), 0);
                    vpCarousel.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                        @Override
                        public void onPageSelected(int position) {
                            setupDots(carouselDots, carouselAdapter.getItemCount(), position);
                        }
                    });
                } else {
                    carouselDots.setVisibility(View.GONE);
                }
            }
        }

        TextView tvSeeAll = view.findViewById(R.id.tvSeeAllOccurrences);
        if (tvSeeAll != null) {
            tvSeeAll.setOnClickListener(v -> {
                Navigation.findNavController(v).navigate(R.id.nav_occurrences);
            });
        }

        View btnNotifications = view.findViewById(R.id.btnNotifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                Navigation.findNavController(v).navigate(R.id.nav_notifications);
            });
        }

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

        rvRecentOccurrences = view.findViewById(R.id.rvRecentOccurrences);
        RecyclerView rvRecent = rvRecentOccurrences;
        loadingHome = view.findViewById(R.id.loadingHome);
        skeletonHomeSummary = view.findViewById(R.id.skeletonHomeSummary);
        skeletonRecentOccurrences = view.findViewById(R.id.skeletonRecentOccurrences);
        layoutSummaryCards = view.findViewById(R.id.layoutSummaryCards);
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
        adapter.setForceMaterialIcon(true);
        rvRecent.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecent.setNestedScrollingEnabled(false);
        rvRecent.setAdapter(adapter);

        getParentFragmentManager().setFragmentResultListener(
                com.aula.volta.ui.notifications.NotificationsSheet.REQUEST_KEY_UPDATED,
                getViewLifecycleOwner(),
                (requestKey, result) -> loadNotificationDot());

        loadSummaryFromCache();
        applyOccurrences(recentOnly(OccurrenceStore.cached(requireContext())));

        if (loadingHome != null) {
            loadingHome.setVisibility(View.VISIBLE);
            loadingHome.postDelayed(() -> {
                if (isAdded() && loadingHome != null) {
                    loadingHome.setVisibility(View.GONE);
                }
            }, 3000);
        }

        OccurrenceStore.addListener(storeListener);
        loadSummary();
        loadOccurrences();
        loadNotificationDot();
        NotificationStore.addListener(this::loadNotificationDot);
        fetchUserProfileFromDatabase();
    }

    @Override
    public void onDestroyView() {
        if (skeletonHomeSummary != null) {
            com.aula.volta.util.SkeletonPulseAnimator.stop(skeletonHomeSummary);
        }
        if (skeletonRecentOccurrences != null) {
            com.aula.volta.util.SkeletonPulseAnimator.stop(skeletonRecentOccurrences);
        }
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

    private void updateGreetingAndUser() {
        if (!isAdded() || getView() == null) return;
        if (tvGreeting == null) tvGreeting = getView().findViewById(R.id.tvGreeting);
        if (tvUserName == null) tvUserName = getView().findViewById(R.id.tvUserName);
        if (tvAvatarInitials == null) tvAvatarInitials = getView().findViewById(R.id.tvAvatarInitials);

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

        String currentName = SessionManager.name(requireContext());
        if (currentName == null || currentName.trim().isEmpty()) {
            currentName = "Colaborador";
        }
        applyUserNameAndInitials(currentName);

        try {
            AuthAPI authApi = ApiClient.get(requireContext()).create(AuthAPI.class);
            authApi.getMyCompany().enqueue(new Callback<com.aula.volta.data.model.CompanyResponseDTO>() {
                @Override
                public void onResponse(@NonNull Call<com.aula.volta.data.model.CompanyResponseDTO> call,
                                       @NonNull Response<com.aula.volta.data.model.CompanyResponseDTO> compResp) {
                    if (isAdded() && compResp.isSuccessful() && compResp.body() != null) {
                        com.aula.volta.data.model.CompanyResponseDTO c = compResp.body();
                        if (c.getId() != null && !c.getId().trim().isEmpty()) {
                            SessionManager.saveCompanyId(requireContext(), c.getId().trim());
                        }
                        SessionManager.saveCompany(requireContext(), c.getName(), c.getFormattedCnpj(), c.getAddress());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<com.aula.volta.data.model.CompanyResponseDTO> call, @NonNull Throwable t) {
                }
            });
            authApi.getMe().enqueue(new Callback<UserResponseDTO>() {
                @Override
                public void onResponse(@NonNull Call<UserResponseDTO> call, @NonNull Response<UserResponseDTO> response) {
                    if (isAdded() && response.isSuccessful() && response.body() != null) {
                        UserResponseDTO u = response.body();
                        String myEmail = SessionManager.email(requireContext());
                        if (myEmail.isEmpty() || (u.getEmail() != null && !u.getEmail().trim().isEmpty() && !u.getEmail().equalsIgnoreCase(myEmail))) {
                            return;
                        }
                        if (u.getId() != null && !u.getId().trim().isEmpty()) {
                            SessionManager.saveUserId(requireContext(), u.getId().trim());
                        }
                        if (u.getCompanyId() != null && !u.getCompanyId().trim().isEmpty()) {
                            SessionManager.saveCompanyId(requireContext(), u.getCompanyId().trim());
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
            String initials = "US";
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
        if ((remoteUrl == null || remoteUrl.isEmpty()) && !cleanEmail.isEmpty()) {
            remoteUrl = com.aula.volta.data.upload.CloudinaryUploader.getDeterministicProfileUrl(cleanEmail);
        }
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
                    if (isAdded() && getActivity() != null) {
                        requireActivity().runOnUiThread(() -> {
                            if (imgAvatar != null) {
                                RoundedBitmapDrawable circular = RoundedBitmapDrawableFactory.create(getResources(), bmp);
                                circular.setCircular(true);
                                imgAvatar.setImageDrawable(circular);
                                imgAvatar.setVisibility(View.VISIBLE);
                                if (tvInitials != null) tvInitials.setVisibility(View.GONE);
                            }
                        });
                    }
                }

                @Override
                public void onError(Exception e) {
                    android.util.Log.w("HomeFragment", "Falha ao carregar foto remota de perfil: " + e.getMessage());
                    if (isAdded() && getActivity() != null) {
                        requireActivity().runOnUiThread(() -> {
                            if (imgAvatar != null) {
                                imgAvatar.setVisibility(View.GONE);
                            }
                            if (tvInitials != null) {
                                tvInitials.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }
            });
        } else {
            imgAvatar.setVisibility(View.GONE);
            if (tvInitials != null) tvInitials.setVisibility(View.VISIBLE);
        }
    }

    private void loadSummary() {
        if (skeletonHomeSummary != null) {
            skeletonHomeSummary.setVisibility(View.VISIBLE);
            com.aula.volta.util.SkeletonPulseAnimator.start(skeletonHomeSummary);
        }
        if (layoutSummaryCards != null) {
            layoutSummaryCards.setVisibility(View.GONE);
        }
        ReportAPI api = ApiClient.get(requireContext()).create(ReportAPI.class);
        api.getSummary().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject raw = response.body();
                    JsonObject summary = parseRealSummary(raw);
                    PrefsHelper.putJson(requireContext(), PREFS_REPORTS, KEY_SUMMARY, summary.toString());
                    applySummary(summary);
                    onLoadFinished();
                } else {
                    fallbackEsgMetrics(api);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (isAdded()) {
                    fallbackEsgMetrics(api);
                }
            }
        });
    }

    private JsonObject parseRealSummary(JsonObject raw) {
        JsonObject summary = new JsonObject();
        if (raw.has("kg_reciclados") && raw.has("abertas") && raw.has("resolvidas")) {
            return raw;
        }

        int kg = 0;
        if (raw.has("esgMetrics") && raw.get("esgMetrics").isJsonArray()) {
            com.google.gson.JsonArray esgArr = raw.getAsJsonArray("esgMetrics");
            if (esgArr.size() > 0) {
                JsonObject first = esgArr.get(0).getAsJsonObject();
                if (first.has("totalRecycledKg") && !first.get("totalRecycledKg").isJsonNull()) {
                    kg = (int) Math.round(first.get("totalRecycledKg").getAsDouble());
                }
                if (first.has("monthlyTarget") && !first.get("monthlyTarget").isJsonNull()) {
                    summary.addProperty("monthlyTarget", first.get("monthlyTarget").getAsDouble());
                }
                if (first.has("weeklyTarget") && !first.get("weeklyTarget").isJsonNull()) {
                    summary.addProperty("weeklyTarget", first.get("weeklyTarget").getAsDouble());
                }
                if (first.has("quarterlyTarget") && !first.get("quarterlyTarget").isJsonNull()) {
                    summary.addProperty("quarterlyTarget", first.get("quarterlyTarget").getAsDouble());
                }
            }
        }

        int openCount = 0;
        int resolvedCount = 0;
        if (raw.has("incidents") && raw.get("incidents").isJsonArray()) {
            for (com.google.gson.JsonElement el : raw.getAsJsonArray("incidents")) {
                if (el.isJsonObject()) {
                    String st = el.getAsJsonObject().has("status") && !el.getAsJsonObject().get("status").isJsonNull()
                            ? el.getAsJsonObject().get("status").getAsString().toUpperCase() : "";
                    if ("CLOSED".equals(st) || "FINALIZADA".equals(st) || "RESOLVIDO".equals(st) || "APROVADO".equals(st) || "APROVADA".equals(st)) {
                        resolvedCount++;
                    } else {
                        openCount++;
                    }
                }
            }
        }

        summary.addProperty("kg_reciclados", kg);
        summary.addProperty("abertas", openCount);
        summary.addProperty("resolvidas", resolvedCount);

        if (raw.has("areas") && raw.get("areas").isJsonArray()) {
            for (com.google.gson.JsonElement el : raw.getAsJsonArray("areas")) {
                if (el.isJsonObject() && el.getAsJsonObject().has("id")) {
                    String areaId = el.getAsJsonObject().get("id").getAsString();
                    PrefsHelper.putJson(requireContext(), "cache_areas", areaId, el.toString());
                }
            }
        }

        if (raw.has("wasteTypes") && raw.get("wasteTypes").isJsonArray()) {
            PrefsHelper.putJson(requireContext(), "cache_waste_types", "list", raw.getAsJsonArray("wasteTypes").toString());
        }

        return summary;
    }

    private void fallbackEsgMetrics(ReportAPI api) {
        api.getEsgMetrics().enqueue(new Callback<List<EsgMetricResponseDTO>>() {
            @Override
            public void onResponse(@NonNull Call<List<EsgMetricResponseDTO>> call,
                                   @NonNull Response<List<EsgMetricResponseDTO>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    EsgMetricResponseDTO metric = response.body().get(0);
                    JsonObject summary = new JsonObject();
                    int kg = metric.getTotalRecycledKg() != null ? metric.getTotalRecycledKg().intValue() : 0;
                    int openCount = 0;
                    int resolvedCount = 0;
                    List<Occurrence> cachedOccs = OccurrenceStore.cached(requireContext());
                    if (cachedOccs != null) {
                        for (Occurrence occ : cachedOccs) {
                            if (occ.isResolved()) {
                                resolvedCount++;
                            } else {
                                openCount++;
                            }
                        }
                    }
                    summary.addProperty("kg_reciclados", kg);
                    summary.addProperty("resolvidas", resolvedCount);
                    summary.addProperty("abertas", openCount);
                    PrefsHelper.putJson(requireContext(), PREFS_REPORTS, KEY_SUMMARY, summary.toString());
                    applySummary(summary);
                } else {
                    loadSummaryFromCache();
                }
                onLoadFinished();
            }

            @Override
            public void onFailure(@NonNull Call<List<EsgMetricResponseDTO>> call, @NonNull Throwable t) {
                if (isAdded()) {
                    loadSummaryFromCache();
                    onLoadFinished();
                }
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

            }
        }

        int openCount = 0;
        int resolvedCount = 0;
        List<Occurrence> cachedOccs = OccurrenceStore.cached(requireContext());
        if (cachedOccs != null) {
            for (Occurrence occ : cachedOccs) {
                if (occ.isResolved()) {
                    resolvedCount++;
                } else {
                    openCount++;
                }
            }
        }

        if (tvStatOpenCount != null && (tvStatOpenCount.getText() == null || "—".contentEquals(tvStatOpenCount.getText()))) {
            tvStatOpenCount.setText(String.valueOf(openCount));
        }
        if (tvStatResolvedCount != null && (tvStatResolvedCount.getText() == null || "—".contentEquals(tvStatResolvedCount.getText()))) {
            tvStatResolvedCount.setText(String.valueOf(resolvedCount));
        }
        if (tvStatRecycledCount != null && (tvStatRecycledCount.getText() == null || "—".contentEquals(tvStatRecycledCount.getText()))) {
            tvStatRecycledCount.setText("0");
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
        if (carouselAdapter != null && summary.has("meta_percent")) {
            carouselAdapter.setGoalPercent(summary.get("meta_percent").getAsInt());
        }
    }

    private void loadOccurrences() {
        if (skeletonRecentOccurrences != null) {
            skeletonRecentOccurrences.setVisibility(View.VISIBLE);
            com.aula.volta.util.SkeletonPulseAnimator.start(skeletonRecentOccurrences);
        }
        if (rvRecentOccurrences != null) {
            rvRecentOccurrences.setVisibility(View.GONE);
        }
        if (tvEmptyOccurrences != null) {
            tvEmptyOccurrences.setVisibility(View.GONE);
        }
        OccurrenceStore.refresh(requireContext(), occurrences -> {
            if (!isAdded()) {
                return;
            }
            if (skeletonRecentOccurrences != null) {
                com.aula.volta.util.SkeletonPulseAnimator.stop(skeletonRecentOccurrences);
                skeletonRecentOccurrences.setVisibility(View.GONE);
            }
            if (rvRecentOccurrences != null) {
                rvRecentOccurrences.setVisibility(View.VISIBLE);
            }
            applyOccurrences(recentOnly(occurrences));
            onLoadFinished();
        });
    }

    private List<Occurrence> recentOnly(List<Occurrence> occurrences) {
        if (occurrences == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(occurrences.subList(0, Math.min(3, occurrences.size())));
    }

    private void applyOccurrences(List<Occurrence> occurrences) {
        adapter.setItems(occurrences);
        if (carouselAdapter != null) {
            carouselAdapter.setOccurrences(OccurrenceStore.cached(requireContext()));
        }
        if (tvEmptyOccurrences != null) {
            tvEmptyOccurrences.setVisibility(
                    occurrences == null || occurrences.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

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
        if (pendingLoads <= 0) {
            if (skeletonHomeSummary != null) {
                com.aula.volta.util.SkeletonPulseAnimator.stop(skeletonHomeSummary);
                skeletonHomeSummary.setVisibility(View.GONE);
            }
            if (layoutSummaryCards != null) {
                layoutSummaryCards.setVisibility(View.VISIBLE);
            }
            if (skeletonRecentOccurrences != null) {
                com.aula.volta.util.SkeletonPulseAnimator.stop(skeletonRecentOccurrences);
                skeletonRecentOccurrences.setVisibility(View.GONE);
            }
            if (rvRecentOccurrences != null) {
                rvRecentOccurrences.setVisibility(View.VISIBLE);
            }
            if (loadingHome != null) {
                loadingHome.setVisibility(View.GONE);
            }
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
                        if (user.getId() != null && !user.getId().trim().isEmpty()) {
                            SessionManager.saveUserId(app, user.getId().trim());
                        }
                        if (user.getCompanyId() != null && !user.getCompanyId().trim().isEmpty()) {
                            SessionManager.saveCompanyId(app, user.getCompanyId().trim());
                        }
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
