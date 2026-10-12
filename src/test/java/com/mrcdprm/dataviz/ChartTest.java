package com.mrcdprm.dataviz;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mrcdprm.dataviz.chart.Aggregation;
import com.mrcdprm.dataviz.chart.ChartData;
import com.mrcdprm.dataviz.chart.ChartEngine;
import com.mrcdprm.dataviz.chart.ChartSpec;
import com.mrcdprm.dataviz.chart.ChartSuggestions;
import com.mrcdprm.dataviz.chart.ChartType;
import com.mrcdprm.dataviz.chart.DateBucket;
import com.mrcdprm.dataviz.chart.Filter;
import com.mrcdprm.dataviz.chart.SortOrder;
import com.mrcdprm.dataviz.data.ColumnType;
import com.mrcdprm.dataviz.data.CsvReader;
import com.mrcdprm.dataviz.data.DataSet;
import com.mrcdprm.dataviz.data.ImportOptions;
import com.mrcdprm.dataviz.data.SampleDataset;
import com.mrcdprm.dataviz.data.VizException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** Gruplama, toplama, sıralama, ilk N, tarih gruplama, filtreler, histogram, dağılım ve öneriler. */
class ChartTest {

    private static final Locale TR = Locale.forLanguageTag("tr");

    private static final String SALES = """
            date,city,product,amount,qty
            2024-01-05,İstanbul,Kalem,100,1
            2024-01-20,Ankara,Kalem,50,2
            2024-02-03,İstanbul,Defter,200,1
            2024-02-10,İzmir,Kalem,30,3
            2024-03-15,Ankara,Defter,120,1
            2024-03-28,İstanbul,Kalem,,4
            2024-04-01,Bursa,Defter,10,1
            """;

    private static DataSet sales() {
        return DataSet.of(CsvReader.readText(SALES, ImportOptions.AUTO), "sales", DataSet.Source.PASTED);
    }

    private static ChartData run(DataSet ds, ChartSpec spec, Filter... global) {
        return ChartEngine.compute(ds, spec, List.of(global), TR);
    }

    @Test
    void sumByCategorySortedByValue() {
        final ChartData d = run(sales(), ChartSpec.of(ChartType.BAR, null, "city", List.of("amount"), Aggregation.SUM));
        assertEquals(List.of("İstanbul", "Ankara", "İzmir", "Bursa"), d.categories());
        assertArrayEquals(new double[] {300, 170, 30, 10}, d.series().get(0).values(), 1e-9);
        assertEquals(6, d.rowsUsed(), "boş tutarlı satır sayılmaz");
    }

    @Test
    void averageCountMinMax() {
        final DataSet ds = sales();
        assertArrayEquals(new double[] {150, 85, 30, 10},
                run(ds, ChartSpec.of(ChartType.BAR, null, "city", List.of("amount"), Aggregation.AVG)).series().get(0).values(), 1e-9);
        final ChartData count = run(ds, ChartSpec.of(ChartType.BAR, null, "city", List.of(), Aggregation.COUNT));
        assertEquals("İstanbul", count.categories().get(0));
        assertEquals(3, count.series().get(0).values()[0], 1e-9);
        assertEquals("Adet", count.series().get(0).name());
        assertArrayEquals(new double[] {100, 50, 30, 10},
                run(ds, ChartSpec.of(ChartType.BAR, null, "city", List.of("amount"), Aggregation.MIN)
                        .withSort(SortOrder.VALUE_DESC, 0)).series().get(0).values(), 1e-9);
    }

    @Test
    void dateBucketsAndLabels() {
        final ChartSpec spec = ChartSpec.of(ChartType.LINE, null, "date", List.of("amount"), Aggregation.SUM);
        final ChartData months = run(sales(), spec.withBucket(DateBucket.MONTH));
        assertEquals(List.of("Oca 2024", "Şub 2024", "Mar 2024", "Nis 2024"), months.categories());
        assertArrayEquals(new double[] {150, 230, 120, 10}, months.series().get(0).values(), 1e-9);
        final ChartData quarters = run(sales(), spec.withBucket(DateBucket.QUARTER));
        assertEquals(List.of("2024 Ç1", "2024 Ç2"), quarters.categories());
        final ChartData en = ChartEngine.compute(sales(), spec.withBucket(DateBucket.QUARTER), List.of(), Locale.ENGLISH);
        assertEquals(List.of("Q1 2024", "Q2 2024"), en.categories());
    }

    @Test
    void seriesFromColumnAndFromSeveralMeasures() {
        final ChartData bySeries = run(sales(), ChartSpec.of(ChartType.STACKED_BAR, null, "city", List.of("amount"),
                Aggregation.SUM).withSeries("product"));
        assertEquals(List.of("Defter", "Kalem"), bySeries.series().stream().map(ChartData.Series::name).toList());
        final int istanbul = bySeries.categories().indexOf("İstanbul");
        assertEquals(200, bySeries.series().get(0).values()[istanbul], 1e-9);
        assertEquals(100, bySeries.series().get(1).values()[istanbul], 1e-9);
        assertTrue(Double.isNaN(bySeries.series().get(0).values()[bySeries.categories().indexOf("İzmir")]));

        final ChartData measures = run(sales(), ChartSpec.of(ChartType.BAR, null, "city", List.of("amount", "qty"), Aggregation.SUM));
        assertEquals(List.of("amount", "qty"), measures.series().stream().map(ChartData.Series::name).toList());
        assertEquals(6, measures.series().get(1).values()[measures.categories().indexOf("İstanbul")], 1e-9);
    }

    @Test
    void topNAndPieOther() {
        final ChartData top = run(sales(), ChartSpec.of(ChartType.BAR, null, "city", List.of("amount"), Aggregation.SUM)
                .withSort(SortOrder.VALUE_DESC, 2));
        assertEquals(List.of("İstanbul", "Ankara"), top.categories());
        final ChartData pie = run(sales(), ChartSpec.of(ChartType.PIE, null, "city", List.of("amount"), Aggregation.SUM)
                .withSort(SortOrder.AUTO, 2));
        assertEquals(List.of("İstanbul", "Ankara", "Diğer"), pie.categories());
        assertArrayEquals(new double[] {300, 170, 40}, pie.series().get(0).values(), 1e-9);
        final ChartData asc = run(sales(), ChartSpec.of(ChartType.BAR, null, "city", List.of("amount"), Aggregation.SUM)
                .withSort(SortOrder.LABEL, 0));
        assertEquals(List.of("Ankara", "Bursa", "İstanbul", "İzmir"), asc.categories(), "Türkçe alfabetik sıra");
    }

    @Test
    void globalAndChartFiltersCombine() {
        final ChartSpec spec = ChartSpec.of(ChartType.BAR, null, "city", List.of("amount"), Aggregation.SUM)
                .withFilters(List.of(Filter.range("amount", 50.0, null)));
        final ChartData d = run(sales(), spec, Filter.dates("date", LocalDate.of(2024, 2, 1), null));
        assertEquals(List.of("İstanbul", "Ankara"), d.categories());
        assertArrayEquals(new double[] {200, 120}, d.series().get(0).values(), 1e-9);
        final ChartData values = run(sales(), ChartSpec.of(ChartType.BAR, null, "product", List.of(), Aggregation.COUNT),
                Filter.values("city", List.of("İstanbul")));
        assertEquals(3, values.series().get(0).values()[0] + values.series().get(0).values()[1], 1e-9);
        // sütunu olmayan filtre yok sayılır ve bildirilir
        final ChartData ignored = run(sales(), spec, Filter.values("yok", List.of("x")));
        assertEquals(1, ignored.ignoredFilters());
    }

    @Test
    void histogramBins() {
        final ChartData h = run(sales(), ChartSpec.of(ChartType.HISTOGRAM, null, "amount", List.of(), Aggregation.COUNT).withBins(4));
        // 10..200 aralığı 4'e bölününce 47,5 çıkar; güzel genişlik 50, başlangıç 0
        assertEquals(List.of("0 – 50", "50 – 100", "100 – 150", "150 – 200"), h.categories());
        assertEquals(6, java.util.Arrays.stream(h.series().get(0).values()).sum(), 1e-9);
        assertEquals(1, h.series().get(0).values()[3], 1e-9, "en büyük değer (200) son aralıkta");
        final ChartData auto = run(SampleDataset.NASA_GISTEMP.load("t"),
                ChartSpec.of(ChartType.HISTOGRAM, null, "Anomaly (°C)", List.of(), Aggregation.COUNT));
        assertTrue(auto.categories().size() >= 5 && auto.categories().size() <= 60);
        assertTrue(auto.categories().stream().allMatch(c -> c.matches("-?\\d+,\\d+ – -?\\d+,\\d+")), auto.categories().toString());
    }

    @Test
    void scatterSamplesLargeData() {
        final DataSet temp = SampleDataset.NASA_GISTEMP.load("t");
        final ChartData d = run(temp, ChartSpec.of(ChartType.SCATTER, null, "Year", List.of("Anomaly (°C)"), Aggregation.SUM));
        assertTrue(d.scatter());
        assertEquals(temp.rowCount(), d.rowsUsed());
        assertFalse(d.truncated());
        final DataSet co2 = SampleDataset.OWID_CO2.load("c");
        final ChartData big = run(co2, ChartSpec.of(ChartType.SCATTER, null, "population", List.of("co2"), Aggregation.SUM));
        assertTrue(big.truncated());
        assertTrue(big.points().get(0).x().length <= ChartEngine.MAX_POINTS);
    }

    @Test
    void invalidSpecsGiveReadableErrors() {
        final DataSet ds = sales();
        assertEquals("columnMissing", assertThrows(VizException.class,
                () -> run(ds, ChartSpec.of(ChartType.BAR, null, "nope", List.of("amount"), Aggregation.SUM))).code());
        assertEquals("needsNumber", assertThrows(VizException.class,
                () -> run(ds, ChartSpec.of(ChartType.BAR, null, "city", List.of("product"), Aggregation.SUM))).code());
        assertEquals("needsNumber", assertThrows(VizException.class,
                () -> run(ds, ChartSpec.of(ChartType.HISTOGRAM, null, "city", List.of(), Aggregation.COUNT))).code());
        assertEquals("needsX", assertThrows(VizException.class,
                () -> run(ds, ChartSpec.of(ChartType.BAR, null, null, List.of(), Aggregation.COUNT))).code());
        // ADET sayı olmayan sütunda da çalışır
        assertEquals(4, run(ds, ChartSpec.of(ChartType.BAR, null, "city", List.of("product"), Aggregation.COUNT)).categories().size());
    }

    @Test
    void suggestionsFitTheData() {
        final List<ChartSpec> s = ChartSuggestions.suggest(sales());
        assertEquals(ChartType.LINE, s.get(0).type());
        assertEquals("date", s.get(0).x());
        assertTrue(s.stream().anyMatch(c -> c.type() == ChartType.BAR && "city".equals(c.x())));
        assertTrue(s.stream().anyMatch(c -> c.type() == ChartType.HISTOGRAM));
        for (ChartSpec spec : s)
            run(sales(), spec); // hiçbiri hata vermemeli
        final DataSet co2 = SampleDataset.OWID_CO2.load("c");
        final List<ChartSpec> c = ChartSuggestions.suggest(co2);
        assertEquals("year", c.get(0).x(), "yıl sütunu ölçü değil eksen olarak seçilir");
        assertEquals(ColumnType.NUMBER, co2.column("year").orElseThrow().type());
    }
}
