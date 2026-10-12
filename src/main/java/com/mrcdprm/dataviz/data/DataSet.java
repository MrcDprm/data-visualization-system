package com.mrcdprm.dataviz.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Bellekteki veri seti: sütunlar, satır sayısı, içe aktarma ayarları, kaynağı ve uyarılar.
 * Kaynak ya uygulamayla gelen örnek veri seti (sampleId) ya da kullanıcının dosyası (file); yapıştırılan
 * tabloda ikisi de boştur ve veri proje dosyasının içine gömülür.
 */
public final class DataSet {

    /** Veri setinin nereden geldiği. */
    public record Source(String sampleId, String file) {

        public static Source sample(String id) {
            return new Source(id, null);
        }

        public static Source file(String path) {
            return new Source(null, path);
        }

        public static final Source PASTED = new Source(null, null);

        public boolean pasted() {
            return sampleId == null && file == null;
        }
    }

    private final String name;
    private final Source source;
    private final ImportOptions options;
    private final List<Column> columns;
    private final int rows;
    private final List<CsvReader.Warning> warnings;

    private DataSet(String name, Source source, ImportOptions options, List<Column> columns, int rows,
            List<CsvReader.Warning> warnings) {
        this.name = name;
        this.source = source;
        this.options = options;
        this.columns = columns;
        this.rows = rows;
        this.warnings = warnings;
    }

    /**
     * Okunan tablodan veri seti kurar. Ondalık ayırıcı ayarda yoksa bütün hücrelerden tahmin edilir
     * (bir dosyada bütün sayılar aynı biçimde yazılır); eşitlikte noktalı virgüllü dosyalar virgül ondalık sayılır.
     */
    public static DataSet of(CsvReader.Table table, String name, Source source) {
        final int width = table.headers().size();
        DecimalStyle decimal = table.options().decimal();
        if (decimal == null) {
            final List<String> samples = new ArrayList<>();
            final int step = Math.max(1, table.rows().size() / 400);
            for (int r = 0; r < table.rows().size(); r += step)
                Collections.addAll(samples, table.rows().get(r));
            final Character d = table.options().delimiter();
            decimal = Numbers.detect(samples, d != null && d == ';' ? DecimalStyle.COMMA : DecimalStyle.DOT);
        }
        final List<Column> columns = new ArrayList<>(width);
        for (int c = 0; c < width; c++) {
            final String[] values = new String[table.rows().size()];
            for (int r = 0; r < values.length; r++)
                values[r] = table.rows().get(r)[c];
            columns.add(new Column(table.headers().get(c), values, decimal));
        }
        return new DataSet(name, source, table.options().withDecimal(decimal), List.copyOf(columns), table.rows().size(),
                table.warnings());
    }

    /** Aynı veriyle yeni ad (içe aktarma penceresinde kullanıcı adı değiştirince veri yeniden okunmaz). */
    public DataSet renamed(String newName) {
        return new DataSet(newName, source, options, columns, rows, warnings);
    }

    public String name() {
        return name;
    }

    public Source source() {
        return source;
    }

    /** Çözümlenmiş içe aktarma ayarları (kodlama, ayırıcı, başlık, ondalık); proje dosyasına bu yazılır. */
    public ImportOptions options() {
        return options;
    }

    public List<Column> columns() {
        return columns;
    }

    public int rowCount() {
        return rows;
    }

    public List<CsvReader.Warning> warnings() {
        return warnings;
    }

    public Optional<Column> column(String columnName) {
        return columns.stream().filter(c -> c.name().equals(columnName)).findFirst();
    }

    public List<Column> columns(ColumnType type) {
        return columns.stream().filter(c -> c.type() == type).toList();
    }
}
