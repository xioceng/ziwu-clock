package com.zhiwu.wallpaper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

/** A small painted swatch used in the appearance picker. */
public final class ThemeSwatchView extends View {

    private final String theme;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Typeface serif = Typeface.create("serif", Typeface.NORMAL);
    private boolean selected;

    public ThemeSwatchView(Context context, String theme) {
        super(context);
        this.theme = theme;
        setClickable(true);
        setFocusable(true);
        setContentDescription(Settings.label(theme) + "配色");
    }

    public String getTheme() {
        return theme;
    }

    public void setSelectedTheme(boolean selected) {
        this.selected = selected;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float d = getResources().getDisplayMetrics().density;
        float pad = 1f * d;
        float labelH = 30f * d;
        float left = pad;
        float top = pad;
        float right = getWidth() - pad;
        float bottom = getHeight() - labelH;
        float radius = 14f * d;
        rect.set(left, top, right, bottom);

        int topColor;
        int bottomColor;
        int ringColor;
        switch (Settings.normalizeTheme(theme)) {
            case Settings.THEME_JADE:
                topColor = 0xFF071A17;
                bottomColor = 0xFF23423A;
                ringColor = 0xFFA9D0BE;
                break;
            case Settings.THEME_CINNABAR:
                topColor = 0xFF1B090C;
                bottomColor = 0xFF3A1216;
                ringColor = 0xFFE06A4E;
                break;
            case Settings.THEME_PAPER:
                topColor = 0xFFF5EFE2;
                bottomColor = 0xFFE2D4BC;
                ringColor = 0xFFA8442E;
                break;
            case Settings.THEME_INK:
                topColor = 0xFF05070D;
                bottomColor = 0xFF162136;
                ringColor = 0xFFD8B36A;
                break;
            default:
                topColor = 0xFF070B16;
                bottomColor = 0xFF23423A;
                ringColor = 0xFFD8B36A;
                break;
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(left, top, left, bottom,
                new int[]{topColor, bottomColor}, null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, radius, radius, paint);
        paint.setShader(null);

        // A tiny twelve-segment dial.
        float ccx = rect.centerX();
        float ccy = rect.centerY();
        float rr = Math.min(rect.width(), rect.height()) * 0.27f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.2f * d);
        paint.setColor(Palette.alpha(ringColor, 0.75f));
        canvas.drawCircle(ccx, ccy, rr, paint);
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(i * 30 - 90);
            float x1 = ccx + (float) Math.cos(a) * (rr - 2.2f * d);
            float y1 = ccy + (float) Math.sin(a) * (rr - 2.2f * d);
            float x2 = ccx + (float) Math.cos(a) * (rr + 1.5f * d);
            float y2 = ccy + (float) Math.sin(a) * (rr + 1.5f * d);
            paint.setStrokeWidth(0.9f * d);
            paint.setColor(Palette.alpha(ringColor, 0.45f));
            canvas.drawLine(x1, y1, x2, y2, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ringColor);
        canvas.drawCircle(ccx, ccy, 2.4f * d, paint);

        if (selected) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.8f * d);
            paint.setColor(0xFFF0D49A);
            canvas.drawRoundRect(rect, radius, radius, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFF0D49A);
            canvas.drawCircle(rect.right - 7f * d, rect.top + 7f * d, 2.6f * d, paint);
        } else {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1f * d);
            paint.setColor(0x22FFFFFF);
            canvas.drawRoundRect(rect, radius, radius, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(serif);
        paint.setTextSize(15f * d);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(selected ? 0xFFF0D49A : 0xFF9A9D98);
        canvas.drawText(Settings.label(theme), getWidth() / 2f, getHeight() - 8f * d, paint);
    }
}
