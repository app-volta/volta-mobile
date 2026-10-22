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

        // Gate de sessão (Fase 13): sem login volta ao fluxo de acesso.
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

        // =========================================================================
        // 1. OCULTAR A BARRA DE NAVEGAÇÃO DO SISTEMA (VOLTAR, HOME, APPS)
        // =========================================================================
        androidx.core.view.WindowInsetsControllerCompat controller =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        if (controller != null) {
            // Esconde os botões de navegação
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.navigationBars());

            // Faz a barra reaparecer temporariamente só se o usuário deslizar da borda
            controller.setSystemBarsBehavior(
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }

        // =========================================================================
        // 2. AJUSTAR O PADDING DO LAYOUT (Apenas para o topo / barra de status)
        // =========================================================================
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            // statusBars() para não aplicar padding desnecessário na parte inferior
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(statusBars.left, statusBars.top, statusBars.right, 0);
            return insets;
        });

        // _________________________________________________________________________________________
        //            TOOLBAR
        // _________________________________________________________________________________________

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainerView);

        NavController navController = navHostFragment.getNavController();

        // _________________________________________________________________________________________
        //            BOTTOM NAVIGATION VIEW (Figma 790:1736: fundo #FFFFFF + borda superior #EEF0F5)
        // _________________________________________________________________________________________

        BottomNavigationView navView = findViewById(R.id.bottomNavigationView);
        navView.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.bottom_bar_bg));

        // Habilitar menu button (dizendo quais os fragments de nível superior) - 4 tabs reais + FAB central
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_cooperatives, R.id.nav_register, R.id.nav_reports, R.id.nav_profile)
                .build();

        // Ativando a barra de ação sincronizada com o NavController
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        navView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_placeholder) {
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
                navigateToTab(navController, item.getItemId());
            }
        });

        android.view.View fabRegister = findViewById(R.id.fabRegister);
        android.view.View tabBarBorder = findViewById(R.id.tabBarBorder);

        navController.addOnDestinationChangedListener((ctrl, destination, args) -> {
            int destId = destination.getId();

            // Todos os fragmentos possuem layout próprio no Figma sem Toolbar nativa
            toolbar.setVisibility(android.view.View.GONE);

            // Fluxo de registro (851:3792), Assistente (790:1911) e Chat (790:3191) não exibem a Tab bar inferior
            boolean hideBottomBar = (destId == R.id.nav_assistant
                    || destId == R.id.nav_chat
                    || destId == R.id.nav_register
                    || destId == R.id.nav_ai_analysis
                    || destId == R.id.nav_occurrence_full);
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

        // FAB Central - navega para Registrar com pilha simples (singleTop + popUpTo home, sem saveState)
        fabRegister.setOnClickListener(v -> {
            if (navController.getCurrentDestination() == null
                    || navController.getCurrentDestination().getId() != R.id.nav_register) {
                NavOptions fabOptions = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setPopUpTo(R.id.nav_home, false)
                        .build();
                navController.navigate(R.id.nav_register, null, fabOptions);
            }
        });

        // Canais de Notificação Android (Fase 26)
        com.aula.volta.data.notification.NotificationHelper.createChannels(this);

        // Notificações em Tempo Real via Firebase Cloud Firestore
        com.aula.volta.data.notification.FirestoreNotificationRepository.getInstance().startListening(this);

        // Obtenção do Token FCM (Cloud Messaging)
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        android.util.Log.d("VoltaFCM", "FCM Device Token: " + task.getResult());
                    }
                });

        // Sincronização Offline Industrial (Fase 25)
        com.aula.volta.data.sync.SyncManager.registerNetworkCallback(this);
        com.aula.volta.data.sync.SyncManager.syncPending(this);
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
