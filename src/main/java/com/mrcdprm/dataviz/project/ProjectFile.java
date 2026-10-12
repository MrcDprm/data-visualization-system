package com.mrcdprm.dataviz.project;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mrcdprm.dataviz.chart.ChartSpec;
import com.mrcdprm.dataviz.chart.Filter;
import com.mrcdprm.dataviz.data.ColumnType;
import com.mrcdprm.dataviz.data.SampleDataset;
import com.mrcdprm.dataviz.data.VizException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Proje dosyasını JSON olarak yazar ve okur. Okunan dosyaya güvenilmez (başkasından gelmiş ya da elle değiştirilmiş
 * olabilir): boyut, sürüm, alanların uzunluğu ve sayısı sınırlanır; bilinmeyen değerler atılır ya da varsayılana döner.
 */
public final class ProjectFile {

    public static final String EXTENSION = ".dvz";
    public static final long MAX_BYTES = 20L * 1024 * 1024;
    public static final int MAX_EMBEDDED = 10 * 1024 * 1024;
    public static final int MAX_CHARTS = 50;
    public static final int MAX_FILTERS = 20;
    public static final int MAX_PATH = 1_000;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private ProjectFile() {
    }

    /** Önce geçici dosyaya yazar, sonra yerine taşır: yazma yarıda kalırsa eski proje bozulmaz. */
    public static void save(Path file, Project project) throws IOException {
        final String json = GSON.toJson(project);
        final Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, json, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static Project load(Path file) throws IOException {
        if (Files.size(file) > MAX_BYTES)
            throw new VizException("projectTooLarge");
        return parse(Files.readString(file, StandardCharsets.UTF_8));
    }

    static Project parse(String json) {
        final Project raw;
        try {
            raw = GSON.fromJson(json, Project.class);
        } catch (JsonParseException | IllegalStateException | NumberFormatException e) {
            throw new VizException("projectInvalid");
        }
        if (raw == null || !Project.APP.equals(raw.app()))
            throw new VizException("projectInvalid");
        if (raw.version() > Project.VERSION)
            throw new VizException("projectNewer");
        return new Project(Project.APP, Project.VERSION, dataset(raw.dataset()), types(raw.types()), filters(raw.filters()),
                charts(raw.charts()));
    }

    private static Project.DatasetRef dataset(Project.DatasetRef d) {
        if (d == null)
            throw new VizException("projectInvalid");
        final String sample = d.sampleId() != null && SampleDataset.of(d.sampleId()).isPresent() ? d.sampleId() : null;
        final String file = d.file() != null && !d.file().isBlank() && d.file().length() <= MAX_PATH ? d.file() : null;
        final String embedded = d.embedded() != null && d.embedded().length() <= MAX_EMBEDDED ? d.embedded() : null;
        if (sample == null && file == null && embedded == null)
            throw new VizException("projectInvalid");
        final String delimiter = d.delimiter() != null && d.delimiter().length() == 1 ? d.delimiter() : null;
        final String charset = d.charset() != null && d.charset().length() <= 40 ? d.charset() : null;
        final String name = d.name() == null ? "" : d.name().length() > 200 ? d.name().substring(0, 200) : d.name();
        return new Project.DatasetRef(name, sample, file, embedded, charset, delimiter, d.header(), d.decimal());
    }

    private static Map<String, ColumnType> types(Map<String, ColumnType> types) {
        final Map<String, ColumnType> out = new LinkedHashMap<>();
        if (types == null)
            return out;
        types.forEach((k, v) -> {
            if (k != null && v != null && k.length() <= 200 && out.size() < 500)
                out.put(k, v);
        });
        return out;
    }

    private static List<Filter> filters(List<Filter> filters) {
        if (filters == null)
            return List.of();
        return filters.stream().filter(ProjectFile::validFilter).limit(MAX_FILTERS).toList();
    }

    static boolean validFilter(Filter f) {
        return f != null && f.kind() != null && f.column() != null && f.column().length() <= 200
                && (f.values() == null || f.values().size() <= Filter.MAX_VALUES)
                && (f.min() == null || Double.isFinite(f.min())) && (f.max() == null || Double.isFinite(f.max()));
    }

    /** Kartlar normalize edilir; aynı kimlik iki kez geçerse yenisi verilir (pano kimlikle iş yapar). */
    private static List<ChartSpec> charts(List<ChartSpec> charts) {
        if (charts == null)
            return List.of();
        final Set<String> ids = new HashSet<>();
        return charts.stream().filter(Objects::nonNull).limit(MAX_CHARTS)
                .map(ChartSpec::normalized)
                .map(c -> c.withFilters(c.filters().stream().filter(ProjectFile::validFilter).toList()))
                .map(c -> ids.add(c.id()) ? c : c.withId(java.util.UUID.randomUUID().toString()))
                .toList();
    }
}
