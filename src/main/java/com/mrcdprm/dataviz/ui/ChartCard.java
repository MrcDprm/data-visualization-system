package com.mrcdprm.dataviz.ui;

import atlantafx.base.theme.Styles;
import com.mrcdprm.dataviz.chart.ChartData;
import com.mrcdprm.dataviz.chart.ChartEngine;
import com.mrcdprm.dataviz.chart.ChartSpec;
import com.mrcdprm.dataviz.project.Workspace;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Panodaki grafik kartı: başlık, not satırı (kaç satır, kesilen veri, uygulanamayan filtre), grafik ve menü
 * (düzenle, çoğalt, genişlik, taşı, veriyi göster/CSV, PNG, sil). Hesap hatası kartın içinde gösterilir.
 */
public final class ChartCard extends VBox {

    /** Kartın panoya bildirdiği işlemler. */
    public interface Actions {
        void edit(ChartSpec spec);

        void duplicate(ChartSpec spec);

        void toggleWide(ChartSpec spec);

        void move(ChartSpec spec, int delta);

        void delete(ChartSpec spec);
    }

    private final AppContext ctx;
    private final ChartSpec spec;
    private final StackPane body = new StackPane();
    private final HBox tools = new HBox(4);
    private ChartData data;

    public ChartCard(AppContext ctx, Workspace w, ChartSpec spec, Actions actions) {
        super(6);
        this.ctx = ctx;
        this.spec = spec;
        getStyleClass().addAll("panel", "chart-card");
        setPadding(new Insets(12, 14, 12, 14));

        final Label title = new Label(ChartTexts.title(spec));
        title.getStyleClass().add("card-title");
        title.setWrapText(true);
        final Label notes = Ui.muted("");
        notes.getStyleClass().add("card-notes");

        final Button edit = Ui.iconButton(Feather.EDIT_2, I18n.t("card.edit"));
        edit.setOnAction(e -> actions.edit(spec));
        final MenuButton menu = new MenuButton(null, new FontIcon(Feather.MORE_VERTICAL));
        menu.getStyleClass().addAll(Styles.FLAT, Styles.BUTTON_ICON, "no-arrow");
        menu.setAccessibleText(I18n.t("card.menu"));
        menu.getItems().addAll(
                item("card.showData", Feather.LIST, this::showData),
                item("card.exportPng", Feather.IMAGE, this::exportPng),
                new SeparatorMenuItem(),
                item("card.duplicate", Feather.COPY, () -> actions.duplicate(spec)),
                item(spec.wide() ? "card.narrow" : "card.wide", spec.wide() ? Feather.MINIMIZE_2 : Feather.MAXIMIZE_2,
                        () -> actions.toggleWide(spec)),
                item("card.moveEarlier", Feather.ARROW_LEFT, () -> actions.move(spec, -1)),
                item("card.moveLater", Feather.ARROW_RIGHT, () -> actions.move(spec, 1)),
                new SeparatorMenuItem(),
                item("card.delete", Feather.TRASH_2, () -> actions.delete(spec)));

        final VBox titles = new VBox(1, title, notes);
        HBox.setHgrow(titles, Priority.ALWAYS);
        tools.getChildren().addAll(edit, menu);
        final HBox header = new HBox(4, titles, tools);
        header.setAlignment(Pos.TOP_LEFT);

        body.setMinHeight(280);
        body.setPrefHeight(330);
        VBox.setVgrow(body, Priority.ALWAYS);
        getChildren().addAll(header, body);

        try {
            data = ChartEngine.compute(w.data(), spec, w.filters(), I18n.locale());
            body.getChildren().setAll(ChartView.create(data, spec, ChartTexts.xAxis(spec), ChartTexts.yAxis(spec)));
            notes.setText(notes(data, w.filters().size() + spec.filters().size()));
        } catch (RuntimeException e) {
            final Label message = new Label(Ui.message(e));
            message.setWrapText(true);
            message.getStyleClass().add(Styles.DANGER);
            final Button fix = Ui.button(I18n.t("card.edit"), Feather.EDIT_2, Styles.SMALL);
            fix.setOnAction(ev -> actions.edit(spec));
            final VBox box = new VBox(8, new FontIcon(Feather.ALERT_TRIANGLE), message, fix);
            box.setAlignment(Pos.CENTER);
            body.getChildren().setAll(box);
            notes.setText("");
        }
    }

    private static MenuItem item(String key, Feather icon, Runnable action) {
        final MenuItem i = new MenuItem(I18n.t(key), new FontIcon(icon));
        i.setOnAction(e -> action.run());
        return i;
    }

    /** Not satırı: "1.234 satır · 3 filtre · ilk 500 kategori gösteriliyor · 1 filtre uygulanamadı". */
    static String notes(ChartData d, int filterCount) {
        final List<String> parts = new ArrayList<>();
        parts.add(I18n.t("card.rows", I18n.count(d.rowsUsed())));
        if (filterCount > 0)
            parts.add(I18n.t("card.filters", filterCount - d.ignoredFilters()));
        if (d.truncated())
            parts.add(I18n.t(d.scatter() ? "card.sampled" : "card.truncated"));
        if (d.seriesDropped() > 0)
            parts.add(I18n.t("card.seriesDropped", d.seriesDropped()));
        if (d.ignoredFilters() > 0)
            parts.add(I18n.t("card.ignoredFilters", d.ignoredFilters()));
        return String.join("  ·  ", parts);
    }

    // ---------------------------------------------------------------- veri tablosu ve dışa aktarma

    private void showData() {
        if (data == null || data.empty()) {
            Ui.info(ctx.window(), I18n.t("chart.noData"));
            return;
        }
        final List<List<String>> rows = Exporter.table(data, ChartTexts.xAxis(spec));
        final TableView<List<String>> table = new TableView<>();
        table.getStyleClass().addAll(Styles.STRIPED, Styles.DENSE);
        final List<String> header = rows.get(0);
        for (int c = 0; c < header.size(); c++) {
            final int col = c;
            final TableColumn<List<String>, String> column = new TableColumn<>(header.get(c));
            column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(display(cell.getValue().get(col), col)));
            column.setPrefWidth(c == 0 ? 180 : 120);
            if (c > 0)
                column.setStyle("-fx-alignment: CENTER-RIGHT;");
            table.getColumns().add(column);
        }
        table.getItems().setAll(rows.subList(1, rows.size()));
        table.setPrefSize(640, 440);

        final Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(ctx.window());
        dialog.setTitle(ChartTexts.title(spec));
        dialog.setResizable(true);
        Theme.style(dialog.getDialogPane());
        final Button save = Ui.button(I18n.t("card.saveCsv"), Feather.DOWNLOAD);
        save.setOnAction(e -> {
            final File file = Exporter.choose(ctx, I18n.t("card.saveCsv"), Exporter.fileName(ChartTexts.title(spec), ".csv"),
                    "CSV", "*.csv");
            if (file == null)
                return;
            Ui.background(save, () -> {
                Exporter.csv(rows, file.toPath());
                return file;
            }, f -> ctx.openUrl(f.toURI().toString()), ex -> Ui.error(ctx.window(), ex));
        });
        final VBox content = new VBox(10, Ui.muted(I18n.t("card.tableNote", I18n.count(rows.size() - 1))), table, save);
        VBox.setVgrow(table, Priority.ALWAYS);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType(I18n.t("action.close"), ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.showAndWait();
    }

    /** Tabloda sayılar binlik ayırıcıyla gösterilir; CSV'ye ise ayırıcısız yazılır. */
    private static String display(String value, int column) {
        if (column == 0 || value.isEmpty())
            return value;
        try {
            return I18n.number(new java.text.DecimalFormat("0.######",
                    java.text.DecimalFormatSymbols.getInstance(I18n.locale())).parse(value).doubleValue());
        } catch (java.text.ParseException e) {
            return value;
        }
    }

    private void exportPng() {
        if (data == null || data.empty()) {
            Ui.info(ctx.window(), I18n.t("chart.noData"));
            return;
        }
        final File file = Exporter.choose(ctx, I18n.t("card.exportPng"), Exporter.fileName(ChartTexts.title(spec), ".png"),
                "PNG", "*.png");
        if (file == null)
            return;
        // resimde düzenle/menü düğmeleri görünmesin
        tools.setVisible(false);
        try {
            Exporter.png(this, file.toPath());
            ctx.openUrl(file.toURI().toString());
        } catch (java.io.IOException | RuntimeException e) {
            Ui.error(ctx.window(), e);
        } finally {
            tools.setVisible(true);
        }
    }
}
