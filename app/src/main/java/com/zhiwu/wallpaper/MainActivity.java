package com.zhiwu.wallpaper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;

public final class MainActivity extends Activity {

    private static final int BG = 0xFF070A12;
    private static final int IVORY = 0xFFF2EBDD;
    private static final int MUTED = 0xFF969A95;
    private static final int DIM = 0xFF676C69;
    private static final int GOLD = 0xFFD8B36A;
    private static final int GOLD_BRIGHT = 0xFFF0D49A;

    private Settings settings;
    private WallpaperPreviewView preview;
    private ThemeSwatchView[] swatches;
    private TextView status;
    private TextView next;
    private Typeface serif;
    private Typeface sans;
    private Typeface sansMedium;
    private Typeface sansLight;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateNow();
            handler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = Settings.load(this);
        serif = Typeface.create("serif", Typeface.NORMAL);
        sans = Typeface.create("sans", Typeface.NORMAL);
        sansMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        sansLight = Typeface.create("sans-serif-light", Typeface.NORMAL);

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().getDecorView().setSystemUiVisibility(0);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
        }

        setContentView(buildContent());
        updateNow();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (preview != null) {
            preview.start();
        }
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    @Override
    protected void onPause() {
        if (preview != null) {
            preview.stop();
        }
        handler.removeCallbacks(ticker);
        super.onPause();
    }

    private View buildContent() {
        FrameLayout root = new FrameLayout(this);
        root.setBackground(makeRootBackground());
        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                int top = insets.getSystemWindowInsetTop();
                int bottom = insets.getSystemWindowInsetBottom();
                if (view.getPaddingTop() != top || view.getPaddingBottom() != bottom) {
                    view.setPadding(0, top, 0, bottom);
                }
                return insets;
            });
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;
        int side = Math.max(dp(22), (screenW - dp(560)) / 2);
        int contentW = screenW - side * 2;

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(side, dp(16), side, dp(30));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content.addView(buildHeader());
        content.addView(buildPreview(contentW, screenH));

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        meta.setPadding(dp(2), dp(12), dp(2), dp(2));
        status = label("", 15f, GOLD, serif);
        status.setLetterSpacing(0.03f);
        next = label("", 13f, MUTED, sans);
        next.setGravity(Gravity.RIGHT);
        meta.addView(status, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        meta.addView(next, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(meta);

        TextView hint = label("轻触预览可切换配色", 12.5f, DIM, sans);
        hint.setGravity(Gravity.RIGHT);
        hint.setPadding(0, 0, dp(2), dp(10));
        content.addView(hint);

        content.addView(buildActions());
        content.addView(buildAppearance(contentW));
        content.addView(buildDisplay());
        content.addView(buildFooter());
        return root;
    }

    private View buildHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, dp(18));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);

        TextView title = label("知午", 38f, IVORY, serif);
        title.setLetterSpacing(0.14f);
        title.setIncludeFontPadding(false);
        brand.addView(title);

        TextView sub = label("子 午 流 注 · 万 年 历", 12f, MUTED, sans);
        sub.setLetterSpacing(0.08f);
        sub.setPadding(0, dp(4), 0, 0);
        brand.addView(sub);

        row.addView(brand, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView live = label("动态壁纸", 12f, GOLD_BRIGHT, sansMedium);
        live.setLetterSpacing(0.08f);
        live.setBackgroundResource(R.drawable.bg_live);
        live.setPadding(dp(13), dp(6), dp(13), dp(6));
        live.setGravity(Gravity.CENTER);
        row.addView(live, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private View buildPreview(int contentW, int screenH) {
        FrameLayout card = new FrameLayout(this);
        card.setBackgroundResource(R.drawable.bg_preview);
        card.setPadding(dp(1), dp(1), dp(1), dp(1));
        card.setClipToOutline(true);
        card.setElevation(dp(10));
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription("动态壁纸实时预览，轻触切换配色");

        int cardH = Math.min((int) (contentW * 1.34f), (int) (screenH * 0.54f));
        cardH = Math.max(cardH, dp(300));
        if (getResources().getDisplayMetrics().widthPixels
                > getResources().getDisplayMetrics().heightPixels) {
            cardH = (int) (screenH * 0.60f);
        }
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, cardH);
        cardParams.bottomMargin = dp(2);

        preview = new WallpaperPreviewView(this);
        preview.setSettings(settings);
        card.addView(preview, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        card.setOnClickListener(v -> cycleTheme());
        return card;
    }

    private View buildActions() {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);

        Button setWallpaper = new Button(this);
        setWallpaper.setText("设为动态壁纸  →");
        setWallpaper.setAllCaps(false);
        setWallpaper.setTextSize(18f);
        setWallpaper.setTypeface(sansMedium);
        setWallpaper.setTextColor(0xFF1A1205);
        setWallpaper.setBackgroundResource(R.drawable.bg_primary);
        setWallpaper.setStateListAnimator(null);
        setWallpaper.setMinHeight(0);
        setWallpaper.setMinimumHeight(0);
        setWallpaper.setPadding(dp(16), 0, dp(16), 0);
        setWallpaper.setOnClickListener(v -> launchWallpaper());
        LinearLayout.LayoutParams primary = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        primary.topMargin = dp(8);
        block.addView(setWallpaper, primary);

        TextView picker = label("或打开系统壁纸选择器", 15f, MUTED, sans);
        picker.setGravity(Gravity.CENTER);
        picker.setPadding(dp(12), dp(12), dp(12), dp(10));
        picker.setOnClickListener(v -> openPicker());
        block.addView(picker);

        TextView note = label(getString(R.string.set_note), 13f, MUTED, sans);
        note.setLineSpacing(dp(2), 1.08f);
        note.setPadding(dp(2), 0, dp(2), dp(8));
        block.addView(note);
        return block;
    }

    private View buildAppearance(int contentW) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(0, dp(16), 0, dp(4));
        block.addView(sectionTitle("外观", "自动配色随十二时辰流转"));

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setPadding(dp(2), dp(8), dp(2), dp(2));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        swatches = new ThemeSwatchView[Settings.THEMES.length];
        for (int i = 0; i < Settings.THEMES.length; i++) {
            ThemeSwatchView swatch = new ThemeSwatchView(this, Settings.THEMES[i]);
            swatch.setSelectedTheme(Settings.THEMES[i].equals(settings.theme));
            swatch.setOnClickListener(v -> applyTheme(((ThemeSwatchView) v).getTheme()));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(92), dp(116));
            p.rightMargin = dp(9);
            row.addView(swatch, p);
            swatches[i] = swatch;
        }
        scroll.addView(row, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        block.addView(scroll);
        return block;
    }

    private View buildDisplay() {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(0, dp(18), 0, dp(2));
        block.addView(sectionTitle("显示", "让表盘保持安静，或更丰富"));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundResource(R.drawable.bg_panel);
        panel.setClipToOutline(true);
        panel.setPadding(dp(4), dp(4), dp(4), dp(4));

        panel.addView(settingRow("显示秒数", "在时刻右上角显示秒数", settings.showSeconds,
                value -> {
                    settings = settings.withSeconds(value);
                    persist();
                }));
        panel.addView(divider());
        panel.addView(settingRow("动态效果", "星尘、流云与四时花木", settings.motion,
                value -> {
                    settings = settings.withMotion(value);
                    persist();
                }));
        panel.addView(divider());
        panel.addView(settingRow("经络提示", "显示当令经络的养生短语", settings.showHealth,
                value -> {
                    settings = settings.withHealth(value);
                    persist();
                }));
        block.addView(panel);
        return block;
    }

    private View buildFooter() {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setGravity(Gravity.CENTER_HORIZONTAL);
        block.setPadding(0, dp(26), 0, 0);

        View line = new View(this);
        line.setBackgroundColor(0x14D8B36A);
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(dp(42), dp(1));
        lineParams.bottomMargin = dp(16);
        block.addView(line, lineParams);

        TextView motto = label("以十二时辰为刻度，顺时而居", 14f, MUTED, serif);
        motto.setLetterSpacing(0.06f);
        block.addView(motto);

        TextView version = label("v1.1.0  ·  本地历算  ·  不联网  ·  关于", 12f, DIM, sans);
        version.setPadding(0, dp(7), 0, 0);
        version.setOnClickListener(v -> showAbout());
        block.addView(version);
        return block;
    }

    private View sectionTitle(String title, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.BOTTOM);

        TextView t = label(title, 22f, IVORY, serif);
        t.setLetterSpacing(0.06f);
        row.addView(t, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView s = label(subtitle, 13f, MUTED, sans);
        s.setGravity(Gravity.RIGHT);
        s.setPadding(dp(10), 0, dp(2), dp(2));
        row.addView(s, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private View settingRow(String title, String subtitle, boolean checked,
                            final OnToggle listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(11), dp(8), dp(11));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView t = label(title, 17f, IVORY, serif);
        t.setLetterSpacing(0.03f);
        labels.addView(t);
        TextView s = label(subtitle, 13f, MUTED, sans);
        s.setPadding(0, dp(2), 0, 0);
        labels.addView(s);
        row.addView(labels, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch toggle = new Switch(this);
        toggle.setChecked(checked);
        toggle.setScaleX(0.86f);
        toggle.setScaleY(0.86f);
        toggle.setThumbTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{GOLD, 0xFF6D706C}));
        toggle.setTrackTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{0x55D8B36A, 0x2AFFFFFF}));
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> listener.onToggle(isChecked));
        row.addView(toggle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(0x12FFFFFF);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        p.leftMargin = dp(14);
        p.rightMargin = dp(14);
        v.setLayoutParams(p);
        return v;
    }

    private TextView label(String value, float sp, int color, Typeface typeface) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setTypeface(typeface);
        v.setIncludeFontPadding(false);
        return v;
    }

    private GradientDrawable makeRootBackground() {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF070A12, 0xFF0A101B, 0xFF111A2A});
        g.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        return g;
    }

    private void updateNow() {
        if (status == null || next == null) {
            return;
        }
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        int second = c.get(Calendar.SECOND);
        int index = Shichen.indexOf(hour);
        int nextIndex = (index + 1) % 12;
        status.setText(Shichen.name(index) + " · " + Shichen.meridian(index) + "当令");
        next.setText(String.format(Locale.CHINA, "距%s %s",
                Shichen.name(nextIndex),
                Shichen.countdown(Shichen.secondsUntilNext(hour, minute, second))));
    }

    private void persist() {
        settings.save(this);
        if (preview != null) {
            preview.setSettings(settings);
        }
    }

    private void applyTheme(String theme) {
        settings = settings.withTheme(theme);
        if (swatches != null) {
            for (ThemeSwatchView s : swatches) {
                s.setSelectedTheme(s.getTheme().equals(settings.theme));
            }
        }
        persist();
    }

    private void cycleTheme() {
        int index = 0;
        for (int i = 0; i < Settings.THEMES.length; i++) {
            if (Settings.THEMES[i].equals(settings.theme)) {
                index = i;
                break;
            }
        }
        applyTheme(Settings.THEMES[(index + 1) % Settings.THEMES.length]);
    }

    private void launchWallpaper() {
        ComponentName component = new ComponentName(this, MeridianWallpaperService.class);
        Intent intent = new Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        intent.putExtra(android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            openPicker();
        }
    }

    private void openPicker() {
        Intent intent = new Intent(android.app.WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.wallpaper_set_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("关于知午")
                .setMessage(R.string.about_text)
                .setPositiveButton("好", null)
                .show();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface OnToggle {
        void onToggle(boolean checked);
    }
}
