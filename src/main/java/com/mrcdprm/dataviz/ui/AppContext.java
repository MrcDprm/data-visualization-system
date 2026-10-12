package com.mrcdprm.dataviz.ui;

import com.mrcdprm.dataviz.project.Workspace;
import java.util.function.Consumer;
import javafx.stage.Window;

/** Ekranların ortak ihtiyaçları: ayarlar, açık çalışma, pencere ve uygulama düzeyindeki geçişler. */
public final class AppContext {

    public interface Navigator {
        /** Başlangıç ekranı (dosya aç, yapıştır, örnek veri setleri, son projeler). */
        void showStart();

        void showWorkspace(Workspace workspace);

        /** Dil ya da tema değişince mevcut ekranı baştan kurar (açık çalışma korunur). */
        void rebuild();

        /** Pencere başlığını günceller (kaydedilmemiş değişiklikte "*"). */
        void updateTitle();
    }

    private final Settings settings;
    private final Navigator navigator;
    private final Consumer<String> openUrl;
    private Window window;
    private Workspace workspace;
    private String tab = "dashboard";

    public AppContext(Settings settings, Navigator navigator, Consumer<String> openUrl) {
        this.settings = settings;
        this.navigator = navigator;
        this.openUrl = openUrl;
    }

    public Settings settings() {
        return settings;
    }

    public Navigator nav() {
        return navigator;
    }

    public Window window() {
        return window;
    }

    public void setWindow(Window window) {
        this.window = window;
    }

    public Workspace workspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    /** Açık sekme ("dashboard" ya da "data"); dil/tema değişince aynı sekmeye dönülür. */
    public String tab() {
        return tab;
    }

    public void setTab(String tab) {
        this.tab = tab;
    }

    public void openUrl(String url) {
        openUrl.accept(url);
    }

    /** Değişiklik oldu: çalışma kaydedilmemiş sayılır ve başlıkta "*" görünür. */
    public void changed() {
        if (workspace != null) {
            workspace.markDirty();
            navigator.updateTitle();
        }
    }
}
