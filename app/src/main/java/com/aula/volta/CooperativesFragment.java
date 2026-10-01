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
        tvEmptyCooperatives = view.findViewById(R.id.tvEmptyCooperatives);

        RecyclerView rvCooperatives = view.findViewById(R.id.rvCooperatives);
        adapter = new CooperativeAdapter();
        adapter.setOnItemClickListener(coop -> openChat(view, coop.getNome()));
        rvCooperatives.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCooperatives.setAdapter(adapter);

        view.findViewById(R.id.btnTalkTo).setOnClickListener(v -> {
            if (firstCoopName != null) {
                openChat(view, firstCoopName);
            }
        });

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
        CooperativeAPI api = ApiClient.get(requireContext()).create(CooperativeAPI.class);
        api.getRecommended(null).enqueue(new Callback<List<CooperativeJSON>>() {
            @Override
            public void onResponse(Call<List<CooperativeJSON>> call,
                                   Response<List<CooperativeJSON>> response) {
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
            public void onFailure(Call<List<CooperativeJSON>> call, Throwable t) {
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
        if (loadingCooperatives != null) {
            loadingCooperatives.setVisibility(View.GONE);
        }
    }
}
