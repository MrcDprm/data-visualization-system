package com.mrcdprm.dataviz.project;

import com.mrcdprm.dataviz.chart.ChartSpec;
import com.mrcdprm.dataviz.chart.Filter;
import com.mrcdprm.dataviz.data.ColumnType;
import com.mrcdprm.dataviz.data.DecimalStyle;
import java.util.List;
import java.util.Map;

/**
 * Proje dosyasının (.dvz, JSON) içeriği: veri setinin nereden ve hangi ayarlarla okunacağı, kullanıcının değiştirdiği
 * sütun türleri, pano filtreleri ve grafik kartları. Verinin kendisi sadece yapıştırılan tablolarda dosyaya gömülür.
 */
public record Project(String app, int version, DatasetRef dataset, Map<String, ColumnType> types, List<Filter> filters,
        List<ChartSpec> charts) {

    public static final String APP = "data-visualizer";
    public static final int VERSION = 1;

    /**
     * Veri setine başvuru: örnek veri seti kimliği, dosya yolu ya da gömülü metin (yapıştırılan tablo).
     * Ayırıcı JSON'da tek karakterlik metin olarak tutulur.
     */
    public record DatasetRef(String name, String sampleId, String file, String embedded, String charset,
            String delimiter, Boolean header, DecimalStyle decimal) {
    }
}
