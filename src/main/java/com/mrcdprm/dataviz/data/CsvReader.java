package com.mrcdprm.dataviz.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * CSV / TSV okuyucu (RFC 4180): tırnak içindeki ayırıcı, çift tırnak ("") ve satır sonu desteklenir.
 * Karakter kodlaması (BOM, UTF-8 ya da Türkçe Windows-1254), ayırıcı ve başlık satırı otomatik bulunur.
 * Dosyaya güvenilmez: boyut, satır, sütun ve hücre uzunluğu sınırlıdır; bozuk satırlar düzeltilip uyarı verilir.
 */
public final class CsvReader {

    /** Okunan tablo: başlıklar, satırlar, çözümlenmiş ayarlar ve uyarılar. */
    public record Table(List<String> headers, List<String[]> rows, ImportOptions options, List<Warning> warnings) {
    }

    /** Kullanıcıya gösterilecek uyarı (kod + argümanlar, arayüzde "warning.<kod>"). */
    public record Warning(String code, Object... args) {
    }

    public static final long MAX_BYTES = 200L * 1024 * 1024;
    /** Bellek sınırı: satır x sütun en fazla 5 milyon hücre. */
    public static final long MAX_CELLS = 5_000_000;
    public static final int MAX_COLUMNS = 500;
    public static final int MAX_CELL_LENGTH = 10_000;
    public static final int MAX_HEADER_LENGTH = 100;
    public static final List<Character> DELIMITERS = List.of(',', ';', '\t', '|');
    public static final Charset TURKISH = Charset.forName("windows-1254");

    private CsvReader() {
    }

    public static Table read(Path file, ImportOptions options) throws IOException {
        if (Files.size(file) > MAX_BYTES)
            throw new VizException("fileTooLarge", MAX_BYTES / 1024 / 1024);
        return read(Files.readAllBytes(file), options);
    }

    public static Table read(InputStream in, ImportOptions options) throws IOException {
        final byte[] bytes = in.readNBytes((int) Math.min(Integer.MAX_VALUE - 8, MAX_BYTES + 1));
        if (bytes.length > MAX_BYTES)
            throw new VizException("fileTooLarge", MAX_BYTES / 1024 / 1024);
        return read(bytes, options);
    }

    public static Table read(byte[] bytes, ImportOptions options) {
        final Charset charset = charset(bytes, options.charset());
        if (!charset.name().startsWith("UTF-16") && looksBinary(bytes))
            throw new VizException("notText");
        return parse(decode(bytes, charset), options.withCharset(charset.name()));
    }

    /** Yapıştırılan metin (Excel'den kopyalanan tablo sekmeyle ayrılmıştır). */
    public static Table readText(String text, ImportOptions options) {
        if (text != null && text.length() > MAX_BYTES / 2)
            throw new VizException("fileTooLarge", MAX_BYTES / 1024 / 1024);
        return parse(text == null ? "" : text, options.withCharset(null));
    }

    // ---------------------------------------------------------------- karakter kodlaması

    /** BOM varsa ona, yoksa UTF-8 olarak hatasız çözülüyorsa UTF-8'e, değilse Türkçe Windows-1254'e karar verir. */
    static Charset charset(byte[] bytes, String requested) {
        if (requested != null) {
            try {
                return Charset.forName(requested);
            } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
                throw new VizException("unknownCharset", requested);
            }
        }
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xEF && (bytes[1] & 0xff) == 0xBB && (bytes[2] & 0xff) == 0xBF)
            return StandardCharsets.UTF_8;
        if (bytes.length >= 2 && (bytes[0] & 0xff) == 0xFF && (bytes[1] & 0xff) == 0xFE)
            return StandardCharsets.UTF_16LE;
        if (bytes.length >= 2 && (bytes[0] & 0xff) == 0xFE && (bytes[1] & 0xff) == 0xFF)
            return StandardCharsets.UTF_16BE;
        try {
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes));
            return StandardCharsets.UTF_8;
        } catch (CharacterCodingException e) {
            return TURKISH;
        }
    }

    /**
     * İçerik denetimi: metin dosyalarında NUL baytı olmaz; resim, Excel (.xlsx), PDF gibi dosyalarda çok olur.
     * Uzantıya bakılmaz (kullanıcı "Bütün dosyalar" ile her şeyi seçebilir), ilk 64 KB'a bakılır.
     */
    static boolean looksBinary(byte[] bytes) {
        final int n = Math.min(bytes.length, 64 * 1024);
        for (int i = 0; i < n; i++)
            if (bytes[i] == 0)
                return true;
        return false;
    }

    private static String decode(byte[] bytes, Charset charset) {
        String text = new String(bytes, charset);
        if (!text.isEmpty() && text.charAt(0) == '﻿')
            text = text.substring(1);
        return text;
    }

    // ---------------------------------------------------------------- ayrıştırma

    private static Table parse(String text, ImportOptions options) {
        final List<Warning> warnings = new ArrayList<>();
        final char delimiter = options.delimiter() != null ? options.delimiter() : detectDelimiter(text);
        final List<String[]> records = records(text, delimiter, warnings);
        if (records.isEmpty())
            throw new VizException("emptyFile");

        final boolean header = options.header() != null ? options.header() : looksLikeHeader(records);
        int width = 0;
        for (String[] r : records)
            width = Math.max(width, r.length);
        if (width > MAX_COLUMNS) {
            warnings.add(new Warning("tooManyColumns", MAX_COLUMNS));
            width = MAX_COLUMNS;
        }
        final List<String> headers = headers(header ? records.get(0) : new String[0], width);
        final List<String[]> rows = new ArrayList<>();
        int ragged = 0;
        final long maxRows = MAX_CELLS / Math.max(1, width);
        for (int i = header ? 1 : 0; i < records.size(); i++) {
            final String[] r = records.get(i);
            if (r.length != width)
                ragged++;
            if (rows.size() >= maxRows) {
                warnings.add(new Warning("truncatedRows", maxRows));
                break;
            }
            final String[] row = new String[width];
            for (int c = 0; c < width; c++)
                row[c] = c < r.length ? r[c] : "";
            rows.add(row);
        }
        if (ragged > 0)
            warnings.add(new Warning("raggedRows", ragged));
        if (rows.isEmpty())
            throw new VizException("noRows");
        return new Table(headers, rows, options.withDelimiter(delimiter).withHeader(header), List.copyOf(warnings));
    }

    /**
     * İlk satırlara her aday ayırıcıyla bakar; satırların çoğunda aynı sayıda (1'den fazla) alan veren ayırıcı seçilir.
     * Birden fazla aday tutarlıysa daha çok sütun üreten kazanır.
     */
    static char detectDelimiter(String text) {
        final String sample = text.length() > 64_000 ? text.substring(0, 64_000) : text;
        char best = ',';
        double bestScore = -1;
        for (char d : DELIMITERS) {
            final List<String[]> recs = records(sample, d, new ArrayList<>());
            final int n = Math.min(recs.size(), 50);
            if (n == 0)
                continue;
            final Map<Integer, Integer> counts = new HashMap<>();
            for (int i = 0; i < n; i++)
                counts.merge(recs.get(i).length, 1, Integer::sum);
            final Map.Entry<Integer, Integer> mode = counts.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).orElseThrow();
            if (mode.getKey() < 2)
                continue;
            final double score = (double) mode.getValue() / n * 1000 + mode.getKey();
            if (score > bestScore) {
                bestScore = score;
                best = d;
            }
        }
        return best;
    }

    /** Metni kayıtlara böler; tırnak içinde ayırıcı ve satır sonu olabilir. Tamamen boş satırlar atlanır. */
    static List<String[]> records(String text, char delimiter, List<Warning> warnings) {
        final List<String[]> out = new ArrayList<>();
        final List<String> fields = new ArrayList<>();
        final StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean longCell = false;
        int i = 0;
        final int n = text.length();
        while (i < n) {
            final char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < n && text.charAt(i + 1) == '"') {
                        append(field, '"');
                        i += 2;
                        continue;
                    }
                    quoted = false;
                } else {
                    longCell |= !append(field, c);
                }
                i++;
                continue;
            }
            if (c == '"' && field.isEmpty()) {
                quoted = true;
            } else if (c == delimiter) {
                fields.add(field.toString().strip());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                fields.add(field.toString().strip());
                field.setLength(0);
                if (!(fields.size() == 1 && fields.get(0).isEmpty()))
                    out.add(fields.toArray(String[]::new));
                fields.clear();
                if (c == '\r' && i + 1 < n && text.charAt(i + 1) == '\n')
                    i++;
            } else {
                longCell |= !append(field, c);
            }
            i++;
        }
        if (!field.isEmpty() || !fields.isEmpty()) {
            fields.add(field.toString().strip());
            if (!(fields.size() == 1 && fields.get(0).isEmpty()))
                out.add(fields.toArray(String[]::new));
        }
        if (quoted)
            warnings.add(new Warning("unclosedQuote"));
        if (longCell)
            warnings.add(new Warning("longCells", MAX_CELL_LENGTH));
        return out;
    }

    private static boolean append(StringBuilder sb, char c) {
        if (sb.length() >= MAX_CELL_LENGTH)
            return false;
        sb.append(c);
        return true;
    }

    /**
     * İlk satır başlık mı? İlk satırda sayı ya da tarih olan hücre yoksa (ve en az bir dolu hücre varsa) başlık sayılır;
     * boş başlık hücresine izin verilir (ör. sıra numarası sütunu).
     * Yanlış tahmin içe aktarma penceresindeki "İlk satır başlık" kutusuyla düzeltilebilir.
     */
    static boolean looksLikeHeader(List<String[]> records) {
        if (records.size() < 2)
            return true;
        final String[] first = records.get(0);
        boolean anyText = false;
        for (String cell : first) {
            if (isValue(cell))
                return false;
            anyText |= !cell.isBlank();
        }
        return anyText;
    }

    private static boolean isValue(String cell) {
        if (cell == null || cell.isBlank())
            return false;
        return !Double.isNaN(Numbers.parse(cell, DecimalStyle.DOT)) || !Double.isNaN(Numbers.parse(cell, DecimalStyle.COMMA))
                || Dates.detect(List.of(cell)) != null;
    }

    /** Boş başlıklar "Column N", tekrar eden başlıklar "ad (2)" olur; çok uzun başlıklar kısaltılır. */
    static List<String> headers(String[] raw, int width) {
        final List<String> out = new ArrayList<>(width);
        final Set<String> used = new HashSet<>();
        for (int i = 0; i < width; i++) {
            String name = i < raw.length ? raw[i].strip() : "";
            if (name.length() > MAX_HEADER_LENGTH)
                name = name.substring(0, MAX_HEADER_LENGTH);
            if (name.isEmpty())
                name = "Column " + (i + 1);
            String unique = name;
            for (int k = 2; !used.add(unique.toLowerCase(Locale.ROOT)); k++)
                unique = name + " (" + k + ")";
            out.add(unique);
        }
        return out;
    }
}
