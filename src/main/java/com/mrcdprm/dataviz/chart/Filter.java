package com.mrcdprm.dataviz.chart;

import com.mrcdprm.dataviz.data.Column;
import com.mrcdprm.dataviz.data.ColumnType;
import com.mrcdprm.dataviz.data.DataSet;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * Satır filtresi. Üç çeşit: metin sütununda seçili değerler (VALUES), sayı sütununda aralık (RANGE),
 * tarih sütununda tarih aralığı (DATES; ISO "2024-02-19" metni). Aralığın boş ucu sınırsız demektir.
 * Proje dosyasına bu hâliyle yazılır.
 */
public record Filter(String column, Kind kind, List<String> values, Double min, Double max, String from, String to) {

    public enum Kind {
        VALUES,
        RANGE,
        DATES
    }

    /** Bir filtrede seçilebilecek en fazla değer sayısı. */
    public static final int MAX_VALUES = 1_000;

    public static Filter values(String column, List<String> values) {
        return new Filter(column, Kind.VALUES, List.copyOf(values), null, null, null, null);
    }

    public static Filter range(String column, Double min, Double max) {
        return new Filter(column, Kind.RANGE, List.of(), min, max, null, null);
    }

    public static Filter dates(String column, LocalDate from, LocalDate to) {
        return new Filter(column, Kind.DATES, List.of(), null, null, from == null ? null : from.toString(),
                to == null ? null : to.toString());
    }

    /** Filtrenin beklediği sütun türü. */
    public ColumnType columnType() {
        return switch (kind) {
            case VALUES -> ColumnType.TEXT;
            case RANGE -> ColumnType.NUMBER;
            case DATES -> ColumnType.DATE;
        };
    }

    /**
     * Satır koşuluna çevirir. Sütun yoksa ya da türü filtreye uymuyorsa (ör. kullanıcı sütun türünü değiştirdi)
     * boş döner; çağıran filtreyi yok sayar ve kullanıcıya bildirir.
     */
    public Optional<IntPredicate> compile(DataSet ds) {
        final Optional<Column> found = ds.column(column);
        if (found.isEmpty() || found.get().type() != columnType())
            return Optional.empty();
        final Column c = found.get();
        return switch (kind) {
            case VALUES -> {
                final Set<String> set = new HashSet<>(values == null ? List.of() : values);
                yield Optional.of(row -> set.contains(c.raw(row).strip()));
            }
            case RANGE -> {
                final double lo = min == null ? Double.NEGATIVE_INFINITY : min;
                final double hi = max == null ? Double.POSITIVE_INFINITY : max;
                yield Optional.of(row -> {
                    final double v = c.number(row);
                    return !Double.isNaN(v) && v >= lo && v <= hi;
                });
            }
            case DATES -> {
                final LocalDate a = parse(from);
                final LocalDate b = parse(to);
                yield Optional.of(row -> {
                    final LocalDate d = c.date(row);
                    return d != null && (a == null || !d.isBefore(a)) && (b == null || !d.isAfter(b));
                });
            }
        };
    }

    public LocalDate fromDate() {
        return parse(from);
    }

    public LocalDate toDate() {
        return parse(to);
    }

    private static LocalDate parse(String iso) {
        if (iso == null || iso.isBlank())
            return null;
        try {
            return LocalDate.parse(iso);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
