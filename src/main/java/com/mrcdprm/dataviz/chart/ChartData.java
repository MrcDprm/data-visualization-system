package com.mrcdprm.dataviz.chart;

import java.util.List;

/**
 * Grafiğin çizilmeye hazır verisi. Kategorili grafiklerde (çubuk, çizgi, alan, pasta, histogram) kategori etiketleri
 * ve her seri için değerler; dağılım grafiğinde nokta serileri. Eksik değer NaN'dır (çizgide boşluk).
 *
 * @param rowsUsed       filtrelerden geçip grafiğe giren satır sayısı
 * @param truncated      kategori ya da nokta sınırı nedeniyle verinin bir kısmı gösterilmiyor
 * @param seriesDropped  seri sınırı nedeniyle gösterilmeyen seri sayısı
 * @param ignoredFilters sütunu bulunamadığı ya da türü uymadığı için uygulanmayan filtre sayısı
 */
public record ChartData(List<String> categories, List<Series> series, List<Points> points, String xName,
        String yName, int rowsUsed, boolean truncated, int seriesDropped, int ignoredFilters) {

    public record Series(String name, double[] values) {
    }

    public record Points(String name, double[] x, double[] y) {
    }

    public boolean scatter() {
        return points != null;
    }

    public boolean empty() {
        if (scatter())
            return points.stream().allMatch(p -> p.x().length == 0);
        return categories.isEmpty() || series.isEmpty();
    }
}
