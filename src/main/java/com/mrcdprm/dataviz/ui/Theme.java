package com.mrcdprm.dataviz.ui;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

/** AtlantaFX Primer teması (açık/koyu) ve uygulamanın kendi stil dosyası. */
public final class Theme {

    private static final String CSS = Theme.class.getResource("/com/mrcdprm/dataviz/app.css").toExternalForm();

    private Theme() {
    }

    public static void apply(boolean dark) {
        Application.setUserAgentStylesheet(dark ? new PrimerDark().getUserAgentStylesheet()
                : new PrimerLight().getUserAgentStylesheet());
    }

    public static void addStylesheet(Scene scene) {
        scene.getStylesheets().add(CSS);
    }

    /** Kendi diyaloglarımızda da aynı sınıflar (panel, field-label vb.) çalışsın; genişlik içerikten gelsin. */
    public static void style(DialogPane pane) {
        pane.getStylesheets().add(CSS);
        // AtlantaFX stil dosyası diyalogları 600 piksele sınırlıyor (.dialog-pane -fx-max-width); grafik düzenleyici ve
        // içe aktarma penceresi daha geniş olmalı. Satır içi stil bütün stil dosyalarından önceliklidir.
        pane.setStyle("-fx-max-width: 4000px;");
    }
}
