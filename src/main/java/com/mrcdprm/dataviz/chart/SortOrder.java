package com.mrcdprm.dataviz.chart;

/**
 * Kategorilerin sırası. AUTO: tarih ve sayı eksenlerinde artan sıra; metin ekseninde çubuk ve pasta grafiklerinde
 * büyükten küçüğe, çizgi ve alan grafiklerinde alfabetik. ORIGINAL: dosyada ilk görüldükleri sıra
 * (ör. "Oca, Şub, Mar" gibi ay adları alfabetik sıralanınca bozulur).
 */
public enum SortOrder {
    AUTO,
    LABEL,
    ORIGINAL,
    VALUE_DESC,
    VALUE_ASC
}
