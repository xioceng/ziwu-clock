package com.zhiwu.wallpaper;

import java.util.Locale;

/** The twelve double-hours and their meridian correspondence. */
public final class Shichen {

    public static final String[] BRANCH = {
            "子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"
    };

    public static final String[] MERIDIAN = {
            "胆经", "肝经", "肺经", "大肠经", "胃经", "脾经",
            "心经", "小肠经", "膀胱经", "肾经", "心包经", "三焦经"
    };

    public static final String[] ORGAN = {
            "胆", "肝", "肺", "大肠", "胃", "脾", "心", "小肠", "膀胱", "肾", "心包", "三焦"
    };

    public static final String[] HEALTH = {
            "宜安卧养胆，忌熬夜与宵夜",
            "深睡藏血，忌饮酒与动怒",
            "肺朝百脉，宜深睡勿早醒",
            "起身排浊，饮一杯温水",
            "按时早餐，忌空腹劳作",
            "脾主运化，宜专注少食",
            "小憩养心，忌剧烈运动",
            "午餐消化，宜舒缓活动",
            "多饮水，宜运动排浊",
            "晚餐清淡，养肾藏精",
            "散步放松，舒缓情绪",
            "准备入睡，忌情绪激动"
    };

    private Shichen() {
    }

    /** Index 0 = 子时 (23:00-01:00), 1 = 丑时, ... */
    public static int indexOf(int hour) {
        return ((hour + 1) % 24) / 2;
    }

    public static String name(int index) {
        return BRANCH[index % 12] + "时";
    }

    public static String meridian(int index) {
        return MERIDIAN[index % 12];
    }

    public static String health(int index) {
        return HEALTH[index % 12];
    }

    public static int startHour(int index) {
        return (23 + index * 2) % 24;
    }

    public static String range(int index) {
        int start = startHour(index);
        int end = (start + 2) % 24;
        return String.format(Locale.CHINA, "%02d:00 — %02d:00", start, end);
    }

    /** Seconds elapsed since the current double-hour began (handles 子时 across midnight). */
    private static int elapsedSeconds(int hour, int minute, int second) {
        int index = indexOf(hour);
        int nowMinute = hour * 60 + minute;
        int elapsedMinute = nowMinute - startHour(index) * 60;
        if (elapsedMinute < 0) {
            elapsedMinute += 24 * 60;
        }
        return elapsedMinute * 60 + second;
    }

    /** Progress through the current double-hour, 0..1. */
    public static float progress(int hour, int minute, int second) {
        return Math.max(0f, Math.min(1f, elapsedSeconds(hour, minute, second) / 7200f));
    }

    /** Whole seconds until the next double-hour begins. */
    public static int secondsUntilNext(int hour, int minute, int second) {
        return Math.max(0, 7200 - elapsedSeconds(hour, minute, second));
    }

    public static String countdown(int totalSeconds) {
        int s = Math.max(0, totalSeconds);
        return String.format(Locale.CHINA, "%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
    }
}
