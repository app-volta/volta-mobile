package com.aula.volta;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.CooperativeAPI;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.model.CooperativeJSON;
import com.aula.volta.data.model.CooperativeResponseDTO;
import com.aula.volta.util.MapNavigator;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CooperativesFragment extends Fragment {

    private static final String PREFS = "cache_cooperatives_db";
    private static final String KEY_LIST = "db_list";

    private static final double DEFAULT_UNIT_LAT = -23.5320;
    private static final double DEFAULT_UNIT_LNG = -46.6850;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private double currentUserLat = DEFAULT_UNIT_LAT;
    private double currentUserLng = DEFAULT_UNIT_LNG;
    private boolean hasRealLocation = false;

    private ProgressBar loadingCooperatives;
    private View skeletonCooperativesList;
    private RecyclerView rvCooperatives;
    private TextView tvEmptyCooperatives;
    private CooperativeAdapter adapter;

    private WebView webViewCoopsMap;
    private ImageView imgCoopsMapPlaceholder;
    private ProgressBar mapProgressBar;
    private TextView tvMapBadgeCount;
    private boolean isMapReady = false;

    private final List<CooperativeJSON> currentCooperatives = new ArrayList<>();
    private final Gson gson = new Gson();

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (getContext() == null || !isAdded()) return;
                boolean fineGranted = Boolean.TRUE.equals(result.get(android.Manifest.permission.ACCESS_FINE_LOCATION));
                boolean coarseGranted = Boolean.TRUE.equals(result.get(android.Manifest.permission.ACCESS_COARSE_LOCATION));
                if (fineGranted || coarseGranted) {
                    startLocationUpdates();
                }
            });

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
        tvMapBadgeCount = view.findViewById(R.id.tvMapBadgeCount);
        imgCoopsMapPlaceholder = view.findViewById(R.id.imgCoopsMapPlaceholder);
        mapProgressBar = view.findViewById(R.id.mapProgressBar);

        rvCooperatives = view.findViewById(R.id.rvCooperatives);
        adapter = new CooperativeAdapter();
        adapter.setOnItemClickListener(this::focusOnCooperative);
        rvCooperatives.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCooperatives.setAdapter(adapter);

        setupInteractiveMap(view);

        View btnRecenter = view.findViewById(R.id.btnRecenterMap);
        if (btnRecenter != null) {
            btnRecenter.setOnClickListener(v -> {
                Location freshLoc = getBestLastKnownLocation();
                if (freshLoc != null) {
                    onUserLocationAcquired(freshLoc.getLatitude(), freshLoc.getLongitude());
                }
                if (webViewCoopsMap != null && isMapReady) {
                    webViewCoopsMap.evaluateJavascript("javascript:recenterMap();", null);
                }
            });
        }

        View btnBack = view.findViewById(R.id.btnBackCoops);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        }

        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }

        loadCooperativesFromDatabase();
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void setupInteractiveMap(View root) {
        webViewCoopsMap = root.findViewById(R.id.webViewCoopsMap);
        if (webViewCoopsMap == null) return;

        WebSettings settings = webViewCoopsMap.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setGeolocationEnabled(true);
        try {
            settings.setGeolocationDatabasePath(requireContext().getFilesDir().getPath());
        } catch (Exception ignored) {}

        webViewCoopsMap.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
            return false;
        });

        webViewCoopsMap.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void onMapReady() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isMapReady = true;
                        if (mapProgressBar != null) {
                            mapProgressBar.setVisibility(View.GONE);
                        }
                        if (imgCoopsMapPlaceholder != null) {
                            imgCoopsMapPlaceholder.setVisibility(View.GONE);
                        }
                        if (webViewCoopsMap != null) {
                            webViewCoopsMap.setVisibility(View.VISIBLE);
                        }
                        if (!currentCooperatives.isEmpty()) {
                            syncMapMarkers();
                        }
                    });
                }
            }

            @JavascriptInterface
            public void onUserLocationFound(double lat, double lng) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> onUserLocationAcquired(lat, lng));
                }
            }

            @JavascriptInterface
            public void openExternalMap(double lat, double lng, String label) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            MapNavigator.openLocation(requireContext(), lat, lng, label));
                }
            }

            @JavascriptInterface
            public void onCooperativeClicked(String coopId) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        for (int i = 0; i < currentCooperatives.size(); i++) {
                            CooperativeJSON c = currentCooperatives.get(i);
                            if (c.getId().equals(coopId) || c.getNome().equals(coopId)) {
                                rvCooperatives.smoothScrollToPosition(i);
                                break;
                            }
                        }
                    });
                }
            }
        }, "AndroidBridge");

        webViewCoopsMap.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });
        webViewCoopsMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                isMapReady = true;
                if (mapProgressBar != null) {
                    mapProgressBar.setVisibility(View.GONE);
                }
                if (imgCoopsMapPlaceholder != null) {
                    imgCoopsMapPlaceholder.setVisibility(View.GONE);
                }
                if (webViewCoopsMap != null) {
                    webViewCoopsMap.setVisibility(View.VISIBLE);
                }
                if (!currentCooperatives.isEmpty()) {
                    syncMapMarkers();
                }
            }
        });
        webViewCoopsMap.loadUrl("file:///android_asset/coops_map.html");
    }

    private void focusOnCooperative(CooperativeJSON coop) {
        if (coop == null || coop.getLatitude() == null || coop.getLongitude() == null) {
            return;
        }
        if (webViewCoopsMap != null && isMapReady) {
            String script = String.format(java.util.Locale.US,
                    "javascript:focusCoop(%f, %f, '%s');",
                    coop.getLatitude(), coop.getLongitude(), coop.getId());
            webViewCoopsMap.evaluateJavascript(script, null);
        }
    }

    @Nullable
    private Location getBestLastKnownLocation() {
        try {
            if (getContext() != null) {
                LocationManager lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
                if (lm != null) {
                    if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        Location best = null;
                        List<String> providers = lm.getProviders(true);
                        for (String provider : providers) {
                            Location l = lm.getLastKnownLocation(provider);
                            if (l != null) {
                                if (best == null || l.getAccuracy() < best.getAccuracy() || l.getTime() > best.getTime()) {
                                    best = l;
                                }
                            }
                        }
                        return best;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void startLocationUpdates() {
        if (getContext() == null) return;
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) return;

            Location best = getBestLastKnownLocation();
            if (best != null) {
                onUserLocationAcquired(best.getLatitude(), best.getLongitude());
            }

            if (locationListener == null) {
                locationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        if (!isAdded()) return;
                        onUserLocationAcquired(location.getLatitude(), location.getLongitude());
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {}

                    @Override
                    public void onProviderEnabled(@NonNull String provider) {}

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {}
                };
            }

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        2500L,
                        3.0f,
                        locationListener,
                        Looper.getMainLooper()
                );
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        2500L,
                        3.0f,
                        locationListener,
                        Looper.getMainLooper()
                );
            }
        } catch (Exception e) {
            android.util.Log.w("CooperativesFragment", "Aviso ao iniciar GPS: " + e.getMessage());
        }
    }

    private void onUserLocationAcquired(double lat, double lng) {
        if (!isAdded()) return;

        currentUserLat = lat;
        currentUserLng = lng;
        hasRealLocation = true;

        if (!currentCooperatives.isEmpty()) {
            for (CooperativeJSON c : currentCooperatives) {
                if (c.getLatitude() != null && c.getLongitude() != null) {
                    double dist = CooperativeJSON.calculateDistanceKm(
                            lat, lng, c.getLatitude(), c.getLongitude());
                    c.setDistanciaKm(dist);
                }
            }
            currentCooperatives.sort((a, b) -> Double.compare(a.getDistanciaKm(), b.getDistanciaKm()));
            if (!currentCooperatives.isEmpty()) {
                currentCooperatives.get(0).setTag("MAIS PRÓXIMA");
            }
            adapter.setItems(currentCooperatives);
        }

        if (webViewCoopsMap != null && isMapReady) {
            String script = String.format(java.util.Locale.US,
                    "javascript:updateUserLocation(%f, %f);", lat, lng);
            webViewCoopsMap.evaluateJavascript(script, null);
        }
    }

    private double getUserLatitude() {
        if (hasRealLocation) {
            return currentUserLat;
        }
        Location loc = getBestLastKnownLocation();
        if (loc != null) {
            currentUserLat = loc.getLatitude();
            hasRealLocation = true;
            return currentUserLat;
        }
        return DEFAULT_UNIT_LAT;
    }

    private double getUserLongitude() {
        if (hasRealLocation) {
            return currentUserLng;
        }
        Location loc = getBestLastKnownLocation();
        if (loc != null) {
            currentUserLng = loc.getLongitude();
            hasRealLocation = true;
            return currentUserLng;
        }
        return DEFAULT_UNIT_LNG;
    }

    private void loadCooperativesFromDatabase() {
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
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    processAndDisplayDatabaseCooperatives(response.body());
                } else {

                    fetchCooperativesAsService();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<CooperativeResponseDTO>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                fetchCooperativesAsService();
            }
        });
    }

    private void processAndDisplayDatabaseCooperatives(List<CooperativeResponseDTO> dtos) {
        double userLat = getUserLatitude();
        double userLng = getUserLongitude();

        List<CooperativeJSON> list = new ArrayList<>();
        for (CooperativeResponseDTO dto : dtos) {
            CooperativeJSON c = CooperativeJSON.fromDTO(dto, userLat, userLng);
            if (c != null) {
                list.add(c);
            }
        }

        list.sort((a, b) -> Double.compare(a.getDistanciaKm(), b.getDistanciaKm()));

        if (!list.isEmpty()) {
            list.get(0).setTag("MAIS PRÓXIMA");
        }

        currentCooperatives.clear();
        currentCooperatives.addAll(list);

        PrefsHelper.putList(requireContext(), PREFS, KEY_LIST, list);
        applyCooperatives(list);
        syncMapMarkers();
        hideLoading();
    }

    private void fetchCooperativesAsService() {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build();

                JsonObject loginBody = new JsonObject();
                loginBody.addProperty("email", ApiClient.ADMIN_EMAIL);
                loginBody.addProperty("password", ApiClient.ADMIN_PASSWORD);
                Request loginReq = new Request.Builder()
                        .url(ApiClient.BASE_URL_REAL + "auth/login")
                        .header("Accept", "application/json")
                        .post(RequestBody.create(loginBody.toString(), MediaType.parse("application/json")))
                        .build();

                okhttp3.Response loginResp = client.newCall(loginReq).execute();
                if (loginResp.isSuccessful() && loginResp.body() != null) {
                    JsonObject respObj = JsonParser.parseString(loginResp.body().string()).getAsJsonObject();
                    String adminToken = respObj.has("token") ? respObj.get("token").getAsString() :
                            (respObj.has("accessToken") ? respObj.get("accessToken").getAsString() : null);
                    if (adminToken != null) {
                        Request coopReq = new Request.Builder()
                                .url(ApiClient.BASE_URL_REAL + "cooperatives")
                                .header("Authorization", "Bearer " + adminToken)
                                .header("X-Company-Id", ApiClient.ADMIN_COMPANY_ID)
                                .header("Accept", "application/json")
                                .get()
                                .build();
                        okhttp3.Response coopResp = client.newCall(coopReq).execute();
                        if (coopResp.isSuccessful() && coopResp.body() != null) {
                            Type listType = new TypeToken<List<CooperativeResponseDTO>>() {}.getType();
                            List<CooperativeResponseDTO> dtos = gson.fromJson(coopResp.body().string(), listType);
                            if (dtos != null && !dtos.isEmpty()) {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        if (!isAdded()) return;
                                        processAndDisplayDatabaseCooperatives(dtos);
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
                    useCachedDatabaseCooperatives();
                });
            }
        }).start();
    }

    private void useCachedDatabaseCooperatives() {
        List<CooperativeJSON> cached = PrefsHelper.getList(requireContext(), PREFS, KEY_LIST,
                PrefsHelper.listType(CooperativeJSON.class));
        if (cached != null && !cached.isEmpty()) {
            currentCooperatives.clear();
            currentCooperatives.addAll(cached);
            applyCooperatives(cached);
            syncMapMarkers();
            Toast.makeText(requireContext(), "Exibindo cooperativas salvas em cache.", Toast.LENGTH_SHORT).show();
        } else {
            if (tvEmptyCooperatives != null) {
                tvEmptyCooperatives.setVisibility(View.VISIBLE);
            }
        }
        hideLoading();
    }

    private void syncMapMarkers() {
        if (webViewCoopsMap == null || !isMapReady || currentCooperatives.isEmpty()) {
            return;
        }

        double userLat = getUserLatitude();
        double userLng = getUserLongitude();
        String jsonCoops = gson.toJson(currentCooperatives);

        String script = "javascript:renderCooperatives(" + userLat + ", " + userLng + ", " + jsonCoops + ");";
        webViewCoopsMap.evaluateJavascript(script, null);

        if (tvMapBadgeCount != null) {
            int count = currentCooperatives.size();
            String label = count == 1 ? "1 cooperativa parceira" : count + " cooperativas parceiras";
            tvMapBadgeCount.setText(label);
        }
    }

    private void applyCooperatives(List<CooperativeJSON> cooperatives) {
        adapter.setItems(cooperatives);
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

    @Override
    public void onDestroyView() {
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (Exception ignored) {
            }
            locationListener = null;
        }
        if (webViewCoopsMap != null) {
            webViewCoopsMap.destroy();
            webViewCoopsMap = null;
        }
        super.onDestroyView();
    }
}
