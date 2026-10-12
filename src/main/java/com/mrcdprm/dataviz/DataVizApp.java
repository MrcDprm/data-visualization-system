package com.mrcdprm.dataviz;

import com.mrcdprm.dataviz.project.Workspace;
import com.mrcdprm.dataviz.ui.AppContext;
import com.mrcdprm.dataviz.ui.FileActions;
import com.mrcdprm.dataviz.ui.I18n;
import com.mrcdprm.dataviz.ui.Settings;
import com.mrcdprm.dataviz.ui.StartView;
import com.mrcdprm.dataviz.ui.Theme;
import com.mrcdprm.dataviz.ui.Ui;
import com.mrcdprm.dataviz.ui.WorkspaceView;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Uygulama girişi: başlangıç ekranı ya da (komut satırında .dvz dosyası verildiyse, ör. dosyaya çift tıklayınca)
 * doğrudan o proje açılır. Pencere kapanırken kaydedilmemiş değişiklik varsa sorulur.
 */
public class DataVizApp extends Application implements AppContext.Navigator {

    public static final String VERSION = "1.0.0";
    public static final String REPO_URL = "https://github.com/MrcDprm/data-visualization-system";

    private Stage stage;
    private Scene scene;
    private AppContext ctx;
    private Settings settings;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        final Path folder = Settings.dataFolder();
        settings = new Settings(folder);
        I18n.setLocale(Locale.forLanguageTag(settings.language()));
        Theme.apply(settings.darkTheme());
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/mrcdprm/dataviz/icon.png")));
        ctx = new AppContext(settings, this, url -> getHostServices().showDocument(url));
        // Beklenmeyen bir hata (ör. bir düğmenin işlevinde) sessizce kaybolmasın: günlüğe yazılır, kullanıcıya
        // ayrıntısız kısa bir mesaj gösterilir (stack trace asla gösterilmez)
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            System.getLogger("dataviz").log(System.Logger.Level.ERROR, "Yakalanmayan hata", error);
            if (Platform.isFxApplicationThread())
                Ui.error(ctx.window(), error);
            else
                Platform.runLater(() -> Ui.error(ctx.window(), error));
        });

        scene = new Scene(new javafx.scene.layout.StackPane(), 1280, 820);
        Theme.addStylesheet(scene);
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(660);
        stage.setOnCloseRequest(e -> {
            if (!FileActions.confirmDiscard(ctx))
                e.consume();
        });
        showStart();
        stage.show();

        final List<String> args = getParameters().getRaw();
        if (!args.isEmpty()) {
            final Path project = Path.of(args.get(0));
            if (Files.isRegularFile(project))
                FileActions.openProject(ctx, project, scene.getRoot(), false);
        }
    }

    private void show(Parent root) {
        scene.setRoot(root);
        ctx.setWindow(stage);
        updateTitle();
    }

    @Override
    public void showStart() {
        ctx.setWorkspace(null);
        show(new StartView(ctx));
    }

    @Override
    public void showWorkspace(Workspace workspace) {
        ctx.setWorkspace(workspace);
        show(new WorkspaceView(ctx));
    }

    @Override
    public void rebuild() {
        Theme.apply(settings.darkTheme());
        show(ctx.workspace() == null ? new StartView(ctx) : new WorkspaceView(ctx));
    }

    @Override
    public void updateTitle() {
        final Workspace w = ctx.workspace();
        if (w == null) {
            stage.setTitle(I18n.t("app.name"));
            return;
        }
        final String file = w.file().map(p -> p.getFileName().toString()).orElse(I18n.t("project.untitled"));
        stage.setTitle((w.dirty() ? "* " : "") + file + " - " + I18n.t("app.name"));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
