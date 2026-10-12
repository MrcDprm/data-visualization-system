package com.mrcdprm.dataviz.chart;

import com.mrcdprm.dataviz.data.Column;
import com.mrcdprm.dataviz.data.ColumnType;
import com.mrcdprm.dataviz.data.DataSet;
import com.mrcdprm.dataviz.data.VizException;
import java.text.Collator;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntPredicate;

/**
 * Grafik verisini hesaplar: filtreler → X'e göre gruplama (tarihte gün/ay/çeyrek/yıl) → seri başına toplama
 * (toplam, ortalama, adet, en küçük, en büyük) → sıralama → ilk N. Histogramda eşit genişlikli aralıklar,
 * dağılımda nokta listesi üretir. Çok büyük sonuçlar sınırlanır ve kullanıcıya bildirilir.
 */
public final class ChartEngine {

    /** Çubuk ve pasta grafiğinde en fazla kategori; daha fazlası okunmaz ve çizimi yavaşlatır. */
    public static final int MAX_CATEGORIES = 500;
    /** Çizgi ve alan grafiğinde en fazla nokta (ör. 20 yıllık günlük veri ≈ 7300). */
    public static final int MAX_LINE_CATEGORIES = 10_000;
    public static final int MAX_SERIES = 10;
    public static final int MAX_POINTS = 5_000;
    /** Pastada varsayılan dilim sayısı (+ "Diğer"); grafik 8 renk kullandığı için 8 dilimde renkler tekrar etmez. */
    public static final int PIE_DEFAULT_TOP = 7;

    private ChartEngine() {
    }

    /** Metinler seçili dile göre: "Diğer"/"Other", "Adet"/"Count". */
    public record Labels(String other, String count) {

        public static Labels of(Locale locale) {
            return "tr".equals(locale.getLanguage()) ? new Labels("Diğer", "Adet") : new Labels("Other", "Count");
        }
    }

    public static ChartData compute(DataSet ds, ChartSpec spec, List<Filter> global, Locale locale) {
        final List<Filter> all = new ArrayList<>(global);
        all.addAll(spec.filters());
        final BitSet rows = new BitSet(ds.rowCount());
        rows.set(0, ds.rowCount());
        int ignored = 0;
        for (Filter f : all) {
            final Optional<IntPredicate> p = f.compile(ds);
            if (p.isEmpty()) {
                ignored++;
                continue;
            }
            for (int r = rows.nextSetBit(0); r >= 0; r = rows.nextSetBit(r + 1))
                if (!p.get().test(r))
                    rows.clear(r);
        }
        final Labels labels = Labels.of(locale);
        return switch (spec.type()) {
            case HISTOGRAM -> histogram(ds, spec, rows, ignored, locale, labels);
            case SCATTER -> scatter(ds, spec, rows, ignored, locale);
            default -> categorical(ds, spec, rows, ignored, locale, labels);
        };
    }

    // ---------------------------------------------------------------- kategorili grafikler

    /** Bir kategori/seri hücresinde toplanan değerler. */
    private static final class Acc {
        double sum;
        int count;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;

        void add(double v) {
            sum += v;
            count++;
            min = Math.min(min, v);
            max = Math.max(max, v);
        }

        double value(Aggregation a) {
            if (count == 0)
                return Double.NaN;
            return switch (a) {
                case SUM -> sum;
                case AVG -> sum / count;
                case COUNT -> count;
                case MIN -> min;
                case MAX -> max;
            };
        }
    }

    private static ChartData categorical(DataSet ds, ChartSpec spec, BitSet rows, int ignored, Locale locale, Labels labels) {
        final Column x = column(ds, spec.x());
        final boolean count = spec.aggregation() == Aggregation.COUNT || spec.y().isEmpty();
        final Aggregation aggregation = spec.y().isEmpty() ? Aggregation.COUNT : spec.aggregation();
        final List<Column> measures = new ArrayList<>();
        for (String name : spec.y()) {
            final Column c = column(ds, name);
            if (!count && c.type() != ColumnType.NUMBER)
                throw new VizException("needsNumber", c.name());
            measures.add(c);
        }
        final boolean bySeriesColumn = spec.series() != null && measures.size() <= 1 && spec.type() != ChartType.PIE;
        final Column seriesColumn = bySeriesColumn ? column(ds, spec.series()) : null;

        // kategori -> seri adı -> birikim
        final Map<Object, Map<String, Acc>> cells = new LinkedHashMap<>();
        final Map<String, Double> seriesWeight = new LinkedHashMap<>();
        int used = 0;
        for (int r = rows.nextSetBit(0); r >= 0; r = rows.nextSetBit(r + 1)) {
            final Object key = key(x, r, spec.bucket());
            if (key == null)
                continue;
            boolean counted = false;
            if (measures.isEmpty()) {
                final String s = bySeriesColumn ? seriesKey(seriesColumn, r, spec.bucket(), locale) : labels.count();
                if (s == null)
                    continue;
                add(cells, seriesWeight, key, s, 1);
                counted = true;
            } else if (bySeriesColumn) {
                final String s = seriesKey(seriesColumn, r, spec.bucket(), locale);
                final Column m = measures.get(0);
                if (s != null && !m.missing(r)) {
                    add(cells, seriesWeight, key, s, count ? 1 : m.number(r));
                    counted = true;
                }
            } else {
                for (Column m : measures) {
                    if (!m.missing(r)) {
                        add(cells, seriesWeight, key, m.name(), count ? 1 : m.number(r));
                        counted = true;
                    }
                }
            }
            if (counted)
                used++;
        }

        // seriler: Y sütunu sırası; kırılım sütununda en ağır (en çok değer taşıyan) ilk 10
        List<String> seriesNames;
        int dropped = 0;
        if (bySeriesColumn || measures.isEmpty()) {
            seriesNames = seriesWeight.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .map(Map.Entry::getKey).toList();
            if (seriesNames.size() > MAX_SERIES) {
                dropped = seriesNames.size() - MAX_SERIES;
                seriesNames = seriesNames.subList(0, MAX_SERIES);
            }
            if (bySeriesColumn)
                seriesNames = sortKeys(seriesNames, seriesColumn, locale);
        } else {
            seriesNames = measures.stream().map(Column::name).toList();
        }
        if (spec.type() == ChartType.PIE && seriesNames.size() > 1)
            seriesNames = seriesNames.subList(0, 1);

        // kategori sırası
        final List<Object> keys = new ArrayList<>(cells.keySet());
        final Map<Object, Double> totals = new HashMap<>();
        for (Object k : keys) {
            double t = 0;
            for (String s : seriesNames) {
                final Acc a = cells.get(k).get(s);
                final double v = a == null ? Double.NaN : a.value(aggregation);
                if (!Double.isNaN(v))
                    t += v;
            }
            totals.put(k, t);
        }
        SortOrder sort = spec.sort();
        if (sort == SortOrder.AUTO) {
            final boolean barLike = spec.type() == ChartType.BAR || spec.type() == ChartType.STACKED_BAR
                    || spec.type() == ChartType.PIE;
            sort = x.type() == ColumnType.TEXT && barLike ? SortOrder.VALUE_DESC : SortOrder.LABEL;
        }
        final Comparator<Object> byLabel = labelComparator(locale);
        switch (sort) {
            case VALUE_DESC -> keys.sort(Comparator.comparing((Object k) -> totals.get(k)).reversed().thenComparing(byLabel));
            case VALUE_ASC -> keys.sort(Comparator.comparing((Object k) -> totals.get(k)).thenComparing(byLabel));
            case ORIGINAL -> {
                // LinkedHashMap anahtarları zaten ilk görülme sırasında
            }
            default -> keys.sort(byLabel);
        }

        // ilk N; pastada geri kalanı "Diğer" dilimi olur
        int topN = spec.topN();
        if (spec.type() == ChartType.PIE && topN == 0)
            topN = PIE_DEFAULT_TOP;
        boolean truncated = false;
        double otherValue = 0;
        boolean addOther = false;
        if (topN > 0 && keys.size() > topN) {
            if (spec.type() == ChartType.PIE && (aggregation == Aggregation.SUM || aggregation == Aggregation.COUNT)) {
                for (Object k : keys.subList(topN, keys.size()))
                    otherValue += Math.max(0, totals.get(k));
                addOther = otherValue > 0;
            }
            keys.subList(topN, keys.size()).clear();
        }
        final int limit = spec.type() == ChartType.LINE || spec.type() == ChartType.AREA ? MAX_LINE_CATEGORIES : MAX_CATEGORIES;
        if (keys.size() > limit) {
            keys.subList(limit, keys.size()).clear();
            truncated = true;
        }

        final List<String> categories = new ArrayList<>(keys.size() + 1);
        for (Object k : keys)
            categories.add(label(k, x.type(), spec.bucket(), locale));
        if (addOther)
            categories.add(labels.other());
        final List<ChartData.Series> series = new ArrayList<>();
        for (String s : seriesNames) {
            final double[] values = new double[categories.size()];
            for (int i = 0; i < keys.size(); i++) {
                final Acc a = cells.get(keys.get(i)).get(s);
                values[i] = a == null ? Double.NaN : a.value(aggregation);
                // pastada eksi ve boş değer dilim olamaz
                if (spec.type() == ChartType.PIE && !(values[i] > 0))
                    values[i] = Double.NaN;
            }
            if (addOther)
                values[values.length - 1] = otherValue;
            series.add(new ChartData.Series(s, values));
        }
        return new ChartData(categories, series, null, x.name(), String.join(", ", spec.y()), used, truncated, dropped,
                ignored);
    }

    private static void add(Map<Object, Map<String, Acc>> cells, Map<String, Double> weight, Object key, String series,
            double value) {
        cells.computeIfAbsent(key, k -> new LinkedHashMap<>()).computeIfAbsent(series, s -> new Acc()).add(value);
        weight.merge(series, Math.abs(value), Double::sum);
    }

    /** Satırın kategori anahtarı: metin, tarih (gruplanmış) ya da sayı; eksikse null (satır atlanır). */
    private static Object key(Column c, int row, DateBucket bucket) {
        if (c.missing(row))
            return null;
        return switch (c.type()) {
            case TEXT -> c.raw(row).strip();
            case DATE -> bucket.start(c.date(row));
            case NUMBER -> c.number(row);
        };
    }

    private static String seriesKey(Column c, int row, DateBucket bucket, Locale locale) {
        final Object k = key(c, row, c.type() == ColumnType.DATE ? DateBucket.YEAR : bucket);
        return k == null ? null : label(k, c.type(), DateBucket.YEAR, locale);
    }

    /** Kırılım serilerini değerlerine göre değil etiketlerine göre sıralar (lejant düzenli görünsün). */
    private static List<String> sortKeys(List<String> names, Column c, Locale locale) {
        final List<String> out = new ArrayList<>(names);
        if (c.type() == ColumnType.TEXT)
            out.sort(Collator.getInstance(locale));
        else
            out.sort(Comparator.naturalOrder());
        return out;
    }

    private static Comparator<Object> labelComparator(Locale locale) {
        final Collator collator = Collator.getInstance(locale);
        return (a, b) -> {
            if (a instanceof Double x && b instanceof Double y)
                return Double.compare(x, y);
            if (a instanceof LocalDate x && b instanceof LocalDate y)
                return x.compareTo(y);
            return collator.compare(String.valueOf(a), String.valueOf(b));
        };
    }

    // ---------------------------------------------------------------- histogram

    private static ChartData histogram(DataSet ds, ChartSpec spec, BitSet rows, int ignored, Locale locale, Labels labels) {
        final Column x = column(ds, spec.x());
        if (x.type() != ColumnType.NUMBER)
            throw new VizException("needsNumber", x.name());
        final double[] values = new double[rows.cardinality()];
        int n = 0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int r = rows.nextSetBit(0); r >= 0; r = rows.nextSetBit(r + 1)) {
            final double v = x.number(r);
            if (Double.isNaN(v))
                continue;
            values[n++] = v;
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        if (n == 0)
            return new ChartData(List.of(), List.of(), null, x.name(), labels.count(), 0, false, 0, ignored);
        // Aralık genişliği "güzel" bir sayıya yuvarlanır (1, 2, 2,5, 5 x 10^n) ve başlangıç onun katına çekilir:
        // etiketler "-0,4 – -0,3" gibi okunur; aralık sayısı istenen sayıya yakın olur
        final double width = min == max ? 1 : nice((max - min) / (spec.bins() > 0 ? spec.bins() : autoBins(n)));
        final double start = min == max ? min : Math.floor(min / width) * width;
        final int bins = min == max ? 1 : Math.max(1, (int) Math.ceil((max - start) / width - 1e-9));
        final double[] counts = new double[bins];
        for (int i = 0; i < n; i++) {
            final int b = (int) Math.max(0, Math.min(bins - 1, Math.floor((values[i] - start) / width + 1e-9)));
            counts[b]++;
        }
        final List<String> categories = new ArrayList<>(bins);
        for (int b = 0; b < bins; b++)
            categories.add(min == max ? plain(min, locale) : range(start + b * width, start + (b + 1) * width, width, locale));
        return new ChartData(categories, List.of(new ChartData.Series(labels.count(), counts)), null, x.name(),
                labels.count(), n, false, 0, ignored);
    }

    /** Sturges kuralı: log2(n) + 1 aralık, 5 ile 50 arasında. */
    static int autoBins(int n) {
        return Math.max(5, Math.min(50, (int) Math.ceil(Math.log(n) / Math.log(2)) + 1));
    }

    // ---------------------------------------------------------------- dağılım

    private static ChartData scatter(DataSet ds, ChartSpec spec, BitSet rows, int ignored, Locale locale) {
        final Column x = column(ds, spec.x());
        if (spec.y().isEmpty())
            throw new VizException("needsY");
        final Column y = column(ds, spec.y().get(0));
        if (x.type() != ColumnType.NUMBER)
            throw new VizException("needsNumber", x.name());
        if (y.type() != ColumnType.NUMBER)
            throw new VizException("needsNumber", y.name());
        final Column group = spec.series() == null ? null : column(ds, spec.series());

        final Map<String, List<Integer>> byGroup = new LinkedHashMap<>();
        int total = 0;
        for (int r = rows.nextSetBit(0); r >= 0; r = rows.nextSetBit(r + 1)) {
            if (Double.isNaN(x.number(r)) || Double.isNaN(y.number(r)))
                continue;
            final String g = group == null ? y.name() : seriesKey(group, r, DateBucket.YEAR, locale);
            if (g == null)
                continue;
            byGroup.computeIfAbsent(g, k -> new ArrayList<>()).add(r);
            total++;
        }
        List<Map.Entry<String, List<Integer>>> groups = byGroup.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, List<Integer>> e) -> e.getValue().size()).reversed())
                .toList();
        int dropped = 0;
        if (groups.size() > MAX_SERIES) {
            dropped = groups.size() - MAX_SERIES;
            groups = groups.subList(0, MAX_SERIES);
        }
        // çok fazla nokta varsa eşit aralıklı örnek alınır (her k'inci nokta)
        final int kept = groups.stream().mapToInt(e -> e.getValue().size()).sum();
        final int stride = Math.max(1, (int) Math.ceil((double) kept / MAX_POINTS));
        final List<ChartData.Points> points = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> e : groups) {
            final List<Integer> idx = e.getValue();
            final int size = (idx.size() + stride - 1) / stride;
            final double[] xs = new double[size];
            final double[] ys = new double[size];
            for (int i = 0, j = 0; i < idx.size() && j < size; i += stride, j++) {
                xs[j] = x.number(idx.get(i));
                ys[j] = y.number(idx.get(i));
            }
            points.add(new ChartData.Points(e.getKey(), xs, ys));
        }
        return new ChartData(null, List.of(), points, x.name(), y.name(), total, stride > 1, dropped, ignored);
    }

    // ---------------------------------------------------------------- yardımcılar

    private static Column column(DataSet ds, String name) {
        if (name == null || name.isBlank())
            throw new VizException("needsX");
        return ds.column(name).orElseThrow(() -> new VizException("columnMissing", name));
    }

    /** Kategori etiketi: tarih gruplamasına göre ("Şub 2024", "2024 Ç1"), sayı binlik ayırıcısız (yıllar için). */
    public static String label(Object key, ColumnType type, DateBucket bucket, Locale locale) {
        if (key instanceof LocalDate d)
            return dateLabel(d, bucket, locale);
        if (key instanceof Double v)
            return plain(v, locale);
        return String.valueOf(key);
    }

    public static String dateLabel(LocalDate d, DateBucket bucket, Locale locale) {
        final boolean tr = "tr".equals(locale.getLanguage());
        return switch (bucket) {
            case DAY -> d.format(DateTimeFormatter.ofPattern(tr ? "dd.MM.yyyy" : "MMM d, yyyy", locale));
            case MONTH -> d.format(DateTimeFormatter.ofPattern("MMM yyyy", locale));
            case QUARTER -> tr ? d.getYear() + " Ç" + ((d.getMonthValue() + 2) / 3) : "Q" + ((d.getMonthValue() + 2) / 3) + " " + d.getYear();
            case YEAR -> String.valueOf(d.getYear());
        };
    }

    /** Binlik ayırıcısız sayı: 1950 -> "1950", 2.5 -> "2,5" (Türkçe). */
    public static String plain(double v, Locale locale) {
        final DecimalFormat f = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(locale));
        return f.format(v);
    }

    /** 1, 2, 2,5, 5 ya da 10'un katı olan en yakın üst "güzel" genişlik. */
    static double nice(double raw) {
        final double exp = Math.pow(10, Math.floor(Math.log10(raw)));
        final double f = raw / exp;
        final double n = f <= 1 ? 1 : f <= 2 ? 2 : f <= 2.5 ? 2.5 : f <= 5 ? 5 : 10;
        return n * exp;
    }

    /** Histogram aralığı: "-0,4 – -0,3", "15 – 20"; ondalık basamak sayısı genişlikten gelir. */
    private static String range(double from, double to, double width, Locale locale) {
        final int decimals = Math.max(0, (int) -Math.floor(Math.log10(width) + 1e-9) + (Math.abs(width / Math.pow(10,
                Math.floor(Math.log10(width) + 1e-9)) - 2.5) < 1e-9 ? 1 : 0));
        final DecimalFormat f = new DecimalFormat("#,##0" + (decimals > 0 ? "." + "0".repeat(decimals) : ""),
                DecimalFormatSymbols.getInstance(locale));
        return f.format(clean(from)) + " – " + f.format(clean(to));
    }

    /** Kayan nokta artığını (0,30000000000000004) ve "-0"ı temizler. */
    private static double clean(double v) {
        final double r = Math.round(v * 1e9) / 1e9;
        return r == 0 ? 0 : r;
    }
}
