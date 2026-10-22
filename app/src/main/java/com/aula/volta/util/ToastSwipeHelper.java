package com.aula.volta.util;

import android.view.MotionEvent;
import android.view.View;

/**
 * Utilitário de gestos para notificações in-app flutuantes (toasts de topo).
 * Suporta:
 *  - Auto-dismiss temporizado (4s)
 *  - Deslizar para o lado (esquerda/direita) ou para cima -> descarta a notificação
 *  - Deslizar para baixo -> abre o relatório técnico em PDF (ou funcionalidade vinculada)
 */
public final class ToastSwipeHelper {

    public interface OnSwipeListener {
        void onDismissed();
        void onSwipeDown();
    }

    private ToastSwipeHelper() {
    }

    public static void attach(View toastView, Runnable dismissRunnable, OnSwipeListener listener) {
        if (toastView == null) {
            return;
        }

        toastView.setOnTouchListener(new View.OnTouchListener() {
            private float downX;
            private float downY;
            private boolean isTracking = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX();
                        downY = event.getRawY();
                        isTracking = true;
                        if (dismissRunnable != null) {
                            v.removeCallbacks(dismissRunnable);
                        }
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        if (!isTracking) {
                            return false;
                        }
                        float dx = event.getRawX() - downX;
                        float dy = event.getRawY() - downY;
                        if (Math.abs(dx) > Math.abs(dy)) {
                            v.setTranslationX(dx);
                            float alpha = 1f - Math.min(1f, Math.abs(dx) / (float) Math.max(1, v.getWidth()));
                            v.setAlpha(Math.max(0.2f, alpha));
                        } else if (dy < 0) { // deslizando para cima
                            v.setTranslationY(dy);
                            float alpha = 1f - Math.min(1f, Math.abs(dy) / (float) Math.max(1, v.getHeight()));
                            v.setAlpha(Math.max(0.2f, alpha));
                        } else if (dy > 0) { // deslizando para baixo
                            v.setTranslationY(dy * 0.4f);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (!isTracking) {
                            return false;
                        }
                        isTracking = false;
                        float totalDx = event.getRawX() - downX;
                        float totalDy = event.getRawY() - downY;

                        // 1. Deslizar para baixo -> dispara abertura do relatório PDF
                        if (totalDy > 80 && Math.abs(totalDy) > Math.abs(totalDx)) {
                            v.animate().translationY(0f).alpha(1f).setDuration(200).start();
                            if (listener != null) {
                                listener.onSwipeDown();
                            }
                            return true;
                        }

                        // 2. Deslizar para os lados -> descarta o banner
                        if (Math.abs(totalDx) > 100) {
                            float targetX = totalDx > 0 ? v.getWidth() : -v.getWidth();
                            v.animate()
                                    .translationX(targetX)
                                    .alpha(0f)
                                    .setDuration(250)
                                    .withEndAction(() -> {
                                        v.setVisibility(View.GONE);
                                        v.setTranslationX(0f);
                                        v.setTranslationY(0f);
                                        if (listener != null) {
                                            listener.onDismissed();
                                        }
                                    })
                                    .start();
                            return true;
                        }

                        // 3. Deslizar para cima -> descarta o banner
                        if (totalDy < -60) {
                            v.animate()
                                    .translationY(-v.getHeight())
                                    .alpha(0f)
                                    .setDuration(250)
                                    .withEndAction(() -> {
                                        v.setVisibility(View.GONE);
                                        v.setTranslationX(0f);
                                        v.setTranslationY(0f);
                                        if (listener != null) {
                                            listener.onDismissed();
                                        }
                                    })
                                    .start();
                            return true;
                        }

                        // 4. Clique ou pequeno toque -> restaura posição e reativa timeout de 4s
                        v.animate().translationX(0f).translationY(0f).alpha(1f).setDuration(200).start();
                        if (Math.abs(totalDx) < 15 && Math.abs(totalDy) < 15) {
                            v.performClick();
                        }
                        if (dismissRunnable != null) {
                            v.postDelayed(dismissRunnable, 4000);
                        }
                        return true;
                }
                return false;
            }
        });
    }
}
