package com.aula.volta;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!com.aula.volta.data.local.SessionManager.isLoggedIn(this)) {
            android.content.Intent intent = new android.content.Intent(
                    this, com.aula.volta.ui.auth.AuthActivity.class);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        androidx.core.view.WindowInsetsControllerCompat controller =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        if (controller != null) {

            controller.hide(androidx.core.view.WindowInsetsCompat.Type.navigationBars());

            controller.setSystemBarsBehavior(
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {

            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(statusBars.left, statusBars.top, statusBars.right, 0);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainerView);

        NavController navController = navHostFragment.getNavController();

        BottomNavigationView navView = findViewById(R.id.bottomNavigationView);
        navView.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.bottom_bar_bg));

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_cooperatives, R.id.nav_register, R.id.nav_reports, R.id.nav_profile)
                .build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        navView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_placeholder) {
                return false;
            }
            if (itemId == R.id.nav_register && !com.aula.volta.util.NetworkUtils.isOnline(this)) {
                navigateToOfflineCameraBlocked(navController);
                return false;
            }
            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == itemId) {
                return true;
            }
            navigateToTab(navController, itemId);
            return true;
        });
        navView.setOnItemReselectedListener(item -> {
            if (item.getItemId() != R.id.nav_placeholder) {
                if (item.getItemId() == R.id.nav_register && !com.aula.volta.util.NetworkUtils.isOnline(this)) {
                    navigateToOfflineCameraBlocked(navController);
                    return;
                }
                navigateToTab(navController, item.getItemId());
            }
        });

        android.view.View fabRegister = findViewById(R.id.fabRegister);
        android.view.View tabBarBorder = findViewById(R.id.tabBarBorder);

        navController.addOnDestinationChangedListener((ctrl, destination, args) -> {
            int destId = destination.getId();

            toolbar.setVisibility(android.view.View.GONE);

            boolean hideBottomBar = (destId == R.id.nav_assistant

                    || destId == R.id.nav_register
                    || destId == R.id.nav_ai_analysis
                    || destId == R.id.nav_occurrence_full
                    || destId == R.id.nav_offline_error);
            int barVis = hideBottomBar ? android.view.View.GONE : android.view.View.VISIBLE;
            navView.setVisibility(barVis);
            if (fabRegister != null) {
                fabRegister.setVisibility(barVis);
            }
            if (tabBarBorder != null) {
                tabBarBorder.setVisibility(barVis);
            }

            if (destId == R.id.nav_register) {
                navView.getMenu().setGroupCheckable(0, true, false);
                for (int i = 0; i < navView.getMenu().size(); i++) {
                    navView.getMenu().getItem(i).setChecked(false);
                }
                navView.getMenu().setGroupCheckable(0, true, true);
            } else {
                android.view.MenuItem menuItem = navView.getMenu().findItem(destId);
                if (menuItem != null) {
                    menuItem.setChecked(true);
                }
            }
        });

        fabRegister.setOnClickListener(v -> {
            if (!com.aula.volta.util.NetworkUtils.isOnline(this)) {
                navigateToOfflineCameraBlocked(navController);
                return;
            }
            if (navController.getCurrentDestination() == null
                    || navController.getCurrentDestination().getId() != R.id.nav_register) {
                NavOptions fabOptions = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setPopUpTo(R.id.nav_home, false)
                        .build();
                navController.navigate(R.id.nav_register, null, fabOptions);
            }
        });

        com.aula.volta.data.notification.NotificationHelper.createChannels(this);

        com.aula.volta.data.notification.FirestoreNotificationRepository.getInstance().startListening(this);

        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        android.util.Log.d("VoltaFCM", "FCM Device Token: " + task.getResult());
                    }
                });
    }

    private void navigateToOfflineCameraBlocked(NavController navController) {
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
        try {
            navController.navigate(R.id.nav_offline_error, errArgs);
        } catch (Exception ignored) {
        }
    }

    private void navigateToTab(NavController navController, int destId) {
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == destId) {
            return;
        }
        NavOptions navOptions = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.nav_home, false)
                .build();
        navController.navigate(destId, null, navOptions);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            com.aula.volta.util.BottomSheetImmersiveHelper.applyImmersiveWindow(getWindow());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainerView);
        return navHostFragment.getNavController().navigateUp() || super.onSupportNavigateUp();
    }
    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (ev.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            android.view.View v = getCurrentFocus();
            if (v instanceof android.widget.EditText) {
                android.graphics.Rect outRect = new android.graphics.Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                    v.clearFocus();
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

}
