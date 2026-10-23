package com.zhiwu.wallpaper;

/** A complete colour scheme for one wallpaper frame. */
public final class Palette {

    public final int bgTop;
    public final int bgMid;
    public final int bgBottom;
    public final int textPrimary;
    public final int textSecondary;
    public final int textMuted;
    public final int accent;
    public final int accentBright;
    public final int accent2;
    public final int orb;
    public final int mountainFar;
    public final int mountainMid;
    public final int mountainNear;
    public final int ridge;
    public final int star;
    public final int particle;
    public final int panel;
    public final int ring;
    public final boolean light;

    private Palette(int bgTop, int bgMid, int bgBottom,
                    int textPrimary, int textSecondary, int textMuted,
                    int accent, int accentBright, int accent2, int orb,
                    int mountainFar, int mountainMid, int mountainNear,
                    int ridge, int star, int particle, int panel, int ring,
                    boolean light) {
        this.bgTop = bgTop;
        this.bgMid = bgMid;
        this.bgBottom = bgBottom;
        this.textPrimary = textPrimary;
        this.textSecondary = textSecondary;
        this.textMuted = textMuted;
        this.accent = accent;
        this.accentBright = accentBright;
        this.accent2 = accent2;
        this.orb = orb;
        this.mountainFar = mountainFar;
        this.mountainMid = mountainMid;
        this.mountainNear = mountainNear;
        this.ridge = ridge;
        this.star = star;
        this.particle = particle;
        this.panel = panel;
        this.ring = ring;
        this.light = light;
    }

    private static final int IVORY = 0xFFFAF4E8;
    private static final int MUTED_WARM = 0xFFD6D0C4;
    private static final int MUTED_DIM = 0xFFA9A59B;

    public static final Palette INK = new Palette(
            0xFF05070D, 0xFF0A1020, 0xFF111A2A,
            IVORY, MUTED_WARM, MUTED_DIM,
            0xFFD8B36A, 0xFFF4D99E, 0xFF7896AA, 0xFFE7DEC7,
            0xFF0B1220, 0xFF080D17, 0xFF05080F,
            0xFFC9A96E, 0xFFE8E2D2, 0xFFD8B36A,
            0xFF0B111C, 0xFF3A3D3C, false);

    public static final Palette JADE = new Palette(
            0xFF04100F, 0xFF0A1C1A, 0xFF102A25,
            0xFFF3F8EF, 0xFFC6D8CF, 0xFF9DB3A9,
            0xFFA9D0BE, 0xFFD9EFE4, 0xFFC9A96E, 0xFFDCE8D8,
            0xFF071714, 0xFF05100E, 0xFF030A09,
            0xFFA9D0BE, 0xFFDCE8E0, 0xFFA9D0BE,
            0xFF0A1A17, 0xFF2F4540, false);

    public static final Palette CINNABAR = new Palette(
            0xFF120608, 0xFF240C0F, 0xFF341114,
            0xFFFFF2E7, 0xFFE0C8BE, 0xFFB99B91,
            0xFFE06A4E, 0xFFF3A082, 0xFFD8B36A, 0xFFE8C9B8,
            0xFF1B090C, 0xFF120608, 0xFF0A0304,
            0xFFE06A4E, 0xFFE8D8C8, 0xFFE06A4E,
            0xFF1B0A0D, 0xFF4A2A28, false);

    public static final Palette PAPER = new Palette(
            0xFFF5EFE2, 0xFFEDE3D0, 0xFFE2D4BC,
            0xFF211E19, 0xFF4E463A, 0xFF6D6253,
            0xFFA8442E, 0xFFC15B3E, 0xFF3E5C55, 0xFFC9A96E,
            0xFFD9CBB4, 0xFFCDBDA4, 0xFFBFAD92,
            0xFFA8442E, 0xFF6B5A45, 0xFFA8442E,
            0xFFFFFAEE, 0xFFD8C9B2, true);

    private static final Palette PREDAWN = new Palette(
            0xFF070812, 0xFF141226, 0xFF281A2A,
            IVORY, MUTED_WARM, MUTED_DIM,
            0xFFC9A0A0, 0xFFE8C3A8, 0xFF8C9DBB, 0xFFD8C8C0,
            0xFF0C0916, 0xFF08050E, 0xFF05030A,
            0xFFC9A0A0, 0xFFE2D6CC, 0xFFC9A0A0,
            0xFF0B0A16, 0xFF3B3544, false);

    private static final Palette DAWN = new Palette(
            0xFF0A1018, 0xFF1A2327, 0xFF383129,
            IVORY, MUTED_WARM, MUTED_DIM,
            0xFFD8A86E, 0xFFF0D0A0, 0xFF91A9A4, 0xFFF0D0A0,
            0xFF0E1418, 0xFF0A0E12, 0xFF06090B,
            0xFFD8A86E, 0xFFF0E0C8, 0xFFD8A86E,
            0xFF111820, 0xFF464A44, false);

    private static final Palette MORNING = new Palette(
            0xFF071312, 0xFF112320, 0xFF23423A,
            IVORY, MUTED_WARM, MUTED_DIM,
            0xFFC9BE8E, 0xFFE8DDB0, 0xFF9EC6B8, 0xFFF0E3C0,
            0xFF091816, 0xFF06110F, 0xFF030A08,
            0xFFC9BE8E, 0xFFE6EBDD, 0xFFC9BE8E,
            0xFF0A1A17, 0xFF394B43, false);

    private static final Palette NOON = new Palette(
            0xFF0A1715, 0xFF15302B, 0xFF2B4E43,
            IVORY, MUTED_WARM, MUTED_DIM,
            0xFFD9C28C, 0xFFF4E2B4, 0xFFA8C7BA, 0xFFF6E7C4,
            0xFF0A1A17, 0xFF07120F, 0xFF040B09,
            0xFFD9C28C, 0xFFE8EFDF, 0xFFD9C28C,
            0xFF0B1E1A, 0xFF41564C, false);

    private static final Palette AFTERNOON = new Palette(
            0xFF101210, 0xFF24221B, 0xFF3B3224,
            IVORY, MUTED_WARM, MUTED_DIM,
            0xFFDDB071, 0xFFF2D09A, 0xFFB5AD8C, 0xFFF0D8A8,
            0xFF141611, 0xFF0E100C, 0xFF080A07,
            0xFFDDB071, 0xFFEBE3CE, 0xFFDDB071,
            0xFF151711, 0xFF4A493C, false);

    private static final Palette DUSK = new Palette(
            0xFF130C13, 0xFF28141B, 0xFF401E22,
            IVORY, MUTED_WARM, MUTED_DIM,
            0xFFDE855F, 0xFFF2B08C, 0xFFC49A84, 0xFFF0B090,
            0xFF180D12, 0xFF10070A, 0xFF080305,
            0xFFDE855F, 0xFFEADACD, 0xFFDE855F,
            0xFF1A0D12, 0xFF4E3733, false);

    private static final Palette EVENING = new Palette(
            0xFF070A16, 0xFF0D1426, 0xFF171E33,
            IVORY, MUTED_WARM, MUTED_DIM,
            0xFFC9A96E, 0xFFE8C98F, 0xFF8FA3BE, 0xFFD8D0C0,
            0xFF0A0E1A, 0xFF070A13, 0xFF04060B,
            0xFFC9A96E, 0xFFE3DCCB, 0xFFC9A96E,
            0xFF0A101C, 0xFF3A3F48, false);

    private static final float[] AUTO_HOURS = {0f, 4.5f, 6.5f, 9f, 12f, 16f, 18.5f, 21f, 24f};
    private static final Palette[] AUTO = {
            INK, PREDAWN, DAWN, MORNING, NOON, AFTERNOON, DUSK, EVENING, INK
    };

    public static Palette forSettings(Settings settings, int minutesOfDay) {
        switch (Settings.normalizeTheme(settings.theme)) {
            case Settings.THEME_INK:
                return INK;
            case Settings.THEME_JADE:
                return JADE;
            case Settings.THEME_CINNABAR:
                return CINNABAR;
            case Settings.THEME_PAPER:
                return PAPER;
            default:
                return auto(minutesOfDay);
        }
    }

    public static Palette auto(int minutesOfDay) {
        float h = (minutesOfDay % 1440) / 60f;
        int i = 0;
        while (i < AUTO_HOURS.length - 2 && h >= AUTO_HOURS[i + 1]) {
            i++;
        }
        float span = AUTO_HOURS[i + 1] - AUTO_HOURS[i];
        float t = span <= 0 ? 0f : (h - AUTO_HOURS[i]) / span;
        t = t * t * (3f - 2f * t);
        return lerp(AUTO[i], AUTO[i + 1], t);
    }

    private static Palette lerp(Palette a, Palette b, float t) {
        return new Palette(
                blend(a.bgTop, b.bgTop, t),
                blend(a.bgMid, b.bgMid, t),
                blend(a.bgBottom, b.bgBottom, t),
                blend(a.textPrimary, b.textPrimary, t),
                blend(a.textSecondary, b.textSecondary, t),
                blend(a.textMuted, b.textMuted, t),
                blend(a.accent, b.accent, t),
                blend(a.accentBright, b.accentBright, t),
                blend(a.accent2, b.accent2, t),
                blend(a.orb, b.orb, t),
                blend(a.mountainFar, b.mountainFar, t),
                blend(a.mountainMid, b.mountainMid, t),
                blend(a.mountainNear, b.mountainNear, t),
                blend(a.ridge, b.ridge, t),
                blend(a.star, b.star, t),
                blend(a.particle, b.particle, t),
                blend(a.panel, b.panel, t),
                blend(a.ring, b.ring, t),
                a.light || b.light);
    }

    public static int blend(int c1, int c2, float t) {
        float u = 1f - t;
        int a = (int) (((c1 >>> 24) & 0xFF) * u + ((c2 >>> 24) & 0xFF) * t);
        int r = (int) (((c1 >>> 16) & 0xFF) * u + ((c2 >>> 16) & 0xFF) * t);
        int g = (int) (((c1 >>> 8) & 0xFF) * u + ((c2 >>> 8) & 0xFF) * t);
        int b = (int) ((c1 & 0xFF) * u + (c2 & 0xFF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int alpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    public boolean sameBackground(Palette other) {
        return other != null
                && bgTop == other.bgTop
                && bgMid == other.bgMid
                && bgBottom == other.bgBottom
                && mountainFar == other.mountainFar
                && mountainMid == other.mountainMid
                && mountainNear == other.mountainNear
                && ridge == other.ridge
                && accent == other.accent
                && light == other.light;
    }

}
