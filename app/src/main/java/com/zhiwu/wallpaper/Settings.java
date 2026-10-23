package com.zhiwu.wallpaper;

import android.content.Context;
import android.content.SharedPreferences;

/** Immutable display preferences shared by the activity and the wallpaper engine. */
public final class Settings {

    public static final String PREFS = "zhiwu_wallpaper";
    public static final String KEY_THEME = "theme";
    public static final String KEY_SECONDS = "show_seconds";
    public static final String KEY_MOTION = "motion";
    public static final String KEY_HEALTH = "show_health";

    public static final String THEME_AUTO = "auto";
    public static final String THEME_INK = "ink";
    public static final String THEME_JADE = "jade";
    public static final String THEME_CINNABAR = "cinnabar";
    public static final String THEME_PAPER = "paper";

    public static final String[] THEMES = {
            THEME_AUTO, THEME_INK, THEME_JADE, THEME_CINNABAR, THEME_PAPER
    };

    public final String theme;
    public final boolean showSeconds;
    public final boolean motion;
    public final boolean showHealth;

    public Settings(String theme, boolean showSeconds, boolean motion, boolean showHealth) {
        this.theme = normalizeTheme(theme);
        this.showSeconds = showSeconds;
        this.motion = motion;
        this.showHealth = showHealth;
    }

    public static Settings defaults() {
        return new Settings(THEME_AUTO, true, true, true);
    }

    public static Settings load(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new Settings(
                p.getString(KEY_THEME, THEME_AUTO),
                p.getBoolean(KEY_SECONDS, true),
                p.getBoolean(KEY_MOTION, true),
                p.getBoolean(KEY_HEALTH, true));
    }

    public void save(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_THEME, theme)
                .putBoolean(KEY_SECONDS, showSeconds)
                .putBoolean(KEY_MOTION, motion)
                .putBoolean(KEY_HEALTH, showHealth)
                .apply();
    }

    public Settings withTheme(String newTheme) {
        return new Settings(newTheme, showSeconds, motion, showHealth);
    }

    public Settings withSeconds(boolean value) {
        return new Settings(theme, value, motion, showHealth);
    }

    public Settings withMotion(boolean value) {
        return new Settings(theme, showSeconds, value, showHealth);
    }

    public Settings withHealth(boolean value) {
        return new Settings(theme, showSeconds, motion, value);
    }

    public static String normalizeTheme(String value) {
        if (value == null) {
            return THEME_AUTO;
        }
        for (String t : THEMES) {
            if (t.equals(value)) {
                return t;
            }
        }
        return THEME_AUTO;
    }

    public static String label(String theme) {
        switch (normalizeTheme(theme)) {
            case THEME_INK:
                return "墨夜";
            case THEME_JADE:
                return "青瓷";
            case THEME_CINNABAR:
                return "朱砂";
            case THEME_PAPER:
                return "素笺";
            default:
                return "自动";
        }
    }
}
