package com.mrcdprm.dataviz.ui;

import com.mrcdprm.dataviz.chart.ChartData;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.transform.Transform;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;

/** Grafiği PNG, grafik verisini CSV olarak kaydetme. */
public final class Exporter {

    private Exporter() {
    }

    /** Dosya adında kullanılamayan karakterleri atar: "CO₂ / yıl" -> "CO₂ _ yıl". */
    public static String fileName(String title, String extension) {
        String s = title == null ? "" : title.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").strip();
        if (s.length() > 80)
            s = s.substring(0, 80).strip();
        return (s.isEmpty() ? "chart" : s) + extension;
    }

    public static File choose(AppContext ctx, String title, String suggested, String description, String pattern) {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.setInitialFileName(suggested);
        chooser.setInitialDirectory(ctx.settings().lastFolder().toFile());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, pattern));
        final File file = chooser.showSaveDialog(ctx.window());
        if (file != null)
            ctx.settings().setLastFolder(file.toPath().getParent());
        return file;
    }

    // ---------------------------------------------------------------- PNG

    /** Grafiği 2 kat çözünürlükte çizip PNG olarak yazar (sunumda ve raporda net görünsün). */
    public static void png(Node node, Path file) throws IOException {
        final SnapshotParameters params = new SnapshotParameters();
        params.setTransform(Transform.scale(2, 2));
        final WritableImage img = node.snapshot(params, null);
        final int w = (int) img.getWidth();
        final int h = (int) img.getHeight();
        final BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        final PixelReader r = img.getPixelReader();
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                out.setRGB(x, y, r.getArgb(x, y));
        if (!ImageIO.write(out, "png", file.toFile()))
            throw new IOException("PNG yazılamadı");
    }

    // ---------------------------------------------------------------- CSV

    /** Grafik verisini tablo olarak: ilk sütun kategori (ya da X), sonra her seri bir sütun. */
    public static List<List<String>> table(ChartData d, String xName) {
        final java.util.ArrayList<List<String>> rows = new java.util.ArrayList<>();
        if (d.scatter()) {
            rows.add(List.of(I18n.t("data.series"), d.xName(), d.yName()));
            for (ChartData.Points p : d.points())
                for (int i = 0; i < p.x().length; i++)
                    rows.add(List.of(p.name(), plain(p.x()[i]), plain(p.y()[i])));
            return rows;
        }
        final java.util.ArrayList<String> header = new java.util.ArrayList<>();
        header.add(xName);
        d.series().forEach(s -> header.add(s.name()));
        rows.add(header);
        for (int i = 0; i < d.categories().size(); i++) {
            final java.util.ArrayList<String> row = new java.util.ArrayList<>();
            row.add(d.categories().get(i));
            for (ChartData.Series s : d.series())
                row.add(plain(s.values()[i]));
            rows.add(row);
        }
        return rows;
    }

    /** Binlik ayırıcısız, seçili dilin ondalık ayırıcısıyla sayı (Excel'de sayı olarak açılsın); boşsa boş. */
    static String plain(double v) {
        if (Double.isNaN(v))
            return "";
        final DecimalFormat f = new DecimalFormat("0.######", DecimalFormatSymbols.getInstance(I18n.locale()));
        return f.format(v);
    }

    /**
     * CSV yazar. Türkçede noktalı virgül ayırıcı (ondalık virgül olduğu için), İngilizcede virgül; Excel Türkçe
     * karakterleri doğru göstersin diye UTF-8 BOM. "=", "+", "-", "@" ile başlayan metin hücreleri tek tırnakla başlatılır:
     * Excel bunları formül olarak çalıştırmasın (CSV/formula injection). Sayılar dokunulmadan yazılır.
     */
    public static void csv(List<List<String>> rows, Path file) throws IOException {
        final char delimiter = I18n.isTurkish() ? ';' : ',';
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write('﻿');
            for (List<String> row : rows) {
                for (int i = 0; i < row.size(); i++) {
                    if (i > 0)
                        w.write(delimiter);
                    w.write(cell(row.get(i), delimiter));
                }
                w.write("\r\n");
            }
        }
    }

    static String cell(String value, char delimiter) {
        String v = value == null ? "" : value;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0 && !isNumber(v))
            v = "'" + v;
        if (v.indexOf(delimiter) >= 0 || v.indexOf('"') >= 0 || v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0)
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }

    private static boolean isNumber(String v) {
        return v.matches("[+-]?\\d+([.,]\\d+)?");
    }
}
