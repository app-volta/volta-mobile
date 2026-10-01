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

        // Fazendo aparecer o nome do Projeto na Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Procuro qual é o fragmentContainerView que utilizará a toolbar
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainerView);

        // Instanciando o NavController
        // (sem setupWithNavController(toolbar) duplicado: só o setupActionBar abaixo sincroniza a Toolbar)
        NavController navController = navHostFragment.getNavController();

        // _________________________________________________________________________________________
        //            BOTTOM NAVIGATION VIEW
        // _________________________________________________________________________________________

        BottomNavigationView navView = findViewById(R.id.bottomNavigationView);
        navView.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.bottom_bar_bg));

        // Habilitar menu button (dizendo quais os fragments de nível superior) - 4 tabs reais + FAB central
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_cooperatives, R.id.nav_register, R.id.nav_reports, R.id.nav_profile)
                .build();

        // Ativando a barra de ação sincronizada com o NavController
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // Navegação custom das abas (SEM setupWithNavController nativo e SEM saveState/restoreState):
        // o Register é destino avulso do FAB fora das abas; o multiple-back-stack do NavigationUI
        // corrompia o estado ao alternar FAB <-> abas e travava a aba de origem.
        // Pilha simples: toda troca de aba faz popUpTo(home). Trade-off consciente: trocar de aba
        // recria a tela (não preserva scroll/form) — aceitável no MVP, reversível no futuro.
        navView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            // Ignora a aba placeholder central (espaço do FAB)
            if (itemId == R.id.nav_placeholder) {
                return false;
            }
            // Se já estiver na aba clicada, ignora para não recriar
            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == itemId) {
                return true;
            }
            navigateToTab(navController, itemId);
            return true;
        });
        navView.setOnItemReselectedListener(item -> {
            // Caminho vivo quando o id interno está stale (ex.: volta do Register):
            // navega explícito em vez de ignorar.
            if (item.getItemId() != R.id.nav_placeholder) {
                navigateToTab(navController, item.getItemId());
            }
        });
        // Listener único: Toolbar na Home + desmarca a BottomNav no Register.
        // No Register nenhum item fica verde (processo diferente no futuro).
        navController.addOnDestinationChangedListener((ctrl, destination, args) -> {
            int destId = destination.getId();

            // Ocultar Toolbar onde há header interno (Home, Registrar, Análise — Figma)
            if (destId == R.id.nav_home || destId == R.id.nav_register
                    || destId == R.id.nav_ai_analysis) {
                toolbar.setVisibility(android.view.View.GONE);
            } else {
                toolbar.setVisibility(android.view.View.VISIBLE);
            }

            // Se estiver no Register (FAB), desmarca a seleção da BottomNav (nenhum verde).
            // Alterna o GroupCheckable para desmarcar sem quebrar o listener visual interno.
            // Nunca usa o placeholder (id fora do nav_graph corrompia o selectedItemId).
            if (destId == R.id.nav_register) {
                navView.getMenu().setGroupCheckable(0, true, false);
                for (int i = 0; i < navView.getMenu().size(); i++) {
                    navView.getMenu().getItem(i).setChecked(false);
                }
                navView.getMenu().setGroupCheckable(0, true, true);
            } else {
                // Sincroniza o item selecionado na BottomNav com a tela atual
                android.view.MenuItem menuItem = navView.getMenu().findItem(destId);
                if (menuItem != null) {
                    menuItem.setChecked(true);
                }
            }
        });

        // FAB Central - navega para Registrar com pilha simples (singleTop + popUpTo home, sem saveState)
        android.view.View fabRegister = findViewById(R.id.fabRegister);
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
    }

    // Pilha simples sem saveState/restoreState para as abas: evita corromper o estado
    // ao alternar entre o Register (destino avulso do FAB) e as abas.
    private void navigateToTab(NavController navController, int destId) {
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == destId) {
            return; // já está no destino
        }
        NavOptions navOptions = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.nav_home, false)
                .build();
        navController.navigate(destId, null, navOptions);
    }

    // Suporte ao botão "Voltar" da Toolbar
    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainerView);
        return navHostFragment.getNavController().navigateUp() || super.onSupportNavigateUp();
    }
}