package com.aula.volta;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
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
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(toolbar, navController);

        // _________________________________________________________________________________________
        //            BOTTOM NAVIGATION VIEW
        // _________________________________________________________________________________________

        BottomNavigationView navView = findViewById(R.id.bottomNavigationView);

        // Habilitar menu button (dizendo quais os fragments de nível superior) - 4 tabs reais + FAB central
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_occurrences, R.id.nav_register, R.id.nav_reports, R.id.nav_profile)
                .build();

        // Ativando a barra de ação sincronizada com o NavController
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        // Custom handling para BottomNav com placeholder central (Figma iconBar.svg)
        // setup manual para ignorar placeholder e manter FAB como nav_register
        navView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_placeholder) {
                return false;
            }
            return NavigationUI.onNavDestinationSelected(item, navController);
        });
        // Sincroniza seleção visual quando navega via FAB ou NavController
        navController.addOnDestinationChangedListener((controller, destination, args) -> {
            int destId = destination.getId();
            if (destId == R.id.nav_register) {
                // FAB central não tem item no bottom bar, limpa seleção ou mantém anterior
                navView.getMenu().findItem(R.id.nav_placeholder).setChecked(true);
            } else {
                // seleciona item correspondente
                if (destId == R.id.nav_home || destId == R.id.nav_occurrences
                        || destId == R.id.nav_reports || destId == R.id.nav_profile) {
                    navView.getMenu().findItem(destId).setChecked(true);
                }
            }
        });

        // FAB Central - navega para Registrar (Figma: círculo verde com câmera)
        com.google.android.material.floatingactionbutton.FloatingActionButton fabRegister = findViewById(R.id.fabRegister);
        fabRegister.setOnClickListener(v -> navController.navigate(R.id.nav_register));

        // Ocultar Toolbar na Home (header é interno ao fragmento)
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.nav_home) {
                toolbar.setVisibility(android.view.View.GONE);
            } else {
                toolbar.setVisibility(android.view.View.VISIBLE);
            }
        });
    }
}