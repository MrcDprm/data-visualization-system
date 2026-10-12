package com.mrcdprm.dataviz.ui;

import atlantafx.base.theme.Styles;
import com.mrcdprm.dataviz.data.Column;
import com.mrcdprm.dataviz.data.CsvReader;
import com.mrcdprm.dataviz.data.DataSet;
import com.mrcdprm.dataviz.data.DecimalStyle;
import com.mrcdprm.dataviz.data.ImportOptions;
import java.util.List;
import java.util.stream.IntStream;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * İçe aktarma: otomatik bulunan karakter kodlaması, ayırıcı, başlık satırı ve ondalık ayırıcı gösterilir; kullanıcı
 * değiştirince önizleme yeniden okunur. Önizlemede ilk 100 satır ve her sütunun tahmin edilen türü görünür.
 */
public final class ImportDialog extends Dialog<DataSet> {

    private static final int PREVIEW_ROWS = 100;
    private static final List<String> CHARSETS = List.of("", "UTF-8", "windows-1254", "ISO-8859-9", "UTF-16");
    private static final List<String> DELIMITERS = List.of("", ",", ";", "\t", "|");

    private final byte[] bytes;
    private final String text;
    private final DataSet.Source source;
    private final TextField name = new TextField();
    private final ComboBox<String> charset = new ComboBox<>();
    private final ComboBox<String> delimiter = new ComboBox<>();
    private final CheckBox header = new CheckBox(I18n.t("import.header"));
    private final ComboBox<DecimalStyle> decimal = new ComboBox<>();
    private final TableView<Integer> preview = new TableView<>();
    private final VBox warnings = new VBox(4);
    private final Label summary = Ui.muted("");
    private final Label error = Ui.errorLabel();
    private DataSet result;
    private boolean updating;

    /** Dosyadan (bytes) ya da yapıştırılan metinden (text); biri null olur. */
    public ImportDialog(AppContext ctx, byte[] bytes, String text, String dataName, DataSet.Source source) {
        this.bytes = bytes;
        this.text = text;
        this.source = source;
        initOwner(ctx.window());
        setTitle(I18n.t("import.title"));
        setResizable(true);
        Theme.style(getDialogPane());

        name.setText(dataName);
        charset.getItems().setAll(CHARSETS);
        charset.setCellFactory(l -> optionCell("import.charsetAuto"));
        charset.setButtonCell(optionCell("import.charsetAuto"));
        charset.setDisable(bytes == null);
        delimiter.getItems().setAll(DELIMITERS);
        delimiter.setCellFactory(l -> delimiterCell());
        delimiter.setButtonCell(delimiterCell());
        decimal.getItems().setAll(DecimalStyle.values());
        decimal.setCellFactory(l -> decimalCell());
        decimal.setButtonCell(decimalCell());
        charset.setValue("");
        delimiter.setValue("");
        charset.valueProperty().addListener((obs, o, n) -> reload(false));
        delimiter.valueProperty().addListener((obs, o, n) -> reload(false));
        header.selectedProperty().addListener((obs, o, n) -> reload(false));
        decimal.valueProperty().addListener((obs, o, n) -> reload(false));

        final HBox options = new HBox(12, Ui.field(I18n.t("import.charset"), charset), Ui.field(I18n.t("import.delimiter"), delimiter),
                Ui.field(I18n.t("import.decimal"), decimal));
        options.setAlignment(Pos.BOTTOM_LEFT);
        final VBox headerBox = new VBox(header);
        headerBox.setAlignment(Pos.BOTTOM_LEFT);
        options.getChildren().add(headerBox);
        preview.getStyleClass().addAll(Styles.STRIPED, Styles.DENSE);
        preview.setPrefSize(860, 360);
        VBox.setVgrow(preview, Priority.ALWAYS);
        final VBox content = new VBox(12, Ui.field(I18n.t("import.name"), name), options, Ui.muted(I18n.t("import.hint")),
                warnings, preview, summary, error);
        content.setPadding(new Insets(6, 4, 0, 4));
        getDialogPane().setContent(content);

        final ButtonType ok = new ButtonType(I18n.t("import.ok"), ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ok, new ButtonType(I18n.t("action.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));
        getDialogPane().lookupButton(ok).addEventFilter(ActionEvent.ACTION, e -> {
            if (result == null) {
                e.consume();
                return;
            }
            final String n = name.getText().strip();
            if (n.isEmpty() || n.length() > 200) {
                Ui.showError(error, I18n.t("import.nameRequired"));
                e.consume();
            }
        });
        setResultConverter(b -> b == ok && result != null ? rename(result) : null);
        reload(true);
    }

    private DataSet rename(DataSet ds) {
        return ds.renamed(name.getText().strip());
    }

    private ImportOptions options() {
        final String c = charset.getValue();
        final String d = delimiter.getValue();
        return new ImportOptions(c == null || c.isEmpty() ? null : c, d == null || d.isEmpty() ? null : d.charAt(0),
                header.isSelected(), decimal.getValue());
    }

    private DataSet read(ImportOptions o, String dataName) {
        final CsvReader.Table table = bytes != null ? CsvReader.read(bytes, o) : CsvReader.readText(text, o);
        return DataSet.of(table, dataName, source);
    }

    /**
     * Ayarlarla yeniden okur. İlk okumada her şey otomatik bulunur ve bulunan değerler kutulara yazılır
     * (kullanıcı neyin tahmin edildiğini görsün); sonraki okumalarda kutulardaki değerler kullanılır.
     */
    private void reload(boolean first) {
        if (updating)
            return;
        Ui.showError(error, null);
        try {
            result = read(first ? ImportOptions.AUTO : options(), name.getText().strip());
            if (first) {
                updating = true;
                header.setSelected(Boolean.TRUE.equals(result.options().header()));
                decimal.setValue(result.options().decimal());
                updating = false;
            }
            fillPreview(result);
        } catch (RuntimeException e) {
            result = null;
            preview.getColumns().clear();
            preview.getItems().clear();
            warnings.getChildren().clear();
            summary.setText("");
            Ui.showError(error, Ui.message(e));
        }
    }

    private void fillPreview(DataSet ds) {
        preview.getColumns().clear();
        for (Column c : ds.columns()) {
            final TableColumn<Integer, String> col = new TableColumn<>(c.name());
            col.setGraphic(new FontIcon(DataView.icon(c.type())));
            col.setCellValueFactory(cell -> new ReadOnlyStringWrapper(c.raw(cell.getValue())));
            col.setSortable(false);
            col.setPrefWidth(Math.max(90, Math.min(200, c.name().length() * 9 + 40)));
            preview.getColumns().add(col);
        }
        preview.getItems().setAll(IntStream.range(0, Math.min(PREVIEW_ROWS, ds.rowCount())).boxed().toList());
        warnings.getChildren().clear();
        for (CsvReader.Warning w : ds.warnings()) {
            final Label l = new Label(I18n.warning(w), new FontIcon(Feather.ALERT_TRIANGLE));
            l.getStyleClass().add("warning-banner");
            l.setWrapText(true);
            warnings.getChildren().add(l);
        }
        final String enc = ds.options().charset() == null ? "" : " · " + ds.options().charset();
        summary.setText(I18n.t("import.summary", I18n.count(ds.rowCount()), ds.columns().size(),
                delimiterName(String.valueOf(ds.options().delimiter()))) + enc);
    }

    // ---------------------------------------------------------------- hücre görünümleri

    private static ListCell<String> optionCell(String autoKey) {
        return new ListCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v.isEmpty() ? I18n.t(autoKey) : v);
            }
        };
    }

    private static ListCell<String> delimiterCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v.isEmpty() ? I18n.t("import.auto") : delimiterName(v));
            }
        };
    }

    static String delimiterName(String d) {
        return switch (d) {
            case "," -> I18n.t("import.comma");
            case ";" -> I18n.t("import.semicolon");
            case "\t" -> I18n.t("import.tab");
            case "|" -> I18n.t("import.pipe");
            default -> d;
        };
    }

    private static ListCell<DecimalStyle> decimalCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(DecimalStyle v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v == DecimalStyle.COMMA ? "1.234,56" : "1,234.56");
            }
        };
    }
}
