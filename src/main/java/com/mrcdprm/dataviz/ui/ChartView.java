package com.mrcdprm.dataviz.ui;

import com.mrcdprm.dataviz.chart.ChartData;
import com.mrcdprm.dataviz.chart.ChartSpec;
import com.mrcdprm.dataviz.chart.ChartType;
import com.mrcdprm.dataviz.chart.Palette;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Base64;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import javafx.util.StringConverter;

/**
 * Hesaplanmış grafik verisini JavaFX grafiğine çevirir. Renk paleti grafiğe özel bir stil dosyasıyla (data: URL)
 * verilir; her veri noktasında değer ipucu (tooltip) vardır; eksen sayıları kısaltılır (1,2 Mn).
 */
public final class ChartView {

    /**
     * Bu kadar kategoriden sonra çizgide noktalar ve dikey ızgara çizgileri gizlenir. Sığmayan eksen etiketlerini
     * JavaFX kendisi atlar (ör. her üç yıldan biri yazılır).
     */
    private static final int MANY = 40;

    private ChartView() {
    }

    public static Node create(ChartData d, ChartSpec spec, String xTitle, String yTitle) {
        // pastada sadece pozitif değerler dilim olur; hepsi sıfır ya da eksiyse boş bir daire yerine mesaj gösterilir
        final boolean emptyPie = spec.type() == ChartType.PIE && !d.empty()
                && java.util.Arrays.stream(d.series().get(0).values()).noneMatch(v -> v > 0);
        if (d.empty() || emptyPie) {
            final Label empty = Ui.muted(I18n.t("chart.noData"));
            final StackPane box = new StackPane(empty);
            box.setAlignment(Pos.CENTER);
            return box;
        }
        final Chart chart = switch (spec.type()) {
            case PIE -> pie(d);
            case SCATTER -> scatter(d, xTitle, yTitle);
            default -> categorical(d, spec, xTitle, yTitle);
        };
        chart.setAnimated(false);
        chart.getStylesheets().add(paletteCss(spec.palette()));
        chart.getStyleClass().add("viz-chart");
        if (spec.type() == ChartType.HISTOGRAM || (d.series() != null && d.series().size() == 1 && spec.type() != ChartType.PIE))
            chart.setLegendVisible(false);
        return chart;
    }

    // ---------------------------------------------------------------- grafik türleri

    private static Chart categorical(ChartData d, ChartSpec spec, String xTitle, String yTitle) {
        final CategoryAxis x = new CategoryAxis();
        final NumberAxis y = new NumberAxis();
        x.setLabel(xTitle);
        y.setLabel(yTitle);
        y.setTickLabelFormatter(compact(y));
        y.setForceZeroInRange(spec.type() != ChartType.LINE);
        // uzun metin kategorileri (ülke adları) yan yana sığmayınca hafif eğik yazılır
        if (d.categories().size() > 5 && d.categories().stream().anyMatch(c -> c.length() > 8))
            x.setTickLabelRotation(-30);
        final XYChart<String, Number> chart = switch (spec.type()) {
            case STACKED_BAR -> new StackedBarChart<>(x, y);
            case LINE -> new LineChart<>(x, y);
            case AREA -> new AreaChart<>(x, y);
            default -> new BarChart<>(x, y);
        };
        chart.setVerticalGridLinesVisible(d.categories().size() <= MANY);
        if (chart instanceof BarChart<String, Number> bar && spec.type() == ChartType.HISTOGRAM) {
            bar.setCategoryGap(1);
            bar.setBarGap(0);
        }
        if (chart instanceof LineChart<String, Number> line)
            line.setCreateSymbols(d.categories().size() <= MANY);
        if (chart instanceof AreaChart<String, Number> area)
            area.setCreateSymbols(d.categories().size() <= MANY);
        // kategori sırası eksene açıkça verilir; yoksa ilk seride eksik olan kategoriler sona kayar
        x.setCategories(javafx.collections.FXCollections.observableArrayList(d.categories()));
        final boolean stacked = spec.type() == ChartType.STACKED_BAR;
        for (ChartData.Series s : d.series()) {
            final XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(s.name());
            for (int i = 0; i < d.categories().size(); i++) {
                final double v = s.values()[i];
                if (Double.isNaN(v) && !stacked)
                    continue;
                series.getData().add(new XYChart.Data<>(d.categories().get(i), Double.isNaN(v) ? 0 : v));
            }
            chart.getData().add(series);
            for (XYChart.Data<String, Number> point : series.getData())
                tooltip(point.getNode(), point.getXValue() + "\n" + s.name() + ": " + I18n.number(point.getYValue().doubleValue()));
        }
        return chart;
    }

    private static Chart pie(ChartData d) {
        final PieChart chart = new PieChart();
        final ChartData.Series s = d.series().get(0);
        double total = 0;
        for (double v : s.values())
            if (!Double.isNaN(v))
                total += v;
        for (int i = 0; i < d.categories().size(); i++) {
            final double v = s.values()[i];
            if (!Double.isNaN(v) && v > 0)
                chart.getData().add(new PieChart.Data(d.categories().get(i), v));
        }
        chart.setLabelsVisible(chart.getData().size() <= 8);
        chart.setLegendSide(Side.RIGHT);
        chart.setStartAngle(90);
        for (PieChart.Data slice : chart.getData()) {
            final double share = total == 0 ? 0 : slice.getPieValue() / total * 100;
            tooltip(slice.getNode(), slice.getName() + "\n" + I18n.number(slice.getPieValue()) + " (" + I18n.t("chart.share", I18n.number(share)) + ")");
        }
        return chart;
    }

    private static Chart scatter(ChartData d, String xTitle, String yTitle) {
        final NumberAxis x = new NumberAxis();
        final NumberAxis y = new NumberAxis();
        x.setLabel(xTitle);
        y.setLabel(yTitle);
        x.setForceZeroInRange(false);
        y.setForceZeroInRange(false);
        x.setTickLabelFormatter(compact(x));
        y.setTickLabelFormatter(compact(y));
        final ScatterChart<Number, Number> chart = new ScatterChart<>(x, y);
        for (ChartData.Points p : d.points()) {
            final XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(p.name());
            for (int i = 0; i < p.x().length; i++)
                series.getData().add(new XYChart.Data<>(p.x()[i], p.y()[i]));
            chart.getData().add(series);
            // binlerce noktada her birine ipucu eklemek yavaşlatır; sadece makul sayıda noktada
            if (p.x().length <= 2_000)
                for (XYChart.Data<Number, Number> point : series.getData())
                    tooltip(point.getNode(), p.name() + "\n" + I18n.number(point.getXValue().doubleValue()) + " ; "
                            + I18n.number(point.getYValue().doubleValue()));
        }
        chart.setLegendVisible(d.points().size() > 1);
        return chart;
    }

    private static void tooltip(Node node, String text) {
        if (node == null)
            return;
        final Tooltip t = new Tooltip(text);
        t.setShowDelay(Duration.millis(80));
        Tooltip.install(node, t);
    }

    // ---------------------------------------------------------------- biçim ve renk

    /**
     * Eksen sayıları: 1.250.000 -> "1,25 Mn" (TR) / "1.25M" (EN); 25.000 -> "25 bin" / "25K". Birim eksenin en büyük
     * değerine göre seçilir; böylece aynı eksende "12,5 bin" ile "7.500" karışık görünmez.
     */
    static StringConverter<Number> compact(NumberAxis axis) {
        return new StringConverter<>() {
            @Override
            public String toString(Number n) {
                final double top = Math.max(Math.abs(axis.getUpperBound()), Math.abs(axis.getLowerBound()));
                return compact(n.doubleValue(), top);
            }

            @Override
            public Number fromString(String s) {
                return 0;
            }
        };
    }

    public static String compact(double v) {
        return compact(v, Math.abs(v));
    }

    static String compact(double v, double scale) {
        if (v == 0)
            return "0";
        final double a = Math.abs(scale);
        final boolean tr = I18n.isTurkish();
        final DecimalFormat f = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(I18n.locale()));
        if (a >= 1e12)
            return f.format(v / 1e12) + (tr ? " Tn" : "T");
        if (a >= 1e9)
            return f.format(v / 1e9) + (tr ? " Mr" : "B");
        if (a >= 1e6)
            return f.format(v / 1e6) + (tr ? " Mn" : "M");
        if (a >= 1e4)
            return f.format(v / 1e3) + (tr ? " bin" : "K");
        return I18n.number(v);
    }

    /**
     * Paleti uygulayan stil dosyası. JavaFX serilere sırayla default-color0..7 sınıfını verir; bu sınıflara paletin
     * renkleri atanır (çubuk, çizgi, nokta, alan, pasta dilimi ve lejant simgeleri).
     */
    static String paletteCss(Palette palette) {
        final StringBuilder css = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            final String c = palette.color(i);
            final String dc = ".default-color" + i;
            css.append(dc).append(".chart-bar { -fx-bar-fill: ").append(c).append("; }\n");
            css.append(dc).append(".chart-series-line { -fx-stroke: ").append(c).append("; -fx-stroke-width: 2px; }\n");
            css.append(dc).append(".chart-line-symbol { -fx-background-color: ").append(c).append(", white; }\n");
            css.append(dc).append(".chart-series-area-line { -fx-stroke: ").append(c).append("; }\n");
            css.append(dc).append(".chart-series-area-fill { -fx-fill: ").append(c).append("40; }\n");
            css.append(dc).append(".chart-area-symbol { -fx-background-color: ").append(c).append(", white; }\n");
            css.append(dc).append(".chart-symbol { -fx-background-color: ").append(c).append("cc; }\n");
            css.append(dc).append(".chart-pie { -fx-pie-color: ").append(c).append("; }\n");
            css.append(dc).append(".pie-legend-symbol { -fx-background-color: ").append(c).append("; }\n");
        }
        return "data:text/css;base64," + Base64.getEncoder().encodeToString(css.toString().getBytes(StandardCharsets.UTF_8));
    }
}
