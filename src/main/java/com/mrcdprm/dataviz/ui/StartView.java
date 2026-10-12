package com.mrcdprm.dataviz.ui;

import atlantafx.base.theme.Styles;
import com.mrcdprm.dataviz.data.SampleDataset;
import java.nio.file.Path;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Başlangıç ekranı: CSV aç, tablo yapıştır, proje aç; kaynak ve lisansıyla örnek açık veri setleri; son projeler.
 */
public final class StartView extends BorderPane {

    private final AppContext ctx;

    public StartView(AppContext ctx) {
        this.ctx = ctx;
        getStyleClass().add("shell");
        setTop(topBar());

        final Label title = new Label(I18n.t("start.title"));
        title.getStyleClass().add("hero-title");
        final Label subtitle = new Label(I18n.t("start.subtitle"));
        subtitle.getStyleClass().add("hero-subtitle");
        subtitle.setWrapText(true);

        final HBox actions = new HBox(14,
                action(Feather.FILE_TEXT, "start.openCsv", "start.openCsvHint", b -> FileActions.openCsv(ctx, b)),
                action(Feather.CLIPBOARD, "start.paste", "start.pasteHint", b -> FileActions.paste(ctx)),
                action(Feather.FOLDER, "start.openProject", "start.openProjectHint", b -> FileActions.openProject(ctx, b)));

        actions.setFillHeight(false);
        final HBox samples = new HBox(14);
        for (SampleDataset s : SampleDataset.values())
            samples.getChildren().add(sampleCard(s));

        final VBox page = new VBox(18, new VBox(6, title, subtitle), actions,
                Ui.section(I18n.t("start.samples")), Ui.muted(I18n.t("start.samplesHint")), samples, recent());
        page.setPadding(new Insets(28, 32, 32, 32));
        page.setMaxWidth(1120);
        final StackPane center = new StackPane(page);
        center.setAlignment(Pos.TOP_CENTER);
        final ScrollPane scroll = new ScrollPane(center);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("transparent-scroll");
        setCenter(scroll);
    }

    private HBox topBar() {
        final ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/com/mrcdprm/dataviz/icon.png")));
        logo.setFitWidth(30);
        logo.setFitHeight(30);
        final Label name = new Label(I18n.t("app.name"));
        name.getStyleClass().add("topbar-title");
        final HBox bar = new HBox(10, logo, name, Ui.spacer(), new TopTools(ctx));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("topbar");
        bar.setPadding(new Insets(10, 16, 10, 22));
        return bar;
    }

    private Button action(Ikon icon, String titleKey, String hintKey, java.util.function.Consumer<Button> onClick) {
        final FontIcon i = new FontIcon(icon);
        i.getStyleClass().add("action-icon");
        final Label t = new Label(I18n.t(titleKey));
        t.getStyleClass().add(Styles.TITLE_4);
        final Label h = Ui.muted(I18n.t(hintKey));
        h.setWrapText(false);
        final VBox text = new VBox(4, t, h);
        final HBox content = new HBox(14, i, text);
        content.setAlignment(Pos.CENTER_LEFT);
        final Button b = new Button(null, content);
        b.getStyleClass().add("action-card");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setMaxHeight(Button.USE_PREF_SIZE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setOnAction(e -> onClick.accept(b));
        HBox.setHgrow(b, Priority.ALWAYS);
        return b;
    }

    private VBox sampleCard(SampleDataset s) {
        final FontIcon icon = new FontIcon(switch (s) {
            case OWID_CO2 -> Feather.GLOBE;
            case NASA_GISTEMP -> Feather.THERMOMETER;
            case IBB_DAMS -> Feather.DROPLET;
        });
        icon.getStyleClass().add("sample-icon");
        final Label name = new Label(I18n.t("sample." + s.id() + ".name"));
        name.getStyleClass().add(Styles.TITLE_4);
        name.setWrapText(true);
        final Label description = Ui.muted(I18n.t("sample." + s.id() + ".description"));
        final Hyperlink source = new Hyperlink(I18n.t("start.source", s.sourceName()));
        source.setWrapText(true);
        source.setOnAction(e -> ctx.openUrl(s.sourceUrl()));
        final Hyperlink license = new Hyperlink(I18n.t("start.license", s.license()));
        license.setOnAction(e -> ctx.openUrl(s.licenseUrl()));
        for (Hyperlink l : List.of(source, license))
            l.setPadding(Insets.EMPTY);
        final Button open = Ui.button(I18n.t("start.openSample"), Feather.ARROW_RIGHT, Styles.ACCENT);
        open.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        open.setOnAction(e -> FileActions.openSample(ctx, s, open));
        final VBox grow = new VBox();
        VBox.setVgrow(grow, Priority.ALWAYS);
        final VBox card = Ui.card(icon, name, description, grow, source, license, open);
        card.getStyleClass().add("sample-card");
        card.setPrefWidth(340);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox recent() {
        final List<Path> list = ctx.settings().recent();
        final VBox box = new VBox(6, Ui.section(I18n.t("start.recent")));
        if (list.isEmpty()) {
            box.getChildren().add(Ui.muted(I18n.t("start.noRecent")));
            return box;
        }
        for (Path p : list) {
            final Hyperlink link = new Hyperlink(p.getFileName().toString(), new FontIcon(Feather.FILE));
            link.setMnemonicParsing(false);
            link.setOnAction(e -> FileActions.openProject(ctx, p, link, true));
            final Label folder = Ui.muted(p.getParent() == null ? "" : p.getParent().toString());
            final HBox row = new HBox(10, link, folder);
            row.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().add(row);
        }
        return box;
    }
}
