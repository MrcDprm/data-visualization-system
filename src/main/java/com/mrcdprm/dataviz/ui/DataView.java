package com.mrcdprm.dataviz.ui;

import atlantafx.base.theme.Styles;
import com.mrcdprm.dataviz.data.Column;
import com.mrcdprm.dataviz.data.ColumnStats;
import com.mrcdprm.dataviz.data.ColumnType;
import com.mrcdprm.dataviz.data.CsvReader;
import com.mrcdprm.dataviz.data.DataSet;
import java.util.Map;
import java.util.stream.IntStream;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Veri sekmesi: solda sütun listesi (tür ve geçersiz hücre sayısı), seçili sütunun türü (değiştirilebilir) ve özeti;
 * sağda verinin kendisi. Seçili türe göre çözülemeyen hücreler tabloda kırmızı görünür.
 */
public final class DataView extends BorderPane {

    private final AppContext ctx;
    private final DataSet ds;
    private final ListView<Column> columns = new ListView<>();
    private final VBox detail = new VBox(10);
    private final TableView<Integer> table = new TableView<>();

    public DataView(AppContext ctx) {
        this.ctx = ctx;
        this.ds = ctx.workspace().data();

        columns.getItems().setAll(ds.columns());
        columns.setCellFactory(l -> new ListCell<>() {
            @Override
            protected void updateItem(Column c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                final FontIcon icon = new FontIcon(icon(c.type()));
                icon.getStyleClass().add("type-icon");
                setGraphic(icon);
                setText(c.invalidCount() > 0 ? c.name() + "  ⚠ " + I18n.count(c.invalidCount()) : c.name());
            }
        });
        columns.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> showDetail(n));
        columns.setPrefWidth(250);
        final ScrollPane detailScroll = new ScrollPane(detail);
        detailScroll.setFitToWidth(true);
        detailScroll.getStyleClass().add("transparent-scroll");
        detail.setPadding(new Insets(4, 6, 8, 2));
        final VBox left = new VBox(10, Ui.section(I18n.t("data.columns")), columns, detailScroll);
        VBox.setVgrow(columns, Priority.ALWAYS);
        VBox.setVgrow(detailScroll, Priority.ALWAYS);
        left.setPrefWidth(290);
        left.setMinWidth(260);
        left.setPadding(new Insets(14, 12, 14, 20));
        setLeft(left);

        buildTable();
        final Label size = Ui.muted(I18n.t("data.size", I18n.count(ds.rowCount()), ds.columns().size()));
        final VBox right = new VBox(8, size, table);
        for (CsvReader.Warning w : ds.warnings()) {
            final Label warning = new Label(I18n.warning(w), new FontIcon(Feather.ALERT_TRIANGLE));
            warning.getStyleClass().add("warning-banner");
            warning.setWrapText(true);
            right.getChildren().add(1, warning);
        }
        VBox.setVgrow(table, Priority.ALWAYS);
        right.setPadding(new Insets(14, 20, 14, 8));
        setCenter(right);
        if (!ds.columns().isEmpty())
            columns.getSelectionModel().select(0);
    }

    static Ikon icon(ColumnType type) {
        return switch (type) {
            case NUMBER -> Feather.HASH;
            case DATE -> Feather.CALENDAR;
            case TEXT -> Feather.TYPE;
        };
    }

    private void buildTable() {
        table.getStyleClass().addAll(Styles.STRIPED, Styles.DENSE, "data-table");
        table.getItems().setAll(IntStream.range(0, ds.rowCount()).boxed().toList());
        final TableColumn<Integer, Integer> index = new TableColumn<>("#");
        index.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue() + 1));
        index.setPrefWidth(60);
        index.setSortable(false);
        index.getStyleClass().add("index-column");
        table.getColumns().add(index);
        for (Column c : ds.columns())
            table.getColumns().add(column(c));
    }

    private TableColumn<Integer, Integer> column(Column c) {
        final TableColumn<Integer, Integer> col = new TableColumn<>(c.name());
        col.setGraphic(new FontIcon(icon(c.type())));
        col.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        col.setSortable(false);
        col.setPrefWidth(Math.max(90, Math.min(220, c.name().length() * 9 + 40)));
        col.setCellFactory(t -> new TableCell<>() {
            @Override
            protected void updateItem(Integer row, boolean empty) {
                super.updateItem(row, empty);
                getStyleClass().remove("cell-invalid");
                if (empty || row == null) {
                    setText(null);
                    return;
                }
                final String raw = c.raw(row);
                if (c.missing(row)) {
                    setText(raw);
                    if (!raw.isBlank())
                        getStyleClass().add("cell-invalid");
                    return;
                }
                setText(switch (c.type()) {
                    case NUMBER -> I18n.number(c.number(row));
                    case DATE -> I18n.date(c.date(row));
                    case TEXT -> raw;
                });
                setAlignment(c.type() == ColumnType.TEXT ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
            }
        });
        return col;
    }

    private void showDetail(Column c) {
        detail.getChildren().clear();
        if (c == null)
            return;
        final Label name = new Label(c.name());
        name.getStyleClass().add(Styles.TITLE_4);
        name.setWrapText(true);

        final ComboBox<ColumnType> type = new ComboBox<>();
        type.getItems().setAll(ColumnType.values());
        if (!c.canBeDate())
            type.getItems().remove(ColumnType.DATE);
        type.setValue(c.type());
        type.setButtonCell(typeCell());
        type.setCellFactory(l -> typeCell());
        type.setMaxWidth(Double.MAX_VALUE);
        type.valueProperty().addListener((obs, o, n) -> {
            if (n == null || n == c.type())
                return;
            try {
                c.setType(n);
                ctx.changed();
                final int i = table.getColumns().indexOf(table.getColumns().stream()
                        .filter(tc -> tc.getText().equals(c.name())).findFirst().orElse(null));
                if (i > 0)
                    table.getColumns().set(i, column(c));
                columns.refresh();
                showDetail(c);
            } catch (RuntimeException e) {
                Ui.error(ctx.window(), e);
                type.setValue(o);
            }
        });
        final Label detected = Ui.muted(I18n.t("data.detected", I18n.of(c.detectedType())));
        detail.getChildren().addAll(name, Ui.field(I18n.t("data.type"), type), detected);
        if (c.invalidCount() > 0) {
            final Label invalid = new Label(I18n.t("data.invalid", I18n.count(c.invalidCount())), new FontIcon(Feather.ALERT_TRIANGLE));
            invalid.getStyleClass().add("warning-text");
            invalid.setWrapText(true);
            detail.getChildren().add(invalid);
        }

        final ColumnStats s = c.stats();
        final GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(4);
        int row = 0;
        row = stat(grid, row, "data.filled", I18n.count(s.filled()));
        row = stat(grid, row, "data.missing", I18n.count(s.missing()));
        row = stat(grid, row, "data.distinct", I18n.count(s.distinct()) + (s.distinctCapped() ? "+" : ""));
        if (c.type() == ColumnType.NUMBER && s.filled() > 0) {
            row = stat(grid, row, "data.min", I18n.number(s.min()));
            row = stat(grid, row, "data.max", I18n.number(s.max()));
            row = stat(grid, row, "data.mean", I18n.number(s.mean()));
            stat(grid, row, "data.sum", I18n.number(s.sum()));
        } else if (c.type() == ColumnType.DATE && s.first() != null) {
            row = stat(grid, row, "data.first", I18n.date(s.first()));
            stat(grid, row, "data.last", I18n.date(s.last()));
        }
        detail.getChildren().addAll(Ui.section(I18n.t("data.summary")), grid);
        if (c.type() == ColumnType.TEXT && !s.topValues().isEmpty()) {
            final VBox top = new VBox(3);
            for (Map.Entry<String, Integer> e : s.topValues()) {
                final Label l = new Label(e.getKey() + "  (" + I18n.count(e.getValue()) + ")");
                l.setWrapText(true);
                top.getChildren().add(l);
            }
            detail.getChildren().addAll(Ui.section(I18n.t("data.top")), top);
        }
    }

    private static int stat(GridPane grid, int row, String key, String value) {
        final Label v = new Label(value);
        v.getStyleClass().add(Styles.TEXT_BOLD);
        grid.addRow(row, Ui.muted(I18n.t(key)), v);
        return row + 1;
    }

    private static ListCell<ColumnType> typeCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(ColumnType t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? null : I18n.of(t));
                setGraphic(empty || t == null ? null : new FontIcon(icon(t)));
            }
        };
    }
}
