package com.mrcdprm.dataviz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mrcdprm.dataviz.chart.Aggregation;
import com.mrcdprm.dataviz.chart.ChartSpec;
import com.mrcdprm.dataviz.chart.ChartType;
import com.mrcdprm.dataviz.chart.Filter;
import com.mrcdprm.dataviz.chart.Palette;
import com.mrcdprm.dataviz.data.ColumnType;
import com.mrcdprm.dataviz.data.CsvReader;
import com.mrcdprm.dataviz.data.DataSet;
import com.mrcdprm.dataviz.data.ImportOptions;
import com.mrcdprm.dataviz.data.SampleDataset;
import com.mrcdprm.dataviz.data.VizException;
import com.mrcdprm.dataviz.project.Workspace;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Proje dosyası: kaydet/aç, dosya taşınınca bulma, yapıştırılan veri, bozuk ve kötü niyetli dosyalar. */
class ProjectTest {

    @TempDir
    Path dir;

    private static ChartSpec bar() {
        return ChartSpec.of(ChartType.BAR, "Şehirler", "city", List.of("amount"), Aggregation.SUM).withPalette(Palette.COLORBLIND);
    }

    @Test
    void saveAndOpenWithFileAndTypeOverride() throws IOException {
        final Path csv = dir.resolve("sales.csv");
        Files.writeString(csv, "city;amount;code\nİstanbul;10,5;001\nAnkara;3;002\n", StandardCharsets.UTF_8);
        final DataSet ds = DataSet.of(CsvReader.read(csv, ImportOptions.AUTO), "Satışlar", DataSet.Source.file(csv.toString()));
        ds.column("code").orElseThrow().setType(ColumnType.TEXT);
        final Workspace w = new Workspace(ds, null);
        w.charts().add(bar());
        w.filters().add(Filter.values("city", List.of("İstanbul")));
        final Path project = dir.resolve("p.dvz");
        w.save(project);
        assertFalse(w.dirty());

        final Workspace back = Workspace.open(project);
        assertEquals("Satışlar", back.data().name());
        assertEquals(10.5, back.data().column("amount").orElseThrow().number(0), 1e-9);
        assertEquals(ColumnType.TEXT, back.data().column("code").orElseThrow().type());
        assertEquals(1, back.charts().size());
        assertEquals(Palette.COLORBLIND, back.charts().get(0).palette());
        assertEquals(List.of("İstanbul"), back.filters().get(0).values());
        // aynı klasördeki veri dosyası sadece adıyla kaydedilir: klasör taşınınca da bulunur
        assertTrue(Files.readString(project).contains("\"file\": \"sales.csv\""));
    }

    @Test
    void missingDataFileCanBeRelocated() throws IOException {
        final Path csv = dir.resolve("a.csv");
        Files.writeString(csv, "x,y\n1,2\n", StandardCharsets.UTF_8);
        final Workspace w = new Workspace(DataSet.of(CsvReader.read(csv, ImportOptions.AUTO), "a",
                DataSet.Source.file(csv.toString())), null);
        final Path project = dir.resolve("p.dvz");
        w.save(project);
        final Path moved = dir.resolve("moved.csv");
        Files.move(csv, moved);
        assertEquals("dataFileMissing", assertThrows(VizException.class, () -> Workspace.open(project)).code());
        final Workspace back = Workspace.open(project, moved);
        assertEquals(1, back.data().rowCount());
        assertTrue(back.dirty(), "yeni yol kaydedilmeli");
    }

    @Test
    void pastedDataAndSamplesAreRestored() throws IOException {
        final String text = "Ürün\tAdet\nKalem\t3\n";
        final Workspace pasted = new Workspace(DataSet.of(CsvReader.readText(text, ImportOptions.AUTO), "Yapıştırılan",
                DataSet.Source.PASTED), text);
        pasted.save(dir.resolve("pasted.dvz"));
        assertEquals(3, Workspace.open(dir.resolve("pasted.dvz")).data().column("Adet").orElseThrow().number(0), 1e-9);

        final Workspace sample = new Workspace(SampleDataset.NASA_GISTEMP.load("Sıcaklık"), null);
        sample.filters().add(Filter.dates("Date", LocalDate.of(2000, 1, 1), null));
        sample.save(dir.resolve("s.dvz"));
        final Workspace back = Workspace.open(dir.resolve("s.dvz"));
        assertEquals(LocalDate.of(2000, 1, 1), back.filters().get(0).fromDate());
        assertTrue(Files.size(dir.resolve("s.dvz")) < 5_000, "örnek veri dosyaya gömülmez");
    }

    @Test
    void untrustedProjectFiles() throws IOException {
        final Path p = dir.resolve("bad.dvz");
        Files.writeString(p, "{ not json");
        assertEquals("projectInvalid", assertThrows(VizException.class, () -> Workspace.open(p)).code());
        Files.writeString(p, "{\"app\":\"other\",\"version\":1}");
        assertEquals("projectInvalid", assertThrows(VizException.class, () -> Workspace.open(p)).code());
        Files.writeString(p, "{\"app\":\"data-visualizer\",\"version\":99,\"dataset\":{\"sampleId\":\"OWID_CO2\"}}");
        assertEquals("projectNewer", assertThrows(VizException.class, () -> Workspace.open(p)).code());
        Files.writeString(p, "{\"app\":\"data-visualizer\",\"version\":1,\"dataset\":{\"sampleId\":\"EVIL\"}}");
        assertEquals("projectInvalid", assertThrows(VizException.class, () -> Workspace.open(p)).code());
        // bilinmeyen grafik türü ve sınır dışı değerler güvenli varsayılana döner, aynı kimlik tekrarlanmaz
        Files.writeString(p, """
                {"app":"data-visualizer","version":1,"dataset":{"name":"t","sampleId":"NASA_GISTEMP"},
                 "types":{"Year":"WHATEVER"},
                 "filters":[{"column":"Year","kind":"RANGE","min":1900}, {"column":null,"kind":"RANGE"}],
                 "charts":[{"id":"a","type":"HOLOGRAM","x":"Year","y":["Anomaly (°C)"],"topN":100000,"bins":-5},
                           {"id":"a","type":"LINE","x":"Year"}]}
                """);
        final Workspace w = Workspace.open(p);
        assertEquals(1, w.filters().size());
        assertEquals(ChartType.BAR, w.charts().get(0).type());
        assertEquals(ChartSpec.MAX_TOP_N, w.charts().get(0).topN());
        assertEquals(0, w.charts().get(0).bins());
        assertFalse(w.charts().get(0).id().equals(w.charts().get(1).id()));
        assertEquals(ColumnType.NUMBER, w.data().column("Year").orElseThrow().type());
    }
}
