package com.mrcdprm.dataviz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mrcdprm.dataviz.chart.Aggregation;
import com.mrcdprm.dataviz.chart.ChartType;
import com.mrcdprm.dataviz.chart.DateBucket;
import com.mrcdprm.dataviz.chart.Palette;
import com.mrcdprm.dataviz.chart.SortOrder;
import com.mrcdprm.dataviz.data.ColumnType;
import com.mrcdprm.dataviz.data.SampleDataset;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** İki dil dosyası aynı anahtarlara sahip mi ve kodda geçen her metin anahtarı tanımlı mı? */
class I18nTest {

    /** Anahtar biçiminde olup metin anahtarı olmayan değerler (dosya adları, sistem özellikleri, kalıplar). */
    private static final Set<String> NOT_KEYS = Set.of("settings.properties", "dataviz.dataDir", "user.home",
            "com.mrcdprm.dataviz.i18n.messages", "dd.MM.yyyy");

    private static Properties load(String lang) throws IOException {
        final Properties p = new Properties();
        try (InputStream in = I18nTest.class.getResourceAsStream("/com/mrcdprm/dataviz/i18n/messages_" + lang + ".properties")) {
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return p;
    }

    @Test
    void bothLanguagesHaveTheSameKeys() throws IOException {
        assertEquals(new TreeSet<>(load("tr").stringPropertyNames()), new TreeSet<>(load("en").stringPropertyNames()));
    }

    @Test
    void everyKeyUsedInCodeExists() throws IOException {
        final Properties tr = load("tr");
        final Set<String> used = new TreeSet<>();
        final Pattern literal = Pattern.compile("\"([a-z]+\\.[a-zA-Z0-9.]+)\"");
        final Pattern error = Pattern.compile("VizException\\(\"([a-zA-Z]+)\"");
        final Pattern warning = Pattern.compile("Warning\\(\"([a-zA-Z]+)\"");
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                final String code = Files.readString(file);
                if (file.toString().contains("ui")) {
                    final Matcher m = literal.matcher(code);
                    while (m.find())
                        used.add(m.group(1));
                }
                final Matcher e = error.matcher(code);
                while (e.find())
                    used.add("error." + e.group(1));
                final Matcher w = warning.matcher(code);
                while (w.find())
                    used.add("warning." + w.group(1));
            }
        }
        for (Enum<?>[] values : List.<Enum<?>[]>of(ColumnType.values(), ChartType.values(), Aggregation.values(),
                DateBucket.values(), SortOrder.values(), Palette.values()))
            for (Enum<?> v : values)
                used.add("enum." + v.getDeclaringClass().getSimpleName() + "." + v.name());
        for (SampleDataset s : SampleDataset.values()) {
            used.add("sample." + s.id() + ".name");
            used.add("sample." + s.id() + ".description");
        }
        used.add("project.untitled");
        used.removeAll(NOT_KEYS);

        final List<String> missing = new ArrayList<>();
        for (String key : used)
            if (!tr.containsKey(key) && !key.endsWith("."))
                missing.add(key);
        assertTrue(missing.isEmpty(), "Eksik anahtarlar: " + missing);
    }
}
