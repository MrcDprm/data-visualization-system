package com.mrcdprm.dataviz.data;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Metindeki tarihi okur. Dosyalarda tarih "2024-02-19", "19.02.2024", "19/02/2024", "02/19/2024" ya da
 * "2024-02" (ay) gibi yazılır. Bir sütundaki bütün değerler aynı biçimde kabul edilir; biçim sütundaki
 * örneklerin en çoğunu okuyabilen kalıptır. Saat kısmı ("2024-02-19 14:30") atılır, sadece gün kullanılır.
 */
public final class Dates {

    /** Denenen kalıplar; aynı sayıda değeri okuyan iki kalıptan listede önce olan seçilir (gün/ay, ay/gün'den önce). */
    public static final List<String> PATTERNS = List.of(
            "uuuu-M-d", "uuuu/M/d", "d.M.uuuu", "d/M/uuuu", "M/d/uuuu", "d-M-uuuu", "uuuu-M", "M/uuuu", "M.uuuu");

    private static final Pattern TIME_SUFFIX = Pattern.compile("[ T]\\d{1,2}:\\d{2}(:\\d{2}(\\.\\d+)?)?(Z|[+-]\\d{2}:?\\d{2})?$");

    private Dates() {
    }

    private static DateTimeFormatter formatter(String pattern) {
        return DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.STRICT);
    }

    /** Verilen kalıpla okur; okunamazsa null. Ay kalıplarında (uuuu-M) ayın ilk günü döner. */
    public static LocalDate parse(String text, String pattern) {
        if (text == null || pattern == null)
            return null;
        String s = text.strip();
        if (s.isEmpty() || s.length() > 40)
            return null;
        s = TIME_SUFFIX.matcher(s).replaceFirst("");
        try {
            if (pattern.contains("d"))
                return LocalDate.parse(s, formatter(pattern));
            return YearMonth.parse(s, formatter(pattern)).atDay(1);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Örneklerin en çoğunu okuyan kalıp; hiçbiri okuyamıyorsa null. */
    public static String detect(Iterable<String> samples) {
        String best = null;
        int bestCount = 0;
        for (String pattern : PATTERNS) {
            int count = 0;
            for (String s : samples)
                if (s != null && !s.isBlank() && parse(s, pattern) != null)
                    count++;
            if (count > bestCount) {
                best = pattern;
                bestCount = count;
            }
        }
        return best;
    }
}
