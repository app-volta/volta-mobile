package com.aula.volta.ui.widget;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * ImageView interativo com suporte a Pinch-to-Zoom (pinça) e Double-Tap (toque duplo).
 * Permite arrastar (pan) quando a imagem estiver com zoom.
 */
public class ZoomableImageView extends AppCompatImageView {

    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();

    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    private PointF start = new PointF();
    private PointF mid = new PointF();

    private float currentScale = 1f;
    private static final float MIN_SCALE = 1f;
    private static final float MAX_SCALE = 4f;

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private boolean isInitialized = false;

    public ZoomableImageView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed || !isInitialized) {
            fitImageToCenter();
        }
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        fitImageToCenter();
    }

    public void fitImageToCenter() {
        Drawable drawable = getDrawable();
        if (drawable == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }

        int dWidth = drawable.getIntrinsicWidth();
        int dHeight = drawable.getIntrinsicHeight();
        if (dWidth <= 0 || dHeight <= 0) {
            return;
        }

        float vWidth = getWidth();
        float vHeight = getHeight();

        float scaleX = vWidth / dWidth;
        float scaleY = vHeight / dHeight;
        float scale = Math.min(scaleX, scaleY);

        float dx = (vWidth - dWidth * scale) * 0.5f;
        float dy = (vHeight - dHeight * scale) * 0.5f;

        matrix.reset();
        matrix.postScale(scale, scale);
        matrix.postTranslate(dx, dy);
        setImageMatrix(matrix);

        currentScale = 1f;
        isInitialized = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        scaleDetector.onTouchEvent(event);

        PointF curr = new PointF(event.getX(), event.getY());

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                mode = DRAG;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                midPoint(mid, event);
                mode = ZOOM;
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                checkMatrixBounds();
                break;

            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG && currentScale > MIN_SCALE) {
                    float dx = curr.x - start.x;
                    float dy = curr.y - start.y;
                    matrix.set(savedMatrix);
                    matrix.postTranslate(dx, dy);
                    checkMatrixBounds();
                }
                break;
        }

        setImageMatrix(matrix);
        return true;
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float mScaleFactor = detector.getScaleFactor();
            float origScale = currentScale;
            currentScale *= mScaleFactor;

            if (currentScale > MAX_SCALE) {
                currentScale = MAX_SCALE;
                mScaleFactor = MAX_SCALE / origScale;
            } else if (currentScale < MIN_SCALE) {
                currentScale = MIN_SCALE;
                mScaleFactor = MIN_SCALE / origScale;
            }

            matrix.postScale(mScaleFactor, mScaleFactor, detector.getFocusX(), detector.getFocusY());
            checkMatrixBounds();
            setImageMatrix(matrix);
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (currentScale > MIN_SCALE) {
                fitImageToCenter();
            } else {
                float targetScale = 2.5f;
                matrix.postScale(targetScale, targetScale, e.getX(), e.getY());
                currentScale = targetScale;
                checkMatrixBounds();
                setImageMatrix(matrix);
            }
            return true;
        }
    }

    private void checkMatrixBounds() {
        RectF rect = new RectF();
        Drawable d = getDrawable();
        if (d == null) return;

        rect.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        matrix.mapRect(rect);

        float width = rect.width();
        float height = rect.height();
        float deltaX = 0;
        float deltaY = 0;

        int viewWidth = getWidth();
        int viewHeight = getHeight();

        if (width < viewWidth) {
            deltaX = (viewWidth - width) / 2 - rect.left;
        } else if (rect.left > 0) {
            deltaX = -rect.left;
        } else if (rect.right < viewWidth) {
            deltaX = viewWidth - rect.right;
        }

        if (height < viewHeight) {
            deltaY = (viewHeight - height) / 2 - rect.top;
        } else if (rect.top > 0) {
            deltaY = -rect.top;
        } else if (rect.bottom < viewHeight) {
            deltaY = viewHeight - rect.bottom;
        }

        matrix.postTranslate(deltaX, deltaY);
    }
}
