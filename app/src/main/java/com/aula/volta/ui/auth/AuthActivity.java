package com.aula.volta.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.aula.volta.MainActivity;
import com.aula.volta.R;
import com.aula.volta.data.local.SessionManager;

/**
 * Fluxo de acesso — Figma 790:955+ (Fase 13, mock local).
 * Launcher do app: com sessão vai direto à Main.
 */
public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (SessionManager.isLoggedIn(this)) {
            goMain();
            return;
        }

        setContentView(R.layout.activity_auth);

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.authContainer);
        if (navHost != null) {
            NavController navController = navHost.getNavController();
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();
                boolean isLightScreen = (id == R.id.auth_register
                        || id == R.id.auth_confirm
                        || id == R.id.auth_forgot
                        || id == R.id.auth_passcode
                        || id == R.id.auth_reset);

                WindowInsetsControllerCompat insetsController =
                        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
                if (insetsController != null) {
                    insetsController.setAppearanceLightStatusBars(isLightScreen);
                }
                if (isLightScreen) {
                    getWindow().setStatusBarColor(getColor(R.color.vf_bg));
                } else {
                    getWindow().setStatusBarColor(getColor(R.color.cta_gradient_end));
                }
            });
        }
    }

    private void goMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** Chamado pelas telas ao concluir login/cadastro. */
    public void finishLogin() {
        goMain();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.authContainer);
        return navHost != null && navHost.getNavController().navigateUp()
                || super.onSupportNavigateUp();
    }
}
