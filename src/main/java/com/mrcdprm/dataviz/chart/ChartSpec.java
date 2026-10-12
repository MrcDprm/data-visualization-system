package com.mrcdprm.dataviz.chart;

import java.util.List;
import java.util.UUID;

/**
 * Bir grafik kartının tanımı: tür, X sütunu, Y sütunları, toplama, seri (kırılım) sütunu, tarih gruplama,
 * sıralama, ilk N, histogram aralık sayısı, renk paleti, başlık ve eksen adları, kartın kendi filtreleri.
 * Proje dosyasına bu hâliyle yazılır; okunurken {@link #normalized()} ile eksik/geçersiz alanlar düzeltilir.
 *
 * @param y     Y sütunları; boşsa satırlar sayılır (ADET). Birden fazlaysa her biri ayrı seri olur.
 * @param series kırılım sütunu: değerleri ayrı seriler olur (tek Y sütunuyla kullanılır)
 * @param topN  0 = hepsi
 * @param bins  histogram aralık sayısı; 0 = otomatik
 * @param wide  pano üzerinde iki sütun genişliğinde gösterilsin mi
 */
public record ChartSpec(String id, ChartType type, String title, String x, List<String> y, Aggregation aggregation,
        String series, DateBucket bucket, SortOrder sort, int topN, int bins, Palette palette, String xLabel,
        String yLabel, List<Filter> filters, boolean wide) {

    public static final int MAX_TOP_N = 100;
    public static final int MAX_BINS = 100;
    public static final int MAX_Y = 10;
    public static final int MAX_TITLE = 120;

    public static ChartSpec of(ChartType type, String title, String x, List<String> y, Aggregation aggregation) {
        return new ChartSpec(UUID.randomUUID().toString(), type, title, x, y, aggregation, null, DateBucket.MONTH,
                SortOrder.AUTO, 0, 0, Palette.DEFAULT, null, null, List.of(), false);
    }

    /** Eksik ya da sınır dışı alanları güvenli varsayılanlara çeker (dosyadan okunan değerlere güvenilmez). */
    public ChartSpec normalized() {
        return new ChartSpec(
                id == null || id.isBlank() || id.length() > 64 ? UUID.randomUUID().toString() : id,
                type == null ? ChartType.BAR : type,
                clip(title, MAX_TITLE),
                x,
                y == null ? List.of() : y.stream().filter(s -> s != null && !s.isBlank()).distinct().limit(MAX_Y).toList(),
                aggregation == null ? Aggregation.SUM : aggregation,
                series == null || series.isBlank() ? null : series,
                bucket == null ? DateBucket.MONTH : bucket,
                sort == null ? SortOrder.AUTO : sort,
                Math.max(0, Math.min(MAX_TOP_N, topN)),
                Math.max(0, Math.min(MAX_BINS, bins)),
                palette == null ? Palette.DEFAULT : palette,
                clip(xLabel, MAX_TITLE),
                clip(yLabel, MAX_TITLE),
                filters == null ? List.of() : filters.stream().filter(f -> f != null && f.kind() != null && f.column() != null)
                        .limit(20).toList(),
                wide);
    }

    private static String clip(String s, int max) {
        if (s == null)
            return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    public ChartSpec withId(String newId) {
        return new ChartSpec(newId, type, title, x, y, aggregation, series, bucket, sort, topN, bins, palette, xLabel,
                yLabel, filters, wide);
    }

    public ChartSpec withWide(boolean value) {
        return new ChartSpec(id, type, title, x, y, aggregation, series, bucket, sort, topN, bins, palette, xLabel,
                yLabel, filters, value);
    }

    public ChartSpec withFilters(List<Filter> value) {
        return new ChartSpec(id, type, title, x, y, aggregation, series, bucket, sort, topN, bins, palette, xLabel,
                yLabel, List.copyOf(value), wide);
    }

    public ChartSpec withSeries(String value) {
        return new ChartSpec(id, type, title, x, y, aggregation, value, bucket, sort, topN, bins, palette, xLabel,
                yLabel, filters, wide);
    }

    public ChartSpec withBucket(DateBucket value) {
        return new ChartSpec(id, type, title, x, y, aggregation, series, value, sort, topN, bins, palette, xLabel,
                yLabel, filters, wide);
    }

    public ChartSpec withSort(SortOrder value, int n) {
        return new ChartSpec(id, type, title, x, y, aggregation, series, bucket, value, n, bins, palette, xLabel,
                yLabel, filters, wide);
    }

    public ChartSpec withBins(int value) {
        return new ChartSpec(id, type, title, x, y, aggregation, series, bucket, sort, topN, value, palette, xLabel,
                yLabel, filters, wide);
    }

    public ChartSpec withPalette(Palette value) {
        return new ChartSpec(id, type, title, x, y, aggregation, series, bucket, sort, topN, bins, value, xLabel,
                yLabel, filters, wide);
    }

    public ChartSpec withLabels(String newX, String newY) {
        return new ChartSpec(id, type, title, x, y, aggregation, series, bucket, sort, topN, bins, palette, newX, newY,
                filters, wide);
    }
}
