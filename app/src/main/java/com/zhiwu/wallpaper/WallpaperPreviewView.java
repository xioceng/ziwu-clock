package com.zhiwu.wallpaper;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

/** Animated in-app preview that uses the same renderer as the wallpaper. */
public final class WallpaperPreviewView extends View {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final WallpaperRenderer renderer;
    private Settings settings;
    private boolean running;
    private boolean attached;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            invalidate();
            handler.postDelayed(this, settings.motion ? 33L : 500L);
        }
    };

    public WallpaperPreviewView(Context context) {
        this(context, null);
    }

    public WallpaperPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        settings = Settings.defaults();
        renderer = new WallpaperRenderer(settings);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
        renderer.setSettings(settings);
        invalidate();
    }

    public void start() {
        if (!attached) {
            return;
        }
        if (!running) {
            running = true;
            handler.removeCallbacks(tick);
            handler.post(tick);
        }
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(tick);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        start();
    }

    @Override
    protected void onDetachedFromWindow() {
        attached = false;
        stop();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE && attached) {
            start();
        } else {
            stop();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        renderer.setSize(w, h);
        renderer.setOffset(0.5f);
        renderer.draw(canvas, System.currentTimeMillis());
    }
}
