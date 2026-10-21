package com.aula.volta.util;

import android.app.Dialog;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

/**
 * Utilitário para garantir que todos os BottomSheets e Dialogs do VOLTA:
 * 1. Não ativem a barra de navegação do sistema ("barra de ações") ao abrir.
 * 2. Iniciem exatamente na base física do celular (bottom = 0), sem espaço ou padding da navigation bar.
 * 3. Desloquem apenas quando o teclado virtual (IME) estiver visível.
 */
public final class BottomSheetImmersiveHelper {

    private BottomSheetImmersiveHelper() {
    }

    public static void configureBottomSheet(BottomSheetDialog dialog) {
        if (dialog == null) return;

        Window window = dialog.getWindow();
        if (window != null) {
            // Impede que o WindowManager ative a barra de ações ao focar a nova janela antes do hide()
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            applyImmersiveWindow(window);

            View decorView = window.getDecorView();
            decorView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(@NonNull View v) {
                    // Sobrescreve imediatamente qualquer flag aplicada pelo BottomSheetDialog.onAttachedToWindow()
                    applyImmersiveWindow(window);
                    stripBottomSheetInsets(dialog);
                }

                @Override
                public void onViewDetachedFromWindow(@NonNull View v) {
                }
            });

            decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
                if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                    applyImmersiveWindow(window);
                }
            });
        }

        dialog.setOnShowListener(d -> {
            stripBottomSheetInsets(dialog);
            Window w = dialog.getWindow();
            if (w != null) {
                applyImmersiveWindow(w);
                w.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                applyImmersiveWindow(w);
                w.getDecorView().post(() -> {
                    applyImmersiveWindow(w);
                    stripBottomSheetInsets(dialog);
                });
            }
        });
    }

    private static void stripBottomSheetInsets(BottomSheetDialog dialog) {
        View container = dialog.findViewById(com.google.android.material.R.id.container);
        if (container != null) {
            container.setFitsSystemWindows(false);
            container.setPadding(0, 0, 0, 0);
            ViewCompat.setOnApplyWindowInsetsListener(container, (v, insets) -> {
                Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                v.setPadding(0, 0, 0, ime.bottom);
                return WindowInsetsCompat.CONSUMED;
            });
        }

        View coordinator = dialog.findViewById(com.google.android.material.R.id.coordinator);
        if (coordinator != null) {
            coordinator.setFitsSystemWindows(false);
            coordinator.setPadding(0, 0, 0, 0);
            ViewCompat.setOnApplyWindowInsetsListener(coordinator, (v, insets) -> {
                v.setPadding(0, 0, 0, 0);
                return WindowInsetsCompat.CONSUMED;
            });
        }

        FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setFitsSystemWindows(false);
            bottomSheet.setBackgroundResource(android.R.color.transparent);
            bottomSheet.setPadding(0, 0, 0, 0);
            ViewCompat.setOnApplyWindowInsetsListener(bottomSheet, (v, insets) -> {
                v.setPadding(0, 0, 0, 0);
                return WindowInsetsCompat.CONSUMED;
            });

            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setGestureInsetBottomIgnored(true);
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    public static void configureDialog(Dialog dialog) {
        if (dialog == null) return;
        Window window = dialog.getWindow();
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            applyImmersiveWindow(window);
            window.getDecorView().addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(@NonNull View v) {
                    applyImmersiveWindow(window);
                }

                @Override
                public void onViewDetachedFromWindow(@NonNull View v) {
                }
            });
        }
        dialog.setOnShowListener(d -> {
            Window w = dialog.getWindow();
            if (w != null) {
                applyImmersiveWindow(w);
                w.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                applyImmersiveWindow(w);
            }
        });
    }

    @SuppressWarnings("deprecation")
    public static void applyImmersiveWindow(Window window) {
        if (window == null) return;
        WindowCompat.setDecorFitsSystemWindows(window, false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(lp);
        }

        View decorView = window.getDecorView();
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, decorView);
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.navigationBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }

        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | flags);
    }
}
