package com.mrcdprm.dataviz.ui;

import atlantafx.base.theme.Styles;
import com.mrcdprm.dataviz.chart.Filter;
import com.mrcdprm.dataviz.data.Column;
import com.mrcdprm.dataviz.data.ColumnStats;
import com.mrcdprm.dataviz.data.DataSet;
import com.mrcdprm.dataviz.data.DecimalStyle;
import com.mrcdprm.dataviz.data.Numbers;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Filtre ekleme/düzenleme: sütun seçilir; metin sütununda değerler işaretlenir (arama kutusuyla), sayı sütununda
 * en küçük/en büyük, tarih sütununda başlangıç/bitiş girilir. Boş bırakılan uç sınırsızdır.
 */
public final class FilterDialog extends Dialog<Filter> {

    /** Listede gösterilen en fazla farklı değer (en sık geçenler). */
    private static final int MAX_LISTED = 1_000;

    private final DataSet ds;
    private final ComboBox<Column> column = new ComboBox<>();
    private final VBox editor = new VBox(10);
    private final Label error = Ui.errorLabel();
    private final Set<String> checked = new LinkedHashSet<>();
    private final TextField min = new TextField();
    private final TextField max = new TextField();
    private final DatePicker from = new DatePicker();
    private final DatePicker to = new DatePicker();
    private Filter result;

    public FilterDialog(AppContext ctx, DataSet ds, Filter existing) {
        this.ds = ds;
        initOwner(ctx.window());
        setTitle(I18n.t(existing == null ? "filter.add" : "filter.edit"));
        Theme.style(getDialogPane());

        column.getItems().setAll(ds.columns());
        column.setCellFactory(l -> columnCell());
        column.setButtonCell(columnCell());
        column.setMaxWidth(Double.MAX_VALUE);
        column.valueProperty().addListener((obs, o, n) -> {
            checked.clear();
            min.clear();
            max.clear();
            from.setValue(null);
            to.setValue(null);
            buildEditor();
        });
        if (existing != null) {
            ds.column(existing.column()).ifPresent(column::setValue);
            if (existing.values() != null)
                checked.addAll(existing.values());
            if (existing.min() != null)
                min.setText(I18n.value(existing.min()).replace(grouping(), ""));
            if (existing.max() != null)
                max.setText(I18n.value(existing.max()).replace(grouping(), ""));
            from.setValue(existing.fromDate());
            to.setValue(existing.toDate());
            buildEditor();
        } else if (!ds.columns().isEmpty()) {
            column.setValue(ds.columns().get(0));
        }

        final VBox content = new VBox(12, Ui.field(I18n.t("filter.column"), column), editor, error);
        content.setPadding(new Insets(6, 4, 0, 4));
        content.setPrefWidth(420);
        getDialogPane().setContent(content);
        final ButtonType ok = new ButtonType(I18n.t("filter.apply"), ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ok, new ButtonType(I18n.t("action.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));
        getDialogPane().lookupButton(ok).addEventFilter(ActionEvent.ACTION, e -> {
            result = build();
            if (result == null)
                e.consume();
        });
        setResultConverter(b -> b == ok ? result : null);
    }

    private static String grouping() {
        return I18n.isTurkish() ? "." : ",";
    }

    private static ListCell<Column> columnCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Column c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.name() + "  ·  " + I18n.of(c.type()));
            }
        };
    }

    private void buildEditor() {
        editor.getChildren().clear();
        Ui.showError(error, null);
        final Column c = column.getValue();
        if (c == null)
            return;
        switch (c.type()) {
            case TEXT -> valuesEditor(c);
            case NUMBER -> {
                final ColumnStats s = c.stats();
                min.setPromptText(I18n.value(s.min()));
                max.setPromptText(I18n.value(s.max()));
                editor.getChildren().addAll(new HBox(10, Ui.field(I18n.t("filter.min"), min), Ui.field(I18n.t("filter.max"), max)),
                        Ui.muted(I18n.t("filter.rangeHint", I18n.value(s.min()), I18n.value(s.max()))));
            }
            case DATE -> {
                final ColumnStats s = c.stats();
                from.setPromptText(I18n.date(s.first()));
                to.setPromptText(I18n.date(s.last()));
                editor.getChildren().addAll(new HBox(10, Ui.field(I18n.t("filter.from"), from), Ui.field(I18n.t("filter.to"), to)),
                        Ui.muted(I18n.t("filter.rangeHint", I18n.date(s.first()), I18n.date(s.last()))));
            }
        }
    }

    /** Metin sütunu: en sık geçen değerler işaretlenebilir liste olarak; arama kutusu listeyi daraltır. */
    private void valuesEditor(Column c) {
        final Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < c.size(); i++)
            if (!c.missing(i))
                counts.merge(c.raw(i).strip(), 1, Integer::sum);
        // işaretli değerler başta (düzenlerken seçim hemen görünsün), sonra en sık geçenler
        final List<String> all = counts.keySet().stream()
                .sorted(Comparator.comparing((String v) -> !checked.contains(v))
                        .thenComparing(Comparator.comparing((String v) -> counts.get(v)).reversed())
                        .thenComparing(java.text.Collator.getInstance(I18n.locale())))
                .limit(MAX_LISTED).toList();
        final ListView<String> list = new ListView<>();
        list.setPrefHeight(260);
        list.setCellFactory(l -> new ListCell<>() {
            private final CheckBox box = new CheckBox();

            {
                box.setMnemonicParsing(false); // "_" kısayol işareti sayılmasın
            }

            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setGraphic(null);
                    return;
                }
                box.setText(v + "  (" + I18n.count(counts.get(v)) + ")");
                box.setSelected(checked.contains(v));
                box.setOnAction(e -> {
                    if (box.isSelected())
                        checked.add(v);
                    else
                        checked.remove(v);
                });
                setGraphic(box);
            }
        });
        final TextField search = new TextField();
        search.setPromptText(I18n.t("filter.search"));
        search.textProperty().addListener((obs, o, n) -> {
            final String q = n.strip().toLowerCase(I18n.locale());
            list.getItems().setAll(q.isEmpty() ? all : all.stream().filter(v -> v.toLowerCase(I18n.locale()).contains(q)).toList());
        });
        list.getItems().setAll(all);
        final Button selectAll = Ui.button(I18n.t("filter.selectShown"), Feather.CHECK_SQUARE, Styles.FLAT, Styles.SMALL);
        selectAll.setOnAction(e -> {
            checked.addAll(list.getItems());
            list.refresh();
        });
        final Button clear = Ui.button(I18n.t("filter.clear"), Feather.SQUARE, Styles.FLAT, Styles.SMALL);
        clear.setOnAction(e -> {
            checked.clear();
            list.refresh();
        });
        final HBox tools = new HBox(4, selectAll, clear);
        editor.getChildren().addAll(new HBox(6, new FontIcon(Feather.SEARCH), search), list, tools);
        HBox.setHgrow(search, javafx.scene.layout.Priority.ALWAYS);
        if (counts.size() > MAX_LISTED)
            editor.getChildren().add(Ui.muted(I18n.t("filter.tooMany", I18n.count(MAX_LISTED), I18n.count(counts.size()))));
    }

    private Filter build() {
        final Column c = column.getValue();
        if (c == null) {
            Ui.showError(error, I18n.t("filter.chooseColumn"));
            return null;
        }
        return switch (c.type()) {
            case TEXT -> {
                if (checked.isEmpty()) {
                    Ui.showError(error, I18n.t("filter.chooseValues"));
                    yield null;
                }
                if (checked.size() > Filter.MAX_VALUES) {
                    Ui.showError(error, I18n.t("filter.tooManySelected", Filter.MAX_VALUES));
                    yield null;
                }
                // arama kutusuyla gizlenmiş ama işaretli değerler de filtreye girer
                yield Filter.values(c.name(), new ArrayList<>(checked));
            }
            case NUMBER -> {
                final Double lo = parse(min.getText());
                final Double hi = parse(max.getText());
                if ((!min.getText().isBlank() && lo == null) || (!max.getText().isBlank() && hi == null)) {
                    Ui.showError(error, I18n.t("filter.invalidNumber"));
                    yield null;
                }
                if (lo == null && hi == null) {
                    Ui.showError(error, I18n.t("filter.emptyRange"));
                    yield null;
                }
                if (lo != null && hi != null && lo > hi) {
                    Ui.showError(error, I18n.t("filter.reversed"));
                    yield null;
                }
                yield Filter.range(c.name(), lo, hi);
            }
            case DATE -> {
                if (from.getValue() == null && to.getValue() == null) {
                    Ui.showError(error, I18n.t("filter.emptyRange"));
                    yield null;
                }
                if (from.getValue() != null && to.getValue() != null && from.getValue().isAfter(to.getValue())) {
                    Ui.showError(error, I18n.t("filter.reversed"));
                    yield null;
                }
                yield Filter.dates(c.name(), from.getValue(), to.getValue());
            }
        };
    }

    /** Kullanıcı seçili dilin biçiminde yazar: Türkçede "12,5", İngilizcede "12.5". */
    private static Double parse(String text) {
        if (text == null || text.isBlank())
            return null;
        final double v = Numbers.parse(text, I18n.isTurkish() ? DecimalStyle.COMMA : DecimalStyle.DOT);
        return Double.isNaN(v) ? null : v;
    }

    /** Filtrenin kısa açıklaması (filtre çubuğundaki etiket): "country: Türkiye, Almanya +3", "year: 2000 – 2020". */
    public static String describe(Filter f) {
        final String body = switch (f.kind()) {
            case VALUES -> {
                final List<String> v = f.values() == null ? List.of() : f.values();
                yield v.size() <= 2 ? String.join(", ", v) : String.join(", ", v.subList(0, 2)) + " +" + (v.size() - 2);
            }
            case RANGE -> (f.min() == null ? "…" : I18n.value(f.min())) + " – " + (f.max() == null ? "…" : I18n.value(f.max()));
            case DATES -> (f.fromDate() == null ? "…" : I18n.date(f.fromDate())) + " – " + (f.toDate() == null ? "…" : I18n.date(f.toDate()));
        };
        return f.column() + ": " + body;
    }

    /** Filtre sütunu veri setinde yoksa ya da türü uymuyorsa (etiket üstü çizili gösterilir). */
    public static boolean broken(Filter f, DataSet ds) {
        return ds.column(f.column()).map(c -> c.type() != f.columnType()).orElse(true);
    }
}
