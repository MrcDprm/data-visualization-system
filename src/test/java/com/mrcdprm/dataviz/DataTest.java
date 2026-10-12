package com.mrcdprm.dataviz;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mrcdprm.dataviz.data.Column;
import com.mrcdprm.dataviz.data.ColumnStats;
import com.mrcdprm.dataviz.data.ColumnType;
import com.mrcdprm.dataviz.data.CsvReader;
import com.mrcdprm.dataviz.data.DataSet;
import com.mrcdprm.dataviz.data.Dates;
import com.mrcdprm.dataviz.data.DecimalStyle;
import com.mrcdprm.dataviz.data.ImportOptions;
import com.mrcdprm.dataviz.data.Numbers;
import com.mrcdprm.dataviz.data.SampleDataset;
import com.mrcdprm.dataviz.data.VizException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** CSV okuma, kodlama/ayırıcı/başlık tanıma, sayı ve tarih çözme, sütun türü ve özetleri. */
class DataTest {

    private static DataSet load(String csv) {
        return DataSet.of(CsvReader.readText(csv, ImportOptions.AUTO), "test", DataSet.Source.PASTED);
    }

    // ---------------------------------------------------------------- sayılar ve tarihler

    @Test
    void numbersInBothStyles() {
        assertEquals(1234.56, Numbers.parse("1,234.56", DecimalStyle.DOT), 1e-9);
        assertEquals(1234.56, Numbers.parse("1.234,56", DecimalStyle.COMMA), 1e-9);
        assertEquals(65.26, Numbers.parse("65,26", DecimalStyle.COMMA), 1e-9);
        assertEquals(-12.5, Numbers.parse("−12,5", DecimalStyle.COMMA), 1e-9);
        assertEquals(12.5, Numbers.parse("%12,5", DecimalStyle.COMMA), 1e-9);
        assertEquals(1250, Numbers.parse("1 250 ₺", DecimalStyle.COMMA), 1e-9);
        assertEquals(1500, Numbers.parse("1500 TL", DecimalStyle.DOT), 1e-9);
        assertEquals(0.0843, Numbers.parse("0.0843", DecimalStyle.DOT), 1e-9);
        assertEquals(2.5e9, Numbers.parse("2.5e9", DecimalStyle.DOT), 1);
        assertTrue(Double.isNaN(Numbers.parse("65,26", DecimalStyle.DOT)));
        assertTrue(Double.isNaN(Numbers.parse("1.2.3", DecimalStyle.DOT)));
        assertTrue(Double.isNaN(Numbers.parse("abc", DecimalStyle.DOT)));
        assertTrue(Double.isNaN(Numbers.parse("", DecimalStyle.DOT)));
        assertTrue(Double.isNaN(Numbers.parse("1e999", DecimalStyle.DOT)));
        assertEquals(DecimalStyle.COMMA, Numbers.detect(List.of("65,26", "75,02", "0,00"), DecimalStyle.DOT));
        assertEquals(DecimalStyle.DOT, Numbers.detect(List.of("0.0843", "12.5", "7"), DecimalStyle.COMMA));
        // sadece tam sayılar: ipucu belirler
        assertEquals(DecimalStyle.COMMA, Numbers.detect(List.of("12", "1500"), DecimalStyle.COMMA));
    }

    @Test
    void datesInCommonFormats() {
        assertEquals(LocalDate.of(2024, 2, 19), Dates.parse("19.02.2024", "d.M.uuuu"));
        assertEquals(LocalDate.of(2023, 4, 1), Dates.parse("1.4.2023", "d.M.uuuu"));
        assertEquals(LocalDate.of(2024, 2, 19), Dates.parse("2024-02-19 14:30:00", "uuuu-M-d"));
        assertEquals(LocalDate.of(2024, 2, 19), Dates.parse("2024-02-19T14:30:00Z", "uuuu-M-d"));
        assertEquals(LocalDate.of(1880, 1, 1), Dates.parse("1880-01", "uuuu-M"));
        assertNull(Dates.parse("31.02.2024", "d.M.uuuu"), "geçersiz gün kabul edilmez");
        assertEquals("d.M.uuuu", Dates.detect(List.of("23.10.2000", "1.4.2023")));
        assertEquals("uuuu-M", Dates.detect(List.of("1880-01", "2024-12")));
        // 13/02 ancak gün/ay olabilir; 02/13 ancak ay/gün
        assertEquals("d/M/uuuu", Dates.detect(List.of("13/02/2024", "01/02/2024")));
        assertEquals("M/d/uuuu", Dates.detect(List.of("02/13/2024", "02/01/2024")));
        assertNull(Dates.detect(List.of("hello", "1234")));
    }

    // ---------------------------------------------------------------- CSV

    @Test
    void quotedFieldsAndEmbeddedNewlines() {
        final CsvReader.Table t = CsvReader.readText("name,comment,value\n\"Kaya, Ali\",\"said \"\"hi\"\"\nthen left\",3\n", ImportOptions.AUTO);
        assertEquals(List.of("name", "comment", "value"), t.headers());
        assertEquals(1, t.rows().size());
        assertArrayEquals(new String[] {"Kaya, Ali", "said \"hi\"\nthen left", "3"}, t.rows().get(0));
    }

    @Test
    void detectsDelimiterHeaderAndTurkishNumbers() {
        final DataSet ds = load("Tarih;Şehir;Doluluk\n01.01.2024;İstanbul;65,26\n02.01.2024;Ankara;70,10\n03.01.2024;İzmir;\n");
        assertEquals(';', ds.options().delimiter());
        assertTrue(ds.options().header());
        assertEquals(DecimalStyle.COMMA, ds.options().decimal());
        assertEquals(ColumnType.DATE, ds.column("Tarih").orElseThrow().type());
        assertEquals(ColumnType.TEXT, ds.column("Şehir").orElseThrow().type());
        final Column d = ds.column("Doluluk").orElseThrow();
        assertEquals(ColumnType.NUMBER, d.type());
        assertEquals(65.26, d.number(0), 1e-9);
        assertTrue(d.missing(2));
    }

    @Test
    void pastedExcelTableIsTabSeparated() {
        final DataSet ds = load("Ürün\tAdet\tFiyat\nKalem\t3\t12,50\nDefter\t5\t40,00\n");
        assertEquals('\t', ds.options().delimiter());
        assertEquals(12.5, ds.column("Fiyat").orElseThrow().number(0), 1e-9);
    }

    @Test
    void headerlessFileGetsColumnNames() {
        final CsvReader.Table t = CsvReader.readText("1,2\n3,4\n", ImportOptions.AUTO);
        assertFalse(t.options().header());
        assertEquals(List.of("Column 1", "Column 2"), t.headers());
        assertEquals(2, t.rows().size());
    }

    @Test
    void duplicateBlankHeadersAndRaggedRows() {
        final CsvReader.Table t = CsvReader.readText("a,a,,b\n1,2,3\n4,5,6,7,8\n", ImportOptions.AUTO);
        assertEquals(List.of("a", "a (2)", "Column 3", "b", "Column 5"), t.headers());
        assertEquals("", t.rows().get(0)[3]);
        assertTrue(t.warnings().stream().anyMatch(w -> w.code().equals("raggedRows")));
    }

    @Test
    void encodings() {
        final String text = "Şehir;Değer\nİstanbul;1\nÇorum;2\n";
        final byte[] windows = text.getBytes(Charset.forName("windows-1254"));
        CsvReader.Table t = CsvReader.read(windows, ImportOptions.AUTO);
        assertEquals("windows-1254", t.options().charset());
        assertEquals("Şehir", t.headers().get(0));
        assertEquals("Çorum", t.rows().get(1)[0]);
        final byte[] bom = ("﻿" + text).getBytes(StandardCharsets.UTF_8);
        t = CsvReader.read(bom, ImportOptions.AUTO);
        assertEquals("UTF-8", t.options().charset());
        assertEquals("Şehir", t.headers().get(0), "BOM başlığa karışmaz");
    }

    @Test
    void emptyAndInvalidInput() {
        assertEquals("emptyFile", assertThrows(VizException.class, () -> CsvReader.readText("\n\n", ImportOptions.AUTO)).code());
        assertEquals("noRows", assertThrows(VizException.class, () -> CsvReader.readText("a,b\n", ImportOptions.AUTO)).code());
        assertEquals("unknownCharset", assertThrows(VizException.class,
                () -> CsvReader.read(new byte[] {65}, ImportOptions.AUTO.withCharset("nope"))).code());
    }

    @Test
    void longCellsAreCut() {
        final String big = "x".repeat(CsvReader.MAX_CELL_LENGTH + 50);
        final CsvReader.Table t = CsvReader.readText("a\n" + big + "\n", ImportOptions.AUTO.withHeader(true));
        assertEquals(CsvReader.MAX_CELL_LENGTH, t.rows().get(0)[0].length());
        assertTrue(t.warnings().stream().anyMatch(w -> w.code().equals("longCells")));
    }

    // ---------------------------------------------------------------- türler ve özetler

    @Test
    void typeOverrideAndStats() {
        final DataSet ds = load("code,value\n001,10\n002,20\n003,x\n004,30\n");
        final Column code = ds.column("code").orElseThrow();
        assertEquals(ColumnType.NUMBER, code.type());
        code.setType(ColumnType.TEXT);
        assertEquals("001", code.raw(0));
        final Column value = ds.column("value").orElseThrow();
        // 4 değerden 1'i sayı değil: %95 altında kaldığı için metin
        assertEquals(ColumnType.TEXT, value.type());
        value.setType(ColumnType.NUMBER);
        assertEquals(1, value.invalidCount());
        final ColumnStats s = value.stats();
        assertEquals(3, s.filled());
        assertEquals(1, s.missing());
        assertEquals(10, s.min(), 1e-9);
        assertEquals(30, s.max(), 1e-9);
        assertEquals(20, s.mean(), 1e-9);
        assertEquals("notDateColumn", assertThrows(VizException.class, () -> value.setType(ColumnType.DATE)).code());
    }

    @Test
    void bundledSampleDatasetsLoadWithExpectedTypes() {
        final DataSet co2 = SampleDataset.OWID_CO2.load("co2");
        assertTrue(co2.rowCount() > 15_000);
        assertEquals(ColumnType.TEXT, co2.column("country").orElseThrow().type());
        assertEquals(ColumnType.NUMBER, co2.column("year").orElseThrow().type());
        assertEquals(ColumnType.NUMBER, co2.column("co2").orElseThrow().type());

        final DataSet temp = SampleDataset.NASA_GISTEMP.load("gistemp");
        assertEquals(ColumnType.DATE, temp.column("Date").orElseThrow().type());
        assertEquals(LocalDate.of(1880, 1, 1), temp.column("Date").orElseThrow().date(0));
        assertEquals(ColumnType.NUMBER, temp.column("Anomaly (°C)").orElseThrow().type());

        final DataSet dams = SampleDataset.IBB_DAMS.load("baraj");
        assertEquals(';', dams.options().delimiter());
        assertEquals(DecimalStyle.COMMA, dams.options().decimal());
        assertEquals(ColumnType.DATE, dams.column("Tarih").orElseThrow().type());
        assertEquals(65.26, dams.column("Ömerli").orElseThrow().number(0), 1e-9);
        assertEquals(0, dams.column("Ömerli").orElseThrow().invalidCount());
        assertTrue(dams.warnings().isEmpty());
    }
}
