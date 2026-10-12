package com.mrcdprm.dataviz.chart;

/** Grafik türleri. */
public enum ChartType {
    BAR,
    STACKED_BAR,
    LINE,
    AREA,
    PIE,
    SCATTER,
    HISTOGRAM;

    /** Kategori (X) ekseni olan grafikler: çubuk, yığılmış çubuk, çizgi, alan ve pasta. */
    public boolean categorical() {
        return this != SCATTER && this != HISTOGRAM;
    }

    /** Birden fazla seri gösterebilen grafikler. */
    public boolean multiSeries() {
        return this == BAR || this == STACKED_BAR || this == LINE || this == AREA || this == SCATTER;
    }
}
