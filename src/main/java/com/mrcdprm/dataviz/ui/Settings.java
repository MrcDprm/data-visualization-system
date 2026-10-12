package com.mrcdprm.dataviz.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Kullanıcı tercihleri: dil, tema, son açılan projeler ve son kullanılan klasör.
 * Dosyadan okunan değere güvenilmez: bilinmeyen değer yerine varsayılan, var olmayan dosya yerine boş liste.
 */
public final class Settings {

    public static final int MAX_RECENT = 8;

    private final Path file;
    private final Properties props = new Properties();

    public Settings(Path folder) {
        this.file = folder.resolve("settings.properties");
        if (Files.isRegularFile(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
            } catch (IOException | IllegalArgumentException ignored) {
                // bozuk dosya: varsayılanlarla devam
            }
        }
    }

    /** Veri klasörü: %APPDATA%\MrcDprm\DataVisualizer (program klasörüne yazılmaz). */
    public static Path dataFolder() {
        final String override = System.getProperty("dataviz.dataDir"); // geliştirme ve ekran görüntüleri için
        if (override != null && !override.isBlank())
            return Path.of(override);
        final String appData = System.getenv("APPDATA");
        final Path base = appData != null ? Path.of(appData) : Path.of(System.getProperty("user.home"));
        return base.resolve("MrcDprm").resolve("DataVisualizer");
    }

    public String language() {
        return "en".equals(props.getProperty("language")) ? "en" : "tr";
    }

    public void setLanguage(String language) {
        props.setProperty("language", "en".equals(language) ? "en" : "tr");
        save();
    }

    public boolean darkTheme() {
        return "dark".equals(props.getProperty("theme"));
    }

    public void setDarkTheme(boolean dark) {
        props.setProperty("theme", dark ? "dark" : "light");
        save();
    }

    /** Son açılan projeler, yeniden eskiye; artık var olmayan dosyalar listelenmez. */
    public List<Path> recent() {
        final List<Path> out = new ArrayList<>();
        for (int i = 0; i < MAX_RECENT; i++) {
            final String p = props.getProperty("recent." + i);
            if (p == null)
                continue;
            try {
                final Path path = Path.of(p);
                if (Files.isRegularFile(path))
                    out.add(path);
            } catch (InvalidPathException ignored) {
                // bozuk kayıt atlanır
            }
        }
        return out;
    }

    public void addRecent(Path project) {
        final List<String> list = new ArrayList<>();
        list.add(project.toAbsolutePath().toString());
        for (int i = 0; i < MAX_RECENT; i++) {
            final String p = props.getProperty("recent." + i);
            if (p != null && !list.contains(p))
                list.add(p);
        }
        for (int i = 0; i < MAX_RECENT; i++) {
            if (i < list.size())
                props.setProperty("recent." + i, list.get(i));
            else
                props.remove("recent." + i);
        }
        save();
    }

    /** Dosya seçme pencerelerinin açılacağı klasör; yoksa Belgeler. */
    public Path lastFolder() {
        final String p = props.getProperty("lastFolder");
        try {
            if (p != null && Files.isDirectory(Path.of(p)))
                return Path.of(p);
        } catch (InvalidPathException ignored) {
            // varsayılana düş
        }
        final Path documents = Path.of(System.getProperty("user.home"), "Documents");
        return Files.isDirectory(documents) ? documents : Path.of(System.getProperty("user.home"));
    }

    public void setLastFolder(Path folder) {
        if (folder != null) {
            props.setProperty("lastFolder", folder.toAbsolutePath().toString());
            save();
        }
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                props.store(out, "Data Visualizer");
            }
        } catch (IOException ignored) {
            // tercih kaydedilemezse uygulama yine çalışır
        }
    }
}
