package com.mrcdprm.dataviz.ui;

import atlantafx.base.theme.Styles;
import com.mrcdprm.dataviz.chart.Filter;
import com.mrcdprm.dataviz.data.DataSet;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Filtre çubuğu: her filtre bir etiket (tıklayınca düzenlenir, x ile silinir) ve "Filtre ekle" düğmesi.
 * Pano geneli filtrelerde ve grafik düzenleyicideki kart filtrelerinde aynı bileşen kullanılır.
 */
public final class FilterBar extends FlowPane {

    private final AppContext ctx;
    private final DataSet ds;
    private final List<Filter> filters;
    private final Runnable onChange;
    private final String addKey;

    public FilterBar(AppContext ctx, DataSet ds, List<Filter> filters, String addKey, Runnable onChange) {
        super(6, 6);
        this.ctx = ctx;
        this.ds = ds;
        this.filters = filters;
        this.onChange = onChange;
        this.addKey = addKey;
        setAlignment(Pos.CENTER_LEFT);
        refresh();
    }

    private void refresh() {
        getChildren().clear();
        for (int i = 0; i < filters.size(); i++)
            getChildren().add(chip(i));
        final Button add = Ui.button(I18n.t(addKey), Feather.FILTER, Styles.FLAT, Styles.SMALL);
        add.setOnAction(e -> new FilterDialog(ctx, ds, null).showAndWait().ifPresent(f -> {
            if (filters.size() >= 20) {
                Ui.info(ctx.window(), I18n.t("filter.limit", 20));
                return;
            }
            filters.add(f);
            changed();
        }));
        getChildren().add(add);
    }

    private HBox chip(int index) {
        final Filter f = filters.get(index);
        final Label text = new Label(FilterDialog.describe(f));
        text.setMaxWidth(320);
        final boolean broken = FilterDialog.broken(f, ds);
        if (broken) {
            text.getStyleClass().add("filter-broken");
            text.setTooltip(new Tooltip(I18n.t("filter.broken")));
        }
        final Button remove = new Button(null, new FontIcon(Feather.X));
        remove.getStyleClass().addAll(Styles.FLAT, Styles.BUTTON_ICON, Styles.SMALL, "chip-remove");
        remove.setAccessibleText(I18n.t("filter.remove"));
        remove.setTooltip(new Tooltip(I18n.t("filter.remove")));
        remove.setOnAction(e -> {
            filters.remove(index);
            changed();
        });
        final HBox chip = new HBox(2, new FontIcon(Feather.FILTER), text, remove);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("filter-chip");
        text.setOnMouseClicked(e -> new FilterDialog(ctx, ds, f).showAndWait().ifPresent(edited -> {
            filters.set(index, edited);
            changed();
        }));
        text.setCursor(javafx.scene.Cursor.HAND);
        return chip;
    }

    private void changed() {
        refresh();
        onChange.run();
    }
}
