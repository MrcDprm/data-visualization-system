package com.mrcdprm.dataviz.ui;

import atlantafx.base.theme.Styles;
import com.mrcdprm.dataviz.data.DataSet;
import com.mrcdprm.dataviz.project.Workspace;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Çalışma ekranı: üstte veri setinin adı ve boyutu, "Pano" / "Veri" sekmeleri, kaydet, farklı kaydet, aç ve
 * başlangıca dönüş. Ctrl+S kaydeder. Veri sekmesinde tür değişince pano sekmesine dönüldüğünde grafikler yeniden hesaplanır.
 */
public final class WorkspaceView extends BorderPane {

    private final AppContext ctx;

    public WorkspaceView(AppContext ctx) {
        this.ctx = ctx;
        getStyleClass().add("shell");
        final Workspace w = ctx.workspace();
        final DataSet ds = w.data();

        final ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/com/mrcdprm/dataviz/icon.png")));
        logo.setFitWidth(28);
        logo.setFitHeight(28);
        final Button home = new Button(null, logo);
        home.getStyleClass().addAll(Styles.FLAT, Styles.BUTTON_ICON);
        home.setTooltip(new javafx.scene.control.Tooltip(I18n.t("workspace.home")));
        home.setAccessibleText(I18n.t("workspace.home"));
        home.setOnAction(e -> {
            if (FileActions.confirmDiscard(ctx))
                ctx.nav().showStart();
        });
        final Label name = new Label(ds.name());
        name.getStyleClass().add("topbar-title");
        name.setMaxWidth(320);
        final Label size = Ui.muted(I18n.t("data.size", I18n.count(ds.rowCount()), ds.columns().size()));
        final VBox titles = new VBox(0, name, size);

        final ToggleButton dashboard = new ToggleButton(I18n.t("workspace.dashboard"), new FontIcon(Feather.GRID));
        final ToggleButton data = new ToggleButton(I18n.t("workspace.data"), new FontIcon(Feather.DATABASE));
        final ToggleGroup tabs = new ToggleGroup();
        for (ToggleButton t : new ToggleButton[] {dashboard, data}) {
            t.setToggleGroup(tabs);
            t.getStyleClass().add("top-tab");
        }
        tabs.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n == null) {
                tabs.selectToggle(o);
                return;
            }
            ctx.setTab(n == data ? "data" : "dashboard");
            setCenter(n == data ? new DataView(ctx) : new DashboardView(ctx));
        });

        final Button save = Ui.button(I18n.t("project.save"), Feather.SAVE, Styles.FLAT);
        save.setOnAction(e -> FileActions.save(ctx));
        save.setTooltip(new javafx.scene.control.Tooltip("Ctrl+S"));
        final Button saveAs = Ui.iconButton(Feather.COPY, I18n.t("project.saveAs"));
        saveAs.setOnAction(e -> FileActions.saveAs(ctx));
        final Button open = Ui.iconButton(Feather.FOLDER, I18n.t("start.openProject"));
        open.setOnAction(e -> FileActions.openProject(ctx, open));

        final HBox bar = new HBox(12, home, titles, new HBox(4, dashboard, data), Ui.spacer(), save, saveAs, open, new TopTools(ctx));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("topbar");
        bar.setPadding(new Insets(8, 16, 8, 14));
        setTop(bar);

        sceneProperty().addListener((obs, o, scene) -> {
            if (scene != null)
                scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), () -> FileActions.save(ctx));
        });
        tabs.selectToggle("data".equals(ctx.tab()) ? data : dashboard);
    }
}
