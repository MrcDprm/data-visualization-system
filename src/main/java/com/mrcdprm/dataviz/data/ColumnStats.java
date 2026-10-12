package com.mrcdprm.dataviz.data;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sütun özeti: dolu ve eksik hücre, farklı değer sayısı; sayıda en küçük/en büyük/ortalama/toplam,
 * tarihte ilk/son tarih, metinde en sık geçen değerler.
 */
public record ColumnStats(int filled, int missing, int distinct, boolean distinctCapped, double min, double max,
        double mean, double sum, LocalDate first, LocalDate last, List<Map.Entry<String, Integer>> topValues) {

    /** Farklı değer sayımı bu sınırda durur (çok büyük metin sütunlarında bellek şişmesin). */
    public static final int DISTINCT_CAP = 100_000;

    static ColumnStats of(Column c) {
        int filled = 0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        double sum = 0;
        LocalDate first = null;
        LocalDate last = null;
        final Map<String, Integer> counts = new HashMap<>();
        boolean capped = false;
        for (int i = 0; i < c.size(); i++) {
            if (c.missing(i))
                continue;
            filled++;
            switch (c.type()) {
                case NUMBER -> {
                    final double v = c.number(i);
                    min = Math.min(min, v);
                    max = Math.max(max, v);
                    sum += v;
                }
                case DATE -> {
                    final LocalDate d = c.date(i);
                    if (first == null || d.isBefore(first))
                        first = d;
                    if (last == null || d.isAfter(last))
                        last = d;
                }
                case TEXT -> {
                }
            }
            final String key = c.raw(i).strip();
            if (counts.size() < DISTINCT_CAP || counts.containsKey(key))
                counts.merge(key, 1, Integer::sum);
            else
                capped = true;
        }
        final List<Map.Entry<String, Integer>> top = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                .limit(5)
                .map(e -> Map.entry(e.getKey(), e.getValue()))
                .toList();
        final boolean numbers = c.type() == ColumnType.NUMBER && filled > 0;
        return new ColumnStats(filled, c.size() - filled, counts.size(), capped, numbers ? min : Double.NaN,
                numbers ? max : Double.NaN, numbers ? sum / filled : Double.NaN, numbers ? sum : Double.NaN, first, last, top);
    }
}
