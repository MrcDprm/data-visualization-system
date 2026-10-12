package com.mrcdprm.dataviz.ui;

import atlantafx.base.theme.Styles;
import com.mrcdprm.dataviz.DataVizApp;
import com.mrcdprm.dataviz.data.SampleDataset;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Hakkında penceresi: sürüm, açıklama, kaynak kodu, lisans, kütüphaneler ve örnek veri setlerinin kaynakları. */
public final class AboutDialog extends Dialog<Void> {

    private static final double TEXT_WIDTH = 420;

    public AboutDialog(AppContext ctx) {
        initOwner(ctx.window());
        setTitle(I18n.t("about.title"));
        getDialogPane().getButtonTypes().add(new ButtonType(I18n.t("action.close"), ButtonBar.ButtonData.CANCEL_CLOSE));
        Theme.style(getDialogPane());

        final ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/com/mrcdprm/dataviz/icon.png")));
        icon.setFitWidth(64);
        icon.setFitHeight(64);

        final Label title = new Label(I18n.t("app.name"));
        title.getStyleClass().add(Styles.TITLE_3);
        final Hyperlink repo = new Hyperlink(I18n.t("about.source"));
        repo.setOnAction(e -> ctx.openUrl(DataVizApp.REPO_URL));
        repo.setPadding(Insets.EMPTY);

        final VBox text = new VBox(6, title, Ui.muted(I18n.t("about.version", DataVizApp.VERSION)),
                wrap(new Label(I18n.t("about.description"))), repo, Ui.muted(I18n.t("about.license")),
                Ui.muted("JavaFX, AtlantaFX, Ikonli, Gson"), Ui.section(I18n.t("about.dataSources")));
        for (SampleDataset s : SampleDataset.values()) {
            final Hyperlink link = new Hyperlink(s.sourceName() + " (" + s.license() + ")");
            link.setOnAction(e -> ctx.openUrl(s.sourceUrl()));
            link.setPadding(Insets.EMPTY);
            link.setWrapText(true);
            text.getChildren().add(link);
        }
        text.getChildren().add(Ui.muted(I18n.t("about.dataFolder", Settings.dataFolder())));
        // Sabit genişlik: uzun satırlar kenara taşmadan alt satıra geçer
        text.setPrefWidth(TEXT_WIDTH);
        text.setMinWidth(TEXT_WIDTH);
        for (Node n : text.getChildren())
            if (n instanceof Label l)
                wrap(l);
            else if (n instanceof Hyperlink h)
                h.setMaxWidth(TEXT_WIDTH);
        final HBox content = new HBox(18, icon, text);
        content.setAlignment(Pos.TOP_LEFT);
        content.setPadding(new Insets(12, 16, 4, 8));
        getDialogPane().setContent(content);
    }

    private static Label wrap(Label l) {
        l.setWrapText(true);
        l.setMaxWidth(TEXT_WIDTH);
        l.setMinHeight(Region.USE_PREF_SIZE);
        return l;
    }
}
