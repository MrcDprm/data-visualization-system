package com.mrcdprm.dataviz.ui;

import com.mrcdprm.dataviz.chart.ChartSuggestions;
import com.mrcdprm.dataviz.data.CsvReader;
import com.mrcdprm.dataviz.data.DataSet;
import com.mrcdprm.dataviz.data.SampleDataset;
import com.mrcdprm.dataviz.data.VizException;
import com.mrcdprm.dataviz.project.ProjectFile;
import com.mrcdprm.dataviz.project.Workspace;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Dosya işlemleri: CSV açma, tablo yapıştırma, örnek veri seti, proje açma/kaydetme. Açık çalışmada kaydedilmemiş
 * değişiklik varsa önce sorulur. Dosya okuma arka planda yapılır (büyük dosyada pencere donmasın).
 */
public final class FileActions {

    private FileActions() {
    }

    // ---------------------------------------------------------------- veri

    public static void openCsv(AppContext ctx, Node busy) {
        if (!confirmDiscard(ctx))
            return;
        final FileChooser chooser = chooser(ctx, I18n.t("start.openCsv"));
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(I18n.t("files.csv"), "*.csv", "*.tsv", "*.txt"),
                new FileChooser.ExtensionFilter(I18n.t("files.all"), "*.*"));
        final File file = chooser.showOpenDialog(ctx.window());
        if (file == null)
            return;
        ctx.settings().setLastFolder(file.toPath().getParent());
        Ui.background(busy, () -> {
            if (Files.size(file.toPath()) > CsvReader.MAX_BYTES)
                throw new VizException("fileTooLarge", CsvReader.MAX_BYTES / 1024 / 1024);
            return Files.readAllBytes(file.toPath());
        }, bytes -> {
            final String name = file.getName().replaceFirst("\\.[^.]+$", "");
            new ImportDialog(ctx, bytes, null, name, DataSet.Source.file(file.getAbsolutePath()))
                    .showAndWait().ifPresent(ds -> start(ctx, new Workspace(ds, null)));
        }, ex -> Ui.error(ctx.window(), ex));
    }

    public static void paste(AppContext ctx) {
        if (!confirmDiscard(ctx))
            return;
        final TextArea area = new TextArea();
        area.setPromptText(I18n.t("paste.prompt"));
        area.setPrefSize(640, 340);
        area.setStyle("-fx-font-family: 'Consolas', monospace;");
        final javafx.scene.input.Clipboard clip = javafx.scene.input.Clipboard.getSystemClipboard();
        if (clip.hasString() && clip.getString().contains("\t"))
            area.setText(clip.getString());
        final Dialog<String> dialog = new Dialog<>();
        dialog.initOwner(ctx.window());
        dialog.setTitle(I18n.t("start.paste"));
        Theme.style(dialog.getDialogPane());
        final Label hint = Ui.muted(I18n.t("paste.hint"));
        dialog.getDialogPane().setContent(new VBox(10, hint, area));
        final ButtonType next = new ButtonType(I18n.t("paste.next"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(next, new ButtonType(I18n.t("action.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.getDialogPane().lookupButton(next).disableProperty().bind(area.textProperty().isEmpty());
        dialog.setResultConverter(b -> b == next ? area.getText() : null);
        javafx.application.Platform.runLater(area::requestFocus);
        dialog.showAndWait().ifPresent(text -> {
            if (text.length() > ProjectFile.MAX_EMBEDDED) {
                Ui.info(ctx.window(), I18n.t("paste.tooLarge"));
                return;
            }
            new ImportDialog(ctx, null, text, I18n.t("paste.name"), DataSet.Source.PASTED)
                    .showAndWait().ifPresent(ds -> start(ctx, new Workspace(ds, text)));
        });
    }

    public static void openSample(AppContext ctx, SampleDataset sample, Node busy) {
        if (!confirmDiscard(ctx))
            return;
        Ui.background(busy, () -> SampleDashboards.open(sample), w -> {
            ctx.setTab("dashboard");
            ctx.nav().showWorkspace(w);
        }, ex -> Ui.error(ctx.window(), ex));
    }

    /** Yeni veri setiyle çalışma: önerilen grafiklerle başlar, pano sekmesi açılır. */
    private static void start(AppContext ctx, Workspace w) {
        w.charts().addAll(ChartSuggestions.suggest(w.data()));
        w.markDirty();
        ctx.setTab("dashboard");
        ctx.nav().showWorkspace(w);
    }

    // ---------------------------------------------------------------- proje

    public static void openProject(AppContext ctx, Node busy) {
        if (!confirmDiscard(ctx))
            return;
        final FileChooser chooser = chooser(ctx, I18n.t("start.openProject"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.t("files.project"), "*" + ProjectFile.EXTENSION));
        final File file = chooser.showOpenDialog(ctx.window());
        if (file != null) {
            ctx.settings().setLastFolder(file.toPath().getParent());
            openProject(ctx, file.toPath(), busy, false);
        }
    }

    /** Proje açar; veri dosyası bulunamazsa kullanıcıya yerini sorar. */
    public static void openProject(AppContext ctx, Path project, Node busy, boolean askDiscard) {
        if (askDiscard && !confirmDiscard(ctx))
            return;
        Ui.background(busy, () -> Workspace.open(project), w -> opened(ctx, project, w), ex -> {
            if (ex instanceof VizException v && v.code().equals("dataFileMissing")) {
                if (!Ui.confirm(ctx.window(), I18n.t("project.locateQuestion", v.args()[0]), I18n.t("project.locate")))
                    return;
                final FileChooser chooser = chooser(ctx, I18n.t("project.locate"));
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.t("files.csv"), "*.csv", "*.tsv", "*.txt"));
                final File data = chooser.showOpenDialog(ctx.window());
                if (data != null)
                    Ui.background(busy, () -> Workspace.open(project, data.toPath()), w -> opened(ctx, project, w),
                            ex2 -> Ui.error(ctx.window(), ex2));
            } else {
                Ui.error(ctx.window(), ex);
            }
        });
    }

    private static void opened(AppContext ctx, Path project, Workspace w) {
        ctx.settings().addRecent(project);
        ctx.setTab("dashboard");
        ctx.nav().showWorkspace(w);
    }

    /** Kaydet: daha önce kaydedildiyse aynı dosyaya, değilse "Farklı kaydet". Başarılıysa true. */
    public static boolean save(AppContext ctx) {
        final Workspace w = ctx.workspace();
        if (w == null)
            return true;
        final Optional<Path> file = w.file();
        return file.isPresent() ? write(ctx, file.get()) : saveAs(ctx);
    }

    public static boolean saveAs(AppContext ctx) {
        final Workspace w = ctx.workspace();
        final FileChooser chooser = chooser(ctx, I18n.t("project.saveAs"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.t("files.project"), "*" + ProjectFile.EXTENSION));
        chooser.setInitialFileName(Exporter.fileName(w.data().name(), ProjectFile.EXTENSION));
        final File file = chooser.showSaveDialog(ctx.window());
        if (file == null)
            return false;
        Path target = file.toPath();
        if (!target.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(ProjectFile.EXTENSION))
            target = target.resolveSibling(target.getFileName() + ProjectFile.EXTENSION);
        ctx.settings().setLastFolder(target.getParent());
        return write(ctx, target);
    }

    private static boolean write(AppContext ctx, Path target) {
        try {
            ctx.workspace().save(target);
            ctx.settings().addRecent(target);
            ctx.nav().updateTitle();
            return true;
        } catch (Exception e) {
            Ui.error(ctx.window(), e);
            return false;
        }
    }

    /**
     * Kaydedilmemiş değişiklik varsa sorar: Kaydet / Kaydetme / Vazgeç. Devam edilebiliyorsa true.
     */
    public static boolean confirmDiscard(AppContext ctx) {
        final Workspace w = ctx.workspace();
        if (w == null || !w.dirty())
            return true;
        final ButtonType save = new ButtonType(I18n.t("project.save"), ButtonBar.ButtonData.YES);
        final ButtonType discard = new ButtonType(I18n.t("project.discard"), ButtonBar.ButtonData.NO);
        final ButtonType cancel = new ButtonType(I18n.t("action.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, I18n.t("project.unsaved", w.data().name()), save, discard, cancel);
        alert.initOwner(ctx.window());
        alert.setTitle(I18n.t("dialog.confirm"));
        alert.setHeaderText(null);
        final Optional<ButtonType> answer = alert.showAndWait();
        if (answer.isEmpty() || answer.get() == cancel)
            return false;
        return answer.get() == discard || save(ctx);
    }

    private static FileChooser chooser(AppContext ctx, String title) {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.setInitialDirectory(ctx.settings().lastFolder().toFile());
        return chooser;
    }
}
