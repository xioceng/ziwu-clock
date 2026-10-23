package com.zhiwu.wallpaper;

import com.nlf.calendar.JieQi;
import com.nlf.calendar.Lunar;
import com.nlf.calendar.Solar;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * A snapshot of the Chinese calendar for one civil day.
 *
 * <p>The calculation is local and deterministic. Days run from midnight for the
 * displayed day pillar, which is the convention used by most desk calendars.</p>
 */
public final class CalendarData {

    public final int year;
    public final int month;
    public final int day;
    public final int hour;
    public final int minute;
    public final int second;

    public final String lunarDate;
    public final String lunarYearGanZhi;
    public final String yearGanZhi;
    public final String monthGanZhi;
    public final String dayGanZhi;
    public final String zodiac;
    public final String weekday;
    public final String gregorian;

    public final String termName;
    public final String termTodayName;
    public final String nextTermName;
    public final String termPhase;
    public final String termTime;
    public final String nextTermDate;
    public final int daysToNextTerm;
    public final boolean termToday;
    public final boolean termPending;
    public final boolean termDay;
    public final String festival;

    public final int lunarDay;
    public final String moonPhase;
    public final String season;
    public final boolean leapMonth;

    private CalendarData(Builder b) {
        year = b.year;
        month = b.month;
        day = b.day;
        hour = b.hour;
        minute = b.minute;
        second = b.second;
        lunarDate = b.lunarDate;
        lunarYearGanZhi = b.lunarYearGanZhi;
        yearGanZhi = b.yearGanZhi;
        monthGanZhi = b.monthGanZhi;
        dayGanZhi = b.dayGanZhi;
        zodiac = b.zodiac;
        weekday = b.weekday;
        gregorian = b.gregorian;
        termName = b.termName;
        termTodayName = b.termTodayName;
        nextTermName = b.nextTermName;
        termPhase = b.termPhase;
        termTime = b.termTime;
        nextTermDate = b.nextTermDate;
        daysToNextTerm = b.daysToNextTerm;
        termToday = b.termToday;
        termPending = b.termPending;
        termDay = b.termDay;
        festival = b.festival;
        lunarDay = b.lunarDay;
        moonPhase = b.moonPhase;
        season = b.season;
        leapMonth = b.leapMonth;
    }

    public static CalendarData from(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH) + 1;
        int d = c.get(Calendar.DAY_OF_MONTH);
        int hh = c.get(Calendar.HOUR_OF_DAY);
        int mm = c.get(Calendar.MINUTE);
        int ss = c.get(Calendar.SECOND);

        Solar solar = Solar.fromYmdHms(y, m, d, hh, mm, ss);
        Lunar lunar = solar.getLunar();

        Builder b = new Builder();
        b.year = y;
        b.month = m;
        b.day = d;
        b.hour = hh;
        b.minute = mm;
        b.second = ss;

        b.leapMonth = lunar.getMonth() < 0;
        b.lunarDate = lunar.getMonthInChinese() + "月" + lunar.getDayInChinese();
        b.lunarYearGanZhi = lunar.getYearInGanZhiExact() + "年";
        b.yearGanZhi = lunar.getYearInGanZhiExact();
        b.monthGanZhi = lunar.getMonthInGanZhiExact();
        b.dayGanZhi = lunar.getDayInGanZhi();
        b.zodiac = lunar.getYearShengXiaoExact();
        b.weekday = "星期" + solar.getWeekInChinese();
        b.gregorian = String.format(Locale.CHINA, "%d.%02d.%02d", y, m, d);
        b.lunarDay = lunar.getDay();
        b.moonPhase = moonPhase(lunar.getDay());
        b.season = seasonOf(m, d);

        Solar today = Solar.fromYmd(y, m, d);
        JieQi termToday = lunar.getCurrentJieQi();
        JieQi next;
        if (termToday != null) {
            Solar termMoment = termToday.getSolar();
            b.termDay = true;
            b.termTodayName = termToday.getName();
            b.termTime = String.format(Locale.CHINA, "%02d:%02d",
                    termMoment.getHour(), termMoment.getMinute());
            Solar now = Solar.fromYmdHms(y, m, d, hh, mm, ss);
            b.termPending = now.isBefore(termMoment);
            if (b.termPending) {
                // The almanac page has turned, but the sun has not yet reached the term.
                JieQi prev = lunar.getPrevJieQi();
                b.termName = prev == null ? "" : prev.getName();
                if (prev != null) {
                    int since = Math.max(0, daysBetween(dateOnly(prev.getSolar()), today));
                    b.termPhase = "第" + (since + 1) + "天";
                } else {
                    b.termPhase = "";
                }
                b.nextTermName = termToday.getName();
                b.daysToNextTerm = 0;
                b.nextTermDate = String.format(Locale.CHINA, "%02d.%02d", m, d);
            } else {
                b.termToday = true;
                b.termName = termToday.getName();
                b.termPhase = "今日交节";
                next = lunar.getNextJieQi(true);
                b.nextTermName = next == null ? "" : next.getName();
                if (next != null) {
                    b.daysToNextTerm = Math.max(0, daysBetween(today, dateOnly(next.getSolar())));
                    b.nextTermDate = String.format(Locale.CHINA, "%02d.%02d",
                            next.getSolar().getMonth(), next.getSolar().getDay());
                } else {
                    b.daysToNextTerm = 0;
                    b.nextTermDate = "";
                }
            }
        } else {
            JieQi prev = lunar.getPrevJieQi();
            next = lunar.getNextJieQi(true);
            b.termName = prev == null ? "" : prev.getName();
            if (prev != null) {
                int since = Math.max(0, daysBetween(dateOnly(prev.getSolar()), today));
                b.termPhase = "第" + (since + 1) + "天";
            } else {
                b.termPhase = "";
            }
            b.nextTermName = next == null ? "" : next.getName();
            if (next != null) {
                b.daysToNextTerm = Math.max(0, daysBetween(today, dateOnly(next.getSolar())));
                b.nextTermDate = String.format(Locale.CHINA, "%02d.%02d",
                        next.getSolar().getMonth(), next.getSolar().getDay());
            } else {
                b.daysToNextTerm = 0;
                b.nextTermDate = "";
            }
        }

        b.festival = firstFestival(lunar, solar);
        return new CalendarData(b);
    }

    private static Solar dateOnly(Solar s) {
        return Solar.fromYmd(s.getYear(), s.getMonth(), s.getDay());
    }

    private static int daysBetween(Solar from, Solar to) {
        return Math.abs(from.subtract(to));
    }

    private static String firstFestival(Lunar lunar, Solar solar) {
        List<String> festivals = lunar.getFestivals();
        if (festivals != null && !festivals.isEmpty()) {
            return festivals.get(0);
        }
        festivals = solar.getFestivals();
        if (festivals != null && !festivals.isEmpty()) {
            return festivals.get(0);
        }
        return null;
    }

    private static String moonPhase(int lunarDay) {
        if (lunarDay == 1) {
            return "朔";
        } else if (lunarDay <= 6) {
            return "蛾眉月";
        } else if (lunarDay <= 9) {
            return "上弦";
        } else if (lunarDay <= 13) {
            return "盈凸";
        } else if (lunarDay <= 16) {
            return "望";
        } else if (lunarDay <= 20) {
            return "亏凸";
        } else if (lunarDay <= 23) {
            return "下弦";
        } else if (lunarDay <= 29) {
            return "残月";
        }
        return "晦";
    }

    private static String seasonOf(int month, int day) {
        int approx = month * 100 + day;
        if (approx >= 204 && approx < 505) {
            return "春";
        }
        if (approx >= 505 && approx < 807) {
            return "夏";
        }
        if (approx >= 807 && approx < 1107) {
            return "秋";
        }
        return "冬";
    }

    private static final class Builder {
        int year, month, day, hour, minute, second;
        String lunarDate, lunarYearGanZhi, yearGanZhi, monthGanZhi, dayGanZhi;
        String zodiac, weekday, gregorian, termName, termTodayName, nextTermName;
        String termPhase, termTime, nextTermDate;
        int daysToNextTerm;
        boolean termToday, termPending, termDay;
        String festival, moonPhase, season;
        int lunarDay;
        boolean leapMonth;
    }
}
