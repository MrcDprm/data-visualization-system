package com.mrcdprm.dataviz.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Bir sütun: dosyadaki ham metin değerleri ve seçili türe göre çözülmüş hâli (sayı ya da tarih).
 * Tür otomatik tahmin edilir, kullanıcı değiştirebilir; o zaman değerler yeni türe göre yeniden çözülür.
 * Çözülemeyen hücreler boş (eksik) sayılır ve sayısı gösterilir.
 */
public final class Column {

    /** Tür tahmininde bakılan en fazla örnek sayısı ve sayılması gereken başarı oranı. */
    static final int SAMPLE_SIZE = 500;
    static final double MIN_RATIO = 0.95;

    private final String name;
    private final String[] raw;
    private final DecimalStyle decimal;
    private final ColumnType detected;
    private final String datePattern;
    private ColumnType type;
    private double[] numbers;
    private LocalDate[] dates;
    private int invalid;

    Column(String name, String[] raw, DecimalStyle decimal) {
        this.name = name;
        this.raw = raw;
        this.decimal = decimal;
        final List<String> samples = samples(raw);
        this.datePattern = Dates.detect(samples);
        this.detected = detect(samples, decimal, datePattern);
        setType(detected);
    }

    /** Boş olmayan değerlerden eşit aralıklarla en fazla 500 örnek. */
    private static List<String> samples(String[] raw) {
        final List<String> nonEmpty = new ArrayList<>();
        for (String s : raw)
            if (s != null && !s.isBlank())
                nonEmpty.add(s);
        if (nonEmpty.size() <= SAMPLE_SIZE)
            return nonEmpty;
        final List<String> out = new ArrayList<>(SAMPLE_SIZE);
        final double step = (double) nonEmpty.size() / SAMPLE_SIZE;
        for (int i = 0; i < SAMPLE_SIZE; i++)
            out.add(nonEmpty.get((int) (i * step)));
        return out;
    }

    /** Örneklerin en az %95'i sayıysa sayı, değilse en az %95'i tarihse tarih, değilse metin. */
    static ColumnType detect(List<String> samples, DecimalStyle decimal, String datePattern) {
        if (samples.isEmpty())
            return ColumnType.TEXT;
        int numbers = 0;
        int dates = 0;
        for (String s : samples) {
            if (!Double.isNaN(Numbers.parse(s, decimal)))
                numbers++;
            if (datePattern != null && Dates.parse(s, datePattern) != null)
                dates++;
        }
        if (numbers >= samples.size() * MIN_RATIO)
            return ColumnType.NUMBER;
        if (dates >= samples.size() * MIN_RATIO)
            return ColumnType.DATE;
        return ColumnType.TEXT;
    }

    /** Türü değiştirir ve bütün değerleri yeniden çözer. Tarih kalıbı bulunamadıysa tarih seçilemez. */
    public void setType(ColumnType newType) {
        if (newType == ColumnType.DATE && datePattern == null)
            throw new VizException("notDateColumn", name);
        type = newType;
        numbers = null;
        dates = null;
        invalid = 0;
        if (newType == ColumnType.NUMBER) {
            numbers = new double[raw.length];
            for (int i = 0; i < raw.length; i++) {
                numbers[i] = Numbers.parse(raw[i], decimal);
                if (Double.isNaN(numbers[i]) && !raw[i].isBlank())
                    invalid++;
            }
        } else if (newType == ColumnType.DATE) {
            dates = new LocalDate[raw.length];
            for (int i = 0; i < raw.length; i++) {
                dates[i] = Dates.parse(raw[i], datePattern);
                if (dates[i] == null && !raw[i].isBlank())
                    invalid++;
            }
        }
    }

    public String name() {
        return name;
    }

    public ColumnType type() {
        return type;
    }

    public ColumnType detectedType() {
        return detected;
    }

    /** Bu sütun tarih olarak çözülebilir mi (tür menüsünde "Tarih" seçeneği için). */
    public boolean canBeDate() {
        return datePattern != null;
    }

    public String datePattern() {
        return datePattern;
    }

    public int size() {
        return raw.length;
    }

    /** Seçili türe göre çözülemeyen (boş olmayan ama geçersiz) hücre sayısı. */
    public int invalidCount() {
        return invalid;
    }

    public String raw(int row) {
        return raw[row];
    }

    /** Sayı değeri; sütun sayı değilse ya da hücre boş/geçersizse NaN. */
    public double number(int row) {
        return numbers == null ? Double.NaN : numbers[row];
    }

    /** Tarih değeri; sütun tarih değilse ya da hücre boş/geçersizse null. */
    public LocalDate date(int row) {
        return dates == null ? null : dates[row];
    }

    /** Hücre seçili türe göre boş ya da geçersiz mi? */
    public boolean missing(int row) {
        return switch (type) {
            case NUMBER -> Double.isNaN(numbers[row]);
            case DATE -> dates[row] == null;
            case TEXT -> raw[row].isBlank();
        };
    }

    public ColumnStats stats() {
        return ColumnStats.of(this);
    }
}
