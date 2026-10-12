package com.mrcdprm.dataviz.ui;

import atlantafx.base.theme.Styles;
import com.mrcdprm.dataviz.chart.Aggregation;
import com.mrcdprm.dataviz.chart.ChartData;
import com.mrcdprm.dataviz.chart.ChartEngine;
import com.mrcdprm.dataviz.chart.ChartSpec;
import com.mrcdprm.dataviz.chart.ChartType;
import com.mrcdprm.dataviz.chart.DateBucket;
import com.mrcdprm.dataviz.chart.Filter;
import com.mrcdprm.dataviz.chart.Palette;
import com.mrcdprm.dataviz.chart.SortOrder;
import com.mrcdprm.dataviz.data.Column;
import com.mrcdprm.dataviz.data.ColumnType;
import com.mrcdprm.dataviz.data.DataSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Grafik düzenleyici: solda ayarlar (tür, X, Y, toplama, kırılım, tarih gruplama, sıralama ve ilk N, aralık sayısı,
 * palet, başlık ve eksen adları, kart filtreleri), sağda canlı önizleme. Seçilen türe uymayan ayarlar gizlenir.
 */
public final class ChartEditor extends Dialog<ChartSpec> {

    private static final Map<ChartType, Ikon> ICONS = new LinkedHashMap<>();

    static {
        ICONS.put(ChartType.BAR, Feather.BAR_CHART_2);
        ICONS.put(ChartType.STACKED_BAR, Feather.BAR_CHART);
        ICONS.put(ChartType.LINE, Feather.TRENDING_UP);
        ICONS.put(ChartType.AREA, Feather.ACTIVITY);
        ICONS.put(ChartType.PIE, Feather.PIE_CHART);
        ICONS.put(ChartType.SCATTER, Feather.GRID);
        ICONS.put(ChartType.HISTOGRAM, Feather.SLIDERS);
    }

    private final DataSet ds;
    private final List<Filter> global;
    private final String id;
    private final boolean wide;
    private final ToggleGroup typeGroup = new ToggleGroup();
    private final ComboBox<String> x = new ComboBox<>();
    private final VBox yBox = new VBox(4);
    private final List<CheckBox> yChecks = new ArrayList<>();
    private final ComboBox<Aggregation> aggregation = new ComboBox<>();
    private final ComboBox<String> series = new ComboBox<>();
    private final ComboBox<DateBucket> bucket = new ComboBox<>();
    private final ComboBox<SortOrder> sort = new ComboBox<>();
    private final Spinner<Integer> topN = Ui.numberSpinner(0, ChartSpec.MAX_TOP_N, 0, 1);
    private final Spinner<Integer> bins = Ui.numberSpinner(0, ChartSpec.MAX_BINS, 0, 1);
    private final ComboBox<Palette> palette = new ComboBox<>();
    private final TextField title = new TextField();
    private final TextField xLabel = new TextField();
    private final TextField yLabel = new TextField();
    private final List<Filter> filters = new ArrayList<>();
    private final StackPane preview = new StackPane();
    private final Label info = Ui.muted("");
    private final PauseTransition debounce = new PauseTransition(Duration.millis(150));
    private final Map<String, Node> rows = new LinkedHashMap<>();
    private boolean loading;

    public ChartEditor(AppContext ctx, DataSet ds, List<Filter> global, ChartSpec existing) {
        this.ds = ds;
        this.global = global;
        this.id = existing == null ? UUID.randomUUID().toString() : existing.id();
        this.wide = existing != null && existing.wide();
        initOwner(ctx.window());
        setTitle(I18n.t(existing == null ? "editor.add" : "editor.edit"));
        setResizable(true);
        Theme.style(getDialogPane());

        // ---- tür seçimi
        final FlowPane types = new FlowPane(6, 6);
        for (Map.Entry<ChartType, Ikon> e : ICONS.entrySet()) {
            final ToggleButton b = new ToggleButton(I18n.of(e.getKey()), new FontIcon(e.getValue()));
            b.setUserData(e.getKey());
            b.setToggleGroup(typeGroup);
            b.getStyleClass().add("type-toggle");
            types.getChildren().add(b);
        }
        typeGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n == null)
                typeGroup.selectToggle(o);
            else
                typeChanged();
        });

        // ---- seçim kutuları
        for (ComboBox<?> c : List.of(x, aggregation, series, bucket, sort, palette))
            c.setMaxWidth(Double.MAX_VALUE);
        aggregation.getItems().setAll(Aggregation.values());
        aggregation.setButtonCell(enumCell());
        aggregation.setCellFactory(l -> enumCell());
        bucket.getItems().setAll(DateBucket.values());
        bucket.setButtonCell(enumCell());
        bucket.setCellFactory(l -> enumCell());
        sort.getItems().setAll(SortOrder.values());
        sort.setButtonCell(enumCell());
        sort.setCellFactory(l -> enumCell());
        palette.getItems().setAll(Palette.values());
        palette.setButtonCell(paletteCell());
        palette.setCellFactory(l -> paletteCell());
        series.setButtonCell(noneCell());
        series.setCellFactory(l -> noneCell());
        x.valueProperty().addListener((obs, o, n) -> {
            updateVisibility();
            refresh();
        });
        for (ComboBox<?> c : List.of(aggregation, series, bucket, sort, palette))
            c.valueProperty().addListener((obs, o, n) -> {
                updateVisibility();
                refresh();
            });
        for (Spinner<Integer> s : List.of(topN, bins))
            s.valueProperty().addListener((obs, o, n) -> refresh());
        for (TextField t : List.of(title, xLabel, yLabel))
            t.textProperty().addListener((obs, o, n) -> refresh());

        final ScrollPane yScroll = new ScrollPane(yBox);
        yScroll.setFitToWidth(true);
        yScroll.setPrefViewportHeight(130);
        yScroll.getStyleClass().add("y-list");
        final FilterBar filterBar = new FilterBar(ctx, ds, filters, "filter.addChart", this::refresh);

        final VBox form = new VBox(12,
                Ui.section(I18n.t("editor.type")), types,
                row("x", Ui.field(I18n.t("editor.x"), x)),
                row("y", Ui.field(I18n.t("editor.y"), yScroll), hintRow("yHint", "editor.yHint")),
                row("aggregation", Ui.field(I18n.t("editor.aggregation"), aggregation)),
                row("series", Ui.field(I18n.t("editor.series"), series), hintRow("seriesHint", "editor.seriesHint")),
                row("bucket", Ui.field(I18n.t("editor.bucket"), bucket)),
                row("sort", new HBox(10, grow(Ui.field(I18n.t("editor.sort"), sort)), Ui.field(I18n.t("editor.topN"), topN))),
                row("bins", Ui.field(I18n.t("editor.bins"), bins)),
                Ui.section(I18n.t("editor.appearance")),
                Ui.field(I18n.t("editor.title"), title),
                new HBox(10, grow(Ui.field(I18n.t("editor.xLabel"), xLabel)), grow(Ui.field(I18n.t("editor.yLabel"), yLabel))),
                Ui.field(I18n.t("editor.palette"), palette),
                Ui.section(I18n.t("editor.filters")), Ui.muted(I18n.t("editor.filtersHint")), filterBar);
        form.setPadding(new Insets(4, 14, 8, 2));
        form.setPrefWidth(370);
        final ScrollPane formScroll = new ScrollPane(form);
        formScroll.setFitToWidth(true);
        formScroll.setPrefViewportWidth(390);
        formScroll.getStyleClass().add("transparent-scroll");
        topN.setPrefWidth(90);
        bins.setPrefWidth(110);

        preview.getStyleClass().add("preview-box");
        preview.setPrefSize(600, 460);
        final VBox right = new VBox(8, Ui.section(I18n.t("editor.preview")), preview, info);
        VBox.setVgrow(preview, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        final HBox content = new HBox(18, formScroll, right);
        content.setPrefSize(1060, 640);
        final BorderPane root = new BorderPane(content);
        getDialogPane().setContent(root);
        getDialogPane().setPrefSize(1100, 720);

        final ButtonType ok = new ButtonType(I18n.t(existing == null ? "editor.addButton" : "editor.save"), ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ok, new ButtonType(I18n.t("action.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));
        setResultConverter(b -> b == ok ? spec() : null);
        debounce.setOnFinished(e -> renderPreview());

        load(existing != null ? existing : ChartSpec.of(ChartType.BAR, null, firstColumn(), firstNumber(), Aggregation.SUM));
    }

    private static VBox grow(VBox box) {
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private Node row(String key, Node... children) {
        final VBox box = new VBox(4, children);
        box.managedProperty().bind(box.visibleProperty());
        rows.put(key, box);
        return box;
    }

    private Node hintRow(String key, String textKey) {
        final Label l = Ui.muted(I18n.t(textKey));
        l.managedProperty().bind(l.visibleProperty());
        rows.put(key, l);
        return l;
    }

    private String firstColumn() {
        return ds.columns().stream().filter(c -> c.type() != ColumnType.NUMBER).map(Column::name).findFirst()
                .orElse(ds.columns().isEmpty() ? null : ds.columns().get(0).name());
    }

    private List<String> firstNumber() {
        return ds.columns(ColumnType.NUMBER).stream().limit(1).map(Column::name).toList();
    }

    // ---------------------------------------------------------------- doldurma ve okuma

    private void load(ChartSpec s) {
        loading = true;
        for (var t : typeGroup.getToggles())
            if (t.getUserData() == s.type())
                typeGroup.selectToggle(t);
        fillColumns(s.type());
        x.setValue(s.x());
        for (CheckBox c : yChecks)
            c.setSelected(s.y().contains(c.getText()));
        aggregation.setValue(s.aggregation());
        series.setValue(s.series() == null ? "" : s.series());
        bucket.setValue(s.bucket());
        sort.setValue(s.sort());
        topN.getValueFactory().setValue(s.topN());
        bins.getValueFactory().setValue(s.bins());
        palette.setValue(s.palette());
        title.setText(s.title() == null ? "" : s.title());
        xLabel.setText(s.xLabel() == null ? "" : s.xLabel());
        yLabel.setText(s.yLabel() == null ? "" : s.yLabel());
        filters.clear();
        filters.addAll(s.filters());
        loading = false;
        updateVisibility();
        renderPreview();
    }

    private ChartType type() {
        return typeGroup.getSelectedToggle() == null ? ChartType.BAR : (ChartType) typeGroup.getSelectedToggle().getUserData();
    }

    /** Formdaki seçimlerden grafik tanımı. Türe uymayan alanlar boşaltılır. */
    ChartSpec spec() {
        final ChartType t = type();
        List<String> y = yChecks.stream().filter(CheckBox::isSelected).map(CheckBox::getText).toList();
        if (t == ChartType.HISTOGRAM)
            y = List.of();
        if ((t == ChartType.PIE || t == ChartType.SCATTER) && y.size() > 1)
            y = y.subList(0, 1);
        final boolean seriesAllowed = t != ChartType.PIE && t != ChartType.HISTOGRAM && y.size() <= 1;
        final String seriesValue = seriesAllowed && series.getValue() != null && !series.getValue().isEmpty() ? series.getValue() : null;
        return new ChartSpec(id, t, blank(title.getText()), x.getValue(), y,
                t == ChartType.HISTOGRAM ? Aggregation.COUNT : aggregation.getValue(), seriesValue, bucket.getValue(),
                sort.getValue(), topN.getValue(), bins.getValue(), palette.getValue(), blank(xLabel.getText()),
                blank(yLabel.getText()), List.copyOf(filters), wide).normalized();
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    // ---------------------------------------------------------------- türe göre form

    private void typeChanged() {
        if (loading)
            return;
        final String keepX = x.getValue();
        final List<String> keepY = yChecks.stream().filter(CheckBox::isSelected).map(CheckBox::getText).toList();
        fillColumns(type());
        x.setValue(x.getItems().contains(keepX) ? keepX : x.getItems().isEmpty() ? null : x.getItems().get(0));
        for (CheckBox c : yChecks)
            c.setSelected(keepY.contains(c.getText()));
        // dağılımda Y şart: seçili yoksa ilk sayısal sütun (X'ten farklı)
        if (type() == ChartType.SCATTER && yChecks.stream().noneMatch(CheckBox::isSelected))
            yChecks.stream().filter(c -> !c.getText().equals(x.getValue())).findFirst().ifPresent(c -> c.setSelected(true));
        updateVisibility();
        refresh();
    }

    /** Türe göre seçilebilecek sütunlar: histogram ve dağılımda X sayısal olmalı. */
    private void fillColumns(ChartType t) {
        final boolean numericX = t == ChartType.HISTOGRAM || t == ChartType.SCATTER;
        x.getItems().setAll(ds.columns().stream().filter(c -> !numericX || c.type() == ColumnType.NUMBER).map(Column::name).toList());
        series.getItems().setAll("");
        series.getItems().addAll(ds.columns().stream().filter(c -> c.type() != ColumnType.NUMBER).map(Column::name).toList());
        if (yChecks.isEmpty()) {
            for (Column c : ds.columns(ColumnType.NUMBER)) {
                final CheckBox box = new CheckBox(c.name());
                box.setMnemonicParsing(false);
                box.selectedProperty().addListener((obs, o, n) -> {
                    // pasta ve dağılımda tek Y: yenisi seçilince diğerleri bırakılır
                    if (n && (type() == ChartType.PIE || type() == ChartType.SCATTER))
                        yChecks.stream().filter(other -> other != box).forEach(other -> other.setSelected(false));
                    updateVisibility();
                    refresh();
                });
                yChecks.add(box);
            }
            yBox.getChildren().setAll(yChecks);
            if (yChecks.isEmpty())
                yBox.getChildren().add(Ui.muted(I18n.t("editor.noNumbers")));
        }
    }

    private void updateVisibility() {
        final ChartType t = type();
        final boolean xIsDate = ds.column(x.getValue()).map(c -> c.type() == ColumnType.DATE).orElse(false);
        final long ySelected = yChecks.stream().filter(CheckBox::isSelected).count();
        rows.get("y").setVisible(t != ChartType.HISTOGRAM);
        rows.get("yHint").setVisible(t != ChartType.HISTOGRAM && t != ChartType.SCATTER);
        rows.get("aggregation").setVisible(t.categorical());
        rows.get("series").setVisible(t != ChartType.PIE && t != ChartType.HISTOGRAM);
        rows.get("seriesHint").setVisible(t != ChartType.PIE && t != ChartType.HISTOGRAM && ySelected > 1);
        series.setDisable(ySelected > 1);
        rows.get("bucket").setVisible(t.categorical() && xIsDate);
        rows.get("sort").setVisible(t.categorical());
        rows.get("bins").setVisible(t == ChartType.HISTOGRAM);
        title.setPromptText(ChartTexts.title(safeSpec()));
        xLabel.setPromptText(ChartTexts.xAxis(safeSpec()));
        yLabel.setPromptText(ChartTexts.yAxis(safeSpec()));
    }

    private ChartSpec safeSpec() {
        try {
            return spec();
        } catch (RuntimeException e) {
            return ChartSpec.of(type(), null, x.getValue(), List.of(), Aggregation.COUNT);
        }
    }

    // ---------------------------------------------------------------- önizleme

    private void refresh() {
        if (!loading)
            debounce.playFromStart();
    }

    private void renderPreview() {
        final ChartSpec s = spec();
        try {
            final ChartData d = ChartEngine.compute(ds, s, global, I18n.locale());
            preview.getChildren().setAll(ChartView.create(d, s, ChartTexts.xAxis(s), ChartTexts.yAxis(s)));
            info.setText(ChartCard.notes(d, global.size() + s.filters().size()));
        } catch (RuntimeException e) {
            final Label message = new Label(Ui.message(e));
            message.setWrapText(true);
            message.getStyleClass().add(Styles.DANGER);
            final VBox box = new VBox(8, new FontIcon(Feather.ALERT_TRIANGLE), message);
            box.setAlignment(Pos.CENTER);
            box.setMaxWidth(380);
            preview.getChildren().setAll(box);
            info.setText("");
        }
    }

    // ---------------------------------------------------------------- hücre görünümleri

    private static <E extends Enum<E>> ListCell<E> enumCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(E value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : I18n.of(value));
            }
        };
    }

    private static ListCell<String> noneCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value.isEmpty() ? I18n.t("editor.none") : value);
            }
        };
    }

    private static ListCell<Palette> paletteCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Palette p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                final HBox swatches = new HBox(2);
                for (int i = 0; i < 6; i++) {
                    final Region r = new Region();
                    r.setPrefSize(12, 12);
                    r.setStyle("-fx-background-color: " + p.color(i) + "; -fx-background-radius: 3;");
                    swatches.getChildren().add(r);
                }
                setGraphic(swatches);
                setText(I18n.of(p));
            }
        };
    }
}
