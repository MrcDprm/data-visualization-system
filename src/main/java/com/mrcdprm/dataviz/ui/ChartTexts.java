package com.mrcdprm.dataviz.ui;

import com.mrcdprm.dataviz.chart.Aggregation;
import com.mrcdprm.dataviz.chart.ChartSpec;
import com.mrcdprm.dataviz.chart.ChartType;

/**
 * Grafik başlığı ve eksen adları. Kullanıcı yazmadıysa seçimlerden üretilir:
 * "co2 · year (toplam)", "Anomaly (°C) dağılımı", "Satır sayısı · city".
 */
public final class ChartTexts {

    private ChartTexts() {
    }

    public static String title(ChartSpec s) {
        if (s.title() != null && !s.title().isBlank())
            return s.title();
        final String x = s.x() == null ? "?" : s.x();
        return switch (s.type()) {
            case HISTOGRAM -> I18n.t("title.histogram", x);
            case SCATTER -> I18n.t("title.scatter", s.y().isEmpty() ? "?" : s.y().get(0), x);
            default -> s.y().isEmpty() ? I18n.t("title.count", x)
                    : I18n.t("title.measure", String.join(", ", s.y()), x, I18n.of(s.aggregation()).toLowerCase(I18n.locale()));
        };
    }

    public static String xAxis(ChartSpec s) {
        return s.xLabel() != null && !s.xLabel().isBlank() ? s.xLabel() : s.x();
    }

    public static String yAxis(ChartSpec s) {
        if (s.yLabel() != null && !s.yLabel().isBlank())
            return s.yLabel();
        if (s.type() == ChartType.HISTOGRAM || s.y().isEmpty() || s.aggregation() == Aggregation.COUNT)
            return I18n.t("chart.rowCount");
        if (s.type() == ChartType.SCATTER)
            return s.y().get(0);
        return s.y().size() == 1 ? I18n.of(s.aggregation()) + " · " + s.y().get(0) : I18n.of(s.aggregation());
    }
}
