package com.zhiwu.wallpaper;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

/**
 * Live wallpaper service. Rendering happens on a dedicated thread so the
 * launcher's main thread is never blocked by a frame.
 */
public final class MeridianWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new MeridianEngine();
    }

    private final class MeridianEngine extends Engine
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        private SharedPreferences preferences;
        private WallpaperRenderer renderer;
        private RenderThread thread;
        private Settings currentSettings;
        private boolean visible = true;
        private boolean destroyed;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setOffsetNotificationsEnabled(true);
            Context context = getApplicationContext();
            preferences = context.getSharedPreferences(Settings.PREFS, Context.MODE_PRIVATE);
            preferences.registerOnSharedPreferenceChangeListener(this);
            currentSettings = Settings.load(context);
            renderer = new WallpaperRenderer(currentSettings);
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            if (destroyed) {
                return;
            }
            if (thread != null) {
                thread.quit();
                thread.joinQuietly();
            }
            thread = new RenderThread(holder, renderer);
            thread.setVisible(visible);
            if (currentSettings != null) {
                thread.setInterval(currentSettings.motion ? 41L : 1000L);
            }
            thread.start();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            if (renderer != null) {
                renderer.setSize(width, height);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            this.visible = visible;
            if (thread != null) {
                thread.setVisible(visible);
            }
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep,
                                     float yOffsetStep, int xPixelOffset, int yPixelOffset) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep,
                    xPixelOffset, yPixelOffset);
            if (renderer != null) {
                renderer.setOffset(xOffset);
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            if (thread != null) {
                thread.quit();
                thread.joinQuietly();
                thread = null;
            }
            super.onSurfaceDestroyed(holder);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Settings updated = Settings.load(getApplicationContext());
            currentSettings = updated;
            if (renderer != null) {
                renderer.setSettings(updated);
            }
            if (thread != null) {
                thread.setInterval(updated.motion ? 41L : 1000L);
            }
        }

        @Override
        public void onDestroy() {
            destroyed = true;
            if (thread != null) {
                thread.quit();
                thread.joinQuietly();
                thread = null;
            }
            if (preferences != null) {
                preferences.unregisterOnSharedPreferenceChangeListener(this);
            }
            super.onDestroy();
        }
    }

    private static final class RenderThread extends Thread {

        private final SurfaceHolder holder;
        private final WallpaperRenderer renderer;
        private volatile boolean running = true;
        private volatile boolean visible = true;
        private volatile long interval = 41L;

        RenderThread(SurfaceHolder holder, WallpaperRenderer renderer) {
            super("ZhiwuWallpaper");
            this.holder = holder;
            this.renderer = renderer;
            setPriority(Thread.NORM_PRIORITY - 1);
        }

        void setVisible(boolean visible) {
            this.visible = visible;
        }

        void setInterval(long interval) {
            this.interval = Math.max(33L, interval);
        }

        void quit() {
            running = false;
            interrupt();
        }

        void joinQuietly() {
            try {
                join(1200L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void run() {
            while (running) {
                if (!visible) {
                    sleepQuietly(180L);
                    continue;
                }
                if (!holder.getSurface().isValid()) {
                    sleepQuietly(120L);
                    continue;
                }
                long start = System.currentTimeMillis();
                Canvas canvas = null;
                try {
                    canvas = holder.lockCanvas();
                    if (canvas != null) {
                        renderer.draw(canvas, start);
                    }
                } catch (RuntimeException ignored) {
                    // The surface can disappear between isValid() and lockCanvas().
                } finally {
                    if (canvas != null) {
                        try {
                            holder.unlockCanvasAndPost(canvas);
                        } catch (RuntimeException ignored) {
                            // Surface already gone; the next loop will wait for a new one.
                        }
                    }
                }
                long spent = System.currentTimeMillis() - start;
                long wait = interval - spent;
                if (wait > 0L) {
                    sleepQuietly(wait);
                }
            }
        }

        private void sleepQuietly(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ignored) {
                // quit() interrupts the thread.
            }
        }
    }
}
