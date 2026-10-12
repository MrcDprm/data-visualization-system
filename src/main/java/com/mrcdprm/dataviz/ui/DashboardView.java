package com.mrcdprm.dataviz.ui;

import atlantafx.base.theme.Styles;
import com.mrcdprm.dataviz.chart.ChartSpec;
import com.mrcdprm.dataviz.chart.ChartSuggestions;
import com.mrcdprm.dataviz.project.ProjectFile;
import com.mrcdprm.dataviz.project.Workspace;
import java.util.List;
import java.util.UUID;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Pano: üstte bütün grafiklere uygulanan filtreler ve "Grafik ekle", altta iki sütunlu kart ızgarası
 * (geniş kartlar iki sütunu kaplar). Bir şey değişince bütün kartlar yeniden hesaplanır.
 */
public final class DashboardView extends BorderPane implements ChartCard.Actions {

    private final AppContext ctx;
    private final Workspace w;
    private final GridPane grid = new GridPane();
    private final ScrollPane scroll = new ScrollPane();

    public DashboardView(AppContext ctx) {
        this.ctx = ctx;
        this.w = ctx.workspace();

        final Label filtersLabel = new Label(I18n.t("dashboard.filters"));
        filtersLabel.getStyleClass().add(Styles.TEXT_BOLD);
        filtersLabel.setGraphic(new FontIcon(Feather.SLIDERS));
        filtersLabel.setMinWidth(Label.USE_PREF_SIZE);
        final FilterBar bar = new FilterBar(ctx, w.data(), w.filters(), "filter.addDashboard", () -> {
            ctx.changed();
            refresh();
        });
        HBox.setHgrow(bar, Priority.ALWAYS);
        final Button add = Ui.button(I18n.t("dashboard.addChart"), Feather.PLUS, Styles.ACCENT);
        add.setOnAction(e -> addChart());
        add.setMinWidth(Button.USE_PREF_SIZE);
        final HBox top = new HBox(12, filtersLabel, bar, add);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("filter-row");
        top.setPadding(new Insets(10, 20, 10, 20));
        setTop(top);

        final ColumnConstraints half = new ColumnConstraints();
        half.setPercentWidth(50);
        grid.getColumnConstraints().addAll(half, half);
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setPadding(new Insets(16, 20, 24, 20));
        scroll.setContent(grid);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("transparent-scroll");
        setCenter(scroll);
        refresh();
    }

    void refresh() {
        grid.getChildren().clear();
        if (w.charts().isEmpty()) {
            setCenter(emptyState());
            return;
        }
        setCenter(scroll);
        int col = 0;
        int row = 0;
        for (ChartSpec spec : w.charts()) {
            final ChartCard card = new ChartCard(ctx, w, spec, this);
            if (spec.wide() && col == 1) {
                col = 0;
                row++;
            }
            grid.add(card, col, row, spec.wide() ? 2 : 1, 1);
            if (spec.wide() || col == 1) {
                col = 0;
                row++;
            } else {
                col = 1;
            }
        }
    }

    private VBox emptyState() {
        final FontIcon icon = new FontIcon(Feather.BAR_CHART_2);
        icon.getStyleClass().add("empty-icon");
        final Label title = new Label(I18n.t("dashboard.emptyTitle"));
        title.getStyleClass().add(Styles.TITLE_3);
        final Button suggest = Ui.button(I18n.t("dashboard.suggest"), Feather.ZAP, Styles.ACCENT);
        suggest.setOnAction(e -> addSuggestions());
        final Button add = Ui.button(I18n.t("dashboard.addChart"), Feather.PLUS);
        add.setOnAction(e -> addChart());
        final VBox box = new VBox(12, icon, title, Ui.muted(I18n.t("dashboard.emptyText")), new HBox(10, suggest, add));
        ((HBox) box.getChildren().get(3)).setAlignment(Pos.CENTER);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(460);
        final VBox outer = new VBox(box);
        outer.setAlignment(Pos.CENTER);
        return outer;
    }

    private void addSuggestions() {
        final List<ChartSpec> suggestions = ChartSuggestions.suggest(w.data());
        if (suggestions.isEmpty()) {
            Ui.info(ctx.window(), I18n.t("dashboard.noSuggestions"));
            return;
        }
        w.charts().addAll(suggestions);
        ctx.changed();
        refresh();
    }

    private void addChart() {
        if (w.charts().size() >= ProjectFile.MAX_CHARTS) {
            Ui.info(ctx.window(), I18n.t("dashboard.chartLimit", ProjectFile.MAX_CHARTS));
            return;
        }
        new ChartEditor(ctx, w.data(), w.filters(), null).showAndWait().ifPresent(spec -> {
            w.charts().add(spec);
            ctx.changed();
            refresh();
            scroll.layout();
            scroll.setVvalue(1);
        });
    }

    // ---------------------------------------------------------------- kart işlemleri

    @Override
    public void edit(ChartSpec spec) {
        new ChartEditor(ctx, w.data(), w.filters(), spec).showAndWait().ifPresent(edited -> {
            w.replace(edited);
            ctx.changed();
            refresh();
        });
    }

    @Override
    public void duplicate(ChartSpec spec) {
        if (w.charts().size() >= ProjectFile.MAX_CHARTS) {
            Ui.info(ctx.window(), I18n.t("dashboard.chartLimit", ProjectFile.MAX_CHARTS));
            return;
        }
        final int i = w.charts().indexOf(spec);
        w.charts().add(i + 1, spec.withId(UUID.randomUUID().toString()));
        ctx.changed();
        refresh();
    }

    @Override
    public void toggleWide(ChartSpec spec) {
        w.replace(spec.withWide(!spec.wide()));
        ctx.changed();
        refresh();
    }

    @Override
    public void move(ChartSpec spec, int delta) {
        final int i = w.charts().indexOf(spec);
        final int j = i + delta;
        if (i < 0 || j < 0 || j >= w.charts().size())
            return;
        w.charts().set(i, w.charts().get(j));
        w.charts().set(j, spec);
        ctx.changed();
        refresh();
    }

    @Override
    public void delete(ChartSpec spec) {
        if (!Ui.confirm(ctx.window(), I18n.t("card.deleteConfirm", ChartTexts.title(spec)), I18n.t("card.delete")))
            return;
        w.charts().remove(spec);
        ctx.changed();
        refresh();
    }
}
