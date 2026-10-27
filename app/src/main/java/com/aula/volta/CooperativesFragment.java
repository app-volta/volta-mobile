package com.aula.volta;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.CooperativeAPI;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.model.CooperativeJSON;
import com.aula.volta.data.model.CooperativeResponseDTO;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Aba Coops: lista SOMENTE cooperativas (conceito do plano — nunca ocorrências).
 * GET /cooperatives/recommended (mock → backend/AIC depois). Mapa chega com a Maps key.
 */
public class CooperativesFragment extends Fragment {

    private static final String PREFS = "cache_cooperatives";
    private static final String KEY_LIST = "list";

    private ProgressBar loadingCooperatives;
    private View skeletonCooperativesList;
    private androidx.recyclerview.widget.RecyclerView rvCooperatives;
    private TextView tvEmptyCooperatives;
    private CooperativeAdapter adapter;
    private String firstCoopName;

    public CooperativesFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cooperatives, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadingCooperatives = view.findViewById(R.id.loadingCooperatives);
        skeletonCooperativesList = view.findViewById(R.id.skeletonCooperativesList);
        tvEmptyCooperatives = view.findViewById(R.id.tvEmptyCooperatives);

        rvCooperatives = view.findViewById(R.id.rvCooperatives);
        adapter = new CooperativeAdapter();
        adapter.setOnItemClickListener(coop -> openChat(view, coop.getNome()));
        rvCooperatives.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCooperatives.setAdapter(adapter);

        view.findViewById(R.id.btnTalkTo).setOnClickListener(v -> {
            if (firstCoopName != null) {
                openChat(view, firstCoopName);
            }
        });

        View imgMap = view.findViewById(R.id.imgCoopsMap);
        if (imgMap != null) {
            imgMap.setOnClickListener(v -> {
                com.aula.volta.util.MapNavigator.openLocation(
                        requireContext(), -23.5186, -46.7369,
                        firstCoopName != null ? firstCoopName : "JBS Ambiental");
            });
        }

        View btnBack = view.findViewById(R.id.btnBackCoops);
        if (btnBack != null) {
            btnBack.setOnClickListener(v ->
                    Navigation.findNavController(v).navigateUp());
        }

        loadCooperatives();
    }

    private void openChat(View view, String coopName) {
        Bundle args = new Bundle();
        args.putString("coopName", coopName == null ? "" : coopName);
        Navigation.findNavController(view).navigate(R.id.nav_chat, args);
    }

    private void loadCooperatives() {
        if (skeletonCooperativesList != null) {
            skeletonCooperativesList.setVisibility(View.VISIBLE);
            com.aula.volta.util.SkeletonPulseAnimator.start(skeletonCooperativesList);
        }
        if (rvCooperatives != null) {
            rvCooperatives.setVisibility(View.GONE);
        }
        if (tvEmptyCooperatives != null) {
            tvEmptyCooperatives.setVisibility(View.GONE);
        }
        CooperativeAPI api = ApiClient.get(requireContext()).create(CooperativeAPI.class);
        api.getCooperatives().enqueue(new Callback<List<CooperativeResponseDTO>>() {
            @Override
            public void onResponse(@NonNull Call<List<CooperativeResponseDTO>> call,
                                   @NonNull Response<List<CooperativeResponseDTO>> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<CooperativeJSON> coops = new ArrayList<>();
                    for (CooperativeResponseDTO dto : response.body()) {
                        coops.add(CooperativeJSON.fromDTO(dto));
                    }
                    PrefsHelper.putList(requireContext(), PREFS, KEY_LIST, coops);
                    applyCooperatives(coops);
                    hideLoading();
                } else if (response.code() == 403) {
                    fetchCooperativesAsService();
                } else {
                    fallbackRecommended(api);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<CooperativeResponseDTO>> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                fallbackRecommended(api);
            }
        });
    }

    private void fetchCooperativesAsService() {
        new Thread(() -> {
            try {
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .build();

                com.google.gson.JsonObject loginBody = new com.google.gson.JsonObject();
                loginBody.addProperty("email", ApiClient.ADMIN_EMAIL);
                loginBody.addProperty("password", ApiClient.ADMIN_PASSWORD);
                okhttp3.Request loginReq = new okhttp3.Request.Builder()
                        .url(ApiClient.BASE_URL_REAL + "auth/login")
                        .header("Accept", "application/json")
                        .post(okhttp3.RequestBody.create(loginBody.toString(), okhttp3.MediaType.parse("application/json")))
                        .build();

                okhttp3.Response loginResp = client.newCall(loginReq).execute();
                if (loginResp.isSuccessful() && loginResp.body() != null) {
                    com.google.gson.JsonObject respObj = com.google.gson.JsonParser.parseString(loginResp.body().string()).getAsJsonObject();
                    String adminToken = respObj.has("token") ? respObj.get("token").getAsString() : null;
                    if (adminToken != null) {
                        okhttp3.Request coopReq = new okhttp3.Request.Builder()
                                .url(ApiClient.BASE_URL_REAL + "cooperatives")
                                .header("Authorization", "Bearer " + adminToken)
                                .header("X-Company-Id", ApiClient.ADMIN_COMPANY_ID)
                                .header("Accept", "application/json")
                                .get()
                                .build();
                        okhttp3.Response coopResp = client.newCall(coopReq).execute();
                        if (coopResp.isSuccessful() && coopResp.body() != null) {
                            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<CooperativeResponseDTO>>() {}.getType();
                            List<CooperativeResponseDTO> dtos = new com.google.gson.Gson().fromJson(coopResp.body().string(), listType);
                            if (dtos != null && !dtos.isEmpty()) {
                                List<CooperativeJSON> coops = new ArrayList<>();
                                for (CooperativeResponseDTO dto : dtos) {
                                    coops.add(CooperativeJSON.fromDTO(dto));
                                }
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        if (!isAdded()) return;
                                        PrefsHelper.putList(requireContext(), PREFS, KEY_LIST, coops);
                                        applyCooperatives(coops);
                                        hideLoading();
                                    });
                                    return;
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    applyCooperatives(cachedCooperatives());
                    hideLoading();
                });
            }
        }).start();
    }

    private void fallbackRecommended(CooperativeAPI api) {
        api.getRecommended(null).enqueue(new Callback<List<CooperativeJSON>>() {
            @Override
            public void onResponse(@NonNull Call<List<CooperativeJSON>> call,
                                   @NonNull Response<List<CooperativeJSON>> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    PrefsHelper.putList(requireContext(), PREFS, KEY_LIST, response.body());
                    applyCooperatives(response.body());
                } else {
                    applyCooperatives(cachedCooperatives());
                }
                hideLoading();
            }

            @Override
            public void onFailure(@NonNull Call<List<CooperativeJSON>> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                applyCooperatives(cachedCooperatives());
                Toast.makeText(requireContext(), R.string.offline_cache, Toast.LENGTH_LONG).show();
                hideLoading();
            }
        });
    }

    private List<CooperativeJSON> cachedCooperatives() {
        return PrefsHelper.getList(requireContext(), PREFS, KEY_LIST,
                PrefsHelper.listType(CooperativeJSON.class));
    }

    private void applyCooperatives(List<CooperativeJSON> cooperatives) {
        adapter.setItems(cooperatives);
        if (cooperatives != null && !cooperatives.isEmpty()
                && cooperatives.get(0).getNome() != null) {
            firstCoopName = cooperatives.get(0).getNome();
            TextView btnTalk = requireView().findViewById(R.id.btnTalkTo);
            if (btnTalk != null) {
                btnTalk.setText(getString(R.string.chat_talk_to, firstCoopName));
            }
        }
        if (tvEmptyCooperatives != null) {
            tvEmptyCooperatives.setVisibility(
                    cooperatives == null || cooperatives.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void hideLoading() {
        if (skeletonCooperativesList != null) {
            com.aula.volta.util.SkeletonPulseAnimator.stop(skeletonCooperativesList);
            skeletonCooperativesList.setVisibility(View.GONE);
        }
        if (rvCooperatives != null) {
            rvCooperatives.setVisibility(View.VISIBLE);
        }
        if (loadingCooperatives != null) {
            loadingCooperatives.setVisibility(View.GONE);
        }
    }
}
