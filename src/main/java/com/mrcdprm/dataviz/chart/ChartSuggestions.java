package com.mrcdprm.dataviz.chart;

import com.mrcdprm.dataviz.data.Column;
import com.mrcdprm.dataviz.data.ColumnStats;
import com.mrcdprm.dataviz.data.ColumnType;
import com.mrcdprm.dataviz.data.DataSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Yeni yüklenen veri setine göre başlangıç grafikleri önerir: zaman serisi, kategori karşılaştırması,
 * dağılım ve histogram. Kullanıcı boş bir panoyla değil, verisini hemen görerek başlar.
 */
public final class ChartSuggestions {

    /** Oran, yüzde ya da ortalama gibi toplanması anlamsız sütunlar ortalamayla gösterilir. */
    private static final Pattern AVERAGE_LIKE = Pattern.compile(
            "(?iu).*(%|rate|ratio|oran|per.?capita|kişi başı|anomal|fark|avg|average|ortalama|doluluk|temperature|sıcaklık|score|puan|price|fiyat|yüzde|percent).*");

    private ChartSuggestions() {
    }

    public static List<ChartSpec> suggest(DataSet ds) {
        final List<ChartSpec> out = new ArrayList<>();
        final List<Column> numbers = ds.columns(ColumnType.NUMBER);
        final Optional<Column> date = ds.columns(ColumnType.DATE).stream().findFirst();
        final Optional<Column> category = ds.columns(ColumnType.TEXT).stream()
                .filter(c -> {
                    final ColumnStats s = c.stats();
                    return s.distinct() >= 2 && s.distinct() <= Math.max(50, ds.rowCount() / 2);
                })
                .findFirst();
        final List<Column> measures = numbers.stream().filter(c -> !looksLikeYear(c)).toList();
        final Optional<Column> year = numbers.stream().filter(ChartSuggestions::looksLikeYear).findFirst();

        // 1) zaman serisi: tarih ya da yıl sütunu varsa ilk ölçü(ler)in çizgisi
        if (!measures.isEmpty() && (date.isPresent() || year.isPresent())) {
            final Column first = measures.get(0);
            final List<String> y = measures.stream().limit(date.isPresent() ? Math.min(3, measures.size()) : 1)
                    .map(Column::name).toList();
            final ChartSpec line = ChartSpec.of(ChartType.LINE, null, date.map(Column::name).orElseGet(() -> year.get().name()), y,
                    aggregation(first));
            out.add(date.isPresent() ? line.withBucket(bucketFor(date.get())).withWide(true) : line.withWide(true));
        }
        // 2) kategori karşılaştırması: en büyük 10
        if (category.isPresent())
            out.add(ChartSpec.of(ChartType.BAR, null, category.get().name(),
                    measures.isEmpty() ? List.of() : List.of(measures.get(0).name()),
                    measures.isEmpty() ? Aggregation.COUNT : aggregation(measures.get(0))).withSort(SortOrder.VALUE_DESC, 10));
        // 3) histogram: ilk ölçünün dağılımı
        if (!measures.isEmpty())
            out.add(ChartSpec.of(ChartType.HISTOGRAM, null, measures.get(0).name(), List.of(), Aggregation.COUNT));
        // 4) dağılım: iki ölçü arasındaki ilişki
        if (measures.size() >= 2)
            out.add(ChartSpec.of(ChartType.SCATTER, null, measures.get(0).name(), List.of(measures.get(1).name()),
                    Aggregation.SUM));
        // hiç sayı yoksa: kategori sayımı pastası
        if (measures.isEmpty() && category.isPresent())
            out.add(ChartSpec.of(ChartType.PIE, null, category.get().name(), List.of(), Aggregation.COUNT));
        return out;
    }

    static Aggregation aggregation(Column c) {
        return AVERAGE_LIKE.matcher(c.name()).matches() ? Aggregation.AVG : Aggregation.SUM;
    }

    /** Adı "yıl" ise ya da bütün değerleri 1800-2200 arası, az çeşitli tam sayılarsa yıl sütunudur (ölçü değil, eksen). */
    static boolean looksLikeYear(Column c) {
        final String n = c.name().toLowerCase(java.util.Locale.ROOT);
        if (n.equals("year") || n.equals("yıl") || n.equals("yil"))
            return true;
        final ColumnStats s = c.stats();
        if (s.filled() == 0 || s.min() < 1800 || s.max() > 2200)
            return false;
        for (int i = 0; i < c.size(); i++)
            if (!c.missing(i) && c.number(i) != Math.rint(c.number(i)))
                return false;
        // yıllar az sayıda farklı değer alır (1950-2023 = 74); fiyat gibi ölçüler genelde çok çeşitlidir
        return s.distinct() <= 300;
    }

    /** Tarih aralığına göre gruplama: 30 yıldan uzunsa yıl, 4 aydan uzunsa ay, değilse gün. */
    static DateBucket bucketFor(Column date) {
        final ColumnStats s = date.stats();
        if (s.first() == null)
            return DateBucket.MONTH;
        final long days = java.time.temporal.ChronoUnit.DAYS.between(s.first(), s.last());
        if (days > 365L * 30)
            return DateBucket.YEAR;
        if (days > 120)
            return DateBucket.MONTH;
        return DateBucket.DAY;
    }
}
