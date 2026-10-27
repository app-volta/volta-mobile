package com.aula.volta.util;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Utilitário para gerenciar animação suave de pulso (shimmer/skeleton)
 * em layouts de carregamento antes do retorno de APIs.
 */
public final class SkeletonPulseAnimator {

    private static final Map<View, ValueAnimator> animators = new WeakHashMap<>();

    private SkeletonPulseAnimator() {
    }

    /**
     * Inicia a animação de pulso contínuo (alpha 0.45f -> 1.0f -> 0.45f).
     */
    public static void start(View view) {
        if (view == null) return;
        stop(view);
        ValueAnimator anim = ValueAnimator.ofFloat(0.45f, 1.0f);
        anim.setDuration(750);
        anim.setRepeatMode(ValueAnimator.REVERSE);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addUpdateListener(animation -> {
            float val = (Float) animation.getAnimatedValue();
            view.setAlpha(val);
        });
        anim.start();
        animators.put(view, anim);
    }

    /**
     * Interrompe o pulso e restaura a opacidade do elemento para 1.0f.
     */
    public static void stop(View view) {
        if (view == null) return;
        ValueAnimator anim = animators.remove(view);
        if (anim != null) {
            anim.cancel();
        }
        view.setAlpha(1.0f);
    }
}
