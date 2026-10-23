package com.zhiwu.wallpaper;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * Draws the entire wallpaper. This class owns its own bitmaps and Paints and is
 * only drawn from one thread at a time (the wallpaper render thread, or the
 * preview's UI thread).
 */
public final class WallpaperRenderer {

    private static final int DESIGN_WIDTH = 1080;

    private int width;
    private int height;
    private float u;
    private float cx;
    private float cy;
    private float radius;
    private float innerRadius;
    private float headerTop;
    private float headerLine;
    private boolean landscape;

    private volatile int requestedWidth;
    private volatile int requestedHeight;
    private volatile boolean layoutDirty = true;
    private volatile Settings settings = Settings.defaults();
    private volatile float offset = 0.5f;

    private CalendarData calendar;
    private long calendarMinute = Long.MIN_VALUE;
    private Palette palette;
    private int paletteBucket = Integer.MIN_VALUE;
    private Settings paletteSettings;
    private Bitmap background;
    private int backgroundWidth;
    private int backgroundHeight;

    private Bitmap noise;
    private BitmapShader noiseShader;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shaderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final RectF oval = new RectF();
    private final RectF dst = new RectF();
    private final RectF pillRect = new RectF();
    private final Path leafPath = new Path();
    private final Path[] mountainPaths = new Path[3];

    private Typeface serif;
    private Typeface serifBold;
    private Typeface sans;
    private Typeface sansLight;
    private Typeface sansMedium;
    private Typeface thin;
    private Typeface mono;

    private float[] starX;
    private float[] starY;
    private float[] starSize;
    private float[] starPhase;
    private float[] starSpeed;
    private Particle[] particles;

    private Paint vignettePaint;
    private Paint innerPaint;
    private Paint orbHaloPaint;
    private Paint orbDiscPaint;
    private int orbPaintRadius = -1;
    private int orbPaintColor = 0;

    private float parallax;

    public WallpaperRenderer(Settings settings) {
        if (settings != null) {
            this.settings = settings;
        }
        serif = Typeface.create("serif", Typeface.NORMAL);
        serifBold = Typeface.create("serif", Typeface.BOLD);
        sans = Typeface.create("sans-serif", Typeface.NORMAL);
        sansLight = Typeface.create("sans-serif-light", Typeface.NORMAL);
        sansMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        thin = Typeface.create("sans-serif-thin", Typeface.NORMAL);
        mono = Typeface.create("monospace", Typeface.NORMAL);
    }

    public void setSize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (width != requestedWidth || height != requestedHeight) {
            requestedWidth = width;
            requestedHeight = height;
            layoutDirty = true;
        }
    }

    public void setSettings(Settings settings) {
        if (settings != null && settings != this.settings) {
            this.settings = settings;
        }
    }

    public void setOffset(float xOffset) {
        offset = Math.max(0f, Math.min(1f, xOffset));
    }

    public void draw(Canvas canvas, long nowMillis) {
        int w = requestedWidth;
        int h = requestedHeight;
        if (w <= 0 || h <= 0) {
            return;
        }
        if (layoutDirty || w != width || h != height) {
            layout(w, h);
        }

        Settings s = settings;
        Date now = new Date(nowMillis);
        Calendar time = Calendar.getInstance();
        time.setTime(now);
        int minuteOfDay = time.get(Calendar.HOUR_OF_DAY) * 60 + time.get(Calendar.MINUTE);
        long minute = nowMillis / 60000L;
        if (calendar == null || minute != calendarMinute) {
            calendar = CalendarData.from(now);
            calendarMinute = minute;
        }

        int bucket = minuteOfDay / 5;
        if (palette == null || bucket != paletteBucket || s != paletteSettings) {
            palette = Palette.forSettings(s, minuteOfDay);
            paletteBucket = bucket;
            paletteSettings = s;
            background = null;
            buildVignette();
            buildInnerPaint();
            orbPaintRadius = -1;
        }

        if (background == null || backgroundWidth != width || backgroundHeight != height) {
            buildBackground();
        }

        canvas.drawColor(palette.bgTop);
        parallax = (offset - 0.5f) * width * 0.045f;
        dst.set(-width * 0.025f - parallax, 0f, width * 1.025f - parallax, height);
        canvas.drawBitmap(background, null, dst, bitmapPaint);

        drawStars(canvas, nowMillis / 1000f, s.motion);
        drawOrb(canvas, time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE), s.motion);
        drawParticles(canvas, nowMillis / 1000f, s.motion);
        canvas.drawRect(0f, 0f, width, height, vignettePaint);

        drawHeader(canvas);
        drawDial(canvas, time);
        drawNextShichen(canvas, time);
        drawPillars(canvas);
    }

    // ---------------------------------------------------------------------
    // Layout and background
    // ---------------------------------------------------------------------

    private void layout(int w, int h) {
        width = w;
        height = h;
        landscape = w > h * 1.18f;
        if (landscape) {
            u = h / 1080f;
            cx = w * 0.5f;
            cy = h * 0.45f;
            radius = Math.min(w * 0.18f, h * 0.26f);
            headerTop = h * 0.060f;
        } else {
            u = w / (float) DESIGN_WIDTH;
            cx = w * 0.5f;
            cy = h * 0.455f;
            // Nearly edge-to-edge dial: leave a narrow margin for the outer ticks.
            radius = Math.min(w * 0.445f, h * 0.260f);
            headerTop = Math.max(h * 0.077f, 60f * u);
        }
        innerRadius = radius * 0.600f;
        headerLine = headerTop + 108f * u;
        parallax = 0f;

        buildStars();
        buildMountains();
        buildLeaf();
        buildNoise();
        layoutDirty = false;
        background = null;
        palette = null;
        vignettePaint = null;
        orbPaintRadius = -1;
    }

    private void buildStars() {
        Random random = new Random(20261023L);
        int count = landscape ? 64 : 88;
        starX = new float[count];
        starY = new float[count];
        starSize = new float[count];
        starPhase = new float[count];
        starSpeed = new float[count];
        for (int i = 0; i < count; i++) {
            starX[i] = random.nextFloat();
            starY[i] = random.nextFloat() * 0.70f;
            starSize[i] = 0.7f + random.nextFloat() * 2.1f;
            starPhase[i] = random.nextFloat() * 6.28318f;
            starSpeed[i] = 0.25f + random.nextFloat() * 0.65f;
        }

        Random pr = new Random(77123L);
        particles = new Particle[26];
        for (int i = 0; i < particles.length; i++) {
            Particle p = new Particle();
            p.x = pr.nextFloat();
            p.y = pr.nextFloat();
            p.speed = 8f + pr.nextFloat() * 22f;
            p.sway = 12f + pr.nextFloat() * 34f;
            p.size = 2.2f + pr.nextFloat() * 4.8f;
            p.phase = pr.nextFloat() * 6.28318f;
            p.spin = (pr.nextFloat() - 0.5f) * 1.4f;
            p.alpha = 0.16f + pr.nextFloat() * 0.32f;
            particles[i] = p;
        }
    }

    private void buildMountains() {
        mountainPaths[0] = mountainPath(0.742f, 0.060f, 9, 331L);
        mountainPaths[1] = mountainPath(0.830f, 0.046f, 8, 991L);
        mountainPaths[2] = mountainPath(0.925f, 0.030f, 7, 1777L);
    }

    private Path mountainPath(float baseFraction, float ampFraction, int points, long seed) {
        Random r = new Random(seed);
        float margin = width * 0.06f;
        float base = height * baseFraction;
        float amp = height * ampFraction;
        Path path = new Path();
        path.moveTo(-margin, height * 1.02f);
        float prevX = -margin;
        float prevY = base + amp * 0.12f;
        path.lineTo(prevX, prevY);
        for (int i = 1; i <= points; i++) {
            float x = -margin + (width + 2f * margin) * i / points;
            float peak = 0.18f + r.nextFloat() * 0.82f;
            if (i % 2 == 0) {
                peak *= 0.38f;
            }
            float y = base - amp * peak;
            float c1 = prevX + (x - prevX) * 0.42f;
            float c2 = prevX + (x - prevX) * 0.58f;
            path.cubicTo(c1, prevY, c2, y, x, y);
            prevX = x;
            prevY = y;
        }
        path.lineTo(width + margin, height * 1.02f);
        path.close();
        return path;
    }

    private void buildLeaf() {
        leafPath.reset();
        leafPath.moveTo(0f, -1f);
        leafPath.cubicTo(0.75f, -0.55f, 0.62f, 0.45f, 0f, 1f);
        leafPath.cubicTo(-0.62f, 0.45f, -0.75f, -0.55f, 0f, -1f);
        leafPath.close();
    }

    private void buildNoise() {
        if (noise != null) {
            return;
        }
        int n = 96;
        noise = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
        Random random = new Random(4561L);
        int[] pixels = new int[n * n];
        for (int i = 0; i < pixels.length; i++) {
            int a = random.nextInt(35);
            pixels[i] = (a << 24) | 0x00FFFFFF;
        }
        noise.setPixels(pixels, 0, n, 0, 0, n, n);
        noiseShader = new BitmapShader(noise, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    }

    private void buildBackground() {
        int bw = Math.max(1, width / 2);
        int bh = Math.max(1, height / 2);
        Bitmap bitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(bw / (float) width, bh / (float) height);

        Palette p = palette;
        fill.setStyle(Paint.Style.FILL);
        fill.setShader(new LinearGradient(0f, 0f, 0f, height,
                new int[]{p.bgTop, p.bgMid, p.bgBottom},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, width, height, fill);
        fill.setShader(null);

        drawGlow(canvas, width * 0.20f, height * 0.28f, width * 0.48f,
                Palette.alpha(p.accent2, 0.075f));
        drawGlow(canvas, width * 0.83f, height * 0.48f, width * 0.44f,
                Palette.alpha(p.accent, 0.055f));
        drawGlow(canvas, cx, cy, radius * 1.55f,
                Palette.alpha(p.accent, p.light ? 0.035f : 0.06f));

        fill.setShader(noiseShader);
        fill.setAlpha(p.light ? 34 : 52);
        canvas.drawRect(0f, 0f, width, height, fill);
        fill.setAlpha(255);
        fill.setShader(null);

        drawMountain(canvas, mountainPaths[0], p.mountainFar, p, false);
        drawMountain(canvas, mountainPaths[1], p.mountainMid, p, false);
        drawMountain(canvas, mountainPaths[2], p.mountainNear, p, true);

        fill.setShader(new LinearGradient(0f, height * 0.68f, 0f, height,
                new int[]{
                        Palette.alpha(p.bgBottom, 0f),
                        Palette.alpha(p.bgBottom, 0.38f),
                        Palette.alpha(p.bgBottom, 0.86f)
                },
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, height * 0.68f, width, height, fill);
        fill.setShader(null);

        fill.setShader(new LinearGradient(0f, 0f, 0f, height * 0.22f,
                new int[]{
                        Palette.alpha(p.bgTop, 0.82f),
                        Palette.alpha(p.bgTop, 0.48f),
                        Palette.alpha(p.bgTop, 0f)
                },
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, width, height * 0.22f, fill);
        fill.setShader(null);

        if (background != null) {
            background.recycle();
        }
        background = bitmap;
        backgroundWidth = width;
        backgroundHeight = height;
    }

    private void drawGlow(Canvas canvas, float x, float y, float r, int color) {
        shaderPaint.setStyle(Paint.Style.FILL);
        shaderPaint.setShader(new RadialGradient(0f, 0f, r, color, 0x00000000,
                Shader.TileMode.CLAMP));
        canvas.save();
        canvas.translate(x, y);
        canvas.drawCircle(0f, 0f, r, shaderPaint);
        canvas.restore();
        shaderPaint.setShader(null);
    }

    private void drawMountain(Canvas canvas, Path path, int color, Palette p, boolean ridge) {
        fill.setStyle(Paint.Style.FILL);
        fill.setShader(new LinearGradient(0f, height * 0.70f, 0f, height,
                new int[]{color, Palette.blend(color, p.bgBottom, 0.55f)},
                null, Shader.TileMode.CLAMP));
        canvas.drawPath(path, fill);
        fill.setShader(null);
        if (ridge) {
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(Math.max(1f, 1.0f * u));
            stroke.setColor(Palette.alpha(p.ridge, 0.10f));
            canvas.drawPath(path, stroke);
        }
    }

    private void buildVignette() {
        float r = Math.max(radius * 1.8f, height * 0.6f);
        int edge = palette.light ? 0x143A2E22 : 0x78000000;
        vignettePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        vignettePaint.setShader(new RadialGradient(cx, cy, r,
                new int[]{0x00000000, 0x00000000, edge},
                new float[]{0f, 0.42f, 1f}, Shader.TileMode.CLAMP));
    }

    private void buildInnerPaint() {
        innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerPaint.setStyle(Paint.Style.FILL);
        innerPaint.setShader(new RadialGradient(cx, cy, innerRadius,
                new int[]{
                        Palette.alpha(palette.panel, palette.light ? 0.88f : 0.95f),
                        Palette.alpha(palette.bgBottom, palette.light ? 0.74f : 0.86f)
                },
                null, Shader.TileMode.CLAMP));
    }

    // ---------------------------------------------------------------------
    // Dynamic layers
    // ---------------------------------------------------------------------

    private void drawStars(Canvas canvas, float seconds, boolean motion) {
        float t = motion ? seconds : 0f;
        fill.setStyle(Paint.Style.FILL);
        for (int i = 0; i < starX.length; i++) {
            float twinkle = 0.5f + 0.5f * (float) Math.sin(t * starSpeed[i] + starPhase[i]);
            float alpha = (0.12f + 0.38f * twinkle) * (palette.light ? 0.32f : 1f);
            float x = starX[i] * width + parallax * 0.35f;
            float y = starY[i] * height;
            float radius = starSize[i] * u;
            fill.setColor(Palette.alpha(palette.star, alpha));
            canvas.drawCircle(x, y, radius, fill);
            if (starSize[i] > 2.4f) {
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeWidth(Math.max(0.8f, 0.8f * u));
                stroke.setColor(Palette.alpha(palette.star, alpha * 0.45f));
                canvas.drawLine(x - radius * 2.7f, y, x + radius * 2.7f, y, stroke);
                canvas.drawLine(x, y - radius * 2.7f, x, y + radius * 2.7f, stroke);
            }
        }
    }

    private void drawOrb(Canvas canvas, int hour, int minute, boolean motion) {
        boolean day = hour >= 6 && hour < 18;
        float gap = Math.max(0f, cy - radius - headerLine);
        if (gap < 95f * u) {
            return;
        }
        float orbRadius = Math.min(width * 0.085f, radius * 0.245f);
        orbRadius = Math.min(orbRadius, gap * 0.44f);
        if (orbRadius < width * 0.022f) {
            return;
        }
        float x = width * 0.795f + parallax * 0.55f;
        float y = headerLine + gap * 0.46f;
        float t = motion ? (hour * 3600f + minute * 60f) / 86400f : 0.5f;
        float drift = (float) Math.sin(t * 6.28318f) * 12f * u;
        x += drift;
        y += (float) Math.cos(t * 6.28318f) * 5f * u;

        int color = day ? Palette.blend(palette.orb, 0xFFFFE2A8, 0.45f) : palette.orb;
        int key = color ^ (int) (orbRadius * 10f);
        if (orbPaintRadius != (int) orbRadius || orbPaintColor != key) {
            orbPaintRadius = (int) orbRadius;
            orbPaintColor = key;
            orbHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            orbHaloPaint.setShader(new RadialGradient(0f, 0f, orbRadius * 3.0f,
                    new int[]{Palette.alpha(color, day ? 0.20f : 0.15f), 0x00000000},
                    null, Shader.TileMode.CLAMP));
            orbDiscPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            orbDiscPaint.setShader(new RadialGradient(-orbRadius * 0.28f, -orbRadius * 0.32f,
                    orbRadius * 1.25f,
                    new int[]{
                            Palette.blend(color, 0xFFFFFFFF, 0.35f),
                            color,
                            Palette.blend(color, palette.bgBottom, 0.55f)
                    },
                    new float[]{0f, 0.62f, 1f}, Shader.TileMode.CLAMP));
        }

        canvas.save();
        canvas.translate(x, y);
        canvas.drawCircle(0f, 0f, orbRadius * 3.0f, orbHaloPaint);
        canvas.drawCircle(0f, 0f, orbRadius, orbDiscPaint);
        canvas.restore();

        if (!day) {
            float lunar = calendar == null ? 15 : calendar.lunarDay;
            float phase = ((lunar - 1f) / 29.53f) % 1f;
            float fullness = 1f - Math.abs(phase * 2f - 1f);
            float d = orbRadius * 1.92f * fullness;
            float shadowX = x + (phase < 0.5f ? -d : d);
            fill.setColor(Palette.alpha(palette.bgMid, 0.92f));
            fill.setStyle(Paint.Style.FILL);
            canvas.drawCircle(shadowX, y, orbRadius * 0.985f, fill);
        }

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Math.max(1f, 1f * u));
        stroke.setColor(Palette.alpha(color, 0.38f));
        canvas.drawCircle(x, y, orbRadius, stroke);
    }

    private void drawParticles(Canvas canvas, float seconds, boolean motion) {
        float t = motion ? seconds : 0f;
        String season = calendar == null ? "秋" : calendar.season;
        int count = particles.length;
        for (int i = 0; i < count; i++) {
            Particle p = particles[i];
            float phase = t * 0.24f + p.phase + i * 0.37f;
            float x = p.x * width + (float) Math.sin(phase * 1.7f) * p.sway * u + parallax * 0.18f;
            float y;
            boolean firefly = "夏".equals(season);
            if (firefly) {
                y = p.y * height * 0.78f + (float) Math.sin(phase * 0.9f) * 30f * u;
            } else {
                float travel = t * p.speed * u;
                y = (p.y * height + travel) % (height + 100f * u) - 40f * u;
            }
            float dx = x - cx;
            float dy = y - cy;
            if (dx * dx + dy * dy < innerRadius * innerRadius * 1.10f) {
                continue;
            }
            float alpha = p.alpha * (0.55f + 0.45f * (float) Math.sin(phase * 2.1f));
            if (alpha <= 0.02f) {
                continue;
            }
            int color = Palette.alpha(palette.particle, alpha);
            float size = p.size * u;
            if ("春".equals(season)) {
                canvas.save();
                canvas.translate(x, y);
                canvas.rotate(p.phase * 57.3f + t * p.spin * 12f);
                fill.setColor(color);
                fill.setStyle(Paint.Style.FILL);
                oval.set(-size * 0.48f, -size, size * 0.48f, size);
                canvas.drawOval(oval, fill);
                canvas.restore();
            } else if ("秋".equals(season)) {
                canvas.save();
                canvas.translate(x, y);
                canvas.rotate(p.phase * 57.3f + t * p.spin * 18f);
                canvas.scale(size, size);
                fill.setColor(color);
                fill.setStyle(Paint.Style.FILL);
                canvas.drawPath(leafPath, fill);
                canvas.restore();
            } else if (firefly) {
                fill.setColor(Palette.alpha(palette.particle, alpha * 0.16f));
                fill.setStyle(Paint.Style.FILL);
                canvas.drawCircle(x, y, size * 2.4f, fill);
                fill.setColor(Palette.alpha(palette.particle, Math.min(0.9f, alpha * 1.8f)));
                canvas.drawCircle(x, y, size * 0.55f, fill);
            } else {
                fill.setColor(color);
                fill.setStyle(Paint.Style.FILL);
                canvas.drawCircle(x, y, size * 0.55f, fill);
                if (size > 3f * u) {
                    stroke.setStyle(Paint.Style.STROKE);
                    stroke.setStrokeWidth(Math.max(0.7f, 0.7f * u));
                    stroke.setColor(Palette.alpha(palette.particle, alpha * 0.7f));
                    canvas.drawLine(x - size, y, x + size, y, stroke);
                    canvas.drawLine(x, y - size, x, y + size, stroke);
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Foreground
    // ---------------------------------------------------------------------

    private void drawHeader(Canvas canvas) {
        float margin = 54f * u;
        float seal = 62f * u;
        float sealTop = headerTop - 24f * u;

        fill.setStyle(Paint.Style.FILL);
        fill.setColor(Palette.alpha(0xFFB5462F, palette.light ? 0.92f : 0.98f));
        oval.set(margin, sealTop, margin + seal, sealTop + seal);
        canvas.drawRoundRect(oval, 5f * u, 5f * u, fill);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Math.max(1f, 1f * u));
        stroke.setColor(Palette.alpha(0xFFF2D9C2, 0.70f));
        oval.inset(4f * u, 4f * u);
        canvas.drawRoundRect(oval, 2f * u, 2f * u, stroke);
        text(canvas, "知午", margin + seal * 0.5f, sealTop + seal * 0.63f,
                21f * u, serifBold, 0xFFFFF6E8, Paint.Align.CENTER, 0.04f);

        float textX = margin + seal + 20f * u;
        text(canvas, calendar.lunarYearGanZhi + "  " + calendar.lunarDate,
                textX, headerTop + 22f * u, 40f * u, serifBold,
                Palette.alpha(palette.textPrimary, 0.99f), Paint.Align.LEFT, 0.025f);
        text(canvas, calendar.gregorian + "   " + calendar.weekday,
                textX, headerTop + 72f * u, 25f * u, sans,
                Palette.alpha(palette.textSecondary, 0.94f), Paint.Align.LEFT, 0.04f);

        String event;
        if (calendar.festival != null) {
            event = calendar.festival;
        } else if (calendar.termPending) {
            event = "今日 " + calendar.termTodayName;
        } else {
            event = calendar.termName;
        }
        if (event == null) {
            event = "";
        }
        float pillText = 24f * u;
        float pillWidth = measure(event, pillText, serif, 0.04f) + 36f * u;
        float pillHeight = 48f * u;
        float pillLeft = width - margin - pillWidth;
        float pillTop = headerTop - 8f * u;
        pillRect.set(pillLeft, pillTop, pillLeft + pillWidth, pillTop + pillHeight);
        fill.setColor(Palette.alpha(palette.accent, 0.20f));
        fill.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(pillRect, pillHeight / 2f, pillHeight / 2f, fill);
        stroke.setColor(Palette.alpha(palette.accent, 0.70f));
        stroke.setStrokeWidth(Math.max(1f, 1.2f * u));
        canvas.drawRoundRect(pillRect, pillHeight / 2f, pillHeight / 2f, stroke);
        text(canvas, event, pillLeft + pillWidth / 2f, pillTop + pillHeight * 0.64f,
                pillText, serifBold, Palette.alpha(palette.accentBright, 1f),
                Paint.Align.CENTER, 0.06f);

        String sub;
        if (calendar.festival != null) {
            sub = calendar.termName + " · " + calendar.termPhase;
        } else if (calendar.termPending) {
            sub = calendar.termTime + " 交节 · " + calendar.termName + " · " + calendar.termPhase;
        } else {
            sub = "距" + calendar.nextTermName + " " + calendar.daysToNextTerm + "天";
            if (calendar.termToday) {
                sub = "今日交节 · " + sub;
            }
        }
        text(canvas, sub, width - margin, headerTop + 82f * u, 23f * u, sans,
                Palette.alpha(palette.textSecondary, 0.94f), Paint.Align.RIGHT, 0.015f);

        stroke.setColor(Palette.alpha(palette.accent, 0.26f));
        stroke.setStrokeWidth(Math.max(1f, 1.1f * u));
        canvas.drawLine(margin, headerLine, width - margin, headerLine, stroke);
        fill.setColor(Palette.alpha(palette.accent, 0.75f));
        canvas.drawCircle(margin, headerLine, 2.2f * u, fill);
        canvas.drawCircle(width - margin, headerLine, 2.2f * u, fill);
    }

    private void drawDial(Canvas canvas, Calendar time) {
        int hour = time.get(Calendar.HOUR_OF_DAY);
        int minute = time.get(Calendar.MINUTE);
        int second = time.get(Calendar.SECOND);
        int shichen = Shichen.indexOf(hour);
        float progress = Shichen.progress(hour, minute, second);
        float rs = Math.min(1f, radius / (480f * u));

        // A solid charcoal/celadon band keeps the dial legible on every palette.
        float bandMid = (radius + innerRadius) / 2f;
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(radius - innerRadius);
        arcPaint.setStrokeCap(Paint.Cap.BUTT);
        arcPaint.setColor(Palette.alpha(palette.panel, palette.light ? 0.26f : 0.42f));
        canvas.drawCircle(cx, cy, bandMid, arcPaint);
        arcPaint.setStrokeWidth(30f * u);
        oval.set(cx - radius, cy - radius, cx + radius, cy + radius);

        for (int i = 0; i < 12; i++) {
            float start = -105f + i * 30f + 1.1f;
            if (i == shichen) {
                arcPaint.setColor(Palette.alpha(palette.accent, 0.11f));
                arcPaint.setStrokeWidth(44f * u);
                canvas.drawArc(oval, start, 27.8f, false, arcPaint);
                arcPaint.setStrokeWidth(30f * u);
                arcPaint.setColor(Palette.alpha(palette.accent, 0.24f));
                canvas.drawArc(oval, start, 27.8f, false, arcPaint);
                arcPaint.setColor(Palette.alpha(palette.accent, 0.98f));
                canvas.drawArc(oval, start, 27.8f * progress, false, arcPaint);
            } else {
                arcPaint.setColor(Palette.alpha(palette.textPrimary,
                        palette.light ? 0.11f : 0.075f));
                canvas.drawArc(oval, start, 27.8f, false, arcPaint);
            }
        }

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Math.max(1f, 1.1f * u));
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(-105 + i * 30);
            float x1 = cx + (float) Math.cos(a) * (radius - 16f * u);
            float y1 = cy + (float) Math.sin(a) * (radius - 16f * u);
            float x2 = cx + (float) Math.cos(a) * (radius + 16f * u);
            float y2 = cy + (float) Math.sin(a) * (radius + 16f * u);
            stroke.setColor(Palette.alpha(palette.accent, 0.38f));
            canvas.drawLine(x1, y1, x2, y2, stroke);
        }

        // Fine border lines around the wide band.
        stroke.setColor(Palette.alpha(palette.accent, 0.30f));
        stroke.setStrokeWidth(Math.max(1f, 1.2f * u));
        canvas.drawCircle(cx, cy, radius + 16f * u, stroke);
        canvas.drawCircle(cx, cy, radius - 16f * u, stroke);

        // A quiet second ring: 24 solar-term ticks.
        for (int i = 0; i < 24; i++) {
            double a = Math.toRadians(-90 + i * 15);
            float inner = radius + 22f * u;
            float outer = radius + (i % 6 == 0 ? 35f : 29f) * u;
            float x1 = cx + (float) Math.cos(a) * inner;
            float y1 = cy + (float) Math.sin(a) * inner;
            float x2 = cx + (float) Math.cos(a) * outer;
            float y2 = cy + (float) Math.sin(a) * outer;
            stroke.setColor(Palette.alpha(palette.textSecondary, i % 6 == 0 ? 0.42f : 0.24f));
            canvas.drawLine(x1, y1, x2, y2, stroke);
        }

        // Labels: earthly branch + meridian.
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(-90 + i * 30);
            float branchR = radius * 0.880f;
            float lx = cx + (float) Math.cos(a) * branchR;
            float ly = cy + (float) Math.sin(a) * branchR;
            boolean active = i == shichen;
            textCentered(canvas, Shichen.BRANCH[i], lx, ly,
                    (active ? 56f : 52f) * u * rs, serifBold,
                    active ? palette.accentBright : Palette.alpha(palette.textSecondary, 0.94f),
                    active ? 0.04f : 0.0f);

            float meridianR = radius * 0.700f;
            float mx = cx + (float) Math.cos(a) * meridianR;
            float my = cy + (float) Math.sin(a) * meridianR;
            textCentered(canvas, Shichen.MERIDIAN[i], mx, my, 26f * u * rs, serif,
                    active ? Palette.alpha(palette.accent, 1f)
                            : Palette.alpha(palette.textSecondary, 0.82f), 0.02f);
        }

        // Current-time marker on the rim.
        float angle = -90f + (hour * 3600f + minute * 60f + second) / 86400f * 360f;
        double ar = Math.toRadians(angle);
        float nx1 = cx + (float) Math.cos(ar) * (radius - 40f * u);
        float ny1 = cy + (float) Math.sin(ar) * (radius - 40f * u);
        float nx2 = cx + (float) Math.cos(ar) * (radius + 22f * u);
        float ny2 = cy + (float) Math.sin(ar) * (radius + 22f * u);
        stroke.setColor(Palette.alpha(palette.accent, 0.72f));
        stroke.setStrokeWidth(Math.max(1.2f, 1.6f * u));
        canvas.drawLine(nx1, ny1, nx2, ny2, stroke);
        float dotX = cx + (float) Math.cos(ar) * (radius + 24f * u);
        float dotY = cy + (float) Math.sin(ar) * (radius + 24f * u);
        fill.setColor(Palette.alpha(palette.accent, 0.20f));
        canvas.drawCircle(dotX, dotY, 10f * u, fill);
        fill.setColor(palette.accentBright);
        canvas.drawCircle(dotX, dotY, 4.0f * u, fill);

        // Inner disc.
        canvas.drawCircle(cx, cy, innerRadius, innerPaint);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Math.max(1f, 1.2f * u));
        stroke.setColor(Palette.alpha(palette.accent, 0.38f));
        canvas.drawCircle(cx, cy, innerRadius, stroke);
        stroke.setColor(Palette.alpha(palette.accent, 0.16f));
        canvas.drawCircle(cx, cy, innerRadius - 8f * u, stroke);

        text(canvas, "子 午 流 注", cx, cy - 0.525f * radius,
                20f * u * rs, serif, Palette.alpha(palette.textSecondary, 0.82f),
                Paint.Align.CENTER, 0.12f);

        text(canvas, Shichen.name(shichen), cx, cy - 0.310f * radius,
                42f * u * rs, serifBold, Palette.alpha(palette.accent, 1f),
                Paint.Align.CENTER, 0.08f);

        drawBigTime(canvas, hour, minute, second);

        text(canvas, Shichen.meridian(shichen) + "当令", cx, cy + 0.228f * radius,
                32f * u * rs, serifBold, Palette.alpha(palette.accentBright, 1f),
                Paint.Align.CENTER, 0.04f);

        if (settings.showHealth) {
            text(canvas, Shichen.health(shichen), cx, cy + 0.338f * radius,
                    28f * u * rs, sans, Palette.alpha(palette.textSecondary, 0.99f),
                    Paint.Align.CENTER, 0.01f);
        }

        float barWidth = innerRadius * 1.06f;
        float barY = cy + 0.428f * radius;
        fill.setColor(Palette.alpha(palette.textSecondary, 0.30f));
        oval.set(cx - barWidth / 2f, barY, cx + barWidth / 2f, barY + 3.2f * u);
        canvas.drawRoundRect(oval, 1.6f * u, 1.6f * u, fill);
        fill.setColor(Palette.alpha(palette.accent, 0.94f));
        oval.set(cx - barWidth / 2f, barY,
                cx - barWidth / 2f + barWidth * progress, barY + 3.2f * u);
        canvas.drawRoundRect(oval, 1.6f * u, 1.6f * u, fill);
        fill.setColor(palette.accentBright);
        canvas.drawCircle(cx - barWidth / 2f + barWidth * progress, barY + 1.6f * u,
                3.4f * u, fill);
    }

    private void drawBigTime(Canvas canvas, int hour, int minute, int second) {
        String hh = String.format(Locale.CHINA, "%02d", hour);
        String mm = String.format(Locale.CHINA, "%02d", minute);
        String ss = String.format(Locale.CHINA, "%02d", second);
        float size = Math.min(radius * 0.285f, width * 0.125f);
        float baseline = cy + 0.072f * radius;
        float spacing = 0.015f;
        float hw = measure(hh, size, sansMedium, spacing);
        float colonW = measure(":", size, sansMedium, 0f);
        float mw = measure(mm, size, sansMedium, spacing);
        float suffixW = settings.showSeconds ? measure(ss, 29f * u, sansMedium, 0.05f) + 10f * u : 0f;
        float total = hw + colonW + mw + suffixW;
        float x = cx - total / 2f;

        text(canvas, hh, x, baseline, size, sansMedium,
                Palette.alpha(palette.textPrimary, 1f), Paint.Align.LEFT, spacing);
        x += hw;
        text(canvas, ":", x, baseline, size, sansMedium,
                Palette.alpha(palette.accentBright, 1f), Paint.Align.LEFT, 0f);
        x += colonW;
        text(canvas, mm, x, baseline, size, sansMedium,
                Palette.alpha(palette.textPrimary, 1f), Paint.Align.LEFT, spacing);
        x += mw;
        if (settings.showSeconds) {
            text(canvas, ss, x + 10f * u, baseline - size * 0.42f,
                    29f * u, sansMedium, Palette.alpha(palette.textSecondary, 0.92f),
                    Paint.Align.LEFT, 0.05f);
        }
    }

    private void drawNextShichen(Canvas canvas, Calendar time) {
        int hour = time.get(Calendar.HOUR_OF_DAY);
        int minute = time.get(Calendar.MINUTE);
        int second = time.get(Calendar.SECOND);
        int shichen = Shichen.indexOf(hour);
        int next = (shichen + 1) % 12;
        String left = "下一时辰  " + Shichen.name(next) + " · " + Shichen.MERIDIAN[next];
        String right = Shichen.countdown(Shichen.secondsUntilNext(hour, minute, second));
        float textSize = 26f * u;
        float countSize = 28f * u;
        float leftW = measure(left, textSize, serif, 0.0f);
        float rightW = measure(right, countSize, mono, 0.0f);
        float pillWidth = leftW + rightW + 82f * u;
        float pillHeight = 62f * u;
        float centerY = Math.max(cy + radius + 108f * u, height * 0.735f);
        pillRect.set(cx - pillWidth / 2f, centerY - pillHeight / 2f,
                cx + pillWidth / 2f, centerY + pillHeight / 2f);
        fill.setColor(Palette.alpha(palette.panel, palette.light ? 0.82f : 0.88f));
        fill.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(pillRect, pillHeight / 2f, pillHeight / 2f, fill);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Math.max(1f, 1.1f * u));
        stroke.setColor(Palette.alpha(palette.accent, 0.34f));
        canvas.drawRoundRect(pillRect, pillHeight / 2f, pillHeight / 2f, stroke);

        float startX = pillRect.left + 32f * u;
        fill.setColor(Palette.alpha(palette.accent, 0.95f));
        canvas.drawCircle(startX - 13f * u, centerY, 3.6f * u, fill);
        text(canvas, left, startX, centerY + 9f * u, textSize, serif,
                Palette.alpha(palette.textPrimary, 0.99f), Paint.Align.LEFT, 0.0f);
        text(canvas, right, pillRect.right - 26f * u, centerY + 10f * u,
                countSize, mono, Palette.alpha(palette.accentBright, 1f),
                Paint.Align.RIGHT, 0.0f);
    }

    private void drawPillars(Canvas canvas) {
        float y = height * 0.850f;
        String[] labels = {"年柱", "月柱", "日柱"};
        String[] values = {calendar.yearGanZhi, calendar.monthGanZhi, calendar.dayGanZhi};
        for (int i = 0; i < 3; i++) {
            float x = width * (i * 2f + 1f) / 6f;
            text(canvas, values[i], x, y, 36f * u, serifBold,
                    Palette.alpha(palette.textPrimary, 1f), Paint.Align.CENTER, 0.08f);
            text(canvas, labels[i], x, y + 34f * u, 20f * u, sans,
                    Palette.alpha(palette.textSecondary, 0.90f), Paint.Align.CENTER, 0.14f);
            if (i < 2) {
                float lineX = width * (i + 1f) / 3f;
                stroke.setColor(Palette.alpha(palette.accent, 0.24f));
                stroke.setStrokeWidth(Math.max(1f, 1.2f * u));
                canvas.drawLine(lineX, y - 30f * u, lineX, y + 44f * u, stroke);
            }
        }

        float lineY = height * 0.912f;
        text(canvas, "农历 " + calendar.lunarDate, 54f * u, lineY, 22f * u, sans,
                Palette.alpha(palette.textSecondary, 0.90f), Paint.Align.LEFT, 0.0f);
        text(canvas, "生肖 " + calendar.zodiac + " · 月相" + calendar.moonPhase,
                width - 54f * u, lineY, 22f * u, sans,
                Palette.alpha(palette.textSecondary, 0.90f), Paint.Align.RIGHT, 0.0f);
        text(canvas, "气 血 有 时 · 顺 时 而 居", cx, lineY, 18f * u, serif,
                Palette.alpha(palette.textSecondary, 0.74f), Paint.Align.CENTER, 0.03f);

        text(canvas, "知 午   Z H I W U", cx, height * 0.966f, 14f * u, sans,
                Palette.alpha(palette.textSecondary, 0.56f), Paint.Align.CENTER, 0.18f);
    }

    // ---------------------------------------------------------------------
    // Text helpers
    // ---------------------------------------------------------------------

    private void text(Canvas canvas, String value, float x, float y, float size,
                      Typeface typeface, int color, Paint.Align align, float spacing) {
        if (value == null || value.length() == 0) {
            return;
        }
        textPaint.setTypeface(typeface);
        textPaint.setTextSize(size);
        textPaint.setColor(color);
        textPaint.setTextAlign(align);
        textPaint.setLetterSpacing(spacing);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setSubpixelText(true);
        canvas.drawText(value, x, y, textPaint);
    }

    private void textCentered(Canvas canvas, String value, float x, float y, float size,
                              Typeface typeface, int color, float spacing) {
        textPaint.setTypeface(typeface);
        textPaint.setTextSize(size);
        textPaint.setLetterSpacing(spacing);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baseline = y - (fm.ascent + fm.descent) / 2f;
        text(canvas, value, x, baseline, size, typeface, color, Paint.Align.CENTER, spacing);
    }

    private float measure(String value, float size, Typeface typeface, float spacing) {
        if (value == null) {
            return 0f;
        }
        textPaint.setTypeface(typeface);
        textPaint.setTextSize(size);
        textPaint.setLetterSpacing(spacing);
        return textPaint.measureText(value);
    }

    // ---------------------------------------------------------------------
    // Small data types
    // ---------------------------------------------------------------------

    private static final class Particle {
        float x;
        float y;
        float speed;
        float sway;
        float size;
        float phase;
        float spin;
        float alpha;
    }
}
