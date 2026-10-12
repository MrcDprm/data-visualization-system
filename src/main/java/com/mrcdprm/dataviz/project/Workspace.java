package com.mrcdprm.dataviz.project;

import com.mrcdprm.dataviz.chart.ChartSpec;
import com.mrcdprm.dataviz.chart.Filter;
import com.mrcdprm.dataviz.data.Column;
import com.mrcdprm.dataviz.data.ColumnType;
import com.mrcdprm.dataviz.data.CsvReader;
import com.mrcdprm.dataviz.data.DataSet;
import com.mrcdprm.dataviz.data.ImportOptions;
import com.mrcdprm.dataviz.data.SampleDataset;
import com.mrcdprm.dataviz.data.VizException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Açık çalışma: veri seti, pano filtreleri, grafik kartları, kayıtlı olduğu dosya ve kaydedilmemiş değişiklik var mı.
 * Arayüzden bağımsızdır; arayüz değişiklikten sonra {@link #markDirty()} çağırır.
 */
public final class Workspace {

    private final DataSet data;
    private final String pastedText;
    private final List<Filter> filters = new ArrayList<>();
    private final List<ChartSpec> charts = new ArrayList<>();
    private Path file;
    private boolean dirty;

    public Workspace(DataSet data, String pastedText) {
        this.data = data;
        this.pastedText = pastedText;
    }

    public DataSet data() {
        return data;
    }

    public List<Filter> filters() {
        return filters;
    }

    public List<ChartSpec> charts() {
        return charts;
    }

    public Optional<Path> file() {
        return Optional.ofNullable(file);
    }

    public boolean dirty() {
        return dirty;
    }

    public void markDirty() {
        dirty = true;
    }

    public ChartSpec chart(String id) {
        return charts.stream().filter(c -> c.id().equals(id)).findFirst().orElse(null);
    }

    public void replace(ChartSpec spec) {
        for (int i = 0; i < charts.size(); i++)
            if (charts.get(i).id().equals(spec.id()))
                charts.set(i, spec);
        dirty = true;
    }

    // ---------------------------------------------------------------- kaydetme

    public void save(Path target) throws IOException {
        ProjectFile.save(target, toProject(target));
        file = target;
        dirty = false;
    }

    Project toProject(Path target) {
        final DataSet.Source src = data.source();
        final ImportOptions o = data.options();
        // dosya yolu: proje dosyasıyla aynı klasördeyse sadece dosya adı (klasör taşınınca da açılsın)
        String path = src.file();
        if (path != null && target != null && Path.of(path).getParent() != null
                && Path.of(path).getParent().equals(target.toAbsolutePath().getParent()))
            path = Path.of(path).getFileName().toString();
        final Project.DatasetRef ref = new Project.DatasetRef(data.name(), src.sampleId(), path,
                src.pasted() ? pastedText : null, o.charset(), o.delimiter() == null ? null : String.valueOf(o.delimiter()),
                o.header(), o.decimal());
        final Map<String, ColumnType> types = new LinkedHashMap<>();
        for (Column c : data.columns())
            if (c.type() != c.detectedType())
                types.put(c.name(), c.type());
        return new Project(Project.APP, Project.VERSION, ref, types, List.copyOf(filters), List.copyOf(charts));
    }

    // ---------------------------------------------------------------- açma

    /**
     * Proje dosyasını açar ve veri setini yükler. Veri dosyası kayıtlı yolda yoksa proje klasöründe aynı adla aranır;
     * orada da yoksa "dataFileMissing" hatası verilir, arayüz kullanıcıya dosyayı sordurur ({@link #open(Path, Path)}).
     */
    public static Workspace open(Path projectFile) throws IOException {
        return open(projectFile, null);
    }

    public static Workspace open(Path projectFile, Path relocatedData) throws IOException {
        final Project p = ProjectFile.load(projectFile);
        final Project.DatasetRef ref = p.dataset();
        final ImportOptions options = new ImportOptions(ref.charset(),
                ref.delimiter() == null ? null : ref.delimiter().charAt(0), ref.header(), ref.decimal());
        final DataSet data;
        String pasted = null;
        if (ref.sampleId() != null) {
            data = SampleDataset.of(ref.sampleId()).orElseThrow().load(ref.name());
        } else if (ref.embedded() != null) {
            pasted = ref.embedded();
            data = DataSet.of(CsvReader.readText(pasted, options), ref.name(), DataSet.Source.PASTED);
        } else {
            final Path dataFile = relocatedData != null ? relocatedData : locate(projectFile, ref.file());
            data = DataSet.of(CsvReader.read(dataFile, options), ref.name(), DataSet.Source.file(dataFile.toAbsolutePath().toString()));
        }
        p.types().forEach((name, type) -> data.column(name).ifPresent(c -> {
            try {
                c.setType(type);
            } catch (VizException ignored) {
                // tür artık uygulanamıyor (ör. veri değişti): tahmin edilen tür kalır
            }
        }));
        final Workspace w = new Workspace(data, pasted);
        w.filters.addAll(p.filters());
        w.charts.addAll(p.charts());
        w.file = projectFile;
        w.dirty = relocatedData != null;
        return w;
    }

    private static Path locate(Path projectFile, String stored) {
        final Path path;
        try {
            path = Path.of(stored);
        } catch (java.nio.file.InvalidPathException e) {
            throw new VizException("dataFileMissing", stored);
        }
        if (path.isAbsolute() && Files.isRegularFile(path))
            return path;
        final Path dir = projectFile.toAbsolutePath().getParent();
        final Path sibling = dir.resolve(path.getFileName().toString());
        if (Files.isRegularFile(sibling))
            return sibling;
        throw new VizException("dataFileMissing", stored);
    }
}
