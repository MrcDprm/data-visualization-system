package com.mrcdprm.dataviz.ui;

import com.mrcdprm.dataviz.chart.Aggregation;
import com.mrcdprm.dataviz.chart.ChartSpec;
import com.mrcdprm.dataviz.chart.ChartType;
import com.mrcdprm.dataviz.chart.DateBucket;
import com.mrcdprm.dataviz.chart.Filter;
import com.mrcdprm.dataviz.chart.Palette;
import com.mrcdprm.dataviz.chart.SortOrder;
import com.mrcdprm.dataviz.data.SampleDataset;
import com.mrcdprm.dataviz.project.Workspace;
import java.time.LocalDate;
import java.util.List;

/**
 * Örnek veri setleri açılınca hazır gelen panolar: her biri farklı grafik türlerini, tarih gruplamayı, kırılımı,
 * ilk N'i, pano ve kart filtrelerini gösterir. Başlıklar çeviri anahtarı olarak saklanır, seçili dilde gösterilir.
 */
public final class SampleDashboards {

    private SampleDashboards() {
    }

    public static Workspace open(SampleDataset sample) {
        final Workspace w = new Workspace(sample.load("@sample." + sample.id() + ".name"), null);
        switch (sample) {
            case OWID_CO2 -> co2(w);
            case NASA_GISTEMP -> temperature(w);
            case IBB_DAMS -> dams(w);
        }
        return w;
    }

    /** Başlık ve eksen adları metin değil "@anahtar": dil değişince ve proje başka dilde açılınca çevrilir. */
    private static ChartSpec chart(ChartType type, String titleKey, String x, List<String> y, Aggregation a) {
        return ChartSpec.of(type, key(titleKey), x, y, a);
    }

    private static String key(String k) {
        return "@" + k;
    }

    private static List<Filter> year(int year) {
        return List.of(Filter.range("year", (double) year, (double) year));
    }

    private static void co2(Workspace w) {
        w.filters().add(Filter.range("year", 1990.0, null));
        w.charts().add(chart(ChartType.LINE, "sample.co2.world", "year", List.of("co2"), Aggregation.SUM)
                .withLabels(null, key("sample.co2.mt")).withWide(true));
        w.charts().add(chart(ChartType.BAR, "sample.co2.top", "country", List.of("co2"), Aggregation.SUM)
                .withSort(SortOrder.VALUE_DESC, 10).withFilters(year(2024)).withLabels(null, key("sample.co2.mt")));
        w.charts().add(chart(ChartType.PIE, "sample.co2.share", "country", List.of("co2"), Aggregation.SUM)
                .withSort(SortOrder.AUTO, 7).withFilters(year(2024)).withPalette(Palette.COLORBLIND));
        w.charts().add(chart(ChartType.STACKED_BAR, "sample.co2.fuel", "year",
                List.of("coal_co2", "oil_co2", "gas_co2", "cement_co2"), Aggregation.SUM)
                .withLabels(null, key("sample.co2.mt")).withWide(true));
        w.charts().add(chart(ChartType.LINE, "sample.co2.perCapita", "year", List.of("co2_per_capita"), Aggregation.AVG)
                .withSeries("country")
                .withFilters(List.of(Filter.values("country", List.of("Turkey", "Germany", "United Kingdom", "France", "China"))))
                .withLabels(null, key("sample.co2.tPerPerson")));
        w.charts().add(chart(ChartType.HISTOGRAM, "sample.co2.histogram", "co2_per_capita", List.of(), Aggregation.COUNT)
                .withBins(20).withFilters(year(2024)).withPalette(Palette.COOL)
                .withLabels(key("sample.co2.tPerPerson"), key("sample.co2.countries")));
    }

    private static void temperature(Workspace w) {
        final String anomaly = "Anomaly (°C)";
        w.charts().add(chart(ChartType.LINE, "sample.temp.yearly", "Year", List.of(anomaly), Aggregation.AVG)
                .withPalette(Palette.WARM).withWide(true));
        w.charts().add(chart(ChartType.BAR, "sample.temp.months", "Month", List.of(anomaly), Aggregation.AVG)
                .withSort(SortOrder.ORIGINAL, 0).withFilters(List.of(Filter.range("Year", 1991.0, null))).withPalette(Palette.WARM));
        w.charts().add(chart(ChartType.HISTOGRAM, "sample.temp.histogram", anomaly, List.of(), Aggregation.COUNT)
                .withBins(30).withPalette(Palette.WARM));
        w.charts().add(chart(ChartType.SCATTER, "sample.temp.scatter", "Year", List.of(anomaly), Aggregation.SUM)
                .withPalette(Palette.WARM));
        w.charts().add(chart(ChartType.AREA, "sample.temp.recent", "Date", List.of(anomaly), Aggregation.AVG)
                .withBucket(DateBucket.MONTH).withFilters(List.of(Filter.dates("Date", LocalDate.of(2015, 1, 1), null)))
                .withPalette(Palette.WARM).withWide(true));
    }

    private static void dams(Workspace w) {
        w.filters().add(Filter.dates("Tarih", LocalDate.of(2015, 1, 1), null));
        w.charts().add(chart(ChartType.LINE, "sample.dams.monthly", "Tarih",
                List.of("Ömerli", "Darlık", "Terkos", "Elmalı", "Büyükçekmece"), Aggregation.AVG)
                .withBucket(DateBucket.MONTH).withLabels(null, key("sample.dams.percent")).withWide(true));
        w.charts().add(chart(ChartType.BAR, "sample.dams.yearly", "Tarih", List.of("Ömerli"), Aggregation.AVG)
                .withBucket(DateBucket.YEAR).withLabels(null, key("sample.dams.percent")).withPalette(Palette.COOL));
        w.charts().add(chart(ChartType.HISTOGRAM, "sample.dams.histogram", "Ömerli", List.of(), Aggregation.COUNT)
                .withBins(20).withPalette(Palette.COOL).withLabels(key("sample.dams.percent"), key("sample.dams.days")));
        w.charts().add(chart(ChartType.AREA, "sample.dams.terkos", "Tarih", List.of("Terkos"), Aggregation.AVG)
                .withBucket(DateBucket.DAY).withFilters(List.of(Filter.dates("Tarih", LocalDate.of(2022, 1, 1), null)))
                .withLabels(null, key("sample.dams.percent")).withWide(true));
    }
}
